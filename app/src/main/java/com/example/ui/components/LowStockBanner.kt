package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IngredientEntity
import com.example.ui.theme.StockCriticalRed
import com.example.ui.theme.StockLowAmber

@Composable
fun LowStockBanner(
  lowStockItems: List<IngredientEntity>,
  onBannerClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = lowStockItems.isNotEmpty(),
    enter = expandVertically(),
    exit = shrinkVertically(),
    modifier = modifier
  ) {
    val criticalCount = lowStockItems.count { it.isCritical }
    val isAnyCritical = criticalCount > 0

    val bgColor = if (isAnyCritical) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
    val accentColor = if (isAnyCritical) StockCriticalRed else StockLowAmber

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clickable { onBannerClick() }
        .testTag("low_stock_banner"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = bgColor)
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
              .size(36.dp)
              .background(accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = if (isAnyCritical)
                "🚨 $criticalCount Bahan Kritis & ${lowStockItems.size} Menipis!"
              else
                "⚠️ ${lowStockItems.size} Bahan Baku Menipis",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = accentColor
            )
            val names = lowStockItems.take(3).joinToString(", ") { "${it.name} (${it.currentStock.toClean()} ${it.unit})" }
            val suffix = if (lowStockItems.size > 3) "..." else ""
            Text(
              text = "$names$suffix",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Restock",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor
          )
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

private fun Double.toClean(): String {
  return if (this % 1.0 == 0.0) this.toLong().toString() else String.format("%.1f", this)
}
