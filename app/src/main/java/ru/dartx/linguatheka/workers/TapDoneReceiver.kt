package ru.dartx.linguatheka.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.activities.MainActivity
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.entities.Card
import ru.dartx.linguatheka.utils.TimeManager
import kotlin.coroutines.CoroutineContext

class TapDoneReceiver : BroadcastReceiver(), CoroutineScope {
    private var job = Job()
    override fun onReceive(context: Context, intent: Intent?) {
        val database = MainDataBase.getDataBase(context)
        val card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(MainActivity.CARD_DATA, Card::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getSerializableExtra(MainActivity.CARD_DATA) as Card?
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