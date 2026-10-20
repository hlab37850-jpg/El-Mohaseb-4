package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.util.SettingKeys
import com.example.ui.util.boolSetting
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val reminders by viewModel.allReminders.collectAsState()
  val accounts by viewModel.allAccounts.collectAsState()
  val userMessage by viewModel.userMessage.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var showAddDialog by remember { mutableStateOf(false) }

  // Overdue balances: every account that still carries a positive balance is a pending collection.
  val appSettings by viewModel.allSettings.collectAsState()
  val showBalances = appSettings.boolSetting(SettingKeys.NOTIFY_BALANCES, true)
  val openBalances = if (showBalances) accounts.filter { it.parentCategory == "العملاء" && it.balance > 0.0 } else emptyList()

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
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (reminders.isEmpty() && openBalances.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد تنبيهات حالياً", color = Color(0xFF64748B), fontSize = 14.sp)
          }
        }
      }

      items(reminders) { item ->
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
                text = item.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isDone) Color(0xFF94A3B8) else MohasebPrimaryBlue,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Row {
                IconButton(onClick = { viewModel.toggleReminderDone(item) }, modifier = Modifier.size(28.dp)) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "تم",
                    tint = if (item.isDone) Color(0xFF2E7D32) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                  )
                }
                IconButton(
                  onClick = {
                    if (item.phone.isBlank()) {
                      viewModel.showMessage("لا يوجد رقم هاتف مسجل لهذا التنبيه")
                    } else {
                      runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.phone)))
                      }.onFailure { viewModel.showMessage("تعذر فتح تطبيق الاتصال") }
                    }
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF00796B), modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = {
                    if (item.phone.isBlank()) {
                      viewModel.showMessage("لا يوجد رقم هاتف مسجل لهذا التنبيه")
                    } else {
                      runCatching {
                        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + item.phone))
                        sms.putExtra("sms_body", "تذكير: " + item.title)
                        context.startActivity(sms)
                      }.onFailure { viewModel.showMessage("تعذر فتح تطبيق الرسائل") }
                    }
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Message, contentDescription = "رسالة", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.deleteReminder(item) }, modifier = Modifier.size(28.dp)) {
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

      if (openBalances.isNotEmpty()) {
        item {
          Text("أرصدة عملاء مستحقة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.padding(top = 6.dp))
        }
        items(openBalances) { acc ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("المستحق: %,.0f".format(acc.balance), fontSize = 12.sp, color = Color(0xFFC62828))
              }
              IconButton(
                onClick = {
                  if (acc.phone.isBlank()) {
                    viewModel.showMessage("لا يوجد رقم هاتف مسجل للعميل ${acc.name}")
                  } else {
                    runCatching {
                      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + acc.phone)))
                    }.onFailure { viewModel.showMessage("تعذر فتح تطبيق الاتصال") }
                  }
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF00796B), modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(viewModel.todayFormatted) }
    var time by remember { mutableStateOf("12:00") }
    var phone by remember { mutableStateOf("") }
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
          OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف (اختياري)") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (name.isBlank()) {
              viewModel.showMessage("اكتب موضوع التنبيه")
            } else {
              viewModel.addReminder(name, date, time, note, phone) { showAddDialog = false }
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
