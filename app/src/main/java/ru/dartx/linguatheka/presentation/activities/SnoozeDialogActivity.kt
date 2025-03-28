package ru.dartx.linguatheka.presentation.activities

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.presentation.activities.CardActivity.Companion.CARD_DATA
import ru.dartx.linguatheka.presentation.dialogs.SnoozeDialog
import ru.dartx.linguatheka.presentation.viewmodels.MainViewModel
import ru.dartx.linguatheka.utils.ThemeManager
import ru.dartx.linguatheka.utils.TimeManager

class SnoozeDialogActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory(MainDataBase.getDataBase(applicationContext as MainApp))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedDialogTheme(this))
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_snooze_dialog)
        val card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(CARD_DATA, Card::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(CARD_DATA) as Card?
        }
        if (card != null) {
            SnoozeDialog.showSnoozeDialog(
                this,
                object : SnoozeDialog.Listener {
                    override fun onOkClick(delay: Int) {
                        val tempCard =
                            card.copy(
                                remindTime = TimeManager.addHours(
                                    TimeManager.getCurrentTime(),
                                    delay
                                )
                            )
                        val defPreference =
                            PreferenceManager.getDefaultSharedPreferences(this@SnoozeDialogActivity)
                        val editor = defPreference.edit()
                        editor.putString("snooze", delay.toString())
                        editor.apply()
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