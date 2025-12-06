package fr.berliat.hskwidget.ui.theme

import org.jetbrains.compose.resources.DrawableResource

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.cancel_24px
import fr.berliat.hskwidget.check_circle_24px
import fr.berliat.hskwidget.core.SnackbarType
import fr.berliat.hskwidget.info_24px
import fr.berliat.hskwidget.warning_24px

/**
 * Style configuration for a snackbar based on its type.
 */
data class SnackbarStyle(
    val containerColor: Color,
    val contentColor: Color,
    val iconLeft: DrawableResource? = null
)

/**
 * Returns the appropriate snackbar style for the given type using the app's color scheme.
 */
fun ColorScheme.snackbarStyleFor(type: SnackbarType): SnackbarStyle {
    return when (type) {
        SnackbarType.INFO -> SnackbarStyle(
            containerColor = Color(0xFF0870B6),
            contentColor = Color.White,
            iconLeft = Res.drawable.info_24px
        )
        
        SnackbarType.WARNING -> SnackbarStyle(
            containerColor = Color(0xFFFF9800),
            contentColor = Color.White,
            iconLeft = Res.drawable.warning_24px
        )
        
        SnackbarType.ERROR -> SnackbarStyle(
            containerColor = primary,
            contentColor = Color.White,
            iconLeft = Res.drawable.cancel_24px
        )

        SnackbarType.SUCCESS -> SnackbarStyle(
            containerColor = Color(0xFF5CB660),
            contentColor = Color.White,
            iconLeft = Res.drawable.check_circle_24px
        )
    }
}
