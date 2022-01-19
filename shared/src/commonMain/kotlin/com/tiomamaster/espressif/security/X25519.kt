package com.tiomamaster.espressif.security

typealias PrivateKey = ByteArray
typealias PublicKey = ByteArray

internal expect object X25519 {

    fun generateKeyPair(): Pair<PrivateKey, PublicKey>

    fun computeSharedSecret(privateKey: ByteArray, peersPublicValue: ByteArray): ByteArray
}
