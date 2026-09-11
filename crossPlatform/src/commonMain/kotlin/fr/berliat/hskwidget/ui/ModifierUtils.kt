package fr.berliat.hskwidget.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dismissKeyboardOnTap() = composed {
    val focusManager = LocalFocusManager.current
    pointerInput(Unit) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
        })
    }
}

/**
 * Clears focus in the Initial pass.
 * Safe when applied to specific clickable components like Cards or Buttons.
 * This ensures the keyboard is dismissed even if the component consumes the tap.
 */
fun Modifier.dismissKeyboardOnClick() = composed {
    val focusManager = LocalFocusManager.current
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) {
                    focusManager.clearFocus()
                }
            }
        }
    }
}

fun Modifier.horizontalScrollbar(
    state: ScrollState,
    color: Color? = null,
    thickness: Dp = 3.dp,
    bottomPadding: Dp = 1.dp,
    horizontalMargin: Dp = 12.dp
): Modifier = composed {
    val barColor = color ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    drawWithContent {
        drawContent()
        if (state.maxValue > 0) {
            val viewportWidth = size.width - horizontalMargin.toPx() * 2
            if (viewportWidth > 0) {
                val contentWidth = state.maxValue + size.width
                val knobWidth = (size.width / contentWidth) * viewportWidth
                val scrollFraction = state.value.toFloat() / state.maxValue
                val knobX = horizontalMargin.toPx() + scrollFraction * (viewportWidth - knobWidth)
                val drawY = size.height - thickness.toPx() - bottomPadding.toPx()

                // Draw track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(horizontalMargin.toPx(), drawY),
                    size = Size(viewportWidth, thickness.toPx()),
                    cornerRadius = CornerRadius(thickness.toPx() / 2, thickness.toPx() / 2)
                )

                // Draw knob
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(knobX, drawY),
                    size = Size(knobWidth, thickness.toPx()),
                    cornerRadius = CornerRadius(thickness.toPx() / 2, thickness.toPx() / 2)
                )
            }
        }
    }
}
