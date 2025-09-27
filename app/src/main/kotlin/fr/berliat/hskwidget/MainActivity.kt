package fr.berliat.hskwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import fr.berliat.hskwidget.ExpectedUtils.INTENT_SEARCH_WORD
import fr.berliat.hskwidget.core.AppServices
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.databinding.ActivityMainBinding
import fr.berliat.hskwidget.domain.DatabaseDiskBackup
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.domain.getParcelableExtraCompat
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragment
import fr.berliat.hskwidget.ui.utils.StrictModeManager
import fr.berliat.hskwidget.ui.widget.WidgetProvider
import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.support_status_tpl
import hskflashcardswidget.crossplatform.generated.resources.support_total_error
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.AppContext
import org.jetbrains.compose.resources.getString

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseDiskBackup: DatabaseDiskBackup
    private lateinit var supportDevStore: SupportDevStore
    private var showOCRReminder: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ExpectedUtils.init { this }
        HSKAppServices.init(lifecycleScope)

        // TODO: Temporary until moved to compose
        runBlocking {
            HSKAppServices.status
                .filter { it is AppServices.Status.Ready || it is AppServices.Status.Failed}
                .first()
        }
        appConfig = HSKAppServices.appPreferences

        WidgetProvider.init({ applicationContext })
        AppContext.apply { set(applicationContext) }
        FileKit.init(this)

        // Enable StrictMode in Debug mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrictModeManager.init()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseDiskBackup = DatabaseDiskBackup()

        setupSupporter()
        setupActionBar()

        setupSearchBtn()
        setupOCRBtn()

        setupReminderView()

        handleIntents(intent)

       // handleDbOperations()
        handleAppUpdate()
    }

    /*private fun handleDbOperations() {
        lifecycleScope.launch(Dispatchers.IO) {
            DatabaseHelper.getInstance().cleanTempDatabaseFiles()
        }

        if (shouldUpdateDatabaseFromAsset()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.database_update_start),
                Toast.LENGTH_LONG
            ).show()

            updateDatabaseFromAsset({
                Toast.makeText(this, getString(R.string.database_update_success), Toast.LENGTH_LONG).show()

                handleBackUp()
            }, { e ->
                Toast.makeText(
                    this,
                    getString(R.string.database_update_failure, e.toString()),
                    Toast.LENGTH_LONG
                ).show()

                Utils.logAnalyticsError(TAG, "UpdateDatabaseFromAssetFailure", e.message ?: "")

                handleBackUp()
            })
        } else {
            handleBackUp()
        }
    }*/

    private fun handleAppUpdate() {
        if (appConfig.appVersionCode.value != BuildConfig.VERSION_CODE) {
            appConfig.appVersionCode.value = BuildConfig.VERSION_CODE

            // Now we can upgrade stuff
            WidgetProvider().updateAllFlashCardWidgets()
        }
    }

    fun shouldUpdateDatabaseFromAsset(): Boolean {
        if (appConfig.appVersionCode.value == 0) return false // first launch, nothing to update

        val updateDbVersions = listOf(32, 37)

        return updateDbVersions.any { updateVersion ->
            appConfig.appVersionCode.value < updateVersion && BuildConfig.VERSION_CODE >= updateVersion
        }
    }

   /* private fun updateDatabaseFromAsset(successCallback: () -> Unit, failureCallback: (e: Exception) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = DatabaseHelper.getInstance()

            try {
                val assetDbStream = createRoomDatabaseFromFile(DatabaseHelper.DATABASE_ASSET_PATH)

                dbHelper.liveDatabase.flushToDisk()
                val newDb = dbHelper.replaceWordsDataInDB(dbHelper.liveDatabase,assetDbStream)
                newDb.flushToDisk()
                newDb.close()
                dbHelper.updateDatabaseFileOnDisk(newDb)

                withContext(Dispatchers.Main) {
                    successCallback()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    failureCallback(e)
                }
            } finally {
                dbHelper.cleanTempDatabaseFiles()
            }
        }
    }*/

    private fun showOCRReminderIfActive() {
        if (!showOCRReminder) return // user asked to hide

        val nav = findNavController(R.id.nav_host_fragment_content_main)
        val ocrEntry = try {
            nav.getBackStackEntry(R.id.nav_ocr_read)
        } catch (_: IllegalArgumentException) {
            null // Not in back stack
        }
        val isOcrActive = ocrEntry != null
                && (nav.currentDestination?.id != R.id.nav_ocr_read) // don't show reminder on self fragment

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

    /*override fun onBackupFolderSet(path: Path) {
        Utils.getAppScope(applicationContext).launch(Dispatchers.IO) {
            val success = databaseDiskBackup.backUp(path)

            if (success) {
                databaseDiskBackup.cleanOldBackups(path, appConfig.dbBackUpDiskMaxFiles.value)
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

    private fun handleBackUp() {
        if (appConfig.dbBackUpDiskActive.value)
            databaseDiskBackup.getFolder()
    }*/

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun handleWidgetConfigIntent(intent: Intent?) {
        intent?.let {
            if (it.action == ACTION_APPWIDGET_CONFIGURE) {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val action = MobileNavigationDirections.configureWidget(widgetId)
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
            intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { imageUri ->
                // Handle the image URI here
                val action = MobileNavigationDirections.ocrImage(imageUri.toString(), "")
                navController.navigate(action)
            }
        }
    }

    private fun setupSupporter() {
        supportDevStore = SupportDevStore.getInstance(applicationContext)

        updateSupportMenuTitle(appConfig.supportTotalSpent.value)

        supportDevStore.addListener(object : SupportDevStore.SupportDevListener {
            override fun onTotalSpentChange(totalSpent: Float) {
                updateSupportMenuTitle(totalSpent)
            }

            override fun onQueryFailure(result: BillingResult) {
                // TODO remove runBLocking
                val txt = runBlocking { getString(Res.string.support_total_error) }
                Toast.makeText(applicationContext, txt, Toast.LENGTH_LONG).show()
            }

            override fun onPurchaseSuccess(purchase: Purchase) { }

            override fun onPurchaseHistoryUpdate(purchases: Map<SupportDevStore.SupportProduct, Int>) { }

            override fun onPurchaseAcknowledgedSuccess(purchase: Purchase) { }

            override fun onPurchaseFailure(purchase: Purchase?, billingResponseCode: Int) { }
        })

        supportDevStore.connect()
    }

    private fun updateSupportMenuTitle(totalSpent: Float) {
        val tier = supportDevStore.getSupportTier(totalSpent)

        var tpl = getString(R.string.menu_support)
        if (totalSpent > 0) {
            // TODO remove runBLocking
            tpl = runBlocking { getString(Res.string.support_status_tpl) }
        }

        val supportMenu = binding.navView.menu.findItem(R.id.nav_support)

        binding.navView.post {
            // TODO properly pull string
            supportMenu.title = tpl.format(tier.toString())
        }
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
                    val action = MobileNavigationDirections.search()
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
        binding.navView.setNavigationItemSelectedListener { item ->
             when (item.itemId) {
                 R.id.nav_dictionary -> {
                     navController.navigate(R.id.nav_dictionary)
                 }
                 R.id.nav_lists -> {
                     navController.navigate(R.id.nav_lists)
                 }
                 R.id.nav_widgets -> {
                     navController.navigate(R.id.nav_widgets)
                 }
                 R.id.nav_config -> {
                     navController.navigate(R.id.nav_config)
                 }
                 R.id.nav_about -> {
                     navController.navigate(R.id.nav_about)
                 }
                 R.id.nav_support -> {
                     navController.navigate(R.id.nav_support)
                 }
             }

             // Close the drawer
             binding.drawerLayout.closeDrawer(GravityCompat.START)
             true
         }
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