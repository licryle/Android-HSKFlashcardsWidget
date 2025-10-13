package fr.berliat.hsktextviews.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

import fr.berliat.hsktextviews.PinyinUtils
import fr.berliat.hsktextviews.Res
import fr.berliat.hsktextviews.arrow_dropdown_noborder
import fr.berliat.hsktextviews.pinyinselector_dropdown_icon

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HSKPinyinSelector(
    hanzi: Char,
    initialPinyin: String?,
    autoAddFlatTones: Boolean = false,
    onSelectPinyin: (String) -> Unit = {},
    textStyle: TextStyle = TextStyle.Default
) {
    var selectedPinyin by remember { mutableStateOf(initialPinyin ?: "" ) }
    var expanded by remember { mutableStateOf(false) }

    // Get all pinyins for that Char
    val h2p = PinyinUtils.hanzi2Pinyin
    var pinyinOptions = mutableSetOf<String>()
    try {
        pinyinOptions = h2p.getPinyin(hanzi).map { h2p.numberedToTonal(it) }.toMutableSet()

        if (autoAddFlatTones) {
            pinyinOptions.addAll(pinyinOptions.map { h2p.pinyinToToneless(it) })
        }
    } catch (_: Exception) {
        //Log.e("HSKPinyinSelector", "Got asked for a seldom used HanZi")
    }

    if (! pinyinOptions.contains(selectedPinyin) && pinyinOptions.isNotEmpty()) {
        selectedPinyin = pinyinOptions.elementAtOrElse(0) { "" }
        onSelectPinyin(selectedPinyin)
    }

    val showDropdown = pinyinOptions.size > 1

    Box {
        Row(
            modifier = Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = selectedPinyin, style = textStyle)

            if (showDropdown) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_dropdown_noborder),
                    contentDescription = stringResource(Res.string.pinyinselector_dropdown_icon),
                    tint = textStyle.color,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        if (showDropdown) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                pinyinOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, style = textStyle) },
                        onClick = {
                            selectedPinyin = option
                            expanded = false
                            onSelectPinyin(option)
                        }
                    )
                }
            }
        }
    }
}
