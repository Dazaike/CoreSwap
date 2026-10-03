package com.coreswap.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import com.coreswap.app.ui.theme.LocalMotion
import com.coreswap.app.ui.theme.Prism

/**
 * Scrim + floating liquid glass sheet that rises over the page. Dismissed by scrim tap or Back.
 * [content] receives the sheet's own exported backdrop: glass inside the sheet must refract it.
 */
@Composable
fun BoxScope.SheetOverlay(
    backdrop: Backdrop,
    visible: Boolean,
    onDismiss: () -> Unit,
    heightFraction: Float,
    content: @Composable ColumnScope.(surface: Backdrop) -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val colors = Prism.colors
    val motion = LocalMotion.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motion.fade(250)),
        exit = fadeOut(motion.fade(200)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        ) {}
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        // Sliding in from off-screen needs no fade; an alpha on the glass forces an offscreen pass over the blur.
        enter = if (motion.reduced) fadeIn(motion.fade(250)) else slideInVertically(motion.settle()) { it },
        exit = if (motion.reduced) fadeOut(motion.fade(200)) else slideOutVertically(motion.exit(220)) { it },
    ) {
        val surface = rememberLayerBackdrop()
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .fillMaxHeight(heightFraction)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(40.dp) },
                    effects = {
                        vibrancy()
                        blur(28.dp.toPx() * GlassBlurScale)
                        lens(16.dp.toPx(), 32.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = colors.highlightAlpha) },
                    shadow = { Shadow(radius = 24.dp, color = Color.Black.copy(alpha = 0.18f * colors.shadowAlpha)) },
                    exportedBackdrop = surface,
                    onDrawSurface = { drawRect(colors.sheet) },
                )
                .clickable(interactionSource = null, indication = null) {}
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) { content(surface) }
    }
}
