package com.coreswap.app.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.coreswap.app.R

@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: FontWeight) = Font(
    R.font.outfit_variable,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Outfit = FontFamily(
    outfit(FontWeight.ExtraLight),
    outfit(FontWeight.Light),
    outfit(FontWeight.Normal),
    outfit(FontWeight.Medium),
    outfit(FontWeight.SemiBold),
)
