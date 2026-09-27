package dev.clawdboard

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

class BackgroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.channels(this)
        val n = Notifier.ongoing(this)
        if (Build.VERSION.SDK_INT >= 34) startForeground(Notifier.ONGOING_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(Notifier.ONGOING_ID, n)
        return START_STICKY
    }

    companion object {
        fun sync(context: Context, on: Boolean) {
            val i = Intent(context, BackgroundService::class.java)
            if (on) runCatching { ContextCompat.startForegroundService(context, i) } else context.stopService(i)
        }
    }
}
