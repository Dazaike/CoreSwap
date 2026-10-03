package com.coreswap.app.ui.showcase

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.coreswap.app.ui.theme.Prism
import com.coreswap.app.ui.theme.PrismText

/** Titled group of demos on a showcase page. */
@Composable
fun ShowcaseSection(title: String, caption: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = Prism.colors
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(28.dp))
        PrismText(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.subText)
        if (caption != null) {
            Spacer(Modifier.height(2.dp))
            PrismText(caption, fontSize = 13.sp, color = colors.subText)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Label (and optional caption) on the left, a control on the right. */
@Composable
fun DemoRow(label: String, caption: String? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            PrismText(label, fontSize = 16.sp)
            if (caption != null) PrismText(caption, fontSize = 13.sp, color = Prism.subText)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

/** Outlined (not glass) frame, so glass inside it never shows a nesting hole. */
@Composable
fun DemoFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedRectangle(24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .border(1.dp, Prism.colors.outline, shape)
            .padding(12.dp),
        content = content,
    )
}
