package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

data class NotificationItem(
  val id: Long,
  val name: String,
  val date: String,
  val time: String,
  val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val notifications = remember {
    mutableStateListOf(
      NotificationItem(1, "مؤسسة الأمل - سداد قسط الفاتورة #1", "2026-10-20", "11:30", "تذكير بالاتصال بالمحاسب"),
      NotificationItem(2, "توريد بضاعة ركب وفلاتر زيت", "2026-10-22", "14:00", "وصول شحنة المستودع الرئيسي")
    )
  }

  var showAddDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("التنبيهات والمواعيد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_notifications")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        actions = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clickable { showAddDialog = true }
              .padding(end = 12.dp)
          ) {
            Text("إضافة تنبيه", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Add, contentDescription = "إضافة تنبيه", tint = Color.White, modifier = Modifier.size(20.dp))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MohasebPrimaryBlue)
      )
    },
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(notifications) { item ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = item.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MohasebPrimaryBlue,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Row {
                IconButton(onClick = { viewModel.showMessage("إجراء اتصال هاتفي للتنبيه") }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF00796B), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.showMessage("إرسال رسالة تذكير") }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Message, contentDescription = "رسالة", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { notifications.remove(item) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("${item.date}  ${item.time}", fontSize = 11.5.sp, color = Color(0xFF64748B))

            if (item.note.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(item.note, fontSize = 12.5.sp, color = Color(0xFF334155), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
          }
        }
      }
    }
  }

  // Dialog: Add notification matching video 03:35
  if (showAddDialog) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(viewModel.todayFormatted) }
    var time by remember { mutableStateOf("12:00") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("إضافة تنبيه جديد", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الإسم أو موضوع التنبيه") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("الوقت") }, modifier = Modifier.weight(1f))
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (name.isNotBlank()) {
              notifications.add(NotificationItem(System.currentTimeMillis(), name, date, time, note))
              showAddDialog = false
              viewModel.showMessage("تم حفظ التنبيه بنجاح")
            }
          }
        ) {
          Text("موافق", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}
