package com.example.ui.navigation

sealed class Screen(val route: String, val titleAr: String) {
  object Dashboard : Screen("dashboard", "محاسب سوفت")
  object SalesList : Screen("sales_list", "قائمة المبيعات")
  object PurchasesList : Screen("purchases_list", "قائمة المشتريات")
  object CreateInvoice : Screen("create_invoice/{type}", "فاتورة جديدة") {
    fun createRoute(type: String) = "create_invoice/$type"
  }
  object VouchersList : Screen("vouchers_list", "قبض / صرف")
  object CreateVoucher : Screen("create_voucher/{type}", "سند جديد") {
    fun createRoute(type: String) = "create_voucher/$type"
  }
  object AccountsList : Screen("accounts_list", "دليل الحسابات")
  object AddAccount : Screen("add_account", "إضافة حساب")
  object ItemsList : Screen("items_list", "الأصناف والمخزون")
  object AddItem : Screen("add_item", "إضافة صنف جديد")
  object WarehouseOps : Screen("warehouse_ops", "العمليات المخزنية")
  object Currencies : Screen("currencies", "العملات وأسعار الصرف")
  object Reports : Screen("reports", "التقارير المالية")
  object Settings : Screen("settings", "الإعدادات العامة")
  object Notifications : Screen("notifications", "التنبيهات")
}
