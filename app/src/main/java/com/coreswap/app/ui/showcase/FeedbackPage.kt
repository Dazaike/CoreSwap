package com.coreswap.app.ui.showcase

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.coreswap.app.ui.ButtonSize
import com.coreswap.app.ui.ButtonVariant
import com.coreswap.app.ui.ConfirmDialog
import com.coreswap.app.ui.GlassButton
import com.coreswap.app.ui.GlassIconButton
import com.coreswap.app.ui.GlassProgressBar
import com.coreswap.app.ui.GlassSwitch
import com.coreswap.app.ui.LocalToasts
import com.coreswap.app.ui.PrismIcon
import com.coreswap.app.ui.PrismIcons
import com.coreswap.app.ui.SectionLoader
import com.coreswap.app.ui.SkeletonBlock
import com.coreswap.app.ui.Spinner
import com.coreswap.app.ui.ToastKind
import com.coreswap.app.ui.TooltipBox
import com.coreswap.app.ui.theme.LocalMotion
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText
import com.coreswap.app.ui.theme.accentAlpha
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val UploadFiles = 8

private enum class LoadState { Loading, Error, Loaded }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackPage(backdrop: Backdrop) {
    val colors = Prism.colors
    val toasts = LocalToasts.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // ---- Tooltips ----
        ShowcaseSection("Tooltips", "Long-press, hover, or focus with a keyboard") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TooltipBox("More information") {
                    GlassIconButton(backdrop, PrismIcons.Info, "More information", { toasts.show("Pressed More information") })
                }
                TooltipBox("Notifications") {
                    GlassIconButton(backdrop, PrismIcons.Bell, "Notifications", { toasts.show("Pressed Notifications") })
                }
                TooltipBox("Delete") {
                    GlassIconButton(
                        backdrop,
                        PrismIcons.Trash,
                        "Delete",
                        { toasts.show("Pressed Delete") },
                        variant = ButtonVariant.Destructive,
                    )
                }
            }
        }

        // ---- Toasts ----
        ShowcaseSection("Toasts", "A new toast replaces the current one; tap a toast to dismiss it") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassButton(backdrop, "Info", { toasts.show("Settings synced") }, leadingIcon = PrismIcons.Info)
                GlassButton(
                    backdrop,
                    "Success",
                    { toasts.show("Changes saved", ToastKind.Success) },
                    leadingIcon = PrismIcons.Check,
                )
                GlassButton(
                    backdrop,
                    "Error",
                    { toasts.show("Couldn't connect", ToastKind.Error) },
                    leadingIcon = PrismIcons.Alert,
                )
                GlassButton(
                    backdrop,
                    "With action",
                    {
                        toasts.show(
                            "Draft deleted",
                            actionLabel = "Undo",
                            onAction = { toasts.show("Restored", ToastKind.Success) },
                        )
                    },
                    variant = ButtonVariant.Outlined,
                )
            }
        }

        // ---- Progress ----
        ShowcaseSection("Progress") {
            var done by rememberSaveable { mutableIntStateOf(0) }
            var running by remember { mutableStateOf(false) }
            var uploadJob by remember { mutableStateOf<Job?>(null) }
            val pct = done * 100 / UploadFiles
            PrismText(
                if (done >= UploadFiles) "Upload complete" else "Uploading $done of $UploadFiles · $pct%",
                fontSize = 14.sp,
                color = colors.subText,
            )
            Spacer(Modifier.height(10.dp))
            GlassProgressBar(done / UploadFiles.toFloat(), Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    backdrop,
                    "Start upload",
                    {
                        if (!running) {
                            running = true
                            if (done >= UploadFiles) done = 0
                            uploadJob = scope.launch {
                                try {
                                    while (done < UploadFiles) {
                                        delay(350)
                                        done++
                                    }
                                } finally {
                                    running = false
                                }
                            }
                        }
                    },
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Small,
                    enabled = !running,
                )
                GlassButton(
                    backdrop,
                    "Reset",
                    {
                        uploadJob?.cancel()
                        uploadJob = null
                        running = false
                        done = 0
                    },
                    size = ButtonSize.Small,
                )
            }
            Spacer(Modifier.height(20.dp))
            GlassProgressBar(null, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PrismText("Syncing…", fontSize = 13.sp, color = colors.subText)
        }

        // ---- Spinners ----
        ShowcaseSection("Spinners") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spinner(size = 16.dp)
                Spinner(size = 20.dp)
                Spinner(size = 28.dp)
                GlassButton(backdrop, "Loading", {}, variant = ButtonVariant.Primary, loading = true)
            }
        }

        // ---- Skeleton ----
        ShowcaseSection("Skeleton", "Placeholder and content share the same geometry, so nothing shifts") {
            var loading by rememberSaveable { mutableStateOf(true) }
            val motion = LocalMotion.current
            DemoRow("Loading") {
                GlassSwitch(loading, { loading = it }, contentDescription = "Loading")
            }
            Spacer(Modifier.height(8.dp))
            DemoFrame(Modifier.height(120.dp)) {
                Crossfade(targetState = loading, animationSpec = motion.fade(220), label = "skeleton") { isLoading ->
                    if (isLoading) SkeletonCard() else ProfileCard()
                }
            }
        }

        // ---- Section loader ----
        ShowcaseSection("Section loader") {
            var simulateFailure by rememberSaveable { mutableStateOf(false) }
            var state by remember { mutableStateOf(LoadState.Loaded) }
            var loadJob by remember { mutableStateOf<Job?>(null) }
            val reload: () -> Unit = {
                loadJob?.cancel()
                state = LoadState.Loading
                loadJob = scope.launch {
                    delay(1600)
                    state = if (simulateFailure) LoadState.Error else LoadState.Loaded
                }
            }
            DemoRow("Simulate failure") {
                GlassSwitch(simulateFailure, { simulateFailure = it }, contentDescription = "Simulate failure")
            }
            Spacer(Modifier.height(8.dp))
            DemoFrame(Modifier.height(160.dp)) {
                when (state) {
                    LoadState.Loading -> SectionLoader("Loading history…", Modifier.fillMaxSize())
                    LoadState.Error -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PrismIcon(PrismIcons.Alert, null, size = 28.dp, tint = colors.error)
                        Spacer(Modifier.height(8.dp))
                        PrismText("Couldn't load history", fontSize = 15.sp)
                        Spacer(Modifier.height(10.dp))
                        GlassButton(
                            backdrop,
                            "Retry",
                            reload,
                            variant = ButtonVariant.Outlined,
                            size = ButtonSize.Small,
                        )
                    }
                    LoadState.Loaded -> Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        HistoryLikeRow("12 × 7", "84")
                        HistoryLikeRow("√144 + 3", "15")
                        HistoryLikeRow("2⁸ − 1", "255")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GlassButton(
                backdrop,
                "Reload",
                reload,
                size = ButtonSize.Small,
                enabled = state != LoadState.Loading,
            )
        }

        // ---- Dialog ----
        ShowcaseSection("Dialog") {
            var deleteOpen by rememberSaveable { mutableStateOf(false) }
            var deleting by remember { mutableStateOf(false) }
            var saveOpen by rememberSaveable { mutableStateOf(false) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassButton(
                    backdrop,
                    "Delete drafts",
                    { deleteOpen = true },
                    variant = ButtonVariant.Destructive,
                    leadingIcon = PrismIcons.Trash,
                )
                GlassButton(backdrop, "Save changes", { saveOpen = true }, variant = ButtonVariant.Primary)
            }
            ConfirmDialog(
                visible = deleteOpen,
                title = "Delete 3 drafts?",
                message = "This can't be undone.",
                confirmLabel = "Delete",
                onConfirm = {
                    if (!deleting) {
                        deleting = true
                        scope.launch {
                            try {
                                delay(1200)
                                deleteOpen = false
                                toasts.show("Drafts deleted", ToastKind.Success)
                            } finally {
                                deleting = false
                            }
                        }
                    }
                },
                onDismiss = { deleteOpen = false },
                destructive = true,
                confirmLoading = deleting,
            )
            ConfirmDialog(
                visible = saveOpen,
                title = "Save changes?",
                message = "Your edits will be kept on this device.",
                confirmLabel = "Save",
                onConfirm = {
                    saveOpen = false
                    toasts.show("Changes saved", ToastKind.Success)
                },
                onDismiss = { saveOpen = false },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---- Skeleton card geometry: both states use these exact slot sizes ----

private val AvatarSize = 44.dp
private val HeaderSlot = 20.dp
private val HeaderGap = 4.dp
private val ParagraphSlot = 14.dp
private val SectionGap = 10.dp

@Composable
private fun SkeletonCard() {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(AvatarSize), CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                SlotBox(HeaderSlot) { SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(14.dp)) }
                Spacer(Modifier.height(HeaderGap))
                SlotBox(HeaderSlot) { SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(14.dp)) }
            }
        }
        Spacer(Modifier.height(SectionGap))
        listOf(1f, 0.95f, 0.7f).forEach { w ->
            SlotBox(ParagraphSlot) { SkeletonBlock(Modifier.fillMaxWidth(w).height(12.dp)) }
        }
    }
}

