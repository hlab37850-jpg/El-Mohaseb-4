package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountingDao
import com.example.data.local.MohasebDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import kotlinx.coroutines.flow.Flow

class AccountingRepository(
  private val dao: AccountingDao,
  private val database: MohasebDatabase? = null
) {
  // Accounts
  val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
  fun getAccountsByCategory(cat: String): Flow<List<AccountEntity>> = dao.getAccountsByCategory(cat)
  suspend fun insertAccount(account: AccountEntity): Long = dao.insertAccount(account)
  suspend fun updateAccount(account: AccountEntity) = dao.updateAccount(account)
  suspend fun deleteAccount(account: AccountEntity) = dao.deleteAccount(account)

  // Items
  val allItems: Flow<List<ItemEntity>> = dao.getAllItems()
  fun searchItems(query: String): Flow<List<ItemEntity>> = dao.searchItems(query)
  suspend fun insertItem(item: ItemEntity): Long = dao.insertItem(item)
  suspend fun updateItem(item: ItemEntity) = dao.updateItem(item)
  suspend fun deleteItem(item: ItemEntity) = dao.deleteItem(item)

  // Warehouses
  val allWarehouses: Flow<List<WarehouseEntity>> = dao.getAllWarehouses()
  suspend fun insertWarehouse(warehouse: WarehouseEntity): Long = dao.insertWarehouse(warehouse)
  suspend fun updateWarehouse(warehouse: WarehouseEntity) = dao.updateWarehouse(warehouse)
  suspend fun deleteWarehouse(warehouse: WarehouseEntity) = dao.deleteWarehouse(warehouse)

  // Invoices
  val allInvoices: Flow<List<InvoiceEntity>> = dao.getAllInvoices()
  fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>> = dao.getInvoicesByType(type)
  fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItemEntity>> = dao.getInvoiceItems(invoiceId)
  suspend fun getInvoiceItemsSync(invoiceId: Long): List<InvoiceItemEntity> = dao.getInvoiceItemsSync(invoiceId)

  suspend fun createInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Long {
    validateInvoice(invoice, items)
    return if (database != null) {
      database.withTransaction { createInvoiceInTransaction(invoice, items) }
    } else {
      createInvoiceInTransaction(invoice, items)
    }
  }

  private suspend fun createInvoiceInTransaction(
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>
  ): Long {
    val invoiceId = dao.insertInvoice(invoice)
    dao.insertInvoiceItems(items.map { it.copy(invoiceId = invoiceId) })

    items.forEach { itemRow ->
      val qtyDelta = when {
        invoice.type == "SALE" && !invoice.isReturn -> -itemRow.quantity
        invoice.type == "SALE" && invoice.isReturn -> itemRow.quantity
        invoice.type == "PURCHASE" && !invoice.isReturn -> itemRow.quantity
        invoice.type == "PURCHASE" && invoice.isReturn -> -itemRow.quantity
        else -> 0.0
      }
      if (qtyDelta != 0.0) dao.adjustItemStock(itemRow.itemId, qtyDelta)
    }

    if (invoice.isCredit && invoice.partyName.isNotBlank()) {
      val balanceDelta = if (invoice.isReturn) -invoice.remainingAmount else invoice.remainingAmount
      dao.adjustAccountBalance(invoice.partyName, balanceDelta)
    }
    return invoiceId
  }

  private suspend fun validateInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
    require(invoice.type == "SALE" || invoice.type == "PURCHASE") { "نوع الفاتورة غير صحيح" }
    require(items.isNotEmpty()) { "لا يمكن حفظ فاتورة بدون أصناف" }
    require(items.all {
      it.itemId > 0L && it.quantity.isFinite() && it.quantity > 0.0 &&
        it.unitPrice.isFinite() && it.unitPrice >= 0.0 &&
        it.total.isFinite() && it.total >= 0.0 &&
        kotlin.math.abs(it.total - it.quantity * it.unitPrice) <= 0.01
    }) { "بيانات الأصناف أو الكميات أو الأسعار غير صحيحة" }
    val itemRecords = items.map { row ->
      dao.getItemById(row.itemId) ?: throw IllegalArgumentException("أحد أصناف الفاتورة غير موجود في قاعدة البيانات")
    }
    if (invoice.type == "SALE" && !invoice.isReturn) {
      items.forEachIndexed { index, row ->
        require(itemRecords[index].currentStock >= row.quantity) {
          "الكمية المتاحة غير كافية للصنف: ${itemRecords[index].name}"
        }
      }
    }
    if (invoice.isCredit) {
      require(invoice.partyName.isNotBlank() && dao.getAccountByName(invoice.partyName) != null) {
        "الحساب المرتبط بالفاتورة غير موجود في قاعدة البيانات"
      }
    }
    require(listOf(invoice.subtotal, invoice.discount, invoice.tax, invoice.otherFees,
      invoice.paidAmount, invoice.remainingAmount).all { it.isFinite() && it >= 0.0 }) {
      "توجد مبالغ غير صالحة في الفاتورة"
    }
    val netTotal = invoice.subtotal - invoice.discount + invoice.tax + invoice.otherFees
    require(netTotal >= 0.0 && kotlin.math.abs(invoice.remainingAmount + invoice.paidAmount - netTotal) <= 0.01) {
      "إجمالي الفاتورة والمدفوع والمتبقي غير متطابق"
    }
    require(kotlin.math.abs(items.sumOf { it.total } - invoice.subtotal) <= 0.01) {
      "مجموع تفاصيل الأصناف لا يطابق إجمالي الفاتورة"
    }
  }

  suspend fun deleteInvoice(invoice: InvoiceEntity) {
    if (database != null) {
      database.withTransaction { deleteInvoiceInTransaction(invoice) }
    } else {
      deleteInvoiceInTransaction(invoice)
    }
  }

  private suspend fun deleteInvoiceInTransaction(invoice: InvoiceEntity) {
    val items = dao.getInvoiceItemsSync(invoice.id)
    items.forEach { itemRow ->
      val reverseQty = when {
        invoice.type == "SALE" && !invoice.isReturn -> itemRow.quantity
        invoice.type == "SALE" && invoice.isReturn -> -itemRow.quantity
        invoice.type == "PURCHASE" && !invoice.isReturn -> -itemRow.quantity
        invoice.type == "PURCHASE" && invoice.isReturn -> itemRow.quantity
        else -> 0.0
      }
      if (reverseQty != 0.0) dao.adjustItemStock(itemRow.itemId, reverseQty)
    }

    if (invoice.isCredit && invoice.partyName.isNotBlank()) {
      val reverseBalance = if (invoice.isReturn) invoice.remainingAmount else -invoice.remainingAmount
      dao.adjustAccountBalance(invoice.partyName, reverseBalance)
    }
    dao.deleteInvoiceItems(invoice.id)
    dao.deleteInvoice(invoice)
  }

  // Vouchers
  val allVouchers: Flow<List<VoucherEntity>> = dao.getAllVouchers()
  fun getVouchersByType(type: String): Flow<List<VoucherEntity>> = dao.getVouchersByType(type)

  suspend fun insertVoucher(voucher: VoucherEntity): Long {
    require(voucher.amount > 0.0 && voucher.amount.isFinite()) {
      "قيمة السند يجب أن تكون أكبر من صفر"
    }
    val operation: suspend () -> Long = {
      val id = dao.insertVoucher(voucher)
      if (voucher.accountName.isNotBlank()) dao.adjustAccountBalance(voucher.accountName, -voucher.amount)
      id
    }
    return if (database != null) database.withTransaction { operation() } else operation()
  }

  suspend fun updateVoucher(voucher: VoucherEntity) {
    require(voucher.amount > 0.0 && voucher.amount.isFinite()) {
      "قيمة السند يجب أن تكون أكبر من صفر"
    }
    val operation: suspend () -> Unit = {
      val previous = dao.getVoucherById(voucher.id)
        ?: throw IllegalArgumentException("السند المراد تعديله غير موجود")
      if (previous.accountName.isNotBlank()) {
        dao.adjustAccountBalance(previous.accountName, previous.amount)
      }
      dao.updateVoucher(voucher)
      if (voucher.accountName.isNotBlank()) {
        dao.adjustAccountBalance(voucher.accountName, -voucher.amount)
      }
    }
    if (database != null) database.withTransaction { operation() } else operation()
  }

  suspend fun deleteVoucher(voucher: VoucherEntity) {
    val operation: suspend () -> Unit = {
      if (voucher.accountName.isNotBlank()) {
        dao.adjustAccountBalance(voucher.accountName, voucher.amount)
      }
      dao.deleteVoucher(voucher)
    }
    if (database != null) database.withTransaction { operation() } else operation()
  }

  // Currencies
  val allCurrencies: Flow<List<CurrencyEntity>> = dao.getAllCurrencies()
  suspend fun insertCurrency(currency: CurrencyEntity): Long = dao.insertCurrency(currency)
  suspend fun updateCurrency(currency: CurrencyEntity) = dao.updateCurrency(currency)
  suspend fun deleteCurrency(currency: CurrencyEntity) = dao.deleteCurrency(currency)

  // Warehouse Operations
  val allOperations: Flow<List<WarehouseOperationEntity>> = dao.getAllWarehouseOperations()
  suspend fun insertOperation(op: WarehouseOperationEntity): Long = dao.insertWarehouseOperation(op)
  suspend fun updateOperation(op: WarehouseOperationEntity) = dao.updateWarehouseOperation(op)
  suspend fun deleteOperation(op: WarehouseOperationEntity) = dao.deleteWarehouseOperation(op)
}
