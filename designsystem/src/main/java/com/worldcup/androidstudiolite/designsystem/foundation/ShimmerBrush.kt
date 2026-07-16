package com.worldcup.androidstudiolite.designsystem.foundation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun shimmerBrush(): Brush {
    val colors = AslTheme.colors
    val shimmerColors = listOf(
        colors.surfaceContainerLow,
        colors.surfaceContainerHigh,
        colors.surfaceContainerLow,
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translate - 400f, translate - 400f),
        end = Offset(translate, translate),
    )
}
