package fr.berliat.hskwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RoundIconButton(
    iconRes : DrawableResource,
    contentDescriptionRes : StringResource,
    onClick : () -> Unit,
    modifier : Modifier = Modifier,
    tint : Color? = null
) {
    IconButton(
        onClick = { onClick.invoke() },
        ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(contentDescriptionRes),
            tint = tint ?: MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = modifier
                .size(33.dp)           // button size
                .clip(CircleShape)           // make it round
                .background(MaterialTheme.colorScheme.secondaryContainer) // background color
                .padding(5.dp)
        )
    }
}