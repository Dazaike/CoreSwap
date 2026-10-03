package com.coreswap.app.ui.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.coreswap.app.ui.GlassCheckbox
import com.coreswap.app.ui.GlassRadio
import com.coreswap.app.ui.GlassSegmented
import com.coreswap.app.ui.GlassSlider
import com.coreswap.app.ui.GlassSwitch
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import kotlin.math.round
import kotlin.math.roundToInt

private fun percent(v: Float): String = "%d%%".format((v * 100).roundToInt())

@Composable
fun ControlsPage(backdrop: Backdrop) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ShowcaseSection("Toggles") {
            var wifi by rememberSaveable { mutableStateOf(true) }
            var bluetooth by rememberSaveable { mutableStateOf(false) }
            DemoRow("Wi-Fi") {
                GlassSwitch(wifi, { wifi = it }, contentDescription = "Wi-Fi")
            }
            DemoRow("Bluetooth") {
                GlassSwitch(bluetooth, { bluetooth = it }, contentDescription = "Bluetooth")
            }
            DemoRow("Airplane mode", caption = "Disabled") {
                GlassSwitch(false, {}, enabled = false, contentDescription = "Airplane mode")
            }
        }

        ShowcaseSection("Sliders", caption = "Tap to set, drag to scrub, hold to see the value") {
            var alwaysShow by rememberSaveable { mutableStateOf(false) }
            var volume by rememberSaveable { mutableFloatStateOf(0.4f) }
            var brightness by rememberSaveable { mutableFloatStateOf(0.7f) }
            var steps by rememberSaveable { mutableFloatStateOf(0.3f) }
            DemoRow("Always show value") {
                GlassSwitch(alwaysShow, { alwaysShow = it }, contentDescription = "Always show value")
            }
            SliderDemo("Volume", percent(volume), alwaysShow) {
                GlassSlider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueLabel = ::percent,
                    alwaysShowValue = alwaysShow,
                    contentDescription = "Volume",
                )
            }
            SliderDemo("Brightness", percent(brightness), alwaysShow) {
                GlassSlider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueLabel = ::percent,
                    alwaysShowValue = alwaysShow,
                    contentDescription = "Brightness",
                )
            }
            val stepLabel = { v: Float -> "${round(v * 10).toInt()}/10" }
            SliderDemo("Steps", stepLabel(steps), alwaysShow) {
                GlassSlider(
                    value = steps,
                    onValueChange = { steps = it },
                    stepCount = 10,
                    valueLabel = stepLabel,
                    alwaysShowValue = alwaysShow,
                    contentDescription = "Steps",
                )
            }
            SliderDemo("Disabled", percent(0.5f), alwaysShow) {
                GlassSlider(
                    value = 0.5f,
                    onValueChange = {},
                    enabled = false,
                    valueLabel = ::percent,
                    alwaysShowValue = alwaysShow,
                    contentDescription = "Disabled",
                )
            }
        }

        ShowcaseSection("Checkboxes") {
            var email by rememberSaveable { mutableStateOf(true) }
            var push by rememberSaveable { mutableStateOf(false) }
            var weekly by rememberSaveable { mutableStateOf(true) }
            GlassCheckbox(email, { email = it }, Modifier.fillMaxWidth(), label = "Email updates")
            GlassCheckbox(push, { push = it }, Modifier.fillMaxWidth(), label = "Push notifications")
            GlassCheckbox(weekly, { weekly = it }, Modifier.fillMaxWidth(), label = "Weekly summary")
            GlassCheckbox(false, {}, Modifier.fillMaxWidth(), label = "Beta features", enabled = false)
        }

        ShowcaseSection("Radios") {
            var rounding by rememberSaveable { mutableIntStateOf(0) }
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                listOf("Round to nearest", "Round up", "Round down").forEachIndexed { i, label ->
                    GlassRadio(rounding == i, { rounding = i }, Modifier.fillMaxWidth(), label = label)
                }
                GlassRadio(false, {}, Modifier.fillMaxWidth(), label = "Banker's rounding", enabled = false)
            }
        }

        ShowcaseSection("Segmented") {
            val ranges = listOf("Day", "Week", "Month")
            var range by rememberSaveable { mutableIntStateOf(1) }
            GlassSegmented(ranges, range, { range = it })
            Spacer(Modifier.height(8.dp))
            PrismText("Showing: ${ranges[range]}", fontSize = 13.sp, color = Prism.subText)
            Spacer(Modifier.height(16.dp))
            var level by rememberSaveable { mutableIntStateOf(2) }
            GlassSegmented(listOf("Low", "Med", "High", "Max"), level, { level = it })
            Spacer(Modifier.height(16.dp))
            GlassSegmented(listOf("On", "Off"), 0, {}, enabled = false)
        }
    }
}

/**
 * Label + value text above a slider. When the value bubble is always shown, extra room keeps it
 * from covering the label.
 */
@Composable
private fun SliderDemo(
    label: String,
    valueText: String,
    alwaysShow: Boolean,
    slider: @Composable () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PrismText(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        PrismText(valueText, fontSize = 14.sp, color = Prism.subText)
    }
    Spacer(Modifier.height(if (alwaysShow) 30.dp else 4.dp))
    slider()
}
