package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.Utils

import fr.berliat.hskwidget.domain.SearchQuery
import fr.berliat.hskwidget.ui.screens.dictionary.DictionarySearchScreen
import fr.berliat.hskwidget.ui.screens.dictionary.DictionaryViewModel
import fr.berliat.hskwidget.ui.HSKAnkiDelegate

class DictionarySearchFragment : Fragment() {
    private lateinit var ankiDelegate: HSKAnkiDelegate

    private val viewModel = DictionaryViewModel(
        { SearchQuery.fromString(activity?.findViewById<SearchView>(R.id.appbar_search)?.query.toString()) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ankiDelegate = HSKAnkiDelegate(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DictionarySearchScreen(
                    viewModel = viewModel,
                    ankiCaller = ankiDelegate::delegateToAnki,
                    onSpeak = Utils::playWordInBackground,
                    onAnnotate = {
                        val action = DictionarySearchFragmentDirections.annotateWord(it, false)

                        findNavController().navigate(action)
                    },
                    onCopy = Utils::copyToClipBoard
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        performSearch()
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("DictionarySearch")
    }

    fun performSearch() {
        viewModel.performSearch()
    }
}

