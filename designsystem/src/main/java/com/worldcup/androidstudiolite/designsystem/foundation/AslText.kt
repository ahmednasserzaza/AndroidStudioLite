package com.worldcup.androidstudiolite.designsystem.foundation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme
import com.worldcup.androidstudiolite.designsystem.theme.LocalContentColor

@Composable
fun AslText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AslTheme.typography.uiBody,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = resolved, textAlign = textAlign ?: style.textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AslText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = AslTheme.typography.uiBody,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = resolved, textAlign = textAlign ?: style.textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}
