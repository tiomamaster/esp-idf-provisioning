@file:Suppress("IllegalIdentifier")

package com.tiomamaster.espressif

import com.tiomamaster.espressif.model.BleDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class BleDeviceTest {

    @Test
    fun `equals test`() {
        val bleDevice1 = BleDevice("105918_DC56", "10:52:1C:80:DC:56", -50, 25)
        val bleDevice2 = BleDevice("105913_DC54", "10:52:1C:80:DC:56", -10, 125)
        assertEquals(bleDevice1, bleDevice2)
        assertEquals(bleDevice1.hashCode(), bleDevice2.hashCode())
        val bleDevice3 = BleDevice("104859_1D48", "FC:F5:C4:3A:1D:4A", -20, 0)
        assertNotEquals(bleDevice1, bleDevice3)
        assertFalse(bleDevice1.equals(null))
    }
}
