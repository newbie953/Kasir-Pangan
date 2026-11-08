package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.TerracottaPrimary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CashPaymentDialog(
  totalAmount: Double,
  onConfirmSuccess: (cashReceived: Double) -> Unit,
  onDismiss: () -> Unit
) {
  var cashInputText by remember { mutableStateOf("") }

  val cashGiven = cashInputText.toDoubleOrNull() ?: 0.0
  val changeAmount = cashGiven - totalAmount
  val isSufficient = cashGiven >= totalAmount

  // Common denominations
  val suggestions = remember(totalAmount) {
    val list = mutableListOf<Double>()
    list.add(totalAmount) // Uang Pas
    val nextRound50k = kotlin.math.ceil(totalAmount / 50000.0) * 50000.0
    val nextRound100k = kotlin.math.ceil(totalAmount / 100000.0) * 100000.0
    if (nextRound50k > totalAmount && !list.contains(nextRound50k)) list.add(nextRound50k)
    if (nextRound100k > totalAmount && !list.contains(nextRound100k)) list.add(nextRound100k)
    if (totalAmount < 50000.0 && !list.contains(50000.0)) list.add(50000.0)
    if (totalAmount < 100000.0 && !list.contains(100000.0)) list.add(100000.0)
    list.take(4)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 16.dp)
        .testTag("cash_payment_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Payments,
              contentDescription = null,
              tint = FreshGreenTertiary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Pembayaran Tunai",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_cash_button")) {
            Icon(Icons.Default.Close, contentDescription = "Tutup")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Tagihan
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Total Tagihan",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = formatRupiah(totalAmount),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = TerracottaPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Suggestion Chips
        Text(
          text = "Pilih Cepat Uang Diterima:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          suggestions.forEach { amount ->
            val label = if (amount == totalAmount) "Uang Pas" else formatShortRupiah(amount)
            FilterChip(
              selected = (cashInputText == amount.toLong().toString()),
              onClick = { cashInputText = amount.toLong().toString() },
              label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FreshGreenTertiary.copy(alpha = 0.2f),
                selectedLabelColor = FreshGreenTertiary
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manual Input Field
        OutlinedTextField(
          value = cashInputText,
          onValueChange = { input ->
            if (input.all { it.isDigit() }) {
              cashInputText = input
            }
          },
          label = { Text("Jumlah Uang Diterima (Rp)") },
          placeholder = { Text("0") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_received_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Change Calculation Box
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = if (isSufficient) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isSufficient) "Kembalian:" else "Kurang Bayar:",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = if (isSufficient) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Text(
              text = if (isSufficient) formatRupiah(changeAmount) else formatRupiah(kotlin.math.abs(changeAmount)),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = if (isSufficient) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Confirm Button
        Button(
          onClick = {
            onConfirmSuccess(cashGiven)
          },
          enabled = isSufficient,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_cash_payment_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
        ) {
          Text("Selesaikan Transaksi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}

private fun formatRupiah(amount: Double): String {
  val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
  return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}

private fun formatShortRupiah(amount: Double): String {
  val ribuan = (amount / 1000).toLong()
  return "${ribuan}k"
}
