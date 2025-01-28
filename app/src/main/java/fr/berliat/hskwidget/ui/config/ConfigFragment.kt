package fr.berliat.hskwidget.ui.config

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.databinding.FragmentConfigBinding
import fr.berliat.hskwidget.domain.DatabaseBackup
import fr.berliat.hskwidget.domain.DatabaseBackupFolderUriCallbacks
import fr.berliat.hskwidget.domain.Utils

class ConfigFragment : Fragment(), DatabaseBackupFolderUriCallbacks {
    private lateinit var binding: FragmentConfigBinding
    private lateinit var appConfig: AppPreferencesStore
    private lateinit var databaseBackup : DatabaseBackup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseBackup = DatabaseBackup(this, requireContext(), this)
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

        binding.configWriteassistGroqkey.setText(appConfig.groqAPIKey)

        binding.configWriteassistGroqkey.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                binding.configWriteassistGroqkey.clearFocus()
                true  // Indicate that the action was handled
            } else {
                false
            }
        }

        binding.configWriteassistGroqkey.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                appConfig.groqAPIKey = binding.configWriteassistGroqkey.text.toString()

            Toast.makeText(requireContext(), getString(R.string.config_groqkey_saved), Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onUriPermissionGranted(uri: Uri) {
        appConfig.dbBackUpDirectory = uri
        binding.configBtnBackupChangedir.text = uri.toString().substringAfterLast("%3A")
    }

    override fun onUriPermissionDenied() {
        Toast.makeText(requireContext(), getString(R.string.dbbackup_failure_folderpermission), Toast.LENGTH_LONG).show()
    }
}