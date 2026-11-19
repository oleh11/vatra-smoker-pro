package com.oleh11.vatrasmoker.data.ble

import com.oleh11.vatrasmoker.data.model.MeatProbe
import com.oleh11.vatrasmoker.data.model.PelletHopperState
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import java.nio.ByteBuffer

/**
 * Byte formats for Cabela's / Camp Chef grill characteristics.
 * Matches GrillModeType, CampChefCharacteristic and ByteArray_ext in the
 * `com.campchef` SDK bundled with the official Cabela's app. All multi-byte
 * numbers are big-endian (java.nio default).
 */
object SmokerPacketCodec {

    // === Grill mode IDs (GrillModeType.wsId) ===
    const val MODE_STANDBY: Byte = 0x00      // Off / standby
    const val MODE_FAN: Byte = 0x01          // Fan only (cool-down). Payload: [fanLevel]
    const val MODE_FEED: Byte = 0x02         // Prime auger
    const val MODE_RECORD: Byte = 0x03       // Session record
    const val MODE_HIGH: Byte = 0x10         // High / sear
    const val MODE_RUN: Byte = 0x11          // Cook at set temp. Payload: [temp hi][temp lo][smoke]
    const val MODE_COOK_PROGRAM: Byte = 0x12 // Multi-stage cook program
    const val MODE_DEBUG: Byte = 0x7F        // Diagnostics

    // === Error codes (ErrorCode.code) ===
    const val ERROR_NONE: Byte = 0x00
    const val ERROR_SENSOR: Byte = 0x01
    const val ERROR_FLAME_OUT: Byte = 0x02
    const val ERROR_OVER_TEMP: Byte = 0x03

    /** Pellet level the grill reports when it has no hopper sensor reading. */
    const val PELLET_LEVEL_UNKNOWN = 101

    /** Marker byte the official app uses to split long strings across 20-byte writes. */
    const val CONTINUATION_BYTE: Byte = 0x1F

    // Header for the old simulator-only status frame (not used by real grills)
    private const val HEADER: Byte = 0xA5.toByte()

    // ---------------------------------------------------------------------
    // Commands (written to the MODE characteristic, wsId 0x01)
    // ---------------------------------------------------------------------

    /** RUN mode (5 bytes): [0x11][temp hi][temp lo][smoke][0x00]. */
    fun buildCabelasRunModePacket(targetTempF: Int, smokeLevel: Int): ByteArray {
        val buffer = ByteBuffer.allocate(5)
        buffer.put(MODE_RUN)
        buffer.putShort(targetTempF.coerceIn(150, 500).toShort())
        buffer.put(smokeLevel.coerceIn(1, 10).toByte())
        return buffer.array()
    }

    /** STANDBY (1 byte): turns the grill off. */
    fun buildCabelasShutdownPacket(): ByteArray = byteArrayOf(MODE_STANDBY)

    /** HIGH (1 byte). */
    fun buildCabelasHighTempPacket(): ByteArray = byteArrayOf(MODE_HIGH)

    /** FEED (1 byte): prime the auger. */
    fun buildCabelasFeedPacket(): ByteArray = byteArrayOf(MODE_FEED)

    /** FAN (2 bytes): [0x01][fanLevel]. */
    fun buildCabelasFanPacket(fanLevel: Int): ByteArray =
        byteArrayOf(MODE_FAN, fanLevel.coerceIn(0, 255).toByte())

    fun buildShortPacket(value: Short): ByteArray = ByteBuffer.allocate(2).putShort(value).array()

    fun buildBooleanPacket(value: Boolean): ByteArray = byteArrayOf(if (value) 0x01 else 0x00)

    fun parseShort(bytes: ByteArray): Short {
        if (bytes.size < 2) return 0
        return ByteBuffer.wrap(bytes).short
    }

    // ---------------------------------------------------------------------
    // Incoming values
    // ---------------------------------------------------------------------

