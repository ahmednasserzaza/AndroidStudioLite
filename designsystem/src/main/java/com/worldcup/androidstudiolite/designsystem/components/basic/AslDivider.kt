package com.worldcup.androidstudiolite.designsystem.components.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslHorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    androidx.compose.foundation.layout.Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.takeOrElse { AslTheme.colors.divider }),
    )
}

@Composable
fun AslVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    androidx.compose.foundation.layout.Box(
        modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(color.takeOrElse { AslTheme.colors.divider }),
    )
}
