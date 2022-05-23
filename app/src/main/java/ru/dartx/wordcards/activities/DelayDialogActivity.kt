package ru.dartx.wordcards.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import ru.dartx.wordcards.R
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.dialogs.DelayDialog
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager

class DelayDialogActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delay_dialog)
        val sCard = intent.getSerializableExtra(MainActivity.CARD_DATA)
        if (sCard != null) {
            val card = sCard as Card
            DelayDialog.showDelayDialog(
                this,
                object : DelayDialog.Listener {
                    override fun onOkClick(delay: Int) {
                        val tempCard =
                            card.copy(
                                remindTime = TimeManager.addHours(
                                    TimeManager.getCurrentTime(),
                                    delay
                                )
                            )
                        mainViewModel.updateCard(tempCard)
                        with(
                            NotificationManagerCompat
                                .from(applicationContext)
                        ) { cancel(card.id!!) }
                        setResult(RESULT_OK)
                        finish()
                    }

                    override fun onCancelClick() {
                        finish()
                    }
                }
            )
        } else setResult(RESULT_CANCELED)
    }
}