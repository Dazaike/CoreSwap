package com.coreswap.app.ui.showcase

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.coreswap.app.data.ThemeMode
import com.coreswap.app.data.UiSettings
import com.coreswap.app.ui.ButtonSize
import com.coreswap.app.ui.ButtonVariant
import com.coreswap.app.ui.GlassButton
import com.coreswap.app.ui.GlassDrawer
import com.coreswap.app.ui.GlassIconButton
import com.coreswap.app.ui.GlassSegmented
import com.coreswap.app.ui.GlassSidebar
import com.coreswap.app.ui.GlassSlider
import com.coreswap.app.ui.GlassSwitch
import com.coreswap.app.ui.HapticKind
import com.coreswap.app.ui.LocalHaptics
import com.coreswap.app.ui.PrismIcons
import com.coreswap.app.ui.SheetOverlay
import com.coreswap.app.ui.SidebarItem
import com.coreswap.app.ui.TooltipBox
import com.coreswap.app.ui.theme.LocalMotion
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismColors
import com.coreswap.app.ui.theme.PrismText
import kotlin.math.roundToInt

enum class ShowcasePage(val label: String, @DrawableRes val icon: Int) {
    Buttons("Buttons", PrismIcons.Plus),
    Controls("Controls", PrismIcons.Sliders),
    Inputs("Inputs", PrismIcons.Text),
    Search("Search", PrismIcons.Search),
    Menus("Menus", PrismIcons.ChevronDown),
    Feedback("Feedback", PrismIcons.Bell),
    Icons("Icons", PrismIcons.Grid),
    Navigation("Navigation", PrismIcons.Sidebar),
}

enum class ShowcaseBackground { Plain, Gradient, Stripes }

private val Pages = ShowcasePage.entries.map { SidebarItem(it.name, it.label, it.icon) }

/** Full-screen component showcase with navigation and a tuning sheet for motion, brightness and haptics. */
@Composable
fun ShowcaseScreen(
    initialPage: ShowcasePage,
    ui: UiSettings,
    onUi: ((UiSettings) -> UiSettings) -> Unit,
    onClose: () -> Unit,
) {
    // Registered before the tuning sheet so the sheet's handler wins while it is open.
    BackHandler(onBack = onClose)
    val colors = Prism.colors
    val accent = Prism.accent
    val motion = LocalMotion.current

    var page by rememberSaveable { mutableStateOf(initialPage) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var tuneOpen by rememberSaveable { mutableStateOf(false) }
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    var background by rememberSaveable { mutableStateOf(ShowcaseBackground.Gradient) }

    val showcaseBackdrop = rememberLayerBackdrop()
    val pageBackdrop = rememberLayerBackdrop()
    val holder = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(showcaseBackdrop)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(pageBackdrop)
                    .drawBehind { drawShowcaseBackground(background, colors, accent) },
            )
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                val wide = maxWidth >= 600.dp
                Row(Modifier.fillMaxSize()) {
                    if (wide) {
                        GlassSidebar(
                            backdrop = pageBackdrop,
                            items = Pages,
                            selectedKey = page.name,
                            onSelect = { key -> page = ShowcasePage.valueOf(key) },
                            expanded = sidebarExpanded,
                            modifier = Modifier.padding(12.dp).fillMaxHeight(),
                            header = "Components",
                            onToggleExpanded = { sidebarExpanded = !sidebarExpanded },
                        )
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TooltipBox("Close showcase") {
                                GlassIconButton(pageBackdrop, PrismIcons.ArrowLeft, "Close showcase", onClose)
                            }
                            if (!wide) {
                                TooltipBox("Open navigation") {
                                    GlassIconButton(pageBackdrop, PrismIcons.Menu, "Open navigation", { drawerOpen = true })
                                }
                            }
                            AnimatedContent(
                                targetState = page.label,
                                modifier = Modifier.weight(1f),
                                transitionSpec = { fadeIn(motion.fade(220)) togetherWith fadeOut(motion.fade(120)) },
                                label = "showcaseTitle",
                            ) { title ->
                                PrismText(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            TooltipBox("Tuning") {
                                GlassIconButton(pageBackdrop, PrismIcons.Sliders, "Tuning", { tuneOpen = true })
                            }
                        }
                        AnimatedContent(
                            targetState = page,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            transitionSpec = { fadeIn(motion.fade(220)) togetherWith fadeOut(motion.fade(120)) },
                            label = "showcasePage",
                        ) { p ->
                            holder.SaveableStateProvider(p.name) { PageContent(p, pageBackdrop) }
                        }
                    }
                }
                GlassDrawer(
                    visible = !wide && drawerOpen,
                    onDismiss = { drawerOpen = false },
                    items = Pages,
                    selectedKey = page.name,
                    onSelect = { key -> page = ShowcasePage.valueOf(key) },
                    header = "Components",
                )
            }
        }
        TuningSheet(showcaseBackdrop, tuneOpen, ui, onUi, background, { background = it }) { tuneOpen = false }
    }
}

@Composable
private fun PageContent(page: ShowcasePage, backdrop: Backdrop) {
    when (page) {
        ShowcasePage.Buttons -> ButtonsPage(backdrop)
        ShowcasePage.Controls -> ControlsPage(backdrop)
        ShowcasePage.Inputs -> InputsPage(backdrop)
        ShowcasePage.Search -> SearchPage(backdrop)
        ShowcasePage.Menus -> MenusPage(backdrop)
        ShowcasePage.Feedback -> FeedbackPage(backdrop)
        ShowcasePage.Icons -> IconsPage(backdrop)
        ShowcasePage.Navigation -> NavigationPage(backdrop)
    }
}

