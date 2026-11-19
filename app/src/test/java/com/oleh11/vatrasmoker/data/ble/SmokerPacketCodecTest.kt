package com.oleh11.vatrasmoker.data.ble

import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmokerPacketCodecTest {

    @Test
    fun testEncodeAndDecodeStatusPacket() {
        val initial = SmokerState(
            powerMode = SmokerPowerMode.RUNNING,
            currentChamberTempF = 225,
            targetChamberTempF = 250,
            smokeLevel = 8,
            isAugerActive = true,
            isFanActive = true
        )

        val packet = SmokerPacketCodec.encodeMockStatusPacket(initial)
        assertNotNull(packet)
        assertTrue(packet.isNotEmpty())

        val decoded = SmokerPacketCodec.decodeStatusPacket(packet, SmokerState())
        assertNotNull(decoded)
        assertEquals(SmokerPowerMode.RUNNING, decoded?.powerMode)
        assertEquals(225, decoded?.currentChamberTempF)
        assertEquals(250, decoded?.targetChamberTempF)
        assertEquals(8, decoded?.smokeLevel)
        assertTrue(decoded?.isAugerActive == true)
        assertTrue(decoded?.isFanActive == true)
    }

    @Test
    fun testOfficialCabelasRunModePacket() {
        val packet = SmokerPacketCodec.buildCabelasRunModePacket(225, 7)
        assertEquals(5, packet.size)
        assertEquals(0x11.toByte(), packet[0]) // RUN wsId from official SDK
        val temp = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
        assertEquals(225, temp)
        assertEquals(7.toByte(), packet[3])
        assertEquals(0x00.toByte(), packet[4])

        // Parse back
        val state = SmokerPacketCodec.parseModePacket(packet, SmokerState())
        assertEquals(SmokerPowerMode.RUNNING, state.powerMode)
        assertEquals(225, state.targetChamberTempF)
        assertEquals(7, state.smokeLevel)
    }

    @Test
    fun testOfficialCabelasShutdownPacket() {
        val packet = SmokerPacketCodec.buildCabelasShutdownPacket()
        assertEquals(1, packet.size)
        assertEquals(SmokerPacketCodec.MODE_STANDBY, packet[0]) // 0x00

        val state = SmokerPacketCodec.parseModePacket(packet, SmokerState(powerMode = SmokerPowerMode.RUNNING))
        assertEquals(SmokerPowerMode.OFF, state.powerMode)
    }

    @Test
    fun testOfficialCabelasChamberAndProbeTempParsing() {
        val chamberBytes = SmokerPacketCodec.buildShortPacket(275.toShort())
        val chamberState = SmokerPacketCodec.parseChamberTempPacket(chamberBytes, SmokerState())
        assertEquals(275, chamberState.currentChamberTempF)

        val probe1Bytes = SmokerPacketCodec.buildShortPacket(145.toShort())
        val probe1State = SmokerPacketCodec.parseProbeTempPacket(1, probe1Bytes, SmokerState())
        assertTrue(probe1State.probe1.isConnected)
        assertEquals(145, probe1State.probe1.currentTempF)

        val probe2Bytes = SmokerPacketCodec.buildShortPacket(165.toShort())
        val probe2State = SmokerPacketCodec.parseProbeTempPacket(2, probe2Bytes, SmokerState())
        assertTrue(probe2State.probe2.isConnected)
        assertEquals(165, probe2State.probe2.currentTempF)
    }

    @Test
    fun testOfficialCabelasPelletAndFaultParsing() {
        val pelletBytes = byteArrayOf(85.toByte())
        val pelletState = SmokerPacketCodec.parsePelletLevelPacket(pelletBytes, SmokerState())
        assertEquals(85, pelletState.hopper.levelPercentage)

        val faultBytes = byteArrayOf(0x01, SmokerPacketCodec.ERROR_FLAME_OUT)
        val faultState = SmokerPacketCodec.parseLastFaultPacket(faultBytes, SmokerState())
        assertEquals("Flame Out", faultState.errorCode)

        // 101 means "no reading" and must not overwrite the last known level
        val unknown = SmokerPacketCodec.parsePelletLevelPacket(byteArrayOf(101), pelletState)
        assertEquals(85, unknown.hopper.levelPercentage)
    }

    @Test
    fun testOfficialModeIds() {
        assertEquals(0x00.toByte(), SmokerPacketCodec.MODE_STANDBY)
        assertEquals(0x01.toByte(), SmokerPacketCodec.MODE_FAN)
        assertEquals(0x02.toByte(), SmokerPacketCodec.MODE_FEED)
        assertEquals(0x10.toByte(), SmokerPacketCodec.MODE_HIGH)
        assertEquals(0x11.toByte(), SmokerPacketCodec.MODE_RUN)
        assertEquals(0x12.toByte(), SmokerPacketCodec.MODE_COOK_PROGRAM)
        assertEquals(0x7F.toByte(), SmokerPacketCodec.MODE_DEBUG)
    }

    @Test
    fun testCharacteristicUuidsMatchOfficialApp() {
        val cabelas = SmokerGattUuid.Brand.CABELAS
        assertEquals("7dbaefb1-6bc3-4b8c-9990-3509fb398a01", cabelas.characteristic(SmokerGattUuid.WS_ID_MODE).toString())
        assertEquals("7dbaefb1-6bc3-4b8c-9990-3509fb398a10", cabelas.characteristic(SmokerGattUuid.probeWsId(1)).toString())
        assertEquals("7dbaefb1-6bc3-4b8c-9990-3509fb398a13", cabelas.characteristic(SmokerGattUuid.probeWsId(4)).toString())
        assertEquals("7dbaefb1-6bc3-4b8c-9990-3509fb398aff", cabelas.multiplex.toString())
        assertEquals(cabelas, SmokerGattUuid.Brand.fromService(java.util.UUID.fromString("0000efb1-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun testMultiplexNotificationsUpdateState() {
        var state = SmokerState()
        // Chamber 0x06 = 231°F
        state = SmokerPacketCodec.parseMultiplexNotification(byteArrayOf(0x06, 0x00, 0xE7.toByte()), state)
        assertEquals(231, state.currentChamberTempF)
        // Probe 2 (wsId 0x11) = 150°F
        state = SmokerPacketCodec.parseMultiplexNotification(byteArrayOf(0x11, 0x00, 0x96.toByte()), state)
        assertTrue(state.probe2.isConnected)
        assertEquals(150, state.probe2.currentTempF)
        // Mode 0x01 carrying RUN at 250°F smoke 6
        state = SmokerPacketCodec.parseMultiplexNotification(byteArrayOf(0x01, 0x11, 0x00, 0xFA.toByte(), 0x06, 0x00), state)
        assertEquals(SmokerPowerMode.RUNNING, state.powerMode)
        assertEquals(250, state.targetChamberTempF)
        assertEquals(6, state.smokeLevel)
        // Mode STANDBY
        state = SmokerPacketCodec.parseMultiplexNotification(byteArrayOf(0x01, 0x00), state)
        assertEquals(SmokerPowerMode.OFF, state.powerMode)
    }

    @Test
    fun testLongStringsAreChunkedLikeOfficialApp() {
        assertEquals(1, SmokerPacketCodec.buildChunkedStringWrites("ShortSSID").size)

        val long = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcd" // 40 chars
        val chunks = SmokerPacketCodec.buildChunkedStringWrites(long)
        chunks.forEach { assertTrue(it.size <= 20) }
        assertEquals(SmokerPacketCodec.CONTINUATION_BYTE, chunks.first().last())
        assertEquals(SmokerPacketCodec.CONTINUATION_BYTE, chunks.last().first())
        val rebuilt = chunks.joinToString("") { c ->
            c.filter { it != SmokerPacketCodec.CONTINUATION_BYTE }.toByteArray().toString(Charsets.UTF_8)
        }
        assertEquals(long, rebuilt)
    }
}
