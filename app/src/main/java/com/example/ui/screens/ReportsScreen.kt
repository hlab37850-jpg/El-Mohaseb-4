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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  initialReport: String = "حركة الأصناف",
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val invoices by viewModel.allInvoices.collectAsState()
  val items by viewModel.allItems.collectAsState()
  val accounts by viewModel.allAccounts.collectAsState()
  val vouchers by viewModel.allVouchers.collectAsState()

  val reportTabs = listOf("حركة الأصناف", "ميزان المراجعة", "قائمة الدخل", "المركز المالي", "تقارير أخرى")
  val initialIndex = reportTabs.indexOfFirst { it.contains(initialReport) }.coerceAtLeast(0)
  var selectedTab by remember { mutableIntStateOf(initialIndex) }

  // Financial Stats
  val totalSales = invoices.filter { it.type == "SALE" }.sumOf { it.subtotal - it.discount + it.tax + it.otherFees }
  val totalPurchases = invoices.filter { it.type == "PURCHASE" }.sumOf { it.subtotal - it.discount + it.tax + it.otherFees }
  val totalReceipts = vouchers.filter { it.type == "RECEIPT" }.sumOf { it.amount }
  val totalPayments = vouchers.filter { it.type == "PAYMENT" }.sumOf { it.amount }
  val grossProfit = (totalSales - (totalPurchases * 0.7)).coerceAtLeast(0.0)
  val netProfit = (grossProfit - totalPayments).coerceAtLeast(0.0)

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
          IconButton(onClick = { viewModel.showMessage("تصدير التقرير كـ PDF...") }) {
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
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
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

      when (selectedTab) {
        0 -> ItemMovementsReport(items)
        1 -> TrialBalanceReport(accounts)
        2 -> IncomeStatementReport(totalSales, totalPurchases, grossProfit, totalPayments, netProfit)
        3 -> BalanceSheetReport(accounts, totalSales, totalPurchases)
        4 -> OtherReportsList(viewModel)
      }
    }
  }
}

@Composable
fun ItemMovementsReport(items: List<com.example.data.model.ItemEntity>) {
  Column(modifier = Modifier.fillMaxSize()) {
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
        Text("الصنف", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
        Text("الافتتاحي", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("النقص المحسوب", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("المتاح", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
      }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(items) { item ->
        val incoming = item.openingQuantity
        val outgoing = (incoming - item.currentStock).coerceAtLeast(0.0)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f))
          Text("%.0f".format(incoming), fontSize = 13.sp, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
          Text("%.0f".format(outgoing), fontSize = 13.sp, color = Color(0xFFC62828), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
          Text("%.0f".format(item.currentStock), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
        Divider(color = Color(0xFFE2E8F0))
      }
    }
  }
}

@Composable
fun TrialBalanceReport(accounts: List<com.example.data.model.AccountEntity>) {
  Column(modifier = Modifier.fillMaxSize()) {
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
        Text("الحساب", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
        Text("مدين", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
        Text("دائن", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
      }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(accounts) { acc ->
        val debit = if (acc.balance >= 0) acc.balance else 0.0
        val credit = if (acc.balance < 0) -acc.balance else 0.0
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
          Text("%,.0f".format(debit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
          Text("%,.0f".format(credit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
        }
        Divider(color = Color(0xFFE2E8F0))
      }
    }
  }
}

@Composable
fun IncomeStatementReport(
  totalSales: Double,
  totalPurchases: Double,
  grossProfit: Double,
  totalExpenses: Double,
  netProfit: Double
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ReportMetricCard("إجمالي المبيعات الإيرادية", totalSales, Color(0xFF1565C0))
    ReportMetricCard("تكلفة المشتريات والبضاعة", totalPurchases, Color(0xFFC62828))
    ReportMetricCard("مجمل الربح التجاري", grossProfit, Color(0xFF00796B))
    ReportMetricCard("المصروفات والسندات الصادرة", totalExpenses, Color(0xFFD84315))
    ReportMetricCard("صافي الأرباح الصافية", netProfit, Color(0xFF2E7D32), isHighlight = true)
  }
}

@Composable
fun BalanceSheetReport(
  accounts: List<com.example.data.model.AccountEntity>,
  totalSales: Double,
  totalPurchases: Double
) {
  val totalAssets = accounts.filter { it.balance > 0 }.sumOf { it.balance }
  val totalLiabilities = accounts.filter { it.balance < 0 }.sumOf { -it.balance }
  val workingCapital = totalAssets - totalLiabilities

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ReportMetricCard("إجمالي الأصول والأرصدة النقدية", totalAssets, Color(0xFF2E7D32))
    ReportMetricCard("إجمالي الخصوم والالتزامات", totalLiabilities, Color(0xFFC62828))
    ReportMetricCard("رأس المال العامل الصافي", workingCapital, MohasebPrimaryBlue, isHighlight = true)
  }
}

@Composable
fun OtherReportsList(viewModel: AccountingViewModel) {
  val otherReports = listOf(
    "المخزون المتبقي",
    "أرصدة الحسابات",
    "أرباح الأصناف",
    "العمليات اليومية",
    "القيود اليومية",
    "حركة الصندوق",
    "حركة الحسابات",
    "إجمالي التصنيفات",
    "فوارق أسعار العملات",
    "رأس المال العامل",
    "الربح على مستوى العميل",
    "المبيعات حسب الصنف",
    "المشتريات حسب الصنف",
    "تفاصيل حركة الأصناف"
  )

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(otherReports) { r ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { viewModel.showMessage("عرض تقرير: $r") },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = r, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
          Text(r, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@Composable
fun ReportMetricCard(label: String, value: Double, accentColor: Color, isHighlight: Boolean = false) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = if (isHighlight) Color(0xFFE8F5E9) else Color.White
    ),
    shape = RoundedCornerShape(10.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      Text(
        "%,.0f ر.ي".format(value),
        fontSize = if (isHighlight) 15.sp else 13.5.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        maxLines = 1
      )
    }
  }
}
