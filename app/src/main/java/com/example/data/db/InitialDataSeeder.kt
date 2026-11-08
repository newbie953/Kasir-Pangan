package com.example.data.db

import com.example.data.model.IngredientEntity
import com.example.data.model.MenuItemEntity
import com.example.data.model.MenuItemRecipeEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.StockLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InitialDataSeeder {

  suspend fun seedIfEmpty(database: AppDatabase) = withContext(Dispatchers.IO) {
    val existingCount = database.orderDao().getOrdersCount()
    val menuDao = database.menuDao()
    val ingredientDao = database.ingredientDao()
    val orderDao = database.orderDao()
    val stockLogDao = database.stockLogDao()

    // Check if ingredients already exist
    val existingIngredients = ingredientDao.getLowStockIngredients()
    if (existingCount > 0 || existingIngredients.isNotEmpty()) {
      return@withContext
    }

    // 1. Seed Ingredients
    val ingredients = listOf(
      IngredientEntity(id = 1, name = "Beras Premium", currentStock = 4500.0, unit = "gram", minThreshold = 1000.0, costPerUnit = 14.0),
      IngredientEntity(id = 2, name = "Daging Ayam Fillet", currentStock = 2800.0, unit = "gram", minThreshold = 800.0, costPerUnit = 42.0),
      IngredientEntity(id = 3, name = "Daging Sapi Pilihan", currentStock = 1800.0, unit = "gram", minThreshold = 600.0, costPerUnit = 110.0),
      IngredientEntity(id = 4, name = "Telur Ayam", currentStock = 38.0, unit = "butir", minThreshold = 12.0, costPerUnit = 2000.0),
      IngredientEntity(id = 5, name = "Minyak Goreng", currentStock = 3200.0, unit = "ml", minThreshold = 1000.0, costPerUnit = 16.0),
      IngredientEntity(id = 6, name = "Biji Kopi Robusta", currentStock = 950.0, unit = "gram", minThreshold = 250.0, costPerUnit = 130.0),
      IngredientEntity(id = 7, name = "Susu Segar UHT", currentStock = 900.0, unit = "ml", minThreshold = 1000.0, costPerUnit = 22.0), // Menipis for realistic low stock warning!
      IngredientEntity(id = 8, name = "Gula Aren Cair", currentStock = 1200.0, unit = "ml", minThreshold = 350.0, costPerUnit = 35.0),
      IngredientEntity(id = 9, name = "Daun Teh Melati", currentStock = 450.0, unit = "gram", minThreshold = 150.0, costPerUnit = 30.0),
      IngredientEntity(id = 10, name = "Gula Pasir", currentStock = 2200.0, unit = "gram", minThreshold = 500.0, costPerUnit = 16.0),
      IngredientEntity(id = 11, name = "Buah Alpukat Mentega", currentStock = 2200.0, unit = "gram", minThreshold = 800.0, costPerUnit = 32.0),
      IngredientEntity(id = 12, name = "Jeruk Peras Segar", currentStock = 2600.0, unit = "gram", minThreshold = 900.0, costPerUnit = 22.0),
      IngredientEntity(id = 13, name = "Pisang Raja", currentStock = 26.0, unit = "buah", minThreshold = 10.0, costPerUnit = 1500.0),
      IngredientEntity(id = 14, name = "Keju Cheddar", currentStock = 750.0, unit = "gram", minThreshold = 200.0, costPerUnit = 90.0),
      IngredientEntity(id = 15, name = "Tahu Putih", currentStock = 35.0, unit = "potong", minThreshold = 15.0, costPerUnit = 800.0),
      IngredientEntity(id = 16, name = "Kentang Beku", currentStock = 2400.0, unit = "gram", minThreshold = 750.0, costPerUnit = 30.0),
      IngredientEntity(id = 17, name = "Bumbu Rempah Racik", currentStock = 1800.0, unit = "gram", minThreshold = 450.0, costPerUnit = 40.0),
      IngredientEntity(id = 18, name = "Cup & Sedotan Takeaway", currentStock = 65.0, unit = "pcs", minThreshold = 25.0, costPerUnit = 550.0)
    )
    ingredientDao.insertIngredients(ingredients)

    // 2. Seed Menu Items
    val menuItems = listOf(
      MenuItemEntity(id = 1, name = "Nasi Goreng Spesial", category = "Makanan", price = 25000.0, costEst = 11500.0, iconKey = "nasi_goreng", description = "Nasi goreng bumbu racik istimewa dengan telur, ayam suwir & kerupuk"),
      MenuItemEntity(id = 2, name = "Ayam Bakar Madu", category = "Makanan", price = 30000.0, costEst = 14000.0, iconKey = "ayam_bakar", description = "Ayam bakar kecap madu gurih manis dengan sambal bajak"),
      MenuItemEntity(id = 3, name = "Mie Goreng Jawa", category = "Makanan", price = 24000.0, costEst = 10000.0, iconKey = "mie", description = "Mie telur kenyal dimasak rempah khas Jawa dengan sayuran & ayam"),
      MenuItemEntity(id = 4, name = "Soto Ayam Lamongan", category = "Makanan", price = 22000.0, costEst = 9500.0, iconKey = "soto", description = "Kuah kuning gurih segar dengan koya renyah, sohun & telur"),
      MenuItemEntity(id = 5, name = "Rendang Daging Sapi", category = "Makanan", price = 35000.0, costEst = 18500.0, iconKey = "rendang", description = "Daging sapi empuk kaya santan dan rempah Minang otentik"),
      MenuItemEntity(id = 6, name = "Kopi Susu Gula Aren", category = "Minuman", price = 18000.0, costEst = 7000.0, iconKey = "kopi", description = "Espresso robusta mantap dipadu susu segar & gula aren murni"),
      MenuItemEntity(id = 7, name = "Es Teh Manis Melati", category = "Minuman", price = 6000.0, costEst = 2200.0, iconKey = "es_teh", description = "Seduhan teh melati wangi dingin menyegarkan"),
      MenuItemEntity(id = 8, name = "Jus Alpukat Kocok", category = "Minuman", price = 16000.0, costEst = 6500.0, iconKey = "jus", description = "Alpukat mentega kental dengan susu kental cokelat"),
      MenuItemEntity(id = 9, name = "Es Jeruk Peras Segar", category = "Minuman", price = 10000.0, costEst = 3800.0, iconKey = "es_jeruk", description = "Perasan jeruk segar asli tanpa pemanis buatan"),
      MenuItemEntity(id = 10, name = "Pisang Goreng Keju", category = "Camilan", price = 15000.0, costEst = 5500.0, iconKey = "pisang", description = "Pisang raja manis lembut berbalut keju cheddar melimpah"),
      MenuItemEntity(id = 11, name = "Tahu Crispy Gurih", category = "Camilan", price = 12000.0, costEst = 4200.0, iconKey = "tahu", description = "Tahu potong renyah berbumbu pedas asin lezat"),
      MenuItemEntity(id = 12, name = "Kentang Goreng Stik", category = "Camilan", price = 14000.0, costEst = 5200.0, iconKey = "kentang", description = "Kentang renyah keemasan dengan saus sambal & mayones")
    )
    menuDao.insertMenuItems(menuItems)

    // 3. Seed Recipes (Bahan Baku per Porsi Menu)
    val recipes = listOf(
      // Nasi Goreng Spesial (id: 1)
      MenuItemRecipeEntity(menuItemId = 1, ingredientId = 1, amountRequired = 150.0), // Beras 150g
      MenuItemRecipeEntity(menuItemId = 1, ingredientId = 4, amountRequired = 1.0),   // Telur 1 butir
      MenuItemRecipeEntity(menuItemId = 1, ingredientId = 5, amountRequired = 20.0),  // Minyak 20ml
      MenuItemRecipeEntity(menuItemId = 1, ingredientId = 17, amountRequired = 25.0), // Bumbu 25g
      MenuItemRecipeEntity(menuItemId = 1, ingredientId = 2, amountRequired = 40.0),  // Ayam 40g

      // Ayam Bakar Madu (id: 2)
      MenuItemRecipeEntity(menuItemId = 2, ingredientId = 2, amountRequired = 200.0), // Ayam 200g
      MenuItemRecipeEntity(menuItemId = 2, ingredientId = 5, amountRequired = 15.0),  // Minyak 15ml
      MenuItemRecipeEntity(menuItemId = 2, ingredientId = 17, amountRequired = 35.0), // Bumbu 35g

      // Mie Goreng Jawa (id: 3)
      MenuItemRecipeEntity(menuItemId = 3, ingredientId = 4, amountRequired = 1.0),   // Telur 1 butir
      MenuItemRecipeEntity(menuItemId = 3, ingredientId = 5, amountRequired = 20.0),  // Minyak 20ml
      MenuItemRecipeEntity(menuItemId = 3, ingredientId = 2, amountRequired = 40.0),  // Ayam 40g
      MenuItemRecipeEntity(menuItemId = 3, ingredientId = 17, amountRequired = 25.0), // Bumbu 25g

      // Soto Ayam Lamongan (id: 4)
      MenuItemRecipeEntity(menuItemId = 4, ingredientId = 2, amountRequired = 120.0), // Ayam 120g
      MenuItemRecipeEntity(menuItemId = 4, ingredientId = 4, amountRequired = 1.0),   // Telur 1 butir
      MenuItemRecipeEntity(menuItemId = 4, ingredientId = 17, amountRequired = 30.0), // Bumbu 30g

      // Rendang Daging Sapi (id: 5)
      MenuItemRecipeEntity(menuItemId = 5, ingredientId = 3, amountRequired = 150.0), // Sapi 150g
      MenuItemRecipeEntity(menuItemId = 5, ingredientId = 5, amountRequired = 20.0),  // Minyak 20ml
      MenuItemRecipeEntity(menuItemId = 5, ingredientId = 17, amountRequired = 40.0), // Bumbu 40g

      // Kopi Susu Gula Aren (id: 6)
      MenuItemRecipeEntity(menuItemId = 6, ingredientId = 6, amountRequired = 18.0),  // Kopi 18g
      MenuItemRecipeEntity(menuItemId = 6, ingredientId = 7, amountRequired = 120.0), // Susu 120ml
      MenuItemRecipeEntity(menuItemId = 6, ingredientId = 8, amountRequired = 25.0),  // Gula aren 25ml
      MenuItemRecipeEntity(menuItemId = 6, ingredientId = 18, amountRequired = 1.0),  // Cup 1 pcs

      // Es Teh Manis Melati (id: 7)
      MenuItemRecipeEntity(menuItemId = 7, ingredientId = 9, amountRequired = 10.0),  // Teh 10g
      MenuItemRecipeEntity(menuItemId = 7, ingredientId = 10, amountRequired = 20.0), // Gula 20g
      MenuItemRecipeEntity(menuItemId = 7, ingredientId = 18, amountRequired = 1.0),  // Cup 1 pcs

      // Jus Alpukat Kocok (id: 8)
      MenuItemRecipeEntity(menuItemId = 8, ingredientId = 11, amountRequired = 150.0),// Alpukat 150g
      MenuItemRecipeEntity(menuItemId = 8, ingredientId = 7, amountRequired = 50.0),  // Susu 50ml
      MenuItemRecipeEntity(menuItemId = 8, ingredientId = 10, amountRequired = 20.0), // Gula 20g
      MenuItemRecipeEntity(menuItemId = 8, ingredientId = 18, amountRequired = 1.0),  // Cup 1 pcs

      // Es Jeruk Peras Segar (id: 9)
      MenuItemRecipeEntity(menuItemId = 9, ingredientId = 12, amountRequired = 160.0),// Jeruk 160g
      MenuItemRecipeEntity(menuItemId = 9, ingredientId = 10, amountRequired = 20.0), // Gula 20g
      MenuItemRecipeEntity(menuItemId = 9, ingredientId = 18, amountRequired = 1.0),  // Cup 1 pcs

      // Pisang Goreng Keju (id: 10)
      MenuItemRecipeEntity(menuItemId = 10, ingredientId = 13, amountRequired = 2.0), // Pisang 2 buah
      MenuItemRecipeEntity(menuItemId = 10, ingredientId = 5, amountRequired = 25.0), // Minyak 25ml
      MenuItemRecipeEntity(menuItemId = 10, ingredientId = 14, amountRequired = 25.0),// Keju 25g

      // Tahu Crispy Gurih (id: 11)
      MenuItemRecipeEntity(menuItemId = 11, ingredientId = 15, amountRequired = 5.0), // Tahu 5 potong
      MenuItemRecipeEntity(menuItemId = 11, ingredientId = 5, amountRequired = 30.0), // Minyak 30ml
      MenuItemRecipeEntity(menuItemId = 11, ingredientId = 17, amountRequired = 15.0),// Bumbu 15g

      // Kentang Goreng Stik (id: 12)
      MenuItemRecipeEntity(menuItemId = 12, ingredientId = 16, amountRequired = 150.0),// Kentang 150g
      MenuItemRecipeEntity(menuItemId = 12, ingredientId = 5, amountRequired = 25.0),  // Minyak 25ml
      MenuItemRecipeEntity(menuItemId = 12, ingredientId = 17, amountRequired = 10.0)  // Bumbu 10g
    )
    menuDao.insertRecipes(recipes)

    // 4. Seed a few realistic historical sales from today and yesterday
    val now = System.currentTimeMillis()
    val hour = 3600_000L
    val dateFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val todayCode = dateFmt.format(Date(now))

    val sampleOrders = listOf(
      OrderEntity(
        id = 1,
        invoiceNumber = "INV-$todayCode-001",
        timestamp = now - 3 * hour,
        totalAmount = 56000.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        finalAmount = 56000.0,
        paymentMethod = "TUNAI",
        cashReceived = 60000.0,
        cashChange = 4000.0,
        customerName = "Pak Budi",
        orderType = "Dine In"
      ),
      OrderEntity(
        id = 2,
        invoiceNumber = "INV-$todayCode-002",
        timestamp = now - 2 * hour,
        totalAmount = 69000.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        finalAmount = 69000.0,
        paymentMethod = "QRIS",
        qrisReference = "QRIS-ID8921873198",
        customerName = "Ibu Siti",
        orderType = "Take Away"
      ),
      OrderEntity(
        id = 3,
        invoiceNumber = "INV-$todayCode-003",
        timestamp = now - 1 * hour,
        totalAmount = 40000.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        finalAmount = 40000.0,
        paymentMethod = "QRIS",
        qrisReference = "QRIS-ID9821739812",
        customerName = "Rian",
        orderType = "Dine In"
      )
    )

    for (order in sampleOrders) {
      val orderId = orderDao.insertOrder(order)
      when (order.id.toInt()) {
        1 -> {
          orderDao.insertOrderItems(
            listOf(
              OrderItemEntity(orderId = orderId, menuItemId = 1, menuItemName = "Nasi Goreng Spesial", unitPrice = 25000.0, quantity = 2, subtotal = 50000.0, notes = "Satu pedas, satu sedang"),
              OrderItemEntity(orderId = orderId, menuItemId = 7, menuItemName = "Es Teh Manis Melati", unitPrice = 6000.0, quantity = 1, subtotal = 6000.0)
            )
          )
        }
        2 -> {
          orderDao.insertOrderItems(
            listOf(
              OrderItemEntity(orderId = orderId, menuItemId = 2, menuItemName = "Ayam Bakar Madu", unitPrice = 30000.0, quantity = 1, subtotal = 30000.0),
              OrderItemEntity(orderId = orderId, menuItemId = 6, menuItemName = "Kopi Susu Gula Aren", unitPrice = 18000.0, quantity = 1, subtotal = 18000.0, notes = "Less sugar"),
              OrderItemEntity(orderId = orderId, menuItemId = 11, menuItemName = "Tahu Crispy Gurih", unitPrice = 12000.0, quantity = 1, subtotal = 12000.0),
              OrderItemEntity(orderId = orderId, menuItemId = 9, menuItemName = "Es Jeruk Peras Segar", unitPrice = 10000.0, quantity = 1, subtotal = 10000.0)
            )
          )
        }
        3 -> {
          orderDao.insertOrderItems(
            listOf(
              OrderItemEntity(orderId = orderId, menuItemId = 1, menuItemName = "Nasi Goreng Spesial", unitPrice = 25000.0, quantity = 1, subtotal = 25000.0),
              OrderItemEntity(orderId = orderId, menuItemId = 10, menuItemName = "Pisang Goreng Keju", unitPrice = 15000.0, quantity = 1, subtotal = 15000.0)
            )
          )
        }
      }
    }

    // 5. Initial stock logs
    stockLogDao.insertLog(
      StockLogEntity(
        ingredientId = 7,
        ingredientName = "Susu Segar UHT",
        changeAmount = -120.0,
        remainingStock = 900.0,
        unit = "ml",
        reason = "Penjualan INV-$todayCode-002",
        timestamp = now - 2 * hour
      )
    )
    stockLogDao.insertLog(
      StockLogEntity(
        ingredientId = 1,
        ingredientName = "Beras Premium",
        changeAmount = -450.0,
        remainingStock = 4500.0,
        unit = "gram",
        reason = "Penjualan INV-$todayCode-001 & 003",
        timestamp = now - 1 * hour
      )
    )
  }
}
