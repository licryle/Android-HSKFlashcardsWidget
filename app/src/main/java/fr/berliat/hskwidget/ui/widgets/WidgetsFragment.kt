package fr.berliat.hskwidget.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import fr.berliat.hskwidget.databinding.FragmentWidgetsBinding
import fr.berliat.hskwidget.ui.widget.FlashcardWidget

class WidgetsFragment : Fragment() {

    private var _binding: FragmentWidgetsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWidgetsBinding.inflate(inflater, container, false)

        val widgetsViewModel = ViewModelProvider(this)[WidgetsViewModel::class.java]

        binding.widgetsTabs.addOnTabSelectedListener (object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) widgetsViewModel.onToggleTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab != null) widgetsViewModel.onToggleTab(tab.position)
            }
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val tabsLayout: TabLayout = binding.widgetsTabs

        val widgetsViewModel =
            ViewModelProvider(this)[WidgetsViewModel::class.java]

        val prevTabPos = widgetsViewModel.getLastTabPosition()

        val context = requireActivity().applicationContext
        val appMgr = AppWidgetManager.getInstance(context!!)
        val widgetIds = appMgr.getAppWidgetIds(
            ComponentName(context, FlashcardWidget::class.java)
        )

        val widgetPager = binding.widgetsTabsConfigure
        widgetPager.adapter = WidgetPagerAdapter(childFragmentManager, lifecycle, widgetIds)
        TabLayoutMediator(tabsLayout, widgetPager) {
                tab, position ->
            tab.text = "Widget $position"
        }.attach()

        if (widgetIds.size > prevTabPos) {
            tabsLayout.selectTab(binding.widgetsTabs.getTabAt(prevTabPos))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    class WidgetPagerAdapter(fragMgr: FragmentManager, lifecycle: Lifecycle,
                             private val widgetIds : IntArray) :
        FragmentStateAdapter(fragMgr, lifecycle) {
        override fun getItemCount(): Int {
            return widgetIds.count()
        }

        override fun createFragment(position: Int): Fragment {
            return WidgetsWidgetFragment.newInstance(widgetIds[position])
        }

    }
}