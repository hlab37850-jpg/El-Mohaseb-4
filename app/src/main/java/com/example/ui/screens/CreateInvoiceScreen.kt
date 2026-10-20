package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.ui.util.ListTables
import com.example.ui.util.SettingKeys
import com.example.ui.util.boolSetting
import com.example.ui.util.stringSetting
import com.example.ui.util.ReportTable
import com.example.ui.util.rememberExportActions
import com.example.data.model.ItemEntity
import com.example.ui.theme.MohasebBorder
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
  invoiceType: String, // "SALE", "PURCHASE", "QUOTE" or "PURCHASE_ORDER"
  viewModel: AccountingViewModel,
  onBack: () -> Unit,
  editInvoiceId: Long? = null
) {
  val exports = rememberExportActions(viewModel)
  val allInvoicesForEdit by viewModel.allInvoices.collectAsState()
  val editingInvoice = editInvoiceId?.let { id -> allInvoicesForEdit.firstOrNull { it.id == id } }
  val availableWarehouses by viewModel.allWarehouses.collectAsState()
  val availableCurrencies by viewModel.allCurrencies.collectAsState()
  val availableAccounts by viewModel.allAccounts.collectAsState()
  val availableItems by viewModel.allItems.collectAsState()
  val activeWarehouse by viewModel.activeWarehouse.collectAsState()
  val activeCurrency by viewModel.activeCurrency.collectAsState()

  var isCredit by remember { mutableStateOf(true) } // آجل vs نقد
  var isReturn by remember { mutableStateOf(false) } // مرتجع
  var warehouseName by remember { mutableStateOf(activeWarehouse) }
  var currency by remember { mutableStateOf(activeCurrency) }

  LaunchedEffect(availableWarehouses, activeWarehouse) {
    if (editInvoiceId != null) return@LaunchedEffect
    warehouseName = when {
      availableWarehouses.any { it.name == activeWarehouse } -> activeWarehouse
      availableWarehouses.isNotEmpty() && availableWarehouses.none { it.name == warehouseName } ->
        availableWarehouses.first().name
      else -> warehouseName
    }
  }
  LaunchedEffect(availableCurrencies, activeCurrency) {
    if (editInvoiceId != null) return@LaunchedEffect
    currency = when {
      availableCurrencies.any { it.name == activeCurrency } -> activeCurrency
      availableCurrencies.isNotEmpty() && availableCurrencies.none { it.name == currency } ->
        availableCurrencies.first().name
      else -> currency
    }
  }
  var partyName by remember { mutableStateOf("") }
  var invoiceDate by remember { mutableStateOf(viewModel.todayFormatted) }
  var invoiceNumber by remember { mutableStateOf((System.currentTimeMillis() % 10000).toString()) }
  var notes by remember { mutableStateOf("") }

  // Financial inputs
  var discount by remember { mutableDoubleStateOf(0.0) }
  var taxPercent by remember { mutableDoubleStateOf(0.0) }
  var otherFees by remember { mutableDoubleStateOf(0.0) }
  var paidAmount by remember { mutableDoubleStateOf(0.0) }

  // Line Items
  val lineItems = remember { mutableStateListOf<InvoiceItemEntity>() }

  // Defaults from settings for brand-new invoices only.
  val appSettings by viewModel.allSettings.collectAsState()
  val defaultTax = appSettings.stringSetting(SettingKeys.DEFAULT_TAX).replace(',', '.').toDoubleOrNull() ?: 0.0
  val posCash = appSettings.boolSetting(SettingKeys.POS_DEFAULT_CASH, false)
  LaunchedEffect(defaultTax, posCash) {
    if (editInvoiceId == null) {
      if (lineItems.isEmpty()) taxPercent = defaultTax
      if (invoiceType == "SALE" && posCash) isCredit = false
    }
  }

  // Edit mode: load the saved invoice and its lines once.
  var editLoaded by remember { mutableStateOf(false) }
  LaunchedEffect(editingInvoice) {
    val inv = editingInvoice
    if (inv != null && !editLoaded) {
      val savedLines = viewModel.loadInvoiceItems(inv.id)
      isCredit = inv.isCredit
      isReturn = inv.isReturn
      warehouseName = inv.warehouseName
      currency = inv.currency
      partyName = inv.partyName
      invoiceDate = inv.date
      invoiceNumber = inv.invoiceNumber
      notes = inv.notes
      discount = inv.discount
      otherFees = inv.otherFees
      paidAmount = inv.paidAmount
      val taxable = inv.subtotal - inv.discount
      taxPercent = if (taxable > 0.0) inv.tax / taxable * 100.0 else 0.0
      lineItems.clear()
      lineItems.addAll(savedLines)
      editLoaded = true
    }
  }

  // Pickers and Modals
  var showAddItemDialog by remember { mutableStateOf(false) }
  var showSaveConfirmDialog by remember { mutableStateOf(false) }
  var showFinancialAdjustDialog by remember { mutableStateOf(false) }
  var showWarehousePicker by remember { mutableStateOf(false) }
  var showCurrencyPicker by remember { mutableStateOf(false) }
  var showPartyPicker by remember { mutableStateOf(false) }

  // Calculations
  val subtotal = lineItems.sumOf { it.total }
  val taxAmount = (subtotal - discount).coerceAtLeast(0.0) * (taxPercent / 100.0)
  val netTotal = (subtotal - discount + taxAmount + otherFees).coerceAtLeast(0.0)
  val remainingBalance = (netTotal - paidAmount).coerceAtLeast(0.0)

  val isSaleSide = invoiceType == "SALE" || invoiceType == "QUOTE"

  fun currentInvoiceTable(): ReportTable {
    val draft = InvoiceEntity(
      invoiceNumber = invoiceNumber,
      type = invoiceType,
      isCredit = isCredit,
      isReturn = isReturn,
      date = invoiceDate,
      partyName = partyName.ifBlank { if (isSaleSide) "عميل عام" else "مورد عام" },
      warehouseName = warehouseName,
      currency = currency,
      subtotal = subtotal,
      discount = discount,
      tax = taxAmount,
      otherFees = otherFees,
      paidAmount = paidAmount,
      remainingAmount = remainingBalance,
      notes = notes
    )
    return ListTables.invoiceDocument(draft, lineItems.toList())
  }

  val screenTitle = when (invoiceType) {
    "QUOTE" -> "فاتورة عرض سعر"
    "PURCHASE_ORDER" -> "طلب شراء"
    else -> buildString {
      if (isReturn) append("مرتجع ")
      append(if (isSaleSide) "بيع " else "شراء ")
      append(if (isCredit) "آجل" else "نقدي")
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = screenTitle,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_invoice")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "رجوع",
              tint = Color.White
            )
          }
        },
        actions = {
          IconButton(onClick = { exports.print(currentInvoiceTable()) }) {
            Icon(
              imageVector = Icons.Default.Print,
              contentDescription = "طباعة حرارية",
              tint = Color.White
            )
          }
          IconButton(onClick = { exports.pdf(currentInvoiceTable()) }) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFE53935), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "PDF",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          IconButton(
            onClick = {
              if (lineItems.isEmpty()) {
                viewModel.showMessage("يرجى إضافة صنف واحد على الأقل للفاتورة")
              } else {
                showSaveConfirmDialog = true
              }
            },
            modifier = Modifier.testTag("save_invoice_button")
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = "حفظ الفاتورة",
              tint = Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MohasebPrimaryBlue)
      )
    },
    bottomBar = {
      // Bottom Financial Summary bar matching video 00:15
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showFinancialAdjustDialog = true },
        color = Color(0xFFEBF3FB),
        shadowElevation = 8.dp
      ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("الخصم", fontSize = 10.sp, color = Color.Gray)
              Text("%,.0f".format(discount), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("الضريبة", fontSize = 10.sp, color = Color.Gray)
              Text("%.0f%%".format(taxPercent), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("الصافي", fontSize = 11.sp, color = Color.Gray)
              Text("%,.0f".format(netTotal), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MohasebPrimaryBlue, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("المدفوع", fontSize = 10.sp, color = Color.Gray)
              Text("%,.0f".format(paidAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("المتبقي", fontSize = 10.sp, color = Color.Gray)
              Text("%,.0f".format(remainingBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red, maxLines = 1)
            }
          }
        }
      }
    },
    containerColor = Color.White
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(10.dp)
    ) {
      // Top Controls Row: Cash/Credit Toggle, Return Checkbox, Warehouse, Currency
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Return checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = isReturn,
            onCheckedChange = { isReturn = it },
            modifier = Modifier.testTag("checkbox_return")
          )
          Text("مرتجع", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        // Warehouse & Currency interactive chips
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box {
            Text(
              text = "• $warehouseName",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MohasebPrimaryBlue,
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { showWarehousePicker = true }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            DropdownMenu(
              expanded = showWarehousePicker,
              onDismissRequest = { showWarehousePicker = false }
            ) {
              availableWarehouses.forEach { wh ->
                DropdownMenuItem(
                  text = { Text(wh.name) },
                  onClick = {
                    warehouseName = wh.name
                    viewModel.setActiveWarehouse(wh.name)
                    showWarehousePicker = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Box {
            Text(
              text = "• $currency",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00796B),
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { showCurrencyPicker = true }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            DropdownMenu(
              expanded = showCurrencyPicker,
              onDismissRequest = { showCurrencyPicker = false }
            ) {
              availableCurrencies.forEach { curr ->
                DropdownMenuItem(
                  text = { Text("${curr.name} (${curr.symbol})") },
                  onClick = {
                    currency = curr.name
                    viewModel.setActiveCurrency(curr.name)
                    showCurrencyPicker = false
                  }
                )
              }
            }
          }
        }

        // Cash vs Credit switch
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isCredit) "آجل" else "نقد",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(4.dp))
          Switch(
            checked = isCredit,
            onCheckedChange = { isCredit = it },
            modifier = Modifier.testTag("switch_credit")
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Party Name & Date Row matching video
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.weight(1.8f)) {
          OutlinedTextField(
            value = partyName,
            onValueChange = { partyName = it },
            label = { Text(if (isSaleSide) "العميل *" else "المورد *") },
            trailingIcon = {
              IconButton(onClick = { showPartyPicker = true }) {
                Icon(Icons.Default.Person, contentDescription = "اختيار من الحسابات", tint = MohasebPrimaryBlue)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_party_name"),
            singleLine = true
          )

          DropdownMenu(
            expanded = showPartyPicker,
            onDismissRequest = { showPartyPicker = false }
          ) {
            val relevantAccounts = availableAccounts.filter {
              if (isSaleSide) it.parentCategory in listOf("العملاء", "عام")
              else it.parentCategory in listOf("الموردون", "عام")
            }
            if (relevantAccounts.isEmpty()) {
              availableAccounts.take(5).forEach { acc ->
                DropdownMenuItem(
                  text = { Text("${acc.name} (${acc.parentCategory})") },
                  onClick = {
                    partyName = acc.name
                    showPartyPicker = false
                  }
                )
              }
            } else {
              relevantAccounts.forEach { acc ->
                DropdownMenuItem(
                  text = { Text("${acc.name} (${acc.parentCategory})") },
                  onClick = {
                    partyName = acc.name
                    showPartyPicker = false
                  }
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = invoiceDate,
          onValueChange = { invoiceDate = it },
          label = { Text("التاريخ") },
          modifier = Modifier.weight(1.2f),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Notes & Invoice # Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = invoiceNumber,
          onValueChange = { invoiceNumber = it },
          label = { Text("رقم #") },
          modifier = Modifier.weight(0.9f),
          singleLine = true
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("ملاحظات") },
          modifier = Modifier.weight(2.1f),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Add item bar matching video (00:16)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFFF1F5F9))
          .border(1.dp, MohasebBorder, RoundedCornerShape(6.dp))
          .clickable { showAddItemDialog = true }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("add_item_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (lineItems.isEmpty()) "إكتب اسم الصنف أو انقر للإضافة..." else "إضافة صنف آخر (${lineItems.size} مضاف)",
          color = Color(0xFF64748B),
          fontSize = 14.sp
        )
        Box(
          modifier = Modifier
            .size(30.dp)
            .background(MohasebPrimaryBlue, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = "إضافة صنف", tint = Color.White, modifier = Modifier.size(20.dp))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Table Header Row
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp),
        color = Color(0xFF1E3A8A)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("الصنف", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
          Text("الكمية", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
          Text("السعر", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("الإجمالي", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
          Spacer(modifier = Modifier.width(28.dp))
        }
      }

      // Line items list
      if (lineItems.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "انقر على الشريط أعلاه لإضافة أصناف إلى الفاتورة",
            color = Color.Gray,
            fontSize = 14.sp
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          items(lineItems) { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(item.itemName, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
              Text("%.1f".format(item.quantity), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
              Text("%,.0f".format(item.unitPrice), fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
              Text("%,.0f".format(item.total), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center, maxLines = 1)

              IconButton(
                onClick = { lineItems.remove(item) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف الصنف", tint = Color.Red, modifier = Modifier.size(18.dp))
              }
            }
            Divider(color = Color(0xFFF1F5F9))
          }
        }
      }
    }
  }

  // Dialog: Add Item Line (Matching video 00:20 - 00:27)
  if (showAddItemDialog) {
    var selectedItem by remember { mutableStateOf<ItemEntity?>(availableItems.firstOrNull()) }
    var itemSearchText by remember { mutableStateOf(availableItems.firstOrNull()?.name.orEmpty()) }
    var showItemSuggestions by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("1.0") }
    var priceText by remember {
      mutableStateOf(
        selectedItem?.let {
          if (isSaleSide) it.sellingPrice.toString() else it.costPrice.toString()
        } ?: "500.0"
      )
    }

    val qVal = quantityText.toDoubleOrNull() ?: 0.0
    val pVal = priceText.toDoubleOrNull() ?: 0.0
    val lineTotal = qVal * pVal

    AlertDialog(
      onDismissRequest = { showAddItemDialog = false },
      title = {
        Text(
          text = selectedItem?.name ?: "إضافة صنف من المخزن",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = MohasebPrimaryBlue
        )
      },
      text = {
        Column {
          Text("ابحث عن الصنف بالاسم أو الرمز:", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(4.dp))

          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = itemSearchText,
              onValueChange = {
                itemSearchText = it
                selectedItem = null
                showItemSuggestions = true
              },
              label = { Text("الصنف") },
              placeholder = { Text("اكتب اسم الصنف أو رمزه...") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_invoice_item_search"),
              singleLine = true,
              trailingIcon = {
                IconButton(onClick = { showItemSuggestions = !showItemSuggestions }) {
                  Icon(Icons.Default.MoreVert, contentDescription = "عرض الأصناف")
                }
              }
            )
            DropdownMenu(
              expanded = showItemSuggestions,
              onDismissRequest = { showItemSuggestions = false },
              modifier = Modifier.fillMaxWidth(0.95f)
            ) {
              val matchingItems = availableItems.filter {
                itemSearchText.isBlank() ||
                  it.name.contains(itemSearchText.trim(), ignoreCase = true) ||
                  it.code.contains(itemSearchText.trim(), ignoreCase = true)
              }.take(50)
              if (matchingItems.isEmpty()) {
                DropdownMenuItem(
                  text = { Text("لا توجد أصناف مطابقة") },
                  onClick = { showItemSuggestions = false },
                  enabled = false
                )
              } else {
                matchingItems.forEach { itm ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(itm.name, fontWeight = FontWeight.Medium)
                        Text(
                          "الرمز: ${itm.code} • المتوفر: ${"%.2f".format(itm.currentStock)}",
                          fontSize = 11.sp,
                          color = Color.Gray
                        )
                      }
                    },
                    onClick = {
                      selectedItem = itm
                      itemSearchText = itm.name
                      priceText = if (isSaleSide) itm.sellingPrice.toString() else itm.costPrice.toString()
                      showItemSuggestions = false
                    }
                  )
                }
              }
            }
          }

          if (availableItems.isEmpty()) {
            Text("لا توجد أصناف في قاعدة البيانات؛ أضف صنفًا أولًا.", color = Color.Red, fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("الكمية") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_item_qty")
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("السعر") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_item_price")
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("%,.0f".format(lineTotal), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MohasebPrimaryBlue)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val itemToAdd = selectedItem
            if (itemToAdd != null && qVal > 0 && pVal >= 0 && qVal.isFinite() && pVal.isFinite()) {
              lineItems.add(
                InvoiceItemEntity(
                  invoiceId = 0,
                  itemId = itemToAdd.id,
                  itemName = itemToAdd.name,
                  quantity = qVal,
                  unitPrice = pVal,
                  total = lineTotal
                )
              )
              showAddItemDialog = false
            } else {
              viewModel.showMessage(if (itemToAdd == null) "اختر صنفًا من نتائج البحث أولًا" else "يرجى إدخال كمية وسعر صالحين")
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MohasebPrimaryBlue),
          modifier = Modifier.testTag("confirm_add_item_line")
        ) {
          Text("إضافة", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddItemDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Adjust Financials (Discount, Tax, Other fees, Paid) matching video 00:55
  if (showFinancialAdjustDialog) {
    var discountInput by remember { mutableStateOf(discount.toString()) }
    var taxInput by remember { mutableStateOf(taxPercent.toString()) }
    var otherFeesInput by remember { mutableStateOf(otherFees.toString()) }
    var paidInput by remember { mutableStateOf(paidAmount.toString()) }

    AlertDialog(
      onDismissRequest = { showFinancialAdjustDialog = false },
      title = { Text("الخصم والضريبة والدفعات", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = discountInput,
            onValueChange = { discountInput = it },
            label = { Text("الخصم التجاري") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = taxInput,
            onValueChange = { taxInput = it },
            label = { Text("الضريبة %") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = otherFeesInput,
            onValueChange = { otherFeesInput = it },
            label = { Text("رسوم أخرى / أجور نقل") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = paidInput,
            onValueChange = { paidInput = it },
            label = { Text("المبلغ المدفوع") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            discount = discountInput.toDoubleOrNull() ?: 0.0
            taxPercent = taxInput.toDoubleOrNull() ?: 0.0
            otherFees = otherFeesInput.toDoubleOrNull() ?: 0.0
            paidAmount = paidInput.toDoubleOrNull() ?: 0.0
            showFinancialAdjustDialog = false
          }
        ) {
          Text("موافق", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showFinancialAdjustDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Confirm Save (Matching video 00:39 & 01:00 "حفظ: نعم / لا")
  if (showSaveConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showSaveConfirmDialog = false },
      title = { Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      text = { Text("هل تريد حفظ هذه الفاتورة في السجلات؟", fontSize = 15.sp) },
      confirmButton = {
        TextButton(
          onClick = {
            showSaveConfirmDialog = false
            viewModel.saveInvoice(
              type = invoiceType,
              invoiceNumber = invoiceNumber,
              isCredit = isCredit,
              isReturn = isReturn,
              partyName = partyName.ifBlank { if (isSaleSide) "عميل عام" else "مورد عام" },
              date = invoiceDate,
              warehouse = warehouseName,
              currency = currency,
              items = lineItems.toList(),
              discount = discount,
              tax = taxAmount,
              otherFees = otherFees,
              paidAmount = paidAmount,
              notes = notes,
              editing = editingInvoice,
              onSuccess = onBack
            )
          },
          modifier = Modifier.testTag("confirm_save_yes")
        ) {
          Text("نعم", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveConfirmDialog = false }) {
          Text("لا")
        }
      }
    )
  }
}
