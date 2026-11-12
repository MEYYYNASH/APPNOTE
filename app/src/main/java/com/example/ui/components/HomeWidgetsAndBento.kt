package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChecklistItem
import com.example.model.FilterTab
import com.example.model.Note
import com.example.model.NoteColor
import com.example.model.NoteFolder
import com.example.ui.theme.LocalGlassTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val INSPIRATION_QUOTES = listOf(
    "Dream big,\nwrite more.",
    "Small notes,\nbig ideas.",
    "Capture clarity\nevery day.",
    "Create your\nfuture today."
)

@Composable
fun InteractiveGlassWidgetsStrip(
    notes: List<Note>,
    isKhmer: Boolean,
    onQuickNoteClick: () -> Unit,
    onUpcomingReminderClick: (Note?) -> Unit,
    onStatisticsClick: () -> Unit,
    onPomodoroComplete: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeNotesCount = notes.count { !it.isTrashed }
    val upcomingReminderNote = remember(notes) {
        val now = System.currentTimeMillis()
        notes.filter { !it.isTrashed && it.reminderAt != null && it.reminderAt >= now - 3600_000L }
            .minByOrNull { it.reminderAt ?: Long.MAX_VALUE }
            ?: notes.firstOrNull { !it.isTrashed && it.title.contains("Meeting", ignoreCase = true) }
    }

    // Pomodoro live timer state
    var pomodoroRemainingSec by remember { mutableIntStateOf(25 * 60) }
    var pomodoroRunning by remember { mutableStateOf(false) }

    LaunchedEffect(pomodoroRunning) {
        while (pomodoroRunning && pomodoroRemainingSec > 0) {
            delay(1000L)
            pomodoroRemainingSec -= 1
            if (pomodoroRemainingSec == 0) {
                pomodoroRunning = false
                onPomodoroComplete()
            }
        }
    }

    var quoteIndex by remember { mutableIntStateOf(0) }
    var quoteLiked by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Widget 1: Quick Note
        FrostedWidgetCard(
            modifier = Modifier
                .width(196.dp)
                .height(94.dp)
                .testTag("widget_quick_note"),
            gradientColors = listOf(
                Color(0xFFEDE7FF).copy(alpha = 0.92f),
                Color(0xFFDCD3FF).copy(alpha = 0.86f)
            ),
            borderColor = Color(0xFFB39DFF),
            onClick = onQuickNoteClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Purple Feather Circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color(0xFF8B5CF6))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFA855F7), Color(0xFF6366F1))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.82f, h * 0.16f)
                            cubicTo(w * 0.50f, h * 0.16f, w * 0.24f, h * 0.42f, w * 0.20f, h * 0.82f)
                            cubicTo(w * 0.58f, h * 0.76f, w * 0.84f, h * 0.50f, w * 0.82f, h * 0.16f)
                            close()
                        }
                        drawPath(path, Color.White.copy(alpha = 0.95f))
                        drawLine(
                            color = Color(0xFF5B21B6),
                            start = Offset(w * 0.16f, h * 0.86f),
                            end = Offset(w * 0.68f, h * 0.34f),
                            strokeWidth = 2.2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKhmer) "កំណត់ត្រារហ័ស" else "Quick Note",
                        color = Color(0xFF1E1B4B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFC4B5FD).copy(alpha = 0.45f))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isKhmer) "ចុចដើម្បីសរសេរ..." else "Tap to write...",
                            color = Color(0xFF4C1D95).copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "›",
                            color = Color(0xFF5B21B6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Widget 2: Upcoming Reminder
        FrostedWidgetCard(
            modifier = Modifier
                .width(208.dp)
                .height(94.dp)
                .testTag("widget_upcoming_reminder"),
            gradientColors = listOf(
                Color(0xFFEDE8FF).copy(alpha = 0.92f),
                Color(0xFFE0D7FF).copy(alpha = 0.86f)
            ),
            borderColor = Color(0xFFB39DFF),
            onClick = { onUpcomingReminderClick(upcomingReminderNote) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Red Bell Icon
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(34.dp)) {
                        val w = size.width
                        val h = size.height
                        val bellPath = Path().apply {
                            moveTo(w * 0.50f, h * 0.12f)
                            cubicTo(w * 0.26f, h * 0.12f, w * 0.22f, h * 0.42f, w * 0.16f, h * 0.74f)
                            lineTo(w * 0.84f, h * 0.74f)
                            cubicTo(w * 0.78f, h * 0.42f, w * 0.74f, h * 0.12f, w * 0.50f, h * 0.12f)
                            close()
                        }
                        drawPath(
                            path = bellPath,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFF6B8B), Color(0xFFEF4444), Color(0xFFDC2626))
                            )
                        )
                        drawRoundRect(
                            color = Color(0xFFEF4444),
                            topLeft = Offset(w * 0.12f, h * 0.70f),
                            size = Size(w * 0.76f, h * 0.10f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawCircle(
                            color = Color(0xFFB91C1C),
                            radius = w * 0.10f,
                            center = Offset(w * 0.50f, h * 0.85f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKhmer) "ការរំលឹកបន្ទាប់" else "Upcoming Reminder",
                        color = Color(0xFF312E81),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = upcomingReminderNote?.title?.ifBlank { "Team Meeting" } ?: "Team Meeting",
                        color = Color(0xFF1E1B4B),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val reminderTimeText = remember(upcomingReminderNote) {
                        val ts = upcomingReminderNote?.reminderAt
                        if (ts != null) {
                            val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                            "Today • ${fmt.format(Date(ts))}"
                        } else {
                            "Today • 10:00 AM"
                        }
                    }
                    Text(
                        text = reminderTimeText,
                        color = Color(0xFF4C1D95).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                // Mini purple calendar icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF7C3AED).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    LineCalendarIcon(color = Color(0xFF6D28D9), modifier = Modifier.size(16.dp))
                }
            }
        }

        // Widget 3: Note Statistics
        FrostedWidgetCard(
            modifier = Modifier
                .width(212.dp)
                .height(94.dp)
                .testTag("widget_note_statistics"),
            gradientColors = listOf(
                Color(0xFFEAE6FF).copy(alpha = 0.92f),
                Color(0xFFDDD6FE).copy(alpha = 0.86f)
            ),
            borderColor = Color(0xFFB39DFF),
            onClick = onStatisticsClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Blue Bar Chart Icon
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        drawRoundRect(
                            color = Color(0xFF3B82F6),
                            topLeft = Offset(w * 0.10f, h * 0.45f),
                            size = Size(w * 0.20f, h * 0.48f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color(0xFF2563EB),
                            topLeft = Offset(w * 0.40f, h * 0.25f),
                            size = Size(w * 0.20f, h * 0.68f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color(0xFF1D4ED8),
                            topLeft = Offset(w * 0.70f, h * 0.10f),
                            size = Size(w * 0.20f, h * 0.83f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKhmer) "ស្ថិតិកំណត់ត្រា" else "Note Statistics",
                        color = Color(0xFF1E1B4B),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeNotesCount.coerceAtLeast(1).toString(),
                                color = Color(0xFF1E1B4B),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "↑ +12%",
                                color = Color(0xFF059669),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isKhmer) "កំណត់ត្រាសរុប" else "Total Notes",
                            color = Color(0xFF4C1D95).copy(alpha = 0.8f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Sparkline chart
                    Canvas(
                        modifier = Modifier
                            .width(84.dp)
                            .height(34.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val pts = listOf(
                            Offset(0f, h * 0.85f),
                            Offset(w * 0.22f, h * 0.58f),
                            Offset(w * 0.42f, h * 0.68f),
                            Offset(w * 0.64f, h * 0.36f),
                            Offset(w * 0.82f, h * 0.44f),
                            Offset(w, h * 0.12f)
                        )
                        val linePath = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                lineTo(pts[i].x, pts[i].y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF8B5CF6).copy(alpha = 0.38f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = Color(0xFF7C3AED),
                            style = Stroke(width = 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }

        // Widget 4: Pomodoro Focus
        FrostedWidgetCard(
            modifier = Modifier
                .width(204.dp)
                .height(94.dp)
                .testTag("widget_pomodoro_focus"),
            gradientColors = listOf(
                Color(0xFFF3E8FF).copy(alpha = 0.92f),
                Color(0xFFFCE7F3).copy(alpha = 0.88f)
            ),
            borderColor = Color(0xFFD8B4FE),
            onClick = { pomodoroRunning = !pomodoroRunning }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tomato Icon
                Canvas(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            pomodoroRunning = false
                            pomodoroRemainingSec = 25 * 60
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF7A7A), Color(0xFFEF4444), Color(0xFFB91C1C)),
                            center = Offset(w * 0.4f, h * 0.4f)
                        ),
                        radius = w * 0.42f,
                        center = Offset(w * 0.5f, h * 0.56f)
                    )
                    // Green stem leaves
                    val leafPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.14f)
                        lineTo(w * 0.34f, h * 0.28f)
                        lineTo(w * 0.5f, h * 0.24f)
                        lineTo(w * 0.66f, h * 0.28f)
                        close()
                    }
                    drawPath(leafPath, Color(0xFF16A34A))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKhmer) "ផ្តោតអារម្មណ៍" else "Pomodoro Focus",
                        color = Color(0xFF1E1B4B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val mm = pomodoroRemainingSec / 60
                    val ss = pomodoroRemainingSec % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mm, ss),
                        color = Color(0xFF1E1B4B),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                // Purple Play/Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(6.dp, CircleShape, spotColor = Color(0xFF7C3AED))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF9333EA), Color(0xFF6366F1))
                            )
                        )
                        .clickable { pomodoroRunning = !pomodoroRunning }
                        .testTag("pomodoro_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (pomodoroRunning) "❚❚" else "▶",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Widget 5: Inspiration Quote ("Dream big, write more.")
        FrostedWidgetCard(
            modifier = Modifier
                .width(176.dp)
                .height(94.dp)
                .testTag("widget_inspiration_quote"),
            gradientColors = listOf(
                Color(0xFFEDE9FE).copy(alpha = 0.92f),
                Color(0xFFFDE68A).copy(alpha = 0.78f)
            ),
            borderColor = Color(0xFFD8B4FE),
            onClick = { quoteIndex = (quoteIndex + 1) % INSPIRATION_QUOTES.size }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "“",
                    color = Color(0xFF6D28D9),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    text = INSPIRATION_QUOTES[quoteIndex],
                    color = Color(0xFF1E1B4B),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 6.dp)
                )
                Text(
                    text = if (quoteLiked) "♥" else "♡",
                    color = if (quoteLiked) Color(0xFFBE185D) else Color(0xFF6D28D9),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable { quoteLiked = !quoteLiked }
                )
            }
        }
    }
}

