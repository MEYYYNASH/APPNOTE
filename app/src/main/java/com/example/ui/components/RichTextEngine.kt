package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatColorReset
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.EditorFontStyle
import com.example.model.RichTextSpan
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.FigtreeFontFamily
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.resolveEditorFontFamily
import kotlin.math.max
import kotlin.math.min

data class TypingFormatState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val highlightColorArgb: Long? = null,
    val textColorArgb: Long? = null,
    val fontSizeSp: Float? = null,
    val fontStyleId: String? = null,
    val headingLevel: Int = 0,
    val isQuote: Boolean = false,
    val isCode: Boolean = false
) {
    fun hasAnyFormatting(): Boolean {
        return isBold ||
            isItalic ||
            isUnderline ||
            isStrikethrough ||
            highlightColorArgb != null ||
            textColorArgb != null ||
            fontSizeSp != null ||
            !fontStyleId.isNullOrBlank() ||
            headingLevel > 0 ||
            isQuote ||
            isCode
    }

    fun toSpan(start: Int, end: Int): RichTextSpan = RichTextSpan(
        start = start,
        end = end,
        isBold = isBold,
        isItalic = isItalic,
        isUnderline = isUnderline,
        isStrikethrough = isStrikethrough,
        highlightColorArgb = highlightColorArgb,
        textColorArgb = textColorArgb,
        fontSizeSp = fontSizeSp,
        fontStyleId = fontStyleId,
        headingLevel = headingLevel,
        isQuote = isQuote,
        isCode = isCode
    )
}

fun RichTextSpan.toTypingState(): TypingFormatState = TypingFormatState(
    isBold = isBold,
    isItalic = isItalic,
    isUnderline = isUnderline,
    isStrikethrough = isStrikethrough,
    highlightColorArgb = highlightColorArgb,
    textColorArgb = textColorArgb,
    fontSizeSp = fontSizeSp,
    fontStyleId = fontStyleId,
    headingLevel = headingLevel,
    isQuote = isQuote,
    isCode = isCode
)

fun RichTextSpan.sameStyleAs(other: RichTextSpan): Boolean {
    return isBold == other.isBold &&
        isItalic == other.isItalic &&
        isUnderline == other.isUnderline &&
        isStrikethrough == other.isStrikethrough &&
        highlightColorArgb == other.highlightColorArgb &&
        textColorArgb == other.textColorArgb &&
        fontSizeSp == other.fontSizeSp &&
        fontStyleId == other.fontStyleId &&
        headingLevel == other.headingLevel &&
        isQuote == other.isQuote &&
        isCode == other.isCode &&
        linkUrl == other.linkUrl
}

fun normalizeAndCompactSpans(
    spans: List<RichTextSpan>,
    textLength: Int
): List<RichTextSpan> {
    if (textLength <= 0 || spans.isEmpty()) return emptyList()
    val valid = spans.mapNotNull { s ->
        val st = s.start.coerceIn(0, textLength)
        val en = s.end.coerceIn(0, textLength)
        if (st < en && s.hasAnyFormatting()) s.copy(start = st, end = en) else null
    }
    if (valid.isEmpty()) return emptyList()

    val boundaries = sortedSetOf<Int>()
    valid.forEach {
        boundaries.add(it.start)
        boundaries.add(it.end)
    }
    val pts = boundaries.toList()
    val mergedSlices = mutableListOf<RichTextSpan>()

    for (i in 0 until pts.size - 1) {
        val segStart = pts[i]
        val segEnd = pts[i + 1]
        if (segStart >= segEnd) continue
        val covering = valid.filter { it.start <= segStart && it.end >= segEnd }
        if (covering.isEmpty()) continue
        var combined = RichTextSpan(start = segStart, end = segEnd)
        covering.forEach { layer ->
            combined = combined.copy(
                isBold = combined.isBold || layer.isBold,
                isItalic = combined.isItalic || layer.isItalic,
                isUnderline = combined.isUnderline || layer.isUnderline,
                isStrikethrough = combined.isStrikethrough || layer.isStrikethrough,
                highlightColorArgb = layer.highlightColorArgb ?: combined.highlightColorArgb,
                textColorArgb = layer.textColorArgb ?: combined.textColorArgb,
                fontSizeSp = layer.fontSizeSp ?: combined.fontSizeSp,
                fontStyleId = layer.fontStyleId ?: combined.fontStyleId,
                headingLevel = if (layer.headingLevel > 0) layer.headingLevel else combined.headingLevel,
                isQuote = combined.isQuote || layer.isQuote,
                isCode = combined.isCode || layer.isCode,
                linkUrl = layer.linkUrl ?: combined.linkUrl
            )
        }
        if (combined.hasAnyFormatting()) {
            val prev = mergedSlices.lastOrNull()
            if (prev != null && prev.end == combined.start && prev.sameStyleAs(combined)) {
                mergedSlices[mergedSlices.lastIndex] = prev.copy(end = combined.end)
            } else {
                mergedSlices.add(combined)
            }
        }
    }
    return mergedSlices
}

