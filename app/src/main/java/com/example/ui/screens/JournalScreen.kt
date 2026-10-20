package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JournalEntryEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
  initialType: String,
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val entries by viewModel.allJournalEntries.collectAsState()
  val accounts by viewModel.allAccounts.collectAsState()
  val currencies by viewModel.allCurrencies.collectAsState()
  val activeCurrency by viewModel.activeCurrency.collectAsState()
  val userMessage by viewModel.userMessage.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedTab by remember { mutableIntStateOf(if (initialType == "قيد إفتتاحي") 1 else 0) }
  var showAddDialog by remember { mutableStateOf(false) }
  var entryToDelete by remember { mutableStateOf<JournalEntryEntity?>(null) }

  val currentType = if (selectedTab == 1) "OPENING" else "DAILY"
  val filtered = entries.filter { it.entryType == currentType }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("القيود المحاسبية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_journal")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MohasebPrimaryBlue)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MohasebPrimaryBlue,
        modifier = Modifier.testTag("fab_add_journal")
      ) {
        Icon(Icons.Default.Add, contentDescription = "إضافة قيد", tint = Color.White)
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = MohasebPrimaryBlue) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("قيد يومي", fontWeight = FontWeight.Bold) })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("قيد إفتتاحي", fontWeight = FontWeight.Bold) })
      }

      if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("لا توجد قيود مسجلة", color = Color(0xFF64748B), fontSize = 14.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filtered) { entry ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("قيد #${entry.entryNumber}", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue, fontSize = 13.5.sp)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.date, fontSize = 12.sp, color = Color(0xFF64748B))
                    IconButton(onClick = { entryToDelete = entry }, modifier = Modifier.size(32.dp)) {
                      Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("من ح/ ${entry.debitAccount}  (مدين)", fontSize = 13.sp, color = Color(0xFF2E7D32))
                Text("إلى ح/ ${entry.creditAccount}  (دائن)", fontSize = 13.sp, color = Color(0xFFC62828))
                Spacer(modifier = Modifier.height(4.dp))
                Text("%,.2f %s".format(entry.amount, entry.currency), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (entry.statement.isNotBlank()) {
                  Text(entry.statement, fontSize = 12.sp, color = Color(0xFF475569))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    var debit by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(activeCurrency) }
    var debitMenu by remember { mutableStateOf(false) }
    var creditMenu by remember { mutableStateOf(false) }
    var currencyMenu by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = {
        Text(if (currentType == "OPENING") "قيد إفتتاحي جديد" else "قيد يومي جديد", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Box {
            OutlinedButton(onClick = { debitMenu = true }, modifier = Modifier.fillMaxWidth()) {
              Text(if (debit.isBlank()) "الحساب المدين *" else "مدين: $debit")
            }
            DropdownMenu(expanded = debitMenu, onDismissRequest = { debitMenu = false }) {
              accounts.forEach { acc ->
                DropdownMenuItem(text = { Text(acc.name) }, onClick = { debit = acc.name; debitMenu = false })
              }
            }
          }
          Box {
            OutlinedButton(onClick = { creditMenu = true }, modifier = Modifier.fillMaxWidth()) {
              Text(if (credit.isBlank()) "الحساب الدائن *" else "دائن: $credit")
            }
            DropdownMenu(expanded = creditMenu, onDismissRequest = { creditMenu = false }) {
              accounts.forEach { acc ->
                DropdownMenuItem(text = { Text(acc.name) }, onClick = { credit = acc.name; creditMenu = false })
              }
            }
          }
          OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("المبلغ *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_journal_amount")
          )
          Box {
            OutlinedButton(onClick = { currencyMenu = true }, modifier = Modifier.fillMaxWidth()) {
              Text("العملة: $currency")
            }
            DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
              currencies.forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = { currency = c.name; currencyMenu = false })
              }
            }
          }
          OutlinedTextField(
            value = statement,
            onValueChange = { statement = it },
            label = { Text("البيان") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val amount = amountText.trim().replace(',', '.').toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
              viewModel.showMessage("أدخل مبلغًا صالحًا أكبر من صفر")
            } else {
              viewModel.addJournalEntry(currentType, debit, credit, amount, currency, statement) {
                showAddDialog = false
              }
            }
          },
          modifier = Modifier.testTag("submit_journal")
        ) { Text("حفظ القيد", fontWeight = FontWeight.Bold) }
      },
      dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
    )
  }

  entryToDelete?.let { entry ->
    AlertDialog(
      onDismissRequest = { entryToDelete = null },
      title = { Text("حذف القيد", fontWeight = FontWeight.Bold) },
      text = { Text("هل تريد حذف القيد #${entry.entryNumber}؟ سيتم عكس أثره على رصيدي الحسابين.") },
      confirmButton = {
        TextButton(onClick = {
          viewModel.deleteJournalEntry(entry) { entryToDelete = null }
        }) { Text("حذف", color = Color.Red, fontWeight = FontWeight.Bold) }
      },
      dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("إلغاء") } }
    )
  }
}
