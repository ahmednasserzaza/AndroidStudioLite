package com.worldcup.androidstudiolite.designsystem.foundation

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

object AslIndication : IndicationNodeFactory {

    private const val PRESSED_ALPHA = 0.08f

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        AslIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()

    private class AslIndicationNode(
        private val interactionSource: InteractionSource,
    ) : Modifier.Node(), DrawModifierNode {

        private var pressed by mutableStateOf(false)

        override fun onAttach() {
            coroutineScope.launch {
                var pressCount = 0
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressCount++
                        is PressInteraction.Release -> pressCount--
                        is PressInteraction.Cancel -> pressCount--
                    }
                    pressed = pressCount > 0
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (pressed) {
                drawRect(Color.White.copy(alpha = PRESSED_ALPHA))
            }
        }
    }
}

fun Modifier.aslClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(
        interactionSource = null,
        indication = AslIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}

fun Modifier.clickableWithNoFeedback(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}
