package fr.berliat.hskwidget.ui.config

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentConfigBinding
import fr.berliat.hskwidget.domain.AnkiDroidHelper
import fr.berliat.hskwidget.domain.DatabaseBackup
import fr.berliat.hskwidget.domain.DatabaseBackupCallbacks
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ConfigFragment : Fragment(), DatabaseBackupCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback{
    private lateinit var binding: FragmentConfigBinding
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseBackup : DatabaseBackup
    private lateinit var ankiDroid : AnkiDroidHelper
    private lateinit var annotationDAO: ChineseWordAnnotationDAO
    private lateinit var appContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseBackup = DatabaseBackup(this, requireContext(), this)
        ankiDroid = AnkiDroidHelper(this)
        ankiDroid.initRequestPermission { isGranted -> onAnkiRequestPermissionsResult(isGranted) }
        annotationDAO = ChineseWordsDatabase.getInstance(requireContext()).chineseWordAnnotationDAO()
        appContext = requireContext().applicationContext
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView(requireContext(), "Config")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfigBinding.inflate(inflater, container, false)
        appConfig = AppPreferencesStore(requireContext())

        binding.configBackupActivateBtn.isChecked = appConfig.dbBackUpActive
        binding.configBackupActivateBtn.setOnClickListener {
            appConfig.dbBackUpActive = binding.configBackupActivateBtn.isChecked
        }

        val bkDir = appConfig.dbBackUpDirectory.toString().substringAfterLast("%3A")
        if (bkDir != "")
            binding.configBtnBackupChangedir.text = bkDir

        binding.configBtnBackupChangedir.setOnClickListener { databaseBackup.selectFolder() }
        binding.configBtnRestoreChoosefile.setOnClickListener { databaseBackup.selectBackupFile() }

        binding.configAnkiActivateBtn.isChecked = appConfig.ankiSaveNotes
        binding.configAnkiActivateBtn.setOnClickListener { onAnkiIntegrationChanged(binding.configAnkiActivateBtn.isChecked) }

        return binding.root
    }

    private fun onAnkiIntegrationChanged(enabled: Boolean) {
        appConfig = AppPreferencesStore(requireContext())

        if (enabled) {
            if (ankiDroid.isApiAvailable()) {
                if (ankiDroid.shouldRequestPermission()) {
                    ankiDroid.requestPermission()
                    return
                }

                onAnkiRequestPermissionsResult(true)
            } else {
                Toast.makeText(requireContext(), getString(R.string.anki_not_installed), Toast.LENGTH_LONG).show()
                binding.configAnkiActivateBtn.isChecked = false
                appConfig.ankiSaveNotes = false
                return
            }
        } else {
            appConfig.ankiSaveNotes = false
        }
    }

    private fun importsAllNotesToAnkiDroid() {
        Log.i(TAG, "importsAllNotesToAnkiDroid: starting full import into Anki")
        Toast.makeText(requireContext(), R.string.anki_import_started, Toast.LENGTH_LONG).show()

        GlobalScope.launch {
            try {
                val result = ankiDroid.store.importOrUpdateAllCards(true,
                    ::onAnkiImportProgress, ::onAnkiInsertCard)

                withContext(Dispatchers.Main) {
                    val message = String.format(
                        appContext.getString(R.string.anki_import_completed),
                        result[1]
                    )
                    Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Error) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        appContext.getString(R.string.anki_failed_import) + e.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun onAnkiImportProgress(current: Int, total: Int) {
        if (current % 10 != 0) return
        Log.i(TAG, "onAnkiImportProgress: ${current} / ${total}")

        withContext(Dispatchers.Main) {
            val message = String.format(appContext.getString(R.string.anki_import_progress), current, total)
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun onAnkiInsertCard(word: AnnotatedChineseWord, ankiId: Long) {
        Log.i(TAG, "onAnkiInsertCard start for ${word} with ${ankiId}")

        annotationDAO.updateAnkiNoteId(word.simplified, ankiId)
    }

    override fun onBackupFolderSet(uri: Uri) {
        appConfig.dbBackUpDirectory = uri
        binding.configBtnBackupChangedir.text = uri.toString().substringAfterLast("%3A")
    }

    override fun onBackupFolderError() {
        Toast.makeText(requireContext(), getString(R.string.dbbackup_failure_folderpermission), Toast.LENGTH_LONG).show()
    }

    override fun onBackupFileSelected(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val file = Utils.copyUriToCacheDir(requireContext(), uri)

            try {
                databaseBackup.restoreDbFromFile(file)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.dbrestore_success),
                    Toast.LENGTH_LONG
                ).show()

                val action = ConfigFragmentDirections.search()
                findNavController().navigate(action)
            } catch (e: IllegalStateException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.dbrestore_failure_fileformat),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Error) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.dbrestore_failure_import),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onBackupFileSelectionCancelled() {
        Log.d(TAG, "Backup file selection cancelled")
        Toast.makeText(
            requireContext(),
            getString(R.string.dbrestore_failure_nofileselected),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun onAnkiRequestPermissionsResult(isGranted: Boolean) {
        Log.i(TAG, "Requesting Anki permission to read/write")
        AppPreferencesStore(requireContext()).ankiSaveNotes = isGranted
        binding.configAnkiActivateBtn.isChecked = isGranted

        if (isGranted) {
            importsAllNotesToAnkiDroid()
        } else {
            Toast.makeText(requireContext(), R.string.anki_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "ConfigFragment"
    }
}