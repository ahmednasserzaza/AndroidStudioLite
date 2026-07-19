package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class MatchHighlight(val start: Int, val end: Int, val active: Boolean)

data class EditorScrollRequest(val offset: Int, val nonce: Long)

@Composable
fun CodeEditorField(
    value: TextFieldValue,
    fileName: String,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    searchMatches: List<MatchHighlight> = emptyList(),
    bracketOffsets: List<Int> = emptyList(),
    errorLines: Set<Int> = emptySet(),
    warningLines: Set<Int> = emptySet(),
    changedLines: Set<Int> = emptySet(),
    scrollRequest: EditorScrollRequest? = null,
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val colors = AslTheme.colors
    val scope = rememberCoroutineScope()

    val matchColor = colors.primary.copy(alpha = 0.25f)
    val activeMatchColor = colors.primary.copy(alpha = 0.55f)
    val bracketColor = colors.secondary.copy(alpha = 0.4f)
    val transformation = remember(fileName, searchMatches, bracketOffsets) {
        SyntaxTransformation(
            fileName = fileName,
            searchMatches = searchMatches,
            bracketOffsets = bracketOffsets,
            matchColor = matchColor,
            activeMatchColor = activeMatchColor,
            bracketColor = bracketColor,
        )
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val caretLine = remember(value.text, value.selection.start) {
        val limit = value.selection.start.coerceIn(0, value.text.length)
        var count = 0
        for (i in 0 until limit) if (value.text[i] == '\n') count++
        count
    }

    val currentValue by rememberUpdatedState(value)
    LaunchedEffect(scrollRequest) {
        val request = scrollRequest ?: return@LaunchedEffect
        val layout = withTimeoutOrNull(1_500L) {
            snapshotFlow { layoutResult }
                .first { it != null && it.layoutInput.text.text == currentValue.text }
        } ?: return@LaunchedEffect
        val offset = request.offset.coerceIn(0, layout.layoutInput.text.length)
        val line = layout.getLineForOffset(offset)
        val targetTop = (layout.getLineTop(line) - verticalScroll.viewportSize / 3f)
            .roundToInt().coerceAtLeast(0)
        verticalScroll.animateScrollTo(targetTop)
        val targetLeft = (layout.getHorizontalPosition(offset, usePrimaryDirection = true) -
            horizontalScroll.viewportSize / 3f).roundToInt().coerceAtLeast(0)
        horizontalScroll.animateScrollTo(targetLeft)
    }

    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    var pinchZoom by remember { mutableFloatStateOf(1f) }
    var pinchAnchor by remember { mutableStateOf(Offset.Zero) }

    val baseStyle = AslTheme.typography.codeMain
    val codeStyle = remember(baseStyle, colors, fontScale) {
        baseStyle.copy(
            color = colors.syntax.plain,
            fontSize = baseStyle.fontSize * fontScale,
            lineHeight = baseStyle.lineHeight * fontScale,
        )
    }

    val isCodeFile = remember(fileName) {
        val name = fileName.lowercase()
        name.endsWith(".kt") || name.endsWith(".kts") ||
            name.endsWith(".java") || name.endsWith(".gradle")
    }

    Box(modifier.fillMaxSize().background(colors.canvas)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (pinchZoom != 1f) {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = pinchZoom
                        scaleY = pinchZoom
                        translationX = pinchAnchor.x * (1f - pinchZoom)
                        translationY = pinchAnchor.y * (1f - pinchZoom)
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var zoom = 1f
                        var anchor: Offset? = null
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.count { it.pressed } >= 2) {
                                if (anchor == null) {
                                    anchor = event.calculateCentroid()
                                    pinchAnchor = anchor
                                }
                                zoom = (zoom * event.calculateZoom())
                                    .coerceIn(MIN_FONT_SCALE / fontScale, MAX_FONT_SCALE / fontScale)
                                pinchZoom = zoom
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })

                        val pinchedAt = anchor
                        if (pinchedAt != null && zoom != 1f) {
                            val targetV = (zoom * (verticalScroll.value + pinchedAt.y) - pinchedAt.y)
                                .roundToInt().coerceAtLeast(0)
                            val targetH = (zoom * (horizontalScroll.value + pinchedAt.x) - pinchedAt.x)
                                .roundToInt().coerceAtLeast(0)
                            fontScale = (fontScale * zoom).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
                            pinchZoom = 1f
                            scope.launch {
                                withFrameNanos { }
                                verticalScroll.scrollTo(targetV)
                                horizontalScroll.scrollTo(targetH)
                            }
                        } else {
                            pinchZoom = 1f
                        }
                    }
                }
                .verticalScroll(verticalScroll),
        ) {
            LineNumberGutter(
                layoutResult = layoutResult,
                style = codeStyle,
                color = colors.syntax.comment,
                topPadding = EDITOR_PADDING,
                caretLine = caretLine,
                errorLines = errorLines,
                warningLines = warningLines,
                changedLines = changedLines,
                caretColor = colors.onSurface,
                errorColor = colors.error,
                warningColor = colors.tertiaryContainer,
                changeBarColor = colors.primaryContainer,
                modifier = Modifier
                    .background(colors.panel)
                    .padding(horizontal = 6.dp),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val viewportWidth = maxWidth
                Box(Modifier.horizontalScroll(horizontalScroll)) {
                    Box(
                        Modifier.editorDecorations(
                            layoutProvider = { layoutResult },
                            verticalScroll = verticalScroll,
                            caretLine = caretLine,
                            errorLines = errorLines,
                            currentLineColor = colors.onSurface.copy(alpha = 0.05f),
                            errorLineColor = colors.error.copy(alpha = 0.08f),
                            guideColor = colors.onSurface.copy(alpha = 0.08f),
                            showGuides = isCodeFile || fileName.lowercase().endsWith(".xml"),
                        ),
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = codeStyle,
                            cursorBrush = SolidColor(colors.primary),
                            visualTransformation = transformation,
                            onTextLayout = { layoutResult = it },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                            ),
                            modifier = Modifier
                                .widthIn(min = viewportWidth)
                                .padding(
                                    start = EDITOR_PADDING,
                                    top = EDITOR_PADDING,
                                    bottom = 120.dp,
                                    end = EDITOR_PADDING,
                                ),
                        )
                    }
                }
            }
        }

        if (isCodeFile) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val stickyHeader by remember(density) {
                derivedStateOf {
                    val layout = layoutResult ?: return@derivedStateOf null
                    val topPad = with(density) { EDITOR_PADDING.toPx() }
                    val y = (verticalScroll.value - topPad).coerceAtLeast(0f)
                    val firstVisible = layout.getLineForVerticalPosition(y)
                    stickyHeaderFor(layout, firstVisible)
                }
            }
            stickyHeader?.let { (line, headerText) ->
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(colors.panel.copy(alpha = 0.96f))
                        .clickable {
                            scope.launch {
                                val layout = layoutResult ?: return@launch
                                verticalScroll.animateScrollTo(
                                    layout.getLineTop(line).roundToInt().coerceAtLeast(0),
                                )
                            }
                        },
                ) {
                    AslText(
                        headerText,
                        style = AslTheme.typography.codeSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            JumpButton(
                icon = AslIcons.VerticalAlignTop,
                description = "Jump to top",
                onClick = { scope.launch { verticalScroll.animateScrollTo(0) } },
            )
            JumpButton(
                icon = AslIcons.VerticalAlignBottom,
                description = "Jump to bottom",
                onClick = { scope.launch { verticalScroll.animateScrollTo(verticalScroll.maxValue) } },
            )
        }
    }
}

