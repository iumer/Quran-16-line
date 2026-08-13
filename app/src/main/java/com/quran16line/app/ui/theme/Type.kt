package com.quran16line.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quran16line.app.R

val AmiriQuran = FontFamily(Font(R.font.amiri_quran))

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AmiriQuran,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = Ink
    ),
    bodyLarge = TextStyle(
        fontFamily = AmiriQuran,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Muted
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = Ink
    )
)
