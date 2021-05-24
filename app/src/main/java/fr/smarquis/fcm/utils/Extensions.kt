package fr.smarquis.fcm.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.RemoteMessage
import fr.smarquis.fcm.R
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.model.Payload
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

val uiHandler = Handler(Looper.getMainLooper())

fun Throwable.asString(): String = StringWriter().apply { printStackTrace(PrintWriter(this)) }.toString()

fun Context.safeStartActivity(intent: Intent?) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, e.asString(), Toast.LENGTH_LONG).show()
    }
}

fun Context.copyToClipboard(text: CharSequence?) {
    val clipboard = getSystemService<ClipboardManager>() ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
    uiHandler.post {
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}

/**
 * @return UUID.randomUUID() or a previously generated UUID, stored in SharedPreferences
 */
fun uuid(context: Context): String {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    val value = prefs.getString("uuid", null)
    if (!value.isNullOrBlank()) {
        return value
    }
    return UUID.randomUUID().toString().apply {
        prefs.edit().putString("uuid", this).apply()
    }
}

fun RemoteMessage.asMessage() = Message(
        from = from,
        to = to,
        title = notification?.title,
        body = notification?.body,
        imageUrl = notification?.imageUrl.toString(),
        data = data,
        collapseKey = collapseKey,
        messageId = messageId.toString(),
        messageType = messageType,
        sentTime = sentTime,
        ttl = ttl,
        priority = originalPriority,
        originalPriority = priority,
        payload = Payload.extract(this))