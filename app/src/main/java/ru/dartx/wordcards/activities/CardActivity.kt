package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.dartx.wordcards.databinding.ActivityCardBinding
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager.addDays
import ru.dartx.wordcards.utils.TimeManager.getCurrentTime

class CardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardBinding
    private var card: Card? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getCard()
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
        val remindDate = addDays(currentDate, 1)
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
        return card?.copy(
            word = edWord.text.toString(),
            examples = edExamples.text.toString(),
            translation = edTranslation.text.toString()
        )

    }
}