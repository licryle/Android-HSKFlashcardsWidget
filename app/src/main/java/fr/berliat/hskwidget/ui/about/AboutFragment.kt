package fr.berliat.hskwidget.ui.about

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.databinding.FragmentAboutBinding
import fr.berliat.hskwidget.domain.Utils

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private var _context: Context? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @get:JvmName("getContext2")
    private val context get() = _context!!

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView(requireContext(), "About")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _context = requireContext()

        val emailMeIntent: () -> Unit = {
            Utils.sendEmail(requireContext(), "cyrille.berliat+hsk@gmail.com")
            Utils.logAnalyticsScreenView(context, "Email")
        }
        binding.aboutBtnEmail.setOnClickListener { emailMeIntent() }
        binding.aboutBtnEmail2.setOnClickListener { emailMeIntent() }

        binding.aboutBtnViewSource.setOnClickListener {
            startActivity(Utils.getOpenURLIntent("https://github.com/licryle/Android-HSKFlashcardsWidget"))
            Utils.logAnalyticsScreenView(context, "Github")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}