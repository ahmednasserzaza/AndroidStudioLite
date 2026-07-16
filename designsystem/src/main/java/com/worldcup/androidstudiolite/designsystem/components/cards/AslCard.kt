package com.worldcup.androidstudiolite.designsystem.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.AslIndication
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.designsystem.theme.LocalContentColor

@Composable
fun AslCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    shape: Shape = AslTheme.shapes.extraLarge,
    border: BorderStroke? = null,
    contentPadding: Dp = AslTheme.spacing.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = color.takeOrElse { AslTheme.colors.surfaceContainerLow }
    CompositionLocalProvider(LocalContentColor provides AslTheme.colors.onSurface) {
        Column(
            modifier = modifier
                .clip(shape)
                .background(container, shape)
                .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
                .then(
                    onClick?.let {
                        Modifier.clickable(
                            interactionSource = null,
                            indication = AslIndication,
                            onClick = it,
                        )
                    } ?: Modifier,
                )
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AslInnerCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = AslTheme.spacing.md,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AslCard(
        modifier = modifier,
        color = AslTheme.colors.surfaceContainerHigh,
        shape = AslTheme.shapes.large,
        border = BorderStroke(1.dp, AslTheme.colors.divider),
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}
