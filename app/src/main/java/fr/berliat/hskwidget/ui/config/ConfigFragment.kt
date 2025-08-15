package fr.berliat.hskwidget.ui.config

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.databinding.FragmentConfigBinding
import fr.berliat.hskwidget.domain.DatabaseBackup
import fr.berliat.hskwidget.domain.DatabaseBackupCallbacks
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.utils.AnkiDelegate
import fr.berliat.hskwidget.ui.utils.AnkiSyncServiceDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


class ConfigFragment : Fragment(), DatabaseBackupCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback, AnkiDelegate.HandlerInterface {
    private lateinit var binding: FragmentConfigBinding
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseBackup : DatabaseBackup
    private lateinit var ankiDelegate: AnkiDelegate
    private var ankiSyncServiceDelegate: AnkiSyncServiceDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseBackup = DatabaseBackup(this, requireContext(), this)

        ankiDelegate = AnkiDelegate(this)
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("Config")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfigBinding.inflate(inflater, container, false)
        appConfig = AppPreferencesStore(requireContext())

        binding.configBackupActivateBtn.isChecked = appConfig.dbBackUpActive

        // Max number of local files
        val values = listOf(2, 5, 10)
        val labels = resources.getStringArray(R.array.config_backup_max_locally)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)

        binding.configBackupMaxLocal.setAdapter(adapter)
        binding.configBackupMaxLocal.setOnItemClickListener  {
                _, _, position, _ ->
            appConfig.dbBackUpMaxLocalFiles = values[position]
        }

        val index = values.indexOf(appConfig.dbBackUpMaxLocalFiles)
        if (index != -1) {
            binding.configBackupMaxLocal.setText(labels[index], false)
        }
        // End: Max number of local files

        binding.configBackupActivateBtn.setOnClickListener {
            appConfig.dbBackUpActive = binding.configBackupActivateBtn.isChecked

            var evt = Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_OFF
            if (appConfig.dbBackUpActive)
                evt = Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_ON
            Utils.logAnalyticsEvent(evt)
        }

        val bkDir = appConfig.dbBackUpDirectory.toString().substringAfterLast("%3A")
        if (bkDir != "")
            binding.configBtnBackupChangedir.text = URLDecoder.decode(bkDir, StandardCharsets.UTF_8.name())

        binding.configBtnBackupChangedir.setOnClickListener { databaseBackup.selectFolder() }
        binding.configBtnRestoreChoosefile.setOnClickListener { databaseBackup.selectBackupFile() }

        binding.configAnkiActivateBtn.isChecked = appConfig.ankiSaveNotes
        binding.configAnkiActivateBtn.setOnClickListener { onAnkiIntegrationChanged(binding.configAnkiActivateBtn.isChecked) }

        // Setup progress UI
        binding.ankiSyncCancelBtn.setOnClickListener {
            cancelServiceSync()
        }

        return binding.root
    }

    private fun onAnkiIntegrationChanged(enabled: Boolean) {
        appConfig.ankiSaveNotes = enabled
        binding.configAnkiActivateBtn.isChecked = enabled
        binding.ankiSyncProgressSection.visibility = Utils.hideViewIf(!enabled)
        if (enabled) {
            Utils.requestPermissionNotification(requireActivity())
            importsAllNotesToAnkiDroid()
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_ANKI_SYNC_ON)
        } else {
            cancelServiceSync()
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_ANKI_SYNC_OFF)
        }
    }

    private fun cancelServiceSync() {
        ankiSyncServiceDelegate?.cancelCurrentOperation()
    }

    override fun onAnkiRequestPermissionDenied() {
        appConfig.ankiSaveNotes = false
        binding.configAnkiActivateBtn.isChecked = false
    }

    override fun onAnkiServiceStarting(serviceDelegate: AnkiSyncServiceDelegate) {
        ankiSyncServiceDelegate = serviceDelegate
    }

    override fun onAnkiOperationSuccess() {
        Log.i(TAG, "onAnkiOperationSuccess: completed full import into Anki")
        binding.ankiSyncProgressSection.visibility = View.GONE
    }

    override fun onAnkiOperationCancelled() {
        Log.i(TAG, "onAnkiOperationCancelled: full Anki import cancelled by user")
        binding.ankiSyncProgressSection.visibility = View.GONE
    }

    override fun onAnkiOperationFailed(e: Throwable) {
        Log.i(TAG, "onAnkiOperationFailed: failed full import into Anki")

        binding.ankiSyncProgressSection.visibility = View.GONE
        var message = requireContext().getString(R.string.anki_failed_import)
        message = message.format(e.message)

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        Utils.logAnalyticsError(
            "ANKI_SYNC",
            "FullAnkiImportFailed",
            e.message ?: ""
        )
    }

    override fun onAnkiSyncProgress(current: Int, total: Int, message: String) {
        Log.i(TAG, "onAnkiImportProgress: ${current} / ${total}")

        binding.ankiSyncProgressSection.visibility = View.VISIBLE
        binding.ankiSyncProgressMessage.text = message
        if (total > 0) {
            binding.ankiSyncProgressBar.max = total
            binding.ankiSyncProgressBar.progress = current
            binding.ankiSyncProgressBar.visibility = Utils.hideViewIf(current == total)
            binding.ankiSyncCancelBtn.visibility = Utils.hideViewIf(current == total)
        } else {
            binding.ankiSyncProgressBar.isIndeterminate = true
        }
    }

    override fun onAnkiRequestPermissionGranted() {
        appConfig.ankiSaveNotes = true
        binding.configAnkiActivateBtn.isChecked = true
    }

    private fun importsAllNotesToAnkiDroid() {
        Log.i(TAG, "importsAllNotesToAnkiDroid: starting full import into Anki")
        Toast.makeText(requireContext(), R.string.anki_import_started, Toast.LENGTH_LONG).show()

        // Show progress UI
        binding.ankiSyncProgressSection.visibility = View.VISIBLE
        binding.ankiSyncProgressBar.progress = 0
        binding.ankiSyncProgressMessage.text = getString(R.string.anki_import_started)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            ankiDelegate.wordListRepo.delegateToAnki(ankiDelegate.wordListRepo.syncListsToAnki())
        }
    }

    override fun onBackupFolderSet(uri: Uri) {
        appConfig.dbBackUpDirectory = uri
        binding.configBtnBackupChangedir.text = uri.toString().substringAfterLast("%3A")
    }

    override fun onBackupFolderError() {
        Toast.makeText(requireContext(), getString(R.string.dbbackup_failure_folderpermission), Toast.LENGTH_LONG).show()
    }

    override fun onBackupFileSelected(uri: Uri) {
        Toast.makeText(
            requireContext(),
            getString(R.string.dbrestore_start),
            Toast.LENGTH_LONG
        ).show()
        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_RESTORE)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val file = Utils.copyUriToCacheDir(requireContext(), uri)

            try {
                databaseBackup.restoreDbFromFile(file)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dbrestore_success),
                        Toast.LENGTH_LONG
                    ).show()

                    val action = ConfigFragmentDirections.search()
                    findNavController().navigate(action)
                }
            } catch (e: IllegalStateException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dbrestore_failure_fileformat),
                        Toast.LENGTH_LONG
                    ).show()

                    Utils.logAnalyticsError(
                        "BACKUP_RESTORE",
                        getString(R.string.dbrestore_failure_fileformat),
                        e.message ?: ""
                    )
                }
            } catch (e: Error) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dbrestore_failure_import),
                        Toast.LENGTH_LONG
                    ).show()

                    Utils.logAnalyticsError(
                        "BACKUP_RESTORE",
                        getString(R.string.dbrestore_failure_import),
                        e.message ?: ""
                    )
                }
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