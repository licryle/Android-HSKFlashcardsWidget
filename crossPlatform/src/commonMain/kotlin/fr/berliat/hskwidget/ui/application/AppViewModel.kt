package fr.berliat.hskwidget.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.AppServices
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.BuildKonfig
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseDiskBackup
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.domain.SearchQuery
import fr.berliat.hskwidget.ui.navigation.Screen

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.database_update_failure
import fr.berliat.hskwidget.database_update_start
import fr.berliat.hskwidget.database_update_success
import fr.berliat.hskwidget.dbbackup_failure_folderpermission
import fr.berliat.hskwidget.dbbackup_failure_write
import fr.berliat.hskwidget.dbbackup_success

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

expect class AppViewModel: CommonAppViewModel

open class CommonAppViewModel(): ViewModel() {
    private val _navigation = MutableSharedFlow<Screen>()
    val navigation = _navigation.asSharedFlow()

    var _appConfig: AppPreferencesStore? = null
    val appConfig
        get() = _appConfig!!

    private val _isReady = MutableStateFlow<Boolean>(false)
    val isReady = _isReady.asSharedFlow()


    val isHSKAppServicesStatus = HSKAppServices.status

    init {
        HSKAppServices.init(viewModelScope)
        // Launch a coroutine that reacts to changes
        viewModelScope.launch {
            isHSKAppServicesStatus.collect { status ->
                if (status == AppServices.Status.Ready) {
                    finishInitialization()
                }
            }
        }
    }

    protected open fun finishInitialization() {
        _appConfig = HSKAppServices.appPreferences
        _isReady.value = true

        setupSupporter()
        handleDbOperations()

        if (didUpdateApp()) handleAppUpdate()
    }

    protected open fun handleAppUpdate() {
        appConfig.appVersionCode.value = BuildKonfig.VERSION_CODE
    }

    fun didUpdateApp(): Boolean {
        return appConfig.appVersionCode.value != BuildKonfig.VERSION_CODE
    }

    private fun handleDbOperations() {
        viewModelScope.launch(AppDispatchers.IO) {
            DatabaseHelper.getInstance().cleanTempDatabaseFiles()
        }

        if (shouldUpdateDatabaseFromAsset()) {
            Utils.toast(Res.string.database_update_start)

            viewModelScope.launch(AppDispatchers.IO) {
                DatabaseHelper.getInstance().updateLiveDatabaseFromAsset({
                    Utils.toast(Res.string.database_update_success)

                    handleBackupDisk()
                }, { e ->
                    Utils.toast(Res.string.database_update_failure, listOf(e.message ?: ""))

                    Utils.logAnalyticsError(TAG, "UpdateDatabaseFromAssetFailure", e.message ?: "")

                    handleBackupDisk()
                })
            }
        } else {
            handleBackupDisk()
        }
    }

    fun shouldUpdateDatabaseFromAsset(): Boolean {
        if (appConfig.appVersionCode.value == 0) return false // first launch, nothing to update

        val updateDbVersions = listOf(32, 37)

        return updateDbVersions.any { updateVersion ->
            appConfig.appVersionCode.value < updateVersion && Utils.getAppVersion() >= updateVersion
        }
    }

    private fun handleBackupDisk() {
        val bookMark = appConfig.dbBackUpDiskDirectory.value
        if (appConfig.dbBackUpDiskActive.value && bookMark != null) {
            val backupFolder = PlatformFile.fromBookmarkData(bookMark)
            viewModelScope.launch(AppDispatchers.IO) {
                DatabaseDiskBackup.getFolder(
                    bookMark,
                    onSuccess = {
                        viewModelScope.launch(AppDispatchers.IO) {
                            DatabaseDiskBackup.backUp(
                                bookMark,
                                onSuccess = {
                                    Utils.toast(Res.string.dbbackup_success)
                                    viewModelScope.launch(AppDispatchers.IO) {
                                        DatabaseDiskBackup.cleanOldBackups(
                                            backupFolder,
                                            appConfig.dbBackUpDiskMaxFiles.value
                                        )
                                    }
                                },
                                onFail = { Utils.toast(Res.string.dbbackup_failure_write) }
                            )
                        }
                    },
                    onFail = {
                        Utils.toast(Res.string.dbbackup_failure_folderpermission)
                    }
                )
            }
        }
    }

    fun search(s: String) {
        appConfig.searchQuery.value = SearchQuery.processSearchQuery(s)
    }

    fun configureWidget(widgetId: Int) {
        viewModelScope.launch(AppDispatchers.IO) {
            _navigation.emit(Screen.Widgets(widgetId))
        }
    }

    fun ocrImage(imageFile: PlatformFile) {
        viewModelScope.launch(AppDispatchers.IO) {
            _navigation.emit(Screen.OCRDisplay("", imageFile.path))
        }
    }

    private fun setupSupporter() {
        /*supportDevStore = SupportDevStore.getInstance(applicationContext)

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
        }*/
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}