package com.worldcup.androidstudiolite.designsystem.components.indicators

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldcup.androidstudiolite.designsystem.theme.AslTheme

@Composable
fun AslLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val fill = color.takeOrElse { AslTheme.colors.primaryContainer }
    val track = AslTheme.colors.surfaceContainerHighest
    Canvas(
        modifier
            .fillMaxWidth()
            .height(4.dp),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        drawRoundRect(
            fill,
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = radius,
        )
    }
}

@Composable
fun AslIndeterminateLinearProgress(
    modifier: Modifier = Modifier,
) {
    val fill = AslTheme.colors.primaryContainer
    val track = AslTheme.colors.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "linearProgress")
    val head by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "linearProgressHead",
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(4.dp),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        val segment = 0.35f
        val start = (head - segment).coerceAtLeast(0f)
        val end = head.coerceAtMost(1f)
        if (end > start) {
            drawRoundRect(
                fill,
                topLeft = Offset(size.width * start, 0f),
                size = Size(size.width * (end - start), size.height),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
fun AslCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = Color.Unspecified,
) {
    val stroke = color.takeOrElse { AslTheme.colors.primaryContainer }
    val transition = rememberInfiniteTransition(label = "circularProgress")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "circularProgressAngle",
    )
    Canvas(modifier.size(size)) {
        drawArc(
            color = stroke,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
