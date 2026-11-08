package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OrderWithItems
import com.example.service.EscPosPrinterService
import com.example.ui.theme.TerracottaPrimary

@Composable
fun ThermalReceiptDialog(
  orderWithItems: OrderWithItems,
  merchantInfo: EscPosPrinterService.MerchantInfo,
  selectedPrinterAddress: String?,
  isPrinting: Boolean,
  onPrintClick: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val receiptText = EscPosPrinterService.formatPlainTextReceipt(orderWithItems, merchantInfo)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 24.dp)
        .testTag("thermal_receipt_dialog"),
      shape = RoundedCornerShape(20.dp),
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
              imageVector = Icons.Default.Print,
              contentDescription = null,
              tint = TerracottaPrimary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Struk Pembayaran",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_receipt_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thermal paper view container
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .heightIn(max = 420.dp),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFDFBF7) // Thermal paper off-white
          ),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE5DDD5)))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(16.dp)
          ) {
            // Receipt Content in Monospace font
            Text(
              text = receiptText,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              lineHeight = 17.sp,
              color = Color(0xFF1E1E1E),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bluetooth status badge
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              color = if (selectedPrinterAddress != null) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
              shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selectedPrinterAddress != null) Color(0xFF2E7D32) else Color(0xFFEF6C00)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (selectedPrinterAddress != null)
              "Printer Terhubung: ${selectedPrinterAddress.take(14)}..."
            else
              "Printer Belum Dipilih (Simulasi Print Aktif)",
            style = MaterialTheme.typography.bodySmall,
            color = if (selectedPrinterAddress != null) Color(0xFF2E7D32) else Color(0xFFEF6C00),
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Struk Kasir", receiptText)
              clipboard.setPrimaryClip(clip)
            },
            modifier = Modifier
              .weight(1f)
              .testTag("copy_receipt_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Salin Teks", fontSize = 13.sp)
          }

          Button(
            onClick = onPrintClick,
            enabled = !isPrinting,
            modifier = Modifier
              .weight(1.3f)
              .testTag("print_receipt_bluetooth_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
          ) {
            if (isPrinting) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Mencetak...")
            } else {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Cetak Struk", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
