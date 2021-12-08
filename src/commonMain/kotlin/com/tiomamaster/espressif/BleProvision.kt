package com.tiomamaster.espressif

import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class BleProvision(serviceCharacteristicUuid: String) {

    private val scanner = Scanner {
        services = listOf(uuidFrom(serviceCharacteristicUuid))
        logging {
            engine = SystemLogEngine
            level = Logging.Level.Data
            format = Logging.Format.Multiline
        }
    }

    private var characteristics: Map<String?, DiscoveredCharacteristic>? = null

    private lateinit var cipher: Cipher

    private lateinit var peripheral: Peripheral

    private val Advertisement.mac
        get() = toString().substringAfter("bluetoothDevice=").substringBefore(",")

    fun searchDevices(): Flow<Set<BleDevice>> {
        val devices = mutableSetOf<BleDevice>()
        return scanner.advertisements.map {
            devices.add(BleDevice(it.name, it.mac, it.rssi, it.txPower))
            devices
        }.flowOn(Dispatchers.Default)
    }

    suspend fun connect(device: BleDevice, scope: CoroutineScope) {
        val advertisement = scanner.advertisements.firstOrNull { it.mac == device.mac }
            ?: throw Exception("Can't find bluetooth device with the given mac ${device.mac}")
        peripheral = scope.peripheral(advertisement)
        peripheral.connect()

        characteristics = peripheral.discoverCharacteristics()

        val sessionCharacteristic = characteristics?.get("prov-session")
            ?: throw IllegalStateException("Characteristic with prov-session descriptor not found")

        val (privateKey, publicKey) = X25519.generateKeyPair()
        var sessionData = SessionData(
            1,
            Security1Payload(
                Security1MessageType.COMMAND_0,
                SessionCommand0(publicKey)
            )
        )
        peripheral.write(
            sessionCharacteristic,
            ProtoBuf.encodeToByteArray(sessionData),
            WriteType.WithResponse
        )
        val response0 =
            ProtoBuf.decodeFromByteArray<SessionData>(peripheral.read(sessionCharacteristic))
                .security1payload.sessionResponse0
                ?: throw Exception("Session establishment failed. SessionResponse0 is null.")

        val sharedKey = X25519.computeSharedSecret(privateKey, response0.devicePublicKey)
        cipher = Cipher(response0.deviceRandom, sharedKey)
        val clientVerify = cipher.encrypt(response0.devicePublicKey)

        sessionData = SessionData(
            1,
            Security1Payload(
                Security1MessageType.COMMAND_1,
                sessionCommand1 = SessionCommand1(clientVerify)
            )
        )
        peripheral.write(
            sessionCharacteristic,
            ProtoBuf.encodeToByteArray(sessionData),
            WriteType.WithResponse
        )
        val deviceVerifyData =
            ProtoBuf.decodeFromByteArray<SessionData>(peripheral.read(sessionCharacteristic))
                .security1payload.sessionResponse1?.deviceVerifyData?.run(cipher::decrypt)
                ?: throw Exception("Session establishment failed. Device verify data is null.")

        if (!publicKey.contentEquals(deviceVerifyData)) {
            throw Exception("Session establishment failed. Public key is not equal to device verify data.")
        }
        Napier.i("Session successfully established with device $device")
    }

    suspend fun getWiFiList(): List<WiFi> {
        val scanCharacteristic = characteristics?.get("prov-scan")
            ?: throw IllegalStateException("Characteristic with prov-scan descriptor not found")

        // start wifi scan
        var wifiScanPayload = WifiScanPayload(
            WiFiScanMessageType.COMMAND_SCAN_START,
            commandScanStart = CommandScanStart(
                blocking = true,
                passive = false,
                groupChannels = 0,
                periodMs = 120
            )
        )
        peripheral.write(
            scanCharacteristic,
            cipher.encrypt(ProtoBuf.encodeToByteArray(wifiScanPayload)),
            WriteType.WithResponse
        )
        wifiScanPayload =
            ProtoBuf.decodeFromByteArray(cipher.decrypt(peripheral.read(scanCharacteristic)))
        Napier.d("Start wifi scan response = $wifiScanPayload")

        // wifi scan status
        wifiScanPayload = WifiScanPayload(
            WiFiScanMessageType.COMMAND_SCAN_STATUS,
            commandScanStatus = CommandScanStatus()
        )
        peripheral.write(
            scanCharacteristic,
            cipher.encrypt(ProtoBuf.encodeToByteArray(wifiScanPayload)),
            WriteType.WithResponse
        )
        val responseScanStatus = ProtoBuf.decodeFromByteArray<WifiScanPayload>(
            cipher.decrypt(peripheral.read(scanCharacteristic))
        ).responseScanStatus
            ?: throw Exception("Unable to get wifi list from devices. Scan status response is null.")
        Napier.d("WiFi scan status response = $wifiScanPayload")

        // wifi scan result
        wifiScanPayload = WifiScanPayload(
            WiFiScanMessageType.COMMAND_SCAN_RESULT,
            commandScanResult = CommandScanResult(0, responseScanStatus.resultCount)
        )
        peripheral.write(
            scanCharacteristic,
            cipher.encrypt(ProtoBuf.encodeToByteArray(wifiScanPayload)),
            WriteType.WithResponse
        )
        wifiScanPayload =
            ProtoBuf.decodeFromByteArray(cipher.decrypt(peripheral.read(scanCharacteristic)))
        return wifiScanPayload.responseScanResult?.entries?.map {
            WiFi(it.ssid.decodeToString(), it.channel, it.rssi, it.bssid.decodeToString(), it.auth)
        }?.also {
            Napier.d("WiFi scan result = $wifiScanPayload")
        } ?: emptyList()
    }

    private suspend fun Peripheral.discoverCharacteristics() = this.services
        ?.flatMap(DiscoveredService::characteristics)
        ?.associateBy {
            it.descriptors.firstOrNull()
                ?.let { descriptor -> this.read(descriptor).decodeToString() }
        }
}
