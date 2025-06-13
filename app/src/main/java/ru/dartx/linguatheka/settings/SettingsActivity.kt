package ru.dartx.linguatheka.settings

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkManager
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.SettingsActivityBinding
import ru.dartx.linguatheka.utils.AuthorizationClientManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager.isGrantedAllScopes
import ru.dartx.linguatheka.utils.LanguagesManager
import ru.dartx.linguatheka.utils.ThemeManager
import ru.dartx.linguatheka.utils.TimeManager
import ru.dartx.linguatheka.workers.BackupWorker

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            backupSummary()
            LanguagesManager.getUsedLanguages(this@SettingsFragment.requireActivity().application)
            val defLangPref: ListPreference = findPreference("def_lang")!!
            val nativeLangPref: ListPreference = findPreference("native_lang")!!
            val langArray = LanguagesManager.getLanguages()
            defLangPref.entryValues = langArray[0]
            nativeLangPref.entryValues = langArray[0]
            defLangPref.entries = langArray[1]
            nativeLangPref.entries = langArray[1]
            val nightMode: Preference? = findPreference("night_mode")
            val themeMode: Preference? = findPreference("theme")
            val autoBackup: CheckBoxPreference? = findPreference("auto_backup")
            val restoreMenu: Preference? = findPreference("restore_menu")
            if (!BackupAndRestoreManager.checkForGooglePlayServices(requireContext())) restoreMenu?.isVisible =
                false
            val authorizationLauncher = registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) {
                if (it.resultCode == RESULT_OK) {
                    val authorizationResult = Identity.getAuthorizationClient(requireActivity())
                        .getAuthorizationResultFromIntent(it.data)
                    if (isGrantedAllScopes(authorizationResult)) {
                        autoBackup?.isChecked = true
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.grant_access),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.grant_access),
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
            nightMode?.setOnPreferenceChangeListener { _, value ->
                AppCompatDelegate.setDefaultNightMode(
                    when (value as String) {
                        "day" -> AppCompatDelegate.MODE_NIGHT_NO
                        "night" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                true
            }
            themeMode?.setOnPreferenceChangeListener { _, value ->
                activity?.setTheme(
                    when (value as String) {
                        "blue" -> R.style.Theme_WordCardsBlue
                        "green" -> R.style.Theme_WordCardsGreen
                        else -> R.style.Theme_WordCardsRed
                    }
                )
                activity?.recreate()
                true
            }
            autoBackup?.setOnPreferenceChangeListener { _, newValue ->
                var result = true
                if (newValue as Boolean) {
                    AuthorizationClientManager.authorize(
                        requireActivity(),
                        authorizationLauncher
                    ) { authorizationResult ->
                        if (!isGrantedAllScopes(authorizationResult)) {
                            result = false
                        }
                    }

                    if (result) {
                        BackupWorker.startBackupWorker(requireContext().applicationContext)
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.grant_access),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                } else {
                    WorkManager.getInstance(requireContext()).cancelAllWorkByTag("backup_cards")
                }
                result
            }
        }

        private fun backupSummary() {

            AuthorizationClientManager.authorize(requireActivity()) { authorizationResult ->
                val driveService =
                    BackupAndRestoreManager.googleDriveClient(authorizationResult, requireContext())
                if (BackupAndRestoreManager.isOnline(requireContext())) {
                    var backupTime = ""
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = withContext(Dispatchers.IO) {
                            try {
                                val files = driveService.files().list()
                                    .setSpaces("appDataFolder")
                                    .setFields("files(id, name, createdTime)")
                                    .setPageSize(10)
                                    .execute()
                                for (file in files.files) {
                                    when (file.name) {
                                        getString(R.string.file_name) -> {
                                            backupTime = getString(R.string.last_backup) +
                                                    TimeManager.getTimeWithZone(file.createdTime.toString())
                                        }
                                    }
                                }
                                true
                            } catch (e: GoogleAuthIOException) {
                                println("Authorisation error: ${e.message}")
                                false
                            } catch (e: Exception) {
                                println("Authorisation error: ${e.message}")
                                false
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (success) {
                                val backupSummary: Preference? = findPreference("backup")
                                backupSummary?.summary = backupTime
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
