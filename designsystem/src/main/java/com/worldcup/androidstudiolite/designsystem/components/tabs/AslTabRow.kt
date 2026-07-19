package com.worldcup.androidstudiolite.designsystem.components.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.components.basic.AslHorizontalDivider
import com.worldcup.androidstudiolite.designsystem.foundation.AslIcon
import com.worldcup.androidstudiolite.designsystem.foundation.AslIndication
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.foundation.clickableWithNoFeedback
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

data class AslTab(
    val id: String,
    val title: String,
    val modified: Boolean = false,
    val closeable: Boolean = true,
    val badge: String? = null,
    val badgeColor: Color? = null,
)

@Composable
fun AslTabRow(
    tabs: List<AslTab>,
    selectedId: String?,
    onSelect: (AslTab) -> Unit,
    modifier: Modifier = Modifier,
    onClose: ((AslTab) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(AslTheme.colors.panel)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            tabs.forEach { tab ->
                val selected = tab.id == selectedId
                Column(
                    Modifier
                        .background(if (selected) AslTheme.colors.canvas else AslTheme.colors.panel)
                        .clickable(
                            interactionSource = null,
                            indication = AslIndication,
                        ) { onSelect(tab) },
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (tab.badge != null) {
                            AslText(
                                text = tab.badge,
                                style = AslTheme.typography.uiLabelSmall,
                                color = tab.badgeColor ?: AslTheme.colors.onSurfaceVariant,
                            )
                        }
                        if (tab.modified) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(AslTheme.colors.primaryContainer, AslTheme.shapes.full),
                            )
                        }
                        AslText(
                            text = tab.title,
                            style = AslTheme.typography.uiLabelSmall,
                            color = if (selected) AslTheme.colors.onSurface else AslTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                        if (tab.closeable && onClose != null) {
                            AslIcon(
                                AslIcons.Close,
                                size = 12.dp,
                                tint = AslTheme.colors.onSurfaceVariant,
                                modifier = Modifier.clickableWithNoFeedback { onClose(tab) },
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (selected) AslTheme.colors.primaryContainer
                                else AslTheme.colors.panel,
                            ),
                    )
                }
            }
        }
        AslHorizontalDivider()
    }
}
