package fr.berliat.hskwidget.ui.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import fr.berliat.hskwidget.databinding.FragmentSupportBinding

class SupportFragment : Fragment() {
    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SupportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val activity = requireActivity()
        binding.supportPurchaseTier1.setOnClickListener { viewModel.makePurchase(activity, it.tag.toString()) }
        binding.supportPurchaseTier2.setOnClickListener { viewModel.makePurchase(activity, it.tag.toString()) }
        binding.supportPurchaseTier3.setOnClickListener { viewModel.makePurchase(activity, it.tag.toString()) }

        viewModel.tierIcon.observe(viewLifecycleOwner) { resId ->
            binding.tierIcon.setImageResource(resId)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
