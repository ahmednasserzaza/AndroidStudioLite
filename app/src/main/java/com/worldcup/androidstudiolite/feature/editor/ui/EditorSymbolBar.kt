package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.foundation.AslIndication
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

const val SMART_TAB_SNIPPET = "\t"

@Composable
fun EditorSymbolBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsert: (String) -> Unit,
    onToggleComment: () -> Unit,
    onCaretMove: (Int) -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(AslTheme.colors.panel)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AslTheme.colors.divider))
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AslIconButton(
                AslIcons.Undo,
                onClick = onUndo,
                enabled = canUndo,
                tint = historyTint(canUndo),
                contentDescription = "Undo",
            )
            AslIconButton(
                AslIcons.Redo,
                onClick = onRedo,
                enabled = canRedo,
                tint = historyTint(canRedo),
                contentDescription = "Redo",
            )
            KeyDivider()
            SymbolKey("◀", onClick = { onCaretMove(-1) })
            SymbolKey("▶", onClick = { onCaretMove(+1) })
            KeyDivider()
            SymbolKey("//", onClick = onToggleComment)
            KeyDivider()
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SYMBOL_KEYS.forEach { key ->
                    SymbolKey(
                        label = key.label,
                        onClick = { onInsert(key.snippet) },
                        onLongClick = key.altSnippet?.let { alt -> { onInsert(alt) } },
                    )
                }
            }
            KeyDivider()
            AslIconButton(
                AslIcons.KeyboardHide,
                onClick = onHideKeyboard,
                tint = AslTheme.colors.onSurfaceVariant,
                contentDescription = "Hide keyboard",
            )
        }
    }
}

@Composable
private fun historyTint(enabled: Boolean): Color =
    if (enabled) AslTheme.colors.onSurface
    else AslTheme.colors.onSurfaceVariant.copy(alpha = 0.35f)

@Composable
private fun KeyDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(24.dp)
            .background(AslTheme.colors.divider),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SymbolKey(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxHeight()
            .widthIn(min = 36.dp)
            .clip(AslTheme.shapes.default)
            .combinedClickable(
                interactionSource = null,
                indication = AslIndication,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AslText(label, style = AslTheme.typography.codeMain)
    }
}

private data class SymbolKeyDef(val label: String, val snippet: String, val altSnippet: String? = null)

private val SYMBOL_KEYS = listOf(
    SymbolKeyDef("⇥", SMART_TAB_SNIPPET),
    SymbolKeyDef("{", "{", "{}"),
    SymbolKeyDef("}", "}"),
    SymbolKeyDef("(", "(", "()"),
    SymbolKeyDef(")", ")"),
    SymbolKeyDef(";", ";"),
    SymbolKeyDef(":", ":"),
    SymbolKeyDef(".", "."),
    SymbolKeyDef(",", ","),
    SymbolKeyDef("=", "=", " == "),
    SymbolKeyDef("\"", "\"", "\"\"\""),
    SymbolKeyDef("<", "<", "<>"),
    SymbolKeyDef(">", ">", " -> "),
    SymbolKeyDef("[", "[", "[]"),
    SymbolKeyDef("]", "]"),
    SymbolKeyDef("->", " -> "),
    SymbolKeyDef("+", "+"),
    SymbolKeyDef("-", "-"),
    SymbolKeyDef("*", "*"),
    SymbolKeyDef("/", "/"),
    SymbolKeyDef("!", "!", " != "),
    SymbolKeyDef("?", "?", "?: "),
    SymbolKeyDef("&", "&", " && "),
    SymbolKeyDef("|", "|", " || "),
    SymbolKeyDef("_", "_"),
    SymbolKeyDef("@", "@"),
    SymbolKeyDef("$", "$", "\${}"),
    SymbolKeyDef("#", "#"),
    SymbolKeyDef("'", "'"),
)
