package com.worldcup.androidstudiolite.designsystem.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.worldcup.androidstudiolite.designsystem.foundation.AslSurface
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        AslSurface(
            modifier = modifier.fillMaxWidth(),
            color = AslTheme.colors.popover,
            shape = AslTheme.shapes.large,
            border = BorderStroke(1.dp, AslTheme.colors.divider),
        ) {
            Column(
                Modifier.padding(AslTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.md),
            ) {
                AslText(text = title, style = AslTheme.typography.title)
                content()
                if (buttons != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            AslTheme.spacing.sm,
                            Alignment.End,
                        ),
                    ) {
                        buttons()
                    }
                }
            }
        }
    }
}
