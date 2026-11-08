package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderWithItems
import com.example.data.model.TopProductSummary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.ReportPeriod
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LaporanScreen(
  viewModel: PosViewModel,
  uiState: PosUiState
) {
  var selectedTab by remember { mutableIntStateOf(0) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Period Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.FilterList,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
      ReportPeriod.values().forEach { period ->
        FilterChip(
          selected = uiState.selectedPeriod == period,
          onClick = { viewModel.setReportPeriod(period) },
          label = { Text(period.label, fontWeight = if (uiState.selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TerracottaPrimary.copy(alpha = 0.15f),
            selectedLabelColor = TerracottaPrimary
          ),
          modifier = Modifier.testTag("report_period_chip_${period.name}")
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Firebase Cloud Real-time Status Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = FreshGreenTertiary.copy(alpha = 0.08f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudDone, contentDescription = null, tint = FreshGreenTertiary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("Firebase Realtime Database Aktif", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = FreshGreenTertiary)
            Text("Laporan & laba tersinkron ke Realtime DB (Bebas Firestore)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          }
        }
        OutlinedButton(
          onClick = { viewModel.syncAllToFirebase() },
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Top Overview Summary Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricCard(
        title = "Total Omset",
        value = formatRupiah(uiState.salesSummary.totalRevenue),
        subtitle = "${uiState.salesSummary.totalOrders} Transaksi",
        icon = Icons.Default.TrendingUp,
        accentColor = TerracottaPrimary,
        modifier = Modifier.weight(1.2f)
      )
      MetricCard(
        title = "Estimasi Laba",
        value = formatRupiah(uiState.salesSummary.estimatedProfit),
        subtitle = "Margin ~52%",
        icon = Icons.Default.AttachMoney,
        accentColor = FreshGreenTertiary,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Tabs: "Ringkasan & Terlaris" vs "Daftar Transaksi"
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Analisis Penjualan", fontWeight = FontWeight.Bold)
          }
        }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Semua Struk (${uiState.filteredOrders.size})", fontWeight = FontWeight.Bold)
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (selectedTab == 0) {
      // Analytics view
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Payment breakdown card
        item {
          PaymentMethodBreakdownCard(
            totalCash = uiState.salesSummary.totalCash,
            totalQris = uiState.salesSummary.totalQris,
            totalRevenue = uiState.salesSummary.totalRevenue
          )
        }

        // Top Selling Menu Items
        item {
          Text(
            text = "Menu Paling Laris (Top Terjual)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        if (uiState.topProducts.isEmpty()) {
          item {
            Text(
              text = "Belum ada penjualan dalam periode ini",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          items(uiState.topProducts.take(10)) { topProduct ->
            TopProductRow(product = topProduct)
          }
        }
      }
    } else {
      // Transactions List
      if (uiState.filteredOrders.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Tidak ada transaksi dalam periode ini",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("orders_history_list"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(uiState.filteredOrders, key = { it.order.id }) { orderWithItems ->
            TransactionOrderCard(
              orderWithItems = orderWithItems,
              onClick = {
                viewModel.showOrderReceipt(orderWithItems)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(accentColor.copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = accentColor
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun PaymentMethodBreakdownCard(
  totalCash: Double,
  totalQris: Double,
  totalRevenue: Double
) {
  val cashRatio = if (totalRevenue > 0) (totalCash / totalRevenue).toFloat() else 0f
  val qrisRatio = if (totalRevenue > 0) (totalQris / totalRevenue).toFloat() else 0f

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Text(
        text = "Distribusi Pembayaran",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(10.dp))

      // Cash Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Payments, contentDescription = null, tint = FreshGreenTertiary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tunai (Cash)", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
          text = "${formatRupiah(totalCash)} (${(cashRatio * 100).toInt()}%)",
          fontWeight = FontWeight.Bold,
          color = FreshGreenTertiary
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { cashRatio },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = FreshGreenTertiary,
        trackColor = FreshGreenTertiary.copy(alpha = 0.15f)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // QRIS Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.QrCode2, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("QRIS Digital", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
          text = "${formatRupiah(totalQris)} (${(qrisRatio * 100).toInt()}%)",
          fontWeight = FontWeight.Bold,
          color = TerracottaPrimary
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { qrisRatio },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = TerracottaPrimary,
        trackColor = TerracottaPrimary.copy(alpha = 0.15f)
      )
    }
  }
}

@Composable
private fun TopProductRow(product: TopProductSummary) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(TerracottaPrimary.copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${product.totalSold}",
            fontWeight = FontWeight.ExtraBold,
            color = TerracottaPrimary,
            fontSize = 14.sp
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = product.menuItemName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${product.totalSold} porsi terjual",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = formatRupiah(product.totalRevenue),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
private fun TransactionOrderCard(
  orderWithItems: OrderWithItems,
  onClick: () -> Unit
) {
  val order = orderWithItems.order
  val dateFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
  val timeStr = dateFmt.format(Date(order.timestamp))
  val isQris = order.paymentMethod == "QRIS"

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("order_item_${order.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = order.invoiceNumber,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "$timeStr • ${order.customerName} (${order.orderType})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Payment Method Badge
        Box(
          modifier = Modifier
            .background(
              if (isQris) TerracottaPrimary.copy(alpha = 0.12f) else FreshGreenTertiary.copy(alpha = 0.12f),
              shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = order.paymentMethod,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (isQris) TerracottaPrimary else FreshGreenTertiary
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Items summary
      val itemSummary = orderWithItems.items.joinToString(", ") { "${it.quantity}x ${it.menuItemName}" }
      Text(
        text = itemSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formatRupiah(order.finalAmount),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = TerracottaPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Print,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Lihat & Cetak",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

private fun formatRupiah(amount: Double): String {
  val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
  return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
