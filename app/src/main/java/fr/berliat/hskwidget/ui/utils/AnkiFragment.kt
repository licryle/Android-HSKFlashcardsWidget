package fr.berliat.hskwidget.ui.utils

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.AnkiDroidHelper

open class AnkiFragment: Fragment() {
    protected lateinit var ankiDroid: AnkiDroidHelper
    private lateinit var appConfig: AppPreferencesStore

    // A stack is an overkill, but just to be safe
    private var callQueue: ArrayDeque<() -> Unit> = ArrayDeque()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appConfig = AppPreferencesStore(requireContext())

        ankiDroid = AnkiDroidHelper(this)
        ankiDroid.initRequestPermission { isGranted -> onAnkiRequestPermissionsResult(isGranted) }
    }

    private fun onAnkiRequestPermissionsResult(isGranted: Boolean) {
        Log.i(TAG, "AnkiPermissions to read/write is granted? $isGranted")
        appConfig.ankiSaveNotes = isGranted

        if (isGranted) {
            onAnkiRequestPermissionGranted()
            while (callQueue.isNotEmpty()) {
                val action = callQueue.removeFirst() // FIFO
                safelyModifyAnkiDb { action() }
            }
        } else {
            onAnkiRequestPermissionDenied()
            Toast.makeText(requireContext(), R.string.anki_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    protected open fun onAnkiRequestPermissionGranted() { }

    protected open fun onAnkiRequestPermissionDenied() { }

    private fun safelyModifyAnkiDb(ankiDbAction: () -> Unit) {
        ankiDroid.ensureAnkiDroidIsRunning()
        ankiDbAction()
    }

    protected fun safelyModifyAnkiDbIfAllowed(ankiDbAction: () -> Unit) {
        if (appConfig.ankiSaveNotes) {
            if (ankiDroid.isApiAvailable()) {
                if (ankiDroid.shouldRequestPermission()) {
                    callQueue.add(ankiDbAction)
                    ankiDroid.requestPermission()
                    return
                }

                safelyModifyAnkiDb { ankiDbAction() }
            } else {
                Toast.makeText(requireContext(), getString(R.string.anki_not_installed), Toast.LENGTH_LONG).show()
                // App was uninstalled, turning it off
                appConfig.ankiSaveNotes = false
                return
            }
        } else {
            // Do nothing, because it's configured as OFF
        }
    }

    companion object {
        const val TAG = "AnkiFragment"
    }
}