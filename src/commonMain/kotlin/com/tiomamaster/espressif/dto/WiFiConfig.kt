package com.tiomamaster.espressif.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

enum class WiFiConfigMessageType(val value: Int) {
    COMMAND_GET_STATUS(0),
    RESPONSE_GET_STATUS(1),
    COMMAND_SET_CONFIG(2),
    RESPONSE_SET_CONFIG(3),
    COMMAND_APPLY_CONFIG(4),
    RESPONSE_APPLY_CONFIG(5)
}

enum class WifiStationState(val value: Int) {
    CONNECTED(0),
    CONNECTING(1),
    DISCONNECTED(2),
    CONNECTION_FAILED(3)
}

enum class WifiConnectFailedReason(val value: Int) {
    AUTH_ERROR(0),
    NETWORK_NOT_FOUND(1)
}

@Serializable
data class WiFiConfigPayload(
    @ProtoNumber(1) val msg: WiFiConfigMessageType,
    @ProtoNumber(10) val commandGetStatus: CommandGetStatus? = null,
    @ProtoNumber(11) val responseGetStatus: ResponseGetStatus? = null,
    @ProtoNumber(12) val commandSetConfig: CommandSetConfig? = null,
    @ProtoNumber(13) val responseSetConfig: StatusResponse? = null,
    @ProtoNumber(14) val commApplyConfig: CommandApplyConfig? = null,
    @ProtoNumber(15) val responseApplyConfig: StatusResponse? = null,
)

@Serializable
class CommandGetStatus

@Serializable
data class ResponseGetStatus(
    @ProtoNumber(1) val status: Status? = null,
    @ProtoNumber(2) val stationState: WifiStationState? = null,
    @ProtoNumber(10) val failedReason: WifiConnectFailedReason? = null
)

@Serializable
data class CommandSetConfig(
    @ProtoNumber(1) val ssid: String,
    @ProtoNumber(2) val passphrase: String
)

@Serializable
data class StatusResponse(@ProtoNumber(1) val status: Status? = null)

@Serializable
class CommandApplyConfig
