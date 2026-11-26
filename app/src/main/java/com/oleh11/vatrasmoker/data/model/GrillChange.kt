package com.oleh11.vatrasmoker.data.model

/** How a change sent to the grill turned out. */
enum class CommandResult {
    /** The grill reported the new setting back. */
    CONFIRMED,
    /** Delivered over Bluetooth, but the grill has not reported the new setting. */
    UNCONFIRMED,
    /** Never reached the grill. */
    FAILED
}

object ChangeMessage {
    fun of(result: CommandResult, change: String, simulator: Boolean = false): String = when {
        simulator -> "Done (simulator): $change"
        result == CommandResult.CONFIRMED -> "\u2713 Grill confirmed: $change"
        result == CommandResult.UNCONFIRMED -> "Sent, but not confirmed: $change. Check the grill."
        else -> "Not changed: $change. The grill did not receive it."
    }
}