private fun Modifier.editorDecorations(
    layoutProvider: () -> TextLayoutResult?,
    verticalScroll: androidx.compose.foundation.ScrollState,
    caretLine: Int,
    errorLines: Set<Int>,
    currentLineColor: Color,
    errorLineColor: Color,
    guideColor: Color,
    showGuides: Boolean,
): Modifier = drawBehind {
    val layout = layoutProvider() ?: return@drawBehind
    val topPad = EDITOR_PADDING.toPx()
    val text = layout.layoutInput.text.text

    fun drawLineRect(line: Int, color: Color) {
        if (line !in 0 until layout.lineCount) return
        val top = layout.getLineTop(line) + topPad
        val bottom = layout.getLineBottom(line) + topPad
        drawRect(color, topLeft = Offset(0f, top), size = Size(size.width, bottom - top))
    }

    errorLines.forEach { drawLineRect(it, errorLineColor) }
    drawLineRect(caretLine, currentLineColor)

    if (!showGuides) return@drawBehind

    val visibleTop = (verticalScroll.value - topPad).coerceAtLeast(0f)
    val visibleBottom = visibleTop + verticalScroll.viewportSize
    val firstVisible = layout.getLineForVerticalPosition(visibleTop)
    val lastVisible = layout.getLineForVerticalPosition(visibleBottom)

    var charWidth = 0f
    for (line in firstVisible..lastVisible) {
        val start = layout.getLineStart(line)
        if (start < layout.getLineEnd(line)) {
            charWidth = layout.getBoundingBox(start).width
            break
        }
    }
    if (charWidth <= 0f) return@drawBehind

    for (line in firstVisible..lastVisible) {
        if (line >= layout.lineCount) break
        val start = layout.getLineStart(line)
        val end = layout.getLineEnd(line)
        if (end <= start) continue
        var indent = 0
        var i = start
        while (i < end && i < text.length && text[i] == ' ') { indent++; i++ }
        if (i >= end || i >= text.length) continue
        val top = layout.getLineTop(line) + topPad
        val bottom = layout.getLineBottom(line) + topPad
        var level = 1
        while (level * 4 < indent) {
            val x = topPad + level * 4 * charWidth
            drawRect(guideColor, topLeft = Offset(x, top), size = Size(1.dp.toPx(), bottom - top))
            level++
        }
    }
}

