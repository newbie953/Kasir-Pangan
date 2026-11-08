package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MenuItemEntity
import com.example.data.model.MenuItemWithStock
import com.example.ui.components.CashPaymentDialog
import com.example.ui.components.LowStockBanner
import com.example.ui.components.QrisPaymentDialog
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockGoodGreen
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
  viewModel: PosViewModel,
  uiState: PosUiState
) {
  var showCartBottomSheet by remember { mutableStateOf(false) }
  var showCashDialog by remember { mutableStateOf(false) }
  var showQrisDialog by remember { mutableStateOf(false) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  val categories = listOf("Semua", "Makanan", "Minuman", "Camilan")

  // Filtered menus
  val filteredMenus = uiState.menuItemsWithStock.filter { itemWithStock ->
    val matchesCategory = uiState.selectedCategory == "Semua" || itemWithStock.menuItem.category == uiState.selectedCategory
    val matchesQuery = uiState.searchQuery.isBlank() ||
      itemWithStock.menuItem.name.contains(uiState.searchQuery, ignoreCase = true) ||
      itemWithStock.menuItem.description.contains(uiState.searchQuery, ignoreCase = true)
    matchesCategory && matchesQuery
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = if (uiState.cartItemCount > 0) 80.dp else 8.dp)
    ) {
      // Low stock interactive banner
      LowStockBanner(
        lowStockItems = uiState.lowStockAlerts,
        onBannerClick = { viewModel.setTab(AppTab.STOK_BAHAN) }
      )

      // Search Bar
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Cari menu makanan, minuman, camilan...") },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("search_menu_input"),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )

      // Category Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        categories.forEach { cat ->
          FilterChip(
            selected = uiState.selectedCategory == cat,
            onClick = { viewModel.setSelectedCategory(cat) },
            label = { Text(cat, fontWeight = if (uiState.selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = TerracottaPrimary.copy(alpha = 0.15f),
              selectedLabelColor = TerracottaPrimary
            ),
            modifier = Modifier.testTag("category_chip_$cat")
          )
        }
      }

      // Menu Grid
      if (filteredMenus.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.RestaurantMenu,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Menu tidak ditemukan",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 165.dp),
          contentPadding = PaddingValues(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("menu_items_grid")
        ) {
          items(filteredMenus, key = { it.menuItem.id }) { itemWithStock ->
            MenuCardItem(
              itemWithStock = itemWithStock,
              inCartQuantity = uiState.cart.filter { it.menuItem.id == itemWithStock.menuItem.id }.sumOf { it.quantity },
              onAddClick = {
                viewModel.addToCart(itemWithStock.menuItem)
              }
            )
          }
        }
      }
    }

    // Floating Cart Summary Bar
    AnimatedVisibility(
      visible = uiState.cartItemCount > 0,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(16.dp)
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showCartBottomSheet = true }
          .testTag("floating_cart_bar"),
        shape = RoundedCornerShape(16.dp),
        color = TerracottaPrimary,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(
              badge = {
                Badge(
                  containerColor = Color.White,
                  contentColor = TerracottaPrimary
                ) {
                  Text("${uiState.cartItemCount}", fontWeight = FontWeight.Bold)
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "${uiState.cartItemCount} Menu Dipilih",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall
              )
              Text(
                text = formatRupiah(uiState.cartTotal),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }

          Button(
            onClick = { showCartBottomSheet = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = TerracottaPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("open_cart_sheet_button")
          ) {
            Text("Proses Pesanan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }

  // Cart Bottom Sheet
  if (showCartBottomSheet) {
    ModalBottomSheet(
      onDismissRequest = { showCartBottomSheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
      dragHandle = null
    ) {
      CartBottomSheetContent(
        uiState = uiState,
        onUpdateQuantity = { idx, qty -> viewModel.updateCartItemQuantity(idx, qty) },
        onUpdateNotes = { idx, notes -> viewModel.updateCartItemNotes(idx, notes) },
        onCustomerNameChange = { viewModel.setCustomerName(it) },
        onOrderTypeChange = { viewModel.setOrderType(it) },
        onClearCart = {
          viewModel.clearCart()
          showCartBottomSheet = false
        },
        onPayCash = {
          showCashDialog = true
        },
        onPayQris = {
          showQrisDialog = true
        },
        onClose = {
          scope.launch { sheetState.hide() }.invokeOnCompletion {
            showCartBottomSheet = false
          }
        }
      )
    }
  }

  // Cash Payment Dialog
  if (showCashDialog) {
    CashPaymentDialog(
      totalAmount = uiState.cartTotal,
      onConfirmSuccess = { cashReceived ->
        viewModel.checkoutOrder(paymentMethod = "TUNAI", cashReceived = cashReceived)
        showCashDialog = false
        showCartBottomSheet = false
      },
      onDismiss = { showCashDialog = false }
    )
  }

  // QRIS Payment Dialog
  if (showQrisDialog) {
    QrisPaymentDialog(
      amount = uiState.cartTotal,
      onConfirmSuccess = { qrisRef ->
        viewModel.checkoutOrder(paymentMethod = "QRIS", qrisRef = qrisRef)
        showQrisDialog = false
        showCartBottomSheet = false
      },
      onDismiss = { showQrisDialog = false }
    )
  }
}

@Composable
private fun MenuCardItem(
  itemWithStock: MenuItemWithStock,
  inCartQuantity: Int,
  onAddClick: () -> Unit
) {
  val menu = itemWithStock.menuItem
  val isOut = itemWithStock.isOutOfStock
  val available = itemWithStock.availablePortions

  val stockColor = when {
    isOut -> StockCriticalRed
    available <= 5 -> StockLowAmber
    else -> StockGoodGreen
  }

  val stockLabel = when {
    isOut -> "Habis"
    available <= 5 -> "Sisa $available porsi"
    else -> "$available porsi"
  }

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = !isOut) { onAddClick() }
      .testTag("menu_card_${menu.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Top icon & category tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(
              color = getCategoryColor(menu.category).copy(alpha = 0.12f),
              shape = RoundedCornerShape(10.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = getMenuIcon(menu.iconKey),
            contentDescription = null,
            tint = getCategoryColor(menu.category),
            modifier = Modifier.size(24.dp)
          )
        }

        // Available portions badge
        Box(
          modifier = Modifier
            .background(stockColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = stockLabel,
            color = stockColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Menu title
      Text(
        text = menu.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      // Description
      Text(
        text = menu.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.height(34.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Price and Add Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formatRupiah(menu.price),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = TerracottaPrimary
        )

        if (inCartQuantity > 0) {
          Box(
            modifier = Modifier
              .background(TerracottaPrimary, shape = CircleShape)
              .size(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "$inCartQuantity",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        } else {
          IconButton(
            onClick = onAddClick,
            enabled = !isOut,
            modifier = Modifier
              .size(32.dp)
              .background(
                if (isOut) Color.LightGray.copy(alpha = 0.4f) else TerracottaPrimary.copy(alpha = 0.12f),
                CircleShape
              )
              .testTag("add_menu_button_${menu.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Tambah",
              tint = if (isOut) Color.Gray else TerracottaPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CartBottomSheetContent(
  uiState: PosUiState,
  onUpdateQuantity: (Int, Int) -> Unit,
  onUpdateNotes: (Int, String) -> Unit,
  onCustomerNameChange: (String) -> Unit,
  onOrderTypeChange: (String) -> Unit,
  onClearCart: () -> Unit,
  onPayCash: () -> Unit,
  onPayQris: () -> Unit,
  onClose: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.ShoppingCart,
          contentDescription = null,
          tint = TerracottaPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Ringkasan Pesanan (${uiState.cartItemCount})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
      TextButton(
        onClick = onClearCart,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
      ) {
        Text("Kosongkan")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Customer Name & Order Type row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = uiState.customerName,
        onValueChange = onCustomerNameChange,
        label = { Text("Nama/No Meja") },
        singleLine = true,
        modifier = Modifier
          .weight(1.3f)
          .testTag("customer_name_input"),
        shape = RoundedCornerShape(12.dp)
      )

      // Order Type Toggle
      Row(
        modifier = Modifier
          .weight(1.2f)
          .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        listOf("Dine In", "Take Away").forEach { type ->
          val isSelected = uiState.orderType == type
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) TerracottaPrimary else Color.Transparent)
              .clickable { onOrderTypeChange(type) }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (type == "Dine In") "Makan" else "Bungkus",
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Cart Items List
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f, fill = false)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      uiState.cart.forEachIndexed { index, cartItem ->
        CartItemRow(
          cartItem = cartItem,
          onQuantityChange = { newQty -> onUpdateQuantity(index, newQty) },
          onNotesChange = { notes -> onUpdateNotes(index, notes) }
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Bill Breakdown
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      shape = RoundedCornerShape(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
          Text(formatRupiah(uiState.cartSubtotal), style = MaterialTheme.typography.bodyMedium)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Total Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            formatRupiah(uiState.cartTotal),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TerracottaPrimary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Payment method buttons (Tunai vs QRIS)
    Text(
      text = "Pilih Metode Pembayaran:",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onPayCash,
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("checkout_cash_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FreshGreenTertiary)
      ) {
        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tunai (Cash)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      Button(
        onClick = onPayQris,
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("checkout_qris_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
      ) {
        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("QRIS Digital", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun CartItemRow(
  cartItem: CartItem,
  onQuantityChange: (Int) -> Unit,
  onNotesChange: (String) -> Unit
) {
  var isEditingNotes by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = cartItem.menuItem.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = formatRupiah(cartItem.menuItem.price),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Quantity controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          IconButton(
            onClick = { onQuantityChange(cartItem.quantity - 1) },
            modifier = Modifier
              .size(28.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
          ) {
            Icon(
              imageVector = if (cartItem.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
              contentDescription = "Kurang",
              modifier = Modifier.size(16.dp)
            )
          }

          Text(
            text = "${cartItem.quantity}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
          )

          IconButton(
            onClick = { onQuantityChange(cartItem.quantity + 1) },
            modifier = Modifier
              .size(28.dp)
              .background(TerracottaPrimary, CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Tambah",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = formatRupiah(cartItem.subtotal),
            fontWeight = FontWeight.Bold,
            color = TerracottaPrimary,
            fontSize = 13.sp
          )
        }
      }

      // Notes
      if (cartItem.notes.isNotBlank() && !isEditingNotes) {
        Text(
          text = "Catatan: ${cartItem.notes}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .clickable { isEditingNotes = true }
            .padding(top = 4.dp)
        )
      } else if (!isEditingNotes) {
        Text(
          text = "+ Tambah Catatan (misal: pedas, tanpa es)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
          modifier = Modifier
            .clickable { isEditingNotes = true }
            .padding(top = 4.dp)
        )
      } else {
        OutlinedTextField(
          value = cartItem.notes,
          onValueChange = onNotesChange,
          placeholder = { Text("Contoh: Pedas level 2, jangan pakai bawang") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
          shape = RoundedCornerShape(8.dp),
          trailingIcon = {
            IconButton(onClick = { isEditingNotes = false }) {
              Icon(Icons.Default.Add, contentDescription = "Selesai", modifier = Modifier.size(16.dp))
            }
          }
        )
      }
    }
  }
}

private fun getMenuIcon(iconKey: String): ImageVector {
  return when (iconKey) {
    "kopi" -> Icons.Default.LocalCafe
    "es_teh", "jus", "es_jeruk" -> Icons.Default.LocalDining
    "camilan", "pisang", "tahu", "kentang" -> Icons.Default.Fastfood
    else -> Icons.Default.RestaurantMenu
  }
}

private fun getCategoryColor(category: String): Color {
  return when (category) {
    "Makanan" -> TerracottaPrimary
    "Minuman" -> Color(0xFF0288D1)
    "Camilan" -> Color(0xFFF57C00)
    else -> Color(0xFF5D4037)
  }
}

private fun formatRupiah(amount: Double): String {
  val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
  return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
