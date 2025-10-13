package fr.berliat.hskwidget.ui.screens.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.ui.components.DetailedWordView
import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.screens.wordlist.WordListSelectionDialog

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.bookmark_add_24px
import fr.berliat.hskwidget.bookmark_heart_24px
import fr.berliat.hskwidget.dictionary_search_loading
import fr.berliat.hskwidget.dictionary_noresult_icon
import fr.berliat.hskwidget.dictionary_noresult_text
import fr.berliat.hskwidget.dictionary_search_filter_hasannotation_hint
import fr.berliat.hskwidget.dictionary_search_filter_hsk3definition_hint
import fr.berliat.hskwidget.ui.components.PrettyCardShapeModifier

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DictionarySearchScreen(
    onAnnotate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionarySearchViewModel = remember { DictionarySearchViewModel(
        prefsStore = HSKAppServices.appPreferences,
        annotatedChineseWordDAO = HSKAppServices.database.annotatedChineseWordDAO()
    ) }
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val hasMoreResults by viewModel.hasMoreResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val showHSK3 by viewModel.showHSK3.collectAsState()
    val hasAnnotationFilter by viewModel.hasAnnotationFilter.collectAsState()

    var showWordListDialog by remember { mutableStateOf<ChineseWord?>(null) }

    val listState = rememberLazyListState()

    // Whenever searchQuery changes, scroll to top
    LaunchedEffect(searchQuery) {
        viewModel.performSearch()
        listState.scrollToItem(0)
    }

    showWordListDialog?.let {
        WordListSelectionDialog(
            word = it,
            onDismiss = { showWordListDialog = null },
            onSaved = {
                viewModel.listsAssociationChanged()
                showWordListDialog = null
            },
            modifier = modifier
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filters row
        DictionarySearchFilters(
            showHSK3,
            { viewModel.toggleHSK3(it) },
            hasAnnotationFilter,
            { viewModel.toggleHasAnnotation(it) }
        )

        // Main content
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                LoadingView(loadingText = Res.string.dictionary_search_loading)
            } else if (results.isEmpty() && searchQuery.query.isNotEmpty()) {
                DictionarySearchNoResult(
                    query = searchQuery.query,
                    onClick = { onAnnotate(searchQuery.query) },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    itemsIndexed(results) { index, word ->
                        DetailedWordView(
                            word = word,
                            showHSK3Definition = showHSK3,
                            onFavoriteClick = { onAnnotate(word.simplified) },
                            onSpeakClick = { viewModel.speakWord(word.simplified) },
                            onCopyClick = { viewModel.copyWord(word.simplified) },
                            onListsClick = { showWordListDialog = word.word },
                            shapeModifier = when {
                                results.size == 1 -> PrettyCardShapeModifier.Single
                                index == 0 -> PrettyCardShapeModifier.First
                                index == results.size - 1 && !hasMoreResults -> PrettyCardShapeModifier.Last
                                else -> PrettyCardShapeModifier.Middle
                            }
                        )

                        if (!isLoading && !isLoadingMore
                            && hasMoreResults && index >= results.size - 5) {
                            // Trigger pagination
                            LaunchedEffect(Unit) { viewModel.loadMore() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionarySearchFilters(
    showHSK3: Boolean,
    onShowHSKToggle: (Boolean) -> Unit,
    hasAnnotation: Boolean,
    onHasAnnotationToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(start = 15.dp, end = 15.dp, top = 0.dp, bottom = 4.dp),
    ) {
        FilterChip(
            selected = showHSK3,
            onClick = { onShowHSKToggle(!showHSK3) },
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(end = 8.dp),
            label = {
                Text(
                    text = stringResource(Res.string.dictionary_search_filter_hsk3definition_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )

        FilterChip(
            selected = hasAnnotation,
            onClick = { onHasAnnotationToggle(!hasAnnotation) },
            shape = RoundedCornerShape(50),
            label = {
                Icon(
                    painter = painterResource(Res.drawable.bookmark_heart_24px),
                    contentDescription = stringResource(Res.string.dictionary_search_filter_hasannotation_hint),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.dictionary_search_filter_hasannotation_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}

@Composable
private fun DictionarySearchNoResult(
    query: String,
    onClick : ((String) -> Unit)?,
    modifier : Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick?.invoke(query) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(Res.drawable.bookmark_add_24px),
            contentDescription = stringResource(Res.string.dictionary_noresult_icon),
            modifier = modifier.size(48.dp)
        )
        Text(
            text = stringResource(Res.string.dictionary_noresult_text, query),
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(top = 8.dp)
        )
    }
}