package fr.berliat.hskwidget.ui.screens.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.ui.components.IconButton

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.about_btn_email2
import fr.berliat.hskwidget.about_btn_view_source
import fr.berliat.hskwidget.about_intro1
import fr.berliat.hskwidget.about_intro2
import fr.berliat.hskwidget.about_stats_title
import fr.berliat.hskwidget.about_terms_conditions_title
import fr.berliat.hskwidget.about_intro3
import fr.berliat.hskwidget.about_stats_text
import fr.berliat.hskwidget.ic_email
import fr.berliat.hskwidget.ic_github
import fr.berliat.hskwidget.ui.components.AppDivider
import fr.berliat.hskwidget.ui.theme.AppSizes.screenWithFABBottomPadding

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = remember { AboutViewModel() }
) {
    val stats by viewModel.stats.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
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
                    text = stringResource(Res.string.about_btn_view_source),
                    onClick = { viewModel.onClickWebsite() },
                    modifier = Modifier.fillMaxWidth(),
                    drawable = Res.drawable.ic_github
                )

                Spacer()

                Text(stringResource(Res.string.about_intro2, viewModel.version),
                    style = MaterialTheme.typography.bodyMedium)

                Spacer()

                IconButton(
                    text = stringResource(Res.string.about_btn_email2),
                    onClick = viewModel::openEmail,
                    modifier = Modifier.fillMaxWidth(),
                    drawable = Res.drawable.ic_email
                )

                AppDivider()

                Text(stringResource(Res.string.about_stats_title),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(
                        Res.string.about_stats_text,
                        stats.wordsCnt,
                        stats.annotationCnt
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                AppDivider()

                Text(stringResource(Res.string.about_terms_conditions_title),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(Res.string.about_intro3),
                    style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(screenWithFABBottomPadding))
            }
        }
    }
}
