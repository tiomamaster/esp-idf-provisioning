package com.tiomamaster.espressif.model

import com.tiomamaster.espressif.dto.WiFiAuthMode

data class WiFiNetwork(
    val ssid: String,
    val channel: Int,
    val rssi: Int,
    val bssid: String,
    val auth: WiFiAuthMode
)
