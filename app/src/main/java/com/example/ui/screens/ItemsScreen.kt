package com.example.ui.screens

import com.example.ui.util.listSetting
import com.example.ui.util.SuggestionRow
import com.example.ui.util.DEFAULT_UNITS
import com.example.ui.util.DEFAULT_CATEGORIES
import com.example.ui.util.SettingKeys
import androidx.compose.material.icons.filled.QrCode
import com.example.ui.util.ListTables
import com.example.ui.util.rememberExportActions
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.ItemEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val items by viewModel.allItems.collectAsState()
  val exports = rememberExportActions(viewModel)
  val savedSettings by viewModel.allSettings.collectAsState()
  val categorySuggestions = (savedSettings.listSetting(SettingKeys.CATEGORIES, DEFAULT_CATEGORIES) +
    savedSettings.listSetting(SettingKeys.GROUPS, emptyList())).distinct()
  val unitSuggestions = savedSettings.listSetting(SettingKeys.UNITS, DEFAULT_UNITS)
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedItemForDetails by remember { mutableStateOf<ItemEntity?>(null) }
  var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
  var itemToDelete by remember { mutableStateOf<ItemEntity?>(null) }

  val filteredItems = items.filter {
    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery)
  }

  val totalStockCount = filteredItems.sumOf { it.currentStock }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "دليل الأصناف",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_items")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = { exports.barcodes(items) }) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color(0xFF455A64), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.QrCode, contentDescription = "باركود", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
          IconButton(onClick = { exports.excel(ListTables.items(items)) }) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color(0xFF2E7D32), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Description, contentDescription = "Excel", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
          IconButton(onClick = { exports.pdf(ListTables.items(items)) }) {
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
            text = "إجمالي المخزون: %,.0f قطعة".format(totalStockCount),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )

          FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color.White,
            contentColor = MohasebPrimaryBlue,
            shape = CircleShape,
            modifier = Modifier
              .size(44.dp)
              .testTag("add_item_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة صنف", modifier = Modifier.size(26.dp))
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
      // Search Box
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("بحث عن اسم أو كود الصنف...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("search_items_input"),
        singleLine = true
      )

      // Table Header Row matching video 02:19
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp),
        color = Color(0xFF0F172A)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("إسم الصنف", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
          Text("المجموعة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("التكلفة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("سعر البيع", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("المتاح", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
      }

      if (filteredItems.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "لا توجد أصناف مطابقة...",
            color = Color(0xFF64748B),
            fontSize = 16.sp
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(filteredItems) { itm ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedItemForDetails = itm
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(itm.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
              Text(itm.category, fontSize = 11.5.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
              Text("%,.0f".format(itm.costPrice), fontSize = 11.5.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
              Text("%,.0f".format(itm.sellingPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
              Text(
                text = "%.0f".format(itm.currentStock),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (itm.currentStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.weight(1f),
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

  // Dialog: Add Item matching video 02:20
  if (showAddDialog) {
    var itemName by remember { mutableStateOf("") }
    var itemCategory by remember { mutableStateOf("عام") }
    var itemUnit by remember { mutableStateOf("حبة") }
    var costPriceInput by remember { mutableStateOf("") }
    var sellPriceInput by remember { mutableStateOf("") }
    var openingQtyInput by remember { mutableStateOf("0.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("إضافة صنف جديد", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
          }
          OutlinedTextField(
            value = itemName,
            onValueChange = {
              itemName = it
              errorMessage = null
            },
            label = { Text("إسم الصنف *") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_new_item_name")
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = itemCategory,
            onValueChange = { itemCategory = it },
            label = { Text("مجموعة الصنف") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = itemUnit,
              onValueChange = { itemUnit = it },
              label = { Text("الوحدة") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = openingQtyInput,
              onValueChange = { openingQtyInput = it },
              label = { Text("الكمية الافتتاحية") },
              modifier = Modifier.weight(1f)
            )
          }
          SuggestionRow("المجموعات", categorySuggestions) { itemCategory = it }
          SuggestionRow("الوحدات", unitSuggestions) { itemUnit = it }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = costPriceInput,
              onValueChange = { costPriceInput = it },
              label = { Text("تكلفة الوحدة") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sellPriceInput,
              onValueChange = { sellPriceInput = it },
              label = { Text("سعر البيع *") },
              modifier = Modifier.weight(1f)
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val cost = costPriceInput.toDoubleOrNull() ?: 0.0
            val sell = sellPriceInput.toDoubleOrNull() ?: 0.0
            val qty = openingQtyInput.toDoubleOrNull() ?: 0.0
            if (itemName.isBlank()) {
              errorMessage = "يرجى كتابة اسم الصنف"
            } else if (sell <= 0) {
              errorMessage = "يرجى كتابة سعر بيع صالح أكبر من 0"
            } else {
              viewModel.addItem(
                code = "ITM-" + (System.currentTimeMillis() % 1000),
                name = itemName.trim(),
                category = itemCategory.trim().ifBlank { "عام" },
                unit = itemUnit.trim().ifBlank { "حبة" },
                costPrice = cost,
                sellingPrice = sell,
                openingQty = qty
              ) {
                showAddDialog = false
              }
            }
          },
          modifier = Modifier.testTag("submit_new_item_button")
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

  // Dialog: Item Details & Actions (Read & Edit/Delete triggers)
  selectedItemForDetails?.let { itm ->
    val valuation = itm.currentStock * itm.costPrice
    AlertDialog(
      onDismissRequest = { selectedItemForDetails = null },
      title = { Text(itm.name, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("كود الصنف: ${itm.code}", fontSize = 13.sp)
          Text("المجموعة: ${itm.category} • الوحدة: ${itm.unit}", fontSize = 13.sp)
          Text("تكلفة الشراء: %,.0f ر.ي".format(itm.costPrice), fontSize = 13.sp)
          Text("سعر البيع: %,.0f ر.ي".format(itm.sellingPrice), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue)
          Text(
            text = "الكمية المتاحة: %.1f %s".format(itm.currentStock, itm.unit),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (itm.currentStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
          )
          Text("إجمالي قيمة المخزون: %,.0f ر.ي".format(valuation), fontSize = 12.5.sp, color = Color.Gray)
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              itemToEdit = itm
              selectedItemForDetails = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MohasebPrimaryBlue)
          ) {
            Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("تعديل")
          }

          Button(
            onClick = {
              itemToDelete = itm
              selectedItemForDetails = null
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
        TextButton(onClick = { selectedItemForDetails = null }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: Edit Item (Update)
  itemToEdit?.let { targetItm ->
    var name by remember { mutableStateOf(targetItm.name) }
    var category by remember { mutableStateOf(targetItm.category) }
    var unit by remember { mutableStateOf(targetItm.unit) }
    var costPriceInput by remember { mutableStateOf(targetItm.costPrice.toString()) }
    var sellPriceInput by remember { mutableStateOf(targetItm.sellingPrice.toString()) }
    var stockInput by remember { mutableStateOf(targetItm.currentStock.toString()) }
    var editError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { itemToEdit = null },
      title = { Text("تعديل بيانات الصنف", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          if (editError != null) {
            Text(editError!!, color = Color.Red, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
          }
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              editError = null
            },
            label = { Text("إسم الصنف *") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("المجموعة") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = unit,
              onValueChange = { unit = it },
              label = { Text("الوحدة") },
              modifier = Modifier.weight(1f)
            )
          }
          SuggestionRow("المجموعات", categorySuggestions) { category = it }
          SuggestionRow("الوحدات", unitSuggestions) { unit = it }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = costPriceInput,
              onValueChange = { costPriceInput = it },
              label = { Text("سعر التكلفة") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sellPriceInput,
              onValueChange = { sellPriceInput = it },
              label = { Text("سعر البيع *") },
              modifier = Modifier.weight(1f)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = stockInput,
            onValueChange = { stockInput = it },
            label = { Text("الرصيد المتاح بالمخزن") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val cost = costPriceInput.toDoubleOrNull() ?: 0.0
            val sell = sellPriceInput.toDoubleOrNull() ?: 0.0
            val stock = stockInput.toDoubleOrNull() ?: 0.0
            if (name.isBlank()) {
              editError = "اسم الصنف مطلوب"
            } else if (sell <= 0) {
              editError = "سعر البيع يجب أن يكون أكبر من 0"
            } else {
              viewModel.updateItem(
                targetItm.copy(
                  name = name.trim(),
                  category = category.trim().ifBlank { "عام" },
                  unit = unit.trim().ifBlank { "حبة" },
                  costPrice = cost,
                  sellingPrice = sell,
                  currentStock = stock
                )
              ) {
                itemToEdit = null
              }
            }
          }
        ) {
          Text("حفظ التعديل", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { itemToEdit = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Confirm Delete Item (Delete)
  itemToDelete?.let { itm ->
    AlertDialog(
      onDismissRequest = { itemToDelete = null },
      title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
      text = { Text("هل أنت متأكد من حذف الصنف \"${itm.name}\" بشكل نهائي؟") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteItem(itm) {
              itemToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("نعم، احذف")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { itemToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}
