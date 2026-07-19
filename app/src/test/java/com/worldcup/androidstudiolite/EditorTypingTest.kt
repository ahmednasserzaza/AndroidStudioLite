package com.worldcup.androidstudiolite

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.worldcup.androidstudiolite.feature.editor.ui.applyTypingAssists
import com.worldcup.androidstudiolite.feature.editor.ui.findBracketPair
import com.worldcup.androidstudiolite.feature.editor.ui.smartTabInsertion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorTypingTest {

    private fun typed(before: String, caret: Int, char: Char): Pair<TextFieldValue, TextFieldValue> {
        val old = TextFieldValue(before, TextRange(caret))
        val proposed = TextFieldValue(
            before.substring(0, caret) + char + before.substring(caret),
            TextRange(caret + 1),
        )
        return old to proposed
    }

    @Test
    fun `typing an opening paren inserts the closing pair`() {
        val (old, proposed) = typed("val x = ", 8, '(')
        val result = applyTypingAssists(old, proposed)
        assertEquals("val x = ()", result.text)
        assertEquals(9, result.selection.start)
    }

    @Test
    fun `typing a closer over an identical char skips it`() {
        val (old, proposed) = typed("foo()", 4, ')')
        val result = applyTypingAssists(old, proposed)
        assertEquals("foo()", result.text)
        assertEquals(5, result.selection.start)
    }

    @Test
    fun `backspace inside an empty pair deletes both`() {
        val old = TextFieldValue("foo()", TextRange(4))
        val proposed = TextFieldValue("foo)", TextRange(3))
        val result = applyTypingAssists(old, proposed)
        assertEquals("foo", result.text)
        assertEquals(3, result.selection.start)
    }

    @Test
    fun `typing a bracket with a selection wraps it`() {
        val old = TextFieldValue("abc", TextRange(0, 3))
        val proposed = TextFieldValue("(", TextRange(1))
        val result = applyTypingAssists(old, proposed)
        assertEquals("(abc)", result.text)
        assertEquals(TextRange(1, 4), result.selection)
    }

    @Test
    fun `enter between braces expands to a block`() {
        val old = TextFieldValue("fun f() {}", TextRange(9))
        val proposed = TextFieldValue("fun f() {\n}", TextRange(10))
        val result = applyTypingAssists(old, proposed)
        assertEquals("fun f() {\n    \n}", result.text)
        assertEquals(14, result.selection.start)
    }

    @Test
    fun `enter after brace carries indent plus one step`() {
        val old = TextFieldValue("    if (x) {", TextRange(12))
        val proposed = TextFieldValue("    if (x) {\n", TextRange(13))
        val result = applyTypingAssists(old, proposed)
        assertEquals("    if (x) {\n        ", result.text)
    }

    @Test
    fun `apostrophe inside a word does not pair`() {
        val (old, proposed) = typed("it", 2, '\'')
        val result = applyTypingAssists(old, proposed)
        assertEquals("it'", result.text)
    }

    @Test
    fun `smart tab fills to the next four column stop`() {
        assertEquals("    ", smartTabInsertion("", 0))
        assertEquals("  ", smartTabInsertion("ab", 2))
        assertEquals("    ", smartTabInsertion("abcd", 4))
        assertEquals("   ", smartTabInsertion("x\nab", 3))
    }

    @Test
    fun `bracket pair is found across nesting`() {
        val text = "fun f(a: (Int) -> Unit) {}"
        assertEquals(5 to 22, findBracketPair(text, 6))
        assertEquals(24 to 25, findBracketPair(text, 25))
        assertNull(findBracketPair("abc", 1))
    }
}
