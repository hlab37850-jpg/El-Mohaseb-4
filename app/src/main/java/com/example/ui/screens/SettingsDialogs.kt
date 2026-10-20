package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.util.PinHasher
import com.example.ui.util.ReportTable
import com.example.ui.util.SettingKeys
import com.example.ui.util.ThermalPrinter
import com.example.ui.util.boolSetting
import com.example.ui.util.listSetting
import com.example.ui.util.stringSetting
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.launch

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = { Text(text, fontSize = 14.sp) },
    confirmButton = { TextButton(onClick = onDismiss) { Text("موافق", fontWeight = FontWeight.Bold) } }
  )
}

@Composable
fun SimpleTextSettingDialog(
  title: String,
  label: String,
  key: String,
  settings: List<AppSettingEntity>,
  viewModel: AccountingViewModel,
  keyboardType: KeyboardType = KeyboardType.Text,
  validate: (String) -> String? = { null },
  onDismiss: () -> Unit
) {
  var value by remember { mutableStateOf(settings.stringSetting(key)) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = {
      TextButton(onClick = {
        val error = validate(value.trim())
        if (error != null) viewModel.showMessage(error)
        else viewModel.saveSettings(mapOf(key to value.trim())) { onDismiss() }
      }) { Text("حفظ", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
  )
}

@Composable
fun ToggleSettingDialog(
  title: String,
  description: String,
  key: String,
  default: Boolean,
  settings: List<AppSettingEntity>,
  viewModel: AccountingViewModel,
  onDismiss: () -> Unit
) {
  var enabled by remember { mutableStateOf(settings.boolSetting(key, default)) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(description, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { enabled = it })
      }
    },
    confirmButton = {
      TextButton(onClick = {
        viewModel.saveSettings(mapOf(key to enabled.toString())) { onDismiss() }
      }) { Text("حفظ", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
  )
}

@Composable
fun ListEditorDialog(
  title: String,
  key: String,
  defaults: List<String>,
  settings: List<AppSettingEntity>,
  viewModel: AccountingViewModel,
  onDismiss: () -> Unit
) {
  val entries = remember { mutableStateListOf<String>().apply { addAll(settings.listSetting(key, defaults)) } }
  var newEntry by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        entries.forEachIndexed { index, entry ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(entry, fontSize = 14.sp)
            IconButton(onClick = { entries.removeAt(index) }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = newEntry,
          onValueChange = { newEntry = it },
          label = { Text("إضافة عنصر جديد") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
          onClick = {
            val v = newEntry.trim()
            if (v.isNotEmpty() && v !in entries) entries.add(v)
            newEntry = ""
          },
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) { Text("إضافة") }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        viewModel.saveSettings(mapOf(key to entries.joinToString("\n"))) { onDismiss() }
      }) { Text("حفظ", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
  )
}

@Composable
fun SecurityDialog(settings: List<AppSettingEntity>, viewModel: AccountingViewModel, onDismiss: () -> Unit) {
  val existingHash = settings.stringSetting(SettingKeys.PIN_HASH)
  var current by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirm by remember { mutableStateOf("") }
  val numeric = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("الرمز السري للتطبيق", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          if (existingHash.isBlank()) "لا يوجد رمز سري حاليًا. عيّن رمزًا من 4 إلى 12 رقمًا ليُطلب عند فتح التطبيق."
          else "الرمز السري مفعّل. لا توجد طريقة لاسترجاعه إذا نسيته إلا بمسح بيانات التطبيق، فاحتفظ بنسخة احتياطية.",
          fontSize = 12.5.sp,
          color = Color(0xFF475569)
        )
        if (existingHash.isNotBlank()) {
          OutlinedTextField(
            value = current, onValueChange = { current = it.filter { c -> c.isDigit() } },
            label = { Text("الرمز الحالي") }, singleLine = true, keyboardOptions = numeric,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
          )
        }
        OutlinedTextField(
          value = newPin, onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(12) },
          label = { Text("الرمز الجديد") }, singleLine = true, keyboardOptions = numeric,
          visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = confirm, onValueChange = { confirm = it.filter { c -> c.isDigit() }.take(12) },
          label = { Text("تأكيد الرمز الجديد") }, singleLine = true, keyboardOptions = numeric,
          visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      TextButton(onClick = {
        when {
          existingHash.isNotBlank() && PinHasher.hash(current) != existingHash ->
            viewModel.showMessage("الرمز الحالي غير صحيح")
          newPin.length < 4 -> viewModel.showMessage("الرمز يجب ألا يقل عن 4 أرقام")
          newPin != confirm -> viewModel.showMessage("تأكيد الرمز غير مطابق")
          else -> viewModel.saveSettings(mapOf(SettingKeys.PIN_HASH to PinHasher.hash(newPin))) { onDismiss() }
        }
      }) { Text("حفظ", fontWeight = FontWeight.Bold) }
    },
    dismissButton = {
      Row {
        if (existingHash.isNotBlank()) {
          TextButton(onClick = {
            if (PinHasher.hash(current) != existingHash) viewModel.showMessage("أدخل الرمز الحالي لإلغاء القفل")
            else viewModel.saveSettings(mapOf(SettingKeys.PIN_HASH to "")) { onDismiss() }
          }) { Text("إلغاء القفل", color = Color.Red) }
        }
        TextButton(onClick = onDismiss) { Text("إغلاق") }
      }
    }
  )
}

@Composable
fun BackupDialog(onBackup: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("النسخ الاحتياطي", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تُحفظ النسخة كملف واحد يمكنك وضعه في الذاكرة أو جوجل درايف أو أي موقع تختاره.", fontSize = 13.sp)
        Button(onClick = { onDismiss(); onBackup() }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء نسخة احتياطية الآن") }
        OutlinedButton(onClick = { onDismiss(); onRestore() }, modifier = Modifier.fillMaxWidth()) { Text("استرجاع من نسخة") }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
  )
}

@Composable
fun ThermalPrinterDialog(settings: List<AppSettingEntity>, viewModel: AccountingViewModel, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var enabled by remember { mutableStateOf(settings.boolSetting("printer_thermal_enabled", false)) }
  var autoCut by remember { mutableStateOf(settings.boolSetting("printer_auto_cut", false)) }
  var wide by remember { mutableStateOf(settings.boolSetting(SettingKeys.PRINTER_WIDTH_80, true)) }
  var address by remember { mutableStateOf(settings.stringSetting(SettingKeys.PRINTER_ADDRESS)) }
  var deviceName by remember { mutableStateOf(settings.stringSetting(SettingKeys.PRINTER_NAME)) }
  var devices by remember { mutableStateOf(ThermalPrinter.pairedDevices(context)) }

  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) devices = ThermalPrinter.pairedDevices(context)
    else viewModel.showMessage("لا يمكن اختيار الطابعة بدون صلاحية البلوتوث")
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("إعدادات الطابعة الحرارية", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
          Text("الطباعة عبر البلوتوث", fontSize = 14.sp)
          Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
          Text("عرض الورق 80mm (وإلا 58mm)", fontSize = 14.sp)
          Switch(checked = wide, onCheckedChange = { wide = it })
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
          Text("قطع الورق بعد كل طباعة", fontSize = 14.sp)
          Switch(checked = autoCut, onCheckedChange = { autoCut = it })
        }
        Text(
          if (address.isBlank()) "لم تُحدد طابعة" else "الطابعة: ${deviceName.ifBlank { address }}",
          fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
        OutlinedButton(
          onClick = {
            if (!ThermalPrinter.hasPermission(context) && Build.VERSION.SDK_INT >= 31) {
              permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
              devices = ThermalPrinter.pairedDevices(context)
              if (devices.isEmpty()) viewModel.showMessage("لا توجد أجهزة مقترنة؛ اقترن بالطابعة من إعدادات البلوتوث أولًا")
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) { Text("البحث في الأجهزة المقترنة") }
        devices.forEach { (name, addr) ->
          Text(
            text = "• $name",
            fontSize = 13.sp,
            color = if (addr == address) MohasebPrimaryBlue else Color(0xFF334155),
            fontWeight = if (addr == address) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth().clickable { address = addr; deviceName = name }.padding(vertical = 6.dp)
          )
        }
        OutlinedButton(
          onClick = {
            if (address.isBlank()) {
              viewModel.showMessage("اختر الطابعة أولًا")
            } else {
              scope.launch {
                runCatching {
                  ThermalPrinter.print(
                    context, address,
                    ReportTable("اختبار الطباعة", listOf("البند", "القيمة"), listOf(listOf("المحاسب", "يعمل"))),
                    wide, autoCut
                  )
                }.onSuccess { viewModel.showMessage("تم إرسال صفحة الاختبار") }
                  .onFailure { viewModel.showMessage(it.message ?: "تعذر الاتصال بالطابعة") }
              }
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) { Text("طباعة صفحة اختبار") }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        viewModel.saveSettings(
          mapOf(
            "printer_thermal_enabled" to enabled.toString(),
            "printer_auto_cut" to autoCut.toString(),
            SettingKeys.PRINTER_WIDTH_80 to wide.toString(),
            SettingKeys.PRINTER_ADDRESS to address,
            SettingKeys.PRINTER_NAME to deviceName
          )
        ) { onDismiss() }
      }) { Text("موافق", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
  )
}
