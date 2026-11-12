package com.example.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LineIconType {
    HOME,
    LAYERS,
    HEXAGON,
    PLUS,
    QUICK_CAPTURE,
    SEARCH,
    FILTER,
    SETTINGS,
    SUN,
    MOON,
    BACK,
    PIN,
    CHECKLIST,
    COPY,
    DUPLICATE,
    TRASH,
    RESTORE,
    ARCHIVE,
    SHARE,
    HISTORY,
    EYE,
    MIC,
    IMAGE,
    CAMERA,
    DRAW,
    FOLDER,
    TAG,
    CHECK,
    CLOSE,
    PLAY,
    PAUSE,
    WEB_CODE,
    MORE,
    BELL,
    CROP,
    ROTATE,
    FONT,
    CLOUD_UPLOAD,
    VOLUME,
    SPEEDOMETER,
    TIMER
}

@Composable
fun LineIcon(
    type: LineIconType,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidthDp: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sx = w / 24f
        val sy = h / 24f
        val stroke = Stroke(
            width = strokeWidthDp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        fun pt(x: Float, y: Float) = Offset(x * sx, y * sy)

        when (type) {
            LineIconType.HOME -> {
                // Minimalist house with inner horizontal line (matching reference image 1)
                val p = Path().apply {
                    moveTo(4f * sx, 10.5f * sy)
                    lineTo(12f * sx, 3.8f * sy)
                    lineTo(20f * sx, 10.5f * sy)
                    lineTo(20f * sx, 19.2f * sy)
                    quadraticTo(20f * sx, 20.5f * sy, 18.5f * sx, 20.5f * sy)
                    lineTo(5.5f * sx, 20.5f * sy)
                    quadraticTo(4f * sx, 20.5f * sy, 4f * sx, 19.2f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawLine(tint, pt(9f, 16.5f), pt(15f, 16.5f), stroke.width, StrokeCap.Round)
            }
            LineIconType.LAYERS -> {
                // Stacked double-diamond layers icon (matching reference image 1)
                val topLayer = Path().apply {
                    moveTo(12f * sx, 4f * sy)
                    lineTo(20f * sx, 9f * sy)
                    lineTo(12f * sx, 14f * sy)
                    lineTo(4f * sx, 9f * sy)
                    close()
                }
                drawPath(topLayer, tint, style = stroke)
                val bottomLayer = Path().apply {
                    moveTo(4f * sx, 14f * sy)
                    lineTo(12f * sx, 19f * sy)
                    lineTo(20f * sx, 14f * sy)
                }
                drawPath(bottomLayer, tint, style = stroke)
            }
            LineIconType.HEXAGON, LineIconType.SETTINGS -> {
                // Clean geometric hexagon icon (matching reference image 1)
                val hex = Path().apply {
                    moveTo(12f * sx, 3.2f * sy)
                    lineTo(19.5f * sx, 7.6f * sy)
                    lineTo(19.5f * sx, 16.4f * sy)
                    lineTo(12f * sx, 20.8f * sy)
                    lineTo(4.5f * sx, 16.4f * sy)
                    lineTo(4.5f * sx, 7.6f * sy)
                    close()
                }
                drawPath(hex, tint, style = stroke)
                drawCircle(tint, radius = 2.5f * sx, center = pt(12f, 12f), style = stroke)
            }
            LineIconType.PLUS -> {
                drawLine(tint, pt(12f, 5.5f), pt(12f, 18.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(5.5f, 12f), pt(18.5f, 12f), stroke.width, StrokeCap.Round)
            }
            LineIconType.QUICK_CAPTURE -> {
                val p = Path().apply {
                    moveTo(13f * sx, 2.5f * sy)
                    lineTo(4.5f * sx, 13.8f * sy)
                    lineTo(11.2f * sx, 13.8f * sy)
                    lineTo(10.2f * sx, 21.5f * sy)
                    lineTo(19.5f * sx, 10.2f * sy)
                    lineTo(12.8f * sx, 10.2f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.SEARCH -> {
                drawCircle(tint, radius = 6.8f * sx, center = pt(11f, 11f), style = stroke)
                drawLine(tint, pt(19.8f, 19.8f), pt(16f, 16f), stroke.width, StrokeCap.Round)
            }
            LineIconType.FILTER -> {
                drawLine(tint, pt(4f, 6.5f), pt(20f, 6.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(7f, 12f), pt(17f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(10f, 17.5f), pt(14f, 17.5f), stroke.width, StrokeCap.Round)
            }
            LineIconType.SUN -> {
                drawCircle(tint, radius = 4f * sx, center = pt(12f, 12f), style = stroke)
                drawLine(tint, pt(12f, 2.2f), pt(12f, 4.2f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(12f, 19.8f), pt(12f, 21.8f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(2.2f, 12f), pt(4.2f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(19.8f, 12f), pt(21.8f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(5f, 5f), pt(6.5f, 6.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(17.5f, 17.5f), pt(19f, 19f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(5f, 19f), pt(6.5f, 17.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(17.5f, 6.5f), pt(19f, 5f), stroke.width, StrokeCap.Round)
            }
            LineIconType.MOON -> {
                val p = Path().apply {
                    moveTo(20.5f * sx, 13.5f * sy)
                    cubicTo(19f * sx, 18.5f * sy, 13.5f * sx, 21f * sy, 8.5f * sx, 19f * sy)
                    cubicTo(4f * sx, 17f * sy, 2.5f * sx, 11.5f * sy, 5f * sx, 7f * sy)
                    cubicTo(6.5f * sx, 4.5f * sy, 9f * sx, 3.2f * sy, 11.5f * sx, 3f * sy)
                    cubicTo(9f * sx, 6.5f * sy, 9.8f * sx, 11.5f * sy, 13.5f * sx, 14f * sy)
                    cubicTo(15.8f * sx, 15.2f * sy, 18.5f * sx, 14.8f * sy, 20.5f * sx, 13.5f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.BACK -> {
                val p = Path().apply {
                    moveTo(15f * sx, 18f * sy)
                    lineTo(9f * sx, 12f * sy)
                    lineTo(15f * sx, 6f * sy)
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.PIN -> {
                drawLine(tint, pt(12f, 17f), pt(12f, 21.5f), stroke.width, StrokeCap.Round)
                val p = Path().apply {
                    moveTo(9f * sx, 3.5f * sy)
                    lineTo(15f * sx, 3.5f * sy)
                    lineTo(14f * sx, 9f * sy)
                    lineTo(17f * sx, 12f * sy)
                    lineTo(17f * sx, 14f * sy)
                    lineTo(7f * sx, 14f * sy)
                    lineTo(7f * sx, 12f * sy)
                    lineTo(10f * sx, 9f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.CHECKLIST -> {
                // Rounded stacked pill icon (matching the center pill icon in reference image 2)
                drawRoundRect(
                    tint,
                    topLeft = pt(5f, 4.5f),
                    size = Size(14f * sx, 6.2f * sy),
                    cornerRadius = CornerRadius(3f * sx, 3f * sy),
                    style = stroke
                )
                drawRoundRect(
                    tint,
                    topLeft = pt(5f, 13.3f),
                    size = Size(14f * sx, 6.2f * sy),
                    cornerRadius = CornerRadius(3f * sx, 3f * sy),
                    style = stroke
                )
            }
            LineIconType.COPY -> {
                // Clipboard icon (distinct from DUPLICATE)
                drawRoundRect(
                    tint,
                    topLeft = pt(6f, 5f),
                    size = Size(12f * sx, 15f * sy),
                    cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy),
                    style = stroke
                )
                drawRoundRect(
                    tint,
                    topLeft = pt(9f, 3f),
                    size = Size(6f * sx, 3.5f * sy),
                    cornerRadius = CornerRadius(1.5f * sx, 1.5f * sy),
                    style = stroke
                )
                drawLine(tint, pt(9f, 11f), pt(15f, 11f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(9f, 15f), pt(13f, 15f), stroke.width, StrokeCap.Round)
            }
            LineIconType.DUPLICATE -> {
                // Overlapping twin cards with a + inside (distinct from COPY)
                drawRoundRect(
                    tint,
                    topLeft = pt(8.5f, 8.5f),
                    size = Size(11.5f * sx, 11.5f * sy),
                    cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy),
                    style = stroke
                )
                val p = Path().apply {
                    moveTo(5f * sx, 15f * sy)
                    lineTo(5f * sx, 6.5f * sy)
                    quadraticTo(5f * sx, 4.5f * sy, 7f * sx, 4.5f * sy)
                    lineTo(15.5f * sx, 4.5f * sy)
                }
                drawPath(p, tint, style = stroke)
                drawLine(tint, pt(14.2f, 11.8f), pt(14.2f, 16.6f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(11.8f, 14.2f), pt(16.6f, 14.2f), stroke.width, StrokeCap.Round)
            }
            LineIconType.TRASH -> {
                drawLine(tint, pt(4f, 7f), pt(20f, 7f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(10f, 11f), pt(10f, 17f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(14f, 11f), pt(14f, 17f), stroke.width, StrokeCap.Round)
                val body = Path().apply {
                    moveTo(6f * sx, 7f * sy)
                    lineTo(7f * sx, 20f * sy)
                    lineTo(17f * sx, 20f * sy)
                    lineTo(18f * sx, 7f * sy)
                }
                drawPath(body, tint, style = stroke)
                val top = Path().apply {
                    moveTo(9f * sx, 7f * sy)
                    lineTo(9f * sx, 4f * sy)
                    lineTo(15f * sx, 4f * sy)
                    lineTo(15f * sx, 7f * sy)
                }
                drawPath(top, tint, style = stroke)
            }
            LineIconType.RESTORE -> {
                val p = Path().apply {
                    moveTo(6f * sx, 10f * sy)
                    lineTo(6f * sx, 5f * sy)
                    lineTo(11f * sx, 5f * sy)
                }
                drawPath(p, tint, style = stroke)
                drawArc(
                    color = tint,
                    startAngle = -140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = pt(5f, 5f),
                    size = Size(14f * sx, 14f * sy),
                    style = stroke
                )
            }
            LineIconType.ARCHIVE -> {
                drawRoundRect(
                    tint,
                    topLeft = pt(3f, 4f),
                    size = Size(18f * sx, 5f * sy),
                    cornerRadius = CornerRadius(1.5f * sx, 1.5f * sy),
                    style = stroke
                )
                val p = Path().apply {
                    moveTo(5f * sx, 9f * sy)
                    lineTo(5f * sx, 19f * sy)
                    lineTo(19f * sx, 19f * sy)
                    lineTo(19f * sx, 9f * sy)
                }
                drawPath(p, tint, style = stroke)
                drawLine(tint, pt(10f, 13f), pt(14f, 13f), stroke.width, StrokeCap.Round)
            }
            LineIconType.SHARE -> {
                drawCircle(tint, radius = 2.4f * sx, center = pt(17.5f, 5.5f), style = stroke)
                drawCircle(tint, radius = 2.4f * sx, center = pt(6.5f, 12f), style = stroke)
                drawCircle(tint, radius = 2.4f * sx, center = pt(17.5f, 18.5f), style = stroke)
                drawLine(tint, pt(8.6f, 10.8f), pt(15.4f, 6.7f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(8.6f, 13.2f), pt(15.4f, 17.3f), stroke.width, StrokeCap.Round)
            }
            LineIconType.HISTORY -> {
                drawCircle(tint, radius = 8f * sx, center = pt(12f, 12f), style = stroke)
                val p = Path().apply {
                    moveTo(12f * sx, 7.5f * sy)
                    lineTo(12f * sx, 12f * sy)
                    lineTo(15.5f * sx, 14f * sy)
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.EYE -> {
                val p = Path().apply {
                    moveTo(3f * sx, 12f * sy)
                    quadraticTo(12f * sx, 4.5f * sy, 21f * sx, 12f * sy)
                    quadraticTo(12f * sx, 19.5f * sy, 3f * sx, 12f * sy)
                }
                drawPath(p, tint, style = stroke)
                drawCircle(tint, radius = 2.6f * sx, center = pt(12f, 12f), style = stroke)
            }
            LineIconType.MIC -> {
                drawRoundRect(
                    tint,
                    topLeft = pt(9f, 3.8f),
                    size = Size(6f * sx, 9.5f * sy),
                    cornerRadius = CornerRadius(3f * sx, 3f * sy),
                    style = stroke
                )
                drawArc(
                    tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = pt(6f, 7.5f),
                    size = Size(12f * sx, 9.5f * sy),
                    style = stroke
                )
                drawLine(tint, pt(12f, 17f), pt(12f, 20.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(8.5f, 20.5f), pt(15.5f, 20.5f), stroke.width, StrokeCap.Round)
            }
            LineIconType.IMAGE -> {
                drawRoundRect(
                    tint,
                    topLeft = pt(4f, 5f),
                    size = Size(16f * sx, 14f * sy),
                    cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy),
                    style = stroke
                )
                drawCircle(tint, radius = 1.5f * sx, center = pt(8.5f, 9f), style = stroke)
                val p = Path().apply {
                    moveTo(5f * sx, 17.5f * sy)
                    lineTo(10.5f * sx, 12.5f * sy)
                    lineTo(14.5f * sx, 16f * sy)
                    lineTo(17.5f * sx, 13.5f * sy)
                    lineTo(19.5f * sx, 17f * sy)
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.CAMERA -> {
                drawRoundRect(
                    tint,
                    topLeft = pt(4f, 7f),
                    size = Size(16f * sx, 12f * sy),
                    cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy),
                    style = stroke
                )
                drawCircle(tint, radius = 3.2f * sx, center = pt(12f, 13f), style = stroke)
                drawLine(tint, pt(9f, 4.5f), pt(15f, 4.5f), stroke.width, StrokeCap.Round)
            }
            LineIconType.DRAW -> {
                val p = Path().apply {
                    moveTo(16.5f * sx, 4f * sy)
                    lineTo(20f * sx, 7.5f * sy)
                    lineTo(8f * sx, 19.5f * sy)
                    lineTo(4f * sx, 20f * sy)
                    lineTo(4.5f * sx, 16f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.FOLDER -> {
                val p = Path().apply {
                    moveTo(4f * sx, 6f * sy)
                    lineTo(9.5f * sx, 6f * sy)
                    lineTo(11.5f * sx, 8.5f * sy)
                    lineTo(20f * sx, 8.5f * sy)
                    lineTo(20f * sx, 18.5f * sy)
                    lineTo(4f * sx, 18.5f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.TAG -> {
                val p = Path().apply {
                    moveTo(5f * sx, 5f * sy)
                    lineTo(12.5f * sx, 5f * sy)
                    lineTo(19.5f * sx, 12f * sy)
                    lineTo(12f * sx, 19.5f * sy)
                    lineTo(5f * sx, 12.5f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawCircle(tint, radius = 1f * sx, center = pt(8.5f, 8.5f), style = stroke)
            }
            LineIconType.CHECK -> {
                val p = Path().apply {
                    moveTo(5f * sx, 12f * sy)
                    lineTo(10f * sx, 17f * sy)
                    lineTo(19f * sx, 7f * sy)
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.CLOSE -> {
                drawLine(tint, pt(6f, 6f), pt(18f, 18f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(18f, 6f), pt(6f, 18f), stroke.width, StrokeCap.Round)
            }
            LineIconType.PLAY -> {
                val p = Path().apply {
                    moveTo(8f * sx, 6f * sy)
                    lineTo(18f * sx, 12f * sy)
                    lineTo(8f * sx, 18f * sy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            LineIconType.PAUSE -> {
                drawLine(tint, pt(9f, 6f), pt(9f, 18f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(15f, 6f), pt(15f, 18f), stroke.width, StrokeCap.Round)
            }
            LineIconType.WEB_CODE -> {
                val left = Path().apply {
                    moveTo(8.5f * sx, 7f * sy)
                    lineTo(4f * sx, 12f * sy)
                    lineTo(8.5f * sx, 17f * sy)
                }
                val right = Path().apply {
                    moveTo(15.5f * sx, 7f * sy)
                    lineTo(20f * sx, 12f * sy)
                    lineTo(15.5f * sx, 17f * sy)
                }
                drawPath(left, tint, style = stroke)
                drawPath(right, tint, style = stroke)
                drawLine(tint, pt(13.2f, 5.5f), pt(10.8f, 18.5f), stroke.width, StrokeCap.Round)
            }
            LineIconType.MORE -> {
                drawCircle(tint, radius = 1.4f * sx, center = pt(6f, 12f), style = stroke)
                drawCircle(tint, radius = 1.4f * sx, center = pt(12f, 12f), style = stroke)
                drawCircle(tint, radius = 1.4f * sx, center = pt(18f, 12f), style = stroke)
            }
            LineIconType.BELL -> {
                val bell = Path().apply {
                    moveTo(18f * sx, 9.5f * sy)
                    cubicTo(18f * sx, 6f * sy, 15.3f * sx, 3.8f * sy, 12f * sx, 3.8f * sy)
                    cubicTo(8.7f * sx, 3.8f * sy, 6f * sx, 6f * sy, 6f * sx, 9.5f * sy)
                    lineTo(6f * sx, 14.5f * sy)
                    lineTo(4.2f * sx, 16.8f * sy)
                    lineTo(19.8f * sx, 16.8f * sy)
                    lineTo(18f * sx, 14.5f * sy)
                    close()
                }
                drawPath(bell, tint, style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = pt(9.8f, 17.2f),
                    size = Size(4.4f * sx, 3.5f * sy),
                    style = stroke
                )
            }
            LineIconType.CROP -> {
                val l1 = Path().apply {
                    moveTo(6.5f * sx, 3f * sy)
                    lineTo(6.5f * sx, 17.5f * sy)
                    lineTo(21f * sx, 17.5f * sy)
                }
                val l2 = Path().apply {
                    moveTo(3f * sx, 6.5f * sy)
                    lineTo(17.5f * sx, 6.5f * sy)
                    lineTo(17.5f * sx, 21f * sy)
                }
                drawPath(l1, tint, style = stroke)
                drawPath(l2, tint, style = stroke)
            }
            LineIconType.ROTATE -> {
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = pt(5f, 5f),
                    size = Size(14f * sx, 14f * sy),
                    style = stroke
                )
                val arrow = Path().apply {
                    moveTo(12f * sx, 2.5f * sy)
                    lineTo(15.5f * sx, 5.2f * sy)
                    lineTo(12f * sx, 7.8f * sy)
                }
                drawPath(arrow, tint, style = stroke)
            }
            LineIconType.FONT -> {
                drawLine(tint, pt(5f, 5.5f), pt(19f, 5.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(5f, 5.5f), pt(5f, 8.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(19f, 5.5f), pt(19f, 8.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(12f, 5.5f), pt(12f, 19f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(8.5f, 19f), pt(15.5f, 19f), stroke.width, StrokeCap.Round)
            }
            LineIconType.CLOUD_UPLOAD -> {
                val cloud = Path().apply {
                    moveTo(6.5f * sx, 18f * sy)
                    cubicTo(3.8f * sx, 18f * sy, 2.5f * sx, 15.8f * sy, 3.2f * sx, 13.5f * sy)
                    cubicTo(3.8f * sx, 11.5f * sy, 5.5f * sx, 10.5f * sy, 7f * sx, 10.5f * sy)
                    cubicTo(7.8f * sx, 7f * sy, 11f * sx, 5f * sy, 14.2f * sx, 6f * sy)
                    cubicTo(17f * sx, 6.8f * sy, 18.5f * sx, 9.5f * sy, 18.2f * sx, 11.8f * sy)
                    cubicTo(20.2f * sx, 12.2f * sy, 21.2f * sx, 14.2f * sy, 20.5f * sx, 16.2f * sy)
                    cubicTo(19.8f * sx, 17.5f * sy, 18.5f * sx, 18f * sy, 17f * sx, 18f * sy)
                }
                drawPath(cloud, tint, style = stroke)
                drawLine(tint, pt(12f, 19.5f), pt(12f, 11.5f), stroke.width, StrokeCap.Round)
                val up = Path().apply {
                    moveTo(9.2f * sx, 14.2f * sy)
                    lineTo(12f * sx, 11.2f * sy)
                    lineTo(14.8f * sx, 14.2f * sy)
                }
                drawPath(up, tint, style = stroke)
            }
            LineIconType.VOLUME -> {
                val spk = Path().apply {
                    moveTo(4.5f * sx, 9.5f * sy)
                    lineTo(8.5f * sx, 9.5f * sy)
                    lineTo(13.5f * sx, 5.5f * sy)
                    lineTo(13.5f * sx, 18.5f * sy)
                    lineTo(8.5f * sx, 14.5f * sy)
                    lineTo(4.5f * sx, 14.5f * sy)
                    close()
                }
                drawPath(spk, tint, style = stroke)
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = pt(13.5f, 8.5f),
                    size = Size(5f * sx, 7f * sy),
                    style = stroke
                )
            }
            LineIconType.SPEEDOMETER -> {
                // Outer speedometer gauge arc (from bottom-left to bottom-right)
                drawArc(
                    color = tint,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = pt(3.5f, 3.5f),
                    size = Size(17f * sx, 17f * sy),
                    style = stroke
                )
                // Speed indicator needle pointing high-speed towards upper-right
                drawLine(tint, pt(12f, 13f), pt(16.5f, 8.5f), stroke.width * 1.15f, StrokeCap.Round)
                // Center pivot dot
                drawCircle(tint, radius = 2.2f * sx, center = pt(12f, 13f))
                // Bottom indicator tick marks
                drawLine(tint, pt(6.5f, 16.5f), pt(8f, 15f), stroke.width * 0.8f, StrokeCap.Round)
                drawLine(tint, pt(17.5f, 16.5f), pt(16f, 15f), stroke.width * 0.8f, StrokeCap.Round)
            }
            LineIconType.TIMER -> {
                // Clock/Timer face
                drawCircle(tint, radius = 8f * sx, center = pt(12f, 13f), style = stroke)
                // Top stopwatch pusher
                drawLine(tint, pt(12f, 2.5f), pt(12f, 5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(9.5f, 2.5f), pt(14.5f, 2.5f), stroke.width, StrokeCap.Round)
                // Clock hands (hour & minute indicating elapsed/remaining time)
                drawLine(tint, pt(12f, 13f), pt(12f, 8.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, pt(12f, 13f), pt(15.5f, 13f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

