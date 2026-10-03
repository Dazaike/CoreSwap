package com.coreswap.ui

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.coreswap.app.ui.glassDepth
import com.coreswap.app.ui.liquidGlass
import com.coreswap.app.ui.rememberPressLayer
import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.coreswap.app.ui.LocalToasts
import com.coreswap.app.ui.ToastKind
import com.coreswap.app.ui.ToastState
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.coreswap.app.data.ThemeMode
import com.coreswap.app.data.UiSettings
import com.coreswap.app.ui.ButtonSize
import com.coreswap.app.ui.ButtonVariant
import com.coreswap.app.ui.GlassButton
import com.coreswap.app.ui.GlassSlider
import com.coreswap.app.ui.GlassSwitch
import com.coreswap.app.ui.GlassTextField
import com.coreswap.app.ui.HapticKind
import com.coreswap.app.ui.LocalHaptics
import com.coreswap.app.ui.OverlayHost
import com.coreswap.app.ui.SheetOverlay
import com.coreswap.app.ui.Spinner
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import com.coreswap.app.R
import com.coreswap.app.ui.PrismIcon
import com.coreswap.app.ui.focusRing
import com.coreswap.app.ui.pressInput
import com.coreswap.app.ui.rememberPressState
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import com.coreswap.app.ui.theme.PrismTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import com.coreswap.bluetooth.connectedMacs
import com.coreswap.bluetooth.hasBluetoothConnectPermission
import com.coreswap.core.KeepAliveService
import com.coreswap.core.PlaybackWatcherService
import com.coreswap.core.Prefs
import com.coreswap.core.SessionHolder
import com.coreswap.core.ShizukuAccess
import com.coreswap.lib.bindings.deviceModels
import com.coreswap.lib.bindings.translateDeviceModel
import com.coreswap.lib.wrapper.PairedDevice
import com.coreswap.mode.DebugMonitor
import com.coreswap.mode.ModeSwitcher
import com.coreswap.mode.ModeMenuActivity
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import com.coreswap.mode.AppShortcuts

private data class BondedDevice(val name: String, val macAddress: String)

class MainActivity : ComponentActivity() {
    private var paired by mutableStateOf<List<PairedDevice>>(emptyList())
    private var connected by mutableStateOf<Set<String>>(emptySet())
    private var busy by mutableStateOf(false)
    private var toasts: ToastState? = null
    private var switchingMode by mutableStateOf<String?>(null)
    private var keepAliveEnabled by mutableStateOf(false)
    private var toastOnSuccess by mutableStateOf(true)
    private var modeStatus by mutableStateOf("Checking…")
    private var pauseDelaySec by mutableStateOf(2f)
    private var transparencyOnPause by mutableStateOf(false)
    private var playbackAccess by mutableStateOf(false)
    private var shizukuManaged by mutableStateOf(false)

    private val shizukuResult = Shizuku.OnRequestPermissionResultListener { code, grant ->
        if (code != ShizukuAccess.REQUEST_CODE) return@OnRequestPermissionResultListener
        if (grant == PackageManager.PERMISSION_GRANTED) {
            applyShizukuManaged(true)
        } else {
            toast("Shizuku permission denied", ToastKind.Error)
        }
    }

    private var modeMenuLauncherEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(shizukuResult)
        toastOnSuccess = Prefs.toastOnSuccess(this)
        transparencyOnPause = Prefs.transparencyOnPause(this)
        shizukuManaged = Prefs.shizukuManaged(this)
        pauseDelaySec = Prefs.pauseDelayMs(this) / 1000f
        modeMenuLauncherEnabled = ModeMenuActivity.isLauncherEnabled(this)
        AppShortcuts.setup(this)
        setContent {
            val ui = remember { UiSettings(theme = ThemeMode.Dark) }
            PrismTheme(ui) {
                DisposableEffect(Unit) {
                    val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    enableEdgeToEdge(bars, bars)
                    onDispose {}
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { refresh() }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !hasBluetoothConnectPermission(this@MainActivity)
                    ) {
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
                OverlayHost { MainScreen() }
            }
        }
    }

