package com.tiomamaster.espressif

import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.tiomamaster.espressif.dto.*
import com.tiomamaster.espressif.model.BleDevice
import com.tiomamaster.espressif.model.WiFiNetwork
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
            SecuritySchemeVersion.SECURITY_SCHEME_1,
            Security1Payload(
                Security1MessageType.COMMAND_0,
                SessionCommand0(publicKey)
            )
        )
        val response0 = sessionCharacteristic.writeAndRead(sessionData, false)
            .security1payload.sessionResponse0
            ?: throw Exception("Session establishment failed. SessionResponse0 is null.")

        val sharedKey = X25519.computeSharedSecret(privateKey, response0.devicePublicKey)
        cipher = Cipher(response0.deviceRandom, sharedKey)
        val clientVerify = cipher.encrypt(response0.devicePublicKey)

        sessionData = SessionData(
            SecuritySchemeVersion.SECURITY_SCHEME_1,
            Security1Payload(
                Security1MessageType.COMMAND_1,
                sessionCommand1 = SessionCommand1(clientVerify)
            )
        )
        val deviceVerifyData = sessionCharacteristic.writeAndRead(sessionData, false)
            .security1payload.sessionResponse1?.deviceVerifyData?.run(cipher::decrypt)
            ?: throw Exception("Session establishment failed. Device verify data is null.")

        if (!publicKey.contentEquals(deviceVerifyData)) {
            throw Exception("Session establishment failed. Public key is not equal to device verify data.")
        }
        Napier.i("Session successfully established with the device $device")
    }

    suspend fun getWiFiList(): List<WiFiNetwork> {
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
        wifiScanPayload = scanCharacteristic.writeAndRead(wifiScanPayload)
        Napier.d("Start wifi scan response = $wifiScanPayload")

        // wifi scan status
        wifiScanPayload = WifiScanPayload(
            WiFiScanMessageType.COMMAND_SCAN_STATUS,
            commandScanStatus = CommandScanStatus()
        )
        val responseScanStatus = scanCharacteristic.writeAndRead(wifiScanPayload).responseScanStatus
            ?: throw Exception("Unable to get wifi list from devices. Scan status response is null.")
        Napier.d("WiFi scan status response = $wifiScanPayload")

        // wifi scan result
        wifiScanPayload = WifiScanPayload(
            WiFiScanMessageType.COMMAND_SCAN_RESULT,
            commandScanResult = CommandScanResult(0, responseScanStatus.resultCount)
        )
        return scanCharacteristic.writeAndRead(wifiScanPayload).also {
            Napier.d("WiFi scan result = $it")
        }.responseScanResult?.entries?.map {
            WiFiNetwork(it.ssid, it.channel, it.rssi, it.bssid, it.auth)
        } ?: emptyList()
    }

    private suspend fun Peripheral.discoverCharacteristics() = this.services
        ?.flatMap(DiscoveredService::characteristics)
        ?.associateBy {
            it.descriptors.firstOrNull()
                ?.let { descriptor -> this.read(descriptor).decodeToString() }
        }

    private suspend inline fun <reified T> DiscoveredCharacteristic.writeAndRead(
        data: T,
        encrypted: Boolean = true
    ): T {
        var bytes = ProtoBuf.encodeToByteArray(data)
        peripheral.write(
            this,
            if (encrypted) cipher.encrypt(bytes) else bytes,
            WriteType.WithResponse
        )
        bytes = peripheral.read(this)
        return ProtoBuf.decodeFromByteArray(if (encrypted) cipher.decrypt(bytes) else bytes)
    }
}
