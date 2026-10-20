package com.example.ui.screens

import com.example.ui.util.rememberExportActions
import com.example.ui.util.DEFAULT_UNITS
import com.example.ui.util.DEFAULT_CATEGORIES
import com.example.ui.util.SettingKeys
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

data class SettingItem(val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: AccountingViewModel,
  onBack: () -> Unit,
  onBackupRequested: () -> Unit = {},
  onRestoreRequested: () -> Unit = {}
) {
  var showCompanyProfileDialog by remember { mutableStateOf(false) }
  var showPrinterDialog by remember { mutableStateOf(false) }
  var activeDialog by remember { mutableStateOf<String?>(null) }
  val exports = rememberExportActions(viewModel)
  val allItemsForBarcodes by viewModel.allItems.collectAsState()
  val savedSettings by viewModel.allSettings.collectAsState()
  val settingsMap = savedSettings.associate { it.settingKey to it.settingValue }

  val settingsList = listOf(
    SettingItem("البيانات الشخصية والشركة", Icons.Default.Person),
    SettingItem("خيارات الطباعة", Icons.Default.Print),
    SettingItem("خيارات الأمان وكلمات المرور", Icons.Default.Lock),
    SettingItem("المستخدمين والصلاحيات", Icons.Default.Security),
    SettingItem("التصنيفات", Icons.Default.Category),
    SettingItem("مجموعة الصنف", Icons.Default.Badge),
    SettingItem("وحدات القياس", Icons.Default.Scale),
    SettingItem("خيارات حفظ البيانات والنسخ الاحتياطي", Icons.Default.Save),
    SettingItem("الطابعة الحرارية (80mm / Bluetooth)", Icons.Default.Print),
    SettingItem("الضريبة المضافة", Icons.Default.Tune),
    SettingItem("طباعة باركود الأصناف", Icons.Default.QrCode),
    SettingItem("خيارات الإشعارات", Icons.Default.Notifications),
    SettingItem("خيارات أخرى ونقاط البيع", Icons.Default.DisplaySettings),
    SettingItem("تفعيل الإشتراك", Icons.Default.Star)
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("الإعدادات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_settings")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MohasebPrimaryBlue)
      )
    },
    containerColor = Color(0xFFF8FAFC)
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      items(settingsList) { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              when (item.title) {
                "البيانات الشخصية والشركة" -> showCompanyProfileDialog = true
                "الطابعة الحرارية (80mm / Bluetooth)" -> showPrinterDialog = true
                "طباعة باركود الأصناف" -> exports.barcodes(allItemsForBarcodes)
                else -> activeDialog = item.title
              }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(item.icon, contentDescription = item.title, tint = MohasebPrimaryBlue, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(16.dp))
          Text(item.title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Divider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
      }
    }
  }

  // Dialog: Company Profile (video 03:05)
  if (showCompanyProfileDialog) {
    var compName by remember(showCompanyProfileDialog, settingsMap) { mutableStateOf(settingsMap["company_name"].orEmpty()) }
    var compAddress by remember(showCompanyProfileDialog, settingsMap) { mutableStateOf(settingsMap["company_address"].orEmpty()) }
    var compPhone by remember(showCompanyProfileDialog, settingsMap) { mutableStateOf(settingsMap["company_phone"].orEmpty()) }
    var compTax by remember(showCompanyProfileDialog, settingsMap) { mutableStateOf(settingsMap["company_tax_number"].orEmpty()) }

    AlertDialog(
      onDismissRequest = { showCompanyProfileDialog = false },
      title = { Text("البيانات الشخصية وبيانات المنشأة", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          OutlinedTextField(value = compName, onValueChange = { compName = it }, label = { Text("اسم المؤسسة / المتجر") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = compAddress, onValueChange = { compAddress = it }, label = { Text("العنوان") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = compPhone, onValueChange = { compPhone = it }, label = { Text("رقم التلفون") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(value = compTax, onValueChange = { compTax = it }, label = { Text("الرقم الضريبي") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        TextButton(onClick = {
          if (compName.isBlank()) {
            viewModel.showMessage("اسم المؤسسة مطلوب")
          } else {
            viewModel.saveSettings(
              mapOf(
                "company_name" to compName.trim(),
                "company_address" to compAddress.trim(),
                "company_phone" to compPhone.trim(),
                "company_tax_number" to compTax.trim()
              )
            ) { showCompanyProfileDialog = false }
          }
        }) {
          Text("حفظ", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCompanyProfileDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  if (showPrinterDialog) {
    ThermalPrinterDialog(settings = savedSettings, viewModel = viewModel, onDismiss = { showPrinterDialog = false })
  }

  val closeDialog = { activeDialog = null }
  when (activeDialog) {
    "خيارات الطباعة" -> SimpleTextSettingDialog(
      title = "خيارات الطباعة",
      label = "سطر الترويسة (يظهر أعلى كل تقرير وفاتورة مطبوعة)",
      key = SettingKeys.PRINT_HEADER,
      settings = savedSettings,
      viewModel = viewModel,
      onDismiss = closeDialog
    )
    "خيارات الأمان وكلمات المرور" -> SecurityDialog(savedSettings, viewModel, closeDialog)
    "المستخدمين والصلاحيات" -> InfoDialog(
      "المستخدمين والصلاحيات",
      "التطبيق يعمل حاليًا بحساب واحد. لحماية البيانات فعّل الرمز السري من «خيارات الأمان وكلمات المرور». تعدد المستخدمين بصلاحيات منفصلة غير مفعّل في هذا الإصدار.",
      closeDialog
    )
    "التصنيفات" -> ListEditorDialog("التصنيفات", SettingKeys.CATEGORIES, DEFAULT_CATEGORIES, savedSettings, viewModel, closeDialog)
    "مجموعة الصنف" -> ListEditorDialog("مجموعات الأصناف", SettingKeys.GROUPS, emptyList(), savedSettings, viewModel, closeDialog)
    "وحدات القياس" -> ListEditorDialog("وحدات القياس", SettingKeys.UNITS, DEFAULT_UNITS, savedSettings, viewModel, closeDialog)
    "خيارات حفظ البيانات والنسخ الاحتياطي" -> BackupDialog(onBackupRequested, onRestoreRequested, closeDialog)
    "الضريبة المضافة" -> SimpleTextSettingDialog(
      title = "الضريبة المضافة",
      label = "نسبة الضريبة الافتراضية % (0 = بدون)",
      key = SettingKeys.DEFAULT_TAX,
      settings = savedSettings,
      viewModel = viewModel,
      keyboardType = KeyboardType.Decimal,
      validate = { v ->
        val n = if (v.isBlank()) 0.0 else v.replace(',', '.').toDoubleOrNull()
        if (n == null || n < 0.0 || n > 100.0) "أدخل نسبة بين 0 و 100" else null
      },
      onDismiss = closeDialog
    )
    "خيارات الإشعارات" -> ToggleSettingDialog(
      "خيارات الإشعارات", "عرض أرصدة العملاء المستحقة ضمن التنبيهات",
      SettingKeys.NOTIFY_BALANCES, true, savedSettings, viewModel, closeDialog
    )
    "خيارات أخرى ونقاط البيع" -> ToggleSettingDialog(
      "نقاط البيع", "جعل فاتورة البيع الجديدة نقدية افتراضيًا",
      SettingKeys.POS_DEFAULT_CASH, false, savedSettings, viewModel, closeDialog
    )
    "تفعيل الإشتراك" -> InfoDialog(
      "الاشتراك",
      "هذه النسخة تعمل بالكامل دون اشتراك أو تفعيل، وجميع بياناتك محفوظة على جهازك.",
      closeDialog
    )
    else -> Unit
  }
}
