package com.tiomamaster.espressif.security

internal expect class Cipher(deviceRandom: ByteArray, sharedKey: ByteArray) {

    fun encrypt(data: ByteArray): ByteArray

    fun decrypt(data: ByteArray): ByteArray
}
