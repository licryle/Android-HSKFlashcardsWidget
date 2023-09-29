package fr.berliat.hskwidget.ui.home

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.databinding.FragmentHomeBinding
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import fr.berliat.hskwidget.ui.widgets.FlashcardFragment


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.homeText
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        with(childFragmentManager.beginTransaction()) {
            add(
                R.id.home_flashcard_fragment,
                FlashcardFragment.newInstance(0))
            commit()
        }

        binding.homeAddWidget.setOnClickListener {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val myProvider = ComponentName(requireContext(), FlashcardWidgetProvider::class.java)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val confIntent = Intent(context, FlashcardWidgetProvider::class.java)
                confIntent.action = FlashcardWidgetProvider.ACTION_CONFIGURE_LATEST

                val callbackIntent = PendingIntent.getBroadcast(
                    /* context = */ context,
                    /* requestCode = */ 0,
                    /* intent = */ confIntent,
                    /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                appWidgetManager.requestPinAppWidget(myProvider, null, callbackIntent)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}