package com.tiomamaster.espressif

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

enum class Security1MessageType(val value: Int) {
    COMMAND_0(0),
    RESPONSE_0(1),
    COMMAND_1(2),
    RESPONSE_1(3)
}

enum class Status(val value: Int) {
    SUCCESS(0),
    INVALID_SEC_SCHEME(1),
    INVALID_PROTO(2),
    TOO_MANY_SESSIONS(3),
    INVALID_ARGUMENT(4),
    INTERNAL_ERROR(5),
    CRYPTO_ERROR(6),
    INVALID_SESSION(7)
}

enum class WiFiScanMessageType(val value: Int) {
    COMMAND_SCAN_START(0),
    RESPONSE_SCAN_START(1),
    COMMAND_SCAN_STATUS(2),
    RESPONSE_SCAN_STATUS(3),
    COMMAND_SCAN_RESULT(4),
    RESPONSE_SCAN_RESULT(5);
}

enum class WiFiAuthMode(val value: Int) {
    OPEN(0),
    WEP(1),
    WPA_PSK(2),
    WPA2_PSK(3),
    WPA_WPA2_PSK(4),
    WPA2_ENTERPRISE(5);
}

@Serializable
data class SessionData constructor(
    @ProtoNumber(2) val securityVersion: Int,
    @ProtoNumber(11) val security1payload: Security1Payload
)

@Serializable
data class Security1Payload(
    @ProtoNumber(1) val msg: Security1MessageType,
    @ProtoNumber(20) val sessionCommand0: SessionCommand0? = null,
    @ProtoNumber(21) val sessionResponse0: SessionResponse0? = null,
    @ProtoNumber(22) val sessionCommand1: SessionCommand1? = null,
    @ProtoNumber(23) val sessionResponse1: SessionResponse1? = null
)

@Suppress("ArrayInDataClass")
@Serializable
data class SessionCommand0(@ProtoNumber(1) val clientPublicKey: ByteArray)

@Suppress("ArrayInDataClass")
@Serializable
data class SessionCommand1(@ProtoNumber(2) val clientVerifyData: ByteArray)

@Suppress("ArrayInDataClass")
@Serializable
data class SessionResponse0(
    @ProtoNumber(1) val status: Status? = null,
    @ProtoNumber(2) val devicePublicKey: ByteArray,
    @ProtoNumber(3) val deviceRandom: ByteArray
)

@Suppress("ArrayInDataClass")
@Serializable
data class SessionResponse1(
    @ProtoNumber(1) val status: Status? = null,
    @ProtoNumber(3) val deviceVerifyData: ByteArray
)

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

@Suppress("ArrayInDataClass")
@Serializable
data class WiFiScanResult(
    @ProtoNumber(1) val ssid: ByteArray,
    @ProtoNumber(2) val channel: Int,
    @ProtoNumber(3) val rssi: Int,
    @ProtoNumber(4) val bssid: ByteArray,
    @ProtoNumber(5) val auth: WiFiAuthMode
)
