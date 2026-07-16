package com.worldcup.androidstudiolite.designsystem.components.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.AslSurface
import com.worldcup.androidstudiolite.designsystem.foundation.clickableWithNoFeedback
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickableWithNoFeedback(onClick = onDismissRequest),
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            AslSurface(
                modifier = Modifier.fillMaxWidth(),
                color = AslTheme.colors.popover,
                shape = AslTheme.shapes.extraLarge,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(
                            start = AslTheme.spacing.lg,
                            end = AslTheme.spacing.lg,
                            bottom = AslTheme.spacing.lg,
                        ),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = AslTheme.spacing.sm)
                            .size(width = 36.dp, height = 4.dp)
                            .background(AslTheme.colors.surfaceContainerHighest, AslTheme.shapes.full),
                    )
                    content()
                }
            }
        }
    }
}
