package com.oleh11.vatrasmoker.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiNetworksTest {
    @Test
    fun listsOnlyWhatTheGrillCanJoinStrongestFirst() {
        val result = WifiNetworks.forGrill(
            listOf(
                AccessPoint("Home", -70, 2412, "[WPA2-PSK-CCMP]"),
                AccessPoint("Home", -50, 2437, "[WPA2-PSK-CCMP]"),
                AccessPoint("Cafe", -40, 2462, "[ESS]"),
                AccessPoint("FiveOnly", -30, 5180, "[WPA2-PSK-CCMP]"),
                AccessPoint("Office", -30, 2412, "[WPA2-EAP-CCMP]"),
                AccessPoint("", -30, 2412, "[ESS]")
            )
        )
        assertEquals(listOf(WifiNetwork("Cafe", -40, false), WifiNetwork("Home", -50, true)), result)
    }
}
