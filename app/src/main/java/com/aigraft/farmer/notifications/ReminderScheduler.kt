package com.aigraft.farmer.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aigraft.farmer.MainActivity
import com.aigraft.farmer.R
import com.aigraft.farmer.data.GraftBatch
import com.aigraft.farmer.data.startOfDayMillis
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.GRAFT_PLAN
import com.aigraft.farmer.i18n.PLAN_DAYS
import com.aigraft.farmer.i18n.stringsFor
import java.util.Calendar

private const val CHANNEL_ID = "graft-reminders"
private const val EXTRA_TITLE = "extra_title"
private const val EXTRA_BODY = "extra_body"
private const val EXTRA_BATCH_ID = "extra_batch_id"
private const val EXTRA_NOTIF_ID = "extra_notif_id"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1001)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ReminderScheduler.ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }
}

object ReminderScheduler {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Graft Care Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 8 AM graft care reminders"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleBatch(context: Context, batch: GraftBatch, lang: AppLang) {
        ensureChannel(context)
        cancelBatch(context, batch.id)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val strings = stringsFor(lang)
        val baseDayStart = startOfDayMillis(batch.graftedAtMillis)
        val now = System.currentTimeMillis()

        for (task in GRAFT_PLAN) {
            val triggerCal = Calendar.getInstance().apply {
                timeInMillis = baseDayStart
                add(Calendar.DAY_OF_YEAR, task.day)
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val triggerAt = triggerCal.timeInMillis
            if (triggerAt <= now) continue

            val reqCode = requestCodeFor(batch.id, task.day)
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_TITLE, "${batch.name} · ${strings.dayN} ${task.day}")
                putExtra(EXTRA_BODY, task.text(lang))
                putExtra(EXTRA_BATCH_ID, batch.id)
                putExtra(EXTRA_NOTIF_ID, reqCode)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelBatch(context: Context, batchId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (day in 0..PLAN_DAYS) {
            val reqCode = requestCodeFor(batchId, day)
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun requestCodeFor(batchId: String, day: Int): Int =
        ((batchId.hashCode() and 0x0FFFFFFF) * 31 + day) and 0x7FFFFFFF
}
