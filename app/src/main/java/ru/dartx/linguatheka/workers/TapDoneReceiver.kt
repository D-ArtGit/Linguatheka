package ru.dartx.linguatheka.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.entities.Card
import ru.dartx.linguatheka.entities.Example
import ru.dartx.linguatheka.presentation.activities.CardActivity.Companion.CARD_DATA
import ru.dartx.linguatheka.presentation.activities.MainApp
import ru.dartx.linguatheka.utils.TimeManager

class TapDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val database = MainDataBase.getDataBase(context.applicationContext as MainApp)
        val card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(CARD_DATA, Card::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getSerializableExtra(CARD_DATA) as Card?
        }
        if (card != null) {
            val step = card.step + 1
            val daysArray = context.resources.getIntArray(R.array.remind_days)
            val remindTime = if (step <= 8) {
                TimeManager.addDays(TimeManager.getCurrentTime(), daysArray[step])
            } else {
                TimeManager.ENDLESS_FUTURE
            }
            val tempCard =
                card.copy(
                    remindTime = remindTime,
                    step = step
                )

            CoroutineScope(Dispatchers.IO).launch {
                if (step > 8) {
                    val tmpExampleList: ArrayList<Example> = arrayListOf()
                    val examplesForCard = database.getDao().findExamplesByCardId(card.id!!)
                    examplesForCard.forEachIndexed { index, example ->
                        tmpExampleList.add(
                            example.copy(
                                id = index + 1,
                                finished = true
                            )
                        )
                    }
                    database.getDao().updateCardWithItems(tempCard, tmpExampleList)
                } else {
                    database.getDao().updateCard(tempCard)
                }
            }

            with(
                NotificationManagerCompat
                    .from(context)
            ) { cancel(card.id!!) }
            Toast.makeText(
                context,
                context.resources.getString(R.string.mark_done),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}