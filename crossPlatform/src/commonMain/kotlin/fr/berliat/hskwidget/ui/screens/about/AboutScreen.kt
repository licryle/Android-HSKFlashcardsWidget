package fr.berliat.hskwidget.ui.screens.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.ui.components.IconButton

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.about_btn_email2
import hskflashcardswidget.crossplatform.generated.resources.about_btn_view_source
import hskflashcardswidget.crossplatform.generated.resources.about_intro1
import hskflashcardswidget.crossplatform.generated.resources.about_intro2
import hskflashcardswidget.crossplatform.generated.resources.about_stats_title
import hskflashcardswidget.crossplatform.generated.resources.about_terms_conditions_title
import hskflashcardswidget.crossplatform.generated.resources.about_intro3
import hskflashcardswidget.crossplatform.generated.resources.about_stats_text
import hskflashcardswidget.crossplatform.generated.resources.ic_email
import hskflashcardswidget.crossplatform.generated.resources.ic_github

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(viewModel: AboutViewModel) {
    val stats by viewModel.stats.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.registerVisit()
        viewModel.fetchStats()
    }

    @Composable
    fun Spacer() {
        Spacer(Modifier.height(12.dp))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openEmail) {
                Icon(
                    painter = painterResource(Res.drawable.ic_email),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(stringResource(Res.string.about_intro1, viewModel.version),
                    style = MaterialTheme.typography.bodyMedium)

                Spacer()

                IconButton(
                    stringResource(Res.string.about_btn_view_source),
                    { viewModel.onClickWebsite() },
                    Res.drawable.ic_github
                )

                Spacer()

                Text(stringResource(Res.string.about_intro2, viewModel.version),
                    style = MaterialTheme.typography.bodyMedium)

                Spacer()

                IconButton(
                    stringResource(Res.string.about_btn_email2),
                    viewModel::openEmail,
                    Res.drawable.ic_email
                )

                Spacer()

                Text(stringResource(Res.string.about_stats_title),
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        Res.string.about_stats_text,
                        stats.wordsCnt,
                        stats.annotationCnt
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer()

                Text(stringResource(Res.string.about_terms_conditions_title),
                    style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.about_intro3),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
