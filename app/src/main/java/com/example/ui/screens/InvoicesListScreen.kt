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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.InvoiceEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesListScreen(
  invoiceType: String, // "SALE" or "PURCHASE"
  viewModel: AccountingViewModel,
  onBack: () -> Unit,
  onAddNew: () -> Unit
) {
  val title = if (invoiceType == "SALE") "قائمة المبيعات" else "قائمة المشتريات"
  val invoices by viewModel.allInvoices.collectAsState()
  val filteredInvoices = invoices.filter { it.type == invoiceType }

  var showMenu by remember { mutableStateOf(false) }
  var showDateFilterDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var dateQuery by remember { mutableStateOf("") }

  var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceEntity?>(null) }
  var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }

  val displayedInvoices = filteredInvoices.filter {
    (searchQuery.isBlank() || it.partyName.contains(searchQuery, ignoreCase = true) || it.invoiceNumber.contains(searchQuery)) &&
        (dateQuery.isBlank() || it.date.contains(dateQuery))
  }

  val totalAmount = displayedInvoices.sumOf { it.subtotal - it.discount + it.tax + it.otherFees }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "رجوع",
              tint = Color.White
            )
          }
        },
        actions = {
          IconButton(onClick = { showDateFilterDialog = true }) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "بحث وتصفية",
              tint = Color.White
            )
          }

          IconButton(onClick = { viewModel.showMessage("تصدير كشف الفواتير بصيغة PDF") }) {
            Box(
              modifier = Modifier
                .size(30.dp)
                .background(Color(0xFFE53935), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "PDF",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          IconButton(onClick = { showMenu = true }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "خيارات",
              tint = Color.White
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("تحديث البيانات") },
              onClick = {
                showMenu = false
                viewModel.showMessage("تم تحديث البيانات من قاعدة البيانات")
              }
            )
            DropdownMenuItem(
              text = { Text("تصفير البحث والتصفية") },
              onClick = {
                searchQuery = ""
                dateQuery = ""
                showMenu = false
              }
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MohasebPrimaryBlue
        )
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
            text = "الإجمالي: %,.0f ر.ي".format(totalAmount),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )

          FloatingActionButton(
            onClick = onAddNew,
            containerColor = Color.White,
            contentColor = MohasebPrimaryBlue,
            shape = CircleShape,
            modifier = Modifier
              .size(44.dp)
              .testTag("add_invoice_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة فاتورة", modifier = Modifier.size(26.dp))
          }
        }
      }
    },
    containerColor = Color(0xFFF9FAFC)
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Table Header Row matching video (00:08)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp),
        color = Color(0xFF0D47A1)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "رقم",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center
          )
          Text(
            text = "التاريخ",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.Center
          )
          Text(
            text = "الإسم",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.Center
          )
          Text(
            text = "المبلغ",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.3f),
            textAlign = TextAlign.Center
          )
        }
      }

      if (displayedInvoices.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "لا توجد فواتير مسجلة...",
            color = Color(0xFF5C6BC0),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(displayedInvoices) { inv ->
            val net = inv.subtotal - inv.discount + inv.tax + inv.otherFees
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedInvoiceForDetails = inv
                }
                .padding(vertical = 12.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = inv.invoiceNumber,
                fontSize = 13.sp,
                color = Color(0xFF1E293B),
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1
              )
              Text(
                text = inv.date,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                modifier = Modifier.weight(1.4f),
                textAlign = TextAlign.Center,
                maxLines = 1
              )
              Text(
                text = inv.partyName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                text = "%,.0f".format(net),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B60A8),
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

  // Dialog: Search & Date Filter
  if (showDateFilterDialog) {
    var searchTemp by remember { mutableStateOf(searchQuery) }
    var dateTemp by remember { mutableStateOf(dateQuery) }

    AlertDialog(
      onDismissRequest = { showDateFilterDialog = false },
      title = { Text("بحث وتصفية الفواتير", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(
            value = searchTemp,
            onValueChange = { searchTemp = it },
            label = { Text("بحث بالاسم أو رقم الفاتورة") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = dateTemp,
            onValueChange = { dateTemp = it },
            label = { Text("تصفية بالتاريخ (YYYY-MM-DD)") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            searchQuery = searchTemp.trim()
            dateQuery = dateTemp.trim()
            showDateFilterDialog = false
          }
        ) {
          Text("تطبيق", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            searchQuery = ""
            dateQuery = ""
            showDateFilterDialog = false
          }
        ) {
          Text("إعادة تعيين")
        }
      }
    )
  }

  // Dialog: Invoice Details & Items View
  selectedInvoiceForDetails?.let { inv ->
    val itemsFlow = remember(inv.id) { viewModel.getInvoiceItems(inv.id) }
    val lineItems by itemsFlow.collectAsState(initial = emptyList())
    val net = inv.subtotal - inv.discount + inv.tax + inv.otherFees

    AlertDialog(
      onDismissRequest = { selectedInvoiceForDetails = null },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "فاتورة #${inv.invoiceNumber}",
            fontWeight = FontWeight.Bold,
            color = MohasebPrimaryBlue
          )
          Text(
            text = if (inv.isCredit) "آجل" else "نقدي",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (inv.isCredit) Color(0xFFC62828) else Color(0xFF2E7D32)
          )
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("الطرف: ${inv.partyName}", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
          Text("التاريخ: ${inv.date} • المخزن: ${inv.warehouseName}", fontSize = 12.sp, color = Color.Gray)
          if (inv.notes.isNotBlank()) {
            Text("ملاحظات: ${inv.notes}", fontSize = 12.sp, color = Color(0xFF475569))
          }
          Spacer(modifier = Modifier.height(8.dp))

          Text("الأصناف المسجلة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Divider(modifier = Modifier.padding(vertical = 4.dp))

          if (lineItems.isEmpty()) {
            Text("جاري تحميل الأصناف...", fontSize = 12.sp, color = Color.Gray)
          } else {
            lineItems.forEach { itm ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(itm.itemName, fontSize = 12.sp, modifier = Modifier.weight(2f))
                Text("%.1f × %,.0f".format(itm.quantity, itm.unitPrice), fontSize = 11.5.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("%,.0f".format(itm.total), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
              }
            }
          }

          Divider(modifier = Modifier.padding(vertical = 6.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الإجمالي الصافي:", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text("%,.0f %s".format(net, inv.currency), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MohasebPrimaryBlue)
          }
          if (inv.paidAmount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("المدفوع: %,.0f".format(inv.paidAmount), fontSize = 12.sp)
              Text("المتبقي: %,.0f".format(inv.remainingAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            }
          }
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              viewModel.showMessage("جاري إرسال الفاتورة #${inv.invoiceNumber} للطباعة الحرارية...")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MohasebPrimaryBlue)
          ) {
            Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("طباعة")
          }

          Button(
            onClick = {
              invoiceToDelete = inv
              selectedInvoiceForDetails = null
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
        TextButton(onClick = { selectedInvoiceForDetails = null }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: Confirm Delete Invoice
  invoiceToDelete?.let { inv ->
    AlertDialog(
      onDismissRequest = { invoiceToDelete = null },
      title = { Text("تأكيد حذف الفاتورة", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
      text = { Text("هل تريد حذف الفاتورة #${inv.invoiceNumber} بشكل نهائي؟ سيتم استرجاع كميات المخزون وتحديث أرصدة الحساب.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteInvoice(inv) {
              invoiceToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("نعم، احذف الفاتورة")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { invoiceToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}
