package fr.berliat.hskwidget.ui.application.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.app_name
import hskflashcardswidget.crossplatform.generated.resources.app_slogan
import hskflashcardswidget.crossplatform.generated.resources.ic_launcher
import hskflashcardswidget.crossplatform.generated.resources.nav_header_desc

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppDrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp), // matches your @dimen/nav_header_height,
        verticalArrangement = Arrangement.Bottom
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_launcher),
            contentDescription = stringResource(Res.string.nav_header_desc),
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Fit
        )

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
