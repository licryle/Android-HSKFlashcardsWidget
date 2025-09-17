package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.domain.Utils

import fr.berliat.hskwidget.ui.screens.annotate.AnnotateScreen
import fr.berliat.hskwidget.ui.screens.annotate.AnnotateViewModel
import fr.berliat.hskwidget.ui.utils.HSKAnkiDelegate

class AnnotateFragment: Fragment() {
    private lateinit var annotateViewModel: AnnotateViewModel
    private lateinit var ankiDelegate: HSKAnkiDelegate

    private var pinyins = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle(getString(R.string.menu_annotate))

        ankiDelegate = HSKAnkiDelegate(this)
    }

    private fun setActionBarTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        annotateViewModel = AnnotateViewModel(requireContext(), WordListRepository(requireContext()), null)// TODO: ankiDelegate::delegateToAnki)

        val simplifiedWord = arguments?.getString("simplifiedWord") ?: ""

        val baseView = ComposeView(requireContext())
        Utils.hideKeyboard(requireContext(), baseView)

        // Use ComposeView and setContent with a proper @Composable lambda
        return baseView.apply {
            setContent {
                //binding.annotationEditClassType.setSelection(prefStore.lastAnnotatedClassType.ordinal)
                //binding.annotationEditClassLevel.setSelection(prefStore.lastAnnotatedClassLevel.ordinal)
                AnnotateScreen(simplifiedWord, annotateViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("Annotate")
    }

    enum class ACTION {
        UPDATE,
        DELETE
    }
}
