package com.sourceforgery.callnotifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val buttonName = intent.getStringExtra(MainActivity.EXTRA_BUTTON_NAME) ?: return
        val notificationId = intent.getIntExtra(MainActivity.EXTRA_NOTIFICATION_ID, 0)

        // Notification 4 is owned by a foreground service; stop the service so that
        // Android removes its notification automatically.  For all other notifications
        // a direct cancel() is sufficient.
        if (notificationId == MainActivity.NOTIFICATION_ID_4) {
            context.stopService(Intent(context, CallService::class.java))
        } else {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }

        // Forward the click info to the main activity via a local broadcast
        val updateIntent = Intent(MainActivity.ACTION_BUTTON_CLICKED).apply {
            putExtra(MainActivity.EXTRA_BUTTON_NAME, buttonName)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
    }
}