@Composable
private fun FrostedWidgetCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF7C3AED).copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.2.dp, borderColor.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        content()
    }
}

/**
 * Neon glowing filter pill tabs matching Screenshot 4:
 * [⊞ All] [📌 Pinned] [⭐ Favorites] [📁 Folders] [••• More]
 */
@Composable
fun NeonFilterPillsRow(
    activeTab: FilterTab,
    isKhmer: Boolean,
    onSelectTab: (FilterTab) -> Unit,
    onOpenFoldersModal: () -> Unit,
    onOpenDocumentScanner: () -> Unit,
    onToggleAdvancedFilters: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. All
        NeonFilterPillItem(
            selected = activeTab == FilterTab.ALL,
            label = if (isKhmer) FilterTab.ALL.labelKm else "All",
            tag = "neon_tab_all",
            onClick = { onSelectTab(FilterTab.ALL) },
            leadingIcon = {
                // 2x2 grid icon
                Canvas(modifier = Modifier.size(15.dp)) {
                    val s = size.width * 0.40f
                    val gap = size.width * 0.16f
                    drawRoundRect(Color.White, Offset(0f, 0f), Size(s, s), CornerRadius(3f, 3f))
                    drawRoundRect(Color.White, Offset(s + gap, 0f), Size(s, s), CornerRadius(3f, 3f))
                    drawRoundRect(Color.White, Offset(0f, s + gap), Size(s, s), CornerRadius(3f, 3f))
                    drawRoundRect(Color.White, Offset(s + gap, s + gap), Size(s, s), CornerRadius(3f, 3f))
                }
            }
        )

        // 2. Pinned
        NeonFilterPillItem(
            selected = activeTab == FilterTab.PINNED,
            label = if (isKhmer) FilterTab.PINNED.labelKm else "Pinned",
            tag = "neon_tab_pinned",
            onClick = { onSelectTab(FilterTab.PINNED) },
            leadingIcon = {
                LinePinIcon(
                    color = Color(0xFFD8B4FE),
                    filled = true,
                    modifier = Modifier.size(15.dp)
                )
            }
        )

        // 3. Favorites
        NeonFilterPillItem(
            selected = activeTab == FilterTab.FAVORITES,
            label = if (isKhmer) FilterTab.FAVORITES.labelKm else "Favorites",
            tag = "neon_tab_favorites",
            onClick = { onSelectTab(FilterTab.FAVORITES) },
            leadingIcon = {
                Canvas(modifier = Modifier.size(16.dp)) {
                    val w = size.width
                    val h = size.height
                    val star = Path().apply {
                        moveTo(w * 0.50f, h * 0.06f)
                        lineTo(w * 0.63f, h * 0.35f)
                        lineTo(w * 0.95f, h * 0.38f)
                        lineTo(w * 0.71f, h * 0.60f)
                        lineTo(w * 0.78f, h * 0.92f)
                        lineTo(w * 0.50f, h * 0.75f)
                        lineTo(w * 0.22f, h * 0.92f)
                        lineTo(w * 0.29f, h * 0.60f)
                        lineTo(w * 0.05f, h * 0.38f)
                        lineTo(w * 0.37f, h * 0.35f)
                        close()
                    }
                    drawPath(star, Color(0xFFFBBF24))
                }
            }
        )

        // 4. Folders
        NeonFilterPillItem(
            selected = activeTab == FilterTab.FOLDERS,
            label = if (isKhmer) FilterTab.FOLDERS.labelKm else "Folders",
            tag = "neon_tab_folders",
            onClick = {
                onSelectTab(FilterTab.FOLDERS)
                onOpenFoldersModal()
            },
            leadingIcon = {
                LineFolderIcon(
                    color = Color(0xFFC4B5FD),
                    modifier = Modifier.size(15.dp)
                )
            }
        )

        // 5. More (...)
        Box {
            val isSecondaryActive = activeTab == FilterTab.CHECKLISTS ||
                activeTab == FilterTab.ARCHIVE ||
                activeTab == FilterTab.TRASH
            NeonFilterPillItem(
                selected = isSecondaryActive,
                label = if (isKhmer) "ផ្សេងទៀត" else "More",
                tag = "neon_tab_more",
                onClick = { showMoreMenu = true },
                leadingIcon = {
                    Text(
                        text = "•••",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            )
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (isKhmer) "បញ្ជីកិច្ចការ (Checklists)" else "Checklists") },
                    onClick = {
                        showMoreMenu = false
                        onSelectTab(FilterTab.CHECKLISTS)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isKhmer) "គ្រប់គ្រងថតឯកសារ (Folder Manager)" else "Folder Manager") },
                    onClick = {
                        showMoreMenu = false
                        onOpenFoldersModal()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isKhmer) "ស្កេនឯកសារ (Document Scanner)" else "Document Scanner & OCR") },
                    onClick = {
                        showMoreMenu = false
                        onOpenDocumentScanner()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isKhmer) "បណ្ណសារ (Archive)" else "Archived Notes") },
                    onClick = {
                        showMoreMenu = false
                        onSelectTab(FilterTab.ARCHIVE)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isKhmer) "ធុងសម្រាម (Trash)" else "Trash (Restore Notes)") },
                    onClick = {
                        showMoreMenu = false
                        onSelectTab(FilterTab.TRASH)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isKhmer) "តម្រងកម្រិតខ្ពស់" else "Advanced Search Filters") },
                    onClick = {
                        showMoreMenu = false
                        onToggleAdvancedFilters()
                    }
                )
            }
        }
    }
}

