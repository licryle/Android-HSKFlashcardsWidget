package fr.berliat.hskwidget.ui.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.databinding.FragmentSupportBinding
import fr.berliat.hskwidget.domain.Utils

class SupportFragment : Fragment() {
    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel : SupportViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = SupportViewModel(requireActivity().application, ::toast)

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

        viewModel.purchaseList.observe(viewLifecycleOwner) { products ->
            binding.supportPurchaseTier1.visibility =
                Utils.hideViewIf(products.containsKey(SupportDevStore.SupportProduct.TIER1))
            binding.supportPurchaseTier2.visibility =
                Utils.hideViewIf(products.containsKey(SupportDevStore.SupportProduct.TIER2))
            binding.supportPurchaseTier3.visibility =
                Utils.hideViewIf(products.containsKey(SupportDevStore.SupportProduct.TIER3))
        }

        viewModel.fetchPurchases()

        return binding.root
    }

    private fun toast(resId: Int) {
        if (context == null) return

        Toast.makeText(requireContext(), requireContext().getString(resId), Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
