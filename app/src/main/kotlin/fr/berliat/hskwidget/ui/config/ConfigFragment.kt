package fr.berliat.hskwidget.ui.config

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.ankidroidhelper.AnkiSyncServiceDelegate
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.OldAppPreferencesStore
import fr.berliat.hskwidget.databinding.FragmentConfigBinding
import fr.berliat.hskwidget.domain.DatabaseDiskBackup
import fr.berliat.hskwidget.domain.DatabaseBackupCallbacks
import fr.berliat.googledrivebackup.GoogleDriveBackup
import fr.berliat.googledrivebackup.GoogleDriveBackupFile
import fr.berliat.googledrivebackup.GoogleDriveBackupInterface
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.utils.HSKAnkiDelegate
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant


class ConfigFragment : Fragment(), DatabaseBackupCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback, AnkiDelegate.HandlerInterface,
        GoogleDriveBackupInterface {
    private lateinit var binding: FragmentConfigBinding
    private lateinit var appConfig: OldAppPreferencesStore
    private lateinit var databaseDiskBackup : DatabaseDiskBackup
    private lateinit var ankiDelegate: HSKAnkiDelegate
    private var ankiSyncServiceDelegate: AnkiSyncServiceDelegate? = null
    private lateinit var gDriveBackUp : GoogleDriveBackup
    private var gDriveBackupSnapshot : File? = null
    private var gDriveBackupSnapMutex = Mutex()

    val cloudRestoreFilePath
        get() = File("${requireContext().cacheDir}/restore/${DatabaseHelper.DATABASE_FILENAME}")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseDiskBackup = DatabaseDiskBackup(this, requireContext(), this)

        ankiDelegate = HSKAnkiDelegate(this, this)
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
        appConfig = OldAppPreferencesStore(requireContext())

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

        setBackUpFolderButtonText(appConfig.dbBackUpDirectory)

        binding.configBackupActivateBtn.setOnClickListener {
            if (!binding.configBackupActivateBtn.isChecked) {
                appConfig.dbBackUpActive = false
                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_OFF)
            } else {
                // Try to fetch, or collect folder
                // Ends up triggering either onBackupFolderSet or onBackupFolderError where we set
                // The active/inactive flag
                databaseDiskBackup.getFolder()
            }

        }

        binding.configBtnBackupChangedir.setOnClickListener { databaseDiskBackup.selectFolder() }
        binding.configBtnRestoreChoosefile.setOnClickListener { databaseDiskBackup.selectBackupFile() }

        binding.configAnkiActivateBtn.isChecked = appConfig.ankiSaveNotes
        binding.configAnkiActivateBtn.setOnClickListener { onAnkiIntegrationChanged(binding.configAnkiActivateBtn.isChecked) }

        // Setup progress UI
        binding.ankiSyncCancelBtn.setOnClickListener { cancelAnkiSyncService() }

        gDriveBackUp = GoogleDriveBackup(this, requireActivity(), getString(R.string.app_name))
        gDriveBackUp.transferChunkSize = 1024 * 1024 // 1 MB
        gDriveBackUp.addListener(this)

        val lastBackupCloud = appConfig.dbBackupCloudLastSuccess
        if (lastBackupCloud.toEpochMilli() != 0L)
            binding.configBackupCloudLastone.text = Utils.formatDate(appConfig.dbBackupCloudLastSuccess)

        binding.configBtnBackupCloudBackupnow.setOnClickListener { gDriveBackUp.login { backupToCloud() } }
        binding.configBtnBackupCloudRestorenow.setOnClickListener { gDriveBackUp.login { restoreFromCloud() } }

        /*updateLoggedInAccount()
        binding.configBackupCloudAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.googledrive_logout_confirm))
                .setPositiveButton(R.string.proceed) { _, _ -> gDriveBackUp.deviceAccounts.forEach { gDriveBackUp.logout(it) } }   // user confirms
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
                .setCancelable(true)                                 // allow dismiss by tapping outside
                .show()
        }*/

        return binding.root
    }

    private fun setBackUpFolderButtonText(uri: Uri) {
        val dirPath = uri.path?.substringAfterLast(":") ?: ""

        if (dirPath != "")
            binding.configBtnBackupChangedir.text = dirPath
    }

    /*private fun updateLoggedInAccount() {
        if (gDriveBackUp.deviceAccounts.isNotEmpty())
            binding.configBackupCloudAccount.text = gDriveBackUp.deviceAccounts[0].name
        else
            binding.configBackupCloudAccount.text = getString(R.string.config_backup_cloud_account_notconfigured)
    }*/

    private fun deleteBackupSnapshot() {
        lifecycleScope.launch(Dispatchers.IO) {
            gDriveBackupSnapMutex.withLock {
                if (gDriveBackupSnapshot?.exists() == true) gDriveBackupSnapshot?.delete()
                gDriveBackupSnapshot = null // technically not needed
            }
        }
    }

    private fun backupToCloud() {
        toggleBackupRestoreButtonsClickable(false)

        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = DatabaseHelper.getInstance(requireContext())

            gDriveBackupSnapMutex.withLock {
                gDriveBackupSnapshot = dbHelper.snapshotDatabase()

                gDriveBackUp.backup(
                    listOf(
                        GoogleDriveBackupFile.UploadFile(
                            "database.sqlite",
                            FileInputStream(gDriveBackupSnapshot),
                            "application/octet-stream",
                            gDriveBackupSnapshot!!.length()
                        )
                    )
                )
            }
        }

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUPCLOUD_BACKUP)
    }

    private fun restoreFromCloud() {
        toggleBackupRestoreButtonsClickable(false)
        cloudRestoreFilePath.parentFile?.mkdirs()

        gDriveBackUp.restore(
            listOf(GoogleDriveBackupFile.DownloadFile(
                "database.sqlite",
                FileOutputStream(cloudRestoreFilePath)
            ))
        )

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUPCLOUD_RESTORE)
    }

    private fun onAnkiIntegrationChanged(enabled: Boolean) {
        binding.configAnkiActivateBtn.isChecked = enabled

        if (enabled) {
            appConfig.setAnkiSaveNotes(true) {
                importsAllNotesToAnkiDroid()
            }
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_ANKI_SYNC_ON)
        } else {
            appConfig.ankiSaveNotes = false
            cancelAnkiSyncService()
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_ANKI_SYNC_OFF)
        }
    }

    private fun cancelAnkiSyncService() {
        ankiSyncServiceDelegate?.cancelCurrentOperation()
    }

    override fun onAnkiRequestPermissionGranted() {
        appConfig.ankiSaveNotes = true
        binding.configAnkiActivateBtn.isChecked = true

        Utils.requestPermissionNotification(requireActivity())
    }

    override fun onAnkiRequestPermissionDenied() {
        appConfig.ankiSaveNotes = false
        binding.configAnkiActivateBtn.isChecked = false

        binding.ankiSyncProgressSection.visibility = View.GONE
    }

    override fun onAnkiServiceStarting(serviceDelegate: AnkiSyncServiceDelegate) {
        ankiSyncServiceDelegate = serviceDelegate

        binding.ankiSyncProgressBar.progress = 0
        binding.ankiSyncProgressBar.isIndeterminate = true
        binding.ankiSyncProgressTitle.text = getString(R.string.anki_sync_start_title)
        binding.ankiSyncProgressMessage.text = getString(R.string.anki_sync_start_message)
        binding.ankiSyncProgressSection.visibility = View.VISIBLE
        binding.ankiSyncProgressBar.visibility = View.VISIBLE
        binding.ankiSyncCancelBtn.visibility = View.VISIBLE
    }

    override fun onAnkiOperationSuccess() {
        Log.i(TAG, "onAnkiOperationSuccess: completed full import into Anki")
        val now = Instant.now()
        val nowString = Utils.formatDate(now)

        binding.ankiSyncProgressTitle.text = getString(R.string.anki_sync_success_title)
        binding.ankiSyncProgressMessage.text = getString(R.string.anki_sync_success_message, nowString)
        binding.ankiSyncProgressBar.visibility = View.GONE
        binding.ankiSyncCancelBtn.visibility = View.GONE
    }

    override fun onAnkiOperationCancelled() {
        Log.i(TAG, "onAnkiOperationCancelled: full Anki import cancelled by user")

        binding.ankiSyncProgressTitle.text = getString(R.string.anki_sync_cancelled_title)
        binding.ankiSyncProgressMessage.text = getString(R.string.anki_sync_cancelled_message)
        binding.ankiSyncProgressBar.visibility = View.GONE
        binding.ankiSyncCancelBtn.visibility = View.GONE
    }

    override fun onAnkiOperationFailed(e: Throwable) {
        Log.i(TAG, "onAnkiOperationFailed: failed full import into Anki")

        binding.ankiSyncProgressTitle.text = getString(R.string.anki_sync_failure_title)
        binding.ankiSyncProgressMessage.text = getString(R.string.anki_sync_failure_message, e.message ?: "")
        binding.ankiSyncProgressBar.visibility = View.GONE
        binding.ankiSyncCancelBtn.visibility = View.GONE

        Utils.logAnalyticsError(
            "ANKI_SYNC",
            "FullAnkiImportFailed",
            e.message ?: ""
        )
    }

    override fun onAnkiSyncProgress(current: Int, total: Int, message: String) {
        Log.i(TAG, "onAnkiImportProgress: $current / $total")

        binding.ankiSyncProgressSection.visibility = View.VISIBLE
        binding.ankiSyncCancelBtn.visibility = View.VISIBLE
        binding.ankiSyncProgressMessage.text = message
        if (total > 0) {
            binding.ankiSyncProgressBar.max = total
            binding.ankiSyncProgressBar.progress = current
            binding.ankiSyncProgressBar.visibility = Utils.hideViewIf(current == total)
            binding.ankiSyncCancelBtn.visibility = Utils.hideViewIf(current == total)
            binding.ankiSyncProgressBar.isIndeterminate = false
        } else {
            binding.ankiSyncProgressBar.isIndeterminate = true
        }
    }

    private fun importsAllNotesToAnkiDroid() {
        Log.i(TAG, "importsAllNotesToAnkiDroid: starting full import into Anki")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            ankiDelegate.delegateToAnki(HSKAppServices.wordListRepo.syncListsToAnki())
        }
    }

    override fun onBackupFolderSet(uri: Uri) {
        appConfig.dbBackUpActive = true
        binding.configBackupActivateBtn.isChecked = true
        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_ON)

        appConfig.dbBackUpDirectory = uri
        setBackUpFolderButtonText(uri)
    }

    override fun onBackupFolderError() {
        appConfig.dbBackUpActive = false
        binding.configBackupActivateBtn.isChecked = false
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
            val dbHelper = DatabaseHelper.getInstance(requireContext())

            try {
                val file = Utils.copyUriToCacheDir(requireContext(), uri)
                val sourceDb = dbHelper.loadExternalDatabase(file)
                dbHelper.replaceUserDataInDB(dbHelper.liveDatabase, sourceDb)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dbrestore_success),
                        Toast.LENGTH_LONG
                    ).show()

                    val action = ConfigFragmentDirections.search()
                    findNavController().navigate(action)
                }

                // Backup was successful, let's trigger widget updates, hoping any matches
                FlashcardWidgetProvider().updateAllFlashCardWidgets(requireContext())

                file.delete()
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
                        getString(R.string.dbrestore_failure_import).format(e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()

                    Utils.logAnalyticsError(
                        "BACKUP_RESTORE",
                        getString(R.string.dbrestore_failure_import),
                        e.message ?: ""
                    )
                }
            } finally {
                dbHelper.cleanTempDatabaseFiles()
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

    override fun onLogout() {
        Log.i(TAG, "GoogleDriveBackup.onLogout")

        //updateLoggedInAccount()
    }

    override fun onReady() {
        Log.i(TAG, "GoogleDriveBackup.onReady")

        //updateLoggedInAccount()
    }

    override fun onNoGoogleAPI(e: Exception) {
        Log.i(TAG, "GoogleDriveBackup.onNoGoogleAPI")
        Toast.makeText(
            requireContext(),
            R.string.googledrive_backup_nogoogleapi,
            Toast.LENGTH_LONG).show()
    }

    override fun onNoAccountSelected() {
        Log.i(TAG, "GoogleDriveBackup.onNoAccountSelected")
        Toast.makeText(
            requireContext(),
            R.string.googledrive_backup_noaccountselected,
            Toast.LENGTH_LONG).show()
    }

    override fun onScopeDenied(e: Exception) {
        Log.i(TAG, "GoogleDriveBackup.onScopeDenied")
        Toast.makeText(
            requireContext(),
            R.string.googledrive_backup_denied_scope,
            Toast.LENGTH_LONG).show()
    }

    override fun onBackupStarted() {
        Log.i(TAG, "GoogleDriveBackup.onBackupStarted")
        toggleGoogleDriveBackupInProgress(true)

        binding.googledriveSyncCancelBtn.setOnClickListener { gDriveBackUp.cancelBackup() }
        binding.googledriveSyncProgressSection.visibility = View.VISIBLE
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_backup_start_title)
        binding.googledriveSyncProgressBar.isIndeterminate = true
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_backup_start_message)
    }

    override fun onBackupProgress(fileName: String, fileIndex: Int, fileCount: Int, bytesSent: Long, bytesTotal: Long) {
        val sentMB = Utils.formatKBToMB(bytesSent)
        val totalMB = Utils.formatKBToMB(bytesTotal)
        Log.i(TAG, "GoogleDriveBackup.onBackupProgress: $sentMB / $totalMB")

        toggleGoogleDriveBackupInProgress(bytesTotal != bytesSent)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_backup_progress_message, sentMB, totalMB)

        binding.googledriveSyncProgressBar.isIndeterminate = false
        binding.googledriveSyncProgressBar.max = bytesTotal.toInt()
        binding.googledriveSyncProgressBar.progress = bytesSent.toInt()
    }

    override fun onBackupSuccess() {
        Log.i(TAG, "GoogleDriveBackup.onBackupSuccess")
        val now = Instant.now()
        val nowString = Utils.formatDate(now)

        toggleGoogleDriveBackupInProgress(false)
        appConfig.dbBackupCloudLastSuccess = now
        binding.configBackupCloudLastone.text = nowString
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_backup_success_message, nowString)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_backup_success_title)

        toggleBackupRestoreButtonsClickable(true)

        deleteBackupSnapshot()
    }

    override fun onBackupCancelled() {
        Log.i(TAG, "GoogleDriveBackup.onBackupCancelled")
        toggleGoogleDriveBackupInProgress(false)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_backup_cancel_title)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_backup_cancel_message)
        toggleBackupRestoreButtonsClickable(true)

        deleteBackupSnapshot()
    }

    override fun onBackupFailed(e: Exception) {
        Log.i(TAG, "GoogleDriveBackup.onBackupFailed")
        toggleGoogleDriveBackupInProgress(false)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_backup_failed_title)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_backup_failed_message, e.message)
        toggleBackupRestoreButtonsClickable(true)
        Utils.logAnalyticsError(TAG,"CloudBackUpFailed", e.message?: "")

        deleteBackupSnapshot()
    }

    override fun onRestoreEmpty() {
        Log.i(TAG, "GoogleDriveBackup.onRestoreEmpty")
        toggleGoogleDriveBackupInProgress(false)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_restore_failed_title)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_restore_empty_message)

        toggleBackupRestoreButtonsClickable(true)
        Utils.logAnalyticsError(TAG,"CloudRestoreEmpty", "")
    }

    override fun onRestoreStarted() {
        Log.i(TAG, "GoogleDriveBackup.onRestoreStarted")
        toggleGoogleDriveBackupInProgress(true)

        binding.googledriveSyncCancelBtn.setOnClickListener { gDriveBackUp.cancelRestore() }
        binding.googledriveSyncProgressSection.visibility = View.VISIBLE
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_restore_start_title)
        binding.googledriveSyncProgressBar.isIndeterminate = true
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_restore_start_message)
    }

    override fun onRestoreProgress(
        fileName: String,
        fileIndex: Int,
        fileCount: Int,
        bytesReceived: Long,
        bytesTotal: Long
    ) {
        val receivedMB = Utils.formatKBToMB(bytesReceived)
        val totalMB = Utils.formatKBToMB(bytesTotal)
        Log.i(TAG, "GoogleDriveBackup.onBackupProgress: $receivedMB / $totalMB")

        toggleGoogleDriveBackupInProgress(bytesTotal != bytesReceived)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_backup_progress_message, receivedMB, totalMB)

        binding.googledriveSyncProgressBar.isIndeterminate = false
        binding.googledriveSyncProgressBar.max = bytesTotal.toInt()
        binding.googledriveSyncProgressBar.progress = bytesReceived.toInt()
    }

    override fun onRestoreSuccess(files: List<GoogleDriveBackupFile.DownloadFile>) {
        Log.i(TAG, "GoogleDriveBackup.onRestoreSuccess")
        if (files.isEmpty() || files[0].name != "database.sqlite") {
            throw Exception("ConfigFragement.onRestoreSuccess: Something went really wrong in GoogleDriveBackUp lib, wrong backup file")
        }
        val now = Instant.now()
        val nowString = Utils.formatDate(now)

        toggleGoogleDriveBackupInProgress(false)

        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_restore_success_message, nowString)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_restore_success_title)

        toggleBackupRestoreButtonsClickable(true)
        binding.googledriveSyncProgressSection.visibility = View.GONE

        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.googledrive_restore_confirm_overwrite, Utils.formatDate(files[0].modifiedTime ?: Instant.ofEpochMilli(0))))
            .setPositiveButton(R.string.proceed) { _, _ -> onBackupFileSelected(cloudRestoreFilePath.toUri()) }   // user confirms
            .setNegativeButton(R.string.cancel) { _, _ -> onRestoreCancelled() }
            .setCancelable(true)                                 // allow dismiss by tapping outside
            .show()
    }

    fun toggleBackupRestoreButtonsClickable(isClickable: Boolean) {
        binding.configBtnBackupCloudRestorenow.isEnabled = isClickable
        binding.configBtnBackupCloudBackupnow.isEnabled = isClickable
        binding.configBtnRestoreChoosefile.isEnabled = isClickable
    }

    override fun onRestoreCancelled() {
        Log.i(TAG, "GoogleDriveBackup.onBackupCancelled")
        toggleGoogleDriveBackupInProgress(false)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_restore_cancel_title)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_restore_cancel_message)
        toggleBackupRestoreButtonsClickable(true)
    }

    override fun onRestoreFailed(e: Exception) {
        Log.i(TAG, "GoogleDriveBackup.onRestoreFailed")
        toggleGoogleDriveBackupInProgress(false)
        binding.googledriveSyncProgressTitle.text =
            getString(R.string.googledrive_restore_failed_title)
        binding.googledriveSyncProgressMessage.text =
            getString(R.string.googledrive_restore_failed_message, e.message)

        toggleBackupRestoreButtonsClickable(true)
        Utils.logAnalyticsError(TAG, "CloudRestoreFailed", e.message ?: e.toString())
    }

    override fun onDeletePreviousBackupFailed(e: Exception) {
        Log.i(TAG, "Failed to delete previous Google Drive backups")
        Utils.logAnalyticsError(TAG, "GoogleDriveFailureDeletePreviousBackup", e.message ?: e.toString())
    }

    override fun onDeletePreviousBackupSuccess() {
        Log.i(TAG, "Successfully deleted previous Google Drive backups")
    }

    private fun toggleGoogleDriveBackupInProgress(inProgress : Boolean) {
        binding.googledriveSyncProgressBar.visibility = Utils.hideViewIf(!inProgress)
        binding.googledriveSyncCancelBtn.visibility = Utils.hideViewIf(!inProgress)
    }

    companion object {
        const val TAG = "ConfigFragment"
    }
}