fun applySpanFormat(
    textLength: Int,
    rangeStart: Int,
    rangeEnd: Int,
    spans: List<RichTextSpan>,
    transform: (RichTextSpan) -> RichTextSpan
): List<RichTextSpan> {
    if (textLength <= 0) return emptyList()
    val targetStart = min(rangeStart, rangeEnd).coerceIn(0, textLength)
    val targetEnd = max(rangeStart, rangeEnd).coerceIn(0, textLength)
    if (targetStart >= targetEnd) return normalizeAndCompactSpans(spans, textLength)

    val normalized = normalizeAndCompactSpans(spans, textLength)
    val pts = sortedSetOf<Int>().apply {
        add(targetStart)
        add(targetEnd)
        normalized.forEach {
            add(it.start)
            add(it.end)
        }
    }.toList()

    val result = mutableListOf<RichTextSpan>()
    for (i in 0 until pts.size - 1) {
        val segStart = pts[i]
        val segEnd = pts[i + 1]
        if (segStart >= segEnd) continue
        val base = normalized.firstOrNull { it.start <= segStart && it.end >= segEnd }
            ?.copy(start = segStart, end = segEnd)
            ?: RichTextSpan(start = segStart, end = segEnd)
        val updated = if (segStart >= targetStart && segEnd <= targetEnd) {
            transform(base).copy(start = segStart, end = segEnd)
        } else {
            base
        }
        if (updated.hasAnyFormatting()) {
            result.add(updated)
        }
    }
    return normalizeAndCompactSpans(result, textLength)
}

fun adjustSpansForTextEdit(
    oldText: String,
    newText: String,
    spans: List<RichTextSpan>,
    typingStyle: TypingFormatState
): List<RichTextSpan> {
    if (newText.isEmpty()) return emptyList()
    if (oldText == newText) return normalizeAndCompactSpans(spans, newText.length)

    var prefix = 0
    val minLen = min(oldText.length, newText.length)
    while (prefix < minLen && oldText[prefix] == newText[prefix]) {
        prefix++
    }

    var oldSuffix = oldText.length
    var newSuffix = newText.length
    while (oldSuffix > prefix && newSuffix > prefix && oldText[oldSuffix - 1] == newText[newSuffix - 1]) {
        oldSuffix--
        newSuffix--
    }

    val delStart = prefix
    val delEnd = oldSuffix
    val insLen = newSuffix - prefix
    val delta = newText.length - oldText.length

    val adjusted = spans.mapNotNull { span ->
        val ns = when {
            span.end <= delStart -> span.start
            span.start >= delEnd -> span.start + delta
            span.start < delStart -> span.start
            else -> delStart
        }
        val ne = when {
            span.end <= delStart -> span.end
            span.start >= delEnd -> span.end + delta
            span.end > delEnd -> span.end + delta
            else -> delStart + insLen
        }
        val clampedStart = ns.coerceIn(0, newText.length)
        val clampedEnd = ne.coerceIn(0, newText.length)
        if (clampedStart < clampedEnd && span.hasAnyFormatting()) {
            span.copy(start = clampedStart, end = clampedEnd)
        } else {
            null
        }
    }.toMutableList()

    if (insLen > 0) {
        val insStart = delStart
        val insEnd = insStart + insLen
        val stripped = adjusted.flatMap { s ->
            when {
                s.end <= insStart || s.start >= insEnd -> listOf(s)
                else -> buildList {
                    if (s.start < insStart) add(s.copy(end = insStart))
                    if (s.end > insEnd) add(s.copy(start = insEnd))
                }
            }
        }.toMutableList()
        if (typingStyle.hasAnyFormatting()) {
            stripped.add(typingStyle.toSpan(insStart, insEnd))
        }
        return normalizeAndCompactSpans(stripped, newText.length)
    }

    return normalizeAndCompactSpans(adjusted, newText.length)
}

