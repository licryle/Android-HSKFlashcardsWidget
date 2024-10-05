package fr.berliat.hskwidget

import android.os.Bundle
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
import fr.berliat.hskwidget.databinding.ActivityMainBinding
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragment
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections

class MainActivity : AppCompatActivity() {
    companion object {
        const val INTENT_SEARCH_WORD: String = "search_word"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        intent?.let {
            val searchWord = it.getStringExtra(INTENT_SEARCH_WORD)
            if (searchWord != null) {
                binding.appBarMain.appbarSearch.setQuery(searchWord, true)
            }
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

                if (currentFragment is DictionarySearchFragment) {
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