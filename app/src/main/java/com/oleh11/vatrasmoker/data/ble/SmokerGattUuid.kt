package com.oleh11.vatrasmoker.data.ble

import java.util.UUID

/**
 * Bluetooth GATT layout used by Cabela's / Camp Chef pellet grill controllers.
 * Matches the `com.campchef` SDK bundled in the official Cabela's app
 * (BleConfig + CampChefCharacteristic).
 *
 * - Every value lives in its own characteristic: `<brand prefix><wsId as 2 hex digits>`,
 *   inside the brand's primary service. Reads and writes go to those characteristics.
 * - Live updates arrive on ONE notify characteristic, the "multiplex" (`...aff`).
 *   Each notification is `[wsId][value bytes...]`.
 */
object SmokerGattUuid {

    enum class Brand(
        val displayName: String,
        val advertisedNamePrefix: String,
        val uuidPrefix: String,
        val service: UUID,
        val multiplex: UUID
    ) {
        CABELAS(
            displayName = "Cabela's",
            advertisedNamePrefix = "Cabelas:",
            uuidPrefix = "7dbaefb1-6bc3-4b8c-9990-3509fb398a",
            service = UUID.fromString("0000efb1-0000-1000-8000-00805F9B34FB"),
            multiplex = UUID.fromString("7dbaefb1-6bc3-4b8c-9990-3509fb398aff")
        ),
        CAMP_CHEF(
            displayName = "Camp Chef",
            advertisedNamePrefix = "CampChef:",
            uuidPrefix = "7dbaefb0-6bc3-4b8c-9990-3509fb398a",
            service = UUID.fromString("0000efb0-0000-1000-8000-00805F9B34FB"),
            multiplex = UUID.fromString("7dbaefb0-6bc3-4b8c-9990-3509fb398aff")
        ),
        KINGSFORD(
            displayName = "Kingsford",
            advertisedNamePrefix = "Kingsford:",
            uuidPrefix = "7dbaefb2-6bc3-4b8c-9990-3509fb398a",
            service = UUID.fromString("0000efb2-0000-1000-8000-00805F9B34FB"),
            multiplex = UUID.fromString("7dbaefb2-6bc3-4b8c-9990-3509fb398aff")
        );

        /** Characteristic UUID for a given wsId, e.g. 0x11 -> "...398a11". */
        fun characteristic(wsId: Byte): UUID =
            UUID.fromString(uuidPrefix + "%02x".format(wsId.toInt() and 0xFF))

        companion object {
            fun fromService(uuid: UUID): Brand? = entries.firstOrNull { it.service == uuid }
        }
    }

    // Kept for code that refers to the Cabela's values directly.
    const val CABELAS_UUID_PREFIX = "7dbaefb1-6bc3-4b8c-9990-3509fb398a"
    const val CAMPCHEF_UUID_PREFIX = "7dbaefb0-6bc3-4b8c-9990-3509fb398a"
    val SERVICE_CABELAS: UUID = Brand.CABELAS.service
    val SERVICE_CABELAS_MULTIPLEX: UUID = Brand.CABELAS.multiplex
    val SERVICE_CAMPCHEF: UUID = Brand.CAMP_CHEF.service
    val SERVICE_CAMPCHEF_MULTIPLEX: UUID = Brand.CAMP_CHEF.multiplex
    val SERVICE_ESP_CONFIG: UUID = UUID.fromString("0d563a58-196a-48ce-ace2-dfec78acc814")

    // === Characteristic wsIds (from CampChefCharacteristic) ===
    const val WS_ID_MODE: Byte = 0x01
    const val WS_ID_GRILL_MODEL: Byte = 0x02
    const val WS_ID_TRANSITIONING: Byte = 0x03
    const val WS_ID_PELLET_LEVEL: Byte = 0x04
    const val WS_ID_LAST_FAULT: Byte = 0x05
    const val WS_ID_CHAMBER_TEMP: Byte = 0x06
    const val WS_ID_CLIENT_SECRET: Byte = 0x08
    const val WS_ID_WEB_ID: Byte = 0x09
    /** Meat probes are 0x10 + index (index 0..15). Probe 1 = 0x10, probe 2 = 0x11, ... */
    const val WS_ID_PROBE_BASE: Byte = 0x10
    const val WS_ID_WIFI_SSID: Byte = 0x20
    const val WS_ID_WIFI_PASSWORD: Byte = 0x21
    const val WS_ID_WIFI_STATUS: Byte = 0x22
    const val WS_ID_WIFI_SCANNING: Byte = 0x23
    const val WS_ID_WIFI_RESULTS: Byte = 0x24
    const val WS_ID_WIFI_RSSI: Byte = 0x25
    const val WS_ID_HTTP_URL: Byte = 0x26
    const val WS_ID_WS_URL: Byte = 0x27
    const val WS_ID_STM_FIRMWARE: Byte = 0x30
    const val WS_ID_ESP_FIRMWARE: Byte = 0x31
    const val WS_ID_OTA_PROGRESS: Byte = 0x34
    const val WS_ID_MULTIPLEX: Byte = 0xFF.toByte()

    /** Number of probes this app shows. */
    const val PROBE_COUNT = 4

    /** wsId for probe number 1..16. */
    fun probeWsId(probeNumber: Int): Byte {
        require(probeNumber in 1..16) { "Illegal probe number $probeNumber" }
        return (WS_ID_PROBE_BASE + (probeNumber - 1)).toByte()
    }

    /** Probe number 1..16 for a wsId, or null if it is not a probe. */
    fun probeNumberFor(wsId: Byte): Int? {
        val id = wsId.toInt() and 0xFF
        return if (id in 0x10..0x1F) id - 0x10 + 1 else null
    }

    fun getCabelasUuid(wsId: Byte): UUID = Brand.CABELAS.characteristic(wsId)

    val CHAR_MODE: UUID = getCabelasUuid(WS_ID_MODE)
    val CHAR_CHAMBER_TEMP: UUID = getCabelasUuid(WS_ID_CHAMBER_TEMP)
    val CHAR_PROBE1: UUID = getCabelasUuid(probeWsId(1))
    val CHAR_PROBE2: UUID = getCabelasUuid(probeWsId(2))
    val CHAR_PELLET_LEVEL: UUID = getCabelasUuid(WS_ID_PELLET_LEVEL)
    val CHAR_LAST_FAULT: UUID = getCabelasUuid(WS_ID_LAST_FAULT)
    val CHAR_TRANSITIONING: UUID = getCabelasUuid(WS_ID_TRANSITIONING)
    val CHAR_WIFI_SSID: UUID = getCabelasUuid(WS_ID_WIFI_SSID)
    val CHAR_WIFI_PASSWORD: UUID = getCabelasUuid(WS_ID_WIFI_PASSWORD)
    val CHAR_WIFI_STATUS: UUID = getCabelasUuid(WS_ID_WIFI_STATUS)
    val CHAR_MULTIPLEX: UUID = SERVICE_CABELAS_MULTIPLEX

    // Standard BLE Descriptor for Notifications (CCCD)
    val CCCD_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val MATCHING_DEVICE_PREFIXES = listOf(
        "Cabelas",
        "Cabela",
        "CampChef",
        "Camp Chef",
        "Kingsford",
        "ProSeries",
        "Grill",
        "SmokeMaster",
        "PID-"
    )
}
