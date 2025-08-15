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
import fr.berliat.hskwidget.ui.widget.FlashcardFragment
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider


class WidgetsListFragment : Fragment() {
    private var _binding: FragmentWidgetsBinding? = null
    private var _viewModel: WidgetsListViewModel? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel get() = _viewModel!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val previewWidget = FlashcardFragment.newInstance(0)
        with(childFragmentManager.beginTransaction()) {
            add(
                R.id.widgets_demoflashcard_fragment,
                previewWidget)
            commit()
        }
        childFragmentManager.executePendingTransactions()
        previewWidget.updateWord()
    }

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

    override fun onResume() {
        super.onResume()

        val widgetsIntro = binding.widgetsIntro
        val tabsLayout = binding.widgetsTabs
        val widgetPager = binding.widgetsTabsConfigure
        val demoFlashcard = binding.widgetsDemoflashcardFragment

        val context = requireActivity().applicationContext
        val widgetIds = FlashcardWidgetProvider().getWidgetIds(context)


        if (widgetIds.isEmpty()) {
            tabsLayout.visibility = View.GONE
            widgetPager.visibility = View.GONE
            demoFlashcard.visibility = View.VISIBLE
            widgetsIntro.visibility = View.VISIBLE
        } else {
            tabsLayout.visibility = View.VISIBLE
            widgetPager.visibility = View.VISIBLE
            demoFlashcard.visibility = View.GONE
            widgetsIntro.visibility = View.GONE

            widgetPager.adapter = WidgetPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, widgetIds)
            TabLayoutMediator(tabsLayout, widgetPager) { tab, position ->
                tab.text = "Widget $position"
            }.attach()

            // Handle intent asking to configure
            handleIntent(arguments)

            val prevTabPos = viewModel.getLastTabPosition()
            if (prevTabPos < widgetIds.size) {
                tabsLayout.selectTab(binding.widgetsTabs.getTabAt(prevTabPos))
            }
        }

        Utils.logAnalyticsScreenView("WidgetsList")
    }

    private fun handleIntent(arguments: Bundle?) {
        if (arguments == null) return

        val intentWidgetId = arguments.getInt("widgetId", AppWidgetManager.INVALID_APPWIDGET_ID)
        if (intentWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val context = requireActivity().applicationContext
            val widgetIds = FlashcardWidgetProvider().getWidgetIds(context)

            val position = widgetIds.indexOf(intentWidgetId)
            viewModel.onToggleTab(position)
            Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, intentWidgetId)
            // Consume condition so we don't come back here until next intent
            arguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

            val adapter = binding.widgetsTabsConfigure.adapter as WidgetPagerAdapter
            adapter.setFragmentToFireIntent(intentWidgetId)
        }
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
        // TODO: Someday, is the API evolves, find a better way to access the fragment that cache
        // it here. I don't know when items are removed.
        private val fragMap = mutableMapOf<Int, WidgetsWidgetConfPreviewFragment>()
        private val fragToFireIntent = mutableListOf<Int>()

        override fun getItemCount(): Int {
            return widgetIds.count()
        }

        override fun createFragment(position: Int): WidgetsWidgetConfPreviewFragment {
            val widgetId = widgetIds[position]
            val newFragment = WidgetsWidgetConfPreviewFragment.newInstance(widgetId)
            fragMap[widgetId] = newFragment

            if (fragToFireIntent.contains(widgetId)) {
                newFragment.widgetExpectsIntent = true
                fragToFireIntent.remove(widgetId)
            }

            return newFragment
        }

        fun setFragmentToFireIntent(widgetId: Int) {
            if (fragMap.contains(widgetId)) {
                fragMap[widgetId]!!.widgetExpectsIntent = true
            } else {
                fragToFireIntent.add(widgetId)
            }
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