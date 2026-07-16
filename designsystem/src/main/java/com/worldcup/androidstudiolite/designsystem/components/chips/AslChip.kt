package com.worldcup.androidstudiolite.designsystem.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(AslTheme.colors.surfaceContainerHighest, AslTheme.shapes.default)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        AslText(
            text = text,
            style = AslTheme.typography.uiLabelSmall,
            color = AslTheme.colors.onSurfaceVariant,
        )
    }
}

enum class AslStatus { Neutral, Running, Success, Failed, Warning }

@Composable
fun AslStatusChip(
    text: String,
    status: AslStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AslTheme.colors
    val accent: Color = when (status) {
        AslStatus.Neutral -> colors.onSurfaceVariant
        AslStatus.Running -> colors.primaryContainer
        AslStatus.Success -> colors.primary
        AslStatus.Failed -> colors.error
        AslStatus.Warning -> colors.tertiaryContainer
    }
    Row(
        modifier = modifier
            .background(accent.copy(alpha = 0.14f), AslTheme.shapes.full)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(accent, AslTheme.shapes.full),
        )
        AslText(
            text = text,
            style = AslTheme.typography.uiLabelSmall,
            color = accent,
        )
    }
}
