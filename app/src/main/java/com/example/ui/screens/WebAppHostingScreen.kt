package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.WebOrder
import com.example.service.QrisService
import com.example.ui.theme.FreshGreenTertiary
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockGoodGreen
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WebAppHostingScreen(
  viewModel: PosViewModel,
  uiState: PosUiState,
  onNavigateBack: () -> Unit = {}
) {
  // Tabs: 0: Semua, 1: Web UI Komputer (LAN), 2: Google Drive & Gmail, 3: Firebase Hosting & DB (Default sesuai screenshot)
  var selectedTab by remember { mutableIntStateOf(3) }
  val context = LocalContext.current

  val serverBaseUrl = if (uiState.localServerIp != "127.0.0.1") {
    "http://${uiState.localServerIp}:${uiState.webServerPort}"
  } else {
    "http://localhost:${uiState.webServerPort}"
  }
  val webReportsUrl = "$serverBaseUrl/laporan"

  val pendingOrdersCount = uiState.incomingWebOrders.count { it.status == "MENUNGGU" }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF141523))
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // 1. TOP HEADER (Sesuai Screenshot: Arrow, Title, Subtitle & Badge 'Firebase Siap')
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { onNavigateBack() },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Kembali",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Hosting & Cadangan Cloud",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "Google Drive, Gmail & Firebase Hosting Realtime",
            fontSize = 12.sp,
            color = Color(0xFF9EA0BC)
          )
        }
      }

      // Badge "Firebase Siap"
      Box(
        modifier = Modifier
          .background(Color(0xFF3E2319), RoundedCornerShape(8.dp))
          .border(BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Firebase Siap",
            color = Color(0xFFFFB74D),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 2. FILTER PILLS NAVIGATION TABS (Sesuai Screenshot: Semua, Web UI LAN, Google Drive, Firebase Hosting)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Tab 0: Semua
      FilterPill(
        title = "Semua",
        icon = Icons.Default.GridView,
        isSelected = selectedTab == 0,
        onClick = { selectedTab = 0 }
      )
      // Tab 1: Web UI Komputer (LAN)
      FilterPill(
        title = "Web UI Komputer (LAN)",
        icon = Icons.Default.DesktopWindows,
        isSelected = selectedTab == 1,
        onClick = { selectedTab = 1 }
      )
      // Tab 2: Google Drive & Gmail
      FilterPill(
        title = "Google Drive & Gmail",
        icon = Icons.Default.FolderOpen,
        isSelected = selectedTab == 2,
        onClick = { selectedTab = 2 }
      )
      // Tab 3: Firebase Hosting & DB (Active in screenshot)
      FilterPill(
        title = "Firebase Hosting & DB",
        icon = Icons.Default.LocalFireDepartment,
        isSelected = selectedTab == 3,
        onClick = { selectedTab = 3 }
      )
    }

    // 3. MAIN CONTENT CARDS
    // CARD: FIREBASE REALTIME DB & WEB HOSTING (Ditampilkan saat tab 3 atau tab 0)
    if (selectedTab == 3 || selectedTab == 0) {
      FirebaseHostingCard(
        uiState = uiState,
        webReportsUrl = webReportsUrl,
        onToggleBackground = { viewModel.toggleBackgroundService(!uiState.isBackgroundServiceRunning) },
        onSyncNow = { viewModel.syncStoreClosingToCloud() },
        onOpenUrl = { url ->
          try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
          } catch (e: Exception) {
            // handle error
          }
        },
        onCopyUrl = { url ->
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText("URL Web Laporan", url)
          clipboard.setPrimaryClip(clip)
        }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    // CARD: WEB UI KOMPUTER (LAN) & PESANAN (Ditampilkan saat tab 1 atau tab 0)
    if (selectedTab == 1 || selectedTab == 0) {
      LanWebHostingCard(
        uiState = uiState,
        serverBaseUrl = serverBaseUrl,
        pendingOrdersCount = pendingOrdersCount,
        onToggleServer = { enabled -> viewModel.toggleWebServer(enabled) },
        onOpenUrl = { url ->
          try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
          } catch (e: Exception) {
            // handle error
          }
        },
        onAcceptOrder = { order -> viewModel.acceptWebOrder(order) },
        onRejectOrder = { orderId -> viewModel.rejectWebOrder(orderId) }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    // CARD: GOOGLE DRIVE & GMAIL BACKUP (Ditampilkan saat tab 2 atau tab 0)
    if (selectedTab == 2 || selectedTab == 0) {
      GoogleDriveGmailCard(
        uiState = uiState,
        onSendEmail = {
          val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Tutup Toko DapurKasir - ${uiState.lastSyncTimestamp}")
            putExtra(
              Intent.EXTRA_TEXT,
              """
              Laporan Tutup Toko DapurKasir
              Tanggal: ${uiState.lastSyncTimestamp}
              Total Omset: ${formatRupiah(uiState.salesSummary.totalRevenue)}
              Estimasi Laba Bersih: ${formatRupiah(uiState.salesSummary.estimatedProfit)}
              Total Transaksi: ${uiState.salesSummary.totalOrders} nota
              Pembayaran Tunai: ${formatRupiah(uiState.salesSummary.totalCash)}
              Pembayaran QRIS: ${formatRupiah(uiState.salesSummary.totalQris)}
              
              Status Database: Firebase Firestore Synced
              """.trimIndent()
            )
          }
          context.startActivity(Intent.createChooser(sendIntent, "Kirim Laporan via Gmail / Email"))
        },
        onSaveDrive = {
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Cadangan Data Penjualan & Laporan DapurKasir")
            putExtra(
              Intent.EXTRA_TEXT,
              "Cadangan Data Transaksi Penjualan Cloud\nTanggal: ${uiState.lastSyncTimestamp}\nTotal Nota: ${uiState.syncedCloudNotesCount} nota"
            )
          }
          context.startActivity(Intent.createChooser(shareIntent, "Simpan ke Google Drive"))
        }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

// ==========================================
// SUB-COMPONENT: FILTER PILL TAB
// ==========================================
@Composable
private fun FilterPill(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .background(
        if (isSelected) Color(0xFFE65100) else Color(0xFF222438),
        RoundedCornerShape(12.dp)
      )
      .border(
        BorderStroke(
          1.dp,
          if (isSelected) Color(0xFFFF9800) else Color(0xFF383A52)
        ),
        RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) Color.White else Color(0xFFB0B2C9),
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = title,
        color = if (isSelected) Color.White else Color(0xFFB0B2C9),
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      )
    }
  }
}

