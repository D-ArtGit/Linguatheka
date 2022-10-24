package ru.dartx.wordcards.settings

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.wordcards.R
import ru.dartx.wordcards.utils.BackupAndRestoreManager
import ru.dartx.wordcards.utils.LanguagesManager
import ru.dartx.wordcards.utils.ThemeManager
import ru.dartx.wordcards.utils.TimeManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val defLangPref: ListPreference = findPreference("def_lang")!!
            val nativeLangPref: ListPreference = findPreference("native_lang")!!
            val langArray = LanguagesManager.getLanguages()
            defLangPref.entryValues = langArray[0]
            nativeLangPref.entryValues = langArray[0]
            defLangPref.entries = langArray[1]
            nativeLangPref.entries = langArray[1]
            val nightMode: Preference? = findPreference("night_mode")
            val themeMode: Preference? = findPreference("theme")
            val autoBackup: Preference? = findPreference("auto_backup")
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
                if (!(newValue as Boolean)) {
                    Log.d("DArtX", "Cancel worker")
                    WorkManager.getInstance(requireContext()).cancelAllWorkByTag("backup_cards")
                } else {
                    BackupAndRestoreManager.startBackupWorker(requireContext())
                }
                true
            }

            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null) {
                val googleDriveService =
                    BackupAndRestoreManager.googleDriveClient(account, requireContext())
                if (googleDriveService != null
                    && BackupAndRestoreManager.isOnline(requireContext())
                ) {
                    var backupTime = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        val success = withContext(Dispatchers.IO) {
                            val files = googleDriveService.files().list()
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
                        }
                        withContext(Dispatchers.Main) {
                            if (success) {
                                val backup: Preference? = findPreference("backup")
                                backup?.summary = backupTime
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