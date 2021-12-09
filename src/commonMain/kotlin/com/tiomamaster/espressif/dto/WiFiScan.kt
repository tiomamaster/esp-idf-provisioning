package com.tiomamaster.espressif.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

enum class WiFiScanMessageType(val value: Int) {
    COMMAND_SCAN_START(0),
    RESPONSE_SCAN_START(1),
    COMMAND_SCAN_STATUS(2),
    RESPONSE_SCAN_STATUS(3),
    COMMAND_SCAN_RESULT(4),
    RESPONSE_SCAN_RESULT(5)
}

enum class WiFiAuthMode(val value: Int) {
    OPEN(0),
    WEP(1),
    WPA_PSK(2),
    WPA2_PSK(3),
    WPA_WPA2_PSK(4),
    WPA2_ENTERPRISE(5)
}

@Serializable
data class WifiScanPayload(
    @ProtoNumber(1) val msg: WiFiScanMessageType,
    @ProtoNumber(2) val status: Status? = null,
    @ProtoNumber(10) val commandScanStart: CommandScanStart? = null,
    @ProtoNumber(12) val commandScanStatus: CommandScanStatus? = null,
    @ProtoNumber(13) val responseScanStatus: ResponseScanStatus? = null,
    @ProtoNumber(14) val commandScanResult: CommandScanResult? = null,
    @ProtoNumber(15) val responseScanResult: ResponseScanResult? = null
)

@Serializable
data class CommandScanStart(
    @ProtoNumber(1) val blocking: Boolean,
    @ProtoNumber(2) val passive: Boolean,
    @ProtoNumber(3) val groupChannels: Int,
    @ProtoNumber(4) val periodMs: Int
)

@Serializable
class CommandScanStatus

@Serializable
data class ResponseScanStatus(
    @ProtoNumber(1) val scanFinished: Boolean,
    @ProtoNumber(2) val resultCount: Int
)

@Serializable
data class CommandScanResult(
    @ProtoNumber(1) val startIndex: Int,
    @ProtoNumber(2) val count: Int
)

@Serializable
data class ResponseScanResult(@ProtoNumber(1) val entries: List<WiFiScanResult> = emptyList())

@Serializable
data class WiFiScanResult(
    @ProtoNumber(1) val ssid: String,
    @ProtoNumber(2) val channel: Int,
    @ProtoNumber(3) val rssi: Int,
    @ProtoNumber(4) val bssid: String,
    @ProtoNumber(5) val auth: WiFiAuthMode
)
