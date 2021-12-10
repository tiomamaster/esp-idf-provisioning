package com.tiomamaster.espressif.security

actual class Cipher actual constructor(deviceRandom: ByteArray, sharedKey: ByteArray) {

    actual fun encrypt(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun decrypt(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}