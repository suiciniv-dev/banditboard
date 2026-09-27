package dev.clawdboard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.clawdboard.core.Alert
import dev.clawdboard.core.AlertKind
import dev.clawdboard.core.txt
import dev.clawdboard.ui.fmtAt

object Notifier {
    private const val ALERTS = "alerts"
    private const val BACKGROUND = "background"
    const val ONGOING_ID = 1

    fun channels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(ALERTS, txt.alertChannel, NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(BACKGROUND, txt.backgroundChannel, NotificationManager.IMPORTANCE_MIN))
    }

    fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun open(context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun ongoing(context: Context): Notification = NotificationCompat.Builder(context, BACKGROUND)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(txt.backgroundTitle)
        .setContentText(txt.backgroundText)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setContentIntent(open(context))
        .build()

    fun alert(context: Context, a: Alert) {
        if (!canNotify(context)) return
        val body = if (a.level == 0) txt.alertFree else a.resetsAt?.let { txt.alertResets(fmtAt(it, System.currentTimeMillis())) }
        val n = NotificationCompat.Builder(context, ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(txt.alertTitle(a.kind == AlertKind.WEEK, a.level))
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(10 + a.kind.ordinal, n) }
    }
}
