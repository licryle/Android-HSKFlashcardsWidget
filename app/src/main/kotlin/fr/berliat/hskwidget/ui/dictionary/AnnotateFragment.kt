package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.Utils

import fr.berliat.hskwidget.ui.screens.annotate.AnnotateScreen
import fr.berliat.hskwidget.ui.HSKAnkiDelegate

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_failure
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_success
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_save_success
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_save_failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.compose.resources.getString

class AnnotateFragment: Fragment() {
    private lateinit var ankiDelegate: HSKAnkiDelegate

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
        val simplifiedWord = arguments?.getString("simplifiedWord") ?: ""

        val baseView = ComposeView(requireContext())
        fr.berliat.hskwidget.domain.Utils.hideKeyboard(requireContext(), baseView)

        // Use ComposeView and setContent with a proper @Composable lambda
        return baseView.apply {
            setContent {
                AnnotateScreen(
                    simplifiedWord,
                    ankiCaller = ankiDelegate::delegateToAnki,
                    onSave = { word, e -> handleSaveResult(word, ACTION.UPDATE, e)},
                    onDelete = { word, e -> handleSaveResult(word, ACTION.DELETE, e)}
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("Annotate")
    }

    fun handleSaveResult(word: String, action: ACTION, e: Exception?) {
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            val msgRes = getString(when {
                action == ACTION.DELETE && e == null -> Res.string.annotation_edit_delete_success
                action == ACTION.DELETE && e != null -> Res.string.annotation_edit_delete_failure
                action == ACTION.UPDATE && e == null -> Res.string.annotation_edit_save_success
                action == ACTION.UPDATE && e != null -> Res.string.annotation_edit_save_failure
                else -> throw (Exception()) // We'll crash
            }, word, e?.message ?: "")

            withContext(Dispatchers.Main) {
                if (e == null) {
                    Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    enum class ACTION {
        UPDATE,
        DELETE
    }
}
