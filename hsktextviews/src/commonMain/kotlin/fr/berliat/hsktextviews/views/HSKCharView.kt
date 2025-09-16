package fr.berliat.hsktextviews.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun HSKCharView(
    hanzi: Char,
    initialPinyin: String,
    pinyinEditable: Boolean = false,
    autoAddFlatTones: Boolean = false,
    onPinyinChange: (String) -> Unit = {},
    pinyinStyle: TextStyle,
    hanziStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    var pinyin by remember(initialPinyin) { mutableStateOf(initialPinyin) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
    ) {
        if (pinyinEditable) {
            HSKPinyinSelector(
                hanzi,
                pinyin,
                autoAddFlatTones,
                { p ->
                    pinyin = p
                    onPinyinChange(p)
                },
                pinyinStyle
            )
        } else {
            Text(
                text = pinyin,
                style = pinyinStyle,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Text(
            text = hanzi.toString(),
            style = hanziStyle
        )
    }
}
