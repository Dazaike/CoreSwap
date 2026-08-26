package com.coreswap.mode

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.coreswap.core.Prefs
import kotlinx.coroutines.launch

/**
 * Headless entry point: switches the mode and finishes. Exported subclasses are the contract
 * MacroDroid launches, so their class names must stay stable.
 */
abstract class ModeActivity(private val mode: String) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                val modelName = ModeSwitcher.apply(applicationContext, lifecycleScope, mode)
                if (Prefs.toastOnSuccess(this@ModeActivity)) {
                    toast("$modelName: ${ModeSwitcher.label(mode)}")
                }
            } catch (t: Throwable) {
                Log.w("CoreSwap", "mode switch failed", t)
                toast(t.message ?: "Mode switch failed")
            } finally {
                finish()
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
