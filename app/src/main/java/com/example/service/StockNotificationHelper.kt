package com.example.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.IngredientEntity

object StockNotificationHelper {

  private const val CHANNEL_ID = "low_stock_alerts_channel"
  private const val CHANNEL_NAME = "Peringatan Stok Bahan Baku"
  private const val CHANNEL_DESC = "Notifikasi otomatis saat stok bahan baku makanan menipis atau habis"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
      }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  @SuppressLint("MissingPermission")
  fun showLowStockAlert(
    context: Context,
    ingredient: IngredientEntity
  ) {
    try {
      createNotificationChannel(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("NAVIGATE_TO", "STOK_BAHAN")
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        ingredient.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val isCritical = ingredient.currentStock <= (ingredient.minThreshold * 0.3)
      val title = if (isCritical) {
        "🚨 STOK KRITIS: ${ingredient.name}"
      } else {
        "⚠️ Stok Menipis: ${ingredient.name}"
      }

      val message = "Tersisa hanya ${ingredient.currentStock.toCleanString()} ${ingredient.unit} (Batas min: ${ingredient.minThreshold.toCleanString()} ${ingredient.unit}). Segera restock!"

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      notificationManager.notify(ingredient.id.toInt(), builder.build())
    } catch (_: Exception) {
      // Graceful fallback if notification permission is not granted
    }
  }

  private fun Double.toCleanString(): String {
    return if (this % 1.0 == 0.0) this.toLong().toString() else String.format("%.1f", this)
  }
}
