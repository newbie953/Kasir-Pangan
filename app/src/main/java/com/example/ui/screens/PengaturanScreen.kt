package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.service.EscPosPrinterService
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@Composable
fun PengaturanScreen(
  viewModel: PosViewModel,
  uiState: PosUiState
) {
  var merchantName by remember { mutableStateOf(uiState.merchantInfo.name) }
  var merchantAddress by remember { mutableStateOf(uiState.merchantInfo.address) }
  var merchantPhone by remember { mutableStateOf(uiState.merchantInfo.phone) }
  var merchantFooter by remember { mutableStateOf(uiState.merchantInfo.footerMessage) }

  // Permission launcher for Bluetooth & Notifications on Android 12+ / 13+
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ ->
    viewModel.refreshPairedPrinters()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Bluetooth Printer Section
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("bluetooth_settings_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(TerracottaPrimary.copy(alpha = 0.12f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Bluetooth, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Printer Thermal Bluetooth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Mendukung printer ESC/POS 58mm & 80mm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                  )
                )
              } else {
                viewModel.refreshPairedPrinters()
              }
            },
            modifier = Modifier.testTag("refresh_printers_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Pindai Ulang")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Paired Devices List
        if (uiState.pairedPrinters.isEmpty()) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
            ) {
              Text(
                text = "Belum ada printer Bluetooth yang dipasangkan.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Nyalakan Bluetooth printer Anda, pasangkan (pair) di Pengaturan Bluetooth HP, lalu tekan tombol Refresh di atas. Mode simulasi struk otomatis aktif saat printer belum terhubung.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            uiState.pairedPrinters.forEach { device ->
              val isSelected = uiState.selectedPrinterAddress == device.address
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(
                    if (isSelected) TerracottaPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(10.dp)
                  )
                  .clickable { viewModel.selectPrinter(device.address) }
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                if (isSelected) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "Terpilih",
                      color = TerracottaPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      Icons.Default.Check,
                      contentDescription = null,
                      tint = TerracottaPrimary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Test Print Button
        OutlinedButton(
          onClick = {
            val testOrder = OrderWithItems(
              order = OrderEntity(
                invoiceNumber = "TEST-PRINT-001",
                timestamp = System.currentTimeMillis(),
                totalAmount = 35000.0,
                finalAmount = 35000.0,
                paymentMethod = "TUNAI",
                cashReceived = 50000.0,
                cashChange = 15000.0,
                customerName = "Pelanggan Tes",
                orderType = "Dine In"
              ),
              items = listOf(
                OrderItemEntity(
                  orderId = 0,
                  menuItemId = 1,
                  menuItemName = "Nasi Goreng Spesial",
                  unitPrice = 25000.0,
                  quantity = 1,
                  subtotal = 25000.0
                ),
                OrderItemEntity(
                  orderId = 0,
                  menuItemId = 9,
                  menuItemName = "Es Jeruk Peras Segar",
                  unitPrice = 10000.0,
                  quantity = 1,
                  subtotal = 10000.0
                )
              )
            )
            viewModel.showOrderReceipt(testOrder)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("test_print_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Uji Coba Cetak Struk (Test Print)")
        }
      }
    }

    // Merchant Profile Editor Section
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(FreshGreenTertiary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Store, contentDescription = null, tint = FreshGreenTertiary, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Informasi Usaha Kuliner",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedTextField(
          value = merchantName,
          onValueChange = { merchantName = it },
          label = { Text("Nama Rumah Makan / Warung") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = merchantAddress,
          onValueChange = { merchantAddress = it },
          label = { Text("Alamat") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = merchantPhone,
          onValueChange = { merchantPhone = it },
          label = { Text("Nomor Telepon / WhatsApp") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = merchantFooter,
          onValueChange = { merchantFooter = it },
          label = { Text("Pesan Kaki Struk (Footer)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        Button(
          onClick = {
            viewModel.updateMerchantInfo(
              EscPosPrinterService.MerchantInfo(
                name = merchantName,
                address = merchantAddress,
                phone = merchantPhone,
                footerMessage = merchantFooter
              )
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_merchant_info_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Simpan Informasi Struk")
        }
      }
    }

    // System Notifications info
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(Color(0xFFEF6C00).copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Sistem Notifikasi Stok Real-Time",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Notifikasi otomatis aktif setiap kali transaksi mengurangi stok bahan baku di bawah ambang batas minimum.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
