package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityCardBinding
import ru.dartx.wordcards.db.CardAdapter
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager
import ru.dartx.wordcards.utils.TimeManager.addDays
import ru.dartx.wordcards.utils.TimeManager.getCurrentTime
import ru.dartx.wordcards.utils.TimeManager.isTimeToSetNewRemind

class CardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardBinding
    private var card: Card? = null
    private var cardState = MainActivity.CARD_STATE_VIEW
    private lateinit var daysArray: IntArray
    private var timeToSetRemind = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        daysArray = resources.getIntArray(R.array.remind_days)
        actionBarSettings()
        getCard()
        fieldState()
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
        Log.d("DArtX", "editState: $cardState")
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
            R.id.edit -> editCard()
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
        } else cardState = MainActivity.CARD_STATE_NEW
    }

    private fun fieldState() = with(binding) {
        if (cardState == MainActivity.CARD_STATE_VIEW) timeToSetRemind =
            isTimeToSetNewRemind(card!!.remindTime)
        when {
            timeToSetRemind -> {
                btSave.setImageResource(R.drawable.ic_check)
            }
            cardState != MainActivity.CARD_STATE_NEW -> {
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
            cardState = MainActivity.CARD_STATE_EDIT
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
            Log.d("DArtX", "CA: ${card?.id}")
            putExtra(MainActivity.CARD_ID, card?.id.toString())
            putExtra(MainActivity.CARD_STATE, cardState)
        }
        setResult(RESULT_OK, i)
        finish()
    }

    private fun editCard() {
        cardState = MainActivity.CARD_STATE_EDIT
        invalidateOptionsMenu()
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

    private fun actionBarSettings() {
        val ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
    }
}