package com.worldcup.androidstudiolite.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class AslShapes(
    val small: RoundedCornerShape,
    val default: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape,
    val extraLarge: RoundedCornerShape,
    val full: RoundedCornerShape,
    val tab: RoundedCornerShape,
)

val AslDefaultShapes = AslShapes(
    small = RoundedCornerShape(2.dp),
    default = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
    full = RoundedCornerShape(percent = 50),
    tab = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
)

val LocalAslShapes = staticCompositionLocalOf { AslDefaultShapes }
