package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val parentCategory: String = "عام", // أصول، التزامات، مصاريف، إيرادات، الصناديق، العملاء، الموردون
  val balance: Double = 0.0,
  val currency: String = "محلي",
  val phone: String = "",
  val address: String = "",
  val taxNumber: String = "",
  val notifyWhatsApp: Boolean = false,
  val notifySMS: Boolean = false
)

@Entity(tableName = "items")
data class ItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val code: String,
  val name: String,
  val category: String = "عام",
  val unit: String = "حبة",
  val costPrice: Double,
  val sellingPrice: Double,
  val openingQuantity: Double = 0.0,
  val currentStock: Double = 0.0,
  val expiryDate: String = ""
)

@Entity(tableName = "warehouses")
data class WarehouseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val phone: String = "",
  val address: String = ""
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val invoiceNumber: String,
  val type: String, // "SALE" or "PURCHASE"
  val isCredit: Boolean = true, // true = آجل, false = نقدي
  val isReturn: Boolean = false, // مرتجع
  val date: String,
  val partyName: String, // العميل أو المورد
  val warehouseName: String = "المخزن الرئيسي",
  val currency: String = "محلي",
  val subtotal: Double = 0.0,
  val discount: Double = 0.0,
  val tax: Double = 0.0,
  val otherFees: Double = 0.0,
  val paidAmount: Double = 0.0,
  val remainingAmount: Double = 0.0,
  val notes: String = ""
)

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val invoiceId: Long,
  val itemId: Long,
  val itemName: String,
  val quantity: Double,
  val unitPrice: Double,
  val total: Double
)

@Entity(tableName = "vouchers")
data class VoucherEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val voucherNumber: String,
  val type: String, // "RECEIPT" (قبض) or "PAYMENT" (صرف)
  val accountName: String,
  val amount: Double,
  val currency: String = "محلي",
  val date: String,
  val statement: String = "",
  val notes: String = ""
)

@Entity(tableName = "currencies")
data class CurrencyEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val symbol: String,
  val subUnit: String,
  val exchangeRate: Double = 1.0,
  val accountCount: Int = 0
)

@Entity(tableName = "warehouse_operations")
data class WarehouseOperationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val operationNumber: String,
  val operationType: String, // "صرف مخزني", "توريد مخزني", "تحويل مخزني", "تسوية مخزنية", "جرد مخزني"
  val warehouseName: String,
  val targetWarehouseName: String = "",
  val date: String,
  val partyOrAccount: String = "",
  val totalItems: Int = 1,
  val totalCost: Double = 0.0,
  val notes: String = ""
)


@Entity(tableName = "app_settings")
data class AppSettingEntity(
  @PrimaryKey val settingKey: String,
  val settingValue: String
)
