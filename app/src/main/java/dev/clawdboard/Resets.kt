package dev.clawdboard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.clawdboard.core.AgPool
import dev.clawdboard.core.Alert
import dev.clawdboard.core.AlertKind
import dev.clawdboard.core.enumOr

object Resets {
    private const val MIN_LEVEL = 80
    private const val GRACE_MS = 30_000L

    private class Slot(val key: String, val kind: AlertKind, val pool: AgPool?)

    private val slots = AlertKind.entries.map { Slot(it.name, it, null) } +
        AgPool.entries.flatMap { p -> AlertKind.entries.map { k -> Slot("ag_${p.name}_${k.name}", k, p) } }

    private fun pending(context: Context) = context.getSharedPreferences("resets", Context.MODE_PRIVATE)

    fun note(context: Context, key: String, level: Int, at: Long?, freed: Boolean) {
        val sp = pending(context)
        when {
            freed -> sp.edit().remove(key).apply()
            level >= MIN_LEVEL && at != null && sp.getLong(key, 0L) != at -> sp.edit().putLong(key, at).apply()
        }
    }

    fun sync(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val sp = pending(context)
        val now = System.currentTimeMillis()
        slots.forEach { s ->
            val at = sp.getLong(s.key, 0L).takeIf { it > 0 }
            val intent = Intent(context, ResetReceiver::class.java)
                .setAction("dev.clawdboard.RESET.${s.key}")
                .putExtra("mark", s.key).putExtra("kind", s.kind.name).putExtra("pool", s.pool?.name).putExtra("at", at ?: 0L)
            val pi = PendingIntent.getBroadcast(context, s.key.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            if (at != null) runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, maxOf(at + GRACE_MS, now + 5_000L), pi) }
            else am.cancel(pi)
        }
    }

    fun fired(context: Context, key: String, at: Long): Boolean {
        val sp = pending(context)
        if (at <= 0 || sp.getLong(key, 0L) != at) return false
        sp.edit().remove(key).apply()
        return true
    }
}

class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra("mark") ?: return
        if (!Resets.fired(context, key, intent.getLongExtra("at", 0L))) return
        context.getSharedPreferences("alerts", Context.MODE_PRIVATE).edit().putInt(key, 0).apply()
        val kind = enumOr(intent.getStringExtra("kind"), AlertKind.WEEK)
        val pool = intent.getStringExtra("pool")?.let { p -> AgPool.entries.firstOrNull { it.name == p } }
        val prefs = context.repo.settings.value
        if (!prefs.alerts) return
        val alert = Alert(kind, 0, null)
        if (pool == null) Notifier.alert(context, alert)
        else if (prefs.showAg) Notifier.agAlert(context, alert, pool)
    }
}
