package com.oleh11.vatrasmoker.data.wifi

/** One access point as the phone heard it. */
data class AccessPoint(val ssid: String, val rssi: Int, val frequencyMhz: Int, val capabilities: String)

/** A network the user can pick for the grill. */
data class WifiNetwork(val ssid: String, val rssi: Int, val secured: Boolean)

object WifiNetworks {
    /**
     * Networks the grill can join: 2.4 GHz, named, not enterprise login.
     * One entry per name, strongest first.
     */
    fun forGrill(accessPoints: List<AccessPoint>): List<WifiNetwork> = accessPoints
        .filter { it.ssid.isNotBlank() && it.frequencyMhz in 2400..2500 && "EAP" !in it.capabilities }
        .groupBy { it.ssid }
        .map { (ssid, points) ->
            val best = points.maxByOrNull { it.rssi }!!
            WifiNetwork(ssid, best.rssi, listOf("WEP", "PSK", "SAE").any { it in best.capabilities })
        }
        .sortedByDescending { it.rssi }
}
