package fr.berliat.hskwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.databinding.ActivityMainBinding
import fr.berliat.hskwidget.domain.DatabaseBackup
import fr.berliat.hskwidget.domain.DatabaseBackupFolderUriCallbacks
import fr.berliat.hskwidget.domain.SharedViewModel
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.OCR.CaptureImageFragmentDirections
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragment
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), DatabaseBackupFolderUriCallbacks {
    companion object {
        const val INTENT_SEARCH_WORD: String = "search_word"
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseBackup: DatabaseBackup


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSharedViewModel()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseBackup = DatabaseBackup(this, this, this)

        appConfig = AppPreferencesStore(applicationContext)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dictionary, R.id.nav_widgets, R.id.nav_about
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupSearchBtn()
        setupOCRBtn()

        handleSearchIntent(intent)
        handleTextSearchIntent(intent)
        handleImageOCRIntent(intent)
        handleBackUp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleSearchIntent(intent)
        handleTextSearchIntent(intent)
        handleImageOCRIntent(intent)
    }

    override fun onUriPermissionGranted(uri: Uri) {
        GlobalScope.launch {
            val success = databaseBackup.backUp(uri)

            withContext(Dispatchers.Main) {
                if (success)
                    Toast.makeText(applicationContext, getString(R.string.dbbackup_success), Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(applicationContext, getString(R.string.dbbackup_failure_write), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onUriPermissionDenied() {
        Toast.makeText(applicationContext, getString(R.string.dbbackup_failure_folderpermission), Toast.LENGTH_LONG).show()
    }

    private fun handleBackUp() {
        if (appConfig.dbBackUpActive)
            databaseBackup.getFolder()
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}