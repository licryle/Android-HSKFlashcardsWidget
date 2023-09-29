package fr.berliat.hskwidget.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.databinding.FragmentAboutBinding
import fr.berliat.hskwidget.domain.Utils

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val emailMeIntent: () -> Unit =
            { Utils.sendEmail(requireContext(), "cyrille.berliat+hsk@gmail.com") }
        binding.aboutBtnEmail.setOnClickListener { emailMeIntent() }
        binding.aboutBtnEmail2.setOnClickListener { emailMeIntent() }

        binding.aboutBtnViewSource.setOnClickListener {
            startActivity(Utils.getOpenURLIntent("https://github.com/licryle/Android-HSKFlashcardsWidget"))
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}