package com.oleh11.vatrasmoker.data.model

data class PelletHopperState(
    val levelPercentage: Int = 100,
    val isLowPelletAlert: Boolean = false
)
