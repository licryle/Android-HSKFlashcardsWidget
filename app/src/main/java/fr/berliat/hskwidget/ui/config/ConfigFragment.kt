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
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentConfigBinding
import fr.berliat.hskwidget.domain.DatabaseBackup
import fr.berliat.hskwidget.domain.DatabaseBackupCallbacks
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.utils.AnkiDelegate
import kotlinx.coroutines.launch


class ConfigFragment : Fragment(), DatabaseBackupCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback, AnkiDelegate.HandlerInterface {
    private lateinit var binding: FragmentConfigBinding
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseBackup : DatabaseBackup
    private lateinit var annotationDAO: ChineseWordAnnotationDAO
    private lateinit var appContext: Context
    private lateinit var ankiDelegate: AnkiDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseBackup = DatabaseBackup(this, requireContext(), this)
        annotationDAO = ChineseWordsDatabase.getInstance(requireContext()).chineseWordAnnotationDAO()
        appContext = requireContext().applicationContext

        ankiDelegate = AnkiDelegate(this)
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
        appConfig.ankiSaveNotes = enabled
        binding.configAnkiActivateBtn.isChecked = enabled
        if (enabled) {
            importsAllNotesToAnkiDroid()
        }
    }

    override fun onAnkiRequestPermissionDenied() {
        appConfig.ankiSaveNotes = false
        binding.configAnkiActivateBtn.isChecked = false
    }

    override fun onAnkiOperationSuccess() {
        Log.i(TAG, "importsAllNotesToAnkiDroid: completed full import into Anki")
        Toast.makeText(requireContext(), R.string.anki_import_completed, Toast.LENGTH_LONG).show()
    }

    override fun onAnkiOperationFailed(e: Throwable) {
        Log.i(TAG, "importsAllNotesToAnkiDroid: failed full import into Anki")

        var message = requireContext().getString(R.string.anki_failed_import)
        message = message.format(e.message)

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onAnkiRequestPermissionGranted() {
        appConfig.ankiSaveNotes = true
        binding.configAnkiActivateBtn.isChecked = true
    }

    private fun importsAllNotesToAnkiDroid() {
        Log.i(TAG, "importsAllNotesToAnkiDroid: starting full import into Anki")
        Toast.makeText(requireContext(), R.string.anki_import_started, Toast.LENGTH_LONG).show()

        viewLifecycleOwner.lifecycleScope.launch {
            ankiDelegate.wordListRepo.delegateToAnki(ankiDelegate.wordListRepo.syncListsToAnki())
        }
    }

    /*private suspend fun onAnkiImportProgress(current: Int, total: Int) {
        if (current % 10 != 0) return
        Log.i(TAG, "onAnkiImportProgress: ${current} / ${total}")

        withContext(Dispatchers.Main) {
            val message = String.format(appContext.getString(R.string.anki_import_progress), current, total)
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onAnkiInsertCard(word: AnnotatedChineseWord, ankiId: Long) {
        Log.i(TAG, "onAnkiInsertCard start for ${word} with ${ankiId}")
    }*/

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

    companion object {
        const val TAG = "ConfigFragment"
    }
}