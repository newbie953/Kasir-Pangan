package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class OrderWithItems(
  @Embedded val order: OrderEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "orderId"
  )
  val items: List<OrderItemEntity>
)

data class RecipeDetail(
  val recipeId: Long,
  val menuItemId: Long,
  val ingredientId: Long,
  val ingredientName: String,
  val amountRequired: Double,
  val unit: String,
  val currentStock: Double
)

data class MenuItemWithStock(
  val menuItem: MenuItemEntity,
  val availablePortions: Int, // Calculated based on lowest ingredient stock in recipe
  val isOutOfStock: Boolean
)

data class DailySalesSummary(
  val totalOrders: Int = 0,
  val totalRevenue: Double = 0.0,
  val totalCash: Double = 0.0,
  val totalQris: Double = 0.0,
  val estimatedProfit: Double = 0.0,
  val averageTicket: Double = 0.0
)

data class TopProductSummary(
  val menuItemId: Long,
  val menuItemName: String,
  val totalSold: Int,
  val totalRevenue: Double
)
