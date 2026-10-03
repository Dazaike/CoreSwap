package com.coreswap.app.ui.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.coreswap.app.ui.ButtonSize
import com.coreswap.app.ui.ButtonVariant
import com.coreswap.app.ui.GlassButton
import com.coreswap.app.ui.GlassIconButton
import com.coreswap.app.ui.GlassSubmitButton
import com.coreswap.app.ui.LocalToasts
import com.coreswap.app.ui.PrismIcons
import com.coreswap.app.ui.SlideToConfirm
import com.coreswap.app.ui.ToastKind
import com.coreswap.app.ui.TooltipBox
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonsPage(backdrop: Backdrop) {
    val toasts = LocalToasts.current
    var submitted by rememberSaveable { mutableIntStateOf(0) }
    var confirmed by rememberSaveable { mutableIntStateOf(0) }
    val rowSpacing = Arrangement.spacedBy(10.dp)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ShowcaseSection("Variants") {
            FlowRow(horizontalArrangement = rowSpacing, verticalArrangement = rowSpacing) {
                GlassButton(backdrop, "Continue", { toasts.show("Pressed Continue") }, variant = ButtonVariant.Primary)
                GlassButton(backdrop, "Cancel", { toasts.show("Pressed Cancel") }, variant = ButtonVariant.Secondary)
                GlassButton(backdrop, "Details", { toasts.show("Pressed Details") }, variant = ButtonVariant.Outlined)
                GlassButton(
                    backdrop,
                    "Delete",
                    { toasts.show("Pressed Delete") },
                    variant = ButtonVariant.Destructive,
                    leadingIcon = PrismIcons.Trash,
                )
            }
        }

        ShowcaseSection("With icons") {
            FlowRow(horizontalArrangement = rowSpacing, verticalArrangement = rowSpacing) {
                GlassButton(
                    backdrop,
                    "Add item",
                    { toasts.show("Pressed Add item") },
                    variant = ButtonVariant.Primary,
                    leadingIcon = PrismIcons.Plus,
                )
                GlassButton(
                    backdrop,
                    "Next",
                    { toasts.show("Pressed Next") },
                    variant = ButtonVariant.Secondary,
                    trailingIcon = PrismIcons.ArrowRight,
                )
                GlassButton(
                    backdrop,
                    "Back",
                    { toasts.show("Pressed Back") },
                    variant = ButtonVariant.Outlined,
                    leadingIcon = PrismIcons.ArrowLeft,
                )
            }
        }

        ShowcaseSection("Icon-only", caption = "Long-press or hover for a tooltip") {
            FlowRow(horizontalArrangement = rowSpacing, verticalArrangement = rowSpacing) {
                IconDemo(backdrop, PrismIcons.Plus, "Add") { toasts.show("Pressed Add") }
                IconDemo(backdrop, PrismIcons.Minus, "Remove") { toasts.show("Pressed Remove") }
                IconDemo(backdrop, PrismIcons.Search, "Search") { toasts.show("Pressed Search") }
                IconDemo(backdrop, PrismIcons.Settings, "Settings") { toasts.show("Pressed Settings") }
                IconDemo(backdrop, PrismIcons.Close, "Close") { toasts.show("Pressed Close") }
                IconDemo(backdrop, PrismIcons.Check, "Confirm", ButtonVariant.Primary) { toasts.show("Pressed Confirm") }
            }
        }

        ShowcaseSection("Sizes") {
            FlowRow(horizontalArrangement = rowSpacing, verticalArrangement = rowSpacing) {
                GlassButton(backdrop, "Small", { toasts.show("Pressed Small") }, size = ButtonSize.Small)
                GlassButton(backdrop, "Medium", { toasts.show("Pressed Medium") }, size = ButtonSize.Medium)
                GlassButton(backdrop, "Large", { toasts.show("Pressed Large") }, size = ButtonSize.Large)
            }
        }

        ShowcaseSection("States") {
            FlowRow(horizontalArrangement = rowSpacing, verticalArrangement = rowSpacing) {
                GlassButton(backdrop, "Disabled", {}, variant = ButtonVariant.Primary, enabled = false)
                GlassButton(backdrop, "Disabled", {}, variant = ButtonVariant.Secondary, enabled = false)
                GlassButton(backdrop, "Saving", {}, variant = ButtonVariant.Primary, loading = true)
            }
        }

        ShowcaseSection(
            "Submit",
            caption = "Submitted $submitted times — tapping again while saving is ignored",
        ) {
            GlassSubmitButton(backdrop, "Save changes", onSubmit = {
                delay(1800)
                submitted++
                toasts.show("Saved", ToastKind.Success)
            })
        }

        ShowcaseSection("Slide to confirm") {
            SlideToConfirm(
                text = if (confirmed == 0) "Slide to confirm" else "Confirmed ×$confirmed — slide again",
                onConfirm = { confirmed++ },
            )
        }
    }
}

@Composable
private fun IconDemo(
    backdrop: Backdrop,
    icon: Int,
    label: String,
    variant: ButtonVariant = ButtonVariant.Secondary,
    onClick: () -> Unit,
) {
    TooltipBox(label) {
        GlassIconButton(backdrop, icon, label, onClick, variant = variant)
    }
}
