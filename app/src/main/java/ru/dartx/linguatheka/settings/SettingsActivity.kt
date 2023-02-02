package ru.dartx.linguatheka.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.activities.GoogleSignInActivity
import ru.dartx.linguatheka.utils.*

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
            backupSummary()
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
                var result = true
                if (newValue as Boolean) {
                    val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                    if (account == null) {
                        result = false
                    } else {
                        val googleDriveService =
                            BackupAndRestoreManager.googleDriveClient(account, requireContext())
                        if (googleDriveService == null) {
                            result = false
                            GoogleSignInManager.googleSignOut(requireContext())
                        }
                    }
                    if (result) {
                        BackupAndRestoreManager.startBackupWorker(requireContext().applicationContext)
                    } else {
                        startActivity(Intent(requireContext(), GoogleSignInActivity::class.java))
                    }
                } else {
                    WorkManager.getInstance(requireContext()).cancelAllWorkByTag("backup_cards")
                }
                result
            }
        }

        private fun backupSummary() {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null) {
                val googleDriveService =
                    BackupAndRestoreManager.googleDriveClient(account, requireContext())
                if (googleDriveService != null
                    && BackupAndRestoreManager.isOnline(requireContext())
                ) {
                    var backupTime = ""
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = withContext(Dispatchers.IO) {
                            try {
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
                            } catch (e: GoogleAuthIOException) {
                                println("Authorisation error: ${e.message}")
                                val gso =
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestProfile()
                                        .requestEmail()
                                        .requestScopes(
                                            Scope(DriveScopes.DRIVE_FILE),
                                            Scope(DriveScopes.DRIVE_APPDATA)
                                        )
                                        .build()
                                val mGoogleSignInClient =
                                    GoogleSignIn.getClient(requireContext(), gso)
                                mGoogleSignInClient.signOut()
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