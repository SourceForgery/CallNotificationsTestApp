package com.sourceforgery.callnotifications

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/**
 * Foreground service that hosts the CallStyle (Button 4) notification.
 *
 * Android 12+ (API 31+) requires a CallStyle notification to be associated with a
 * foreground service, a user-initiated job, or a fullScreenIntent.  Running as a
 * foreground service with foregroundServiceType="phoneCall" is the canonical approach
 * for call apps: it keeps the notification alive while the call is in progress and
 * allows the system to treat the process with call-priority scheduling.
 */
class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val answer = getString(R.string.action_answer)
        val decline = getString(R.string.action_decline)

        val caller = Person.Builder()
            .setName(getString(R.string.caller_name))
            .setImportant(true)
            .build()

        val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.caller_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    caller,
                    actionPendingIntent(decline),
                    actionPendingIntent(answer)
                )
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MainActivity.NOTIFICATION_ID_4,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(MainActivity.NOTIFICATION_ID_4, notification)
        }

        return START_NOT_STICKY
    }

    /** Builds a PendingIntent that fires [NotificationActionReceiver] for notification 4. */
    private fun actionPendingIntent(buttonName: String): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            putExtra(MainActivity.EXTRA_BUTTON_NAME, buttonName)
            putExtra(MainActivity.EXTRA_NOTIFICATION_ID, MainActivity.NOTIFICATION_ID_4)
        }
        val requestCode =
            (MainActivity.NOTIFICATION_ID_4.toLong() * 31L + buttonName.hashCode().toLong()).toInt()
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
