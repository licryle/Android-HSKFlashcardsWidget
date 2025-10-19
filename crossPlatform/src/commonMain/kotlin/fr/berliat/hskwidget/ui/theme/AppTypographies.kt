package fr.berliat.hskwidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object AppTypographies {
    val pinyin: TextStyle
        @Composable get() = TextStyle(
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


    val hanzi: TextStyle
        @Composable get() = TextStyle(
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

    val clickedPinyin: TextStyle
        @Composable get() = pinyin

    val clickedHanzi
        @Composable get() = hanzi

    val smallestHanziFontSize = 10.sp

    val detailCardSubTitle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.Underline
    )
}