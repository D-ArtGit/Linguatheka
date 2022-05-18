package ru.dartx.wordcards.workers

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.dartx.wordcards.R
import ru.dartx.wordcards.activities.MainActivity
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.utils.TimeManager

class NotificationsWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val database = MainDataBase.getDataBase(applicationContext)
        val notificationCards = database.getDao().notificationCards(TimeManager.getCurrentTime())
        notificationCards.forEach { card ->
            Log.d("DArtX", card.word)
            val builder = NotificationCompat.Builder(applicationContext, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_search)
                .setContentTitle(card.word)
                .setContentText(applicationContext.getString(R.string.time_to_repeat))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${card.examples}\n${card.translation}")
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(card.id!!, builder.build())
            }
        }
        return Result.success()
    }

}