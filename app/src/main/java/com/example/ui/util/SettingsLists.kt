package com.example.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingEntity

object SettingKeys {
  const val CATEGORIES = "item_categories"
  const val GROUPS = "item_groups"
  const val UNITS = "item_units"
  const val DEFAULT_TAX = "default_tax_percent"
  const val PRINT_HEADER = "print_header"
  const val PIN_HASH = "app_pin_hash"
  const val POS_DEFAULT_CASH = "pos_default_cash"
  const val NOTIFY_BALANCES = "notify_show_balances"
  const val PRINTER_ADDRESS = "printer_bt_address"
  const val PRINTER_NAME = "printer_bt_name"
  const val PRINTER_WIDTH_80 = "printer_width_80"
}

val DEFAULT_UNITS = listOf("حبة", "كرتون", "كيلو", "متر", "لتر", "علبة", "طقم")
val DEFAULT_CATEGORIES = listOf("عام", "مواد بناء", "سباكة", "أدوات ورش", "كهرباء")

/** Reads a newline separated list stored in app_settings; falls back to [defaults] when nothing is saved. */
fun List<AppSettingEntity>.listSetting(key: String, defaults: List<String>): List<String> {
  val raw = firstOrNull { it.settingKey == key }?.settingValue
  if (raw == null) return defaults
  return raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}

fun List<AppSettingEntity>.stringSetting(key: String, default: String = ""): String =
  firstOrNull { it.settingKey == key }?.settingValue ?: default

fun List<AppSettingEntity>.boolSetting(key: String, default: Boolean = false): Boolean =
  firstOrNull { it.settingKey == key }?.settingValue?.toBooleanStrictOrNull() ?: default

@Composable
fun SuggestionRow(label: String, options: List<String>, onPick: (String) -> Unit) {
  if (options.isEmpty()) return
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("$label:", fontSize = 11.sp, color = Color(0xFF64748B))
    options.forEach { option ->
      Text(
        text = option,
        fontSize = 11.sp,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFFE2E8F0))
          .clickable { onPick(option) }
          .padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }
  }
}
