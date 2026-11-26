package com.oleh11.vatrasmoker.data.ble

import com.oleh11.vatrasmoker.data.model.ChangeMessage
import com.oleh11.vatrasmoker.data.model.CommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandConfirmationTest {
    private val run = SmokerPacketCodec.buildCabelasRunModePacket(250, 7)

    @Test
    fun confirmsOnlyTheValueTheGrillReports() {
        assertTrue(SmokerPacketCodec.modeHasTargetTemp(run, 250))
        assertFalse(SmokerPacketCodec.modeHasTargetTemp(run, 225))
        assertTrue(SmokerPacketCodec.modeHasSmokeLevel(run, 7))
        assertFalse(SmokerPacketCodec.modeHasSmokeLevel(run, 5))
        assertFalse(SmokerPacketCodec.modeHasTargetTemp(byteArrayOf(0), 250))
    }

    @Test
    fun confirmsPowerChanges() {
        assertTrue(SmokerPacketCodec.modeIsRunning(run))
        assertFalse(SmokerPacketCodec.modeIsShuttingDown(run))
        assertTrue(SmokerPacketCodec.modeIsShuttingDown(byteArrayOf(SmokerPacketCodec.MODE_STANDBY)))
        assertTrue(SmokerPacketCodec.modeIsShuttingDown(byteArrayOf(SmokerPacketCodec.MODE_FAN, 5)))
    }

    @Test
    fun neverClaimsSuccessForAFailure() {
        assertEquals(
            "Not changed: smoke level 7. The grill did not receive it.",
            ChangeMessage.of(CommandResult.FAILED, "smoke level 7")
        )
        assertFalse(ChangeMessage.of(CommandResult.UNCONFIRMED, "x").contains("Grill confirmed"))
    }
}
