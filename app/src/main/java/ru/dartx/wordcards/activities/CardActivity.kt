package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityCardBinding
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
        getCard()
        fieldState()
        binding.btSave.setOnClickListener {
            setMainResult()
        }
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
        var editState = MainActivity.CARD_STATE_NEW
        val tempCard = if (card == null) {
            newCard()
        } else {
            editState = MainActivity.CARD_STATE_EDIT
            updateCard()
        }
        val i = Intent().apply {
            putExtra(MainActivity.CARD_DATA, tempCard)
            putExtra(MainActivity.CARD_STATE, editState)
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
}