package com.coreswap.app.ui.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import com.coreswap.app.ui.GlassButton
import com.coreswap.app.ui.GlassDrawer
import com.coreswap.app.ui.GlassSegmented
import com.coreswap.app.ui.GlassSidebar
import com.coreswap.app.ui.PrismIcons
import com.coreswap.app.ui.SidebarItem
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText

private val NavItems = listOf(
    SidebarItem("home", "Home", PrismIcons.Grid),
    SidebarItem("search", "Search", PrismIcons.Search),
    SidebarItem("history", "History", PrismIcons.History),
    SidebarItem("alerts", "Alerts", PrismIcons.Bell),
    SidebarItem("settings", "Settings", PrismIcons.Settings),
)

@Composable
fun NavigationPage(backdrop: Backdrop) {
    val colors = Prism.colors
    val accent = Prism.accent
    var segment by rememberSaveable { mutableIntStateOf(0) }
    var selectedKey by rememberSaveable { mutableStateOf(NavItems.first().key) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = NavItems.firstOrNull { it.key == selectedKey }?.label ?: ""

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ShowcaseSection("Sidebar", "Collapsed rows show a tooltip; the indicator glides between items") {
            GlassSegmented(
                options = listOf("Expanded", "Collapsed"),
                selectedIndex = segment,
                onSelect = { segment = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            DemoFrame(Modifier.height(360.dp)) {
                val frameBackdrop = rememberLayerBackdrop()
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedRectangle(14.dp))
                        .layerBackdrop(frameBackdrop)
                        .drawBehind { drawShowcaseGradient(colors.background, accent) },
                )
                Row(Modifier.fillMaxSize()) {
                    GlassSidebar(
                        backdrop = frameBackdrop,
                        items = NavItems,
                        selectedKey = selectedKey,
                        onSelect = { selectedKey = it },
                        expanded = segment == 0,
                        modifier = Modifier.fillMaxHeight(),
                        header = "Workspace",
                        onToggleExpanded = { segment = 1 - segment },
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PrismText(
                            "Selected: $selectedLabel",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        ShowcaseSection("Drawer", "Swipe left, tap the scrim, or press Back to close") {
            GlassButton(backdrop, "Open drawer", { drawerOpen = true })
        }

        Spacer(Modifier.height(24.dp))
    }

    GlassDrawer(
        visible = drawerOpen,
        onDismiss = { drawerOpen = false },
        items = NavItems,
        selectedKey = selectedKey,
        onSelect = { selectedKey = it },
        header = "Workspace",
    )
}

/** The showcase "Gradient" background: base colour plus three soft radial blobs. */
private fun DrawScope.drawShowcaseGradient(background: Color, accent: Color) {
    drawRect(background)
    val w = size.width
    val h = size.height
    drawRect(
        Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(w * 0.15f, h * 0.20f),
            radius = w * 0.60f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0xFF5E5CE6).copy(alpha = 0.30f), Color.Transparent),
            center = Offset(w * 0.85f, h * 0.55f),
            radius = w * 0.55f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0xFF30D158).copy(alpha = 0.20f), Color.Transparent),
            center = Offset(w * 0.30f, h * 0.90f),
            radius = w * 0.50f,
        ),
    )
}
