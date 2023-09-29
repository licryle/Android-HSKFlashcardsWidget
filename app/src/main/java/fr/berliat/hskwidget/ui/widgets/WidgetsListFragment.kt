package fr.berliat.hskwidget.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.databinding.FragmentWidgetsBinding
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.flashcard.FlashcardFragment
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider

class WidgetsListFragment : Fragment() {
    private var _binding: FragmentWidgetsBinding? = null
    private var _viewModel: WidgetsListViewModel? = null
    private val _previewFragment: FlashcardFragment? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel get() = _viewModel!!
    private val previewFragment get() = _previewFragment!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWidgetsBinding.inflate(inflater, container, false)
        _viewModel = ViewModelProvider(this)[WidgetsListViewModel::class.java]

        binding.widgetsTabs.addOnTabSelectedListener(TabChangeListener(viewModel))
        binding.widgetsAddNewWidget.setOnClickListener { addNewWidget() }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(childFragmentManager.beginTransaction()) {
            add(
                R.id.widgets_demoflashcard_fragment,
                FlashcardFragment.newInstance(0))
            commit()
        }
    }

    override fun onDestroy() {
        childFragmentManager.beginTransaction().remove(previewFragment).commitAllowingStateLoss()

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        val tabsLayout = binding.widgetsTabs
        val widgetPager = binding.widgetsTabsConfigure
        val demoFlashcard = binding.widgetsDemoflashcardFragment

        val context = requireActivity().applicationContext
        val widgetIds = FlashcardWidgetProvider().getWidgetIds(context)
        if (widgetIds.isEmpty()) {
            tabsLayout.visibility = View.GONE
            widgetPager.visibility = View.GONE
            demoFlashcard.visibility = View.VISIBLE
        } else {
            tabsLayout.visibility = View.VISIBLE
            widgetPager.visibility = View.VISIBLE
            demoFlashcard.visibility = View.GONE

            val prevTabPos = viewModel.getLastTabPosition()

            widgetPager.adapter = WidgetPagerAdapter(childFragmentManager, lifecycle, widgetIds)
            TabLayoutMediator(tabsLayout, widgetPager) { tab, position ->
                tab.text = "Widget $position"
            }.attach()

            if (prevTabPos < widgetIds.size) {
                tabsLayout.selectTab(binding.widgetsTabs.getTabAt(prevTabPos))
            }
        }

        Utils.logAnalyticsScreenView(requireContext(), "WidgetsList")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    fun addNewWidget() {
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

    class WidgetPagerAdapter(fragMgr: FragmentManager, lifecycle: Lifecycle,
                             private val widgetIds : IntArray) :
        FragmentStateAdapter(fragMgr, lifecycle) {
        override fun getItemCount(): Int {
            return widgetIds.count()
        }

        override fun createFragment(position: Int): Fragment {
            return WidgetsWidgetConfPreviewFragment.newInstance(widgetIds[position])
        }

    }

    class TabChangeListener(private val viewModel :WidgetsListViewModel)
        : TabLayout.OnTabSelectedListener{
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if (tab != null) viewModel.onToggleTab(tab.position)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {

        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            if (tab != null) viewModel.onToggleTab(tab.position)
        }
    }
}