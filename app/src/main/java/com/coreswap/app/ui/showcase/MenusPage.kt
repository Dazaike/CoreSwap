package com.coreswap.app.ui.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.coreswap.app.ui.GlassDropdown
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import kotlin.math.abs

private val AngleUnits = listOf("Degrees", "Radians", "Gradians")
private val Regions = listOf("North", "South")

@Composable
fun MenusPage(backdrop: Backdrop) {
    val colors = Prism.colors
    val timeZones = remember {
        (-12..14).map { "UTC" + (if (it < 0) "−" else "+") + "%02d:00".format(abs(it)) }
    }
    var angle by rememberSaveable { mutableIntStateOf(0) }
    var zone by rememberSaveable { mutableIntStateOf(12) }
    var region by rememberSaveable { mutableIntStateOf(-1) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ShowcaseSection("Dropdowns", "Tap outside, press Back or Escape to close; arrow keys move the highlight") {
            GlassDropdown(
                label = "Angle unit",
                options = AngleUnits,
                selectedIndex = angle,
                onSelect = { angle = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            GlassDropdown(
                label = "Time zone",
                options = timeZones,
                selectedIndex = zone,
                onSelect = { zone = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            GlassDropdown(
                label = "Region",
                options = Regions,
                selectedIndex = region,
                onSelect = { region = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                placeholder = "Unavailable",
            )
            Spacer(Modifier.height(16.dp))
            PrismText(
                "Angle unit: ${AngleUnits.getOrElse(angle) { "—" }} · Time zone: ${timeZones.getOrElse(zone) { "—" }}",
                fontSize = 13.sp,
                color = colors.subText,
            )
        }
    }
}