    /** Escape dismisses the topmost overlay (every Prism overlay registers a BackHandler). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (super.dispatchKeyEvent(event)) return true
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.action == KeyEvent.ACTION_UP &&
            onBackPressedDispatcher.hasEnabledCallbacks()
        ) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        refresh()
        refreshModeStatus()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuResult)
        super.onDestroy()
    }

    private fun refresh() {
        // onStart also runs when returning from accessibility settings, so this reflects the switch.
        keepAliveEnabled = KeepAliveService.isEnabled(this)
        playbackAccess = PlaybackWatcherService.hasAccess(this)
        // Re-applies the Shizuku-managed listener state, in case it drifted (e.g. Shizuku restarted).
        lifecycleScope.launch {
            ShizukuAccess.sync(applicationContext)
            playbackAccess = PlaybackWatcherService.hasAccess(applicationContext)
        }
        modeMenuLauncherEnabled = ModeMenuActivity.isLauncherEnabled(this)
        lifecycleScope.launch {
            paired = runCatching { SessionHolder.get(applicationContext).pairedDevices() }
                .getOrElse {
                    toast(it.message ?: "Could not read configured devices", ToastKind.Error)
                    emptyList()
                }
            connected = connectedMacs(applicationContext)
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast("Enable \"CoreSwap keep-alive\" in this list")
        } catch (_: ActivityNotFoundException) {
            toast("Could not open accessibility settings", ToastKind.Error)
        }
    }

    private fun applyShizukuManaged(enabled: Boolean) {
        Prefs.setShizukuManaged(applicationContext, enabled)
        shizukuManaged = enabled
        lifecycleScope.launch {
            ShizukuAccess.sync(applicationContext)
            playbackAccess = PlaybackWatcherService.hasAccess(applicationContext)
        }
    }

    private fun onShizukuSwitch(enabled: Boolean) {
        when {
            !enabled -> applyShizukuManaged(false)
            !ShizukuAccess.isRunning() -> toast("Start Shizuku first")
            ShizukuAccess.hasPermission() -> applyShizukuManaged(true)
            else -> ShizukuAccess.requestPermission()
        }
    }

    private fun openNotificationListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            toast("Enable \"CoreSwap playback watcher\" in this list")
        } catch (_: ActivityNotFoundException) {
            toast("Could not open notification access settings", ToastKind.Error)
        }
    }

    /** Shown through the Prism toast layer; dropped while the UI is not composed (nobody could see it). */
    private fun toast(message: String, kind: ToastKind = ToastKind.Info) {
        toasts?.show(message, kind)
    }

    private fun refreshModeStatus() {
        if (busy) return
        modeStatus = "Checking…"
        lifecycleScope.launch {
            modeStatus = try {
                val result = ModeSwitcher.current(applicationContext, lifecycleScope)
                "${result.modelName}: ${result.previousMode?.let(ModeSwitcher::label) ?: "unknown"}"
            } catch (t: Throwable) {
                t.message ?: "Could not read mode"
            }
        }
    }

    private fun switchMode(mode: String) {
        if (busy) return
        busy = true
        switchingMode = mode
        lifecycleScope.launch {
            try {
                val modelName = ModeSwitcher.apply(applicationContext, lifecycleScope, mode)
                modeStatus = "$modelName: ${ModeSwitcher.label(mode)}"
                if (toastOnSuccess) toast("$modelName: ${ModeSwitcher.label(mode)}", ToastKind.Success)
            } catch (t: Throwable) {
                toast(t.message ?: "Mode switch failed", ToastKind.Error)
            } finally {
                busy = false
                switchingMode = null
            }
        }
    }

    private fun addDevice(macAddress: String, model: String) {
        lifecycleScope.launch {
            try {
                SessionHolder.get(applicationContext)
                    .pair(PairedDevice(macAddress = macAddress, model = model, isDemo = false))
                Prefs.addToPriority(applicationContext, macAddress)
            } catch (t: Throwable) {
                toast(t.message ?: "Could not add device", ToastKind.Error)
            }
            refresh()
        }
    }

    private fun removeDevice(macAddress: String) {
        lifecycleScope.launch {
            try {
                SessionHolder.get(applicationContext).unpair(macAddress)
                Prefs.removeFromPriority(applicationContext, macAddress)
            } catch (t: Throwable) {
                toast(t.message ?: "Could not remove device", ToastKind.Error)
            }
            refresh()
        }
    }