    /**
     * Parses one multiplex notification: [wsId][value bytes...].
     * Returns the updated state, or the same state if the wsId is not one we show.
     */
    fun parseMultiplexNotification(bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.isEmpty()) return currentState
        return parseCharacteristicValue(bytes[0], bytes.copyOfRange(1, bytes.size), currentState)
    }

    /** Parses a value read from (or notified for) the characteristic with this wsId. */
    fun parseCharacteristicValue(wsId: Byte, value: ByteArray, currentState: SmokerState): SmokerState {
        SmokerGattUuid.probeNumberFor(wsId)?.let { probe ->
            return parseProbeTempPacket(probe, value, currentState)
        }
        return when (wsId) {
            SmokerGattUuid.WS_ID_MODE -> parseModePacket(value, currentState)
            SmokerGattUuid.WS_ID_CHAMBER_TEMP -> parseChamberTempPacket(value, currentState)
            SmokerGattUuid.WS_ID_PELLET_LEVEL -> parsePelletLevelPacket(value, currentState)
            SmokerGattUuid.WS_ID_LAST_FAULT -> parseLastFaultPacket(value, currentState)
            SmokerGattUuid.WS_ID_TRANSITIONING -> parseTransitioningPacket(value, currentState)
            SmokerGattUuid.WS_ID_WIFI_SSID -> currentState.copy(
                wifiSsid = value.toString(Charsets.UTF_8).trimEnd('\u0000').ifBlank { null }
            )
            else -> currentState
        }
    }

    /** Parses the MODE characteristic (GrillMode). */
    fun parseModePacket(bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.isEmpty()) return currentState
        val buffer = ByteBuffer.wrap(bytes)

        return when (bytes[0]) {
            MODE_STANDBY -> currentState.copy(
                powerMode = SmokerPowerMode.OFF,
                isAugerActive = false,
                isFanActive = false,
                isIgniterActive = false
            )
            MODE_RUN -> {
                if (bytes.size >= 4) {
                    val temp = buffer.getShort(1).toInt()
                    val smoke = buffer.get(3).toInt() and 0xFF
                    currentState.copy(
                        powerMode = SmokerPowerMode.RUNNING,
                        targetChamberTempF = if (temp in 150..500) temp else currentState.targetChamberTempF,
                        smokeLevel = if (smoke in 1..10) smoke else currentState.smokeLevel,
                        isFanActive = true
                    )
                } else {
                    currentState.copy(powerMode = SmokerPowerMode.RUNNING)
                }
            }
            MODE_HIGH -> currentState.copy(
                powerMode = SmokerPowerMode.RUNNING,
                targetChamberTempF = 500,
                isFanActive = true
            )
            MODE_COOK_PROGRAM -> currentState.copy(powerMode = SmokerPowerMode.RUNNING, isFanActive = true)
            MODE_FEED -> currentState.copy(isAugerActive = true)
            MODE_FAN -> currentState.copy(
                powerMode = SmokerPowerMode.SHUTDOWN,
                isFanActive = true,
                isAugerActive = false
            )
            else -> currentState
        }
    }

    /** TRANSITIONING (1 byte boolean): grill is igniting / changing mode. */
    fun parseTransitioningPacket(bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.isEmpty()) return currentState
        val transitioning = bytes[0].toInt() != 0
        return when {
            transitioning && currentState.powerMode == SmokerPowerMode.RUNNING ->
                currentState.copy(powerMode = SmokerPowerMode.STARTUP, isIgniterActive = true)
            !transitioning && currentState.powerMode == SmokerPowerMode.STARTUP ->
                currentState.copy(powerMode = SmokerPowerMode.RUNNING, isIgniterActive = false)
            else -> currentState.copy(isIgniterActive = transitioning)
        }
    }

    /** CHAMBER (2-byte signed short, °F). */
    fun parseChamberTempPacket(bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.size < 2) return currentState
        val temp = parseShort(bytes).toInt()
        return if (temp in -40..700) currentState.copy(currentChamberTempF = temp) else currentState
    }

    /** PROBE n (2-byte signed short, °F). Out-of-range values mean the probe is unplugged. */
    fun parseProbeTempPacket(probeNumber: Int, bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.size < 2) return currentState
        val temp = parseShort(bytes).toInt()
        val isPluggedIn = temp in 1..700

        fun update(p: MeatProbe) = p.copy(
            isConnected = isPluggedIn,
            currentTempF = if (isPluggedIn) temp else 0,
            isTargetReached = isPluggedIn && p.targetTempF > 0 && temp >= p.targetTempF
        )

        return when (probeNumber) {
            1 -> currentState.copy(probe1 = update(currentState.probe1))
            2 -> currentState.copy(probe2 = update(currentState.probe2))
            3 -> currentState.copy(probe3 = update(currentState.probe3))
            4 -> currentState.copy(probe4 = update(currentState.probe4))
            else -> currentState
        }
    }

    /** PELLET LEVEL (1 byte, 0..100 %, 101 = unknown). */
    fun parsePelletLevelPacket(bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.isEmpty()) return currentState
        val raw = bytes[0].toInt() and 0xFF
        if (raw > 100) return currentState // no reading
        return currentState.copy(
            hopper = PelletHopperState(levelPercentage = raw, isLowPelletAlert = raw < 20)
        )
    }

    /**
     * LAST FAULT (2 bytes): [faultId][errorCode]. This is the most recent fault the
     * controller logged, so it sets [SmokerState.errorCode] without forcing the power mode.
     */
    fun parseLastFaultPacket(bytes: ByteArray, currentState: SmokerState): SmokerState {
        if (bytes.size < 2) return currentState
        val label = when (bytes[1]) {
            ERROR_NONE -> null
            ERROR_SENSOR -> "Sensor Error"
            ERROR_FLAME_OUT -> "Flame Out"
            ERROR_OVER_TEMP -> "Over Temp"
            else -> "Fault code ${bytes[1].toInt() and 0xFF}"
        }
        return currentState.copy(errorCode = label)
    }

    /** Plain UTF-8 string value. */
    fun buildWifiStringPacket(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)

    /**
     * Splits a string into the writes the official app sends (Ble.write in the SDK):
     * up to 18 chars goes as one plain write; longer strings go as 20-byte chunks
     * framed with [CONTINUATION_BYTE].
     */
    fun buildChunkedStringWrites(text: String): List<ByteArray> {
        if (text.length <= 18) return listOf(text.toByteArray(Charsets.UTF_8))

        val writes = mutableListOf<ByteArray>()
        writes += text.substring(0, 19).toByteArray(Charsets.UTF_8) + CONTINUATION_BYTE
        var rest = text.substring(19)
        while (rest.length > 19) {
            writes += byteArrayOf(CONTINUATION_BYTE) +
                rest.substring(0, 18).toByteArray(Charsets.UTF_8) +
                CONTINUATION_BYTE
            rest = rest.substring(18)
        }
        writes += byteArrayOf(CONTINUATION_BYTE) + rest.toByteArray(Charsets.UTF_8)
        return writes
    }

    // =========================================================================
    // Simulator-only status frame (the real grill never sends this)
    // =========================================================================

    fun buildSetTemperaturePacket(targetTempF: Int): ByteArray = buildCabelasRunModePacket(targetTempF, 5)

    fun buildSetSmokeLevelPacket(level: Int): ByteArray = buildCabelasRunModePacket(225, level)

    fun buildStartupPacket(): ByteArray = buildCabelasRunModePacket(225, 5)

    fun buildShutdownPacket(): ByteArray = buildCabelasShutdownPacket()

    fun decodeStatusPacket(bytes: ByteArray, currentState: SmokerState): SmokerState? {
        if (bytes.size < 18 || bytes[0] != HEADER) {
            return null
        }
        var sum = 0
        for (i in 0 until bytes.size - 1) {
            sum += (bytes[i].toInt() and 0xFF)
        }
        if (bytes.last() != (sum and 0xFF).toByte()) {
            return null
        }

        val powerMode = when (bytes[2].toInt() and 0xFF) {
            0 -> SmokerPowerMode.OFF
            1 -> SmokerPowerMode.STARTUP
            2 -> SmokerPowerMode.RUNNING
            3 -> SmokerPowerMode.SHUTDOWN
            else -> SmokerPowerMode.ERROR
        }

        fun u16(i: Int) = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
        fun probe(p: MeatProbe, t: Int) = p.copy(
            isConnected = t in 32..500,
            currentTempF = if (t in 32..500) t else 0,
            isTargetReached = p.targetTempF > 0 && t >= p.targetTempF
        )

        val pelletPct = (bytes[16].toInt() and 0xFF).coerceIn(0, 100)
        val flags = bytes[17].toInt() and 0xFF

        return currentState.copy(
            powerMode = powerMode,
            currentChamberTempF = u16(3),
            targetChamberTempF = u16(5),
            smokeLevel = (bytes[7].toInt() and 0xFF).coerceIn(1, 10),
            probe1 = probe(currentState.probe1, u16(8)),
            probe2 = probe(currentState.probe2, u16(10)),
            probe3 = probe(currentState.probe3, u16(12)),
            probe4 = probe(currentState.probe4, u16(14)),
            hopper = PelletHopperState(levelPercentage = pelletPct, isLowPelletAlert = pelletPct < 20),
            isAugerActive = (flags and 0x01) != 0,
            isFanActive = (flags and 0x02) != 0,
            isIgniterActive = (flags and 0x04) != 0
        )
    }

    fun encodeMockStatusPacket(state: SmokerState): ByteArray {
        val modeByte = when (state.powerMode) {
            SmokerPowerMode.OFF -> 0
            SmokerPowerMode.STARTUP -> 1
            SmokerPowerMode.RUNNING -> 2
            SmokerPowerMode.SHUTDOWN -> 3
            SmokerPowerMode.ERROR -> 4
        }.toByte()

        var flags = 0
        if (state.isAugerActive) flags = flags or 0x01
        if (state.isFanActive) flags = flags or 0x02
        if (state.isIgniterActive) flags = flags or 0x04

        fun hi(v: Int) = ((v shr 8) and 0xFF).toByte()
        fun lo(v: Int) = (v and 0xFF).toByte()

        val payload = byteArrayOf(
            HEADER,
            18.toByte(),
            modeByte,
            hi(state.currentChamberTempF), lo(state.currentChamberTempF),
            hi(state.targetChamberTempF), lo(state.targetChamberTempF),
            state.smokeLevel.toByte(),
            hi(state.probe1.currentTempF), lo(state.probe1.currentTempF),
            hi(state.probe2.currentTempF), lo(state.probe2.currentTempF),
            hi(state.probe3.currentTempF), lo(state.probe3.currentTempF),
            hi(state.probe4.currentTempF), lo(state.probe4.currentTempF),
            state.hopper.levelPercentage.toByte(),
            flags.toByte()
        )
        var sum = 0
        for (b in payload) sum += (b.toInt() and 0xFF)
        return payload + (sum and 0xFF).toByte()
    }
}
