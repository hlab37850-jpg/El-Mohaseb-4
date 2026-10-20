package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebBorder
import com.example.ui.theme.MohasebPrimaryBlue

@Composable
fun MohasebDrawerContent(
  onAccountsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onBackupClick: () -> Unit,
  onRestoreClick: () -> Unit,
  onDriveClick: () -> Unit,
  onSupportClick: () -> Unit,
  onAboutClick: () -> Unit,
  onExitClick: () -> Unit
) {
  ModalDrawerSheet(
    modifier = Modifier
      .width(300.dp)
      .fillMaxHeight(),
    drawerContainerColor = Color.White
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(MohasebPrimaryBlue)
          .padding(vertical = 28.dp, horizontal = 20.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Color.White),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = "شعار التطبيق",
              tint = MohasebPrimaryBlue,
              modifier = Modifier.size(30.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "محاسب سوفت",
              color = Color.White,
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "نظام الحسابات والمخازن",
              color = Color(0xFFE2E8F0),
              fontSize = 13.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Drawer Items
      DrawerItem(
        title = "حفظ نسخة",
        icon = Icons.Default.Backup,
        onClick = onBackupClick,
        testTag = "drawer_backup"
      )
      DrawerItem(
        title = "إسترجاع القاعدة",
        icon = Icons.Default.Restore,
        onClick = onRestoreClick,
        testTag = "drawer_restore"
      )
      DrawerItem(
        title = "جوجل درايف",
        icon = Icons.Default.CloudUpload,
        onClick = onDriveClick,
        testTag = "drawer_drive"
      )
      DrawerItem(
        title = "دليل الحسابات",
        icon = Icons.Default.AccountBalance,
        onClick = onAccountsClick,
        testTag = "drawer_accounts"
      )
      DrawerItem(
        title = "إعدادات",
        icon = Icons.Default.Settings,
        onClick = onSettingsClick,
        testTag = "drawer_settings"
      )

      Divider(color = MohasebBorder, modifier = Modifier.padding(vertical = 8.dp))

      DrawerItem(
        title = "للتواصل والدعم",
        icon = Icons.Default.HeadsetMic,
        onClick = onSupportClick,
        testTag = "drawer_support"
      )
      DrawerItem(
        title = "حول البرنامج",
        icon = Icons.Default.Info,
        onClick = onAboutClick,
        testTag = "drawer_about"
      )
      DrawerItem(
        title = "خروج",
        icon = Icons.Default.ExitToApp,
        onClick = onExitClick,
        testTag = "drawer_exit",
        tint = Color(0xFFC62828)
      )
    }
  }
}

@Composable
fun DrawerItem(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit,
  testTag: String,
  tint: Color = MohasebPrimaryBlue
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 18.dp, vertical = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = tint,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      fontSize = 14.5.sp,
      fontWeight = FontWeight.Medium,
      color = Color(0xFF1E293B),
      maxLines = 1,
      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
  }
}
