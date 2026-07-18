package com.worldcup.androidstudiolite.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.worldcup.androidstudiolite.designsystem.components.appbar.AslTopBar
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslGhostButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslIconButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslPrimaryButton
import com.worldcup.androidstudiolite.designsystem.components.buttons.AslTextButton
import com.worldcup.androidstudiolite.designsystem.components.cards.AslCard
import com.worldcup.androidstudiolite.designsystem.components.chips.AslChip
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatus
import com.worldcup.androidstudiolite.designsystem.components.chips.AslStatusChip
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslCircularProgress
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslIndeterminateLinearProgress
import com.worldcup.androidstudiolite.designsystem.components.indicators.AslLinearProgress
import com.worldcup.androidstudiolite.designsystem.components.navigation.AslBottomNavBar
import com.worldcup.androidstudiolite.designsystem.components.navigation.AslNavItem
import com.worldcup.androidstudiolite.designsystem.components.selectable.AslSwitch
import com.worldcup.androidstudiolite.designsystem.components.tabs.AslTab
import com.worldcup.androidstudiolite.designsystem.components.tabs.AslTabRow
import com.worldcup.androidstudiolite.designsystem.components.textfields.AslTextField
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.icons.AslIcons
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Preview(showBackground = true, backgroundColor = 0xFF131313, heightDp = 1400)
@Composable
private fun GalleryPreview() {
    AslTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(AslTheme.colors.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.lg),
        ) {
            AslTopBar(
                title = "Android Studio Lite",
                actions = {
                    AslIconButton(AslIcons.Search, onClick = {})
                    AslIconButton(AslIcons.Play, onClick = {}, tint = AslTheme.colors.primary)
                },
            )

            Column(
                Modifier.padding(horizontal = AslTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AslTheme.spacing.lg),
            ) {
                AslText("Buttons", style = AslTheme.typography.uiHeader)
                AslPrimaryButton("Connect with GitHub", onClick = {}, leadingIconRes = AslIcons.GitHub)
                Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
                    AslGhostButton("Ghost", onClick = {})
                    AslTextButton("View All", onClick = {})
                    AslPrimaryButton("Loading", onClick = {}, loading = true)
                }

                AslText("Text field", style = AslTheme.typography.uiHeader)
                var value by remember { mutableStateOf("") }
                AslTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Access token",
                    placeholder = "Paste your GitHub access token",
                )
                AslTextField(
                    value = "bad-key",
                    onValueChange = {},
                    errorText = "This key was rejected by the API",
                )

                AslText("Chips & status", style = AslTheme.typography.uiHeader)
                Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
                    AslChip("Kotlin")
                    AslChip("Compose")
                    AslStatusChip("In Progress", AslStatus.Running)
                    AslStatusChip("Failed", AslStatus.Failed)
                }

                AslText("Progress", style = AslTheme.typography.uiHeader)
                AslLinearProgress(0.6f)
                AslIndeterminateLinearProgress()
                AslCircularProgress()

                AslText("Card", style = AslTheme.typography.uiHeader)
                AslCard(Modifier.fillMaxWidth()) {
                    AslText("Welcome to Studio Lite", style = AslTheme.typography.headline)
                    AslText(
                        "Streamlined mobile-first IDE for Kotlin and Jetpack Compose.",
                        color = AslTheme.colors.onSurfaceVariant,
                    )
                }

                AslText("Switch", style = AslTheme.typography.uiHeader)
                Row(horizontalArrangement = Arrangement.spacedBy(AslTheme.spacing.sm)) {
                    AslSwitch(checked = true, onCheckedChange = {})
                    AslSwitch(checked = false, onCheckedChange = {})
                }

                AslText("Code type", style = AslTheme.typography.uiHeader)
                AslText(
                    "fun main() = println(\"Hello\")",
                    style = AslTheme.typography.codeMain,
                    color = AslTheme.colors.syntax.string,
                )
            }

            AslTabRow(
                tabs = listOf(
                    AslTab("1", "MainActivity.kt", modified = true),
                    AslTab("2", "Theme.kt"),
                    AslTab("3", "build.gradle.kts"),
                ),
                selectedId = "1",
                onSelect = {},
                onClose = {},
            )

            AslBottomNavBar(
                items = listOf(
                    AslNavItem("projects", "Project", AslIcons.Folder),
                    AslNavItem("editor", "Editor", AslIcons.Code),
                    AslNavItem("vcs", "VCS", AslIcons.GitBranch),
                    AslNavItem("settings", "Settings", AslIcons.Settings),
                ),
                selectedId = "projects",
                onSelect = {},
            )
        }
    }
}