@Composable
private fun NeonFilterPillItem(
    selected: Boolean,
    label: String,
    tag: String,
    onClick: () -> Unit,
    leadingIcon: @Composable () -> Unit
) {
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF9333EA),
                Color(0xFF4F46E5),
                Color(0xFF06B6D4)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF1E1B4B).copy(alpha = 0.72f),
                Color(0xFF1E1B4B).copy(alpha = 0.58f)
            )
        )
    }
    val borderBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(Color(0xFFF0ABFC), Color(0xFF38BDF8))
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.12f))
        )
    }

    Row(
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier.shadow(12.dp, RoundedCornerShape(50), spotColor = Color(0xFF8B5CF6))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(50))
            .background(bgBrush)
            .border(1.3.dp, borderBrush, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        leadingIcon()
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

/**
 * Glowing 2-column Neon Bento Note Card matching Screenshot 4:
 * - Corner Ribbon Badge (Pin, Star, Calendar, Heart)
 * - Top-right `⋮` overflow action menu
 * - Interactive purple checklist items (`✔ Milk`, `✔ Bread`, `○ Coffee`) or rich text preview
 * - Custom decorative illustration on the right (Laptop, Shopping Cart, Meeting People, Smiling Emoji with Hearts)
 * - Bottom clock icon + formatted timestamp (`Today • 10:24 AM`)
 */
@Composable
fun NeonBentoNoteCard(
    note: Note,
    isSelected: Boolean,
    hidePreview: Boolean,
    availableFolders: List<String>,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onMoveToFolder: (String) -> Unit,
    onCopyToFolder: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFolderSubMenu by remember { mutableStateOf(false) }
    var copyMode by remember { mutableStateOf(false) }

    // Determine card neon theme from note color or title
    val cardPalette = remember(note.color, note.title) {
        when {
            note.title.contains("Shopping", ignoreCase = true) || note.color == NoteColor.ORANGE -> BentoPalette(
                bgStart = Color(0xFF1F1D36),
                bgEnd = Color(0xFF312E81),
                glowBorderStart = Color(0xFFF59E0B),
                glowBorderEnd = Color(0xFF8B5CF6),
                ribbonColor = Color(0xFFF59E0B),
                ribbonType = "star",
                illustrationType = "cart"
            )
            note.title.contains("Meeting", ignoreCase = true) || note.color == NoteColor.GREEN -> BentoPalette(
                bgStart = Color(0xFF064E3B).copy(alpha = 0.85f),
                bgEnd = Color(0xFF0F172A),
                glowBorderStart = Color(0xFF10B981),
                glowBorderEnd = Color(0xFF06B6D4),
                ribbonColor = Color(0xFF10B981),
                ribbonType = "calendar",
                illustrationType = "people"
            )
            note.title.contains("Journal", ignoreCase = true) || note.color == NoteColor.PINK -> BentoPalette(
                bgStart = Color(0xFF4C1D95).copy(alpha = 0.85f),
                bgEnd = Color(0xFF500724).copy(alpha = 0.88f),
                glowBorderStart = Color(0xFFF43F5E),
                glowBorderEnd = Color(0xFFD946EF),
                ribbonColor = Color(0xFFF43F5E),
                ribbonType = "heart",
                illustrationType = "emoji"
            )
            else -> BentoPalette(
                bgStart = Color(0xFF1E1B4B),
                bgEnd = Color(0xFF3B0764),
                glowBorderStart = Color(0xFF8B5CF6),
                glowBorderEnd = Color(0xFFEC4899),
                ribbonColor = Color(0xFF8B5CF6),
                ribbonType = "pin",
                illustrationType = "laptop"
            )
        }
    }

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(186.dp)
            .shadow(
                elevation = if (isSelected) 16.dp else 10.dp,
                shape = shape,
                spotColor = cardPalette.glowBorderStart
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(cardPalette.bgStart, cardPalette.bgEnd),
                    start = Offset(0f, 0f),
                    end = Offset(500f, 500f)
                )
            )
            .border(
                width = if (isSelected) 2.2.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        cardPalette.glowBorderStart.copy(alpha = if (isSelected) 1f else 0.82f),
                        cardPalette.glowBorderEnd.copy(alpha = if (isSelected) 1f else 0.75f)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .testTag("bento_note_card_${note.id}")
    ) {
        // Subtle ambient radial glow in bottom-right
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cardPalette.glowBorderEnd.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.72f),
                    radius = size.width * 0.55f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.72f),
                radius = size.width * 0.55f
            )
        }

        // Right-side decorative illustration
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp, top = 18.dp)
                .size(72.dp)
        ) {
            BentoCardIllustration(type = cardPalette.illustrationType)
        }

        // Top-left Ribbon Badge
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(28.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(cardPalette.ribbonColor, cardPalette.ribbonColor.copy(alpha = 0.78f))
                    )
                )
                .clickable {
                    if (cardPalette.ribbonType == "star" || cardPalette.ribbonType == "heart") {
                        onToggleFavorite()
                    } else {
                        onTogglePin()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when (cardPalette.ribbonType) {
                "star" -> Text("★", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                "calendar" -> LineCalendarIcon(color = Color.White, modifier = Modifier.size(14.dp))
                "heart" -> Text("♥", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                else -> LinePinIcon(color = Color.White, filled = true, modifier = Modifier.size(14.dp))
            }
        }

        // Top-right 3-dots menu button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { showMenu = true }
                    .testTag("bento_card_menu_${note.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⋮",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isFavorite) "Remove from Favorites" else "Add to Favorites ★") },
                    onClick = {
                        showMenu = false
                        onToggleFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move to Folder...") },
                    onClick = {
                        showMenu = false
                        copyMode = false
                        showFolderSubMenu = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy to Folder...") },
                    onClick = {
                        showMenu = false
                        copyMode = true
                        showFolderSubMenu = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate Note") },
                    onClick = {
                        showMenu = false
                        onDuplicate()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move to Trash", color = Color(0xFFEF4444)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }

            DropdownMenu(
                expanded = showFolderSubMenu,
                onDismissRequest = { showFolderSubMenu = false }
            ) {
                val foldersList = (listOf("Personal", "Projects", "Work", "Journal") + availableFolders).distinct()
                foldersList.forEach { folderName ->
                    DropdownMenuItem(
                        text = { Text("📁 $folderName") },
                        onClick = {
                            showFolderSubMenu = false
                            if (copyMode) onCopyToFolder(folderName) else onMoveToFolder(folderName)
                        }
                    )
                }
            }
        }

        // Main Card Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 14.dp, top = 40.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.76f)) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (hidePreview) {
                    Text(
                        text = "••••••••••••",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp
                    )
                } else if (note.isChecklist && note.checklist.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        note.checklist.take(3).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .clickable { onToggleChecklistItem(item.id) }
                                    .testTag("bento_check_${note.id}_${item.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isDone) Color(0xFF7C3AED) else Color.Transparent
                                        )
                                        .border(
                                            1.5.dp,
                                            if (item.isDone) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.55f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.isDone) {
                                        Text(
                                            text = "✓",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = item.text.ifBlank { "Item" },
                                    color = if (item.isDone) Color.White.copy(alpha = 0.82f) else Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    val cleanPreview = remember(note.body) {
                        note.body
                            .replace(Regex("^[#*>-]+\\s*", RegexOption.MULTILINE), "")
                            .replace("**", "")
                            .replace("`", "")
                            .trim()
                    }
                    Text(
                        text = cleanPreview.ifBlank { "Tap to write..." },
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.5.sp,
                        lineHeight = 16.5.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Timestamp Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                LineClockIcon(
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = formatBentoTimestamp(note.updatedAt),
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

private data class BentoPalette(
    val bgStart: Color,
    val bgEnd: Color,
    val glowBorderStart: Color,
    val glowBorderEnd: Color,
    val ribbonColor: Color,
    val ribbonType: String,
    val illustrationType: String
)

@Composable
private fun BentoCardIllustration(type: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (type) {
            "laptop" -> {
                // Sleek glowing laptop screen + keyboard base
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6), Color(0xFFEC4899))
                    ),
                    topLeft = Offset(w * 0.14f, h * 0.18f),
                    size = Size(w * 0.72f, h * 0.48f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.14f, h * 0.18f),
                    size = Size(w * 0.72f, h * 0.48f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2.5f)
                )
                // Base
                val basePath = Path().apply {
                    moveTo(w * 0.04f, h * 0.68f)
                    lineTo(w * 0.96f, h * 0.68f)
                    lineTo(w * 0.88f, h * 0.76f)
                    lineTo(w * 0.12f, h * 0.76f)
                    close()
                }
                drawPath(basePath, Color(0xFFE2E8F0))
            }
            "cart" -> {
                // Neon lavender shopping cart with golden sparkles
                val stroke = Stroke(width = 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val cartPath = Path().apply {
                    moveTo(w * 0.10f, h * 0.24f)
                    lineTo(w * 0.24f, h * 0.24f)
                    lineTo(w * 0.34f, h * 0.62f)
                    lineTo(w * 0.80f, h * 0.62f)
                    lineTo(w * 0.88f, h * 0.34f)
                    lineTo(w * 0.27f, h * 0.34f)
                }
                drawPath(cartPath, Color(0xFFC4B5FD), style = stroke)
                drawCircle(Color(0xFFC4B5FD), radius = w * 0.06f, center = Offset(w * 0.40f, h * 0.74f))
                drawCircle(Color(0xFFC4B5FD), radius = w * 0.06f, center = Offset(w * 0.74f, h * 0.74f))
                // Sparkles above cart
                drawLine(Color(0xFFFBBF24), Offset(w * 0.62f, h * 0.16f), Offset(w * 0.66f, h * 0.08f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFBBF24), Offset(w * 0.76f, h * 0.20f), Offset(w * 0.84f, h * 0.14f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            }
            "people" -> {
                // Two translucent team silhouettes
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = 0.85f),
                    radius = w * 0.13f,
                    center = Offset(w * 0.36f, h * 0.42f)
                )
                drawRoundRect(
                    color = Color(0xFF60A5FA).copy(alpha = 0.75f),
                    topLeft = Offset(w * 0.16f, h * 0.58f),
                    size = Size(w * 0.40f, h * 0.24f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawCircle(
                    color = Color(0xFFA78BFA).copy(alpha = 0.95f),
                    radius = w * 0.15f,
                    center = Offset(w * 0.68f, h * 0.36f)
                )
                drawRoundRect(
                    color = Color(0xFFA78BFA).copy(alpha = 0.90f),
                    topLeft = Offset(w * 0.44f, h * 0.54f),
                    size = Size(w * 0.46f, h * 0.28f),
                    cornerRadius = CornerRadius(20f, 20f)
                )
            }
            "emoji" -> {
                // Warm golden smiling face with floating pink hearts
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
                        center = Offset(w * 0.45f, h * 0.45f)
                    ),
                    radius = w * 0.34f,
                    center = Offset(w * 0.52f, h * 0.54f)
                )
                // Happy closed eyes
                drawArc(
                    color = Color(0xFF78350F),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.44f),
                    size = Size(w * 0.12f, h * 0.10f),
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF78350F),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.56f, h * 0.44f),
                    size = Size(w * 0.12f, h * 0.10f),
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
                // Smile
                drawArc(
                    color = Color(0xFF78350F),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.40f, h * 0.52f),
                    size = Size(w * 0.24f, h * 0.16f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
                // Floating hearts
                drawCircle(Color(0xFFF43F5E), radius = w * 0.07f, center = Offset(w * 0.84f, h * 0.28f))
                drawCircle(Color(0xFFEC4899), radius = w * 0.06f, center = Offset(w * 0.20f, h * 0.76f))
            }
        }
    }
}

/**
 * Bottom Floating Action Dock matching Screenshot 4:
 * Left pill: `[📁 Personal ⌄ | 📊 24 Notes]`
 * Right button: Glowing `[+ New Note]`
 */
@Composable
fun NeonBentoBottomBar(
    activeFolder: String,
    totalNotesCount: Int,
    availableFolders: List<String>,
    isKhmer: Boolean,
    onSelectFolder: (String) -> Unit,
    onOpenFoldersManager: () -> Unit,
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFolderDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Glass Pill: [📁 Personal ⌄ | 📊 24 Notes]
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(10.dp, RoundedCornerShape(50), spotColor = Color(0xFF6366F1).copy(alpha = 0.35f))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1E1B4B).copy(alpha = 0.90f),
                            Color(0xFF312E81).copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color(0xFFA78BFA).copy(alpha = 0.45f))
                    ),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showFolderDropdown = true }
                        .testTag("bottom_dock_folder_picker"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LineFolderIcon(
                        color = Color(0xFFC4B5FD),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = activeFolder.ifBlank { if (isKhmer) "ផ្ទាល់ខ្លួន" else "Personal" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "⌄",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = showFolderDropdown,
                    onDismissRequest = { showFolderDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isKhmer) "គ្រប់ថតទាំងអស់ (All Folders)" else "All Folders") },
                        onClick = {
                            showFolderDropdown = false
                            onSelectFolder("")
                        }
                    )
                    val folders = (listOf("Personal", "Projects", "Work", "Journal") + availableFolders).distinct()
                    folders.forEach { folderName ->
                        DropdownMenuItem(
                            text = { Text("📁 $folderName") },
                            onClick = {
                                showFolderDropdown = false
                                onSelectFolder(folderName)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (isKhmer) "⚙️ គ្រប់គ្រងថតឯកសារ..." else "⚙️ Manage Folders...") },
                        onClick = {
                            showFolderDropdown = false
                            onOpenFoldersManager()
                        }
                    )
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            )

            // Stats count
            Row(
                modifier = Modifier.clickable { onOpenFoldersManager() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Canvas(modifier = Modifier.size(15.dp)) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Color(0xFFC084FC), Offset(0f, h * 0.5f), Size(w * 0.24f, h * 0.5f), CornerRadius(2f, 2f))
                    drawRoundRect(Color(0xFFA855F7), Offset(w * 0.36f, h * 0.28f), Size(w * 0.24f, h * 0.72f), CornerRadius(2f, 2f))
                    drawRoundRect(Color(0xFFEC4899), Offset(w * 0.72f, h * 0.08f), Size(w * 0.24f, h * 0.92f), CornerRadius(2f, 2f))
                }
                Text(
                    text = "$totalNotesCount ${if (isKhmer) "កំណត់ត្រា" else "Notes"}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Right Glowing [+ New Note] Pill Button
        Row(
            modifier = Modifier
                .height(52.dp)
                .shadow(16.dp, RoundedCornerShape(50), spotColor = Color(0xFF8B5CF6))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFA855F7),
                            Color(0xFF6366F1),
                            Color(0xFF2563EB)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0xFFF0ABFC), Color(0xFF67E8F9))
                    ),
                    RoundedCornerShape(50)
                )
                .clickable(onClick = onNewNoteClick)
                .padding(horizontal = 20.dp)
                .testTag("new_note_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (isKhmer) "កំណត់ត្រាថ្មី" else "New Note",
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun formatBentoTimestamp(epochMillis: Long): String {
    val nowCal = Calendar.getInstance()
    val targetCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timePart = timeFmt.format(Date(epochMillis))

    val sameYear = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
    val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - targetCal.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> "Today • $timePart"
        sameYear && dayDiff == 1 -> "Yesterday • $timePart"
        else -> {
            val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())
            "${dateFmt.format(Date(epochMillis))} • $timePart"
        }
    }
}
