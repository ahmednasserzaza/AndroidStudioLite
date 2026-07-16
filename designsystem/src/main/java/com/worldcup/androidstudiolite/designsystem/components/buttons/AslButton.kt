package com.worldcup.androidstudiolite.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslIndication
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.designsystem.theme.LocalContentColor

@Composable
private fun AslBaseButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIconRes: Int? = null,
    fillWidth: Boolean = false,
) {
    val shape = AslTheme.shapes.default
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier else Modifier)
            .alpha(alpha)
            .clip(shape)
            .background(containerColor, shape)
            .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
            .clickable(
                interactionSource = null,
                indication = AslIndication,
                enabled = enabled && !loading,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (loading) {
                ButtonLoadingDots(color = contentColor)
            } else {
                if (leadingIconRes != null) {
                    AslIcon(leadingIconRes, size = 18.dp)
                }
                AslText(
                    text = text,
                    style = AslTheme.typography.uiHeader,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun AslPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIconRes: Int? = null,
) {
    AslBaseButton(
        text = text,
        onClick = onClick,
        containerColor = AslTheme.colors.primaryContainer,
        contentColor = AslTheme.colors.canvas,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        leadingIconRes = leadingIconRes,
    )
}

@Composable
fun AslGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIconRes: Int? = null,
) {
    AslBaseButton(
        text = text,
        onClick = onClick,
        containerColor = Color.Transparent,
        contentColor = AslTheme.colors.onSurface,
        border = BorderStroke(1.dp, AslTheme.colors.outlineVariant),
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        leadingIconRes = leadingIconRes,
    )
}

@Composable
fun AslTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AslBaseButton(
        text = text,
        onClick = onClick,
        containerColor = Color.Transparent,
        contentColor = AslTheme.colors.primary,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun AslDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AslBaseButton(
        text = text,
        onClick = onClick,
        containerColor = AslTheme.colors.errorContainer,
        contentColor = AslTheme.colors.onErrorContainer,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
    )
}

@Composable
fun AslIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(AslTheme.shapes.default)
            .clickable(
                interactionSource = null,
                indication = AslIndication,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AslIcon(iconRes, tint = tint, contentDescription = contentDescription)
    }
}

@Composable
fun AslFab(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(AslTheme.shapes.full)
            .background(AslTheme.colors.primaryContainer)
            .clickable(interactionSource = null, indication = AslIndication, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AslIcon(iconRes, tint = AslTheme.colors.canvas, contentDescription = contentDescription)
    }
}
