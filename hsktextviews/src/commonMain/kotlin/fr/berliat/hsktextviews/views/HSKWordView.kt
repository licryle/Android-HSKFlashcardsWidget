import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import fr.berliat.hsktextviews.PinyinUtils.isHanzi
import fr.berliat.hsktextviews.views.HSKCharView

@Composable
fun HSKWordView(
    hanziText: String,
    pinyinText: String,
    pinyinEditable: Boolean = false,
    isClicked: Boolean = false,
    onWordClick: ((String) -> Unit)? = null,
    pinyinStyle: TextStyle = TextStyle(
        fontSize = 18.sp,
        color = Color.Gray
    ),
    hanziStyle: TextStyle = TextStyle(
        fontSize = 30.sp,
        color = Color.Black,
        fontWeight = FontWeight.Bold
    ),
    clickedPinyinStyle: TextStyle = TextStyle(
        fontSize = 18.sp,
        color = Color.White,
    ),
    clickedHanziStyle: TextStyle = TextStyle(
        fontSize = 30.sp,
        color = Color.White,
        fontWeight = FontWeight.Bold
    ),
    clickedBackgroundColor: Color = Color.Black,
    onPinyinChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pinyinList = remember(pinyinText) { pinyinText.split(" ").map { mutableStateOf(it) } }

    Row(
        modifier = modifier.clickable { onWordClick?.invoke(hanziText) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        var pinyinIndex = -1
        hanziText.forEachIndexed { index, hanzi ->
            val pinyin = if (hanzi.isHanzi()) {
                pinyinIndex++

                if (pinyinIndex >= pinyinList.size) {
                    ""
                } else { // Did not "getOrElse" because it could create an element and cause a refresh
                    pinyinList[pinyinIndex].value
                }
            } else {
                ""
            }

            HSKCharView(
                hanzi,
                pinyin,
                pinyinEditable,
                index == hanziText.length - 1,
                { newPinyin ->
                    pinyinList[pinyinIndex].value = newPinyin
                    onPinyinChange(pinyinList.map {it.value}.joinToString(" "))
                },
                if (isClicked) clickedPinyinStyle else pinyinStyle,
                if (isClicked) clickedHanziStyle else hanziStyle,
                modifier = Modifier.background(if (isClicked) clickedBackgroundColor else Color.Transparent)
            )
        }
    }
}