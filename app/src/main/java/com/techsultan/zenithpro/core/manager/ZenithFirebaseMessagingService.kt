package com.techsultan.zenithpro.core.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.techsultan.zenithpro.MainActivity
import com.techsultan.zenithpro.R

class ZenithFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title  = data["title"]  ?: "Payment Received"
        val body   = data["body"]   ?: "A transfer has been received"
        val amount = data["amount"] ?: ""
        val sender = data["sender"] ?: ""
        val bank   = data["bank"]   ?: ""

        showNotification(title, body, amount, sender, bank)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        ZenithFcmTokenManager.saveTokenLocally(applicationContext, token)
    }

    private fun showNotification(
        title: String,
        body: String,
        amount: String,
        sender: String,
        bank: String,
    ) {
        val channelId = "nomba_payments"
        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Payment Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nomba transfer notifications"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap notification → open Nomba Transfers screen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "nomba_transfers")
            putExtra("amount", amount)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.inventory_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "₦$amount from $sender ($bank)"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}