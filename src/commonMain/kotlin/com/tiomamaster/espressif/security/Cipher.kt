package com.tiomamaster.espressif.security

expect class Cipher(deviceRandom: ByteArray, sharedKey: ByteArray) {

    fun encrypt(data: ByteArray): ByteArray

    fun decrypt(data: ByteArray): ByteArray
}
