package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.databinding.ActivityHowToUseBinding
import ru.dartx.wordcards.settings.SettingsActivity
import ru.dartx.wordcards.utils.ThemeManager

class HowToUseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHowToUseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = defPreference.edit()
        binding = ActivityHowToUseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.apply {
            chBoxDontShow.isChecked = defPreference.getBoolean("not_show_htu", false)
            btClose.setOnClickListener {
                if (!chBoxDontShow.isChecked) editor.putBoolean("not_show_htu", false)
                else editor.putBoolean("not_show_htu", true)
                editor.apply()
                showLangSettings()
                finish()
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    private fun showLangSettings() {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        if (defPreference.getString("def_lang", "") == "" ||
            defPreference.getString("native_lang", "") == ""
        ) {
            Toast.makeText(this, "Выберите языки в настройках", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    this@HowToUseActivity,
                    SettingsActivity::class.java
                )
            )
        }
    }
}