// ==========================================
// CARD: FIREBASE REALTIME DB & WEB HOSTING (Sesuai Persis Tangkapan Layar)
// ==========================================
@Composable
private fun FirebaseHostingCard(
  uiState: PosUiState,
  webReportsUrl: String,
  onToggleBackground: () -> Unit,
  onSyncNow: () -> Unit,
  onOpenUrl: (String) -> Unit,
  onCopyUrl: (String) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("firebase_realtime_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2034)),
    border = BorderStroke(1.5.dp, Color(0xFFE65100).copy(alpha = 0.85f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header Card
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // Flame Avatar Circle
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(Color(0xFF3E2319), CircleShape)
            .border(BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.6f)), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "FIREBASE REALTIME DB & WEB HOSTING",
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFF9800)
            )
            // Green Pill: SYNC REALTIME
            Box(
              modifier = Modifier
                .background(Color(0xFF1B5E20), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "SYNC REALTIME",
                color = Color(0xFF81C784),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Sinkronisasi otomatis ke Google Firebase Realtime DB & Web Hosting publik",
            fontSize = 12.sp,
            color = Color(0xFF9FA2BD)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // INNER DARK CARD CONTAINER (Sesuai Gambar Persis)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF131422), RoundedCornerShape(12.dp))
          .border(BorderStroke(1.dp, Color(0xFF2C2E46)), RoundedCornerShape(12.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Baris 1: Status Sinkronisasi Terakhir
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Status Sinkronisasi Terakhir:",
              fontSize = 12.5.sp,
              color = Color(0xFFC0C2D6)
            )
            Text(
              text = uiState.lastSyncTimestamp,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          // Baris 2: Teks Hijau Laporan Tutup Toko
          Text(
            text = uiState.lastStoreClosingReport,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4CAF50),
            lineHeight = 17.sp
          )

          // Baris 3: Total Nota Tersimpan di Cloud
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Total Nota Tersimpan di Cloud:",
              fontSize = 12.5.sp,
              color = Color(0xFFC0C2D6)
            )
            Text(
              text = "${uiState.syncedCloudNotesCount} nota",
              fontSize = 14.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFFFFA726)
            )
          }

          // Baris 4: Layanan Background Service
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Layanan Background Service:",
              fontSize = 12.5.sp,
              color = Color(0xFFC0C2D6)
            )
            Box(
              modifier = Modifier
                .background(
                  if (uiState.isBackgroundServiceRunning) Color(0xFF1B5E20) else Color(0xFF37474F),
                  RoundedCornerShape(8.dp)
                )
                .clickable { onToggleBackground() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = if (uiState.isBackgroundServiceRunning) "AKTIF (BACKGROUND ON)" else "OFF (NONAKTIF)",
                color = if (uiState.isBackgroundServiceRunning) Color(0xFFC8E6C9) else Color(0xFFB0BEC5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // URL WEB HOSTING LAPORAN & ACTION BUTTONS
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF26283E), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "URL Web Hosting Laporan Transaksi & Laba",
              fontSize = 11.sp,
              color = Color(0xFFA0A3BE),
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = webReportsUrl,
              fontSize = 13.sp,
              color = Color(0xFFFFCC80),
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
          IconButton(onClick = { onCopyUrl(webReportsUrl) }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Salin URL", tint = Color.White)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Buka Web Hosting Laporan
        Button(
          onClick = { onOpenUrl(webReportsUrl) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Buka Web Hosting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Sinkronkan Sekarang
        OutlinedButton(
          onClick = { onSyncNow() },
          border = BorderStroke(1.dp, Color(0xFFFF9800)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          if (uiState.isSyncingNow) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFFFB74D), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sinkronkan Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// ==========================================
// CARD: WEB UI KOMPUTER (LAN) & PESANAN PELANGGAN
// ==========================================
@Composable
private fun LanWebHostingCard(
  uiState: PosUiState,
  serverBaseUrl: String,
  pendingOrdersCount: Int,
  onToggleServer: (Boolean) -> Unit,
  onOpenUrl: (String) -> Unit,
  onAcceptOrder: (WebOrder) -> Unit,
  onRejectOrder: (String) -> Unit
) {
  val qrBitmap = remember(serverBaseUrl) {
    QrisService.generateQrBitmap(serverBaseUrl, 260)
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2034)),
    border = BorderStroke(1.dp, Color(0xFF383A54))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(Color(0xFF233244), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Web UI Komputer (LAN) & Self-Order", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Form pemesanan mandiri & kasir di jaringan WiFi", color = Color(0xFF9FA2BD), fontSize = 11.5.sp)
          }
        }
        Switch(
          checked = uiState.isWebServerRunning,
          onCheckedChange = { onToggleServer(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFF4CAF50)
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Info Baris & Link
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF131422), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Column {
          Text("Alamat Akses Pelanggan (Meja / Kasir LAN):", fontSize = 11.sp, color = Color(0xFF9FA2BD))
          Text(serverBaseUrl, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA), fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onOpenUrl(serverBaseUrl) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Buka Form Pemesanan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // QR Code Box
      if (qrBitmap != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(110.dp)
              .background(Color.White, RoundedCornerShape(12.dp))
              .padding(6.dp)
          ) {
            Image(bitmap = qrBitmap, contentDescription = "QR Form Pemesanan", modifier = Modifier.fillMaxSize())
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text("Scan QR di Meja Pelanggan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Pelanggan tinggal scan barcode ini di meja untuk langsung membuka form menu & order.", color = Color(0xFF9FA2BD), fontSize = 11.5.sp)
          }
        }
      }

      // Incoming Orders Section
      if (uiState.incomingWebOrders.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("Pesanan Masuk dari Web (${uiState.incomingWebOrders.size}):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          uiState.incomingWebOrders.take(3).forEach { order ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF26283E), RoundedCornerShape(10.dp))
                .padding(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("${order.id} • ${order.customerName} (${order.tableOrAddress})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text("${order.items.size} Menu • ${formatRupiah(order.totalAmount)}", color = Color(0xFFFFB74D), fontSize = 11.sp)
                }
                if (order.status == "MENUNGGU") {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                      onClick = { onAcceptOrder(order) },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text("Terima", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                      onClick = { onRejectOrder(order.id) },
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text("Tolak", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                } else {
                  Text(order.status, color = if (order.status == "DITERIMA") Color(0xFF81C784) else Color(0xFFE57373), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

// ==========================================
// CARD: GOOGLE DRIVE & GMAIL BACKUP
// ==========================================
@Composable
private fun GoogleDriveGmailCard(
  uiState: PosUiState,
  onSendEmail: () -> Unit,
  onSaveDrive: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2034)),
    border = BorderStroke(1.dp, Color(0xFF383A54))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .background(Color(0xFF372F44), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Google Drive & Gmail", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("Kirim rekap laporan tutup toko & cadangkan ke cloud", color = Color(0xFF9FA2BD), fontSize = 11.5.sp)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF131422), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Laporan Tutup Toko Siap Dikirim:", fontSize = 12.sp, color = Color(0xFFC0C2D6), fontWeight = FontWeight.Bold)
          Text("• Total Omset: ${formatRupiah(uiState.salesSummary.totalRevenue)}", fontSize = 12.sp, color = Color.White)
          Text("• Laba Bersih: ${formatRupiah(uiState.salesSummary.estimatedProfit)}", fontSize = 12.sp, color = Color(0xFF81C784))
          Text("• Transaksi Selesai: ${uiState.salesSummary.totalOrders} nota", fontSize = 12.sp, color = Color(0xFFFFB74D))
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { onSendEmail() },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Kirim via Gmail", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = { onSaveDrive() },
          border = BorderStroke(1.dp, Color(0xFFBA68C8)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCE93D8)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Simpan ke Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

private fun formatRupiah(amount: Double): String {
  val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
  return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
