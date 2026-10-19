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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.model.CurrencyEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrenciesScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val currencies by viewModel.allCurrencies.collectAsState()
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedCurrencyForDetails by remember { mutableStateOf<CurrencyEntity?>(null) }
  var currencyToEdit by remember { mutableStateOf<CurrencyEntity?>(null) }
  var currencyToDelete by remember { mutableStateOf<CurrencyEntity?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("العملات وأسعار الصرف", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_currencies")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
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
          Text("عدد العملات: ${currencies.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

          FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color.White,
            contentColor = MohasebPrimaryBlue,
            shape = CircleShape,
            modifier = Modifier
              .size(44.dp)
              .testTag("add_currency_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة عملة", modifier = Modifier.size(26.dp))
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
      // Table Header matching video 02:37
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
          Text("العملة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
          Text("الفكة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("رمز العملة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
          Text("سعر الصرف", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
        }
      }

      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(currencies) { curr ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                selectedCurrencyForDetails = curr
              }
              .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(curr.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(curr.subUnit, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
            Text(curr.symbol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MohasebPrimaryBlue, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center, maxLines = 1)
            Text(
              text = if (curr.exchangeRate == 1.0) "1.00 (أساسي)" else "%,.2f".format(curr.exchangeRate),
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1.4f),
              textAlign = TextAlign.Center,
              maxLines = 1
            )
          }
          Divider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
        }
      }
    }
  }

  // Dialog: Add Currency matching video 02:38
  if (showAddDialog) {
    var name by remember { mutableStateOf("") }
    var subUnit by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("1.0") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("إضافة عملة جديدة", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          if (errorMsg != null) {
            Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
          }
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              errorMsg = null
            },
            label = { Text("اسم العملة *") },
            modifier = Modifier.fillMaxWidth().testTag("input_currency_name")
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = subUnit,
            onValueChange = { subUnit = it },
            label = { Text("الفكة (مثال: فلس، سنت)") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it },
            label = { Text("رمز العملة (مثال: YR, SAR, USD) *") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = rateInput,
            onValueChange = { rateInput = it },
            label = { Text("سعر الصرف مقابل العملة الأساسية") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val rate = rateInput.toDoubleOrNull() ?: 1.0
            if (name.isBlank() || symbol.isBlank()) {
              errorMsg = "يرجى إدخال اسم ورمز العملة"
            } else {
              viewModel.addCurrency(name.trim(), symbol.trim(), subUnit.trim().ifBlank { "جزء" }, rate) {
                showAddDialog = false
              }
            }
          },
          modifier = Modifier.testTag("submit_currency_button")
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

  // Dialog: Currency Details & Actions
  selectedCurrencyForDetails?.let { curr ->
    AlertDialog(
      onDismissRequest = { selectedCurrencyForDetails = null },
      title = { Text("${curr.name} (${curr.symbol})", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("الفكة: ${curr.subUnit}", fontSize = 13.sp)
          Text("سعر الصرف: %,.2f".format(curr.exchangeRate), fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("رمز العملة: ${curr.symbol}", fontSize = 13.sp, color = MohasebPrimaryBlue)
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              viewModel.setActiveCurrency(curr.name)
              viewModel.showMessage("تم تعيين ${curr.name} كعملة افتراضية للنظام")
              selectedCurrencyForDetails = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
          ) {
            Icon(Icons.Default.Check, contentDescription = "تعيين كافتراضية", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("تعيين")
          }

          Button(
            onClick = {
              currencyToEdit = curr
              selectedCurrencyForDetails = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MohasebPrimaryBlue)
          ) {
            Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("تعديل")
          }

          if (curr.name != "محلي") {
            Button(
              onClick = {
                currencyToDelete = curr
                selectedCurrencyForDetails = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
              Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("حذف")
            }
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedCurrencyForDetails = null }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: Edit Currency
  currencyToEdit?.let { targetCurr ->
    var name by remember { mutableStateOf(targetCurr.name) }
    var symbol by remember { mutableStateOf(targetCurr.symbol) }
    var subUnit by remember { mutableStateOf(targetCurr.subUnit) }
    var rateInput by remember { mutableStateOf(targetCurr.exchangeRate.toString()) }

    AlertDialog(
      onDismissRequest = { currencyToEdit = null },
      title = { Text("تعديل بيانات العملة", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم العملة") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = subUnit,
            onValueChange = { subUnit = it },
            label = { Text("الفكة") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it },
            label = { Text("رمز العملة") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = rateInput,
            onValueChange = { rateInput = it },
            label = { Text("سعر الصرف") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val r = rateInput.toDoubleOrNull() ?: targetCurr.exchangeRate
            if (name.isNotBlank()) {
              viewModel.updateCurrency(
                targetCurr.copy(
                  name = name.trim(),
                  symbol = symbol.trim(),
                  subUnit = subUnit.trim(),
                  exchangeRate = r
                )
              ) {
                currencyToEdit = null
              }
            }
          }
        ) {
          Text("حفظ التعديل", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { currencyToEdit = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Confirm Delete Currency
  currencyToDelete?.let { curr ->
    AlertDialog(
      onDismissRequest = { currencyToDelete = null },
      title = { Text("تأكيد حذف العملة", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
      text = { Text("هل تريد حذف العملة \"${curr.name}\" من قاعدة البيانات؟") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCurrency(curr) {
              currencyToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("نعم، احذف")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { currencyToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}
