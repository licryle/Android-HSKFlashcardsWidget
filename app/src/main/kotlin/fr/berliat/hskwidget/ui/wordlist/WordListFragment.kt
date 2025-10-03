package fr.berliat.hskwidget.ui.wordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.screens.wordlist.WordListScreen

class WordListFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("WordListFragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                WordListScreen(
                    ankiCaller = HSKAppServices.ankiDelegator,
                    onClickList = { list -> consultList(list) }
                )
            }
        }
    }

    private fun consultList(list: WordList) {
        val query = "list:\"${list.name}\""
        val searchView = requireActivity().findViewById<SearchView>(R.id.appbar_search)
        searchView.setQuery(query, true)
    }
}