private fun stickyHeaderFor(layout: TextLayoutResult, firstVisibleLine: Int): Pair<Int, String>? {
    if (firstVisibleLine <= 0) return null
    val text = layout.layoutInput.text.text

    fun lineText(line: Int): String {
        val start = layout.getLineStart(line)
        val end = layout.getLineEnd(line)
        return if (end > start && end <= text.length) text.substring(start, end) else ""
    }

    var refLine = firstVisibleLine
    var probes = 0
    while (refLine < layout.lineCount && lineText(refLine).isBlank() && probes++ < 5) refLine++
    if (refLine >= layout.lineCount) return null
    var refIndent = lineText(refLine).takeWhile { it == ' ' }.length
    if (refIndent == 0) return null

    var line = firstVisibleLine - 1
    var walked = 0
    while (line >= 0 && walked++ < MAX_HEADER_WALK) {
        val content = lineText(line)
        if (content.isNotBlank()) {
            val indent = content.takeWhile { it == ' ' }.length
            if (indent < refIndent) {
                if (DECLARATION_REGEX.containsMatchIn(content)) return line to content.trim()
                refIndent = indent
                if (indent == 0) return null
            }
        }
        line--
    }
    return null
}

private val DECLARATION_REGEX = Regex(
    "^\\s*(?:@\\w+\\s+)?(?:(?:public|private|protected|internal|open|abstract|final|sealed|" +
        "data|inner|suspend|override|operator|infix)\\s+)*" +
        "(?:class|interface|object|fun|enum\\s+class|companion\\s+object)\\b",
)

private const val MAX_HEADER_WALK = 500

