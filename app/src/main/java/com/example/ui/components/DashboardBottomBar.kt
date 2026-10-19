package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebDarkBlue
import com.example.ui.theme.MohasebPrimaryBlue

@Composable
fun DashboardBottomBar(
  currentWarehouse: String,
  onWarehouseClick: () -> Unit,
  onYouTubeClick: () -> Unit,
  onRefreshClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp),
    color = MohasebPrimaryBlue
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: YouTube badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFFCC0000))
          .clickable { onYouTubeClick() }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("youtube_button")
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "قناة يوتيوب التعليمية",
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = "YouTube",
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Center: Active Warehouse indicator
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { onWarehouseClick() }
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("active_warehouse_indicator")
      ) {
        Icon(
          imageVector = Icons.Default.Store,
          contentDescription = "المخزن النشط",
          tint = Color(0xFFE0F2FE),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = currentWarehouse,
          color = Color.White,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }

      // Right: Switch / Refresh icon
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { onRefreshClick() }
          .testTag("bottom_switch_icon"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.SwapHoriz,
          contentDescription = "تبديل أو تحديث",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
}
