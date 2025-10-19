package fr.berliat.hskwidget.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (isSystemInDarkTheme()) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private val LightColors = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.Primary,
    onPrimaryContainer = Color.White,
    secondary = AppColors.Bronze,
    onSecondary = Color.White,
    secondaryContainer = AppColors.Primary.copy(alpha = 0.1f),
    onSecondaryContainer = Color.DarkGray,
    tertiary = AppColors.Bronze,
    tertiaryContainer = AppColors.Bronze.copy(alpha = 0.1f),
    onTertiaryContainer = AppColors.Bronze,
    background = Color(0xFFFDFDFD),
    surface = Color(0xFFEFEFEF),
    onSurface = Color(0xFF2C1C1C),
    onSurfaceVariant = Color.DarkGray,
    surfaceContainer = Color(0xFFF5F5F5),
    inverseOnSurface = AppColors.Bronze
)

private val DarkColors = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.Primary,
    onPrimaryContainer = Color.White,
    secondary = AppColors.Bronze,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.Primary.copy(alpha = 0.2f),
    onSecondaryContainer = Color.LightGray,
    tertiary = AppColors.Bronze,
    tertiaryContainer = AppColors.Bronze.copy(alpha = 0.2f),
    onTertiaryContainer = AppColors.Bronze,
    background = Color(0xFF121212),
    surface = Color(0xFF2C1C1C),
    onSurface = Color(0xFFEFEFEF),
    onSurfaceVariant = Color.Gray,
    surfaceContainer = Color(0xFF3C2C2C),
    inverseOnSurface = AppColors.Bronze
)


val widgetDefaultBox = Modifier
    .size(185.dp)
    .padding(5.dp)
    .border(
        width = 0.5.dp,
        color = AppColors.Primary,
        shape = RoundedCornerShape(20.dp))
