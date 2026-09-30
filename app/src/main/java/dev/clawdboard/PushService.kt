package dev.clawdboard

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.concurrent.thread

class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        thread { repo.registerPush(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val blob = message.data["blob"] ?: return
        repo.receiveSealed(blob, message.data["at"]?.toLongOrNull() ?: 0L)
    }

    companion object {
        fun sync(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) return
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> thread { context.repo.registerPush(token) } }
            }
        }
    }
}
