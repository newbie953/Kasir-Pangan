package com.example.data.realtime

import android.content.Context
import android.util.Log
import com.example.data.model.DailySalesSummary
import com.example.data.model.OrderWithItems
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class RealtimeReportDto(
  val reportDate: String = "",
  val totalOrders: Long = 0L,
  val totalRevenue: Double = 0.0,
  val totalCash: Double = 0.0,
  val totalQris: Double = 0.0,
  val estimatedProfit: Double = 0.0,
  val averageTicket: Double = 0.0,
  val userId: String = "pos_admin"
)

data class RealtimeTransactionDto(
  val invoiceNumber: String = "",
  val customerName: String = "",
  val orderType: String = "",
  val totalAmount: Double = 0.0,
  val finalAmount: Double = 0.0,
  val paymentMethod: String = "",
  val itemsCount: Long = 0L,
  val userId: String = "pos_admin"
)

/**
 * Service sinkronisasi laporan dan transaksi ke Firebase Realtime Database
 * sesuai permintaan: "Buatkan web laporan menggunakan realtimedatabase tanpa firestore".
 */
class RealtimeDatabaseSyncService(private val context: Context) {

  val realtimeDb: FirebaseDatabase by lazy {
    FirebaseDatabase.getInstance().apply {
      try {
        setPersistenceEnabled(true)
      } catch (_: Exception) {
        // Abaikan jika sudah diaktifkan sebelumnya
      }
    }
  }

  private val auth = Firebase.auth

  fun getUserId(): String {
    return auth.currentUser?.uid ?: "pos_admin"
  }

  suspend fun syncDailyReport(summary: DailySalesSummary, dateStr: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = getUserId()
      val reportData = mapOf(
        "userId" to uid,
        "reportDate" to dateStr,
        "totalOrders" to summary.totalOrders.toLong(),
        "totalRevenue" to summary.totalRevenue,
        "totalCash" to summary.totalCash,
        "totalQris" to summary.totalQris,
        "estimatedProfit" to summary.estimatedProfit,
        "averageTicket" to summary.averageTicket,
        "updatedAt" to ServerValue.TIMESTAMP
      )

      // Simpan ke node tabel sales_reports dan tabel laporan di Realtime Database
      val rootRef = realtimeDb.reference
      rootRef.child("sales_reports").child(dateStr).updateChildren(reportData).await()
      rootRef.child("laporan").child(dateStr).updateChildren(reportData).await()

      Log.i(TAG, "Successfully synced daily report to Firebase Realtime Database for $dateStr")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync daily report to Realtime Database: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun syncStoreClosing(summary: DailySalesSummary, dateStr: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = getUserId()
      val closingData = mapOf(
        "userId" to uid,
        "reportDate" to dateStr,
        "storeStatus" to "CLOSED",
        "totalOrders" to summary.totalOrders.toLong(),
        "totalRevenue" to summary.totalRevenue,
        "totalCash" to summary.totalCash,
        "totalQris" to summary.totalQris,
        "estimatedProfit" to summary.estimatedProfit,
        "syncedAt" to ServerValue.TIMESTAMP
      )

      val rootRef = realtimeDb.reference
      rootRef.child("laporan_tutup_toko").child(dateStr).updateChildren(closingData).await()
      rootRef.child("laporan").child(dateStr).updateChildren(closingData).await()

      Log.i(TAG, "Successfully synced Laporan Tutup Toko to Realtime Database for $dateStr")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync Laporan Tutup Toko to Realtime Database: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun syncTransaction(orderWithItems: OrderWithItems): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val order = orderWithItems.order
      val uid = getUserId()
      // Realtime Database keys tidak boleh mengandung '.', '#', '$', '[', atau ']'
      val safeKey = order.invoiceNumber.replace(".", "_").replace("/", "-")

      val transactionData = mapOf(
        "userId" to uid,
        "invoiceNumber" to order.invoiceNumber,
        "timestamp" to System.currentTimeMillis(),
        "customerName" to order.customerName,
        "orderType" to order.orderType,
        "totalAmount" to order.totalAmount,
        "finalAmount" to order.finalAmount,
        "paymentMethod" to order.paymentMethod,
        "itemsCount" to orderWithItems.items.size.toLong(),
        "itemsSummary" to orderWithItems.items.joinToString(", ") { "${it.quantity}x ${it.menuItemName}" }
      )

      val rootRef = realtimeDb.reference
      rootRef.child("transactions").child(safeKey).updateChildren(transactionData).await()
      rootRef.child("laporan_transaksi").child(safeKey).updateChildren(transactionData).await()

      Log.i(TAG, "Successfully synced transaction ${order.invoiceNumber} to Realtime Database")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync transaction to Realtime Database: ${e.message}", e)
      Result.failure(e)
    }
  }

  companion object {
    private const val TAG = "RealtimeDbSyncService"
  }
}
