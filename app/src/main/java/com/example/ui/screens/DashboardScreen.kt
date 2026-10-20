package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DashboardAccordions
import com.example.ui.components.DashboardBottomBar
import com.example.ui.components.DashboardGrid
import com.example.ui.components.MohasebDrawerContent
import com.example.ui.components.MohasebTopBar
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
  viewModel: AccountingViewModel,
  onNavigateToSales: () -> Unit,
  onNavigateToNewSale: () -> Unit,
  onNavigateToPurchases: () -> Unit,
  onNavigateToNewPurchase: () -> Unit,
  onNavigateToVouchers: () -> Unit,
  onNavigateToNewVoucher: (String) -> Unit,
  onNavigateToAccounts: () -> Unit,
  onNavigateToAddAccount: () -> Unit,
  onNavigateToItems: () -> Unit,
  onNavigateToAddItem: () -> Unit,
  onNavigateToWarehouseOps: (String) -> Unit,
  onNavigateToCurrencies: () -> Unit,
  onNavigateToReports: (String) -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToNotifications: () -> Unit,
  onNavigateToDocuments: (String) -> Unit = {},
  onBackupRequested: () -> Unit = {},
  onRestoreRequested: () -> Unit = {},
  onExitRequested: () -> Unit = {}
) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val activeWarehouse by viewModel.activeWarehouse.collectAsState()
  val userMessage by viewModel.userMessage.collectAsState()

  // Dialog states
  var showAddWarehouseDialog by remember { mutableStateOf(false) }
  var showYearEndClosingDialog by remember { mutableStateOf(false) }
  var showSwitchWarehouseDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }
  var showRestoreConfirmDialog by remember { mutableStateOf(false) }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      MohasebDrawerContent(
        onAccountsClick = {
          scope.launch { drawerState.close() }
          onNavigateToAccounts()
        },
        onSettingsClick = {
          scope.launch { drawerState.close() }
          onNavigateToSettings()
        },
        onBackupClick = {
          scope.launch { drawerState.close() }
          onBackupRequested()
        },
        onRestoreClick = {
          scope.launch { drawerState.close() }
          showRestoreConfirmDialog = true
        },
        onDriveClick = {
          scope.launch { drawerState.close() }
          viewModel.showMessage("اختر Google Drive من قائمة المواقع في نافذة الحفظ")
          onBackupRequested()
        },
        onSupportClick = {
          scope.launch { drawerState.close() }
          viewModel.showMessage("خدمة العملاء: support@mohasebsoft.com")
        },
        onAboutClick = {
          scope.launch { drawerState.close() }
          showAboutDialog = true
        },
        onExitClick = {
          scope.launch { drawerState.close() }
          onExitRequested()
        }
      )
    }
  ) {
    Scaffold(
      topBar = {
        MohasebTopBar(
          title = "محاسب سوفت",
          onMenuClick = { scope.launch { drawerState.open() } },
          onNotificationClick = onNavigateToNotifications,
          onShareClick = { viewModel.showMessage("رابط التطبيق: mohaseb-soft.apk") }
        )
      },
      bottomBar = {
        DashboardBottomBar(
          currentWarehouse = activeWarehouse,
          onWarehouseClick = { showSwitchWarehouseDialog = true },
          onYouTubeClick = { viewModel.showMessage("فتح الشروحات التعليمية على يوتيوب") },
          onRefreshClick = {
            viewModel.showMessage("تم تحديث البيانات وقاعدة البيانات المحلية")
          }
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      containerColor = Color(0xFFF4F7FB)
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.TopCenter
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Quick Action Grid (Sales, Purchases, Vouchers, Accounts)
          DashboardGrid(
          onSalesListClick = onNavigateToSales,
          onNewSaleClick = onNavigateToNewSale,
          onPurchasesListClick = onNavigateToPurchases,
          onNewPurchaseClick = onNavigateToNewPurchase,
          onVouchersClick = onNavigateToVouchers,
          onAccountsClick = onNavigateToAccounts
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 5 Expandable Accordion sections matching video
        DashboardAccordions(
          onWarehouseOpClick = { op ->
            if (op == "إضافة مخزن") {
              showAddWarehouseDialog = true
            } else {
              onNavigateToWarehouseOps(op)
            }
          },
          onAccountsOpClick = { op ->
            when (op) {
              "قيد يومي", "قيد إفتتاحي" -> onNavigateToNewVoucher(op)
              "إضافة حساب" -> onNavigateToAddAccount()
              "حركة الصندوق" -> onNavigateToReports("حركة الصندوق")
              "دليل الحسابات" -> onNavigateToAccounts()
              "إقفال سنوي" -> showYearEndClosingDialog = true
              else -> onNavigateToAccounts()
            }
          },
          onItemsOpClick = { op ->
            when (op) {
              "الأصناف", "أسعار البيع", "وحدات الصنف" -> onNavigateToItems()
              "فاتورة عرض سعر" -> onNavigateToDocuments("QUOTE")
              "طلب شراء" -> onNavigateToDocuments("PURCHASE_ORDER")
              else -> onNavigateToItems()
            }
          },
          onCurrenciesOpClick = { op ->
            onNavigateToCurrencies()
          },
          onReportsOpClick = { op ->
            onNavigateToReports(op)
          }
        )

        Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }

  // Dialog: Add Warehouse (from video 01:40)
  if (showAddWarehouseDialog) {
    var warehouseName by remember { mutableStateOf("") }
    var warehousePhone by remember { mutableStateOf("") }
    var warehouseAddress by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddWarehouseDialog = false },
      title = { Text("إضافة فرع / مخزن", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = warehouseName,
            onValueChange = { warehouseName = it },
            label = { Text("اسم المخزن") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_warehouse_name")
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = warehousePhone,
            onValueChange = { warehousePhone = it },
            label = { Text("رقم التلفون") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = warehouseAddress,
            onValueChange = { warehouseAddress = it },
            label = { Text("العنوان") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (warehouseName.isNotBlank()) {
              viewModel.addWarehouse(warehouseName, warehousePhone, warehouseAddress) {
                showAddWarehouseDialog = false
              }
            }
          },
          modifier = Modifier.testTag("submit_add_warehouse")
        ) {
          Text("موافق", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddWarehouseDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Year-end Closing (from video 02:17)
  if (showYearEndClosingDialog) {
    // Closes the previous calendar year; balances already carry forward as running account balances.
    val closingDate = "${(viewModel.todayFormatted.take(4).toIntOrNull() ?: 2026) - 1}-12-31"
    AlertDialog(
      onDismissRequest = { showYearEndClosingDialog = false },
      title = { Text("إغلاق سنوي حتى $closingDate", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("• عليك بعمل نسخة احتياطية قبل الإقفال.", fontSize = 13.sp)
          Text("• تأكد من مطابقة الجرد اليدوي مع المخزون المتبقي.", fontSize = 13.sp)
          Text("• تأكد من التسويات المخزنية اللازمة قبل الإغلاق.", fontSize = 13.sp)
          Text("• لن يتم السماح بإضافة فاتورة أو قيد للسنة المقفلة بعد الإغلاق.", fontSize = 13.sp)
          Text("• أرصدة الحسابات والمخزون تبقى كما هي وتُرحَّل تلقائيًا للسنة الجديدة.", fontSize = 13.sp)
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.saveSettings(mapOf("closed_until" to closingDate)) {
              showYearEndClosingDialog = false
            }
          }
        ) {
          Text("تنفيذ الإقفال", color = Color.Red, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showYearEndClosingDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Dialog: Switch Warehouse
  if (showSwitchWarehouseDialog) {
    val warehouses by viewModel.allWarehouses.collectAsState()
    AlertDialog(
      onDismissRequest = { showSwitchWarehouseDialog = false },
      title = { Text("اختر المخزن النشط", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          warehouses.forEach { wh ->
            TextButton(
              onClick = {
                viewModel.setActiveWarehouse(wh.name)
                showSwitchWarehouseDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = wh.name,
                fontWeight = if (wh.name == activeWarehouse) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSwitchWarehouseDialog = false }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Dialog: confirm restore (replaces all current data)
  if (showRestoreConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showRestoreConfirmDialog = false },
      title = { Text("استرجاع نسخة احتياطية", fontWeight = FontWeight.Bold) },
      text = { Text("سيتم استبدال جميع البيانات الحالية ببيانات النسخة المختارة ثم يُعاد تشغيل التطبيق. هل تريد المتابعة؟") },
      confirmButton = {
        TextButton(onClick = {
          showRestoreConfirmDialog = false
          onRestoreRequested()
        }) { Text("اختيار النسخة", color = Color.Red, fontWeight = FontWeight.Bold) }
      },
      dismissButton = { TextButton(onClick = { showRestoreConfirmDialog = false }) { Text("إلغاء") } }
    )
  }

  // Dialog: About
  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      title = { Text("محاسب سوفت - الإصدار 1.0", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "نظام شامل ومتكامل لإدارة الحسابات، نقاط البيع، والمخازن، متوافق مع كافة الأجهزة ويعمل محلياً بأمان تام."
        )
      },
      confirmButton = {
        TextButton(onClick = { showAboutDialog = false }) {
          Text("حسناً")
        }
      }
    )
  }
}
