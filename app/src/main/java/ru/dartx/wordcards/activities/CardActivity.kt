package ru.dartx.wordcards.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.StyleSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.R
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.dialogs.ConfirmDialog
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.HtmlManager
import ru.dartx.wordcards.utils.TimeManager
import ru.dartx.wordcards.utils.TimeManager.addDays
import ru.dartx.wordcards.utils.TimeManager.getCurrentTime
import ru.dartx.wordcards.utils.TimeManager.isTimeToSetNewRemind
import ru.dartx.wordcards.databinding.ActivityCardBinding as ActivityCardBinding1

class CardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardBinding1
    private var ab: ActionBar? = null
    private var card: Card? = null
    private var cardState = CARD_STATE_VIEW
    private lateinit var daysArray: IntArray
    private var timeToSetRemind = false
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }
    private lateinit var defPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityCardBinding1.inflate(layoutInflater)
        setThemeColor(defPreference.getString("theme", "blue").toString())
        setContentView(binding.root)
        daysArray = resources.getIntArray(R.array.remind_days)
        getCard()
        fieldState()
        actionBarSettings()
        binding.btSave.setOnClickListener {
            setMainResult()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.card_menu, menu)
        val itemBold = menu?.findItem(R.id.bold)
        val itemEdit = menu?.findItem(R.id.edit)
        val itemReset = menu?.findItem(R.id.reset)
        val itemDelete = menu?.findItem(R.id.delete)
        if (cardState == CARD_STATE_VIEW) {
            itemEdit?.isVisible = true
            itemBold?.isVisible = false
        }
        if (cardState == CARD_STATE_NEW) {
            itemReset?.isVisible = false
            itemDelete?.isVisible = false
            itemEdit?.isVisible = false
        }
        if (cardState == CARD_STATE_EDIT) {
            itemBold?.isVisible = true
            itemEdit?.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete -> deleteCard()
            R.id.reset -> resetCardState()
            R.id.edit -> editCardState()
            R.id.bold -> setBoldForSelectedText()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getCard() {
        val sCard = intent.getSerializableExtra(MainActivity.CARD_DATA)
        if (sCard != null) {
            card = sCard as Card
            with(NotificationManagerCompat.from(applicationContext)) { cancel(card!!.id!!) }
            binding.apply {
                tvCardWord.text = card!!.word
                edWord.setText(card?.word)
                tvCardExamples.text = HtmlManager.getFromHtml(card?.examples_html!!).trim()
                edExamples.setText(HtmlManager.getFromHtml(card?.examples_html!!).trim())
                tvCardTranslation.text = HtmlManager.getFromHtml(card?.translation_html!!).trim()
                edTranslation.setText(HtmlManager.getFromHtml(card?.translation_html!!).trim())
            }
            timeToSetRemind = isTimeToSetNewRemind(card!!.remindTime)
            cardState = if (timeToSetRemind) CARD_STATE_CHECK
            else CARD_STATE_VIEW
        } else cardState = CARD_STATE_NEW
    }

    private fun fieldState() = with(binding) {
        when (cardState) {
            CARD_STATE_CHECK -> {
                btSave.setImageResource(R.drawable.ic_check)
            }
            CARD_STATE_VIEW -> {
                btSave.visibility = View.GONE
            }
            else -> {
                tvCardWord.visibility = View.GONE
                tvCardExamples.visibility = View.GONE
                tvCardTranslation.visibility = View.GONE
                edWord.visibility = View.VISIBLE
                edExamples.visibility = View.VISIBLE
                edTranslation.visibility = View.VISIBLE
            }
        }
    }

    private fun setMainResult() {
        binding.edExamples.clearComposingText()
        binding.edTranslation.clearComposingText()
        val tempCard = if (card == null) {
            newCard()
        } else {
            updateCard()
        }
        if (tempCard != null) {
            val i = Intent()
            setResult(RESULT_OK, i)
            if (cardState == CARD_STATE_NEW) {
                mainViewModel.insertCard(tempCard)
            } else {
                mainViewModel.updateCard(tempCard)
            }

            finish()
        }
    }

    private fun newCard(): Card? {
        val currentTime = getCurrentTime()
        val remindTime = addDays(currentTime, daysArray[0])
        binding.apply {
            if (edWord.text.isNullOrEmpty()) {
                edWord.error = getString(R.string.fill_field)
                return null
            } else if (edExamples.text.isNullOrEmpty()) {
                edExamples.error = getString(R.string.fill_field)
                return null
            } else {
                return Card(
                    null,
                    "ru_RU",
                    edWord.text.toString(),
                    edExamples.text.toString(),
                    HtmlManager.toHtml(edExamples.text),
                    edTranslation.text.toString(),
                    HtmlManager.toHtml(edTranslation.text),
                    currentTime,
                    remindTime,
                    0
                )
            }
        }
    }

    private fun updateCard(): Card? = with(binding) {
        var remindTime = addDays(getCurrentTime(), daysArray[0])
        var step = 0
        if (cardState != CARD_STATE_RESET) {
            step = card!!.step
            remindTime = card!!.remindTime
            if (timeToSetRemind) {
                step++
                remindTime = if (step <= 8) {
                    addDays(getCurrentTime(), daysArray[step])
                } else {
                    TimeManager.ENDLESS_FUTURE
                }
            }
        }
        if (edWord.text.isNullOrEmpty()) {
            edWord.error = getString(R.string.fill_field)
            return null
        } else if (edExamples.text.isNullOrEmpty()) {
            edExamples.error = getString(R.string.fill_field)
            return null
        } else {
            return card?.copy(
                word = edWord.text.toString(),
                examples = edExamples.text.toString(),
                examples_html = HtmlManager.toHtml(edExamples.text),
                translation = edTranslation.text.toString(),
                translation_html = HtmlManager.toHtml(edTranslation.text),
                remindTime = remindTime,
                step = step
            )
        }
    }

    private fun deleteCard() {
        val message = getString(R.string.confirm_delete)
        ConfirmDialog.showDialog(
            this, object : ConfirmDialog.Listener {
                override fun onClick() {
                    cardState = CARD_STATE_DELETE
                    val i = Intent()
                    mainViewModel.deleteCard(card?.id!!)
                    setResult(RESULT_OK, i)
                    finish()
                }
            },
            message
        )


    }

    private fun editCardState() {
        cardState = CARD_STATE_EDIT
        invalidateOptionsMenu()
        ab?.setTitle(R.string.edit_card)
        binding.apply {
            btSave.visibility = View.VISIBLE
            btSave.setImageResource(R.drawable.ic_save)
            tvCardWord.visibility = View.GONE
            tvCardExamples.visibility = View.GONE
            tvCardTranslation.visibility = View.GONE
            edWord.visibility = View.VISIBLE
            edExamples.visibility = View.VISIBLE
            edTranslation.visibility = View.VISIBLE
        }
    }

    private fun resetCardState() {
        val message = getString(R.string.confirm_reset)
        ConfirmDialog.showDialog(
            this, object : ConfirmDialog.Listener {
                override fun onClick() {
                    cardState = CARD_STATE_RESET
                    setMainResult()
                }
            },
            message
        )
    }

    private fun setBoldForSelectedText() = with(binding) {
        Log.d("DArtX", "Focused: ${edExamples.hasFocus()}")
        if (edExamples.hasFocus()) {
            val startPos = edExamples.selectionStart
            val endPos = edExamples.selectionEnd
            val styles = edExamples.text.getSpans(startPos, endPos, StyleSpan::class.java)
            Log.d("DArtX", "S: $startPos, E: $endPos, ${styles.size}")
            var boldStyle: StyleSpan? = null
            if (styles.isNotEmpty()) edExamples.text.removeSpan(styles[0])
            else boldStyle = StyleSpan(Typeface.BOLD)
            edExamples.text.setSpan(boldStyle, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            edExamples.text.trim()
            edExamples.setSelection(startPos)
        } else if (edTranslation.hasFocus()) {
            val startPos = edTranslation.selectionStart
            val endPos = edTranslation.selectionEnd
            val styles = edTranslation.text.getSpans(startPos, endPos, StyleSpan::class.java)
            var boldStyle: StyleSpan? = null
            if (styles.isNotEmpty()) edTranslation.text.removeSpan(styles[0])
            else boldStyle = StyleSpan(Typeface.BOLD)
            edTranslation.text.setSpan(
                boldStyle,
                startPos,
                endPos,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            edTranslation.text.trim()
            edTranslation.setSelection(startPos)
        }
    }

    private fun actionBarSettings() {
        ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
        when (cardState) {
            CARD_STATE_CHECK -> ab?.setTitle(R.string.repeat_card)
            CARD_STATE_NEW -> ab?.setTitle(R.string.fill_card)
            CARD_STATE_VIEW -> ab?.setTitle(R.string.view_card)
        }
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

    private fun setThemeColor(theme: String) {
        binding.apply {
            when (theme) {
                "blue" -> lLCardActivity.setBackgroundColor(getColor(R.color.blue_100))
                "green" -> lLCardActivity.setBackgroundColor(getColor(R.color.green_100))
                else -> lLCardActivity.setBackgroundColor(getColor(R.color.red_100))
            }
        }
    }

    companion object {
        const val CARD_STATE_NEW = 1
        const val CARD_STATE_EDIT = 2
        const val CARD_STATE_VIEW = 3
        const val CARD_STATE_DELETE = 4
        const val CARD_STATE_CHECK = 5
        const val CARD_STATE_RESET = 6
    }
}