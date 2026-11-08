package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Language
import com.example.service.StockNotificationHelper
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.screens.KasirScreen
import com.example.ui.screens.LaporanScreen
import com.example.ui.screens.PengaturanScreen
import com.example.ui.screens.StokBahanScreen
import com.example.ui.screens.WebAppHostingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

  private val viewModel: PosViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize notification channel for low stock alerts
    StockNotificationHelper.createNotificationChannel(this)

    // Check if opened from low stock notification
    handleIntent(intent)

    setContent {
      MyApplicationTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // Collect snackbar events
        LaunchedEffect(Unit) {
          viewModel.snackbarEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
          }
        }

        // BackHandler: navigate back to Kasir tab if on other tabs
        BackHandler(enabled = uiState.currentTab != AppTab.KASIR) {
          viewModel.setTab(AppTab.KASIR)
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            if (uiState.currentTab != AppTab.WEB_APP) {
              CenterAlignedTopAppBar(
                title = {
                  Text(
                    text = "DapurKasir",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                  )
                },
              navigationIcon = {
                Icon(
                  imageVector = Icons.Default.Restaurant,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(start = 16.dp).size(24.dp)
                )
              },
              actions = {
                if (uiState.lowStockAlerts.isNotEmpty()) {
                  IconButton(
                    onClick = { viewModel.setTab(AppTab.STOK_BAHAN) },
                    modifier = Modifier.testTag("top_bar_low_stock_button")
                  ) {
                    BadgedBox(
                      badge = {
                        Badge(containerColor = StockCriticalRed) {
                          Text("${uiState.lowStockAlerts.size}", color = Color.White)
                        }
                      }
                    ) {
                      Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan Stok",
                        tint = MaterialTheme.colorScheme.onPrimary
                      )
                    }
                  }
                }
              },
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = TerracottaPrimary
              )
            )
          }
        },
          bottomBar = {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 8.dp
            ) {
              // 1. Kasir
              NavigationBarItem(
                selected = uiState.currentTab == AppTab.KASIR,
                onClick = { viewModel.setTab(AppTab.KASIR) },
                icon = {
                  Icon(
                    if (uiState.currentTab == AppTab.KASIR) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale,
                    contentDescription = "Kasir"
                  )
                },
                label = { Text("Kasir", fontWeight = if (uiState.currentTab == AppTab.KASIR) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = TerracottaPrimary.copy(alpha = 0.15f),
                  selectedIconColor = TerracottaPrimary,
                  selectedTextColor = TerracottaPrimary
                ),
                modifier = Modifier.testTag("nav_item_kasir")
              )

              // 2. Stok Bahan
              NavigationBarItem(
                selected = uiState.currentTab == AppTab.STOK_BAHAN,
                onClick = { viewModel.setTab(AppTab.STOK_BAHAN) },
                icon = {
                  if (uiState.lowStockAlerts.isNotEmpty()) {
                    BadgedBox(
                      badge = {
                        Badge(containerColor = StockLowAmber) {
                          Text("${uiState.lowStockAlerts.size}", color = Color.White)
                        }
                      }
                    ) {
                      Icon(
                        if (uiState.currentTab == AppTab.STOK_BAHAN) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                        contentDescription = "Stok Bahan"
                      )
                    }
                  } else {
                    Icon(
                      if (uiState.currentTab == AppTab.STOK_BAHAN) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                      contentDescription = "Stok Bahan"
                    )
                  }
                },
                label = { Text("Stok Bahan", fontWeight = if (uiState.currentTab == AppTab.STOK_BAHAN) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = TerracottaPrimary.copy(alpha = 0.15f),
                  selectedIconColor = TerracottaPrimary,
                  selectedTextColor = TerracottaPrimary
                ),
                modifier = Modifier.testTag("nav_item_stok")
              )

              // 3. Laporan
              NavigationBarItem(
                selected = uiState.currentTab == AppTab.LAPORAN,
                onClick = { viewModel.setTab(AppTab.LAPORAN) },
                icon = {
                  Icon(
                    if (uiState.currentTab == AppTab.LAPORAN) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                    contentDescription = "Laporan"
                  )
                },
                label = { Text("Laporan", fontWeight = if (uiState.currentTab == AppTab.LAPORAN) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = TerracottaPrimary.copy(alpha = 0.15f),
                  selectedIconColor = TerracottaPrimary,
                  selectedTextColor = TerracottaPrimary
                ),
                modifier = Modifier.testTag("nav_item_laporan")
              )

              // 4. Web App & Hosting
              val pendingWebOrdersCount = uiState.incomingWebOrders.count { it.status == "MENUNGGU" }
              NavigationBarItem(
                selected = uiState.currentTab == AppTab.WEB_APP,
                onClick = { viewModel.setTab(AppTab.WEB_APP) },
                icon = {
                  if (pendingWebOrdersCount > 0) {
                    BadgedBox(
                      badge = {
                        Badge(containerColor = TerracottaPrimary) {
                          Text("$pendingWebOrdersCount", color = Color.White)
                        }
                      }
                    ) {
                      Icon(
                        if (uiState.currentTab == AppTab.WEB_APP) Icons.Filled.Language else Icons.Outlined.Language,
                        contentDescription = "Web App"
                      )
                    }
                  } else {
                    Icon(
                      if (uiState.currentTab == AppTab.WEB_APP) Icons.Filled.Language else Icons.Outlined.Language,
                      contentDescription = "Web App"
                    )
                  }
                },
                label = { Text("Web App", fontWeight = if (uiState.currentTab == AppTab.WEB_APP) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = TerracottaPrimary.copy(alpha = 0.15f),
                  selectedIconColor = TerracottaPrimary,
                  selectedTextColor = TerracottaPrimary
                ),
                modifier = Modifier.testTag("nav_item_web_app")
              )

              // 5. Pengaturan
              NavigationBarItem(
                selected = uiState.currentTab == AppTab.PENGATURAN,
                onClick = { viewModel.setTab(AppTab.PENGATURAN) },
                icon = {
                  Icon(
                    if (uiState.currentTab == AppTab.PENGATURAN) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Pengaturan"
                  )
                },
                label = { Text("Printer", fontWeight = if (uiState.currentTab == AppTab.PENGATURAN) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = TerracottaPrimary.copy(alpha = 0.15f),
                  selectedIconColor = TerracottaPrimary,
                  selectedTextColor = TerracottaPrimary
                ),
                modifier = Modifier.testTag("nav_item_pengaturan")
              )
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            AnimatedContent(
              targetState = uiState.currentTab,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "tab_transition"
            ) { tab ->
              when (tab) {
                AppTab.KASIR -> KasirScreen(viewModel = viewModel, uiState = uiState)
                AppTab.STOK_BAHAN -> StokBahanScreen(viewModel = viewModel, uiState = uiState)
                AppTab.LAPORAN -> LaporanScreen(viewModel = viewModel, uiState = uiState)
                AppTab.WEB_APP -> WebAppHostingScreen(
                  viewModel = viewModel,
                  uiState = uiState,
                  onNavigateBack = { viewModel.setTab(AppTab.KASIR) }
                )
                AppTab.PENGATURAN -> PengaturanScreen(viewModel = viewModel, uiState = uiState)
              }
            }
          }
        }

        // Thermal Receipt Dialog modal
        if (uiState.isReceiptModalOpen && uiState.lastCompletedOrder != null) {
          ThermalReceiptDialog(
            orderWithItems = uiState.lastCompletedOrder!!,
            merchantInfo = uiState.merchantInfo,
            selectedPrinterAddress = uiState.selectedPrinterAddress,
            isPrinting = uiState.isPrinting,
            onPrintClick = {
              viewModel.printReceipt(uiState.lastCompletedOrder!!)
            },
            onDismiss = {
              viewModel.closeReceiptModal()
            }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getStringExtra("NAVIGATE_TO") == "STOK_BAHAN") {
      viewModel.setTab(AppTab.STOK_BAHAN)
    }
  }
}
