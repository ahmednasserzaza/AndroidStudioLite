package com.worldcup.androidstudiolite.designsystem.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.theme.LocalContentColor

val AslIconSize = 20.dp

@Composable
fun AslIcon(
    iconRes: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = AslIconSize,
    contentDescription: String? = null,
) {
    val resolved = tint.takeOrElse { LocalContentColor.current }
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .size(size)
            .paint(
                painter = painterResource(iconRes),
                colorFilter = ColorFilter.tint(resolved),
                contentScale = ContentScale.Fit,
            )
            .then(semantics),
    )
}
