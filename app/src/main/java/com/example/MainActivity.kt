package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
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
import com.example.data.local.MohasebDatabase
import com.example.data.repository.AccountingRepository
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CreateInvoiceScreen
import com.example.ui.screens.CurrenciesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesListScreen
import com.example.ui.screens.ItemsScreen
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
  data class CreateInvoice(val type: String) : NavDestination()
  object VouchersList : NavDestination()
  object AccountsList : NavDestination()
  object ItemsList : NavDestination()
  data class WarehouseOps(val op: String) : NavDestination()
  object Currencies : NavDestination()
  data class Reports(val report: String) : NavDestination()
  object Settings : NavDestination()
  object Notifications : NavDestination()
}

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: AccountingViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room DB & Repository
    val database = MohasebDatabase.getDatabase(applicationContext, lifecycleScope)
    val repository = AccountingRepository(database.accountingDao(), database)
    val factory = AccountingViewModelFactory(repository)
    viewModel = ViewModelProvider(this, factory)[AccountingViewModel::class.java]

    setContent {
      MohasebSoftTheme {
        // Enforce RTL layout direction for natural Arabic experience
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          MohasebAppNavigation(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun MohasebAppNavigation(viewModel: AccountingViewModel) {
  val backStack = remember { mutableStateListOf<NavDestination>(NavDestination.Dashboard) }
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
          navigateTo(NavDestination.VouchersList)
        },
        onNavigateToAccounts = { navigateTo(NavDestination.AccountsList) },
        onNavigateToAddAccount = { navigateTo(NavDestination.AccountsList) },
        onNavigateToItems = { navigateTo(NavDestination.ItemsList) },
        onNavigateToAddItem = { navigateTo(NavDestination.ItemsList) },
        onNavigateToWarehouseOps = { op -> navigateTo(NavDestination.WarehouseOps(op)) },
        onNavigateToCurrencies = { navigateTo(NavDestination.Currencies) },
        onNavigateToReports = { r -> navigateTo(NavDestination.Reports(r)) },
        onNavigateToSettings = { navigateTo(NavDestination.Settings) },
        onNavigateToNotifications = { navigateTo(NavDestination.Notifications) }
      )
    }

    is NavDestination.SalesList -> {
      InvoicesListScreen(
        invoiceType = "SALE",
        viewModel = viewModel,
        onBack = { navigateBack() },
        onAddNew = { navigateTo(NavDestination.CreateInvoice("SALE")) }
      )
    }

    is NavDestination.PurchasesList -> {
      InvoicesListScreen(
        invoiceType = "PURCHASE",
        viewModel = viewModel,
        onBack = { navigateBack() },
        onAddNew = { navigateTo(NavDestination.CreateInvoice("PURCHASE")) }
      )
    }

    is NavDestination.CreateInvoice -> {
      CreateInvoiceScreen(
        invoiceType = screen.type,
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
    }
  }
}
