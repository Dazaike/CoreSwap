package com.coreswap.app.ui.showcase

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import com.coreswap.app.ui.GlassSegmented
import com.coreswap.app.ui.HapticKind
import com.coreswap.app.ui.LocalHaptics
import com.coreswap.app.ui.PrismIcon
import com.coreswap.app.ui.PrismIcons
import com.coreswap.app.ui.focusRing
import com.coreswap.app.ui.pressInput
import com.coreswap.app.ui.rememberPressLayer
import com.coreswap.app.ui.rememberPressState
import com.coreswap.app.ui.theme.LocalContentColor
import com.coreswap.app.ui.theme.LocalMotion
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import kotlin.math.max

private val IconSizes = listOf(16, 20, 24, 32)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconsPage(backdrop: Backdrop) {
    var sizeIndex by rememberSaveable { mutableIntStateOf(2) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val iconSize = IconSizes[sizeIndex].dp

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ShowcaseSection(
            "Icon set",
            "${PrismIcons.all.size} stroked icons that follow the current content colour. Tap one to tint it with the accent.",
        ) {
            GlassSegmented(
                options = IconSizes.map { it.toString() },
                selectedIndex = sizeIndex,
                onSelect = { sizeIndex = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrismIcons.all.forEach { (name, icon) ->
                    key(name) {
                        IconTile(
                            name = name,
                            icon = icon,
                            iconSize = iconSize,
                            isSelected = selectedName == name,
                            onClick = { selectedName = if (selectedName == name) null else name },
                        )
                    }
                }
            }
        }
    }
}

/** Flat pressable tile (not glass): Ghost-style press fill over a weak base fill. */
@Composable
private fun IconTile(
    name: String,
    @DrawableRes icon: Int,
    iconSize: Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = Prism.colors
    val accent = Prism.accent
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val press = rememberPressState()
    val layer = rememberPressLayer(press)
    val shape = RoundedRectangle(16.dp)
    val tint by animateColorAsState(
        targetValue = if (isSelected) accent else LocalContentColor.current,
        animationSpec = motion.fade(180),
        label = "iconTint",
    )

    Column(
        Modifier
            .focusRing(press, shape, accent.copy(alpha = 0.85f))
            .size(72.dp)
            .graphicsLayer(layer)
            .clip(shape)
            .drawBehind {
                drawRect(colors.fillWeak)
                val a = colors.fillWeak.alpha * max(press.hover, press.progress * 1.6f)
                if (a > 0f) drawRect(colors.fillWeak.copy(alpha = a))
            }
            .pressInput(press)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                role = Role.Button,
            ) {
                haptics.perform(HapticKind.Tick)
                onClick()
            }
            .semantics { selected = isSelected }
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PrismIcon(icon, null, size = iconSize, tint = tint)
        Spacer(Modifier.height(6.dp))
        PrismText(
            name,
            fontSize = 11.sp,
            color = colors.subText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
