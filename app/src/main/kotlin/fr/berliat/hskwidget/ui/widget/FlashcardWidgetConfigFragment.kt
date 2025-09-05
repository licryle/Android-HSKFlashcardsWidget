package fr.berliat.hskwidget.ui.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.WidgetListEntry
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.databinding.FlashcardWidgetConfigureBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlashcardWidgetConfigFragment() : Fragment() {
    private lateinit var viewBinding: FlashcardWidgetConfigureBinding
    private var _widgetId: Int? = null
    private val prefListeners = mutableListOf<WidgetPreferenceListener>()
    private suspend fun widgetListsDAO() = withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(requireContext()).widgetListDAO()
    }
    private suspend fun wordListDAO() = withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(requireContext()).wordListDAO()
    }

    private val switchList = mutableMapOf<Long, MaterialSwitch>()

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    override fun onCreate(savedInstanceState: Bundle?) {
        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding =
            FlashcardWidgetConfigureBinding.inflate(inflater, container, false) // Inflate here

        lifecycleScope.launch(Dispatchers.IO) {
            val widgetList = widgetListsDAO().getListsForWidget(widgetId)
            val allLists = wordListDAO().getAllLists()

            withContext(Dispatchers.Main) {
                for (list in allLists) {
                    val newView = inflater.inflate(R.layout.flashcard_widget_configure_list, container, false)
                    newView.findViewById<TextView>(R.id.flashcard_widget_configure_list_label).text = list.name

                    newView.findViewById<TextView>(R.id.flashcard_widget_configure_list_wordcount)
                        .text = requireContext().getString(R.string.wordlist_word_count).format(list.wordCount)

                    val switch = newView.findViewById<MaterialSwitch>(R.id.flashcard_widget_configure_list_switch)
                    switch.isChecked = list.id in widgetList
                    switch.setOnClickListener { fireWidgetPreferenceChange(list.id, switch.isChecked) }

                    switchList[list.id] = switch

                    viewBinding.flashcardConfigureContainer.addView(newView)
                }
            }
        }

        viewBinding.flashcardWidgetConfigureAddwidget.setOnClickListener {
            val entriesToAdd = mutableListOf<WidgetListEntry>()
            for ((listId, switch) in switchList) {
                if (switch.isChecked) {
                    entriesToAdd.add(WidgetListEntry(widgetId, listId))
                }
            }

            if (entriesToAdd.isEmpty()) {
                fireWidgetPreferenceEmpty()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    widgetListsDAO().deleteWidget(widgetId)
                    widgetListsDAO().insertListsToWidget(entriesToAdd)

                    withContext(Dispatchers.Main) {
                        fireWidgetPreferenceSaved()
                    }
                }
            }
        }


        return viewBinding.root // Return the root v
    }

    fun addWidgetPreferenceListener(listener: WidgetPreferenceListener) {
        prefListeners.add(listener)
    }

    fun removeWidgetPreferenceListener(listener: WidgetPreferenceListener) {
        prefListeners.remove(listener)
    }

    private fun fireWidgetPreferenceChange(listId: Long, included: Boolean) {
        prefListeners.forEach() {
            it.onWidgetPreferenceChange(widgetId, listId, included)
        }
    }

    private fun fireWidgetPreferenceEmpty() {
        prefListeners.forEach() {
            it.onWidgetPreferenceEmpty(widgetId)
        }
    }

    private fun fireWidgetPreferenceSaved() {
        prefListeners.forEach() {
            it.onWidgetPreferenceSaved(widgetId)
        }
    }

    companion object {
        const val TAG = "FlashcardWidgetConfigFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param widgetId The id the widget to configure
         * @return A new instance of fragment WidgetsWidgetConfPreviewFragment.
         */
        @JvmStatic
        fun newInstance(widgetId: Int) =
            FlashcardWidgetConfigFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }

    interface WidgetPreferenceListener {
        fun onWidgetPreferenceChange(widgetId: Int, listId: Long, included: Boolean)
        fun onWidgetPreferenceSaved(widgetId: Int)
        fun onWidgetPreferenceEmpty(widgetId: Int)
    }
}