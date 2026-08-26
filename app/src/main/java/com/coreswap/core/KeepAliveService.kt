package com.coreswap.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Deliberately does nothing with accessibility events.
 *
 * Its only job is to exist: while an accessibility service is enabled the system keeps our process
 * bound and restarts it if it dies, so a mode switch launched from MacroDroid or a quick tile does
 * not pay for a cold start (loading the 22 MB engine, initializing i18n, opening the database).
 *
 * It requests no event types and cannot retrieve window content; see
 * res/xml/accessibility_service_config.xml.
 */
class KeepAliveService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "keep-alive service connected; warming engine")
        // Native.initialize() already ran in Application.init. Opening the session here moves the
        // sqlite open and first pairing read off the mode-switch path.
        scope.launch {
            try {
                SessionHolder.get(applicationContext).pairedDevices()
                Log.i(TAG, "engine warm")
            } catch (t: Throwable) {
                Log.w(TAG, "could not warm engine", t)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CoreSwap"

        /**
         * Whether the user has switched our service on. It cannot be enabled programmatically:
         * only the user can, from system settings.
         */
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
            val expected = ComponentName(context, KeepAliveService::class.java)
            return manager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info ->
                    val service = info.resolveInfo?.serviceInfo ?: return@any false
                    ComponentName(service.packageName, service.name) == expected
                }
        }
    }
}
