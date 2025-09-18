package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.OldAppPreferencesStore
import fr.berliat.hskwidget.domain.Utils

import fr.berliat.hskwidget.domain.SearchQuery
import fr.berliat.hskwidget.ui.screens.dictionary.DictionarySearchScreen
import fr.berliat.hskwidget.ui.screens.dictionary.DictionaryViewModel
import fr.berliat.hskwidget.ui.wordlist.WordListSelectionDialog

class DictionarySearchFragment : Fragment() {
    private lateinit var appConfig: OldAppPreferencesStore

    private val viewModel = DictionaryViewModel(
        { SearchQuery.fromString(activity?.findViewById<SearchView>(R.id.appbar_search)?.query.toString()) }
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        appConfig = OldAppPreferencesStore(requireContext())

        ComposeView(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                DictionarySearchScreen(
                    viewModel,
                    onSpeak = { Utils.playWordInBackground(requireContext(), it) },
                    onAnnotate = {
                        val action = DictionarySearchFragmentDirections.annotateWord(it, false)

                        findNavController().navigate(action)
                    },
                    onCopy = { Utils.copyToClipBoard(requireContext(), it) },
                    onListChange = {
                        val dialog = WordListSelectionDialog.newInstance(it)
                        dialog.onSave = { performSearch() }
                        dialog.show((context as FragmentActivity).supportFragmentManager, "WordListSelectionDialog")
                    }
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

    companion object {
        const val TAG = "DictionarySearchFragment"
    }
}

