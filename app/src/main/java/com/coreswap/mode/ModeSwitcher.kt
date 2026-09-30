package com.coreswap.mode

import android.content.Context
import com.coreswap.bluetooth.activeOutputMacs
import com.coreswap.bluetooth.connectedMacs
import com.coreswap.bluetooth.connectionBackends
import com.coreswap.bluetooth.hasBluetoothConnectPermission
import com.coreswap.bluetooth.selectTarget
import com.coreswap.core.Prefs
import com.coreswap.core.SessionHolder
import com.coreswap.lib.bindings.OpenScq30Device
import com.coreswap.lib.bindings.SettingIdValuePair
import com.coreswap.lib.bindings.translateDeviceModel
import com.coreswap.lib.wrapper.Setting
import com.coreswap.lib.wrapper.toValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout

object ModeSwitcher {
    const val SETTING_AMBIENT_SOUND_MODE = "ambientSoundMode"
    const val MODE_NOISE_CANCELING = "NoiseCanceling"
    const val MODE_TRANSPARENCY = "Transparency"
    const val MODE_NORMAL = "Normal"

    private const val TIMEOUT_MS = 25_000L

    fun label(mode: String): String = when (mode) {
        MODE_NOISE_CANCELING -> "Noise Canceling"
        MODE_TRANSPARENCY -> "Transparency"
        MODE_NORMAL -> "Normal"
        else -> mode
    }

    data class Result(val modelName: String, val previousMode: String?)

    suspend fun apply(context: Context, scope: CoroutineScope, mode: String): String =
        switch(context, scope, mode).modelName

    /** Reads the connected device's current ambient sound mode without changing it. */
    suspend fun current(context: Context, scope: CoroutineScope): Result = switch(context, scope, null)

    class Opened(val modelName: String, val device: OpenScq30Device)

    /** Connects to whichever configured device is currently connected. The caller closes it. */
    suspend fun open(context: Context, scope: CoroutineScope): Opened {
        if (!hasBluetoothConnectPermission(context)) {
            throw IllegalStateException("Open CoreSwap and grant the Bluetooth permission")
        }

        val session = SessionHolder.get(context)
        val paired = session.pairedDevices()
        if (paired.isEmpty()) {
            throw IllegalStateException("No devices configured — open CoreSwap")
        }

        val target = selectTarget(
            paired = paired,
            connectedMacs = connectedMacs(context),
            activeOutputMacs = activeOutputMacs(context),
            priority = Prefs.priority(context),
        ) ?: throw IllegalStateException("No configured Soundcore device is connected")

        val device = session.connectWithBackends(
            connectionBackends(context, scope),
            target.macAddress,
        )
        return Opened(translateDeviceModel(target.model), device)
    }

    /**
     * Sets the ambient sound mode on the connected device (or only reads it when [mode] is null) and
     * disconnects. Returns the translated model name and the mode the device was in beforehand.
     * Reuses the connection held by [DebugMonitor] when it is running, and leaves it open.
     *
     * When [onlyIfCurrent] is set the switch is skipped unless the device is in that mode.
     *
     * [scope] must outlive this call but die with the caller: the RFCOMM read loop runs in it.
     */
    suspend fun switch(
        context: Context,
        scope: CoroutineScope,
        mode: String?,
        onlyIfCurrent: String? = null,
    ): Result = withTimeout(TIMEOUT_MS) {
        val held = DebugMonitor.held
        val opened = held ?: open(context, scope)
        val modelName = opened.modelName
        val device = opened.device
        var previousMode: String? = null
        try {
            val setting = device.setting(SETTING_AMBIENT_SOUND_MODE)
            if (setting !is Setting.SelectSetting) {
                throw IllegalStateException("$modelName has no ambient sound mode")
            }
            if (mode != null && mode !in setting.setting.options) {
                throw IllegalStateException("$modelName does not support ${label(mode)}")
            }
            val previous = setting.value
            if (mode != null && (onlyIfCurrent == null || previous == onlyIfCurrent)) {
                // setSettingValues waits for the device's ack, so closing right after is safe.
                device.setSettingValues(listOf(SettingIdValuePair(SETTING_AMBIENT_SOUND_MODE, mode.toValue())))
            }
            previousMode = previous
        } finally {
            if (held == null) device.close()
        }
        Result(modelName, previousMode)
    }
}
