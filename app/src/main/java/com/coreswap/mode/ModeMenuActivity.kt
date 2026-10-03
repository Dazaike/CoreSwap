package com.coreswap.mode

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.coreswap.app.R
import com.coreswap.app.data.ThemeMode
import com.coreswap.app.data.UiSettings
import com.coreswap.app.ui.glassDepth
import com.coreswap.app.ui.liquidGlass
import com.coreswap.app.ui.theme.LocalMotion
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import com.coreswap.app.ui.theme.PrismTheme
import com.coreswap.core.Prefs
import com.coreswap.ui.ModeCell
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch

class ModeMenuActivity : ComponentActivity() {
    companion object {
        const val LAUNCHER_ALIAS = "com.coreswap.mode.ModeMenuLauncher"

        fun isLauncherEnabled(context: Context): Boolean {
            val state = context.packageManager.getComponentEnabledSetting(
                ComponentName(context, LAUNCHER_ALIAS),
            )
            return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        fun setLauncherEnabled(context: Context, enabled: Boolean) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, LAUNCHER_ALIAS),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private var busy by mutableStateOf(false)
    private var selectedMode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ui = remember { UiSettings(theme = ThemeMode.Dark) }
            PrismTheme(ui) {
                ModeMenuScreen(
                    busy = busy,
                    selectedMode = selectedMode,
                    onSelect = ::applyMode,
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun applyMode(mode: String) {
        if (busy) return
        busy = true
        selectedMode = mode
        lifecycleScope.launch {
            try {
                val modelName = ModeSwitcher.apply(applicationContext, lifecycleScope, mode)
                if (Prefs.toastOnSuccess(this@ModeMenuActivity)) {
                    toast("$modelName: ${ModeSwitcher.label(mode)}")
                }
            } catch (t: Throwable) {
                toast(t.message ?: "Mode switch failed")
            } finally {
                finish()
            }
        }
    }

    // The activity finishes right after the switch, so an in-window toast would die with it.
    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ModeMenuScreen(
    busy: Boolean,
    selectedMode: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    BackHandler(enabled = !busy, onBack = onDismiss)

    val colors = Prism.colors
    val motion = LocalMotion.current
    val m = motion.magnitude
    val windowBackdrop = rememberLayerBackdrop()
    val cardBackdrop = rememberLayerBackdrop()

    // The scrim is recorded so the card glass blurs it; the window behind stays untouched.
    Box(Modifier.fillMaxSize().layerBackdrop(windowBackdrop)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(interactionSource = null, indication = null, enabled = !busy, onClick = onDismiss),
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(motion.enter(240)) + scaleIn(motion.settle(), initialScale = 1f - 0.06f * m),
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .liquidGlass(
                        backdrop = windowBackdrop,
                        shape = { RoundedRectangle(28.dp) },
                        depth = glassDepth(elevation = 20.dp),
                        blurRadius = 28.dp,
                        refractionHeight = 16.dp,
                        refractionAmount = 32.dp,
                        surface = colors.background.copy(alpha = 0.88f),
                        exportedBackdrop = cardBackdrop,
                    )
                    .clickable(interactionSource = null, indication = null) { /* absorb */ }
                    .semantics { paneTitle = "Switch mode" }
                    .padding(20.dp),
            ) {
                PrismText("Switch mode", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeCell(
                        cardBackdrop,
                        ModeSwitcher.MODE_NOISE_CANCELING,
                        R.drawable.ic_shortcut_anc,
                        busy,
                        selectedMode,
                        onSelect,
                        Modifier.weight(1f),
                    )
                    ModeCell(
                        cardBackdrop,
                        ModeSwitcher.MODE_TRANSPARENCY,
                        R.drawable.ic_shortcut_transparency,
                        busy,
                        selectedMode,
                        onSelect,
                        Modifier.weight(1f),
                    )
                    ModeCell(
                        cardBackdrop,
                        ModeSwitcher.MODE_NORMAL,
                        R.drawable.ic_shortcut_normal,
                        busy,
                        selectedMode,
                        onSelect,
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
