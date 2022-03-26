package ru.dartx.wordcards.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}