package ru.dartx.wordcards.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityNewCardBinding

class NewCardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewCardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}