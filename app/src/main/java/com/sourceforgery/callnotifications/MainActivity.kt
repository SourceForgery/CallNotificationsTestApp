package com.sourceforgery.callnotifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.localbroadcastmanager.content.LocalBroadcastManager

// LocalBroadcastManager is deprecated but is the simplest choice for this test app.
// In a production app, prefer LiveData/StateFlow for internal event propagation.
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "call_notifications"
        const val NOTIFICATION_ID_1 = 1001
        const val NOTIFICATION_ID_2 = 1002
        const val NOTIFICATION_ID_3 = 1003
        const val NOTIFICATION_ID_4 = 1004
        const val ACTION_BUTTON_CLICKED = "com.sourceforgery.callnotifications.BUTTON_CLICKED"
        const val EXTRA_BUTTON_NAME = "button_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    private lateinit var statusText: TextView

    private val buttonClickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val buttonName = intent.getStringExtra(EXTRA_BUTTON_NAME) ?: return
            statusText.text = "'$buttonName' was clicked"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        findViewById<Button>(R.id.button1).setOnClickListener { showNotification1() }
        findViewById<Button>(R.id.button2).setOnClickListener { showNotification2() }
        findViewById<Button>(R.id.button3).setOnClickListener { showNotification3() }
        findViewById<Button>(R.id.button4).setOnClickListener { showNotification4() }
        findViewById<Button>(R.id.button5).setOnClickListener { closeAllNotifications() }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(buttonClickReceiver, IntentFilter(ACTION_BUTTON_CLICKED))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(buttonClickReceiver)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming calls"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    /** Builds a PendingIntent that fires [NotificationActionReceiver] with the given button name. */
    private fun actionPendingIntent(buttonName: String, notificationId: Int): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_BUTTON_NAME, buttonName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        // Use a unique request code per (notification, button) pair to avoid PendingIntent collisions.
        // Combine notificationId and the full button name hashCode so distinct names always differ.
        val requestCode = (notificationId.toLong() * 31L + buttonName.hashCode().toLong()).toInt()
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // -------------------------------------------------------------------------
    // Button 1 – old pre-Android-12 style, order: Answér | Décline
    // -------------------------------------------------------------------------
    private fun showNotification1() {
        val answer = getString(R.string.action_answer)
        val decline = getString(R.string.action_decline)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.caller_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .addAction(R.drawable.ic_call_notification, answer, actionPendingIntent(answer, NOTIFICATION_ID_1))
            .addAction(R.drawable.ic_call_notification, decline, actionPendingIntent(decline, NOTIFICATION_ID_1))
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_1, notification)
    }

    // -------------------------------------------------------------------------
    // Button 2 – old pre-Android-12 style, order: Décline | Answér
    // -------------------------------------------------------------------------
    private fun showNotification2() {
        val answer = getString(R.string.action_answer)
        val decline = getString(R.string.action_decline)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.caller_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .addAction(R.drawable.ic_call_notification, decline, actionPendingIntent(decline, NOTIFICATION_ID_2))
            .addAction(R.drawable.ic_call_notification, answer, actionPendingIntent(answer, NOTIFICATION_ID_2))
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_2, notification)
    }

    // -------------------------------------------------------------------------
    // Button 3 – custom RemoteViews with purple Décline and blue Answér buttons
    // -------------------------------------------------------------------------
    private fun showNotification3() {
        val answer = getString(R.string.action_answer)
        val decline = getString(R.string.action_decline)

        val remoteViews = RemoteViews(packageName, R.layout.notification_call_colored).apply {
            setTextViewText(R.id.notifCallerName, getString(R.string.caller_name))
            setTextViewText(R.id.notifCallSubtitle, getString(R.string.notification_title))
            setOnClickPendingIntent(R.id.btnDecline, actionPendingIntent(decline, NOTIFICATION_ID_3))
            setOnClickPendingIntent(R.id.btnAnswer, actionPendingIntent(answer, NOTIFICATION_ID_3))
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.caller_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_3, notification)
    }

    // -------------------------------------------------------------------------
    // Button 4 – CallStyle (Android 12 / API 31+, degrades gracefully below)
    // -------------------------------------------------------------------------
    private fun showNotification4() {
        val answer = getString(R.string.action_answer)
        val decline = getString(R.string.action_decline)

        val caller = Person.Builder()
            .setName(getString(R.string.caller_name))
            .setImportant(true)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.caller_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    caller,
                    actionPendingIntent(decline, NOTIFICATION_ID_4),
                    actionPendingIntent(answer, NOTIFICATION_ID_4)
                )
            )
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_4, notification)
    }

    // -------------------------------------------------------------------------
    // Button 5 – dismiss every notification from this app
    // -------------------------------------------------------------------------
    private fun closeAllNotifications() {
        getSystemService(NotificationManager::class.java).cancelAll()
    }
}
