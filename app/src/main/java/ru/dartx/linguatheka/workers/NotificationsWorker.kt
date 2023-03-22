package ru.dartx.linguatheka.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.activities.CardActivity
import ru.dartx.linguatheka.activities.MainActivity
import ru.dartx.linguatheka.activities.MainApp
import ru.dartx.linguatheka.activities.SnoozeDialogActivity
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.utils.HtmlManager
import ru.dartx.linguatheka.utils.TimeManager

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
        val database = MainDataBase.getDataBase(applicationContext as MainApp)
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
            val examples = database.getDao().findExamplesByCardId(card.id!!)
            var moreThanOneLine = false
            var examplesForCard = ""
            examples.forEach { example ->
                if (!example.finished) {
                    if (moreThanOneLine) {
                        examplesForCard += "\n"
                    }
                    examplesForCard += HtmlManager.getFromHtml(example.example).trim()
                    moreThanOneLine = true
                }
            }
            resultIntent.putExtra(MainActivity.CARD_DATA, card)
            snoozeIntent.putExtra(MainActivity.CARD_DATA, card)
            doneIntent.putExtra(MainActivity.CARD_DATA, card)
            resultPendingIntent = TaskStackBuilder.create(applicationContext).run {
                addNextIntentWithParentStack(resultIntent)
                getPendingIntent(card.id, PendingIntent.FLAG_IMMUTABLE)
            }
            snoozePendingIntent = PendingIntent.getActivity(
                applicationContext,
                card.id,
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
                        .bigText(examplesForCard)
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
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                notify(card.id, builder.build())
            }
        }
    }

    companion object {
        const val ACTION_DONE = "wc_done"
    }
}