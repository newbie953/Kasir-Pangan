package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.IngredientEntity
import com.example.data.model.MenuItemEntity
import com.example.data.model.MenuItemRecipeEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.data.model.StockLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
  @Query("SELECT * FROM menu_items WHERE isActive = 1 ORDER BY category ASC, name ASC")
  fun getAllMenuItems(): Flow<List<MenuItemEntity>>

  @Query("SELECT * FROM menu_items WHERE id = :id")
  suspend fun getMenuItemById(id: Long): MenuItemEntity?

  @Query("SELECT * FROM menu_recipes")
  fun getAllRecipes(): Flow<List<MenuItemRecipeEntity>>

  @Query("SELECT * FROM menu_recipes WHERE menuItemId = :menuItemId")
  suspend fun getRecipesForMenu(menuItemId: Long): List<MenuItemRecipeEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMenuItems(items: List<MenuItemEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMenuItem(item: MenuItemEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecipes(recipes: List<MenuItemRecipeEntity>)

  @Update
  suspend fun updateMenuItem(item: MenuItemEntity)
}

@Dao
interface IngredientDao {
  @Query("SELECT * FROM ingredients ORDER BY name ASC")
  fun getAllIngredients(): Flow<List<IngredientEntity>>

  @Query("SELECT * FROM ingredients WHERE currentStock <= minThreshold ORDER BY (currentStock / minThreshold) ASC")
  fun getLowStockIngredientsFlow(): Flow<List<IngredientEntity>>

  @Query("SELECT * FROM ingredients WHERE currentStock <= minThreshold")
  suspend fun getLowStockIngredients(): List<IngredientEntity>

  @Query("SELECT * FROM ingredients WHERE id = :id")
  suspend fun getIngredientById(id: Long): IngredientEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertIngredients(ingredients: List<IngredientEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertIngredient(ingredient: IngredientEntity): Long

  @Update
  suspend fun updateIngredient(ingredient: IngredientEntity)

  @Query("UPDATE ingredients SET currentStock = :newStock, lastRestockedMillis = :timestamp WHERE id = :id")
  suspend fun updateStock(id: Long, newStock: Double, timestamp: Long)
}

@Dao
interface OrderDao {
  @Transaction
  @Query("SELECT * FROM orders ORDER BY timestamp DESC")
  fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

  @Transaction
  @Query("SELECT * FROM orders WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
  fun getOrdersBetween(startTime: Long, endTime: Long): Flow<List<OrderWithItems>>

  @Transaction
  @Query("SELECT * FROM orders WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
  suspend fun getOrdersBetweenOnce(startTime: Long, endTime: Long): List<OrderWithItems>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrder(order: OrderEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long>

  @Query("SELECT COUNT(*) FROM orders")
  suspend fun getOrdersCount(): Int
}

@Dao
interface StockLogDao {
  @Query("SELECT * FROM stock_logs ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentLogs(limit: Int = 100): Flow<List<StockLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: StockLogEntity): Long
}
