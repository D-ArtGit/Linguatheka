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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btSave.setOnClickListener {
            val card = Card(
                null,
                "ru_RU",
                binding.edWord.text.toString(),
                binding.edExamples.text.toString(),
                binding.edTranslation.text.toString(),
                TimeManager.getCurrentTime(),
                TimeManager.getCurrentTime(),
                0
            )
            val i = Intent().apply {
                putExtra("card", card)
            }
            setResult(RESULT_OK, i)
            finish()
        }
    }
}