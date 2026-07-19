package com.worldcup.androidstudiolite.feature.editor.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.foundation.AslText
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

data class FileBadge(val label: String, val color: Color)

fun fileBadge(fileName: String): FileBadge {
    val name = fileName.lowercase()
    return when {
        name.contains("gradle") -> FileBadge("G", Color(0xFF3DDC84))
        name.endsWith(".kt") || name.endsWith(".kts") -> FileBadge("K", Color(0xFFA97BFF))
        name.endsWith(".java") -> FileBadge("J", Color(0xFFE76F00))
        name.endsWith(".xml") -> FileBadge("</>", Color(0xFFFFB33A))
        name.endsWith(".yml") || name.endsWith(".yaml") -> FileBadge("Y", Color(0xFFA8C8FF))
        name.endsWith(".json") -> FileBadge("{}", Color(0xFFFFC66D))
        name.endsWith(".md") -> FileBadge("M", Color(0xFF6BC6C4))
        name.endsWith(".properties") || name == ".gitignore" -> FileBadge("P", Color(0xFF869587))
        name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
            name.endsWith(".webp") -> FileBadge("IMG", Color(0xFFBBB529))
        name.endsWith(".keystore") || name.endsWith(".jks") -> FileBadge("KS", Color(0xFF869587))
        else -> FileBadge("·", Color(0xFF869587))
    }
}

@Composable
fun FileTypeBadge(fileName: String, modifier: Modifier = Modifier) {
    val badge = fileBadge(fileName)
    AslText(
        text = badge.label,
        style = AslTheme.typography.uiLabelSmall.copy(fontWeight = FontWeight.Bold),
        color = badge.color,
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(min = 16.dp),
    )
}
