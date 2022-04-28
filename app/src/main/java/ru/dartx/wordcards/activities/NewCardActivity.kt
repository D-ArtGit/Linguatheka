package ru.dartx.wordcards.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityNewCardBinding
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager

class NewCardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewCardBinding
    private var card: Card? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCardBinding.inflate(layoutInflater)
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
                edWord.setText(card?.word)
                edExamples.setText(card?.examples)
                edTranslation.setText(card?.translation)
            }
        }
    }

    private fun setMainResult() {
        var editState = "new"
        val tempCard = if (card == null) {
            newCard()
        } else {
            editState = "edit"
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
        return Card(
            null,
            "ru_RU",
            binding.edWord.text.toString(),
            binding.edExamples.text.toString(),
            binding.edTranslation.text.toString(),
            TimeManager.getCurrentTime(),
            TimeManager.getCurrentTime(),
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