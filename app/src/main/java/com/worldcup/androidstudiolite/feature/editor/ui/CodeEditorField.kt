package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.launch

@Composable
fun CodeEditorField(
    value: TextFieldValue,
    fileName: String,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val colors = AslTheme.colors
    val scope = rememberCoroutineScope()

    val transformation = remember(fileName) { SyntaxTransformation(fileName) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

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
                modifier = Modifier
                    .background(colors.panel)
                    .padding(horizontal = 6.dp),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val viewportWidth = maxWidth
                Box(Modifier.horizontalScroll(horizontalScroll)) {
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
                AslText((i + 1).toString().padStart(width), style = style, color = color)
            }
        },
        modifier = modifier,
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

private class SyntaxTransformation(private val fileName: String) : VisualTransformation {
    private var lastText: String? = null
    private var lastResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val cached = if (text.text == lastText) lastResult else null
        val highlighted = cached ?: Syntax.highlight(text.text, fileName).also {
            lastText = text.text
            lastResult = it
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
