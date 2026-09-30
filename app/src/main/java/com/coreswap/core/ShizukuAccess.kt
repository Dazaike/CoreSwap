package com.coreswap.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Uses Shizuku (shell privileges) to grant and revoke this app's own notification-listener access,
 * so [PlaybackWatcherService] is only bound while auto-Transparency is switched on.
 * Entirely optional: with [Prefs.shizukuManaged] off, nothing here runs.
 */
object ShizukuAccess {
    private const val TAG = "CoreSwap"
    const val REQUEST_CODE = 4711

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = isRunning() &&
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)

    /** Shows Shizuku's grant dialog; the result arrives on a listener added via [Shizuku]. */
    fun requestPermission() = Shizuku.requestPermission(REQUEST_CODE)

    /** Turns notification access for [PlaybackWatcherService] on or off. Returns whether it worked. */
    suspend fun setListenerEnabled(context: Context, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val component = ComponentName(context, PlaybackWatcherService::class.java).flattenToString()
        val verb = if (enabled) "allow_listener" else "disallow_listener"
        try {
            val process = newProcess(arrayOf("cmd", "notification", verb, component))
            val error = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            if (code != 0) Log.w(TAG, "cmd notification $verb failed ($code): $error")
            code == 0
        } catch (t: Throwable) {
            Log.w(TAG, "cmd notification $verb failed", t)
            false
        }
    }

    /**
     * Applies the listener state that matches the current preferences. No-op unless the user opted
     * in and Shizuku is running with our permission granted.
     */
    suspend fun sync(context: Context) {
        if (!Prefs.shizukuManaged(context) || !hasPermission()) return
        setListenerEnabled(context, Prefs.transparencyOnPause(context))
    }

    // Shizuku made newProcess private in 13.1; it is still the supported shell entry point
    // short of writing a bound UserService for a single command.
    private fun newProcess(command: Array<String>): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, command, null, null) as Process
    }
}
