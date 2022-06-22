package ru.dartx.wordcards.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.dartx.wordcards.R
import ru.dartx.wordcards.activities.MainActivity
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager
import kotlin.coroutines.CoroutineContext

class TapDoneReceiver : BroadcastReceiver(), CoroutineScope {
    private var job = Job()
    override fun onReceive(context: Context, intent: Intent?) {
        val database = MainDataBase.getDataBase(context)
        val sCard = intent?.getSerializableExtra(MainActivity.CARD_DATA)
        if (sCard != null) {
            val card = sCard as Card
            val step = card.step + 1
            Log.d("DArtX", "received, id = ${card.id}, word = ${card.word}, step = $step")
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
            launch { database.getDao().updateCard(tempCard) }

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

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}