package ru.dartx.wordcards.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.dartx.wordcards.R
import ru.dartx.wordcards.activities.CardActivity
import ru.dartx.wordcards.activities.MainActivity
import ru.dartx.wordcards.activities.SnoozeDialogActivity
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.utils.TimeManager

class NotificationsWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        createChannel()
        createNotifications()
        return Result.success()
    }

    private fun createChannel() {
        val name = applicationContext.getString(R.string.channel_name)
        val descriptionText = applicationContext.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(MainActivity.CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotifications() {
        val database = MainDataBase.getDataBase(applicationContext)
        val notificationCards = database.getDao().notificationCards(TimeManager.getCurrentTime())
        val resultIntent = Intent(applicationContext, CardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val snoozeIntent = Intent(applicationContext, SnoozeDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val doneIntent = Intent(applicationContext, TapDoneReceiver::class.java).apply {
            action = ACTION_DONE
        }
        var resultPendingIntent: PendingIntent?
        var snoozePendingIntent: PendingIntent?
        var donePendingIntent: PendingIntent?
        notificationCards.forEach { card ->
            resultIntent.putExtra(MainActivity.CARD_DATA, card)
            snoozeIntent.putExtra(MainActivity.CARD_DATA, card)
            doneIntent.putExtra(MainActivity.CARD_DATA, card)
            resultPendingIntent = TaskStackBuilder.create(applicationContext).run {
                addNextIntentWithParentStack(resultIntent)
                getPendingIntent(card.id!!, PendingIntent.FLAG_IMMUTABLE)
            }
            snoozePendingIntent = PendingIntent.getActivity(
                applicationContext,
                card.id!!,
                snoozeIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            donePendingIntent =
                PendingIntent.getBroadcast(
                    applicationContext,
                    card.id,
                    doneIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            val builder = NotificationCompat.Builder(applicationContext, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_icon_notif_50)
                .setContentTitle(card.word)
                .setContentText(applicationContext.getString(R.string.time_to_repeat))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${card.examples}\n${card.translation}")
                )
                .setContentIntent(resultPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup("word_notification")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(
                    R.drawable.ic_done,
                    applicationContext.getString(R.string.done),
                    donePendingIntent
                )
                .addAction(
                    R.drawable.ic_snooze,
                    applicationContext.getString(R.string.snooze),
                    snoozePendingIntent
                )
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(card.id, builder.build())
            }
        }
    }

    companion object {
        const val ACTION_DONE = "wc_done"
    }
}