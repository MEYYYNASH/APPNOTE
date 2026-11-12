package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.BlobBlueDark
import com.example.ui.theme.BlobBlueLight
import com.example.ui.theme.BlobCyanDark
import com.example.ui.theme.BlobCyanLight
import com.example.ui.theme.BlobVioletDark
import com.example.ui.theme.BlobVioletLight
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.ToastMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "blob_drift")

    val drift1 by if (tokens.animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "drift1"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.3f) }
    }

    val drift2 by if (tokens.animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "drift2"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.7f) }
    }

    val blueBlob = if (tokens.isDark) BlobBlueDark else BlobBlueLight
    val violetBlob = if (tokens.isDark) BlobVioletDark else BlobVioletLight
    val cyanBlob = if (tokens.isDark) BlobCyanDark else BlobCyanLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(tokens.bgStart, tokens.bgEnd),
                    start = Offset.Zero,
                    end = Offset(1400f, 2400f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val vmax = maxOf(w, h)

            val r1 = vmax * 0.46f * (1f + 0.15f * drift1)
            val c1 = Offset(
                x = w * 0.10f + vmax * 0.08f * drift1,
                y = h * 0.08f - vmax * 0.06f * drift1
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to blueBlob,
                    0.48f to blueBlob.copy(alpha = blueBlob.alpha * 0.60f),
                    0.82f to blueBlob.copy(alpha = blueBlob.alpha * 0.18f),
                    1.0f to Color.Transparent,
                    center = c1,
                    radius = r1
                ),
                radius = r1,
                center = c1
            )

            val r2 = vmax * 0.46f * (1f + 0.15f * drift2)
            val c2 = Offset(
                x = w * 0.90f - vmax * 0.08f * drift2,
                y = h * 0.90f - vmax * 0.06f * drift2
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to violetBlob,
                    0.48f to violetBlob.copy(alpha = violetBlob.alpha * 0.60f),
                    0.82f to violetBlob.copy(alpha = violetBlob.alpha * 0.18f),
                    1.0f to Color.Transparent,
                    center = c2,
                    radius = r2
                ),
                radius = r2,
                center = c2
            )

            val r3 = vmax * 0.30f * (1f + 0.15f * drift1)
            val c3 = Offset(
                x = w * 0.48f + vmax * 0.08f * drift2,
                y = h * 0.45f - vmax * 0.06f * drift1
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to cyanBlob,
                    0.48f to cyanBlob.copy(alpha = cyanBlob.alpha * 0.60f),
                    0.82f to cyanBlob.copy(alpha = cyanBlob.alpha * 0.18f),
                    1.0f to Color.Transparent,
                    center = c3,
                    radius = r3
                ),
                radius = r3,
                center = c3
            )
        }

        content()
    }
}

/**
 * Large Panel container using exact `--glass`:
 * Light: rgba(255,255,255,.5)
 * Dark: rgba(18,28,50,.5)
 */
@Composable
fun GlassOuterPane(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(tokens.paneFill),
        content = content
    )
}

