package fr.tchyze.vigie.watch

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.tchyze.vigie.MainActivity
import fr.tchyze.vigie.R
import fr.tchyze.vigie.domain.WatchEvent

/** Notifications des nouveautés : une seule notification, résumée s'il y a plusieurs faits. */
class Notifier(private val context: Context) {

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Nouveautés suivies", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Nouvelles versions, avis de sécurité et fins de support des technologies suivies"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(events: List<WatchEvent>) {
        if (events.isEmpty() || !canNotify()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (events.size == 1) {
            val e = events.single()
            builder.setContentTitle(e.title)
                .setContentText(e.text.lineSequence().first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(e.text))
        } else {
            val style = NotificationCompat.InboxStyle()
            events.take(MAX_LINES).forEach { style.addLine(it.title) }
            if (events.size > MAX_LINES) style.setSummaryText("et ${events.size - MAX_LINES} autres")
            builder.setContentTitle("${events.size} nouveautés")
                .setContentText(events.joinToString(" · ") { it.title })
                .setStyle(style)
                .setNumber(events.size)
        }
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission retirée entre la vérification et l'envoi : rien à faire.
        }
    }

    private companion object {
        const val CHANNEL_ID = "nouveautes"
        const val NOTIFICATION_ID = 1
        const val MAX_LINES = 6
    }
}
