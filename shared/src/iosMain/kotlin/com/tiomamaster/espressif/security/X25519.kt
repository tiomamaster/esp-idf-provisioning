package com.tiomamaster.espressif.security

internal actual object X25519 {

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
