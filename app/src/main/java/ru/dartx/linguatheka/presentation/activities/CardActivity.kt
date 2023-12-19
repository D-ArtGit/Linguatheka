package ru.dartx.linguatheka.presentation.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityCardBinding
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.presentation.dialogs.ConfirmDialog
import ru.dartx.linguatheka.presentation.fragments.CardEditFragment
import ru.dartx.linguatheka.presentation.fragments.CardViewFragment
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_CHECK
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_EDIT
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_NEW
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_VIEW
import ru.dartx.linguatheka.settings.SettingsActivity
import ru.dartx.linguatheka.utils.ThemeManager

class CardActivity : AppCompatActivity(), OnActionListener,
    CoroutineScope by MainScope() {
    private lateinit var binding: ActivityCardBinding
    private var ab: ActionBar? = null
    private var card: Card? = null
    private var cardState = CARD_STATE_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showLangSettings(defPreference)
        actionBarSettings()
        getCard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.card_menu, menu)
        val itemEdit = menu?.findItem(R.id.edit)
        val itemReset = menu?.findItem(R.id.reset)
        val itemDelete = menu?.findItem(R.id.delete)
        if (cardState == CARD_STATE_VIEW || cardState == CARD_STATE_CHECK) {
            itemEdit?.isVisible = true
        }
        if (cardState == CARD_STATE_NEW) {
            itemReset?.isVisible = false
            itemDelete?.isVisible = false
            itemEdit?.isVisible = false
        }
        if (cardState == CARD_STATE_EDIT) {
            itemEdit?.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete -> deleteCard()
            R.id.reset -> resetCardState()
            R.id.edit -> editCard()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getCard() {
        if (intent.hasExtra(CARD_DATA)) {
            card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(CARD_DATA, Card::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getSerializableExtra(CARD_DATA) as Card?
            }
            if (card != null) {
                with(NotificationManagerCompat.from(applicationContext)) { cancel(card!!.id!!) }
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.example_item_container,
                        CardViewFragment.newInstance(card!!.id!!),
                        CARD_FRAGMENT
                    )
                    .commit()
            } else throw RuntimeException("There are no card data in the intent")
        } else {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.example_item_container,
                    CardEditFragment.newInstance(0),
                    CARD_FRAGMENT
                )
                .commit()
        }
    }

    private fun deleteCard() {
        val message = getString(R.string.confirm_delete)
        ConfirmDialog.showDialog(
            this, object : ConfirmDialog.Listener {
                override fun onClick() {
                    if (cardState == CARD_STATE_EDIT) {
                        val fragment =
                            supportFragmentManager.findFragmentByTag(CARD_FRAGMENT) as CardEditFragment
                        fragment.deleteCard()
                    } else {
                        val fragment =
                            supportFragmentManager.findFragmentByTag(CARD_FRAGMENT) as CardViewFragment
                        fragment.deleteCard()
                    }
                }

                override fun onCancel() {

                }
            },
            message
        )
    }

    private fun editCard() {
        card?.id?.let {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.example_item_container,
                    CardEditFragment.newInstance(it),
                    CARD_FRAGMENT
                )
                .commit()
        }
    }

    private fun resetCardState() {
        val message = getString(R.string.confirm_reset)
        ConfirmDialog.showDialog(
            this, object : ConfirmDialog.Listener {
                override fun onClick() {
                    if (cardState == CARD_STATE_EDIT) {
                        val fragment =
                            supportFragmentManager.findFragmentByTag(CARD_FRAGMENT) as CardEditFragment
                        fragment.resetCard()
                    } else {
                        val fragment =
                            supportFragmentManager.findFragmentByTag(CARD_FRAGMENT) as CardViewFragment
                        fragment.resetCard()
                    }
                }

                override fun onCancel() {

                }
            },
            message
        )
    }

    private fun actionBarSettings() {
        ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setActionBarTitle() {
        when (cardState) {
            CARD_STATE_CHECK -> ab?.setTitle(R.string.repeat_card)
            CARD_STATE_NEW -> ab?.setTitle(R.string.fill_card)
            CARD_STATE_EDIT -> ab?.setTitle(R.string.edit_card)
            CARD_STATE_VIEW -> ab?.setTitle(R.string.view_card)
        }
    }

    private fun showLangSettings(defPreference: SharedPreferences) {
        if (defPreference.getString("def_lang", "") == "" ||
            defPreference.getString("native_lang", "") == ""
        ) {
            Toast.makeText(this, getString(R.string.choose_lang_settings), Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    this@CardActivity,
                    SettingsActivity::class.java
                )
            )
            finish()
        }
    }

    companion object {
        const val CARD_DATA = "card"
        const val CARD_FRAGMENT = "card_fragment"
        const val NEED_TO_SCROLL = "need_to_scroll"

        fun intentCardActivityForEdit(context: Context, card: Card): Intent {
            val i = Intent(context, CardActivity::class.java)
            i.putExtra(CARD_DATA, card)
            return i
        }
    }

    override fun onFinished(message: String, isChecked: Boolean) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (isChecked) {
            val i = Intent()
            i.putExtra(NEED_TO_SCROLL, true)
            setResult(RESULT_OK, i)
        }
        finish()
    }

    override fun setState(state: Int) {
        cardState = state
        invalidateOptionsMenu()
        setActionBarTitle()
    }
}