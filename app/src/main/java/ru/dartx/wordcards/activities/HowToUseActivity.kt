package ru.dartx.wordcards.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.dartx.wordcards.databinding.ActivityHowToUseBinding
import ru.dartx.wordcards.utils.ThemeManager

class HowToUseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHowToUseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityHowToUseBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}