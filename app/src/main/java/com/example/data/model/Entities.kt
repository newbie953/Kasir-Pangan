package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val category: String, // "Makanan", "Minuman", "Camilan"
  val price: Double,
  val costEst: Double, // Perkiraan HPP untuk hitung laba
  val description: String = "",
  val iconKey: String = "food", // "nasi_goreng", "ayam_bakar", "mie", "soto", "rendang", "kopi", "es_teh", "jus", "camilan"
  val isActive: Boolean = true
)

@Entity(tableName = "ingredients")
data class IngredientEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val currentStock: Double, // misal 5000.0 (gram) atau 50.0 (butir)
  val unit: String, // "gram", "ml", "butir", "pcs", "porsi"
  val minThreshold: Double, // Ambang batas notifikasi stok menipis
  val costPerUnit: Double = 0.0,
  val lastRestockedMillis: Long = System.currentTimeMillis()
) {
  val isLowStock: Boolean
    get() = currentStock <= minThreshold

  val isCritical: Boolean
    get() = currentStock <= (minThreshold * 0.3)
}

@Entity(
  tableName = "menu_recipes",
  foreignKeys = [
    ForeignKey(
      entity = MenuItemEntity::class,
      parentColumns = ["id"],
      childColumns = ["menuItemId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = IngredientEntity::class,
      parentColumns = ["id"],
      childColumns = ["ingredientId"],
      onDelete = ForeignKey.RESTRICT
    )
  ],
  indices = [Index(value = ["menuItemId"]), Index(value = ["ingredientId"])]
)
data class MenuItemRecipeEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val menuItemId: Long,
  val ingredientId: Long,
  val amountRequired: Double // Jumlah bahan yang dibutuhkan per 1 porsi menu
)

@Entity(tableName = "orders")
data class OrderEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val invoiceNumber: String, // misal "INV-20261028-001"
  val timestamp: Long = System.currentTimeMillis(),
  val totalAmount: Double,
  val discountAmount: Double = 0.0,
  val taxAmount: Double = 0.0,
  val finalAmount: Double,
  val paymentMethod: String, // "TUNAI" atau "QRIS"
  val cashReceived: Double = 0.0,
  val cashChange: Double = 0.0,
  val qrisReference: String = "",
  val customerName: String = "Pelanggan",
  val orderType: String = "Dine In", // "Dine In" atau "Take Away"
  val cashierName: String = "Kasir 1",
  val status: String = "SELESAI" // "SELESAI", "BATAL"
)

@Entity(
  tableName = "order_items",
  foreignKeys = [
    ForeignKey(
      entity = OrderEntity::class,
      parentColumns = ["id"],
      childColumns = ["orderId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val orderId: Long,
  val menuItemId: Long,
  val menuItemName: String,
  val unitPrice: Double,
  val quantity: Int,
  val subtotal: Double,
  val notes: String = ""
)

@Entity(tableName = "stock_logs")
data class StockLogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val ingredientId: Long,
  val ingredientName: String,
  val changeAmount: Double, // Positif untuk restock (+1000), negatif untuk pengurangan (-150)
  val remainingStock: Double,
  val unit: String,
  val reason: String, // "Penjualan INV-...", "Restock Manual", dll
  val timestamp: Long = System.currentTimeMillis()
)
