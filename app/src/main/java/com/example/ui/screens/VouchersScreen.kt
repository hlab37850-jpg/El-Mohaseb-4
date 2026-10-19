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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.VoucherEntity
import com.example.ui.theme.MohasebBorder
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val vouchers by viewModel.allVouchers.collectAsState()
  val accounts by viewModel.allAccounts.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Receipt (قبض), 2: Payment (صرف)
  var showNewVoucherDialog by remember { mutableStateOf(false) }
  var showSaveConfirmDialog by remember { mutableStateOf(false) }
  var selectedVoucherForDetails by remember { mutableStateOf<VoucherEntity?>(null) }
  var voucherToEdit by remember { mutableStateOf<VoucherEntity?>(null) }
  var voucherToDelete by remember { mutableStateOf<VoucherEntity?>(null) }

  // New voucher form states
  var voucherType by remember { mutableStateOf("RECEIPT") } // "RECEIPT" or "PAYMENT"
  var selectedAccount by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }
  var statementInput by remember { mutableStateOf("") }
  var notesInput by remember { mutableStateOf("") }
  var currency by remember { mutableStateOf("محلي") }
  var showAccountPicker by remember { mutableStateOf(false) }

  val filteredVouchers = vouchers.filter {
    when (selectedTab) {
      1 -> it.type == "RECEIPT"
      2 -> it.type == "PAYMENT"
      else -> true
    }
  }

  val totalReceipts = vouchers.filter { it.type == "RECEIPT" }.sumOf { it.amount }
  val totalPayments = vouchers.filter { it.type == "PAYMENT" }.sumOf { it.amount }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "سندات القبض والصرف",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_vouchers")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = { viewModel.showMessage("تصدير كشف السندات كـ PDF...") }) {
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
            text = "صافي الصندوق: %,.0f ر.ي".format(totalReceipts - totalPayments),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )

          FloatingActionButton(
            onClick = {
              selectedAccount = accounts.firstOrNull()?.name ?: "الصندوق الرئيسي"
              amountInput = ""
              statementInput = ""
              notesInput = ""
              showNewVoucherDialog = true
            },
            containerColor = Color.White,
            contentColor = MohasebPrimaryBlue,
            shape = CircleShape,
            modifier = Modifier
              .size(44.dp)
              .testTag("add_voucher_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة سند", modifier = Modifier.size(26.dp))
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
      // Tabs: All, Receipt (قبض), Payment (صرف)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = MohasebPrimaryBlue
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("الكل (${vouchers.size})", fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("سندات قبض", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("سندات صرف", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) }
        )
      }

      // Table Header Row
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp),
        color = Color(0xFF0F172A)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("رقم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
          Text("النوع", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
          Text("الحساب", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
          Text("المبلغ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
        }
      }

      if (filteredVouchers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "لا توجد سندات مسجلة...",
            color = Color(0xFF64748B),
            fontSize = 16.sp
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(filteredVouchers) { v ->
            val isReceipt = v.type == "RECEIPT"
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedVoucherForDetails = v
                }
                .padding(vertical = 12.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(v.voucherNumber, fontSize = 12.sp, color = Color(0xFF334155), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, maxLines = 1)
              Text(
                text = if (isReceipt) "قبض" else "صرف",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1
              )
              Text(
                text = v.accountName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                text = "%,.0f".format(v.amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828),
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

  // Dialog: Add Voucher matching video (01:03 - 01:13)
  if (showNewVoucherDialog) {
    AlertDialog(
      onDismissRequest = { showNewVoucherDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (voucherType == "RECEIPT") "سند قبض" else "سند صرف",
            fontWeight = FontWeight.Bold,
            color = MohasebPrimaryBlue
          )

          // Toggle Receipt / Payment
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (voucherType == "RECEIPT") "قبض" else "صرف",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
              checked = voucherType == "RECEIPT",
              onCheckedChange = { isRec ->
                voucherType = if (isRec) "RECEIPT" else "PAYMENT"
              }
            )
          }
        }
      },
      text = {
        Column {
          // Currency selector (محلي vs دولار) matching video 01:12
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("العملة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(selected = currency == "محلي", onClick = { currency = "محلي" })
              Text("محلي", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(selected = currency == "دولار", onClick = { currency = "دولار" })
              Text("دولار", fontSize = 13.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Box {
            OutlinedTextField(
              value = selectedAccount,
              onValueChange = { selectedAccount = it },
              label = { Text("اسم الحساب / العميل *") },
              trailingIcon = {
                IconButton(onClick = { showAccountPicker = true }) {
                  Icon(Icons.Default.Person, contentDescription = "اختر الحساب", tint = MohasebPrimaryBlue)
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_voucher_account")
            )

            DropdownMenu(
              expanded = showAccountPicker,
              onDismissRequest = { showAccountPicker = false }
            ) {
              accounts.forEach { acc ->
                DropdownMenuItem(
                  text = { Text("${acc.name} (${acc.parentCategory})") },
                  onClick = {
                    selectedAccount = acc.name
                    showAccountPicker = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text("المبلغ *") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_voucher_amount")
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = statementInput,
            onValueChange = { statementInput = it },
            label = { Text("البيان") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = notesInput,
            onValueChange = { notesInput = it },
            label = { Text("ملاحظات") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val amount = amountInput.toDoubleOrNull() ?: 0.0
            if (amount > 0 && selectedAccount.isNotBlank()) {
              showSaveConfirmDialog = true
            } else {
              viewModel.showMessage("يرجى إدخال اسم الحساب ومبلغ صالح أكبر من صفر")
            }
          },
          modifier = Modifier.testTag("confirm_voucher_button")
        ) {
          Text("حفظ", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewVoucherDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Confirm Save
  if (showSaveConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showSaveConfirmDialog = false },
      title = { Text("حفظ", fontWeight = FontWeight.Bold) },
      text = { Text("هل تريد حفظ السند نهائياً وتحديث أرصدة الحساب؟") },
      confirmButton = {
        TextButton(
          onClick = {
            val amount = amountInput.toDoubleOrNull() ?: 0.0
            showSaveConfirmDialog = false
            showNewVoucherDialog = false
            viewModel.saveVoucher(
              type = voucherType,
              accountName = selectedAccount.trim(),
              amount = amount,
              currency = currency,
              statement = statementInput.trim(),
              notes = notesInput.trim(),
              onSuccess = {}
            )
          }
        ) {
          Text("نعم", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveConfirmDialog = false }) {
          Text("لا")
        }
      }
    )
  }

  // Dialog: Voucher Details & Actions
  selectedVoucherForDetails?.let { v ->
    AlertDialog(
      onDismissRequest = { selectedVoucherForDetails = null },
      title = {
        Text(
          text = "${if (v.type == "RECEIPT") "سند قبض" else "سند صرف"} #${v.voucherNumber}",
          fontWeight = FontWeight.Bold,
          color = MohasebPrimaryBlue
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("الحساب: ${v.accountName}", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
          Text(
            text = "المبلغ: %,.0f %s".format(v.amount, v.currency),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (v.type == "RECEIPT") Color(0xFF2E7D32) else Color(0xFFC62828)
          )
          Text("التاريخ: ${v.date}", fontSize = 12.5.sp, color = Color.Gray)
          if (v.statement.isNotBlank()) Text("البيان: ${v.statement}", fontSize = 12.5.sp)
          if (v.notes.isNotBlank()) Text("ملاحظات: ${v.notes}", fontSize = 12.sp, color = Color.Gray)
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              voucherToEdit = v
              selectedVoucherForDetails = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MohasebPrimaryBlue)
          ) {
            Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("تعديل")
          }

          Button(
            onClick = {
              voucherToDelete = v
              selectedVoucherForDetails = null
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
        TextButton(onClick = { selectedVoucherForDetails = null }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: Edit Voucher
  voucherToEdit?.let { targetV ->
    var editAccount by remember { mutableStateOf(targetV.accountName) }
    var editAmount by remember { mutableStateOf(targetV.amount.toString()) }
    var editStatement by remember { mutableStateOf(targetV.statement) }
    var editNotes by remember { mutableStateOf(targetV.notes) }

    AlertDialog(
      onDismissRequest = { voucherToEdit = null },
      title = { Text("تعديل السند #${targetV.voucherNumber}", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(
            value = editAccount,
            onValueChange = { editAccount = it },
            label = { Text("اسم الحساب") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = editAmount,
            onValueChange = { editAmount = it },
            label = { Text("المبلغ") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = editStatement,
            onValueChange = { editStatement = it },
            label = { Text("البيان") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = editNotes,
            onValueChange = { editNotes = it },
            label = { Text("ملاحظات") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val amt = editAmount.toDoubleOrNull() ?: 0.0
            if (amt > 0 && editAccount.isNotBlank()) {
              viewModel.updateVoucher(
                targetV.copy(
                  accountName = editAccount.trim(),
                  amount = amt,
                  statement = editStatement.trim(),
                  notes = editNotes.trim()
                )
              ) {
                voucherToEdit = null
              }
            }
          }
        ) {
          Text("حفظ التعديل", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { voucherToEdit = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Confirm Delete Voucher
  voucherToDelete?.let { v ->
    AlertDialog(
      onDismissRequest = { voucherToDelete = null },
      title = { Text("تأكيد حذف السند", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
      text = { Text("هل تريد حذف السند رقم #${v.voucherNumber} بمبلغ %,.0f ر.ي؟ سيتم استرجاع الرصيد للحساب.".format(v.amount)) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteVoucher(v) {
              voucherToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("نعم، احذف")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { voucherToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}
