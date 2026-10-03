package com.coreswap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coreswap.app.ui.HapticKind
import com.coreswap.app.ui.LocalHaptics
import com.coreswap.app.ui.PrismIcon
import com.coreswap.app.ui.Spinner
import com.coreswap.app.ui.focusRing
import com.coreswap.app.ui.glassDepth
import com.coreswap.app.ui.liquidGlass
import com.coreswap.app.ui.pressInput
import com.coreswap.app.ui.rememberPressLayer
import com.coreswap.app.ui.rememberPressState
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import com.coreswap.mode.ModeSwitcher
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle

private val CellShape = RoundedRectangle(20.dp)

/**
 * One icon-over-label glass cell of the mode grid, shared by the main screen and the mode menu.
 * While a switch runs ([busy]) the tapped cell ([switching]) shows a spinner and the others dim.
 */
@Composable
internal fun ModeCell(
    backdrop: Backdrop,
    mode: String,
    icon: Int,
    busy: Boolean,
    switching: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = rememberPressState(!busy)
    val colors = Prism.colors
    val haptics = LocalHaptics.current
    val layer = rememberPressLayer(press)
    val depth = glassDepth(press)
    val label = ModeSwitcher.label(mode)
    Column(
        modifier
            .alpha(if (busy && switching != mode) 0.4f else 1f)
            .graphicsLayer { }
            .focusRing(press, CellShape, Prism.accent.copy(alpha = 0.85f))
            .liquidGlass(
                backdrop = backdrop,
                shape = { CellShape },
                depth = depth,
                blurRadius = 6.dp,
                refractionHeight = 8.dp,
                refractionAmount = 16.dp,
                surface = colors.fill.copy(alpha = colors.fill.alpha + 0.035f * press.hover),
                layerBlock = layer,
                onDrawFront = {
                    val outline = CellShape.createOutline(size, layoutDirection, this)
                    val c = colors.outline
                    drawOutline(
                        outline,
                        c.copy(alpha = (c.alpha + 0.15f * press.hover).coerceAtMost(1f)),
                        style = Stroke(1.25.dp.toPx()),
                    )
                },
            )
            .pressInput(press, enabled = !busy)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                enabled = !busy,
                role = Role.Button,
            ) {
                haptics.perform(HapticKind.Press)
                onSelect(mode)
            }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (busy && switching == mode) Spinner(size = 28.dp) else PrismIcon(icon, null, size = 32.dp)
        }
        Spacer(Modifier.height(8.dp))
        PrismText(
            label,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
