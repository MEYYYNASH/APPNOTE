package com.example

import androidx.compose.ui.text.TextRange
import com.example.model.RichTextSpan
import com.example.ui.components.TypingFormatState
import com.example.ui.components.adjustSpansForTextEdit
import com.example.ui.components.applySpanFormat
import com.example.ui.components.convertLegacyMarkdownToRichText
import com.example.ui.components.resolveFormatStateAt
import com.example.ui.components.toggleLinePrefixInSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextEngineTest {

    @Test
    fun `applySpanFormat applies and compacts inline spans without altering plain text`() {
        val text = "Hello Glass Notes"
        val spans1 = applySpanFormat(
            textLength = text.length,
            rangeStart = 6,
            rangeEnd = 11,
            spans = emptyList()
        ) { it.copy(isBold = true) }

        assertEquals(1, spans1.size)
        assertEquals(6, spans1[0].start)
        assertEquals(11, spans1[0].end)
        assertTrue(spans1[0].isBold)

        val spans2 = applySpanFormat(
            textLength = text.length,
            rangeStart = 6,
            rangeEnd = 11,
            spans = spans1
        ) { it.copy(textColorArgb = 0xFF38BDF8L) }

        assertEquals(1, spans2.size)
        assertTrue(spans2[0].isBold)
        assertEquals(0xFF38BDF8L, spans2[0].textColorArgb)
    }

    @Test
    fun `adjustSpansForTextEdit shifts spans on insertion and preserves typing style`() {
        val oldText = "Hello Notes"
        val initialSpans = listOf(RichTextSpan(start = 6, end = 11, isItalic = true))
        val newText = "Hello Warm Notes"

        val updated = adjustSpansForTextEdit(
            oldText = oldText,
            newText = newText,
            spans = initialSpans,
            typingStyle = TypingFormatState(isBold = true, textColorArgb = 0xFFA855F7L)
        )

        val insertedSpan = updated.first { it.start == 6 && it.end == 11 }
        assertTrue(insertedSpan.isBold)
        assertEquals(0xFFA855F7L, insertedSpan.textColorArgb)

        val shiftedItalic = updated.first { it.isItalic }
        assertEquals(11, shiftedItalic.start)
        assertEquals(16, shiftedItalic.end)
    }

    @Test
    fun `convertLegacyMarkdownToRichText strips raw markdown syntax and creates live spans`() {
        val raw = "# Heading 1\n==highlighted text==\n**bold** *italic* <u>underlined</u> ~~strike~~\n[APPNOTE](https://github.com/MEYYYNASH/APPNOTE)\n#style#Text"
        val (cleanText, spans) = convertLegacyMarkdownToRichText(raw, emptyList())

        assertFalse(cleanText.contains("# Heading"))
        assertFalse(cleanText.contains("**"))
        assertFalse(cleanText.contains("=="))
        assertFalse(cleanText.contains("<u>"))
        assertFalse(cleanText.contains("~~"))
        assertFalse(cleanText.contains("#style#"))
        assertFalse(cleanText.contains("https://"))
        assertTrue(cleanText.contains("Heading 1"))
        assertTrue(cleanText.contains("highlighted text"))
        assertTrue(cleanText.contains("APPNOTE"))

        assertTrue(spans.any { it.headingLevel == 1 })
        assertTrue(spans.any { it.highlightColorArgb != null })
        assertTrue(spans.any { it.isBold })
        assertTrue(spans.any { it.isItalic })
        assertTrue(spans.any { it.isUnderline })
        assertTrue(spans.any { it.isStrikethrough })
        assertTrue(spans.any { it.linkUrl == "https://github.com/MEYYYNASH/APPNOTE" })
    }

    @Test
    fun `toggleLinePrefixInSelection toggles clean bullet and numbered lists`() {
        val text = "First item\nSecond item"
        val (bulleted, _, _) = toggleLinePrefixInSelection(
            text = text,
            selection = TextRange(0, text.length),
            spans = emptyList(),
            numbered = false
        )
        assertEquals("• First item\n• Second item", bulleted)

        val (numbered, _, _) = toggleLinePrefixInSelection(
            text = bulleted,
            selection = TextRange(0, bulleted.length),
            spans = emptyList(),
            numbered = true
        )
        assertEquals("1. First item\n2. Second item", numbered)
    }

    @Test
    fun `resolveFormatStateAt inspects shared selection styles accurately`() {
        val spans = listOf(
            RichTextSpan(start = 0, end = 5, isBold = true, textColorArgb = 0xFFEC4899L),
            RichTextSpan(start = 5, end = 10, isBold = true, textColorArgb = 0xFF38BDF8L)
        )
        val combined = resolveFormatStateAt(spans, TextRange(0, 10))
        assertTrue(combined.isBold)
        assertNull(combined.textColorArgb)
    }
}
