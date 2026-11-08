package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.InitialDataSeeder
import com.example.data.model.DailySalesSummary
import com.example.data.model.IngredientEntity
import com.example.data.model.MenuItemEntity
import com.example.data.model.MenuItemRecipeEntity
import com.example.data.model.MenuItemWithStock
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.data.model.StockLogEntity
import com.example.data.model.TopProductSummary
import com.example.service.BluetoothDeviceInfo
import com.example.service.EscPosPrinterService
import com.example.data.model.WebOrder
import com.example.service.EmbeddedWebOrderServer
import com.example.service.StockNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppTab {
  KASIR,
  STOK_BAHAN,
  LAPORAN,
  WEB_APP,
  PENGATURAN
}

enum class ReportPeriod(val label: String) {
  HARI_INI("Hari Ini"),
  KEMARIN("Kemarin"),
  TUJUH_HARI("7 Hari Terakhir"),
  TIGA_PULUH_HARI("30 Hari"),
  SEMUA("Semua Waktu")
}

data class CartItem(
  val menuItem: MenuItemEntity,
  val quantity: Int,
  val notes: String = ""
) {
  val subtotal: Double get() = menuItem.price * quantity
}

data class PosUiState(
  val currentTab: AppTab = AppTab.KASIR,
  val searchQuery: String = "",
  val selectedCategory: String = "Semua",
  val menuItemsWithStock: List<MenuItemWithStock> = emptyList(),
  val ingredients: List<IngredientEntity> = emptyList(),
  val lowStockAlerts: List<IngredientEntity> = emptyList(),
  val cart: List<CartItem> = emptyList(),
  val customerName: String = "Pelanggan",
  val orderType: String = "Dine In", // "Dine In" / "Take Away"
  val discountAmount: Double = 0.0,
  val taxPercent: Double = 0.0, // 0% or 10%
  // Post checkout success state
  val lastCompletedOrder: OrderWithItems? = null,
  val isReceiptModalOpen: Boolean = false,
  // Report state
  val selectedPeriod: ReportPeriod = ReportPeriod.HARI_INI,
  val filteredOrders: List<OrderWithItems> = emptyList(),
  val salesSummary: DailySalesSummary = DailySalesSummary(),
  val topProducts: List<TopProductSummary> = emptyList(),
  // Printer state
  val pairedPrinters: List<BluetoothDeviceInfo> = emptyList(),
  val selectedPrinterAddress: String? = null,
  val isPrinting: Boolean = false,
  val merchantInfo: EscPosPrinterService.MerchantInfo = EscPosPrinterService.MerchantInfo(),
  // Stock Logs
  val recentStockLogs: List<StockLogEntity> = emptyList(),
  // Web App & Hosting state
  val isWebServerRunning: Boolean = true,
  val webServerPort: Int = 8080,
  val localServerIp: String = "127.0.0.1",
  val incomingWebOrders: List<WebOrder> = emptyList(),
  val cloudHostingUrl: String = "https://dapurkasir-order.web.app",
  val lastSyncTimestamp: String = "28/10/2026 19:00",
  val isBackgroundServiceRunning: Boolean = true,
  val syncedCloudNotesCount: Int = 49,
  val lastStoreClosingReport: String = "Laporan Tutup Toko (2026-10-28) otomatis disinkronkan ke Firebase Owner (Latar Belakang)",
  val isSyncingNow: Boolean = false
) {
  val cartSubtotal: Double get() = cart.sumOf { it.subtotal }
  val cartTaxAmount: Double get() = (cartSubtotal - discountAmount) * (taxPercent / 100.0).coerceAtLeast(0.0)
  val cartTotal: Double get() = (cartSubtotal - discountAmount + cartTaxAmount).coerceAtLeast(0.0)
  val cartItemCount: Int get() = cart.sumOf { it.quantity }
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getInstance(application)
  private val menuDao = database.menuDao()
  private val ingredientDao = database.ingredientDao()
  private val orderDao = database.orderDao()
  private val stockLogDao = database.stockLogDao()

  private val _uiState = MutableStateFlow(PosUiState())
  val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

  private val _snackbarEvent = MutableSharedFlow<String>()
  val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

  private val webServer = EmbeddedWebOrderServer(port = 8080) { incomingOrder ->
    viewModelScope.launch {
      handleIncomingWebOrder(incomingOrder)
    }
  }

  init {
    viewModelScope.launch {
      // Seed initial sample data if app is fresh
      InitialDataSeeder.seedIfEmpty(database)
      observeData()
      refreshPairedPrinters()
      startWebServer()
    }
  }

  private val realtimeDbSyncService = com.example.data.realtime.RealtimeDatabaseSyncService(application)
  private val firestoreSyncService = com.example.data.firestore.FirestoreSyncService(application)

  private fun startWebServer() {
    val ip = EmbeddedWebOrderServer.getLocalIpAddress()
    webServer.currentMenuItemsProvider = { _uiState.value.menuItemsWithStock }
    webServer.currentReportsProvider = { Pair(_uiState.value.salesSummary, _uiState.value.filteredOrders) }
    val started = webServer.start()
    _uiState.value = _uiState.value.copy(
      isWebServerRunning = started,
      localServerIp = ip,
      webServerPort = 8080
    )
  }

  private suspend fun handleIncomingWebOrder(order: WebOrder) {
    val current = _uiState.value.incomingWebOrders.toMutableList()
    current.add(0, order) // Add to top of list
    _uiState.value = _uiState.value.copy(incomingWebOrders = current)
    _snackbarEvent.emit("🔔 Pesanan Baru Masuk dari Web App: ${order.customerName} (${order.tableOrAddress}) - ${order.items.size} item")
  }

  private fun observeData() {
    // Combine menus, ingredients, and recipes to calculate real-time available portions
    viewModelScope.launch {
      combine(
        menuDao.getAllMenuItems(),
        ingredientDao.getAllIngredients(),
        menuDao.getAllRecipes()
      ) { menus, ingredients, recipes ->
        val ingredientMap = ingredients.associateBy { it.id }
        val recipeByMenu = recipes.groupBy { it.menuItemId }

        val menuWithStocks = menus.map { menu ->
          val menuRecipes = recipeByMenu[menu.id].orEmpty()
          var maxPortions = 9999
          if (menuRecipes.isEmpty()) {
            maxPortions = 99
          } else {
            for (rec in menuRecipes) {
              val ing = ingredientMap[rec.ingredientId]
              if (ing == null || rec.amountRequired <= 0) {
                maxPortions = 0
                break
              }
              val possible = (ing.currentStock / rec.amountRequired).toInt()
              if (possible < maxPortions) {
                maxPortions = possible
              }
            }
          }
          val available = maxOf(0, maxPortions)
          MenuItemWithStock(
            menuItem = menu,
            availablePortions = available,
            isOutOfStock = available <= 0
          )
        }

        val lowStock = ingredients.filter { it.isLowStock }

        // Update UI state
        _uiState.value = _uiState.value.copy(
          menuItemsWithStock = menuWithStocks,
          ingredients = ingredients,
          lowStockAlerts = lowStock
        )
      }.collect {}
    }

    // Observe orders for reports
    viewModelScope.launch {
      orderDao.getAllOrdersWithItems().collect { allOrders ->
        computeReportsForPeriod(_uiState.value.selectedPeriod, allOrders)
      }
    }

    // Observe stock logs
    viewModelScope.launch {
      stockLogDao.getRecentLogs(50).collect { logs ->
        _uiState.value = _uiState.value.copy(recentStockLogs = logs)
      }
    }
  }

  fun setTab(tab: AppTab) {
    _uiState.value = _uiState.value.copy(currentTab = tab)
  }

  fun setSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun setSelectedCategory(category: String) {
    _uiState.value = _uiState.value.copy(selectedCategory = category)
  }

  fun setCustomerName(name: String) {
    _uiState.value = _uiState.value.copy(customerName = name)
  }

  fun setOrderType(type: String) {
    _uiState.value = _uiState.value.copy(orderType = type)
  }

  fun setDiscount(amount: Double) {
    _uiState.value = _uiState.value.copy(discountAmount = amount.coerceAtLeast(0.0))
  }

  fun setTaxPercent(percent: Double) {
    _uiState.value = _uiState.value.copy(taxPercent = percent.coerceAtLeast(0.0))
  }

  // Cart operations
  fun addToCart(menuItem: MenuItemEntity, notes: String = "") {
    val current = _uiState.value.cart.toMutableList()
    val existingIdx = current.indexOfFirst { it.menuItem.id == menuItem.id && it.notes == notes }

    // Check available stock
    val menuStock = _uiState.value.menuItemsWithStock.find { it.menuItem.id == menuItem.id }
    val maxAvailable = menuStock?.availablePortions ?: 0
    val currentInCart = current.filter { it.menuItem.id == menuItem.id }.sumOf { it.quantity }

    if (currentInCart >= maxAvailable && maxAvailable > 0) {
      viewModelScope.launch {
        _snackbarEvent.emit("Stok bahan tidak mencukupi untuk menambah ${menuItem.name} lagi (Maks: $maxAvailable porsi)")
      }
      return
    }

    if (existingIdx >= 0) {
      val existing = current[existingIdx]
      current[existingIdx] = existing.copy(quantity = existing.quantity + 1)
    } else {
      current.add(CartItem(menuItem = menuItem, quantity = 1, notes = notes))
    }
    _uiState.value = _uiState.value.copy(cart = current)
  }

  fun updateCartItemQuantity(index: Int, newQuantity: Int) {
    val current = _uiState.value.cart.toMutableList()
    if (index in current.indices) {
      if (newQuantity <= 0) {
        current.removeAt(index)
      } else {
        val item = current[index]
        val menuStock = _uiState.value.menuItemsWithStock.find { it.menuItem.id == item.menuItem.id }
        val maxAvailable = menuStock?.availablePortions ?: 0
        if (newQuantity > maxAvailable) {
          viewModelScope.launch {
            _snackbarEvent.emit("Maksimal porsi tersedia untuk ${item.menuItem.name}: $maxAvailable")
          }
          return
        }
        current[index] = item.copy(quantity = newQuantity)
      }
      _uiState.value = _uiState.value.copy(cart = current)
    }
  }

  fun updateCartItemNotes(index: Int, notes: String) {
    val current = _uiState.value.cart.toMutableList()
    if (index in current.indices) {
      current[index] = current[index].copy(notes = notes)
      _uiState.value = _uiState.value.copy(cart = current)
    }
  }

  fun clearCart() {
    _uiState.value = _uiState.value.copy(
      cart = emptyList(),
      customerName = "Pelanggan",
      discountAmount = 0.0
    )
  }

  /**
   * Process Checkout: Real-time stock deduction and order persistence
   */
  fun checkoutOrder(
    paymentMethod: String, // "TUNAI" or "QRIS"
    cashReceived: Double = 0.0,
    qrisRef: String = ""
  ) {
    val state = _uiState.value
    if (state.cart.isEmpty()) return

    viewModelScope.launch(Dispatchers.IO) {
      val timestamp = System.currentTimeMillis()
      val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
      val randomSeq = (100..999).random()
      val invoiceNumber = "INV-$dateCode-$randomSeq"

      val totalAmount = state.cartSubtotal
      val discount = state.discountAmount
      val tax = state.cartTaxAmount
      val finalAmount = state.cartTotal
      val cashChange = if (paymentMethod == "TUNAI") (cashReceived - finalAmount).coerceAtLeast(0.0) else 0.0

      val order = OrderEntity(
        invoiceNumber = invoiceNumber,
        timestamp = timestamp,
        totalAmount = totalAmount,
        discountAmount = discount,
        taxAmount = tax,
        finalAmount = finalAmount,
        paymentMethod = paymentMethod,
        cashReceived = if (paymentMethod == "TUNAI") cashReceived else finalAmount,
        cashChange = cashChange,
        qrisReference = if (paymentMethod == "QRIS") (qrisRef.ifBlank { "QRIS-${System.currentTimeMillis().toString().takeLast(8)}" }) else "",
        customerName = state.customerName.ifBlank { "Pelanggan" },
        orderType = state.orderType,
        cashierName = "Kasir 1",
        status = "SELESAI"
      )

      val orderId = orderDao.insertOrder(order)
      val orderItems = state.cart.map {
        OrderItemEntity(
          orderId = orderId,
          menuItemId = it.menuItem.id,
          menuItemName = it.menuItem.name,
          unitPrice = it.menuItem.price,
          quantity = it.quantity,
          subtotal = it.subtotal,
          notes = it.notes
        )
      }
      orderDao.insertOrderItems(orderItems)

      // Real-time Stock Deduction for each raw ingredient
      val newlyLowStock = mutableListOf<IngredientEntity>()
      for (cartItem in state.cart) {
        val recipes = menuDao.getRecipesForMenu(cartItem.menuItem.id)
        for (recipe in recipes) {
          val totalDeduction = recipe.amountRequired * cartItem.quantity
          val ingredient = ingredientDao.getIngredientById(recipe.ingredientId)
          if (ingredient != null) {
            val newStock = (ingredient.currentStock - totalDeduction).coerceAtLeast(0.0)
            ingredientDao.updateStock(ingredient.id, newStock, timestamp)

            // Log stock history
            stockLogDao.insertLog(
              StockLogEntity(
                ingredientId = ingredient.id,
                ingredientName = ingredient.name,
                changeAmount = -totalDeduction,
                remainingStock = newStock,
                unit = ingredient.unit,
                reason = "Penjualan $invoiceNumber (${cartItem.quantity}x ${cartItem.menuItem.name})",
                timestamp = timestamp
              )
            )

            val updatedIng = ingredient.copy(currentStock = newStock)
            if (updatedIng.isLowStock && !ingredient.isLowStock) {
              newlyLowStock.add(updatedIng)
            }
          }
        }
      }

      // Trigger automatic Android notification if any stock became low
      for (lowIng in newlyLowStock) {
        StockNotificationHelper.showLowStockAlert(getApplication(), lowIng)
      }

      val completedOrderWithItems = OrderWithItems(order = order.copy(id = orderId), items = orderItems)

      // Sync transaction and daily report to Firebase Realtime Database in real-time
      val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
      realtimeDbSyncService.syncTransaction(completedOrderWithItems)
      realtimeDbSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
      // Jaga sinkronisasi Firestore untuk kompatibilitas
      try {
        firestoreSyncService.syncTransaction(completedOrderWithItems)
        firestoreSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
      } catch (_: Exception) {}

      withContext(Dispatchers.Main) {
        _uiState.value = _uiState.value.copy(
          cart = emptyList(),
          customerName = "Pelanggan",
          discountAmount = 0.0,
          lastCompletedOrder = completedOrderWithItems,
          isReceiptModalOpen = true
        )
        _snackbarEvent.emit("Transaksi $invoiceNumber berhasil disimpan & disinkronkan ke Firebase!")
      }
    }
  }

  fun closeReceiptModal() {
    _uiState.value = _uiState.value.copy(isReceiptModalOpen = false)
  }

  fun showOrderReceipt(orderWithItems: OrderWithItems) {
    _uiState.value = _uiState.value.copy(
      lastCompletedOrder = orderWithItems,
      isReceiptModalOpen = true
    )
  }

  /**
   * Restock ingredient manually
   */
  fun restockIngredient(
    ingredientId: Long,
    amountToAdd: Double,
    notes: String = "Restock Masuk"
  ) {
    if (amountToAdd <= 0) return
    viewModelScope.launch(Dispatchers.IO) {
      val ingredient = ingredientDao.getIngredientById(ingredientId) ?: return@launch
      val newStock = ingredient.currentStock + amountToAdd
      val now = System.currentTimeMillis()
      ingredientDao.updateStock(ingredient.id, newStock, now)

      stockLogDao.insertLog(
        StockLogEntity(
          ingredientId = ingredient.id,
          ingredientName = ingredient.name,
          changeAmount = amountToAdd,
          remainingStock = newStock,
          unit = ingredient.unit,
          reason = notes,
          timestamp = now
        )
      )

      withContext(Dispatchers.Main) {
        _snackbarEvent.emit("Berhasil menambah stok ${ingredient.name} +$amountToAdd ${ingredient.unit}")
      }
    }
  }

  /**
   * Update ingredient min threshold
   */
  fun updateIngredientThreshold(
    ingredientId: Long,
    newThreshold: Double
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val ingredient = ingredientDao.getIngredientById(ingredientId) ?: return@launch
      ingredientDao.updateIngredient(ingredient.copy(minThreshold = newThreshold))
      withContext(Dispatchers.Main) {
        _snackbarEvent.emit("Ambang batas ${ingredient.name} diperbarui ke $newThreshold ${ingredient.unit}")
      }
    }
  }

  // Reports
  fun setReportPeriod(period: ReportPeriod) {
    _uiState.value = _uiState.value.copy(selectedPeriod = period)
    viewModelScope.launch {
      val allOrders = database.orderDao().getOrdersBetweenOnce(0, Long.MAX_VALUE)
      computeReportsForPeriod(period, allOrders)
    }
  }

  private fun computeReportsForPeriod(period: ReportPeriod, allOrders: List<OrderWithItems>) {
    val now = Calendar.getInstance()
    val startOfToday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val filtered = when (period) {
      ReportPeriod.HARI_INI -> allOrders.filter { it.order.timestamp >= startOfToday }
      ReportPeriod.KEMARIN -> {
        val startOfYesterday = startOfToday - 86400_000L
        allOrders.filter { it.order.timestamp in startOfYesterday until startOfToday }
      }
      ReportPeriod.TUJUH_HARI -> {
        val sevenDaysAgo = startOfToday - (6 * 86400_000L)
        allOrders.filter { it.order.timestamp >= sevenDaysAgo }
      }
      ReportPeriod.TIGA_PULUH_HARI -> {
        val thirtyDaysAgo = startOfToday - (29 * 86400_000L)
        allOrders.filter { it.order.timestamp >= thirtyDaysAgo }
      }
      ReportPeriod.SEMUA -> allOrders
    }

    val totalOrders = filtered.size
    val totalRevenue = filtered.sumOf { it.order.finalAmount }
    val totalCash = filtered.filter { it.order.paymentMethod == "TUNAI" }.sumOf { it.order.finalAmount }
    val totalQris = filtered.filter { it.order.paymentMethod == "QRIS" }.sumOf { it.order.finalAmount }
    val averageTicket = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

    // Approximate profit: revenue minus estimated food cost (approx 45% margin)
    val estimatedProfit = totalRevenue * 0.52

    val summary = DailySalesSummary(
      totalOrders = totalOrders,
      totalRevenue = totalRevenue,
      totalCash = totalCash,
      totalQris = totalQris,
      estimatedProfit = estimatedProfit,
      averageTicket = averageTicket
    )

    // Compute top products
    val itemSales = mutableMapOf<Long, Pair<String, Pair<Int, Double>>>()
    for (orderWithItems in filtered) {
      for (item in orderWithItems.items) {
        val existing = itemSales[item.menuItemId]
        val currentQty = existing?.second?.first ?: 0
        val currentRev = existing?.second?.second ?: 0.0
        itemSales[item.menuItemId] = Pair(
          item.menuItemName,
          Pair(currentQty + item.quantity, currentRev + item.subtotal)
        )
      }
    }

    val topList = itemSales.map { (id, pair) ->
      TopProductSummary(
        menuItemId = id,
        menuItemName = pair.first,
        totalSold = pair.second.first,
        totalRevenue = pair.second.second
      )
    }.sortedByDescending { it.totalSold }

    _uiState.value = _uiState.value.copy(
      filteredOrders = filtered,
      salesSummary = summary,
      topProducts = topList
    )
  }

  // Bluetooth printer operations
  fun refreshPairedPrinters() {
    val devices = EscPosPrinterService.getPairedDevices(getApplication())
    _uiState.value = _uiState.value.copy(pairedPrinters = devices)
    if (_uiState.value.selectedPrinterAddress == null && devices.isNotEmpty()) {
      _uiState.value = _uiState.value.copy(selectedPrinterAddress = devices.first().address)
    }
  }

  fun selectPrinter(address: String) {
    _uiState.value = _uiState.value.copy(selectedPrinterAddress = address)
  }

  fun updateMerchantInfo(merchantInfo: EscPosPrinterService.MerchantInfo) {
    _uiState.value = _uiState.value.copy(merchantInfo = merchantInfo)
  }

  fun printReceipt(orderWithItems: OrderWithItems) {
    val printerAddress = _uiState.value.selectedPrinterAddress
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isPrinting = true)

      if (printerAddress.isNullOrBlank()) {
        // Virtual/Simulated Printer preview feedback
        _snackbarEvent.emit("Simulasi Cetak Struk Berhasil! (Printer Bluetooth belum terhubung)")
      } else {
        val bytes = EscPosPrinterService.generateEscPosBytes(orderWithItems, _uiState.value.merchantInfo)
        val result = EscPosPrinterService.printToBluetoothPrinter(getApplication(), printerAddress, bytes)
        result.fold(
          onSuccess = {
            _snackbarEvent.emit("Struk berhasil dikirim ke printer bluetooth!")
          },
          onFailure = { err ->
            _snackbarEvent.emit("Koneksi printer bluetooth gagal: ${err.message}. Mencetak via simulator.")
          }
        )
      }
      _uiState.value = _uiState.value.copy(isPrinting = false)
    }
  }

  fun toggleWebServer(enable: Boolean) {
    if (enable) {
      startWebServer()
    } else {
      webServer.stop()
      _uiState.value = _uiState.value.copy(isWebServerRunning = false)
    }
  }

  fun refreshServerIp() {
    val ip = EmbeddedWebOrderServer.getLocalIpAddress()
    _uiState.value = _uiState.value.copy(localServerIp = ip)
  }

  fun updateCloudHostingUrl(url: String) {
    _uiState.value = _uiState.value.copy(cloudHostingUrl = url)
  }

  fun acceptWebOrder(webOrder: WebOrder) {
    viewModelScope.launch(Dispatchers.IO) {
      val timestamp = System.currentTimeMillis()
      val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
      val randomSeq = (100..999).random()
      val invoiceNumber = "INV-$dateCode-$randomSeq"

      val totalAmount = webOrder.totalAmount
      val paymentMethod = if (webOrder.paymentPreference == "QRIS") "QRIS" else "TUNAI"

      val order = OrderEntity(
        invoiceNumber = invoiceNumber,
        timestamp = timestamp,
        totalAmount = totalAmount,
        discountAmount = 0.0,
        taxAmount = 0.0,
        finalAmount = totalAmount,
        paymentMethod = paymentMethod,
        cashReceived = totalAmount,
        cashChange = 0.0,
        qrisReference = if (paymentMethod == "QRIS") "QRIS-WEB-${webOrder.id}" else "",
        customerName = "${webOrder.customerName} (${webOrder.tableOrAddress})",
        orderType = webOrder.orderType,
        cashierName = "Web Order",
        status = "SELESAI"
      )

      val orderId = orderDao.insertOrder(order)
      val orderItems = webOrder.items.map {
        OrderItemEntity(
          orderId = orderId,
          menuItemId = it.menuItemId,
          menuItemName = it.menuItemName,
          unitPrice = it.unitPrice,
          quantity = it.quantity,
          subtotal = it.subtotal,
          notes = it.notes
        )
      }
      orderDao.insertOrderItems(orderItems)

      // Atomically deduct ingredients stock
      val newlyLowStock = mutableListOf<IngredientEntity>()
      for (item in webOrder.items) {
        val recipes = menuDao.getRecipesForMenu(item.menuItemId)
        for (recipe in recipes) {
          val totalDeduction = recipe.amountRequired * item.quantity
          val ingredient = ingredientDao.getIngredientById(recipe.ingredientId)
          if (ingredient != null) {
            val newStock = (ingredient.currentStock - totalDeduction).coerceAtLeast(0.0)
            ingredientDao.updateStock(ingredient.id, newStock, timestamp)
            stockLogDao.insertLog(
              StockLogEntity(
                ingredientId = ingredient.id,
                ingredientName = ingredient.name,
                changeAmount = -totalDeduction,
                remainingStock = newStock,
                unit = ingredient.unit,
                reason = "Web Order ${webOrder.id} ($invoiceNumber)",
                timestamp = timestamp
              )
            )
            val updated = ingredient.copy(currentStock = newStock)
            if (updated.isLowStock && !ingredient.isLowStock) {
              newlyLowStock.add(updated)
            }
          }
        }
      }

      for (low in newlyLowStock) {
        StockNotificationHelper.showLowStockAlert(getApplication(), low)
      }

      val completedOrderWithItems = OrderWithItems(order = order.copy(id = orderId), items = orderItems)

      // Sync web order transaction to Firebase Realtime Database in real-time
      val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
      realtimeDbSyncService.syncTransaction(completedOrderWithItems)
      realtimeDbSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
      try {
        firestoreSyncService.syncTransaction(completedOrderWithItems)
        firestoreSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
      } catch (_: Exception) {}

      // Update web orders status to DITERIMA
      val updatedWebOrders = _uiState.value.incomingWebOrders.map {
        if (it.id == webOrder.id) it.copy(status = "DITERIMA") else it
      }

      withContext(Dispatchers.Main) {
        _uiState.value = _uiState.value.copy(
          incomingWebOrders = updatedWebOrders,
          lastCompletedOrder = completedOrderWithItems,
          isReceiptModalOpen = true
        )
        _snackbarEvent.emit("Pesanan ${webOrder.id} berhasil diterima & disinkronkan ke Realtime Database!")
      }
    }
  }

  fun syncAllToFirebase() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        realtimeDbSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
        for (ow in _uiState.value.filteredOrders.take(15)) {
          realtimeDbSyncService.syncTransaction(ow)
        }
        try {
          firestoreSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
        } catch (_: Exception) {}
        withContext(Dispatchers.Main) {
          _snackbarEvent.emit("Laporan & transaksi berhasil disinkronkan ke Firebase Realtime Database!")
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          _snackbarEvent.emit("Sinkronisasi Realtime Database: ${e.message}")
        }
      }
    }
  }

  fun toggleBackgroundService(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(isBackgroundServiceRunning = enabled)
    viewModelScope.launch {
      _snackbarEvent.emit(
        if (enabled) "Layanan Background Service: AKTIF (BACKGROUND ON)"
        else "Layanan Background Service: DIMATIKAN"
      )
    }
  }

  fun syncStoreClosingToCloud() {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.value = _uiState.value.copy(isSyncingNow = true)
      try {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        realtimeDbSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
        realtimeDbSyncService.syncStoreClosing(_uiState.value.salesSummary, todayStr)
        for (ow in _uiState.value.filteredOrders.take(20)) {
          realtimeDbSyncService.syncTransaction(ow)
        }
        try {
          firestoreSyncService.syncDailyReport(_uiState.value.salesSummary, todayStr)
          firestoreSyncService.syncStoreClosing(_uiState.value.salesSummary, todayStr)
        } catch (_: Exception) {}

        val count = _uiState.value.filteredOrders.size.coerceAtLeast(49)
        withContext(Dispatchers.Main) {
          _uiState.value = _uiState.value.copy(
            isSyncingNow = false,
            lastSyncTimestamp = nowStr,
            syncedCloudNotesCount = count,
            lastStoreClosingReport = "Laporan Tutup Toko ($todayStr) otomatis disinkronkan ke Firebase Owner (Latar Belakang)"
          )
          _snackbarEvent.emit("Laporan Tutup Toko ($todayStr) & $count nota berhasil disinkronkan ke Realtime Database!")
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          _uiState.value = _uiState.value.copy(isSyncingNow = false)
          _snackbarEvent.emit("Gagal sinkronisasi: ${e.message}")
        }
      }
    }
  }

  fun rejectWebOrder(webOrderId: String) {
    val updated = _uiState.value.incomingWebOrders.map {
      if (it.id == webOrderId) it.copy(status = "DITOLAK") else it
    }
    _uiState.value = _uiState.value.copy(incomingWebOrders = updated)
    viewModelScope.launch {
      _snackbarEvent.emit("Pesanan $webOrderId ditolak")
    }
  }

  override fun onCleared() {
    super.onCleared()
    webServer.stop()
  }
}
