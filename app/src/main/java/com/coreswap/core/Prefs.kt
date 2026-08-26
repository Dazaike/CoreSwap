package com.coreswap.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

object Prefs {
    private const val NAME = "coreswap"
    private const val KEY_TOAST_ON_SUCCESS = "toastOnSuccess"
    private const val KEY_DEVICE_PRIORITY = "devicePriority"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun toastOnSuccess(context: Context): Boolean = prefs(context).getBoolean(KEY_TOAST_ON_SUCCESS, true)

    fun setToastOnSuccess(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_TOAST_ON_SUCCESS, value).apply()
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
