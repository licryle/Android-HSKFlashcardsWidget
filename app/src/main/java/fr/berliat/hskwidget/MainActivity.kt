package fr.berliat.hskwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.billingclient.api.Purchase
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.databinding.ActivityMainBinding
import fr.berliat.hskwidget.domain.DatabaseBackup
import fr.berliat.hskwidget.domain.DatabaseBackupCallbacks
import fr.berliat.hskwidget.domain.SharedViewModel
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.OCR.CaptureImageFragmentDirections
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragment
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections
import fr.berliat.hskwidget.ui.utils.StrictModeManager
import fr.berliat.hskwidget.ui.widgets.WidgetsListFragmentDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), DatabaseBackupCallbacks {
    companion object {
        const val INTENT_SEARCH_WORD: String = "search_word"
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseBackup: DatabaseBackup
    private lateinit var supportDevStore: SupportDevStore
    private var showOCRReminder: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSharedViewModel()

        // Enable StrictMode in Debug mode
        StrictModeManager.init()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseBackup = DatabaseBackup(this, this, this)

        appConfig = AppPreferencesStore(applicationContext)

        setupSupporter()
        setupActionBar()

        setupSearchBtn()
        setupOCRBtn()

        setupReminderView()

        handleIntents(intent)
        handleBackUp()
    }

    private fun showOCRReminderIfActive() {
        if (!showOCRReminder) return

        val nav = findNavController(R.id.nav_host_fragment_content_main)
        val items = nav.currentBackStack.value

        var lastOCRDisplay: NavBackStackEntry? = null
        for (i in items.indices.reversed()) {
            // Handle item from last to first
            val item = items[i]

            if (i == items.size - 1 && item.destination.id == R.id.nav_ocr_read) {
                break // If we're in an OCR; we don't care about it
            }

            if (item.destination.id == R.id.nav_ocr_read) {
                lastOCRDisplay = item
                break
            }
        }

        val isOcrActive = lastOCRDisplay != null
        val ocrStatusBar = binding.appBarMain.navHostContentMain.navHostOcrIndicator
        ocrStatusBar.visibility = if (isOcrActive && showOCRReminder) View.VISIBLE else View.GONE
        binding.appBarMain.navHostContentMain.navHostOcrIndicatorClose.setOnClickListener {
            showOCRReminder = false
            ocrStatusBar.visibility = View.GONE
        }

        ocrStatusBar.setOnClickListener {
            nav.popBackStack(R.id.nav_ocr_read, false)
        }
    }

    fun setOCRReminderVisible() {
        showOCRReminder = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent) {
        handleWidgetConfigIntent(intent)
        handleSearchIntent(intent)
        handleTextSearchIntent(intent)
        handleImageOCRIntent(intent)
    }

    override fun onBackupFolderSet(uri: Uri) {
        Utils.getAppScope(applicationContext).launch(Dispatchers.IO) {
            val success = databaseBackup.backUp(uri)

            if (success) {
                databaseBackup.cleanOldBackups(uri, appConfig.dbBackUpMaxLocalFiles)
            }

            withContext(Dispatchers.Main) {
                if (success)
                    Toast.makeText(applicationContext, getString(R.string.dbbackup_success), Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(applicationContext, getString(R.string.dbbackup_failure_write), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onBackupFolderError() {
        Toast.makeText(applicationContext, getString(R.string.dbbackup_failure_folderpermission), Toast.LENGTH_LONG).show()
    }

    override fun onBackupFileSelected(uri: Uri) {
        throw Error("This should never be hit")
    }

    override fun onBackupFileSelectionCancelled() {
        throw Error("This should never be hit")
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun handleBackUp() {
        if (appConfig.dbBackUpActive)
            databaseBackup.getFolder()
    }

    private fun handleWidgetConfigIntent(intent: Intent?) {
        intent?.let {
            if (it.action == ACTION_APPWIDGET_CONFIGURE) {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val action = WidgetsListFragmentDirections.configureWidget(widgetId)
                    navController.navigate(action)
                }
            }
        }
    }

    private fun handleSearchIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(INTENT_SEARCH_WORD)) {
                val searchWord = it.getStringExtra(INTENT_SEARCH_WORD)
                Log.i(TAG, "Received a search intent: $searchWord")
                if (searchWord != null && searchWord != "") {
                    binding.appBarMain.appbarSearch.setQuery(searchWord, true)
                }
            }
        }
    }

    private fun handleTextSearchIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_PROCESS_TEXT && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            Log.i(TAG, "Received a shared text intent: $sharedText")
            if (sharedText != null) {
                binding.appBarMain.appbarSearch.setQuery(sharedText, true)
            }
        }
    }

    private fun handleImageOCRIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            Log.i(TAG, "Received a shared image intent")
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { imageUri ->
                // Handle the image URI here
                val action = CaptureImageFragmentDirections.displayOCR(imageUri.toString(), "")
                navController.navigate(action)
            }
        }

    }

    private fun setupSupporter() {
        supportDevStore = SupportDevStore.getInstance(applicationContext)

        updateSupportMenuTitle(appConfig.supportTotalSpent)

        supportDevStore.addListener(object : SupportDevStore.SupportDevListener {
            override fun onTotalSpentChange(totalSpent: Float) {
                updateSupportMenuTitle(totalSpent)
            }

            override fun onPurchaseSuccess(purchase: Purchase) { }

            override fun onPurchaseAcknowledgedSuccess(purchase: Purchase) { }

            override fun onPurchaseFailed(purchase: Purchase?, billingResponseCode: Int) { }
        })
    }

    private fun updateSupportMenuTitle(totalSpent: Float) {
        val tier = supportDevStore.getSupportTier(totalSpent)

        var tpl = R.string.menu_support
        if (totalSpent > 0) {
            tpl = R.string.support_status_tpl
        }

        val supStrId = supportDevStore.getSupportTierString(tier)
        val supportMenu = binding.navView.menu.findItem(R.id.nav_support)

        binding.navView.post {
            supportMenu.title = getString(tpl).format(getString(supStrId))
        }
    }

    private fun setupSharedViewModel() {
        SharedViewModel.getInstance(this)
    }

    private fun setupOCRBtn() {
        binding.appBarMain.appbarOcr.setOnClickListener { navController.navigate(R.id.nav_ocr_capture) }
    }

    private fun setupSearchBtn() {
        // Set listener to handle search queries
        binding.appBarMain.appbarSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Triggered when the search button is pressed (or search query submitted)
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (currentFocus != null)
                    Utils.hideKeyboard(applicationContext, currentFocus!!)

                return onQueryTextChange(query)
            }

            // Triggered when the query text is changed
            override fun onQueryTextChange(query: String?): Boolean {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()

                if (currentFragment is DictionarySearchFragment && currentFragment.isAdded && currentFragment.view != null) {
                    currentFragment.performSearch()
                } else {
                    val action = DictionarySearchFragmentDirections.search()
                    navController.navigate(action)
                }

                return true
            }
        })
    }

    private fun setupActionBar() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        setSupportActionBar(binding.appBarMain.toolbar)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dictionary, R.id.nav_widgets, R.id.nav_about, R.id.nav_lists,
                R.id.nav_ocr_read, R.id.nav_config, R.id.nav_support
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    private fun setupReminderView() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    showOCRReminderIfActive()
                }

                override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                    showOCRReminderIfActive()
                }
            },
            true // recursive = true, to catch nested fragments too
        )
    }
}