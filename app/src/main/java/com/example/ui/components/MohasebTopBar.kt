package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebPrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MohasebTopBar(
  title: String = "محاسب سوفت",
  onMenuClick: () -> Unit,
  onNotificationClick: () -> Unit,
  onShareClick: () -> Unit
) {
  CenterAlignedTopAppBar(
    title = {
      Text(
        text = title,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
    },
    navigationIcon = {
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier.testTag("drawer_menu_button")
      ) {
        Icon(
          imageVector = Icons.Default.Menu,
          contentDescription = "القائمة الجانبية",
          tint = Color.White
        )
      }
    },
    actions = {
      IconButton(
        onClick = onShareClick,
        modifier = Modifier.testTag("share_button")
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "مشاركة التطبيق",
          tint = Color.White
        )
      }

      IconButton(
        onClick = onNotificationClick,
        modifier = Modifier.testTag("notification_button")
      ) {
        BadgedBox(
          badge = {
            Badge(
              containerColor = Color.Red,
              contentColor = Color.White
            ) {
              Text("2", fontSize = 10.sp)
            }
          }
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "التنبيهات",
            tint = Color.White
          )
        }
      }
    },
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
      containerColor = MohasebPrimaryBlue
    )
  )
}