/**
 * Card / Modal container using exact `--card` (or `--glass` when elevated) and `--edge`:
 * Light `--card`: rgba(255,255,255,.55)
 * Dark `--card`: rgba(255,255,255,.06)
 * Light `--edge`: rgba(255,255,255,.75)
 * Dark `--edge`: rgba(255,255,255,.13)
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    cornerRadius: Dp = 20.dp,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val fill = if (elevated) tokens.paneFill else tokens.panelFill
    val edge = borderColor ?: tokens.panelBorder

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(width = 1.dp, color = edge, shape = shape),
        content = content
    )
}

enum class BottomDockSection {
    HOME,
    CHECKLISTS,
    VAULT,
    SETTINGS
}

@Composable
fun FloatingCapsuleBottomDock(
    activeSection: BottomDockSection,
    homeLabel: String,
    checklistsLabel: String,
    vaultLabel: String,
    settingsLabel: String,
    newNoteLabel: String,
    onSelectSection: (BottomDockSection) -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current

    // Outer glass dock plate using --glass & --edge + inner dark capsule pill matching reference image 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(tokens.paneFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(36.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Dark Capsule Pill Dock
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(58.dp)
                .clip(CircleShape)
                .background(Color(0xFF151821))
                .border(1.dp, tokens.panelBorder, CircleShape)
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CapsuleDockItem(
                icon = LineIconType.HOME,
                label = homeLabel,
                selected = activeSection == BottomDockSection.HOME,
                testTag = "filter_chip_all",
                onClick = { onSelectSection(BottomDockSection.HOME) }
            )
            CapsuleDockItem(
                icon = LineIconType.CHECKLIST,
                label = checklistsLabel,
                selected = activeSection == BottomDockSection.CHECKLISTS,
                testTag = "filter_chip_checklists",
                onClick = { onSelectSection(BottomDockSection.CHECKLISTS) }
            )
            CapsuleDockItem(
                icon = LineIconType.LAYERS,
                label = vaultLabel,
                selected = activeSection == BottomDockSection.VAULT,
                testTag = "filter_chip_archive",
                onClick = { onSelectSection(BottomDockSection.VAULT) }
            )
            CapsuleDockItem(
                icon = LineIconType.HEXAGON,
                label = settingsLabel,
                selected = activeSection == BottomDockSection.SETTINGS,
                testTag = "open_settings_button",
                onClick = { onSelectSection(BottomDockSection.SETTINGS) }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Separate Circular Action Button (+ New Note)
        Box(
            modifier = Modifier
                .testTag("new_note_fab")
                .size(58.dp)
                .drawBehind {
                    drawCircle(
                        color = tokens.accentStart.copy(alpha = 0.35f),
                        radius = size.minDimension / 2f,
                        center = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
                    )
                }
                .clip(CircleShape)
                .background(tokens.accentBrush)
                .border(1.dp, tokens.panelBorder, CircleShape)
                .clickable(onClick = onNewNote)
                .semantics { contentDescription = newNoteLabel },
            contentAlignment = Alignment.Center
        ) {
            LineIcon(
                type = LineIconType.PLUS,
                tint = Color.White,
                size = 24.dp,
                strokeWidthDp = 2.2.dp
            )
        }
    }
}

@Composable
private fun CapsuleDockItem(
    icon: LineIconType,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val activePillBg = Color.White
    val activeContentColor = Color(0xFF11151E)
    val inactiveContentColor = Color.White.copy(alpha = 0.75f)

    Row(
        modifier = Modifier
            .testTag(testTag)
            .height(48.dp)
            .clip(CircleShape)
            .background(if (selected) activePillBg else Color.Transparent)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .padding(horizontal = if (selected) 16.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        LineIcon(
            type = icon,
            tint = if (selected) activeContentColor else inactiveContentColor,
            size = 19.dp
        )
        if (selected) {
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = activeContentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * IconButton using exact `--field` and `--edge`:
 * Light `--field`: rgba(255,255,255,.6), `--edge`: rgba(255,255,255,.75)
 * Dark `--field`: rgba(255,255,255,.07), `--edge`: rgba(255,255,255,.13)
 */
@Composable
fun GlassIconButton(
    icon: LineIconType,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color? = null,
    sizeDp: Dp = 42.dp,
    testTagName: String? = null
) {
    val tokens = LocalGlassTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && tokens.animationsEnabled) 0.90f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_press"
    )

    val shape = CircleShape
    val iconColor = tint ?: if (active) Color.White else tokens.textPrimary

    Box(
        modifier = modifier
            .let { if (testTagName != null) it.testTag(testTagName) else it }
            .minimumInteractiveComponentSize()
            .size(sizeDp)
            .scale(scale)
            .clip(shape)
            .then(
                if (active) {
                    Modifier.background(tokens.accentBrush)
                } else {
                    Modifier.background(tokens.fieldFill)
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> tokens.accentStart
                    active -> Color.Transparent
                    else -> tokens.panelBorder
                },
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        LineIcon(
            type = icon,
            tint = iconColor,
            size = 20.dp
        )
    }
}

@Composable
fun DynamicIslandSavePill(
    visible: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    AnimatedVisibility(
        visible = visible,
        enter = if (tokens.animationsEnabled) {
            slideInVertically(
                initialOffsetY = { -70 },
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(160))
        } else {
            fadeIn(tween(0))
        },
        exit = if (tokens.animationsEnabled) {
            slideOutVertically(
                targetOffsetY = { -70 },
                animationSpec = tween(220)
            ) + fadeOut(tween(160))
        } else {
            fadeOut(tween(0))
        },
        modifier = modifier.testTag("dynamic_island_save_pill")
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UndoToastBar(
    toast: ToastMessage?,
    undoLabel: String,
    onUndo: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    AnimatedVisibility(
        visible = toast != null,
        enter = if (tokens.animationsEnabled) {
            slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(tween(220))
        } else {
            fadeIn(tween(0))
        },
        exit = if (tokens.animationsEnabled) {
            slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(tween(200))
        } else {
            fadeOut(tween(0))
        },
        modifier = modifier.testTag("toast_bar")
    ) {
        if (toast != null) {
            GlassPanel(
                elevated = true,
                cornerRadius = 16.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = toast.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (toast.undoNoteId != null) {
                        Text(
                            text = undoLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .testTag("toast_undo_button")
                                .clickable { onUndo(toast.undoNoteId) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            LineIcon(
                                type = LineIconType.CLOSE,
                                tint = tokens.textSecondary,
                                size = 15.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatNoteTime(timestamp: Long): String {
    val now = Date()
    val d = Date(timestamp)
    val fmtDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return if (fmtDay.format(now) == fmtDay.format(d)) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(d)
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(d)
    }
}

fun formatFullTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
}
