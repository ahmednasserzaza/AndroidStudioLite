package com.worldcup.androidstudiolite.designsystem.components.selectable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.clickableWithNoFeedback
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AslTheme.colors
    val trackColor by animateColorAsState(
        if (checked) colors.primaryContainer else colors.surfaceContainerHighest,
        label = "switchTrack",
    )
    val thumbColor by animateColorAsState(
        if (checked) colors.canvas else colors.onSurfaceVariant,
        label = "switchThumb",
    )
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .background(trackColor, AslTheme.shapes.full)
            .clickableWithNoFeedback(enabled = enabled) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .background(thumbColor, AslTheme.shapes.full),
        )
    }
}
