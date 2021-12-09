package com.tiomamaster.espressif.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

enum class SecuritySchemeVersion(val value: Int) {
    SECURITY_SCHEME_0(0), /* Unsecured - plaintext communication */
    SECURITY_SCHEME_1(1)  /* Security scheme 1 - Curve25519 + AES-256-CTR*/
}

@Serializable
data class SessionData(
    @ProtoNumber(2) val securityVersion: SecuritySchemeVersion,
    @ProtoNumber(11) val security1payload: Security1Payload
)