fun currentParagraphRange(text: String, selection: TextRange): IntRange {
    if (text.isEmpty()) return 0..0
    val selMin = min(selection.start, selection.end).coerceIn(0, text.length)
    val selMax = max(selection.start, selection.end).coerceIn(0, text.length)
    var lineStart = selMin
    while (lineStart > 0 && text[lineStart - 1] != '\n') {
        lineStart--
    }
    var lineEnd = if (selMax > selMin && selMax > 0 && text.getOrNull(selMax - 1) == '\n') {
        selMax - 1
    } else {
        selMax
    }
    while (lineEnd < text.length && text[lineEnd] != '\n') {
        lineEnd++
    }
    return lineStart until lineEnd
}

fun toggleLinePrefixInSelection(
    text: String,
    selection: TextRange,
    spans: List<RichTextSpan>,
    numbered: Boolean
): Triple<String, TextRange, List<RichTextSpan>> {
    val selEnd = max(selection.start, selection.end).coerceIn(0, text.length)
    val range = currentParagraphRange(text, selection)
    val blockStart = range.first
    val blockEndExclusive = (range.last + 1).coerceIn(blockStart, text.length)
    val blockText = text.substring(blockStart, blockEndExclusive)
    val lines = blockText.split("\n")

    val allHavePrefix = lines.all { line ->
        val t = line.trimStart()
        if (numbered) t.matches(Regex("^\\d+\\.\\s.*")) else t.startsWith("• ")
    }

    var indexOffset = 1
    val updatedLines = lines.map { line ->
        if (allHavePrefix) {
            if (numbered) line.replaceFirst(Regex("^\\s*\\d+\\.\\s"), "")
            else line.replaceFirst(Regex("^\\s*•\\s"), "")
        } else {
            val cleaned = line
                .replaceFirst(Regex("^\\s*•\\s"), "")
                .replaceFirst(Regex("^\\s*\\d+\\.\\s"), "")
            if (numbered) "${indexOffset++}. $cleaned" else "• $cleaned"
        }
    }

    val replacement = updatedLines.joinToString("\n")
    val newText = text.substring(0, blockStart) + replacement + text.substring(blockEndExclusive)
    val newSpans = adjustSpansForTextEdit(text, newText, spans, TypingFormatState())
    val cursorDelta = replacement.length - blockText.length
    val newSel = if (selection.collapsed) {
        TextRange((selEnd + cursorDelta).coerceIn(0, newText.length))
    } else {
        TextRange(blockStart, (blockStart + replacement.length).coerceIn(0, newText.length))
    }
    return Triple(newText, newSel, newSpans)
}

fun resolveFormatStateAt(
    spans: List<RichTextSpan>,
    selection: TextRange
): TypingFormatState {
    if (spans.isEmpty()) return TypingFormatState()
    return if (selection.collapsed) {
        val cursor = selection.start
        val match = spans.firstOrNull { cursor > it.start && cursor <= it.end }
            ?: spans.firstOrNull { cursor >= it.start && cursor < it.end }
        match?.toTypingState() ?: TypingFormatState()
    } else {
        val selStart = min(selection.start, selection.end)
        val selEnd = max(selection.start, selection.end)
        val intersecting = spans.filter { it.end > selStart && it.start < selEnd }
        if (intersecting.isEmpty()) return TypingFormatState()
        val first = intersecting.first()
        TypingFormatState(
            isBold = intersecting.all { it.isBold },
            isItalic = intersecting.all { it.isItalic },
            isUnderline = intersecting.all { it.isUnderline },
            isStrikethrough = intersecting.all { it.isStrikethrough },
            highlightColorArgb = if (intersecting.all { it.highlightColorArgb == first.highlightColorArgb }) first.highlightColorArgb else null,
            textColorArgb = if (intersecting.all { it.textColorArgb == first.textColorArgb }) first.textColorArgb else null,
            fontSizeSp = if (intersecting.all { it.fontSizeSp == first.fontSizeSp }) first.fontSizeSp else null,
            fontStyleId = if (intersecting.all { it.fontStyleId == first.fontStyleId }) first.fontStyleId else null,
            headingLevel = if (intersecting.all { it.headingLevel == first.headingLevel }) first.headingLevel else 0,
            isQuote = intersecting.all { it.isQuote },
            isCode = intersecting.all { it.isCode }
        )
    }
}

