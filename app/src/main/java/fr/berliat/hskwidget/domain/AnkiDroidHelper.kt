package fr.berliat.hskwidget.domain

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION
import fr.berliat.hskwidget.data.store.AnkiIntegrationStore


class AnkiDroidHelper(val fragment: Fragment) {
    private val context: Context = fragment.requireContext()
    private val ankiStore: AnkiIntegrationStore = AnkiIntegrationStore(context)
    private lateinit var mPermissionCall : ActivityResultLauncher<Array<String>>

    val store: AnkiIntegrationStore
        get() = ankiStore

    /**
     * Whether or not the API is available to use.
     * The API could be unavailable if AnkiDroid is not installed or the user explicitly disabled the API
     * @return true if the API is available to use
     */
    fun isApiAvailable(): Boolean {
        return AddContentApi.getAnkiDroidPackageName(context) != null
    }

    /**
     * Whether or not we should request full access to the AnkiDroid API
     */
    fun shouldRequestPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            READ_WRITE_PERMISSION
        ) != PackageManager.PERMISSION_GRANTED
    }

    /**
     * Initialize the Request permission from the user to access the AnkiDroid API (for SDK 23+)
     */
    fun initRequestPermission(callback: (Boolean) -> Unit) {
        mPermissionCall = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { result ->
                callback(result[READ_WRITE_PERMISSION] ?: false)
        }
    }

    fun requestPermission() {
        mPermissionCall.launch(arrayOf(READ_WRITE_PERMISSION))
    }
}
