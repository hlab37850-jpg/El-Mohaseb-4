package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import com.example.ui.util.PinLockScreen
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.data.local.BackupManager
import com.example.data.local.MohasebDatabase
import com.example.data.repository.AccountingRepository
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CreateInvoiceScreen
import com.example.ui.screens.CurrenciesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesListScreen
import com.example.ui.screens.ItemsScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VouchersScreen
import com.example.ui.screens.WarehouseOperationsScreen
import com.example.ui.theme.MohasebSoftTheme
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.AccountingViewModelFactory

sealed class NavDestination {
  object Dashboard : NavDestination()
  object SalesList : NavDestination()
  object PurchasesList : NavDestination()
  data class CreateInvoice(val type: String, val editId: Long? = null) : NavDestination()
  object VouchersList : NavDestination()
  object AccountsList : NavDestination()
  object ItemsList : NavDestination()
  data class WarehouseOps(val op: String) : NavDestination()
  object Currencies : NavDestination()
  data class Reports(val report: String) : NavDestination()
  object Settings : NavDestination()
  object Notifications : NavDestination()
  data class Journal(val type: String) : NavDestination()
  data class DocumentList(val type: String) : NavDestination()
}

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: AccountingViewModel
  private lateinit var database: MohasebDatabase

  private val backupLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
      if (uri != null) {
        lifecycleScope.launch {
          runCatching { BackupManager.backupTo(applicationContext, database, uri) }
            .onSuccess { viewModel.showMessage("تم حفظ النسخة الاحتياطية بنجاح") }
            .onFailure { viewModel.showMessage(it.message ?: "تعذر حفظ النسخة الاحتياطية") }
        }
      }
    }

  private val restoreLauncher =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        lifecycleScope.launch {
          runCatching { BackupManager.restoreFrom(applicationContext, uri) }
            .onSuccess { restartApp() }
            .onFailure { viewModel.showMessage(it.message ?: "تعذر استرجاع النسخة الاحتياطية") }
        }
      }
    }

  private fun restartApp() {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    finishAffinity()
    if (launchIntent?.component != null) {
      startActivity(Intent.makeRestartActivityTask(launchIntent.component))
    }
    android.os.Process.killProcess(android.os.Process.myPid())
  }

  private fun startBackup() {
    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd_HHmm", java.util.Locale.ENGLISH).format(java.util.Date())
    backupLauncher.launch("mohaseb_backup_$stamp.db")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room DB & Repository
    database = MohasebDatabase.getDatabase(applicationContext, lifecycleScope)
    val repository = AccountingRepository(database.accountingDao(), database)
    val factory = AccountingViewModelFactory(repository)
    viewModel = ViewModelProvider(this, factory)[AccountingViewModel::class.java]

    setContent {
      MohasebSoftTheme {
        // Enforce RTL layout direction for natural Arabic experience
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          MohasebAppNavigation(
            viewModel = viewModel,
            onBackupRequested = { startBackup() },
            onRestoreRequested = { restoreLauncher.launch(arrayOf("*/*")) },
            onExitRequested = { finishAffinity() }
          )
        }
      }
    }
  }
}

@Composable
fun MohasebAppNavigation(
  viewModel: AccountingViewModel,
  onBackupRequested: () -> Unit = {},
  onRestoreRequested: () -> Unit = {},
  onExitRequested: () -> Unit = {}
) {
  val startupPin by viewModel.startupPinHash.collectAsState()
  var unlocked by remember { mutableStateOf(false) }
  val pin = startupPin
  when {
    pin == null -> Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7FB)))
    pin.isNotBlank() && !unlocked -> PinLockScreen(expectedHash = pin, onUnlocked = { unlocked = true })
    else -> MohasebAppContent(viewModel, onBackupRequested, onRestoreRequested, onExitRequested)
  }
}

