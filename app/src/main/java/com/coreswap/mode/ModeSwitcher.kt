package com.coreswap.mode

import android.content.Context
import com.coreswap.bluetooth.activeOutputMacs
import com.coreswap.bluetooth.connectedMacs
import com.coreswap.bluetooth.connectionBackends
import com.coreswap.bluetooth.hasBluetoothConnectPermission
import com.coreswap.bluetooth.selectTarget
import com.coreswap.core.Prefs
import com.coreswap.core.SessionHolder
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

    /**
     * Connects to whichever configured device is currently connected, sets the ambient sound mode,
     * and disconnects. Returns the translated model name of the device that was switched.
     *
     * [scope] must outlive this call but die with the caller: the RFCOMM read loop runs in it.
     */
    suspend fun apply(context: Context, scope: CoroutineScope, mode: String): String = withTimeout(TIMEOUT_MS) {
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

        val modelName = translateDeviceModel(target.model)
        val device = session.connectWithBackends(
            connectionBackends(context, scope),
            target.macAddress,
        )
        try {
            val setting = device.setting(SETTING_AMBIENT_SOUND_MODE)
            if (setting !is Setting.SelectSetting) {
                throw IllegalStateException("$modelName has no ambient sound mode")
            }
            if (mode !in setting.setting.options) {
                throw IllegalStateException("$modelName does not support ${label(mode)}")
            }
            // setSettingValues waits for the device's ack, so closing right after is safe.
            device.setSettingValues(listOf(SettingIdValuePair(SETTING_AMBIENT_SOUND_MODE, mode.toValue())))
        } finally {
            device.close()
        }
        modelName
    }
}
