package ru.dartx.wordcards.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import ru.dartx.wordcards.R
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.dialogs.SnoozeDialog
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager

class SnoozeDialogActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snooze_dialog)
        val sCard = intent.getSerializableExtra(MainActivity.CARD_DATA)
        if (sCard != null) {
            val card = sCard as Card
            SnoozeDialog.showSnoozeDialog(
                this,
                object : SnoozeDialog.Listener {
                    override fun onOkClick(snooze: Int) {
                        val tempCard =
                            card.copy(
                                remindTime = TimeManager.addHours(
                                    TimeManager.getCurrentTime(),
                                    snooze
                                )
                            )
                        mainViewModel.updateCard(tempCard)
                        with(
                            NotificationManagerCompat
                                .from(applicationContext)
                        ) { cancel(card.id!!) }
                        val text =
                            getString(R.string.reminder_snoozed_to) + TimeManager.getTimeFormat(
                                tempCard.remindTime
                            )
                        Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
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