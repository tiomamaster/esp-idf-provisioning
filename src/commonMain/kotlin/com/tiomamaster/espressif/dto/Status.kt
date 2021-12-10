package com.tiomamaster.espressif.dto

enum class Status(val value: Int) {
    SUCCESS(0),
    INVALID_SECURITY_SCHEME(1),
    INVALID_PROTO(2),
    TOO_MANY_SESSIONS(3),
    INVALID_ARGUMENT(4),
    INTERNAL_ERROR(5),
    CRYPTO_ERROR(6),
    INVALID_SESSION(7)
}
