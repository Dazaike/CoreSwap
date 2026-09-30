package com.coreswap.mode

import android.content.Context
import android.util.Log
import com.coreswap.lib.wrapper.Setting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Debug aid: holds one connection to the device open and reads the ambient sound mode every
 * [POLL_MS]. While it runs, [ModeSwitcher] reuses [held] instead of connecting per switch.
 * Not persisted: it stops when the process dies or the user switches it off.
 */
object DebugMonitor {
    private const val TAG = "CoreSwap"
    private const val POLL_MS = 500L
    private const val RETRY_MS = 2_000L
    private const val CONNECT_TIMEOUT_MS = 25_000L

    data class State(val running: Boolean = false, val text: String = "")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val _state = MutableStateFlow(State())

    val state: StateFlow<State> = _state

    @Volatile
    var held: ModeSwitcher.Opened? = null
        private set

    @Synchronized
    fun start(context: Context) {
        if (job != null) return
        val app = context.applicationContext
        _state.value = State(running = true, text = "Connecting…")
        job = scope.launch { run(app) }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun CoroutineScope.run(context: Context) {
        var polls = 0L
        try {
            while (isActive) {
                try {
                    val opened = held ?: withTimeout(CONNECT_TIMEOUT_MS) {
                        // This scope hosts the RFCOMM read loop, so it lives until stop().
                        ModeSwitcher.open(context, this@run)
                    }.also { held = it }
                    val setting = opened.device.setting(ModeSwitcher.SETTING_AMBIENT_SOUND_MODE)
                    val mode = (setting as? Setting.SelectSetting)?.value
                    polls++
                    _state.value = State(
                        running = true,
                        text = "${opened.modelName}: ${mode?.let(ModeSwitcher::label) ?: "unknown"}\n" +
                            "connected, polling every ${POLL_MS}ms (#$polls)",
                    )
                    delay(POLL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.w(TAG, "debug monitor: connection lost", t)
                    drop()
                    _state.value = State(running = true, text = "Reconnecting: ${t.message ?: t.javaClass.simpleName}")
                    delay(RETRY_MS)
                }
            }
        } finally {
            withContext(NonCancellable) { drop() }
            _state.value = State()
        }
    }

    private fun drop() {
        val opened = held
        held = null
        runCatching { opened?.device?.close() }
    }
}
