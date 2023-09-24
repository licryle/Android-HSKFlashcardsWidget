package fr.berliat.hskwidget.ui.widgets

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetConfigureFragment

private const val ARG_WIDGETID = "WIDGETID"

/**
 * A simple [Fragment] subclass.
 * Use the [WidgetsWidgetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WidgetsWidgetFragment() : Fragment() {
    private var widgetId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            widgetId = it.getInt(ARG_WIDGETID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_widgets_widget, container, false)

        childFragmentManager.beginTransaction()
            .add(R.id.widgets_widget_container,
                 FlashcardWidgetConfigureFragment.newInstance(widgetId!!))
            .commit()

        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param widgetId The id the widget to configure
         * @return A new instance of fragment WidgetsWidgetFragment.
         */
        @JvmStatic
        fun newInstance(widgetId: Int) =
            WidgetsWidgetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }
}