@Composable
fun MohasebAppContent(
  viewModel: AccountingViewModel,
  onBackupRequested: () -> Unit = {},
  onRestoreRequested: () -> Unit = {},
  onExitRequested: () -> Unit = {}
) {
  val backStack = remember { mutableStateListOf<NavDestination>(NavDestination.Dashboard) }
  val globalSnackbar = remember { SnackbarHostState() }
  val globalMessage by viewModel.userMessage.collectAsState()
  // Every screen reports success / errors through the ViewModel; show them in one place.
  LaunchedEffect(globalMessage) {
    globalMessage?.let {
      globalSnackbar.showSnackbar(it)
      viewModel.clearMessage()
    }
  }
  val currentScreen = backStack.lastOrNull() ?: NavDestination.Dashboard

  // Proper back navigation
  BackHandler(enabled = backStack.size > 1) {
    backStack.removeAt(backStack.size - 1)
  }

  fun navigateTo(dest: NavDestination) {
    backStack.add(dest)
  }

  fun navigateBack() {
    if (backStack.size > 1) {
      backStack.removeAt(backStack.size - 1)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFF4F7FB)),
    contentAlignment = Alignment.TopCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
    ) {
      when (val screen = currentScreen) {
    is NavDestination.Dashboard -> {
      DashboardScreen(
        viewModel = viewModel,
        onNavigateToSales = { navigateTo(NavDestination.SalesList) },
        onNavigateToNewSale = { navigateTo(NavDestination.CreateInvoice("SALE")) },
        onNavigateToPurchases = { navigateTo(NavDestination.PurchasesList) },
        onNavigateToNewPurchase = { navigateTo(NavDestination.CreateInvoice("PURCHASE")) },
        onNavigateToVouchers = { navigateTo(NavDestination.VouchersList) },
        onNavigateToNewVoucher = { op ->
          if (op == "قيد يومي" || op == "قيد إفتتاحي") {
            navigateTo(NavDestination.Journal(op))
          } else {
            navigateTo(NavDestination.VouchersList)
          }
        },
        onNavigateToAccounts = { navigateTo(NavDestination.AccountsList) },
        onNavigateToAddAccount = { navigateTo(NavDestination.AccountsList) },
        onNavigateToItems = { navigateTo(NavDestination.ItemsList) },
        onNavigateToAddItem = { navigateTo(NavDestination.ItemsList) },
        onNavigateToWarehouseOps = { op -> navigateTo(NavDestination.WarehouseOps(op)) },
        onNavigateToCurrencies = { navigateTo(NavDestination.Currencies) },
        onNavigateToReports = { r -> navigateTo(NavDestination.Reports(r)) },
        onNavigateToSettings = { navigateTo(NavDestination.Settings) },
        onNavigateToNotifications = { navigateTo(NavDestination.Notifications) },
        onNavigateToDocuments = { t -> navigateTo(NavDestination.DocumentList(t)) },
        onBackupRequested = onBackupRequested,
        onRestoreRequested = onRestoreRequested,
        onExitRequested = onExitRequested
      )
    }

    is NavDestination.SalesList -> {
      InvoicesListScreen(
        invoiceType = "SALE",
        viewModel = viewModel,
        onBack = { navigateBack() },
        onAddNew = { navigateTo(NavDestination.CreateInvoice("SALE")) },
        onEdit = { id -> navigateTo(NavDestination.CreateInvoice("SALE", id)) }
      )
    }

    is NavDestination.PurchasesList -> {
      InvoicesListScreen(
        invoiceType = "PURCHASE",
        viewModel = viewModel,
        onBack = { navigateBack() },
        onAddNew = { navigateTo(NavDestination.CreateInvoice("PURCHASE")) },
        onEdit = { id -> navigateTo(NavDestination.CreateInvoice("PURCHASE", id)) }
      )
    }

    is NavDestination.CreateInvoice -> {
      CreateInvoiceScreen(
        invoiceType = screen.type,
        editInvoiceId = screen.editId,
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.VouchersList -> {
      VouchersScreen(
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.AccountsList -> {
      AccountsScreen(
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.ItemsList -> {
      ItemsScreen(
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.WarehouseOps -> {
      WarehouseOperationsScreen(
        initialOpType = screen.op,
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.Currencies -> {
      CurrenciesScreen(
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.Reports -> {
      ReportsScreen(
        initialReport = screen.report,
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.Settings -> {
      SettingsScreen(
        viewModel = viewModel,
        onBack = { navigateBack() },
        onBackupRequested = onBackupRequested,
        onRestoreRequested = onRestoreRequested
      )
    }

    is NavDestination.DocumentList -> {
      InvoicesListScreen(
        invoiceType = screen.type,
        viewModel = viewModel,
        onBack = { navigateBack() },
        onAddNew = { navigateTo(NavDestination.CreateInvoice(screen.type)) },
        onEdit = { id -> navigateTo(NavDestination.CreateInvoice(screen.type, id)) }
      )
    }

    is NavDestination.Journal -> {
      JournalScreen(
        initialType = screen.type,
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }

    is NavDestination.Notifications -> {
      NotificationsScreen(
        viewModel = viewModel,
        onBack = { navigateBack() }
      )
    }
      }
      SnackbarHost(globalSnackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
  }
}
