package com.example.ui.screens

import com.example.ui.util.rememberExportActions
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.util.ReportBuilders
import com.example.ui.util.ReportData
import com.example.ui.util.ReportTable
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  initialReport: String = "حركة الأصناف",
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val invoices by viewModel.allInvoices.collectAsState()
  val invoiceItems by viewModel.allInvoiceItems.collectAsState()
  val items by viewModel.allItems.collectAsState()
  val accounts by viewModel.allAccounts.collectAsState()
  val vouchers by viewModel.allVouchers.collectAsState()
  val journal by viewModel.allJournalEntries.collectAsState()
  val currencies by viewModel.allCurrencies.collectAsState()
  val userMessage by viewModel.userMessage.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  val reportTabs = listOf("حركة الأصناف", "ميزان المراجعة", "قائمة الدخل", "المركز المالي", "تقارير أخرى")
  val isOtherReport = initialReport in ReportBuilders.otherReportNames
  val initialIndex = if (isOtherReport) 4 else reportTabs.indexOfFirst { it.contains(initialReport) }.coerceAtLeast(0)
  var selectedTab by remember { mutableIntStateOf(initialIndex) }
  var otherReport by remember { mutableStateOf<String?>(if (isOtherReport) initialReport else null) }
  var statementAccount by remember { mutableStateOf("") }
  val exports = rememberExportActions(viewModel)
  val warehousesList by viewModel.allWarehouses.collectAsState()
  val warehouseStockList by viewModel.allWarehouseStock.collectAsState()

  val data = ReportData(
    invoices = invoices,
    invoiceItems = invoiceItems,
    items = items,
    accounts = accounts,
    vouchers = vouchers,
    journal = journal,
    currencies = currencies,
    today = viewModel.todayFormatted,
    warehouses = warehousesList,
    warehouseStock = warehouseStockList
  )

  val currentTable: ReportTable? = when (selectedTab) {
    0 -> ReportBuilders.itemMovements(data)
    1 -> ReportBuilders.trialBalance(data)
    2 -> ReportBuilders.incomeStatement(data)
    3 -> ReportBuilders.balanceSheet(data)
    else -> otherReport?.let { ReportBuilders.build(it, data, statementAccount) }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("التقارير المالية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_reports")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = {
            val table = currentTable
            if (table == null) viewModel.showMessage("اختر تقريرًا أولًا") else exports.print(table)
          }) {
            Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color.White)
          }
          IconButton(onClick = {
            val table = currentTable
            if (table == null) viewModel.showMessage("اختر تقريرًا أولًا") else exports.excel(table)
          }) {
            Box(
              modifier = Modifier.size(28.dp).background(Color(0xFF2E7D32), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Description, contentDescription = "Excel", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
          IconButton(onClick = {
            val table = currentTable
            if (table == null) viewModel.showMessage("اختر تقريرًا أولًا") else exports.pdf(table)
          }) {
            Box(
              modifier = Modifier.size(28.dp).background(Color(0xFFE53935), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MohasebPrimaryBlue)
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = MohasebPrimaryBlue,
        edgePadding = 8.dp
      ) {
        reportTabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
        }
      }

      if (selectedTab < 4) {
        ReportTableView(currentTable ?: ReportTable("", emptyList(), emptyList()))
      } else if (otherReport == null) {
        OtherReportsList(onSelect = { otherReport = it })
      } else {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(otherReport.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MohasebPrimaryBlue)
          TextButton(onClick = { otherReport = null }) { Text("كل التقارير") }
        }
        if (otherReport == "حركة الحسابات") {
          var menuOpen by remember { mutableStateOf(false) }
          Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
              Text(if (statementAccount.isBlank()) "اختر الحساب" else statementAccount)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
              accounts.forEach { acc ->
                DropdownMenuItem(text = { Text(acc.name) }, onClick = { statementAccount = acc.name; menuOpen = false })
              }
            }
          }
        }
        ReportTableView(currentTable ?: ReportTable("", emptyList(), emptyList()))
      }
    }
  }
}

@Composable
fun ReportTableView(table: ReportTable) {
  val cols = table.headers.size.coerceAtLeast(1)
  fun weightOf(i: Int): Float = if (i == 0) 2f else 1f

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(modifier = Modifier.fillMaxWidth().height(42.dp), color = Color(0xFF0F172A)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        table.headers.forEachIndexed { i, h ->
          Text(
            h, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(weightOf(i)),
            textAlign = if (i == 0) TextAlign.Start else TextAlign.Center
          )
        }
      }
    }

    if (table.rows.isEmpty()) {
      Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text("لا توجد بيانات لعرضها", color = Color(0xFF64748B), fontSize = 14.sp)
      }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(table.rows) { row ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp, horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 0 until cols) {
            Text(
              row.getOrElse(i) { "" },
              fontSize = 12.5.sp,
              fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
              modifier = Modifier.weight(weightOf(i)),
              textAlign = if (i == 0) TextAlign.Start else TextAlign.Center
            )
          }
        }
        Divider(color = Color(0xFFE2E8F0))
      }
      table.totals?.let { totals ->
        item {
          Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9)).padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            for (i in 0 until cols) {
              Text(
                totals.getOrElse(i) { "" },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(weightOf(i)),
                textAlign = if (i == 0) TextAlign.Start else TextAlign.Center
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun OtherReportsList(onSelect: (String) -> Unit) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(ReportBuilders.otherReportNames) { r ->
      Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(r) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(r, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
          Icon(Icons.Default.PictureAsPdf, contentDescription = r, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
        }
      }
    }
  }
}
