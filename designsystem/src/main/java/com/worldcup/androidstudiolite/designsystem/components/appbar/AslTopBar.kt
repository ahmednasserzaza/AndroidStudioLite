package com.worldcup.androidstudiolite.designsystem.components.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.worldcup.androidstudiolite.designsystem.components.basic.AslHorizontalDivider
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    titleLeading: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
) {
    androidx.compose.foundation.layout.Column(modifier.background(AslTheme.colors.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AslTheme.spacing.toolbarHeight)
                .padding(horizontal = AslTheme.spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm),
        ) {
            if (navigation != null) navigation()
            if (titleLeading != null) titleLeading()
            AslText(
                text = title,
                style = AslTheme.typography.uiHeader,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (actions != null) actions()
        }
        if (showDivider) AslHorizontalDivider()
    }
}
