package com.example.data.model

data class WebOrderItem(
  val menuItemId: Long,
  val menuItemName: String,
  val unitPrice: Double,
  val quantity: Int,
  val notes: String = ""
) {
  val subtotal: Double get() = unitPrice * quantity
}

data class WebOrder(
  val id: String, // misal "WEB-1024"
  val customerName: String,
  val tableOrAddress: String,
  val orderType: String = "Dine In", // "Dine In" atau "Take Away"
  val customerPhone: String = "",
  val notes: String = "",
  val items: List<WebOrderItem>,
  val totalAmount: Double,
  val paymentPreference: String = "QRIS", // "QRIS" atau "TUNAI_KASIR"
  val timestamp: Long = System.currentTimeMillis(),
  val status: String = "MENUNGGU" // "MENUNGGU", "DITERIMA", "DITOLAK"
)
