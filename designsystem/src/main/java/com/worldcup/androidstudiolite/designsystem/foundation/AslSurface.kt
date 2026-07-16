package com.worldcup.androidstudiolite.designsystem.foundation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.designsystem.theme.LocalContentColor

@Composable
fun AslSurface(
    modifier: Modifier = Modifier,
    color: Color = AslTheme.colors.surface,
    contentColor: Color = AslTheme.colors.onSurface,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier
                .clip(shape)
                .background(color, shape)
                .then(border?.let { Modifier.border(it, shape) } ?: Modifier),
        ) {
            content()
        }
    }
}
