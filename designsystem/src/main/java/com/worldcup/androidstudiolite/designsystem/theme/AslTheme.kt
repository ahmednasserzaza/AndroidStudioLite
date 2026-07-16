package com.worldcup.androidstudiolite.designsystem.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun AslTheme(
    colors: AslColorScheme = AslDarkColors,
    typography: AslTypography = AslDefaultTypography,
    shapes: AslShapes = AslDefaultShapes,
    spacing: AslSpacing = AslSpacing(),
    content: @Composable () -> Unit,
) {
    val selectionColors = TextSelectionColors(
        handleColor = colors.primaryContainer,
        backgroundColor = colors.primaryContainer.copy(alpha = 0.35f),
    )
    CompositionLocalProvider(
        LocalAslColors provides colors,
        LocalAslTypography provides typography,
        LocalAslShapes provides shapes,
        LocalAslSpacing provides spacing,
        LocalContentColor provides colors.onSurface,
        LocalTextSelectionColors provides selectionColors,
        content = content,
    )
}

object AslTheme {
    val colors: AslColorScheme
        @Composable @ReadOnlyComposable get() = LocalAslColors.current
    val typography: AslTypography
        @Composable @ReadOnlyComposable get() = LocalAslTypography.current
    val shapes: AslShapes
        @Composable @ReadOnlyComposable get() = LocalAslShapes.current
    val spacing: AslSpacing
        @Composable @ReadOnlyComposable get() = LocalAslSpacing.current
}
