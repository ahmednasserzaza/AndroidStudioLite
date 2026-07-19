package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun applyTypingAssists(old: TextFieldValue, proposed: TextFieldValue): TextFieldValue {
    wrapSelection(old, proposed)?.let { return it }
    singleCharAssist(old, proposed)?.let { return it }
    pairBackspace(old, proposed)?.let { return it }
    return autoIndentNewline(old, proposed)
}

private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'')
private val CLOSERS = PAIRS.values.toSet()

private fun insertedChar(old: TextFieldValue, proposed: TextFieldValue): Int? {
    if (proposed.text.length != old.text.length + 1) return null
    if (!proposed.selection.collapsed || !old.selection.collapsed) return null
    val pos = proposed.selection.start - 1
    if (pos < 0 || pos != old.selection.start) return null
    if (proposed.text.substring(0, pos) != old.text.substring(0, pos)) return null
    if (proposed.text.substring(pos + 1) != old.text.substring(pos)) return null
    return pos
}

private fun singleCharAssist(old: TextFieldValue, proposed: TextFieldValue): TextFieldValue? {
    val pos = insertedChar(old, proposed) ?: return null
    val typed = proposed.text[pos]
    val nextInOld = if (pos < old.text.length) old.text[pos] else null

    if (typed in CLOSERS && nextInOld == typed) {
        return TextFieldValue(old.text, TextRange(pos + 1))
    }

    val closer = PAIRS[typed] ?: return null

    if (typed == '\'' && pos > 0 && old.text[pos - 1].isLetterOrDigit()) return null

    if ((typed == '"' || typed == '\'') && nextInOld?.isLetterOrDigit() == true) return null

    return TextFieldValue(
        text = old.text.substring(0, pos) + typed + closer + old.text.substring(pos),
        selection = TextRange(pos + 1),
    )
}

private fun wrapSelection(old: TextFieldValue, proposed: TextFieldValue): TextFieldValue? {
    if (old.selection.collapsed) return null
    val selMin = old.selection.min
    val selMax = old.selection.max
    if (proposed.text.length != old.text.length - (selMax - selMin) + 1) return null
    if (proposed.text.getOrNull(selMin) == null) return null
    val typed = proposed.text[selMin]
    val closer = PAIRS[typed] ?: return null
    if (typed == '\'') return null

    if (proposed.text.substring(0, selMin) != old.text.substring(0, selMin)) return null
    if (proposed.text.substring(selMin + 1) != old.text.substring(selMax)) return null

    val selected = old.text.substring(selMin, selMax)
    return TextFieldValue(
        text = old.text.substring(0, selMin) + typed + selected + closer + old.text.substring(selMax),
        selection = TextRange(selMin + 1, selMin + 1 + selected.length),
    )
}

private fun pairBackspace(old: TextFieldValue, proposed: TextFieldValue): TextFieldValue? {
    if (proposed.text.length != old.text.length - 1) return null
    if (!proposed.selection.collapsed || !old.selection.collapsed) return null
    val caret = old.selection.start
    if (caret < 1 || proposed.selection.start != caret - 1) return null
    val deleted = old.text[caret - 1]
    val closer = PAIRS[deleted] ?: return null
    if (caret >= old.text.length || old.text[caret] != closer) return null
    if (proposed.text.substring(0, caret - 1) != old.text.substring(0, caret - 1)) return null
    if (proposed.text.substring(caret - 1) != old.text.substring(caret)) return null

    return TextFieldValue(
        text = old.text.removeRange(caret - 1, caret + 1),
        selection = TextRange(caret - 1),
    )
}

fun autoIndentNewline(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (new.text.length != old.text.length + 1) return new
    if (!new.selection.collapsed) return new
    val insertPos = new.selection.start - 1
    if (insertPos < 0 || insertPos >= new.text.length || new.text[insertPos] != '\n') return new

    val lineStart = new.text.lastIndexOf('\n', insertPos - 1) + 1
    val prevLine = new.text.substring(lineStart, insertPos)
    val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
    val trimmed = prevLine.trimEnd()
    val extra = if (trimmed.endsWith("{") || trimmed.endsWith("(")) INDENT_STEP else ""

    val nextChar = new.text.getOrNull(insertPos + 1)
    if (trimmed.endsWith("{") && (nextChar == '}' || nextChar == ')')) {
        val insertion = indent + INDENT_STEP + "\n" + indent
        val newText = new.text.substring(0, insertPos + 1) + insertion +
            new.text.substring(insertPos + 1)
        return TextFieldValue(newText, TextRange(insertPos + 1 + indent.length + INDENT_STEP.length))
    }

    val insertion = indent + extra
    if (insertion.isEmpty()) return new

    val newText = new.text.substring(0, insertPos + 1) + insertion + new.text.substring(insertPos + 1)
    return TextFieldValue(newText, TextRange(insertPos + 1 + insertion.length))
}

fun smartTabInsertion(text: String, caret: Int): String {
    val lineStart = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0)).let {
        if (caret == 0) 0 else it + 1
    }
    val column = caret - lineStart
    val spaces = 4 - (column % 4)
    return " ".repeat(spaces)
}

fun findBracketPair(text: String, caret: Int): Pair<Int, Int>? {
    val brackets = "()[]{}"
    val at = caret.takeIf { it < text.length && text[it] in brackets }
    val before = (caret - 1).takeIf { it >= 0 && it < text.length && text[it] in brackets }
    val pos = before ?: at ?: return null
    val c = text[pos]
    val open = "([{"
    val close = ")]}"
    return if (c in open) {
        val closer = close[open.indexOf(c)]
        var depth = 0
        for (i in pos until text.length) {
            when (text[i]) {
                c -> depth++
                closer -> {
                    depth--
                    if (depth == 0) return pos to i
                }
            }
        }
        null
    } else {
        val opener = open[close.indexOf(c)]
        var depth = 0
        for (i in pos downTo 0) {
            when (text[i]) {
                c -> depth++
                opener -> {
                    depth--
                    if (depth == 0) return i to pos
                }
            }
        }
        null
    }
}

const val INDENT_STEP = "    "
