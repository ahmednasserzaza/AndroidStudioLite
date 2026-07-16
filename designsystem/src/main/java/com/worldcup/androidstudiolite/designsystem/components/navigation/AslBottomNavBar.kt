package com.worldcup.androidstudiolite.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.components.basic.AslHorizontalDivider
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.foundation.clickableWithNoFeedback
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

data class AslNavItem(
    val id: String,
    val label: String,
    val iconRes: Int,
)

@Composable
fun AslBottomNavBar(
    items: List<AslNavItem>,
    selectedId: String,
    onSelect: (AslNavItem) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
) {
    Column(modifier.fillMaxWidth().background(AslTheme.colors.surface)) {
        AslHorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(AslTheme.spacing.bottomNavHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                val tint = if (selected) AslTheme.colors.primary else AslTheme.colors.onSurfaceVariant
                Column(
                    Modifier
                        .weight(1f)
                        .clickableWithNoFeedback { onSelect(item) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    AslIcon(item.iconRes, tint = tint, contentDescription = item.label)
                    if (showLabels) {
                        AslText(
                            text = item.label,
                            style = AslTheme.typography.uiLabelSmall,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
