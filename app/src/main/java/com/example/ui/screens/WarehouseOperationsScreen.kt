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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.WarehouseOperationEntity
import com.example.ui.theme.MohasebPrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseOperationsScreen(
  initialOpType: String = "الكل",
  viewModel: AccountingViewModel,
  onBack: () -> Unit
) {
  val operations by viewModel.allOperations.collectAsState()
  val warehouses by viewModel.allWarehouses.collectAsState()
  val activeWarehouse by viewModel.activeWarehouse.collectAsState()

  var selectedTab by remember {
    mutableIntStateOf(
      when {
        initialOpType.contains("توريد") -> 1
        initialOpType.contains("صرف") -> 2
        initialOpType.contains("تحويل") -> 3
        initialOpType.contains("تسوية") -> 4
        initialOpType.contains("جرد") -> 5
        else -> 0
      }
    )
  }
  val tabs = listOf("الكل", "توريد", "صرف", "تحويل", "تسوية", "جرد")

  var showNewOpDialog by remember { mutableStateOf(false) }
  var selectedOpForDetails by remember { mutableStateOf<WarehouseOperationEntity?>(null) }
  var opToDelete by remember { mutableStateOf<WarehouseOperationEntity?>(null) }

  val filteredOps = operations.filter { op ->
    when (selectedTab) {
      1 -> op.operationType.contains("توريد")
      2 -> op.operationType.contains("صرف")
      3 -> op.operationType.contains("تحويل")
      4 -> op.operationType.contains("تسوية")
      5 -> op.operationType.contains("جرد")
      else -> true
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "العمليات المخزنية",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_wh_ops")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = { viewModel.showMessage("تصدير تقرير العمليات المخزنية كـ PDF...") }) {
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
            text = "المخزن: $activeWarehouse",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )

          FloatingActionButton(
            onClick = { showNewOpDialog = true },
            containerColor = Color.White,
            contentColor = MohasebPrimaryBlue,
            shape = CircleShape,
            modifier = Modifier
              .size(44.dp)
              .testTag("add_warehouse_op_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "عملية جديدة", modifier = Modifier.size(26.dp))
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
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = MohasebPrimaryBlue
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
          )
        }
      }

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp),
        color = Color(0xFF0F172A)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("العملية", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
          Text("المخزن", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f), textAlign = TextAlign.Center)
          Text("التاريخ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
          Text("التكلفة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
        }
      }

      if (filteredOps.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "لا توجد عمليات... اضغط (+) للإضافة",
            color = Color(0xFF64748B),
            fontSize = 16.sp
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(filteredOps) { op ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedOpForDetails = op
                }
                .padding(vertical = 12.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(op.operationType, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
              Text(op.warehouseName, fontSize = 11.5.sp, color = Color(0xFF334155), modifier = Modifier.weight(1.8f), textAlign = TextAlign.Center, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
              Text(op.date, fontSize = 11.5.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
              Text("%,.0f".format(op.totalCost), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1)
            }
            Divider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
          }
        }
      }
    }
  }

  // Dialog: New Warehouse Operation matching video (01:18 - 01:46)
  if (showNewOpDialog) {
    var opType by remember { mutableStateOf("توريد مخزني") }
    var selectedWh by remember { mutableStateOf(activeWarehouse) }
    var targetWh by remember { mutableStateOf(warehouses.lastOrNull()?.name ?: "مخزن فرع المبيعات") }
    var partyInput by remember { mutableStateOf("") }
    var itemsCountInput by remember { mutableStateOf("1") }
    var totalCostInput by remember { mutableStateOf("0.0") }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showNewOpDialog = false },
      title = { Text("تسجيل عملية مخزنية جديدة", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column {
          Text("نوع العملية:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("توريد مخزني", "صرف مخزني", "تحويل مخزني").forEach { type ->
              val isSel = type == opType
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSel) MohasebPrimaryBlue else Color(0xFFE2E8F0))
                  .clickable { opType = type }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Text(
                  text = type,
                  color = if (isSel) Color.White else Color.Black,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = selectedWh,
            onValueChange = { selectedWh = it },
            label = { Text("من مخزن") },
            modifier = Modifier.fillMaxWidth()
          )

          if (opType.contains("تحويل")) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = targetWh,
              onValueChange = { targetWh = it },
              label = { Text("إلى مخزن") },
              modifier = Modifier.fillMaxWidth()
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = partyInput,
            onValueChange = { partyInput = it },
            label = { Text("الطرف المعني / الحساب") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = itemsCountInput,
              onValueChange = { itemsCountInput = it },
              label = { Text("عدد المواد") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = totalCostInput,
              onValueChange = { totalCostInput = it },
              label = { Text("إجمالي التكلفة") },
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = notesInput,
            onValueChange = { notesInput = it },
            label = { Text("ملاحظات وبيان") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val count = itemsCountInput.toIntOrNull() ?: 1
            val cost = totalCostInput.toDoubleOrNull() ?: 0.0
            viewModel.addWarehouseOperation(
              opType = opType,
              warehouse = selectedWh,
              toWarehouse = if (opType.contains("تحويل")) targetWh else "",
              party = partyInput,
              notes = notesInput,
              totalItems = count,
              totalCost = cost
            ) {
              showNewOpDialog = false
            }
          },
          modifier = Modifier.testTag("submit_wh_op_button")
        ) {
          Text("حفظ العملية", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewOpDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Operation Details & Delete
  selectedOpForDetails?.let { op ->
    AlertDialog(
      onDismissRequest = { selectedOpForDetails = null },
      title = { Text("${op.operationType} #${op.operationNumber}", fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("المخزن: ${op.warehouseName}", fontSize = 13.5.sp)
          if (op.targetWarehouseName.isNotBlank()) Text("إلى مخزن: ${op.targetWarehouseName}", fontSize = 13.5.sp)
          if (op.partyOrAccount.isNotBlank()) Text("الطرف المعني: ${op.partyOrAccount}", fontSize = 13.sp)
          Text("التاريخ: ${op.date}", fontSize = 12.5.sp, color = Color.Gray)
          Text("عدد الأصناف: ${op.totalItems} • إجمالي التكلفة: %,.0f ر.ي".format(op.totalCost), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
          if (op.notes.isNotBlank()) Text("ملاحظات: ${op.notes}", fontSize = 12.sp, color = Color.Gray)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            opToDelete = op
            selectedOpForDetails = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("حذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedOpForDetails = null }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: Confirm Delete Operation
  opToDelete?.let { op ->
    AlertDialog(
      onDismissRequest = { opToDelete = null },
      title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
      text = { Text("هل تريد حذف العملية المخزنية #${op.operationNumber} (${op.operationType}) من قاعدة البيانات؟") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteWarehouseOperation(op) {
              opToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("نعم، احذف")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { opToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}
