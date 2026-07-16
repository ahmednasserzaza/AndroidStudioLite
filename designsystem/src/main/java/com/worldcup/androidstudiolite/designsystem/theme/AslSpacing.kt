package com.worldcup.androidstudiolite.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AslSpacing(
    val unit: Dp = 4.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val gutter: Dp = 12.dp,
    val componentGap: Dp = 8.dp,
    val toolbarHeight: Dp = 48.dp,
    val denseRowHeight: Dp = 24.dp,
    val bottomNavHeight: Dp = 56.dp,
    val minTouchTarget: Dp = 44.dp,
)

val LocalAslSpacing = staticCompositionLocalOf { AslSpacing() }
