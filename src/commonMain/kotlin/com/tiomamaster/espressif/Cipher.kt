package com.tiomamaster.espressif

expect class Cipher(deviceRandom: ByteArray, sharedKey: ByteArray) {

    fun encrypt(data: ByteArray): ByteArray

    fun decrypt(data: ByteArray): ByteArray
}
