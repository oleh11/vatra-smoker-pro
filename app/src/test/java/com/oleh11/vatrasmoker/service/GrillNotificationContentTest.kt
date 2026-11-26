package com.oleh11.vatrasmoker.service

import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GrillNotificationContentTest {
    private val connected = ConnectionState.Connected("Grill", "00:11")
    private val running = SmokerState(
        powerMode = SmokerPowerMode.RUNNING, currentChamberTempF = 218, targetChamberTempF = 225, smokeLevel = 7
    )

    @Test
    fun showsGrillTempSetTempAndSmoke() {
        val content = GrillNotificationContent.from(connected, running)!!
        assertEquals("Grill 218°F · Set 225°F", content.title)
        assertEquals("Smoke level 7", content.text)
    }

    @Test
    fun usesTheChosenUnit() {
        val content = GrillNotificationContent.from(connected, running.copy(temperatureUnit = TemperatureUnit.CELSIUS))!!
        assertEquals("Grill 103°C · Set 107°C", content.title)
    }

    @Test
    fun hiddenWhenGrillIsOffOrNotConnected() {
        assertNull(GrillNotificationContent.from(connected, running.copy(powerMode = SmokerPowerMode.OFF)))
        assertNull(GrillNotificationContent.from(ConnectionState.Disconnected, running))
        assertNull(GrillNotificationContent.from(ConnectionState.Error("lost"), running))
    }

    @Test
    fun staysWhileReconnecting() {
        val content = GrillNotificationContent.from(ConnectionState.Connecting("Grill", "00:11"), running)!!
        assertEquals("Smoke level 7 · Reconnecting, last reading", content.text)
    }
}
