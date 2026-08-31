package fr.berliat.hskwidget.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

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
