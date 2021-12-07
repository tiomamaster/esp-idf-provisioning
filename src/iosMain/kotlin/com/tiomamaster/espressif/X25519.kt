package com.tiomamaster.espressif

actual object X25519 {

    actual fun generateKeyPair(): Pair<PrivateKey, PublicKey> {
        TODO("Not yet implemented")
    }

    actual fun computeSharedSecret(
        privateKey: ByteArray,
        peersPublicValue: ByteArray
    ): ByteArray {
        TODO("Not yet implemented")
    }
}
