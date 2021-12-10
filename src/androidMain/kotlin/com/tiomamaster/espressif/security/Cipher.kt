package com.tiomamaster.espressif.security

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class Cipher actual constructor(deviceRandom: ByteArray, sharedKey: ByteArray) {

    private val cipher = Cipher.getInstance("AES/CTR/NoPadding").apply {
        init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(sharedKey, 0, sharedKey.size, "AES"),
            IvParameterSpec(deviceRandom)
        )
    }

    actual fun encrypt(data: ByteArray): ByteArray = cipher.update(data)

    actual fun decrypt(data: ByteArray): ByteArray = cipher.update(data)
}