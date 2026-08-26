package com.coreswap.core

import android.content.Context
import com.coreswap.lib.bindings.OpenScq30Session
import com.coreswap.lib.bindings.newSession
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The engine session owns a sqlite database holding the mac-to-model pairings. CoreSwap keeps its
 * own database file so it never contends with an installed OpenSCQ30 app.
 */
object SessionHolder {
    private val mutex = Mutex()
    private var session: OpenScq30Session? = null

    suspend fun get(context: Context): OpenScq30Session = mutex.withLock {
        session ?: newSession(
            File(context.applicationInfo.dataDir, "coreswap_lib.sqlite").path,
        ).also { session = it }
    }
}