private fun DrawScope.drawShowcaseBackground(bg: ShowcaseBackground, colors: PrismColors, accent: Color) {
    drawRect(colors.background)
    when (bg) {
        ShowcaseBackground.Plain -> Unit
        ShowcaseBackground.Gradient -> {
            val w = size.width
            val h = size.height
            fun blob(color: Color, x: Float, y: Float, r: Float) {
                val c = Offset(w * x, h * y)
                drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = w * r))
            }
            blob(accent.copy(alpha = 0.35f), 0.15f, 0.20f, 0.60f)
            blob(Color(0xFF5E5CE6).copy(alpha = 0.30f), 0.85f, 0.55f, 0.55f)
            blob(Color(0xFF30D158).copy(alpha = 0.20f), 0.30f, 0.90f, 0.50f)
        }
        ShowcaseBackground.Stripes -> {
            val thick = 10.dp.toPx()
            val period = 24.dp.toPx()
            val color = colors.ink.copy(alpha = 0.06f)
            val span = size.width + size.height
            rotate(-30f) {
                var x = -span
                while (x < span * 2) {
                    drawRect(color, topLeft = Offset(x, -span), size = androidx.compose.ui.geometry.Size(thick, span * 3))
                    x += period
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.TuningSheet(
    backdrop: Backdrop,
    visible: Boolean,
    ui: UiSettings,
    onUi: ((UiSettings) -> UiSettings) -> Unit,
    background: ShowcaseBackground,
    onBackground: (ShowcaseBackground) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current
    SheetOverlay(backdrop, visible, onDismiss, heightFraction = 0.78f) { surface ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PrismText("Tuning", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            TuneLabel("Theme")
            GlassSegmented(
                listOf("System", "Light", "Dark"),
                ui.theme.ordinal,
                { i -> onUi { it.copy(theme = ThemeMode.entries[i]) } },
            )

            TuneLabel("Animation speed", "×%.2f".format(ui.animationSpeed))
            GlassSlider(
                value = (ui.animationSpeed - 0.5f) / 1.5f,
                onValueChange = { v -> onUi { it.copy(animationSpeed = 0.5f + 1.5f * v) } },
                stepCount = 6,
                valueLabel = { v -> "×%.2f".format(0.5f + 1.5f * v) },
                contentDescription = "Animation speed",
            )

            TuneLabel("Motion intensity", "${(ui.motionIntensity * 100).roundToInt()}%")
            GlassSlider(
                value = ui.motionIntensity,
                onValueChange = { v -> onUi { it.copy(motionIntensity = v) } },
                valueLabel = { v -> "${(v * 100).roundToInt()}%" },
                contentDescription = "Motion intensity",
            )

            DemoRow(
                "Reduce motion",
                "System animations: " + if (ValueAnimator.areAnimatorsEnabled()) "on" else "off",
            ) {
                GlassSwitch(ui.reduceMotion, { v -> onUi { it.copy(reduceMotion = v) } }, contentDescription = "Reduce motion")
            }

            TuneLabel("Surface brightness", "${(ui.brightness * 100).roundToInt()}%")
            GlassSlider(
                value = ui.brightness - 0.5f,
                onValueChange = { v -> onUi { it.copy(brightness = 0.5f + v) } },
                stepCount = 10,
                valueLabel = { v -> "${((0.5f + v) * 100).roundToInt()}%" },
                contentDescription = "Surface brightness",
            )

            DemoRow("Haptics") {
                GlassSwitch(ui.haptics, { v -> onUi { it.copy(haptics = v) } }, contentDescription = "Haptics")
            }

            TuneLabel("Haptic strength", "${(ui.hapticStrength * 100).roundToInt()}%")
            GlassSlider(
                value = (ui.hapticStrength - 0.25f) / 0.75f,
                onValueChange = { v -> onUi { it.copy(hapticStrength = 0.25f + 0.75f * v) } },
                enabled = ui.haptics,
                stepCount = 3,
                valueLabel = { v -> "${((0.25f + 0.75f * v) * 100).roundToInt()}%" },
                contentDescription = "Haptic strength",
            )

            TuneLabel("Haptic test")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    "Tap" to HapticKind.Press,
                    "Tick" to HapticKind.Tick,
                    "Success" to HapticKind.Success,
                    "Error" to HapticKind.Error,
                ).forEach { (label, kind) ->
                    GlassButton(surface, label, { haptics.perform(kind) }, size = ButtonSize.Small, enabled = ui.haptics)
                }
            }

            TuneLabel("Background")
            GlassSegmented(
                ShowcaseBackground.entries.map { it.name },
                background.ordinal,
                { onBackground(ShowcaseBackground.entries[it]) },
            )

            Spacer(Modifier.height(24.dp))
            GlassButton(
                surface,
                "Reset tuning",
                {
                    onUi {
                        it.copy(
                            hapticStrength = 0.75f,
                            animationSpeed = 1f,
                            motionIntensity = 1f,
                            reduceMotion = false,
                            brightness = 1f,
                        )
                    }
                },
                variant = ButtonVariant.Outlined,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TuneLabel(label: String, value: String? = null) {
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        PrismText(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value != null) PrismText(value, fontSize = 14.sp, color = Prism.subText)
    }
}
