package com.coreswap.lib.wrapper

import kotlinx.serialization.Serializable

@Serializable
data class PairedDevice(val macAddress: String, val model: String, val isDemo: Boolean)