@Composable
private fun ProfileCard() {
    val colors = Prism.colors
    val accent = Prism.accent
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(AvatarSize)
                    .background(accent.copy(alpha = colors.accentAlpha(0.85f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PrismText("AL", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Prism.onAccent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                SlotText(HeaderSlot, "Ada Lovelace", 15.sp, colors.text, FontWeight.Medium)
                Spacer(Modifier.height(HeaderGap))
                SlotText(HeaderSlot, "Analytical engine notes", 13.sp, colors.subText)
            }
        }
        Spacer(Modifier.height(SectionGap))
        listOf(
            "Her notes describe an algorithm for the engine",
            "to compute Bernoulli numbers, often cited as",
            "the first published computer program.",
        ).forEach { line ->
            SlotText(ParagraphSlot, line, 12.sp, colors.subText)
        }
    }
}

@Composable
private fun SlotBox(height: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.CenterStart) { content() }
}

@Composable
private fun SlotText(
    height: Dp,
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    SlotBox(height) {
        PrismText(
            text,
            // Unbounded so the glyph line box can overhang the slot without clipping; the slot keeps the geometry.
            modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryLikeRow(expression: String, result: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrismText(expression, fontSize = 14.sp, color = Prism.subText, modifier = Modifier.weight(1f))
        PrismText(result, fontSize = 20.sp, fontWeight = FontWeight.Light)
    }
}
