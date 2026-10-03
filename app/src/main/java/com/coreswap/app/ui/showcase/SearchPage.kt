package com.coreswap.app.ui.showcase

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.remember
import com.coreswap.app.ui.theme.LocalMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import com.coreswap.app.ui.ButtonSize
import com.coreswap.app.ui.ButtonVariant
import com.coreswap.app.ui.GlassButton
import com.coreswap.app.ui.GlassSearchBar
import com.coreswap.app.ui.HapticKind
import com.coreswap.app.ui.LocalHaptics
import com.coreswap.app.ui.LocalToasts
import com.coreswap.app.ui.PrismIcon
import com.coreswap.app.ui.PrismIcons
import com.coreswap.app.ui.focusRing
import com.coreswap.app.ui.pressInput
import com.coreswap.app.ui.rememberPressState
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import kotlin.math.max

private class CalcFunction(val name: String, val description: String)

private val Functions = listOf(
    CalcFunction("sin", "Sine of an angle"),
    CalcFunction("cos", "Cosine"),
    CalcFunction("tan", "Tangent"),
    CalcFunction("asin", "Inverse sine"),
    CalcFunction("acos", "Inverse cosine"),
    CalcFunction("atan", "Inverse tangent"),
    CalcFunction("ln", "Natural logarithm"),
    CalcFunction("log", "Base-10 logarithm"),
    CalcFunction("√", "Square root"),
    CalcFunction("x²", "Square"),
    CalcFunction("xʸ", "Power"),
    CalcFunction("10ˣ", "Power of ten"),
    CalcFunction("n!", "Factorial"),
    CalcFunction("π", "Pi constant"),
    CalcFunction("e", "Euler's number"),
    CalcFunction("%", "Percent"),
    CalcFunction("( )", "Parentheses"),
    CalcFunction("DEG", "Degrees mode"),
    CalcFunction("RAD", "Radians mode"),
    CalcFunction("AC", "Clear all"),
    CalcFunction("⌫", "Delete last digit"),
    CalcFunction("History", "Past calculations"),
    CalcFunction("Accent", "Accent colour"),
    CalcFunction("Haptics", "Vibration feedback"),
)

private val RowShape = RoundedRectangle(14.dp)

private enum class SearchMode { Empty, NoResults, Results }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPage(backdrop: Backdrop) {
    val state = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val colors = Prism.colors
    val motion = LocalMotion.current
    val query = state.text.toString().trim()
    val results = if (query.isEmpty()) {
        emptyList()
    } else {
        Functions.filter {
            it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        GlassSearchBar(
            backdrop = backdrop,
            state = state,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Search functions",
            onSearch = { focusManager.clearFocus() },
        )
        Spacer(Modifier.height(20.dp))

        val mode = when {
            query.isEmpty() -> SearchMode.Empty
            results.isEmpty() -> SearchMode.NoResults
            else -> SearchMode.Results
        }
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(motion.fade(220, delayMs = 60)) togetherWith fadeOut(motion.fade(120)) },
            label = "searchMode",
        ) { current ->
            when (current) {
                SearchMode.Empty -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    PrismIcon(PrismIcons.Search, null, size = 32.dp, tint = colors.subText)
                    Spacer(Modifier.height(12.dp))
                    PrismText(
                        "Search ${Functions.size} calculator functions",
                        fontSize = 15.sp,
                        color = colors.subText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf("sin", "log", "History").forEach { chip ->
                            GlassButton(
                                backdrop = backdrop,
                                text = chip,
                                onClick = { state.setTextAndPlaceCursorAtEnd(chip) },
                                size = ButtonSize.Small,
                            )
                        }
                    }
                }

                SearchMode.NoResults -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    PrismIcon(PrismIcons.Search, null, size = 32.dp, tint = colors.subText)
                    Spacer(Modifier.height(12.dp))
                    PrismText("No matches for “$query”", fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    PrismText(
                        "Check the spelling or try a shorter term",
                        fontSize = 13.sp,
                        color = colors.subText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    GlassButton(
                        backdrop = backdrop,
                        text = "Clear search",
                        onClick = { state.clearText() },
                        variant = ButtonVariant.Outlined,
                        size = ButtonSize.Small,
                    )
                }

                SearchMode.Results -> Column(Modifier.fillMaxWidth()) {
                    PrismText(
                        if (results.size == 1) "1 result" else "${results.size} results",
                        fontSize = 13.sp,
                        color = colors.subText,
                    )
                    Spacer(Modifier.height(8.dp))
                    val accent = Prism.accent
                    Functions.forEachIndexed { index, fn ->
                        val matches = results.any { it === fn }
                        // Rows start hidden and animate in (staggered); filtering animates them out and reflows the list.
                        val visible = remember { MutableTransitionState(false) }
                        visible.targetState = matches
                        val order = results.indexOfFirst { it === fn }.coerceAtLeast(0)
                        AnimatedVisibility(
                            visibleState = visible,
                            enter = expandVertically(motion.settle()) +
                                fadeIn(motion.fade(220, delayMs = (order * 30).coerceAtMost(240))),
                            exit = shrinkVertically(motion.exit(200)) + fadeOut(motion.fade(120)),
                        ) {
                            ResultRow(
                                name = highlight(fn.name, query, accent),
                                description = highlight(fn.description, query, accent),
                                label = fn.name,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Ghost row: immediate press dip lighting, no ripple. */
@Composable
private fun ResultRow(name: AnnotatedString, description: AnnotatedString, label: String) {
    val press = rememberPressState()
    val colors = Prism.colors
    val haptics = LocalHaptics.current
    val toasts = LocalToasts.current
    Column(
        Modifier
            .focusRing(press, RowShape, Prism.accent.copy(alpha = 0.85f))
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .drawBehind {
                val a = max(press.hover, press.progress * 1.6f)
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
                toasts.show("Selected $label")
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        PrismText(name, fontSize = 16.sp, maxLines = 1)
        PrismText(description, fontSize = 13.sp, color = colors.subText, maxLines = 1)
    }
}

/** [text] with the first case-insensitive occurrence of [query] in the accent colour. */
private fun highlight(text: String, query: String, accent: Color): AnnotatedString {
    val start = text.indexOf(query, ignoreCase = true)
    if (start < 0) return AnnotatedString(text)
    val end = start + query.length
    return buildAnnotatedString {
        append(text, 0, start)
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Medium)) { append(text, start, end) }
        append(text, end, text.length)
    }
}
