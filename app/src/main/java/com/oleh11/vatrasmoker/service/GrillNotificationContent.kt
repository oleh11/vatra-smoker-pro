package com.oleh11.vatrasmoker.service

import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState

/** Text of the ongoing grill status notification. */
data class GrillNotificationContent(val title: String, val text: String) {

    companion object {
        /** Returns null when there should be no notification: no grill link, or the grill is off. */
        fun from(connection: ConnectionState, state: SmokerState): GrillNotificationContent? {
            val reconnecting = when (connection) {
                is ConnectionState.Connected, is ConnectionState.SimulatorActive -> false
                is ConnectionState.Connecting, is ConnectionState.Authenticating -> true
                else -> return null
            }
            if (state.powerMode == SmokerPowerMode.OFF) return null

            val unit = state.temperatureUnit
            val status = when {
                reconnecting -> "Reconnecting, last reading"
                state.powerMode == SmokerPowerMode.STARTUP -> "Starting up"
                state.powerMode == SmokerPowerMode.SHUTDOWN -> "Cooling down"
                state.powerMode == SmokerPowerMode.ERROR -> "Grill error"
                else -> null
            }
            return GrillNotificationContent(
                title = "Grill ${unit.format(state.currentChamberTempF)} · Set ${unit.format(state.targetChamberTempF)}",
                text = listOfNotNull("Smoke level ${state.smokeLevel}", status).joinToString(" · ")
            )
        }
    }
}
