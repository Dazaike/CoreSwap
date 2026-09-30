package com.coreswap.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

object Prefs {
    private const val NAME = "coreswap"
    private const val KEY_TOAST_ON_SUCCESS = "toastOnSuccess"
    private const val KEY_DEVICE_PRIORITY = "devicePriority"
    private const val KEY_TRANSPARENCY_ON_PAUSE = "transparencyOnPause"
    private const val KEY_SHIZUKU_MANAGED = "shizukuManagedListener"
    private const val KEY_PAUSE_DELAY_MS = "pauseDelayMs"
    const val DEFAULT_PAUSE_DELAY_MS = 2_000L

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun toastOnSuccess(context: Context): Boolean = prefs(context).getBoolean(KEY_TOAST_ON_SUCCESS, true)

    fun setToastOnSuccess(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_TOAST_ON_SUCCESS, value).apply()
    }


    /** Whether CoreSwap may use Shizuku to grant/revoke its own notification access on demand. */
    fun shizukuManaged(context: Context): Boolean = prefs(context).getBoolean(KEY_SHIZUKU_MANAGED, false)

    fun setShizukuManaged(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHIZUKU_MANAGED, value).apply()
    }

    fun transparencyOnPause(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRANSPARENCY_ON_PAUSE, false)

    fun setTransparencyOnPause(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_TRANSPARENCY_ON_PAUSE, value).apply()
    }

    /** How long playback must stay paused before switching to Transparency. */
    fun pauseDelayMs(context: Context): Long = prefs(context).getLong(KEY_PAUSE_DELAY_MS, DEFAULT_PAUSE_DELAY_MS)

    fun setPauseDelayMs(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_PAUSE_DELAY_MS, value).apply()
    }

    /** Mac addresses in the order the user added them; earlier entries win ties when switching. */
    fun priority(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_DEVICE_PRIORITY, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<String>>(raw)
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private fun setPriority(context: Context, macAddresses: List<String>) {
        prefs(context).edit()
            .putString(KEY_DEVICE_PRIORITY, Json.encodeToString(macAddresses))
            .apply()
    }

    fun addToPriority(context: Context, macAddress: String) {
        val current = priority(context)
        if (current.any { it.equals(macAddress, ignoreCase = true) }) return
        setPriority(context, current + macAddress)
    }

    fun removeFromPriority(context: Context, macAddress: String) {
        setPriority(context, priority(context).filterNot { it.equals(macAddress, ignoreCase = true) })
    }
}