fun buildRichAnnotatedString(
    text: String,
    spans: List<RichTextSpan>,
    baseFontFamily: FontFamily,
    baseFontSizeSp: Float,
    baseColor: Color,
    accentColor: Color
): AnnotatedString {
    val cleanSpans = normalizeAndCompactSpans(spans, text.length)
    return buildAnnotatedString {
        append(text)
        cleanSpans.forEach { span ->
            val st = span.start.coerceIn(0, text.length)
            val en = span.end.coerceIn(0, text.length)
            if (st < en) {
                val decorations = buildList {
                    if (span.isUnderline || !span.linkUrl.isNullOrBlank()) add(TextDecoration.Underline)
                    if (span.isStrikethrough) add(TextDecoration.LineThrough)
                }
                val textDecoration = when {
                    decorations.isEmpty() -> null
                    decorations.size == 1 -> decorations.first()
                    else -> TextDecoration.combine(decorations)
                }
                val effectiveSize = when {
                    span.headingLevel == 1 -> (baseFontSizeSp * 1.55f).sp
                    span.headingLevel == 2 -> (baseFontSizeSp * 1.30f).sp
                    span.headingLevel == 3 -> (baseFontSizeSp * 1.15f).sp
                    span.fontSizeSp != null -> span.fontSizeSp.sp
                    else -> androidx.compose.ui.unit.TextUnit.Unspecified
                }
                val effectiveWeight = when {
                    span.headingLevel == 1 -> FontWeight.ExtraBold
                    span.headingLevel == 2 || span.isBold -> FontWeight.Bold
                    span.headingLevel == 3 -> FontWeight.SemiBold
                    else -> null
                }
                val effectiveColor = when {
                    span.textColorArgb != null -> Color(span.textColorArgb)
                    !span.linkUrl.isNullOrBlank() -> accentColor
                    span.isQuote -> accentColor.copy(alpha = 0.92f)
                    else -> baseColor
                }
                val effectiveBg = when {
                    span.highlightColorArgb != null -> Color(span.highlightColorArgb).copy(alpha = 0.36f)
                    span.isCode -> accentColor.copy(alpha = 0.16f)
                    span.isQuote -> accentColor.copy(alpha = 0.10f)
                    else -> Color.Unspecified
                }
                val effectiveFont = when {
                    !span.fontStyleId.isNullOrBlank() -> resolveEditorFontFamily(span.fontStyleId, baseFontFamily)
                    span.isCode -> RobotoMonoFontFamily
                    else -> null
                }
                addStyle(
                    style = SpanStyle(
                        color = effectiveColor,
                        fontSize = effectiveSize,
                        fontWeight = effectiveWeight,
                        fontStyle = if (span.isItalic || span.isQuote) FontStyle.Italic else null,
                        fontFamily = effectiveFont,
                        background = effectiveBg,
                        textDecoration = textDecoration
                    ),
                    start = st,
                    end = en
                )
            }
        }
    }
}

