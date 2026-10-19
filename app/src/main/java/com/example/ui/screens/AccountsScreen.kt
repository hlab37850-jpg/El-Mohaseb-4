package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val accounts by viewModel.allAccounts.collectAsState()
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedAccountForDetails by remember { mutableStateOf<AccountEntity?>(null) }
  var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
  var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("الكل") }

  val categories = listOf("الكل", "الصناديق", "البنوك", "العملاء", "الموردون", "مصروفات")

  val filteredAccounts = accounts.filter {
    (selectedCategory == "الكل" || it.parentCategory == selectedCategory) &&
        (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery))
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "دليل الحسابات",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_accounts")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = { viewModel.showMessage("تصدير دليل الحسابات إلى PDF...") }) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFE53935), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MohasebPrimaryBlue)
      )
    },
    bottomBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        color = MohasebPrimaryBlue
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "إجمالي الحسابات: ${filteredAccounts.size}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )

          FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color.White,
            contentColor = MohasebPrimaryBlue,
            shape = CircleShape,
            modifier = Modifier
              .size(44.dp)
              .testTag("add_account_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة حساب", modifier = Modifier.size(26.dp))
          }
        }
      }
    },
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Categories horizontal bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White)
          .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        categories.forEach { cat ->
          val isSel = cat == selectedCategory
          Box(
            modifier = Modifier
              .background(
                if (isSel) MohasebPrimaryBlue else Color(0xFFF1F5F9),
                CircleShape
              )
              .clickable { selectedCategory = cat }
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = cat,
              color = if (isSel) Color.White else Color(0xFF334155),
              fontSize = 12.sp,
              fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      // Search bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("بحث عن حساب أو هاتف...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("search_accounts_input"),
        singleLine = true
      )

      // Table Header Row matching video 02:07
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp),
        color = Color(0xFF0F172A)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("الإسم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
          Text("التصنيف", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("التلفون", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
          Text("الرصيد", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
        }
      }

      if (filteredAccounts.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "لا توجد حسابات مطابقة...",
            color = Color(0xFF64748B),
            fontSize = 16.sp
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(filteredAccounts) { acc ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedAccountForDetails = acc
                }
                .padding(vertical = 12.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = acc.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(2f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(acc.parentCategory, fontSize = 11.5.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
              Text(acc.phone.ifBlank { "-" }, fontSize = 11.5.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center, maxLines = 1)
              Text(
                text = "%,.0f".format(acc.balance),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (acc.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.weight(1.3f),
                textAlign = TextAlign.Center,
                maxLines = 1
              )
            }
            Divider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
          }
        }
      }
    }
  }

  // Dialog: Add Account matching video (02:00 - 02:06)
  if (showAddDialog) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عام") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var taxNumber by remember { mutableStateOf("") }
    var notifyWhatsApp by remember { mutableStateOf(false) }
    var notifySMS by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("إضافة حساب", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              nameError = false
            },
            label = { Text("اسم الحساب *") },
            isError = nameError,
            supportingText = if (nameError) { { Text("اسم الحساب مطلوب", color = Color.Red) } } else null,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_account_name")
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("التصنيف (عام، الصناديق، العملاء...)") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("التلفون") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("العنوان") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = taxNumber,
            onValueChange = { taxNumber = it },
            label = { Text("الرقم الضريبي") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = notifySMS, onCheckedChange = { notifySMS = it })
            Text("إشعار رسالة نصية", fontSize = 12.sp)
            Spacer(modifier = Modifier.size(12.dp))
            Checkbox(checked = notifyWhatsApp, onCheckedChange = { notifyWhatsApp = it })
            Text("إشعار واتساب", fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (name.isBlank()) {
              nameError = true
            } else {
              viewModel.addAccount(
                name = name.trim(),
                category = category.trim().ifBlank { "عام" },
                phone = phone.trim(),
                address = address.trim(),
                taxNumber = taxNumber.trim(),
                notifyWhatsApp = notifyWhatsApp,
                notifySMS = notifySMS
              ) {
                showAddDialog = false
              }
            }
          },
          modifier = Modifier.testTag("submit_account_button")
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

  // Dialog: Account Details & Actions (Read & Access to Edit / Delete)
  selectedAccountForDetails?.let { acc ->
    AlertDialog(
      onDismissRequest = { selectedAccountForDetails = null },
      title = {
        Text(
          text = acc.name,
          fontWeight = FontWeight.Bold,
          color = MohasebPrimaryBlue
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("التصنيف: ${acc.parentCategory}", fontSize = 13.sp)
          Text(
            text = "الرصيد الحالي: %,.0f ر.ي".format(acc.balance),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (acc.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
          )
          if (acc.phone.isNotBlank()) Text("الهاتف: ${acc.phone}", fontSize = 13.sp)
          if (acc.address.isNotBlank()) Text("العنوان: ${acc.address}", fontSize = 13.sp)
          if (acc.taxNumber.isNotBlank()) Text("الرقم الضريبي: ${acc.taxNumber}", fontSize = 13.sp)
          Text(
            text = "الإشعارات: " + listOfNotNull(
              if (acc.notifyWhatsApp) "واتساب" else null,
              if (acc.notifySMS) "رسالة نصية" else null
            ).joinToString("، ").ifBlank { "معطلة" },
            fontSize = 12.sp,
            color = Color.Gray
          )
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              accountToEdit = acc
              selectedAccountForDetails = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MohasebPrimaryBlue)
          ) {
            Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("تعديل")
          }

          Button(
            onClick = {
              accountToDelete = acc
              selectedAccountForDetails = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
          ) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("حذف")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedAccountForDetails = null }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: Edit Account (Update)
  accountToEdit?.let { targetAcc ->
    var name by remember { mutableStateOf(targetAcc.name) }
    var category by remember { mutableStateOf(targetAcc.parentCategory) }
    var phone by remember { mutableStateOf(targetAcc.phone) }
    var address by remember { mutableStateOf(targetAcc.address) }
    var taxNumber by remember { mutableStateOf(targetAcc.taxNumber) }
    var notifyWhatsApp by remember { mutableStateOf(targetAcc.notifyWhatsApp) }
    var notifySMS by remember { mutableStateOf(targetAcc.notifySMS) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { accountToEdit = null },
      title = { Text("تعديل بيانات الحساب", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              nameError = false
            },
            label = { Text("اسم الحساب *") },
            isError = nameError,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("التصنيف") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("التلفون") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("العنوان") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = taxNumber,
            onValueChange = { taxNumber = it },
            label = { Text("الرقم الضريبي") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = notifySMS, onCheckedChange = { notifySMS = it })
            Text("إشعار رسالة نصية", fontSize = 12.sp)
            Spacer(modifier = Modifier.size(12.dp))
            Checkbox(checked = notifyWhatsApp, onCheckedChange = { notifyWhatsApp = it })
            Text("إشعار واتساب", fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (name.isBlank()) {
              nameError = true
            } else {
              viewModel.updateAccount(
                targetAcc.copy(
                  name = name.trim(),
                  parentCategory = category.trim().ifBlank { "عام" },
                  phone = phone.trim(),
                  address = address.trim(),
                  taxNumber = taxNumber.trim(),
                  notifyWhatsApp = notifyWhatsApp,
                  notifySMS = notifySMS
                )
              ) {
                accountToEdit = null
              }
            }
          }
        ) {
          Text("حفظ التعديل", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { accountToEdit = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Confirm Delete Account (Delete)
  accountToDelete?.let { acc ->
    AlertDialog(
      onDismissRequest = { accountToDelete = null },
      title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
      text = { Text("هل أنت متأكد من حذف الحساب \"${acc.name}\" بشكل نهائي من قاعدة البيانات؟") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteAccount(acc) {
              accountToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("نعم، احذف")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { accountToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}
