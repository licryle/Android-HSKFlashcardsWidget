package fr.berliat.hskwidget.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.baseline_refresh_24
import fr.berliat.hskwidget.baseline_volume_up_24
import fr.berliat.hskwidget.dictionary_item_reload
import fr.berliat.hskwidget.widget_btn_speak

@Composable
fun WidgetWordView(
    word: ChineseWord,
    onClickUpdate: () -> Unit = {},
    onClickSpeak: () -> Unit = {},
    onClickWord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Top row: reload - level - speak
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                iconRes = Res.drawable.baseline_refresh_24,
                contentDescriptionRes = Res.string.dictionary_item_reload,
                onClick = onClickUpdate,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = word.hskLevel.toString(),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.weight(1f))

            RoundIconButton(
                iconRes = Res.drawable.baseline_volume_up_24,
                contentDescriptionRes = Res.string.widget_btn_speak,
                onClick = onClickSpeak,
            )
        }

        // Middle content: text
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clickable(
                    enabled = true,
                    onClick = onClickWord
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word.pinyins.toString(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Text(
                text = word.simplified,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Text(
                text = word.definition[Locale.ENGLISH].toString(),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}