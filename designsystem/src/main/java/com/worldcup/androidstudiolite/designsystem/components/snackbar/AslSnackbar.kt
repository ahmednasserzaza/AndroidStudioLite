package com.worldcup.androidstudiolite.designsystem.components.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.AslSurface
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslSnackbarHost(
    message: String?,
    modifier: Modifier = Modifier,
) {
    val lastShown = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }
    if (message != null) lastShown.value = message

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            val lastMessage = lastShown.value
            AslSurface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(AslTheme.spacing.lg)
                    .fillMaxWidth(),
                color = AslTheme.colors.popover,
                shape = AslTheme.shapes.medium,
                border = BorderStroke(1.dp, AslTheme.colors.divider),
            ) {
                AslText(
                    text = lastMessage,
                    modifier = Modifier.padding(AslTheme.spacing.md),
                    style = AslTheme.typography.uiBody,
                )
            }
        }
    }
}
