package dev.clawdboard

import android.app.Application
import android.content.Context
import dev.clawdboard.core.AlertKind
import dev.clawdboard.core.Repository
import dev.clawdboard.core.nextAlert

class ClawdApp : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repository(this)
        repo.startPanel()
        Notifier.channels(this)
        val marks = getSharedPreferences("alerts", MODE_PRIVATE)
        repo.onPush = { snap ->
            if (repo.settings.value.alerts) {
                listOf(AlertKind.SESSION to snap.fiveHour, AlertKind.WEEK to snap.sevenDay).forEach { (kind, w) ->
                    val (alert, level) = nextAlert(kind, w, marks.getInt(kind.name, 0))
                    marks.edit().putInt(kind.name, level).apply()
                    alert?.let { Notifier.alert(this, it) }
                }
            }
        }
    }
}

val Context.repo: Repository get() = (applicationContext as ClawdApp).repo
