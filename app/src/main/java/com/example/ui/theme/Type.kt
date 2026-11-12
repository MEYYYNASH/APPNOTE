package com.example.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FontOption
import java.io.File

val FigtreeFontFamily = FontFamily(
    Font(R.font.figtree, FontWeight.Normal),
    Font(R.font.figtree, FontWeight.Medium),
    Font(R.font.figtree, FontWeight.SemiBold),
    Font(R.font.figtree, FontWeight.Bold)
)

fun buildAppTypography(
    fontOption: FontOption,
    scale: Float,
    customFontPath: String = ""
): Typography {
    val family = when (fontOption) {
        FontOption.FIGTREE -> FigtreeFontFamily
        FontOption.SERIF -> FontFamily.Serif
        FontOption.MONO -> FontFamily.Monospace
        FontOption.CUSTOM -> {
            if (customFontPath.isNotBlank()) {
                try {
                    val file = File(customFontPath)
                    if (file.exists() && file.length() > 64) {
                        val tf = Typeface.createFromFile(file)
                        FontFamily(tf)
                    } else {
                        FigtreeFontFamily
                    }
                } catch (_: Exception) {
                    FigtreeFontFamily
                }
            } else {
                FigtreeFontFamily
            }
        }
    }
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * scale).sp,
            lineHeight = (30f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (20f * scale).sp,
            lineHeight = (26f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * scale).sp,
            lineHeight = (19f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * scale).sp,
            lineHeight = (18f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * scale).sp,
            lineHeight = (15f * scale).sp
        )
    )
}
