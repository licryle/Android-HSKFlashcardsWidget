package fr.berliat.hskwidget.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.databinding.FragmentAboutBinding
import fr.berliat.hskwidget.domain.Utils

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

        return binding.root
    }
}