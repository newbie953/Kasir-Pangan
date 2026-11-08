package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.DailySalesSummary
import com.example.data.model.OrderWithItems
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudReportDto(
  val reportDate: String = "",
  val totalOrders: Long = 0L,
  val totalRevenue: Double = 0.0,
  val totalCash: Double = 0.0,
  val totalQris: Double = 0.0,
  val estimatedProfit: Double = 0.0,
  val averageTicket: Double = 0.0,
  val userId: String = "pos_admin"
)

data class CloudTransactionDto(
  val invoiceNumber: String = "",
  val customerName: String = "",
  val orderType: String = "",
  val totalAmount: Double = 0.0,
  val finalAmount: Double = 0.0,
  val paymentMethod: String = "",
  val itemsCount: Long = 0L,
  val userId: String = "pos_admin"
)

class FirestoreSyncService(private val context: Context) {

  private val databaseId: String by lazy {
    context.getString(R.string.firestore_database_id)
  }

  val firestore: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance(databaseId)
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
        "updatedAt" to FieldValue.serverTimestamp()
      )

      // Simpan ke tabel sales_reports dan tabel laporan
      firestore.collection("sales_reports")
        .document(dateStr)
        .set(reportData, SetOptions.merge())
        .await()

      firestore.collection("laporan")
        .document(dateStr)
        .set(reportData, SetOptions.merge())
        .await()

      Log.i(TAG, "Successfully synced daily report to Firestore for $dateStr")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync daily report: ${e.message}", e)
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
        "syncedAt" to FieldValue.serverTimestamp()
      )

      firestore.collection("laporan_tutup_toko")
        .document(dateStr)
        .set(closingData, SetOptions.merge())
        .await()

      firestore.collection("laporan")
        .document(dateStr)
        .set(closingData, SetOptions.merge())
        .await()

      Log.i(TAG, "Successfully synced Laporan Tutup Toko to Firestore for $dateStr")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync Laporan Tutup Toko: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun syncTransaction(orderWithItems: OrderWithItems): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val order = orderWithItems.order
      val uid = getUserId()
      val transactionData = mapOf(
        "userId" to uid,
        "invoiceNumber" to order.invoiceNumber,
        "timestamp" to FieldValue.serverTimestamp(),
        "customerName" to order.customerName,
        "orderType" to order.orderType,
        "totalAmount" to order.totalAmount,
        "finalAmount" to order.finalAmount,
        "paymentMethod" to order.paymentMethod,
        "itemsCount" to orderWithItems.items.size.toLong()
      )

      // Simpan ke tabel transactions dan tabel laporan_transaksi
      firestore.collection("transactions")
        .document(order.invoiceNumber)
        .set(transactionData, SetOptions.merge())
        .await()

      firestore.collection("laporan_transaksi")
        .document(order.invoiceNumber)
        .set(transactionData, SetOptions.merge())
        .await()

      Log.i(TAG, "Successfully synced transaction ${order.invoiceNumber} to Firestore")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync transaction: ${e.message}", e)
      Result.failure(e)
    }
  }

  companion object {
    private const val TAG = "FirestoreSyncService"
  }
}