class RichTextVisualTransformation(
    private val spans: List<RichTextSpan>,
    private val baseFontFamily: FontFamily,
    private val baseFontSizeSp: Float,
    private val baseColor: Color,
    private val accentColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = buildRichAnnotatedString(
            text = text.text,
            spans = spans,
            baseFontFamily = baseFontFamily,
            baseFontSizeSp = baseFontSizeSp,
            baseColor = baseColor,
            accentColor = accentColor
        )
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

private const val DEFAULT_HIGHLIGHT_ARGB: Long = 0xFFFACC15L

/**
 * Converts legacy markdown syntax tags (# Heading, **bold**, *italic*, ~~strike~~,
 * ==highlight==, <u>underline</u>, [text](url), #style#) into clean WYSIWYG text + RichTextSpans.
 */
fun convertLegacyMarkdownToRichText(
    rawBody: String,
    existingSpans: List<RichTextSpan>
): Pair<String, List<RichTextSpan>> {
    if (existingSpans.isNotEmpty() || rawBody.isEmpty()) {
        return rawBody to existingSpans
    }
    val hasLegacyMarkup = rawBody.contains("**") ||
        rawBody.contains("==") ||
        rawBody.contains("<u>") ||
        rawBody.contains("~~") ||
        rawBody.contains("```") ||
        rawBody.contains("#style#", ignoreCase = true) ||
        rawBody.contains("#stlye#", ignoreCase = true) ||
        Regex("(?m)^#{1,3}\\s+").containsMatchIn(rawBody) ||
        Regex("(?m)^>\\s+").containsMatchIn(rawBody) ||
        Regex("(?m)^\\|\\s*-+\\s*\\|").containsMatchIn(rawBody) ||
        Regex("\\[[^\\]]+]\\([^)]+\\)").containsMatchIn(rawBody)
    if (!hasLegacyMarkup) return rawBody to existingSpans

    val outText = StringBuilder()
    val outSpans = mutableListOf<RichTextSpan>()
    val cleanedInput = rawBody
        .replace("#style#", "", ignoreCase = true)
        .replace("#stlye#", "", ignoreCase = true)
    val lines = cleanedInput.split("\n")
    var insideCodeFence = false

    lines.forEachIndexed { lineIdx, rawLine ->
        val trimmed = rawLine.trim()
        if (trimmed.startsWith("```")) {
            insideCodeFence = !insideCodeFence
            return@forEachIndexed
        }
        // Skip markdown table divider rows like | --- | --- |
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            val cells = trimmed.trim('|').split("|").map { it.trim() }
            if (cells.isNotEmpty() && cells.all { c -> c.isNotEmpty() && c.all { ch -> ch == '-' || ch == ':' || ch == ' ' } }) {
                return@forEachIndexed
            }
        }

        if (outText.isNotEmpty()) {
            outText.append('\n')
        }

        if (insideCodeFence) {
            val startCode = outText.length
            outText.append(rawLine)
            if (startCode < outText.length) {
                outSpans.add(RichTextSpan(startCode, outText.length, isCode = true))
            }
            return@forEachIndexed
        }

        var line = rawLine
        var heading = 0
        var quote = false
        var isTableRow = false
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            val cells = trimmed.trim('|').split("|").map { it.trim() }
            if (cells.size >= 2) {
                line = cells.joinToString("   •   ")
                isTableRow = true
            }
        }
        if (!isTableRow) {
            when {
                line.startsWith("### ") -> {
                    heading = 3
                    line = line.removePrefix("### ")
                }
                line.startsWith("## ") -> {
                    heading = 2
                    line = line.removePrefix("## ")
                }
                line.startsWith("# ") -> {
                    heading = 1
                    line = line.removePrefix("# ")
                }
                line.startsWith("> ") -> {
                    quote = true
                    line = line.removePrefix("> ")
                }
                line.startsWith("- ") -> {
                    line = "• " + line.removePrefix("- ")
                }
            }
        }
        val lineStartOut = outText.length
        var i = 0
        while (i < line.length) {
            when {
                line.startsWith("**", i) -> {
                    val end = line.indexOf("**", i + 2)
                    if (end != -1) {
                        val s = outText.length
                        outText.append(line, i + 2, end)
                        outSpans.add(RichTextSpan(s, outText.length, isBold = true, headingLevel = heading, isQuote = quote))
                        i = end + 2
                    } else {
                        outText.append(line[i])
                        i++
                    }
                }
                line.startsWith("==", i) -> {
                    val end = line.indexOf("==", i + 2)
                    if (end != -1) {
                        val s = outText.length
                        outText.append(line, i + 2, end)
                        outSpans.add(RichTextSpan(s, outText.length, highlightColorArgb = DEFAULT_HIGHLIGHT_ARGB, headingLevel = heading, isQuote = quote))
                        i = end + 2
                    } else {
                        outText.append(line[i])
                        i++
                    }
                }
                line.startsWith("<u>", i) -> {
                    val end = line.indexOf("</u>", i + 3)
                    if (end != -1) {
                        val s = outText.length
                        outText.append(line, i + 3, end)
                        outSpans.add(RichTextSpan(s, outText.length, isUnderline = true, headingLevel = heading, isQuote = quote))
                        i = end + 4
                    } else {
                        outText.append(line[i])
                        i++
                    }
                }
                line.startsWith("~~", i) -> {
                    val end = line.indexOf("~~", i + 2)
                    if (end != -1) {
                        val s = outText.length
                        outText.append(line, i + 2, end)
                        outSpans.add(RichTextSpan(s, outText.length, isStrikethrough = true, headingLevel = heading, isQuote = quote))
                        i = end + 2
                    } else {
                        outText.append(line[i])
                        i++
                    }
                }
                line.startsWith("*", i) && !line.startsWith("* ", i) -> {
                    val end = line.indexOf("*", i + 1)
                    if (end != -1) {
                        val s = outText.length
                        outText.append(line, i + 1, end)
                        outSpans.add(RichTextSpan(s, outText.length, isItalic = true, headingLevel = heading, isQuote = quote))
                        i = end + 1
                    } else {
                        outText.append(line[i])
                        i++
                    }
                }
                line.startsWith("[", i) -> {
                    val mid = line.indexOf("](", i + 1)
                    val end = if (mid != -1) line.indexOf(")", mid + 2) else -1
                    if (mid != -1 && end != -1) {
                        val label = line.substring(i + 1, mid)
                        val url = line.substring(mid + 2, end)
                        val s = outText.length
                        outText.append(label)
                        outSpans.add(RichTextSpan(s, outText.length, linkUrl = url, headingLevel = heading, isQuote = quote))
                        i = end + 1
                    } else {
                        outText.append(line[i])
                        i++
                    }
                }
                else -> {
                    outText.append(line[i])
                    i++
                }
            }
        }
        val lineEndOut = outText.length
        if ((heading > 0 || quote) && lineStartOut < lineEndOut) {
            outSpans.add(RichTextSpan(lineStartOut, lineEndOut, headingLevel = heading, isQuote = quote))
        }
        if (isTableRow && lineStartOut < lineEndOut && lines.getOrNull(lineIdx + 1)?.trim()?.contains("---") == true) {
            outSpans.add(RichTextSpan(lineStartOut, lineEndOut, isBold = true))
        }
    }

    val finalStr = outText.toString()
    return finalStr to normalizeAndCompactSpans(outSpans, finalStr.length)
}

