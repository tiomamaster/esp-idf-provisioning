package com.tiomamaster.espressif

data class BleDevice(val name: String?, val mac: String?, val rssi: Int, val txPower: Int?) {

    override fun equals(other: Any?): Boolean = other is BleDevice && other.mac == mac

    override fun hashCode(): Int = mac?.hashCode() ?: 0
}
