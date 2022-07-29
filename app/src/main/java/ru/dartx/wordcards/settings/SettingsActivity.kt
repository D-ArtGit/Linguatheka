package ru.dartx.wordcards.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var defPreference: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        defPreference.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "theme") {
                setTheme(getSelectedTheme())
                recreate()
            } else if (key == "night_mode") {
                AppCompatDelegate.setDefaultNightMode(getSelectedNightMode())
            }

        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
//            val nightMode: Preference? = findPreference("night_mode")
//            nightMode?.setOnPreferenceChangeListener { _, value ->
//                Log.d("DArtX", "NightMode: ${value as String}")
//                AppCompatDelegate.setDefaultNightMode(
//                    when (value as String) {
//                        "day" -> AppCompatDelegate.MODE_NIGHT_NO
//                        "night" -> AppCompatDelegate.MODE_NIGHT_YES
//                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
//                    }
//                )
//                true
//            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    private fun getSelectedTheme(): Int {
        return when (defPreference.getString(
            "theme",
            "blue"
        )) {
            "blue" -> R.style.Theme_WordCardsBlue
            "green" -> R.style.Theme_WordCardsGreen
            else -> R.style.Theme_WordCardsRed
        }
    }

    private fun getSelectedNightMode(): Int {
        return when (defPreference.getString(
            "night_mode",
            "system"
        )) {
            "day" -> AppCompatDelegate.MODE_NIGHT_NO
            "night" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}