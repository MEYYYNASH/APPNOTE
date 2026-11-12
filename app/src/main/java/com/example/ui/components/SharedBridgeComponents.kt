package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens

@Composable
fun GlassPillButton(
    text: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: (@Composable (Color) -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current
    val contentColor = if (selected) Color.White else tokens.textPrimary
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(accentColor, accentColor.copy(alpha = 0.78f))
        )
    } else {
        Brush.horizontalGradient(
            listOf(tokens.fieldFill, tokens.fieldFill)
        )
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (selected) accentColor else tokens.panelBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon(contentColor)
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun LineCalendarIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.22f),
            size = Size(w * 0.72f, h * 0.64f),
            cornerRadius = CornerRadius(4f, 4f),
            style = stroke
        )
        drawLine(color, Offset(w * 0.32f, h * 0.12f), Offset(w * 0.32f, h * 0.28f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.12f), Offset(w * 0.68f, h * 0.28f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(w * 0.14f, h * 0.42f), Offset(w * 0.86f, h * 0.42f), stroke.width, StrokeCap.Round)
    }
}

@Composable
fun LinePinIcon(color: Color, filled: Boolean = false, modifier: Modifier = Modifier) {
    LineIcon(type = LineIconType.PIN, tint = color, modifier = modifier, size = 15.dp)
}

@Composable
fun LineFolderIcon(color: Color, modifier: Modifier = Modifier) {
    LineIcon(type = LineIconType.FOLDER, tint = color, modifier = modifier, size = 16.dp)
}

@Composable
fun LineClockIcon(color: Color, modifier: Modifier = Modifier) {
    LineIcon(type = LineIconType.HISTORY, tint = color, modifier = modifier, size = 13.dp)
}

@Composable
fun LineCloseIcon(color: Color, modifier: Modifier = Modifier) {
    LineIcon(type = LineIconType.CLOSE, tint = color, modifier = modifier, size = 18.dp)
}
