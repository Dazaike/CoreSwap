package com.coreswap.core

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.coreswap.mode.ModeSwitcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Watches media sessions (needs notification access; we never read notifications) and switches to
 * Transparency when playback pauses, then back to the previous mode when it resumes.
 *
 * Buffering, connecting and skipping count as "playing", so a slow track load does not flip the
 * mode. Only an explicit paused/stopped/none state does.
 */
class PlaybackWatcherService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val listenerComponent by lazy { ComponentName(this, PlaybackWatcherService::class.java) }
    private val controllers = mutableMapOf<MediaController, MediaController.Callback>()
    private var sessionManager: MediaSessionManager? = null
    private var pendingPause: Job? = null
    @Volatile
    private var wasPlaying = false

    /** Mode to return to on resume; only written and read under [mutex]. */
    private var restoreMode: String? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        attach(list.orEmpty())
        evaluate()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        sessionManager = manager
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            attach(manager.getActiveSessions(listenerComponent))
            wasPlaying = isPlaying()
            Log.i(TAG, "playback watcher connected; playing=$wasPlaying")
        } catch (t: SecurityException) {
            Log.w(TAG, "no notification access", t)
        }
    }

    override fun onListenerDisconnected() {
        detachAll()
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        sessionManager = null
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        detachAll()
        scope.cancel()
        super.onDestroy()
    }

    private fun attach(sessions: List<MediaController>) {
        val live = sessions.toSet()
        controllers.keys.filter { it !in live }.forEach { controllers.remove(it)?.let(it::unregisterCallback) }
        for (controller in live) {
            if (controller in controllers) continue
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = evaluate()
            }
            controller.registerCallback(callback)
            controllers[controller] = callback
        }
    }

    private fun detachAll() {
        controllers.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        controllers.clear()
        pendingPause?.cancel()
    }

    private fun isPlaying(): Boolean = controllers.keys.any { active(it.playbackState?.state) }

    private fun active(state: Int?): Boolean = when (state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
        -> true
        else -> false
    }

    private fun evaluate() {
        val playing = isPlaying()
        if (playing == wasPlaying) return
        wasPlaying = playing
        pendingPause?.cancel()
        pendingPause = null
        if (playing) onResumed() else onPaused()
    }

    private fun onPaused() {
        if (!Prefs.transparencyOnPause(applicationContext)) return
        pendingPause = scope.launch {
            delay(Prefs.pauseDelayMs(applicationContext))
            // A started switch must finish so restoreMode is recorded.
            withContext(NonCancellable) {
                mutex.withLock {
                    if (wasPlaying || !Prefs.transparencyOnPause(applicationContext)) return@withLock
                    try {
                        val result = ModeSwitcher.switch(applicationContext, scope, ModeSwitcher.MODE_TRANSPARENCY)
                        if (result.previousMode != ModeSwitcher.MODE_TRANSPARENCY) restoreMode = result.previousMode
                        Log.i(TAG, "paused; ${result.modelName} ${result.previousMode} -> Transparency")
                    } catch (t: Throwable) {
                        Log.w(TAG, "auto transparency on pause failed", t)
                    }
                }
            }
        }
    }

    private fun onResumed() {
        scope.launch {
            withContext(NonCancellable) {
                mutex.withLock {
                    val target = restoreMode ?: return@withLock
                    restoreMode = null
                    if (!wasPlaying || !Prefs.transparencyOnPause(applicationContext)) return@withLock
                    try {
                        // Skipped by the switcher if the user changed the mode in the meantime.
                        val result = ModeSwitcher.switch(
                            applicationContext,
                            scope,
                            target,
                            onlyIfCurrent = ModeSwitcher.MODE_TRANSPARENCY,
                        )
                        Log.i(TAG, "resumed; ${result.modelName} restored to $target")
                    } catch (t: Throwable) {
                        Log.w(TAG, "restoring mode on resume failed", t)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CoreSwap"

        fun hasAccess(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    }
}
