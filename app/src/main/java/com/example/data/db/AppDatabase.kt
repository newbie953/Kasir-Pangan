package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.IngredientEntity
import com.example.data.model.MenuItemEntity
import com.example.data.model.MenuItemRecipeEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.StockLogEntity

@Database(
  entities = [
    MenuItemEntity::class,
    IngredientEntity::class,
    MenuItemRecipeEntity::class,
    OrderEntity::class,
    OrderItemEntity::class,
    StockLogEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun menuDao(): MenuDao
  abstract fun ingredientDao(): IngredientDao
  abstract fun orderDao(): OrderDao
  abstract fun stockLogDao(): StockLogDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "dapur_kasir.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
