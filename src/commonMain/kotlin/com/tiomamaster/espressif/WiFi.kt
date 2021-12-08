package com.tiomamaster.espressif

data class WiFi(
    val ssid: String,
    val channel: Int,
    val rssi: Int,
    val bssid: String,
    val auth: WiFiAuthMode
)
