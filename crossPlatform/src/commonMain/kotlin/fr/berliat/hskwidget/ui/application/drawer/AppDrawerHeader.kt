package fr.berliat.hskwidget.ui.application.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        verticalArrangement = Arrangement.Top
    ) {
        val leftPadding = 30.dp
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .height(100.dp)
            .padding(0.dp)) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher),
                contentDescription = stringResource(Res.string.nav_header_desc),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp),
                contentScale = ContentScale.Fit
            )
        }

        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .align(Alignment.Start)) {
            Column(
                modifier = Modifier.padding(top = 9.dp, bottom = 9.dp)
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = leftPadding)
                )

                Text(
                    text = stringResource(Res.string.app_slogan),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = leftPadding)
                )
            }
        }
    }
}
