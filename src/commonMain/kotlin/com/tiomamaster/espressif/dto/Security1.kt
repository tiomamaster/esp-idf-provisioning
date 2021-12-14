package com.tiomamaster.espressif.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

internal enum class Security1MessageType(val value: Int) {
    COMMAND_0(0),
    RESPONSE_0(1),
    COMMAND_1(2),
    RESPONSE_1(3)
}

@Serializable
internal data class Security1Payload(
    @ProtoNumber(1) val msg: Security1MessageType,
    @ProtoNumber(20) val sessionCommand0: SessionCommand0? = null,
    @ProtoNumber(21) val sessionResponse0: SessionResponse0? = null,
    @ProtoNumber(22) val sessionCommand1: SessionCommand1? = null,
    @ProtoNumber(23) val sessionResponse1: SessionResponse1? = null
)

@Suppress("ArrayInDataClass")
@Serializable
internal data class SessionCommand0(@ProtoNumber(1) val clientPublicKey: ByteArray)

@Suppress("ArrayInDataClass")
@Serializable
internal data class SessionResponse0(
    @ProtoNumber(1) val status: Status = Status.SUCCESS,
    @ProtoNumber(2) val devicePublicKey: ByteArray,
    @ProtoNumber(3) val deviceRandom: ByteArray
)

@Suppress("ArrayInDataClass")
@Serializable
internal data class SessionCommand1(@ProtoNumber(2) val clientVerifyData: ByteArray)

@Suppress("ArrayInDataClass")
@Serializable
internal data class SessionResponse1(
    @ProtoNumber(1) val status: Status = Status.SUCCESS,
    @ProtoNumber(3) val deviceVerifyData: ByteArray
)
