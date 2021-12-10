package com.tiomamaster.espressif.security

import com.google.crypto.tink.subtle.X25519

actual object X25519 {

    actual fun generateKeyPair(): Pair<PrivateKey, PublicKey> {
        val privateKey = X25519.generatePrivateKey()
        val publicKey = X25519.publicFromPrivate(privateKey)
        return privateKey to publicKey
    }

    actual fun computeSharedSecret(
        privateKey: ByteArray,
        peersPublicValue: ByteArray
    ): ByteArray = X25519.computeSharedSecret(privateKey, peersPublicValue)
}