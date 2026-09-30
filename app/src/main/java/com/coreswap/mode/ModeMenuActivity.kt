package com.coreswap.mode

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.coreswap.app.R
import com.coreswap.core.Prefs
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
            MaterialTheme(colorScheme = darkColorScheme()) {
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

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = !busy) { onDismiss() },
        )
        AnimatedVisibility(
            visible = shown,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.94f, animationSpec = tween(160)),
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { /* absorb */ },
                shape = RoundedCornerShape(20.dp),
            ) {
                Column {
                    Text(
                        "Switch mode",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                    ) {
                        ModeMenuCell(
                            mode = ModeSwitcher.MODE_NOISE_CANCELING,
                            icon = R.drawable.ic_shortcut_anc,
                            busy = busy,
                            selectedMode = selectedMode,
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f),
                        )
                        ModeMenuCell(
                            mode = ModeSwitcher.MODE_TRANSPARENCY,
                            icon = R.drawable.ic_shortcut_transparency,
                            busy = busy,
                            selectedMode = selectedMode,
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f),
                        )
                        ModeMenuCell(
                            mode = ModeSwitcher.MODE_NORMAL,
                            icon = R.drawable.ic_shortcut_normal,
                            busy = busy,
                            selectedMode = selectedMode,
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeMenuCell(
    mode: String,
    icon: Int,
    busy: Boolean,
    selectedMode: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .alpha(if (busy && selectedMode != mode) 0.4f else 1f)
            .clickable(enabled = !busy) { onSelect(mode) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (busy && selectedMode == mode) {
            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            ModeSwitcher.label(mode),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
