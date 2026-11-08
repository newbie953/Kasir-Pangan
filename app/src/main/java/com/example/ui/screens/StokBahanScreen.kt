package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.IngredientEntity
import com.example.data.model.StockLogEntity
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockGoodGreen
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StokBahanScreen(
  viewModel: PosViewModel,
  uiState: PosUiState
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var ingredientToRestock by remember { mutableStateOf<IngredientEntity?>(null) }
  var ingredientToEditThreshold by remember { mutableStateOf<IngredientEntity?>(null) }

  val totalIngredients = uiState.ingredients.size
  val criticalCount = uiState.ingredients.count { it.isCritical }
  val lowCount = uiState.ingredients.count { it.isLowStock && !it.isCritical }
  val goodCount = totalIngredients - (criticalCount + lowCount)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Top summary cards row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      SummaryMetricCard(
        title = "Aman",
        count = goodCount,
        color = StockGoodGreen,
        modifier = Modifier.weight(1f)
      )
      SummaryMetricCard(
        title = "Menipis",
        count = lowCount,
        color = StockLowAmber,
        modifier = Modifier.weight(1f)
      )
      SummaryMetricCard(
        title = "Kritis",
        count = criticalCount,
        color = StockCriticalRed,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Tabs
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
            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bahan Baku (${uiState.ingredients.size})", fontWeight = FontWeight.Bold)
          }
        }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Riwayat Stok", fontWeight = FontWeight.Bold)
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (selectedTab == 0) {
      // Ingredients List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("ingredients_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(uiState.ingredients, key = { it.id }) { ingredient ->
          IngredientItemCard(
            ingredient = ingredient,
            onRestockClick = { ingredientToRestock = ingredient },
            onEditThresholdClick = { ingredientToEditThreshold = ingredient }
          )
        }
      }
    } else {
      // Stock Logs Audit Trail
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("stock_logs_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(uiState.recentStockLogs, key = { it.id }) { log ->
          StockLogRow(log = log)
        }
      }
    }
  }

  // Restock Dialog
  ingredientToRestock?.let { ingredient ->
    RestockDialog(
      ingredient = ingredient,
      onConfirm = { amount, notes ->
        viewModel.restockIngredient(ingredient.id, amount, notes)
        ingredientToRestock = null
      },
      onDismiss = { ingredientToRestock = null }
    )
  }

  // Edit Threshold Dialog
  ingredientToEditThreshold?.let { ingredient ->
    EditThresholdDialog(
      ingredient = ingredient,
      onConfirm = { newThreshold ->
        viewModel.updateIngredientThreshold(ingredient.id, newThreshold)
        ingredientToEditThreshold = null
      },
      onDismiss = { ingredientToEditThreshold = null }
    )
  }
}

@Composable
private fun SummaryMetricCard(
  title: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = color
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color
      )
    }
  }
}

@Composable
private fun IngredientItemCard(
  ingredient: IngredientEntity,
  onRestockClick: () -> Unit,
  onEditThresholdClick: () -> Unit
) {
  val ratio = (ingredient.currentStock / ingredient.minThreshold).toFloat().coerceIn(0f, 2f)
  val progress = (ingredient.currentStock / (ingredient.minThreshold * 2.0)).toFloat().coerceIn(0f, 1f)

  val (statusLabel, statusColor) = when {
    ingredient.isCritical -> Pair("KRITIS", StockCriticalRed)
    ingredient.isLowStock -> Pair("MENIPIS", StockLowAmber)
    else -> Pair("AMAN", StockGoodGreen)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("ingredient_card_${ingredient.id}"),
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = ingredient.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Ambang Batas Minimum: ${ingredient.minThreshold.toClean()} ${ingredient.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Status Badge
        Box(
          modifier = Modifier
            .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = statusLabel,
            color = statusColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Current Stock display
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = ingredient.currentStock.toClean(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (ingredient.isLowStock) statusColor else MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = ingredient.unit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(
            onClick = onEditThresholdClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Ubah Batas", modifier = Modifier.size(18.dp))
          }

          Button(
            onClick = onRestockClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
            modifier = Modifier.testTag("restock_button_${ingredient.id}")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Restock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Stock progress bar
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp),
        color = statusColor,
        trackColor = statusColor.copy(alpha = 0.15f)
      )
    }
  }
}

@Composable
private fun StockLogRow(log: StockLogEntity) {
  val isPositive = log.changeAmount > 0
  val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
  val timeStr = dateFmt.format(Date(log.timestamp))

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
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
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(
              if (isPositive) FreshGreenTertiary.copy(alpha = 0.15f) else TerracottaPrimary.copy(alpha = 0.15f),
              RoundedCornerShape(8.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = if (isPositive) FreshGreenTertiary else TerracottaPrimary,
            modifier = Modifier.size(16.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = log.ingredientName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${log.reason} • $timeStr",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (isPositive) "+${log.changeAmount.toClean()} ${log.unit}" else "${log.changeAmount.toClean()} ${log.unit}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.ExtraBold,
          color = if (isPositive) FreshGreenTertiary else TerracottaPrimary
        )
        Text(
          text = "Sisa: ${log.remainingStock.toClean()}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun RestockDialog(
  ingredient: IngredientEntity,
  onConfirm: (amount: Double, notes: String) -> Unit,
  onDismiss: () -> Unit
) {
  var amountText by remember { mutableStateOf("") }
  var notesText by remember { mutableStateOf("Restock Pembelian Baru") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("restock_modal_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = "Tambah Stok Masuk",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${ingredient.name} (Satuan: ${ingredient.unit})",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Jumlah Masuk (${ingredient.unit})") },
          placeholder = { Text("Contoh: 1000") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("restock_amount_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = notesText,
          onValueChange = { notesText = it },
          label = { Text("Keterangan / Sumber") },
          placeholder = { Text("Contoh: Toko Bahan Kue Barokah") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
            Text("Batal")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val amt = amountText.toDoubleOrNull() ?: 0.0
              if (amt > 0) onConfirm(amt, notesText)
            },
            enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
            modifier = Modifier.testTag("confirm_restock_button")
          ) {
            Text("Simpan Stok")
          }
        }
      }
    }
  }
}

@Composable
private fun EditThresholdDialog(
  ingredient: IngredientEntity,
  onConfirm: (newThreshold: Double) -> Unit,
  onDismiss: () -> Unit
) {
  var thresholdText by remember { mutableStateOf(ingredient.minThreshold.toClean()) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = "Ubah Ambang Batas Notifikasi",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Peringatan akan otomatis berbunyi jika stok ${ingredient.name} berada di bawah angka ini.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = thresholdText,
          onValueChange = { thresholdText = it },
          label = { Text("Batas Minimum (${ingredient.unit})") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
            Text("Batal")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val thresh = thresholdText.toDoubleOrNull() ?: 0.0
              if (thresh > 0) onConfirm(thresh)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
          ) {
            Text("Perbarui")
          }
        }
      }
    }
  }
}

private fun Double.toClean(): String {
  return if (this % 1.0 == 0.0) this.toLong().toString() else String.format("%.1f", this)
}