    private fun bondedDevices(): List<BondedDevice> {
        if (!hasBluetoothConnectPermission(this)) return emptyList()
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        val alreadyPaired = paired.map { it.macAddress.uppercase() }.toSet()
        return adapter.bondedDevices.orEmpty()
            .filterNot { it.address.uppercase() in alreadyPaired }
            .map { BondedDevice(name = it.name ?: "Unknown", macAddress = it.address) }
            .sortedBy { it.name }
    }

    @Composable
    private fun MainScreen() {
        val toastState = LocalToasts.current
        DisposableEffect(toastState) {
            toasts = toastState
            onDispose { toasts = null }
        }
        var bondedOpen by remember { mutableStateOf(false) }
        var bondedList by remember { mutableStateOf<List<BondedDevice>>(emptyList()) }
        var modelOpen by remember { mutableStateOf(false) }
        // Kept after dismissal so the sheet's exit animation still has content to draw.
        var modelDevice by remember { mutableStateOf<BondedDevice?>(null) }

        val colors = Prism.colors
        val accent = Prism.accent
        val pageBackdrop = rememberLayerBackdrop()
        val sheetBackdrop = rememberLayerBackdrop()

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().layerBackdrop(sheetBackdrop)) {
                // Controls refract only this flat layer, never each other.
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(pageBackdrop)
                        .drawBehind { drawRect(PageBackground) },
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    PrismText("CoreSwap", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)

                    Section("Devices", caption = if (paired.isEmpty()) {
                        "No devices configured yet. Add the Soundcore device you want to control."
                    } else {
                        null
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            paired.forEach { device ->
                                val isConnected = device.macAddress.uppercase() in connected.map { it.uppercase() }
                                Frame {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            PrismText(translateDeviceModel(device.model), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            PrismText(device.macAddress, fontSize = 13.sp, color = colors.subText)
                                            if (isConnected) {
                                                PrismText("connected", fontSize = 13.sp, color = accent, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        Spacer(Modifier.padding(start = 12.dp))
                                        GlassButton(
                                            pageBackdrop,
                                            "Remove",
                                            { removeDevice(device.macAddress) },
                                            variant = ButtonVariant.Outlined,
                                            size = ButtonSize.Small,
                                        )
                                    }
                                }
                            }
                            GlassButton(
                                pageBackdrop,
                                "Add device",
                                {
                                    bondedList = bondedDevices()
                                    bondedOpen = true
                                },
                                variant = ButtonVariant.Primary,
                            )
                        }
                    }

                    Section("Current mode") {
                        val monitor by DebugMonitor.state.collectAsState()
                        Frame {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PrismText(
                                    if (monitor.running) monitor.text else modeStatus,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.padding(start = 12.dp))
                                GlassButton(
                                    pageBackdrop,
                                    "Refresh",
                                    { refreshModeStatus() },
                                    size = ButtonSize.Small,
                                    enabled = !busy && !monitor.running,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SettingRow(
                            "Debug: stay connected and poll",
                            "Holds the connection open and reads the mode every 0.5 s until switched off. " +
                                "Uses battery; not remembered across restarts.",
                        ) {
                            GlassSwitch(
                                checked = monitor.running,
                                onCheckedChange = {
                                    if (it) DebugMonitor.start(applicationContext) else DebugMonitor.stop()
                                },
                                contentDescription = "Debug: stay connected and poll",
                            )
                        }
                    }

                    Section("Switch mode") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModeCell(
                                pageBackdrop,
                                ModeSwitcher.MODE_NOISE_CANCELING,
                                R.drawable.ic_shortcut_anc,
                                busy,
                                switchingMode,
                                ::switchMode,
                                Modifier.weight(1f),
                            )
                            ModeCell(
                                pageBackdrop,
                                ModeSwitcher.MODE_TRANSPARENCY,
                                R.drawable.ic_shortcut_transparency,
                                busy,
                                switchingMode,
                                ::switchMode,
                                Modifier.weight(1f),
                            )
                            ModeCell(
                                pageBackdrop,
                                ModeSwitcher.MODE_NORMAL,
                                R.drawable.ic_shortcut_normal,
                                busy,
                                switchingMode,
                                ::switchMode,
                                Modifier.weight(1f),
                            )
                        }
                    }

                    Section("Behavior") {
                        SettingRow("Show confirmation toast") {
                            GlassSwitch(
                                checked = toastOnSuccess,
                                onCheckedChange = {
                                    toastOnSuccess = it
                                    Prefs.setToastOnSuccess(applicationContext, it)
                                },
                                contentDescription = "Show confirmation toast",
                            )
                        }

                        SettingRow(
                            "Keep running in background",
                            if (keepAliveEnabled) {
                                "On. Switches respond immediately."
                            } else {
                                "Off. Switches cold start and take longer. Turn on the " +
                                    "\"CoreSwap keep-alive\" accessibility service to fix that."
                            },
                        ) {
                            GlassSwitch(
                                checked = keepAliveEnabled,
                                // Only the user can enable an accessibility service, so this opens settings.
                                onCheckedChange = { openAccessibilitySettings() },
                                contentDescription = "Keep running in background",
                            )
                        }

                        SettingRow(
                            "Transparency when playback pauses",
                            when {
                                shizukuManaged && ShizukuAccess.hasPermission() ->
                                    "Notification access is granted only while this is on (via Shizuku)."
                                playbackAccess ->
                                    "Switches to Transparency when music pauses and back when it resumes."
                                else ->
                                    "Needs notification access for \"CoreSwap playback watcher\" (used only " +
                                        "to read play/pause state)."
                            },
                        ) {
                            GlassSwitch(
                                checked = transparencyOnPause,
                                onCheckedChange = {
                                    transparencyOnPause = it
                                    Prefs.setTransparencyOnPause(applicationContext, it)
                                    if (shizukuManaged && ShizukuAccess.hasPermission()) {
                                        lifecycleScope.launch {
                                            ShizukuAccess.sync(applicationContext)
                                            playbackAccess = PlaybackWatcherService.hasAccess(applicationContext)
                                        }
                                    } else if (it && !playbackAccess) {
                                        openNotificationListenerSettings()
                                    }
                                },
                                contentDescription = "Transparency when playback pauses",
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            PrismText("Delay before switching", Modifier.weight(1f))
                            PrismText(
                                String.format(java.util.Locale.US, "%.1f s", pauseDelaySec),
                                fontSize = 14.sp,
                                color = colors.subText,
                            )
                        }
                        GlassSlider(
                            value = pauseDelaySec / PauseDelayMaxSec,
                            onValueChange = { pauseDelaySec = it * PauseDelayMaxSec },
                            enabled = transparencyOnPause,
                            stepCount = 20,
                            onValueChangeFinished = {
                                Prefs.setPauseDelayMs(applicationContext, (pauseDelaySec * 1000).toLong())
                            },
                            contentDescription = "Delay before switching",
                        )

                        SettingRow(
                            "Manage playback watcher with Shizuku",
                            "Optional. Grants notification access only while the pause feature is on, " +
                                "so nothing runs in the background otherwise. Needs Shizuku running.",
                        ) {
                            GlassSwitch(
                                checked = shizukuManaged,
                                onCheckedChange = { onShizukuSwitch(it) },
                                contentDescription = "Manage playback watcher with Shizuku",
                            )
                        }

                        SettingRow(
                            "Show mode picker on home screen",
                            if (modeMenuLauncherEnabled) {
                                "On. A second icon opens the three-mode menu."
                            } else {
                                "Off. Enable to add a home-screen icon for the mode menu."
                            },
                        ) {
                            GlassSwitch(
                                checked = modeMenuLauncherEnabled,
                                onCheckedChange = {
                                    try {
                                        ModeMenuActivity.setLauncherEnabled(applicationContext, it)
                                        modeMenuLauncherEnabled = it
                                    } catch (_: Throwable) {
                                        toast("Could not update home screen icon", ToastKind.Error)
                                    }
                                },
                                contentDescription = "Show mode picker on home screen",
                            )
                        }
                    }

                    Section(
                        "MacroDroid",
                        caption = "Launch these activities to switch modes without opening the app:",
                    ) {
                        PrismText(
                            "com.coreswap.mode.SetNoiseCancelingActivity\n" +
                                "com.coreswap.mode.SetTransparencyActivity\n" +
                                "com.coreswap.mode.SetNormalActivity\n" +
                                "com.coreswap.mode.ModeMenuActivity",
                            fontSize = 13.sp,
                            color = colors.subText,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Sheets sit outside the recorded layer so they never refract themselves.
            BondedDeviceSheet(
                backdrop = sheetBackdrop,
                visible = bondedOpen,
                devices = bondedList,
                onDismiss = { bondedOpen = false },
                onPick = {
                    bondedOpen = false
                    modelDevice = it
                    modelOpen = true
                },
            )
            modelDevice?.let { device ->
                ModelSheet(
                    backdrop = sheetBackdrop,
                    visible = modelOpen,
                    device = device,
                    onDismiss = { modelOpen = false },
                    onPick = { model ->
                        modelOpen = false
                        addDevice(device.macAddress, model)
                    },
                )
            }
        }
    }

    private companion object {
        const val PauseDelayMaxSec = 10f
    }
}

private val RowShape = RoundedRectangle(14.dp)
private val PageBackground = Color(0xFF1A1A20)
/** Plain text header over a group; content stays flat on the page. */
@Composable
private fun Section(title: String, caption: String? = null, content: @Composable () -> Unit) {
    val colors = Prism.colors
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(28.dp))
        PrismText(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.subText)
        if (caption != null) {
            Spacer(Modifier.height(2.dp))
            PrismText(caption, fontSize = 13.sp, color = colors.subText)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Outlined (not glass) frame, so glass inside never shows a nesting hole. */
@Composable
private fun Frame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Prism.colors.outline, RoundedRectangle(24.dp))
            .padding(12.dp),
    ) { content() }
}

/** Label (and optional caption) on the left, a control on the right. */
@Composable
private fun SettingRow(label: String, caption: String? = null, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PrismText(label)
            if (caption != null) PrismText(caption, fontSize = 13.sp, color = Prism.subText)
        }
        Spacer(Modifier.padding(start = 12.dp))
        trailing()
    }
}

