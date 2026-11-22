package com.example.notification

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
import com.example.MainActivity
import com.example.R
import com.example.data.model.OrderEntity

object OrderNotificationHelper {
    const val CHANNEL_ID = "warung_nurbaiti_orders_channel"
    const val EXTRA_NAVIGATE_TO_ORDERS = "extra_navigate_to_orders"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val channelEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!channelEnabled) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNewOrderNotification(context: Context, order: OrderEntity): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_ORDERS, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            order.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = "Pemesan: ${order.customerName}\n" +
            "No. WA: ${order.whatsappNumber}\n" +
            "Pembayaran: ${order.paymentMethod}\n" +
            "Bahan: ${order.orderedIngredients}" +
            if (order.additionalNotes.isNotBlank()) "\nCatatan: ${order.additionalNotes}" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pesanan Baru • ${order.orderCode}")
            .setContentText("${order.customerName} (${order.paymentMethod}) - ${order.whatsappNumber}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(
                (order.id.takeIf { it > 0 } ?: System.currentTimeMillis()).toInt(),
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showTestNotification(context: Context): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_ORDERS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Notifikasi Warung Nurbaiti Aktif")
            .setContentText("Setiap pesanan baru pelanggan akan langsung masuk ke HP Anda.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(9999, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
