package fr.berliat.hskwidget.ui.about

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentAboutBinding
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.launch

class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView(requireContext(), "About")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        val emailMeIntent: () -> Unit = {
            Utils.sendEmail(requireContext(), "cyrille.berliat+hsk@gmail.com")
            Utils.logAnalyticsScreenView(requireContext(), "Email")
        }
        binding.aboutBtnEmail.setOnClickListener { emailMeIntent() }
        binding.aboutBtnEmail2.setOnClickListener { emailMeIntent() }

        binding.aboutBtnViewSource.setOnClickListener {
            startActivity(Utils.getOpenURLIntent("https://github.com/licryle/Android-HSKFlashcardsWidget"))
            Utils.logAnalyticsScreenView(requireContext(), "Github")
        }

        fetchAndDisplayStats()

        return binding.root
    }

    @SuppressLint("StringFormatMatches")
    private fun fetchAndDisplayStats() {
        Log.d("AboutFragment", "fetching stats")
        val db = ChineseWordsDatabase.getInstance(requireContext())
        val words = db.chineseWordDAO()
        val annotations = db.chineseWordAnnotationDAO()

        viewLifecycleOwner.lifecycleScope.launch {
            // Here we executed in the coRoutine Scope
            val wordsCnt = words.getCount()
            val annotationsCnt = annotations.getCount()

            // Switch back to the main thread to update UI
            // Update the UI with the result
            Log.d("AboutFragment", "stats fetched")
            binding.textStats.text = getString(R.string.about_stats_text, wordsCnt, annotationsCnt)
        }
    }
}