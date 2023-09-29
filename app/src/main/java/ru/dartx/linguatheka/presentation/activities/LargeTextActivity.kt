package ru.dartx.linguatheka.presentation.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityLargeTextBinding
import ru.dartx.linguatheka.settings.SettingsActivity
import ru.dartx.linguatheka.utils.ThemeManager
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException

class LargeTextActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLargeTextBinding
    private var ab: ActionBar? = null
    private var largeTextType = HOW_TO_USE
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = defPreference.edit()
        binding = ActivityLargeTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        largeTextType = intent.getIntExtra(LARGE_TEXT_TYPE, HOW_TO_USE)
        actionBarSettings()
        initMode(defPreference)
        setOnClickListener(editor)
    }

    private fun setOnClickListener(editor: SharedPreferences.Editor) = with(binding) {
        btClose.setOnClickListener {
            if (largeTextType == HOW_TO_USE) {
                if (!chBoxDontShow.isChecked) editor.putBoolean("not_show_htu", false)
                else editor.putBoolean("not_show_htu", true)
                editor.apply()
                showLangSettings()
            }
            finish()
        }
    }

    private fun initMode(defPreference: SharedPreferences) = with(binding) {
        if (largeTextType == HOW_TO_USE)
            chBoxDontShow.isChecked = defPreference.getBoolean("not_show_htu", false)
        else chBoxDontShow.visibility = View.GONE
        val text: String? = when (largeTextType) {
            PRIVACY -> getStringFromRawRes(R.raw.privacy)
            AGREEMENT -> getStringFromRawRes(R.raw.agreement)
            else -> null
        }
        if (!text.isNullOrEmpty()) tvLargeText.text = text
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) this.finish()
        return super.onOptionsItemSelected(item)
    }

    private fun showLangSettings() {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        if (defPreference.getString("def_lang", "") == "" ||
            defPreference.getString("native_lang", "") == ""
        ) {
            Toast.makeText(this, getString(R.string.choose_lang_settings), Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    this@LargeTextActivity,
                    SettingsActivity::class.java
                )
            )
        }
    }

    private fun actionBarSettings() {
        ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
        when (largeTextType) {
            PRIVACY -> ab?.setTitle(R.string.privacy)
            AGREEMENT -> ab?.setTitle(R.string.agreement)
        }
    }

    private fun getStringFromRawRes(rawRes: Int): String? {
        val inputStream: InputStream?
        try {
            inputStream = resources.openRawResource(rawRes)
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            return null
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = byteArrayOf(1024.toByte())
        var length = inputStream.read(buffer)
        try {
            while (length != -1) {
                byteArrayOutputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } finally {
            try {
                inputStream.close()
                byteArrayOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val resultString: String?
        try {
            resultString = byteArrayOutputStream.toString("UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        }
        return resultString
    }

    companion object {
        const val LARGE_TEXT_TYPE = "largeTextType"
        const val HOW_TO_USE = 1
        const val PRIVACY = 2
        const val AGREEMENT = 3

        fun intentForPrivacy(context: Context): Intent {
            val i = Intent(context, LargeTextActivity::class.java)
            i.putExtra(LARGE_TEXT_TYPE, PRIVACY)
            return i
        }

        fun intentForAgreement(context: Context): Intent {
            val i = Intent(context, LargeTextActivity::class.java)
            i.putExtra(LARGE_TEXT_TYPE, AGREEMENT)
            return i
        }

        fun intentForHowToUse(context: Context): Intent {
            val i = Intent(context, LargeTextActivity::class.java)
            i.putExtra(LARGE_TEXT_TYPE, HOW_TO_USE)
            return i
        }
    }
}