/** Ghost row: immediate press dip lighting, no ripple. */
@Composable
private fun PickerRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    val press = rememberPressState()
    val colors = Prism.colors
    val haptics = LocalHaptics.current
    Column(
        Modifier
            .focusRing(press, RowShape, Prism.accent.copy(alpha = 0.85f))
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .drawBehind {
                val a = maxOf(press.hover, press.progress * 1.6f)
                if (a > 0f) {
                    drawOutline(
                        outline = RowShape.createOutline(size, layoutDirection, this),
                        color = colors.fillWeak.copy(alpha = (colors.fillWeak.alpha * a).coerceAtMost(1f)),
                    )
                }
            }
            .pressInput(press)
            .clickable(interactionSource = press.interactionSource, indication = null, role = Role.Button) {
                haptics.perform(HapticKind.Press)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        PrismText(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) PrismText(subtitle, fontSize = 13.sp, color = colors.subText, maxLines = 1)
    }
}

@Composable
private fun BoxScope.BondedDeviceSheet(
    backdrop: Backdrop,
    visible: Boolean,
    devices: List<BondedDevice>,
    onDismiss: () -> Unit,
    onPick: (BondedDevice) -> Unit,
) {
    SheetOverlay(backdrop, visible, onDismiss, heightFraction = 0.6f) { surface ->
        PrismText("Pick a paired bluetooth device", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (devices.isEmpty()) {
            PrismText(
                "No bluetooth devices are paired with this phone, or they are all already configured.",
                color = Prism.subText,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(devices) { device -> PickerRow(device.name, device.macAddress) { onPick(device) } }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassButton(surface, "Cancel", onDismiss, Modifier.fillMaxWidth(), variant = ButtonVariant.Outlined)
    }
}

@Composable
private fun BoxScope.ModelSheet(
    backdrop: Backdrop,
    visible: Boolean,
    device: BondedDevice,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val filter = rememberTextFieldState()
    // The engine cannot detect the model over RFCOMM, so the user has to say which one this is.
    val models = remember { deviceModels().map { it to translateDeviceModel(it) }.sortedBy { it.second } }
    val query = filter.text.toString()
    val shown = models.filter { (_, translated) -> translated.contains(query, ignoreCase = true) }

    SheetOverlay(backdrop, visible, onDismiss, heightFraction = 0.78f) { surface ->
        PrismText("Which model is ${device.name}?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        GlassTextField(filter, "Search models", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown) { (model, translated) -> PickerRow(translated) { onPick(model) } }
        }
        Spacer(Modifier.height(12.dp))
        GlassButton(surface, "Cancel", onDismiss, Modifier.fillMaxWidth(), variant = ButtonVariant.Outlined)
    }
}
