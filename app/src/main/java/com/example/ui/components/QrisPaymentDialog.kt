package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.QrisService
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.TerracottaPrimary
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@Composable
fun QrisPaymentDialog(
  amount: Double,
  onConfirmSuccess: (qrisRef: String) -> Unit,
  onDismiss: () -> Unit
) {
  var remainingSeconds by remember { mutableIntStateOf(300) } // 5 minutes timer
  val qrCodeBitmap: ImageBitmap = remember(amount) {
    val dummyInvoice = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
    val payload = QrisService.generateQrisPayload(amount, dummyInvoice)
    QrisService.generateQrBitmap(payload, 450)
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  LaunchedEffect(Unit) {
    while (remainingSeconds > 0) {
      delay(1000)
      remainingSeconds -= 1
    }
  }

  val minutes = remainingSeconds / 60
  val seconds = remainingSeconds % 60
  val formattedTime = String.format("%02d:%02d", minutes, seconds)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 16.dp)
        .testTag("qris_payment_dialog"),
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
        // Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .background(Color(0xFFE53935), shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "QRIS",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Pembayaran Digital",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("qris_close_button")) {
            Icon(Icons.Default.Close, contentDescription = "Tutup")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Amount Display
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Total Tagihan Pembeli",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = formatRupiah(amount),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = TerracottaPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "DAPUR KULINER NUSANTARA • NMID: ID1020261028001",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR Code Container
        Box(
          modifier = Modifier
            .size(240.dp)
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .padding(12.dp),
          contentAlignment = Alignment.Center
        ) {
          Image(
            bitmap = qrCodeBitmap,
            contentDescription = "QRIS QR Code",
            modifier = Modifier.size(216.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status & Timer
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .alpha(pulseAlpha)
              .background(FreshGreenTertiary, shape = CircleShape)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Menunggu Pelanggan Scan • Berakhir dalam $formattedTime",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Button(
          onClick = {
            val randomRef = "QRIS-${(10000000..99999999).random()}"
            onConfirmSuccess(randomRef)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("simulate_qris_paid_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = FreshGreenTertiary)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Konfirmasi Pembayaran Diterima", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("cancel_qris_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("Ganti Metode Bayar")
        }
      }
    }
  }
}

private fun formatRupiah(amount: Double): String {
  val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
  return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
