package com.tiomamaster.espressif

import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.tiomamaster.espressif.dto.*
import com.tiomamaster.espressif.model.BleDevice
import com.tiomamaster.espressif.model.WiFiNetwork
import com.tiomamaster.espressif.security.Cipher
import com.tiomamaster.espressif.security.X25519
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class BleProvisionManager(serviceCharacteristicUuid: String) {

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

    fun searchDevices(): Flow<List<BleDevice>> {
        val devices = mutableSetOf<BleDevice>()
        return scanner.advertisements.map {
            devices.add(BleDevice(it.name, it.mac, it.rssi, it.txPower))
            devices.toList()
        }.flowOn(Dispatchers.Default)
    }

    suspend fun connect(device: BleDevice, scope: CoroutineScope) {
        val advertisement = scanner.advertisements.firstOrNull { it.mac == device.mac }
            ?: throw Exception("Can't find bluetooth device with the given mac ${device.mac}")
        peripheral = scope.peripheral(advertisement)
        peripheral.connect()

        characteristics = peripheral.discoverCharacteristics()

        val sessionCharacteristic = getCharacteristic(PATH_SESSION)

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
        val scanCharacteristic = getCharacteristic(PATH_SCAN)

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

    suspend fun configureWiFi(ssid: String, passphrase: String) {
        val configCharacteristic = getCharacteristic(PATH_CONFIG)

        var wiFiConfigPayload = WiFiConfigPayload(
            WiFiConfigMessageType.COMMAND_SET_CONFIG,
            commandSetConfig = CommandSetConfig(ssid, passphrase)
        )
        wiFiConfigPayload = configCharacteristic.writeAndRead(wiFiConfigPayload)
        Napier.d("WiFi configure response = $wiFiConfigPayload")

        if (wiFiConfigPayload.responseSetConfig?.status != Status.SUCCESS) {
            throw Exception("Could not send wifi credentials to device")
        }
    }

    suspend fun applyConfigurations() {
        val configCharacteristic = getCharacteristic(PATH_CONFIG)

        var wiFiConfigPayload = WiFiConfigPayload(
            WiFiConfigMessageType.COMMAND_APPLY_CONFIG,
            commApplyConfig = CommandApplyConfig()
        )
        wiFiConfigPayload = configCharacteristic.writeAndRead(wiFiConfigPayload)
        Napier.d("Apply configurations response = $wiFiConfigPayload")

        val status = wiFiConfigPayload.responseApplyConfig?.status
        if (status != Status.SUCCESS) throw Exception("Apply configurations failed. Status = $status")
    }

    suspend fun checkWifiConnectionStatus() {
        val configCharacteristic = getCharacteristic(PATH_CONFIG)

        while (true) {
            var wiFiConfigPayload = WiFiConfigPayload(
                WiFiConfigMessageType.COMMAND_GET_STATUS,
                commandGetStatus = CommandGetStatus()
            )
            wiFiConfigPayload = configCharacteristic.writeAndRead(wiFiConfigPayload)
            Napier.d("Get wifi status response = $wiFiConfigPayload")

            val status = wiFiConfigPayload.responseGetStatus?.status
            val failedReason = wiFiConfigPayload.responseGetStatus?.failedReason
            val stationState = wiFiConfigPayload.responseGetStatus?.stationState
            if (status == Status.SUCCESS) {
                when (stationState) {
                    WifiStationState.CONNECTED -> break
                    WifiStationState.CONNECTING -> delay(500)
                    else -> throw Exception("WiFi connection failed. stationState = $stationState, failedReason = $failedReason")
                }
            } else {
                throw Exception("WiFi connection failed. status = $status, stationState = $stationState, failedReason = $failedReason")
            }
        }
    }

    suspend fun sendConfigData(path: String, data: ByteArray): ByteArray =
        getCharacteristic(path).writeAndRead(data)

    private suspend fun Peripheral.discoverCharacteristics() = this.services
        ?.flatMap(DiscoveredService::characteristics)
        ?.associateBy {
            it.descriptors.firstOrNull()
                ?.let { descriptor -> this.read(descriptor).decodeToString() }
        }

    private fun getCharacteristic(path: String): DiscoveredCharacteristic =
        if (characteristics == null) throw IllegalStateException("There are no discovered characteristics. Call connect() first.")
        else characteristics?.get(path)
            ?: throw IllegalStateException("Characteristic with $path descriptor not found")

    private suspend inline fun <reified T> DiscoveredCharacteristic.writeAndRead(
        data: T,
        encrypted: Boolean = true
    ): T {
        var bytes = if (data is ByteArray) data else ProtoBuf.encodeToByteArray(data)
        peripheral.write(
            this,
            if (encrypted) cipher.encrypt(bytes) else bytes,
            WriteType.WithResponse
        )
        bytes = peripheral.read(this)
        bytes = if (encrypted) cipher.decrypt(bytes) else bytes
        return if (data is ByteArray) bytes as T else ProtoBuf.decodeFromByteArray(bytes)
    }

    companion object {
        private const val PATH_SESSION = "prov-session"
        private const val PATH_SCAN = "prov-scan"
        private const val PATH_CONFIG = "prov-config"
    }
}
