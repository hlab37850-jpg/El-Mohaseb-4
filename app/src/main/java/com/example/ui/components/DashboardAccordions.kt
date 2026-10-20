package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohasebBorder
import com.example.ui.theme.MohasebPrimaryBlue

@Composable
fun DashboardAccordions(
  onWarehouseOpClick: (String) -> Unit,
  onAccountsOpClick: (String) -> Unit,
  onItemsOpClick: (String) -> Unit,
  onCurrenciesOpClick: (String) -> Unit,
  onReportsOpClick: (String) -> Unit
) {
  // Track open state for each of the 5 accordions
  var warehouseExpanded by remember { mutableStateOf(false) }
  var accountsExpanded by remember { mutableStateOf(false) }
  var itemsExpanded by remember { mutableStateOf(false) }
  var currenciesExpanded by remember { mutableStateOf(false) }
  var reportsExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. عمليات مخزنية
    AccordionSection(
      title = "عمليات مخزنية",
      isExpanded = warehouseExpanded,
      onToggle = {
        warehouseExpanded = !warehouseExpanded
        if (warehouseExpanded) {
          accountsExpanded = false
          itemsExpanded = false
          currenciesExpanded = false
          reportsExpanded = false
        }
      },
      items = listOf(
        "صرف مخزني",
        "توريد مخزني",
        "تحويل مخزني",
        "تسوية مخزنية",
        "إضافة مخزن",
        "جرد مخزني"
      ),
      onItemClick = onWarehouseOpClick,
      testTag = "accordion_warehouse"
    )

    // 2. قيود وحسابات
    AccordionSection(
      title = "قيود وحسابات",
      isExpanded = accountsExpanded,
      onToggle = {
        accountsExpanded = !accountsExpanded
        if (accountsExpanded) {
          warehouseExpanded = false
          itemsExpanded = false
          currenciesExpanded = false
          reportsExpanded = false
        }
      },
      items = listOf(
        "قيد يومي",
        "قيد إفتتاحي",
        "إضافة حساب",
        "حركة الصندوق",
        "دليل الحسابات",
        "إقفال سنوي"
      ),
      onItemClick = onAccountsOpClick,
      testTag = "accordion_accounts"
    )

    // 3. أصناف
    AccordionSection(
      title = "أصناف",
      isExpanded = itemsExpanded,
      onToggle = {
        itemsExpanded = !itemsExpanded
        if (itemsExpanded) {
          warehouseExpanded = false
          accountsExpanded = false
          currenciesExpanded = false
          reportsExpanded = false
        }
      },
      items = listOf(
        "الأصناف",
        "أسعار البيع",
        "وحدات الصنف",
        "فاتورة عرض سعر",
        "طلب شراء"
      ),
      onItemClick = onItemsOpClick,
      testTag = "accordion_items"
    )

    // 4. العملات
    AccordionSection(
      title = "العملات",
      isExpanded = currenciesExpanded,
      onToggle = {
        currenciesExpanded = !currenciesExpanded
        if (currenciesExpanded) {
          warehouseExpanded = false
          accountsExpanded = false
          itemsExpanded = false
          reportsExpanded = false
        }
      },
      items = listOf(
        "إضافة عملة",
        "سعر العملات",
        "سقف الحساب"
      ),
      onItemClick = onCurrenciesOpClick,
      testTag = "accordion_currencies"
    )

    // 5. التقارير
    AccordionSection(
      title = "التقارير",
      isExpanded = reportsExpanded,
      onToggle = {
        reportsExpanded = !reportsExpanded
        if (reportsExpanded) {
          warehouseExpanded = false
          accountsExpanded = false
          itemsExpanded = false
          currenciesExpanded = false
        }
      },
      items = listOf(
        "حركة الأصناف",
        "ميزان المراجعة",
        "قائمة الدخل",
        "المركز المالي",
        "تقارير أخرى"
      ),
      onItemClick = onReportsOpClick,
      testTag = "accordion_reports"
    )
  }
}

@Composable
fun AccordionSection(
  title: String,
  isExpanded: Boolean,
  onToggle: () -> Unit,
  items: List<String>,
  onItemClick: (String) -> Unit,
  testTag: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .border(1.dp, MohasebBorder, RoundedCornerShape(8.dp))
  ) {
    // Header Bar — 52.dp for better touch target across devices
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .clickable { onToggle() }
        .testTag(testTag),
      color = MohasebPrimaryBlue
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = if (isExpanded) "طي" else "توسيع",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = title,
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }
    }

    // Expandable content
    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White)
      ) {
        items.forEachIndexed { index, subItem ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onItemClick(subItem) }
              .padding(horizontal = 16.dp, vertical = 13.dp)
              .testTag("item_" + subItem.replace(" ", "_")),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = subItem,
              color = Color(0xFF263238),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }
          if (index < items.size - 1) {
            Divider(color = Color(0xFFEAEFF5), thickness = 0.8.dp)
          }
        }
      }
    }
  }
}