private val PRESET_COLOR_SWATCHES = listOf(
    0xFFFFFFFFL, // Pure White
    0xFFE2E8F0L, // Soft Slate
    0xFF0F172AL, // Deep Ink
    0xFF38BDF8L, // Sky Blue
    0xFF60A5FAL, // Electric Blue
    0xFFA855F7L, // Vivid Violet
    0xFFEC4899L, // Rose Pink
    0xFFF43F5EL, // Crimson
    0xFFFB923CL, // Coral Orange
    0xFFFACC15L, // Warm Gold
    0xFF4ADE80L, // Fresh Green
    0xFF2DD4BFL  // Teal Mint
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorPickerDialog(
    hasSelection: Boolean,
    currentTextColorArgb: Long?,
    currentHighlightArgb: Long?,
    defaultTextColor: Color,
    accentColor: Color,
    isKhmer: Boolean,
    onDismiss: () -> Unit,
    onApplyTextColor: (argb: Long?, applyToWholeNote: Boolean) -> Unit,
    onApplyHighlightColor: (argb: Long?) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var modeTab by remember { mutableIntStateOf(0) } // 0 = Text Color, 1 = Highlight Color

    var hue by remember { mutableFloatStateOf(215f) }
    var sat by remember { mutableFloatStateOf(0.8f) }
    var bri by remember { mutableFloatStateOf(0.95f) }

    LaunchedEffect(modeTab, currentTextColorArgb, currentHighlightArgb) {
        val targetColor = Color(
            if (modeTab == 0) (currentTextColorArgb ?: accentColor.toArgb().toLong())
            else (currentHighlightArgb ?: DEFAULT_HIGHLIGHT_ARGB)
        )
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(targetColor.toArgb(), hsv)
        hue = hsv[0]
        sat = hsv[1]
        bri = hsv[2]
    }

    val customColorInt = remember(hue, sat, bri) {
        AndroidColor.HSVToColor(floatArrayOf(hue, sat, bri))
    }
    val customColorArgb = remember(customColorInt) {
        customColorInt.toLong() and 0xFFFFFFFFL
    }
    val customColor = Color(customColorInt)
    var hexInput by remember(customColorArgb) {
        mutableStateOf(String.format("#%06X", (customColorArgb and 0xFFFFFFL).toInt()))
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_color_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isKhmer) "ជ្រើសរើសពណ៌ផ្ទាល់ខ្លួន" else "Custom Text & Highlight Color",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = "Close",
                        sizeDp = 34.dp,
                        onClick = onDismiss
                    )
                }

                // Mode toggle: Text Color vs Highlight Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPillButton(
                        text = if (isKhmer) "ពណ៌អក្សរ" else "Text Color",
                        selected = modeTab == 0,
                        accentColor = accentColor,
                        leadingIcon = { tint ->
                            Icon(Icons.Rounded.FormatColorText, null, tint = tint, modifier = Modifier.size(14.dp))
                        },
                        onClick = { modeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    GlassPillButton(
                        text = if (isKhmer) "ពណ៌បន្លិច" else "Highlight Color",
                        selected = modeTab == 1,
                        accentColor = accentColor,
                        leadingIcon = { tint ->
                            Icon(Icons.Rounded.Highlight, null, tint = tint, modifier = Modifier.size(14.dp))
                        },
                        onClick = { modeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Curated Quick Swatches + Reset
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset to Theme Default swatch
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, CircleShape)
                            .clickable {
                                if (modeTab == 0) {
                                    onApplyTextColor(null, !hasSelection)
                                } else {
                                    onApplyHighlightColor(null)
                                }
                                onDismiss()
                            }
                            .testTag("color_swatch_reset"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatColorReset,
                            contentDescription = "Default color",
                            tint = tokens.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    PRESET_COLOR_SWATCHES.forEach { argb ->
                        val c = Color(argb)
                        val isCurrent = customColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) Color.White else tokens.panelBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(c.toArgb(), hsv)
                                    hue = hsv[0]
                                    sat = hsv[1]
                                    bri = hsv[2]
                                }
                                .testTag("color_swatch_${java.lang.Long.toHexString(argb)}")
                        )
                    }
                }

                // Hue Rainbow Spectrum Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Hue (${hue.toInt()} deg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .align(Alignment.Center)
                        ) {
                            val hueColors = listOf(
                                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(hueColors),
                                cornerRadius = CornerRadius(20f, 20f)
                            )
                        }
                        Slider(
                            value = hue,
                            onValueChange = { hue = it },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = customColor,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("color_hue_slider")
                        )
                    }
                }

                // Saturation & Brightness
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saturation (${(sat * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary
                    )
                    Slider(
                        value = sat,
                        onValueChange = { sat = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = customColor, activeTrackColor = customColor)
                    )
                    Text(
                        text = "Brightness (${(bri * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary
                    )
                    Slider(
                        value = bri,
                        onValueChange = { bri = it },
                        valueRange = 0.05f..1f,
                        colors = SliderDefaults.colors(thumbColor = customColor, activeTrackColor = customColor)
                    )
                }

                // Hex Input + Live Sample Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(customColor)
                            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .width(106.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        BasicTextField(
                            value = hexInput,
                            onValueChange = { typed ->
                                hexInput = typed
                                val clean = typed.trim().removePrefix("#")
                                if (clean.length == 6) {
                                    try {
                                        val parsed = AndroidColor.parseColor("#$clean")
                                        val hsv = FloatArray(3)
                                        AndroidColor.colorToHSV(parsed, hsv)
                                        hue = hsv[0]
                                        sat = hsv[1]
                                        bri = hsv[2]
                                    } catch (_: Exception) {
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = tokens.textPrimary,
                                fontSize = 12.5.sp,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (modeTab == 1) customColor.copy(alpha = 0.32f) else tokens.fieldFill
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Preview Sample",
                            color = if (modeTab == 0) customColor else defaultTextColor,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // Action Buttons
                if (modeTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasSelection) {
                            GlassPillButton(
                                text = "Apply to Selection",
                                selected = true,
                                accentColor = customColor,
                                onClick = {
                                    onApplyTextColor(customColorArgb, false)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("apply_color_selection_button")
                            )
                        } else {
                            GlassPillButton(
                                text = "Typing Only",
                                accentColor = accentColor,
                                onClick = {
                                    onApplyTextColor(customColorArgb, false)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            GlassPillButton(
                                text = "Apply to Note",
                                selected = true,
                                accentColor = accentColor,
                                onClick = {
                                    onApplyTextColor(customColorArgb, true)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("apply_color_note_button")
                            )
                        }
                    }
                } else {
                    GlassPillButton(
                        text = if (hasSelection) "Highlight Selection" else "Set Typing Highlight",
                        selected = true,
                        accentColor = accentColor,
                        onClick = {
                            onApplyHighlightColor(customColorArgb)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_highlight_color_button")
                    )
                }
            }
        }
    }
}

@Composable
fun FontStylePickerDialog(
    hasSelection: Boolean,
    currentNoteFontStyleId: String,
    accentColor: Color,
    isKhmer: Boolean,
    onDismiss: () -> Unit,
    onSelectFontStyle: (fontStyleId: String, applyToWholeNote: Boolean) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val activeStyleId = currentNoteFontStyleId.ifBlank { EditorFontStyle.SANS.id }
    var applyToWholeNote by remember(hasSelection) { mutableStateOf(!hasSelection) }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("font_style_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TextFields,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isKhmer) "រចនាប័ទ្មពុម្ពអក្សរ" else "Font Style",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = "Close",
                        sizeDp = 34.dp,
                        onClick = onDismiss
                    )
                }

                if (!hasSelection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassPillButton(
                            text = if (isKhmer) "ទាំងអស់" else "Whole Note",
                            selected = applyToWholeNote,
                            accentColor = accentColor,
                            onClick = { applyToWholeNote = true },
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = if (isKhmer) "អក្សរថ្មី" else "Next Typing",
                            selected = !applyToWholeNote,
                            accentColor = accentColor,
                            onClick = { applyToWholeNote = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = if (isKhmer) "អនុវត្តលើអក្សរដែលបានជ្រើស" else "Tap a font style to apply to selected text.",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary
                    )
                }

                EditorFontStyle.entries.forEach { styleOption ->
                    val isSelected = activeStyleId == styleOption.id
                    val previewFamily = resolveEditorFontFamily(styleOption.id, FigtreeFontFamily)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.16f) else tokens.fieldFill
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentColor else tokens.panelBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectFontStyle(styleOption.id, applyToWholeNote && !hasSelection)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("font_style_option_${styleOption.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKhmer) styleOption.labelKm else styleOption.labelEn,
                                fontFamily = previewFamily,
                                fontSize = if (styleOption == EditorFontStyle.HANDWRITTEN) 20.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = "The quick glass note captures every idea.",
                                fontFamily = previewFamily,
                                fontSize = if (styleOption == EditorFontStyle.HANDWRITTEN) 15.sp else 12.5.sp,
                                color = tokens.textSecondary
                            )
                        }
                        if (isSelected) {
                            LineIcon(
                                type = LineIconType.CHECK,
                                tint = accentColor,
                                size = 18.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsertLinkDialog(
    initialLabel: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (label: String, url: String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var label by remember { mutableStateOf(initialLabel) }
    var url by remember { mutableStateOf("https://") }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insert_link_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Link, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Insert Clean Link",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (label.isEmpty()) {
                        Text("Display text (e.g. Project Docs)", color = tokens.textSecondary, fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        textStyle = TextStyle(color = tokens.textPrimary, fontSize = 13.5.sp),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        textStyle = TextStyle(color = tokens.textPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPillButton(text = "Cancel", accentColor = accentColor, onClick = onDismiss)
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassPillButton(
                        text = "Insert Link",
                        selected = true,
                        accentColor = accentColor,
                        onClick = {
                            val cleanUrl = url.trim().ifBlank { "https://github.com/MEYYYNASH/APPNOTE" }
                            val cleanLabel = label.trim().ifBlank { cleanUrl.removePrefix("https://").removePrefix("http://") }
                            onConfirm(cleanLabel, cleanUrl)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InsertTableDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (rows: Int, cols: Int) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(2) }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insert_table_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.TableChart, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Insert Table Grid",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rows:", color = tokens.textSecondary, fontSize = 12.sp)
                    listOf(2, 3, 4, 5).forEach { r ->
                        GlassPillButton(
                            text = "$r",
                            selected = rows == r,
                            accentColor = accentColor,
                            onClick = { rows = r }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cols:", color = tokens.textSecondary, fontSize = 12.sp)
                    listOf(2, 3, 4).forEach { c ->
                        GlassPillButton(
                            text = "$c",
                            selected = cols == c,
                            accentColor = accentColor,
                            onClick = { cols = c }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassPillButton(text = "Cancel", accentColor = accentColor, onClick = onDismiss)
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassPillButton(
                        text = "Insert Table",
                        selected = true,
                        accentColor = accentColor,
                        onClick = {
                            onConfirm(rows, cols)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