@Composable
private fun JumpButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier.background(
            AslTheme.colors.panel.copy(alpha = 0.85f),
            AslTheme.shapes.default,
        ),
    ) {
        AslIconButton(
            icon,
            onClick = onClick,
            tint = AslTheme.colors.onSurfaceVariant,
            contentDescription = description,
        )
    }
}

private val EDITOR_PADDING = 8.dp
private const val MIN_FONT_SCALE = 0.6f
private const val MAX_FONT_SCALE = 2.5f

@Composable
private fun LineNumberGutter(
    layoutResult: TextLayoutResult?,
    style: TextStyle,
    color: Color,
    topPadding: androidx.compose.ui.unit.Dp,
    caretLine: Int,
    errorLines: Set<Int>,
    warningLines: Set<Int>,
    changedLines: Set<Int>,
    caretColor: Color,
    errorColor: Color,
    warningColor: Color,
    changeBarColor: Color,
    modifier: Modifier = Modifier,
) {
    val lineTops = remember(layoutResult) {
        val layout = layoutResult ?: return@remember emptyList<Float>()
        val laidOutText = layout.layoutInput.text.text
        buildList {
            var offset = 0
            while (true) {
                add(layout.getLineTop(layout.getLineForOffset(offset)))
                val newline = laidOutText.indexOf('\n', offset)
                if (newline == -1) break
                offset = newline + 1
            }
        }
    }

    Layout(
        content = {
            val width = lineTops.size.toString().length.coerceAtLeast(3)
            lineTops.indices.forEach { i ->
                val lineColor = when {
                    i in errorLines -> errorColor
                    i in warningLines -> warningColor
                    i == caretLine -> caretColor
                    else -> color
                }
                AslText((i + 1).toString().padStart(width), style = style, color = lineColor)
            }
        },
        modifier = modifier.drawBehind {
            if (lineTops.isEmpty()) return@drawBehind
            val topPx = topPadding.toPx()
            val barWidth = 2.5.dp.toPx()
            changedLines.forEach { line ->
                if (line < lineTops.size) {
                    val top = topPx + lineTops[line]
                    val bottom = topPx +
                        (lineTops.getOrNull(line + 1) ?: (layoutResult?.size?.height?.toFloat() ?: top))
                    drawRect(
                        changeBarColor,
                        topLeft = Offset(-4.dp.toPx(), top),
                        size = Size(barWidth, (bottom - top).coerceAtLeast(barWidth)),
                    )
                }
            }
        },
    ) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val topPx = topPadding.roundToPx()
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = topPx + (layoutResult?.size?.height ?: 0)
        layout(width, height) {
            placeables.forEachIndexed { i, placeable ->
                placeable.place(0, topPx + lineTops[i].roundToInt())
            }
        }
    }
}

private class SyntaxTransformation(
    private val fileName: String,
    private val searchMatches: List<MatchHighlight>,
    private val bracketOffsets: List<Int>,
    private val matchColor: Color,
    private val activeMatchColor: Color,
    private val bracketColor: Color,
) : VisualTransformation {
    private var lastText: String? = null
    private var lastResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val cached = if (text.text == lastText) lastResult else null
        val highlighted = cached ?: Syntax.highlight(text.text, fileName).also {
            lastText = text.text
            lastResult = it
        }
        return TransformedText(withOverlays(highlighted, text.length), OffsetMapping.Identity)
    }

    private fun withOverlays(base: AnnotatedString, length: Int): AnnotatedString {
        if (searchMatches.isEmpty() && bracketOffsets.isEmpty()) return base
        return buildAnnotatedString {
            append(base)
            searchMatches.forEach { match ->
                if (match.start < length) {
                    addStyle(
                        SpanStyle(background = if (match.active) activeMatchColor else matchColor),
                        match.start,
                        match.end.coerceAtMost(length),
                    )
                }
            }
            bracketOffsets.forEach { offset ->
                if (offset in 0 until length) {
                    addStyle(SpanStyle(background = bracketColor), offset, offset + 1)
                }
            }
        }
    }
}
