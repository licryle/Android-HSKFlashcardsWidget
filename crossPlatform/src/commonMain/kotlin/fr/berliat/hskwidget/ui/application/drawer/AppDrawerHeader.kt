package fr.berliat.hskwidget.ui.application.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.app_name
import fr.berliat.hskwidget.app_slogan
import fr.berliat.hskwidget.ic_launcher
import fr.berliat.hskwidget.nav_header_desc

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppDrawerHeader() {
    Row(modifier = Modifier
            .height(70.dp)
            .fillMaxWidth()
            .padding(start = 0.dp)) {
        Image(
            painter = painterResource(Res.drawable.ic_launcher),
            contentDescription = stringResource(Res.string.nav_header_desc),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(80.dp)
                .width(80.dp)
                .padding(0.dp)
        )

        Column(
            modifier = Modifier
                .padding(end = 8.dp, bottom = 4.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = stringResource(Res.string.app_slogan),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
