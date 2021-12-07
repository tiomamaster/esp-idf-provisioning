package com.tiomamaster.espressif

typealias PrivateKey = ByteArray
typealias PublicKey = ByteArray

expect object X25519 {

    fun generateKeyPair(): Pair<PrivateKey, PublicKey>

    fun computeSharedSecret(privateKey: ByteArray, peersPublicValue: ByteArray): ByteArray
}
