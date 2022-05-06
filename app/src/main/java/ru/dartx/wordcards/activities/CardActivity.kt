package ru.dartx.wordcards.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.getSpans
import ru.dartx.wordcards.R
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager
import ru.dartx.wordcards.utils.TimeManager.addDays
import ru.dartx.wordcards.utils.TimeManager.getCurrentTime
import ru.dartx.wordcards.utils.TimeManager.isTimeToSetNewRemind
import ru.dartx.wordcards.databinding.ActivityCardBinding as ActivityCardBinding1

class CardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardBinding1
    private var ab: ActionBar? = null
    private var card: Card? = null
    private var cardState = MainActivity.CARD_STATE_VIEW
    private lateinit var daysArray: IntArray
    private var timeToSetRemind = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardBinding1.inflate(layoutInflater)
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
        if (cardState == MainActivity.CARD_STATE_VIEW) {
            itemEdit?.isVisible = true
            itemBold?.isVisible = false
        }
        if (cardState == MainActivity.CARD_STATE_NEW) {
            itemReset?.isVisible = false
            itemDelete?.isVisible = false
            itemEdit?.isVisible = false
        }
        if (cardState == MainActivity.CARD_STATE_EDIT) {
            itemBold?.isVisible = true
            itemEdit?.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete -> deleteCard()
            R.id.edit -> editCardState()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getCard() {
        val sCard = intent.getSerializableExtra(MainActivity.CARD_DATA)
        if (sCard != null) {
            card = sCard as Card
            binding.apply {
                tvCardWord.text = card!!.word
                edWord.setText(card?.word)
                tvCardExamples.text = card!!.examples
                edExamples.setText(card?.examples)
                tvCardTranslation.text = card!!.translation
                edTranslation.setText(card?.translation)
            }
            timeToSetRemind = isTimeToSetNewRemind(card!!.remindTime)
            cardState = if (timeToSetRemind) MainActivity.CARD_STATE_CHECK
            else MainActivity.CARD_STATE_VIEW
        } else cardState = MainActivity.CARD_STATE_NEW
    }

    private fun fieldState() = with(binding) {
        when (cardState) {
            MainActivity.CARD_STATE_CHECK -> {
                btSave.setImageResource(R.drawable.ic_check)
            }
            MainActivity.CARD_STATE_VIEW -> {
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
        val tempCard = if (card == null) {
            newCard()
        } else {
            updateCard()
        }
        val i = Intent().apply {
            putExtra(MainActivity.CARD_DATA, tempCard)
            putExtra(MainActivity.CARD_STATE, cardState)
        }
        setResult(RESULT_OK, i)
        finish()
    }

    private fun newCard(): Card {
        val currentDate = getCurrentTime()
        val remindDate = addDays(currentDate, daysArray[0])
        return Card(
            null,
            "ru_RU",
            binding.edWord.text.toString(),
            binding.edExamples.text.toString(),
            binding.edTranslation.text.toString(),
            currentDate,
            remindDate,
            0
        )
    }

    private fun updateCard(): Card? = with(binding) {
        var step = card!!.step
        var remindTime = card!!.remindTime
        if (timeToSetRemind) {
            step = card!!.step + 1
            remindTime = if (step <= 8) {
                addDays(getCurrentTime(), daysArray[step])
            } else {
                TimeManager.ENDLESS_FUTURE
            }
        }
        return card?.copy(
            word = edWord.text.toString(),
            examples = edExamples.text.toString(),
            translation = edTranslation.text.toString(),
            remindTime = remindTime,
            step = step
        )

    }

    private fun deleteCard() {
        cardState = MainActivity.CARD_STATE_DELETE
        val i = Intent().apply {
            putExtra(MainActivity.CARD_ID, card?.id.toString())
            putExtra(MainActivity.CARD_STATE, cardState)
        }
        setResult(RESULT_OK, i)
        finish()
    }

    private fun editCardState() {
        cardState = MainActivity.CARD_STATE_EDIT
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

    private fun setBoldForSelectedText() = with(binding) {
        val startPos = edExamples.selectionStart
        val endPos = edExamples.selectionEnd
        val styles = edExamples.text.getSpans(startPos, endPos, StyleSpan::class.java)
        var boldStyle: StyleSpan? = null
        if (styles.isNotEmpty()) edExamples.text.removeSpan(styles[0])
        else boldStyle = StyleSpan(Typeface.BOLD)
        edExamples.text.setSpan(boldStyle, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        edExamples.text.trim()
        edExamples.setSelection(startPos)
    }

    private fun actionBarSettings() {
        ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
        when (cardState) {
            MainActivity.CARD_STATE_CHECK -> ab?.setTitle(R.string.repeat_card)
            MainActivity.CARD_STATE_NEW -> ab?.setTitle(R.string.fill_card)
            MainActivity.CARD_STATE_VIEW -> ab?.setTitle(R.string.view_card)
        }
    }
}