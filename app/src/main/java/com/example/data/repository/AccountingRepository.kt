package com.example.data.repository

import com.example.data.local.AccountingDao
import com.example.data.model.AccountEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import kotlinx.coroutines.flow.Flow

class AccountingRepository(private val dao: AccountingDao) {
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
    val invoiceId = dao.insertInvoice(invoice)
    val populatedItems = items.map { it.copy(invoiceId = invoiceId) }
    dao.insertInvoiceItems(populatedItems)

    // Adjust item stocks based on transaction type
    items.forEach { itemRow ->
      val qtyDelta = when {
        invoice.type == "SALE" && !invoice.isReturn -> -itemRow.quantity
        invoice.type == "SALE" && invoice.isReturn -> itemRow.quantity
        invoice.type == "PURCHASE" && !invoice.isReturn -> itemRow.quantity
        invoice.type == "PURCHASE" && invoice.isReturn -> -itemRow.quantity
        else -> 0.0
      }
      if (qtyDelta != 0.0) {
        dao.adjustItemStock(itemRow.itemId, qtyDelta)
      }
    }

    // Adjust Account Balance if credit
    if (invoice.isCredit && invoice.partyName.isNotBlank()) {
      val netBalanceImpact = if (invoice.type == "SALE") {
        if (invoice.isReturn) -invoice.remainingAmount else invoice.remainingAmount
      } else {
        if (invoice.isReturn) -invoice.remainingAmount else invoice.remainingAmount
      }
      dao.adjustAccountBalance(invoice.partyName, netBalanceImpact)
    }

    return invoiceId
  }

  suspend fun deleteInvoice(invoice: InvoiceEntity) {
    // 1. Fetch line items to reverse inventory
    val items = dao.getInvoiceItemsSync(invoice.id)
    items.forEach { itemRow ->
      val reverseQty = when {
        invoice.type == "SALE" && !invoice.isReturn -> itemRow.quantity
        invoice.type == "SALE" && invoice.isReturn -> -itemRow.quantity
        invoice.type == "PURCHASE" && !invoice.isReturn -> -itemRow.quantity
        invoice.type == "PURCHASE" && invoice.isReturn -> itemRow.quantity
        else -> 0.0
      }
      if (reverseQty != 0.0) {
        dao.adjustItemStock(itemRow.itemId, reverseQty)
      }
    }

    // 2. Reverse account balance if credit
    if (invoice.isCredit && invoice.partyName.isNotBlank()) {
      val reverseBalance = if (invoice.type == "SALE") {
        if (invoice.isReturn) invoice.remainingAmount else -invoice.remainingAmount
      } else {
        if (invoice.isReturn) invoice.remainingAmount else -invoice.remainingAmount
      }
      dao.adjustAccountBalance(invoice.partyName, reverseBalance)
    }

    // 3. Delete invoice items and invoice
    dao.deleteInvoiceItems(invoice.id)
    dao.deleteInvoice(invoice)
  }

  // Vouchers
  val allVouchers: Flow<List<VoucherEntity>> = dao.getAllVouchers()
  fun getVouchersByType(type: String): Flow<List<VoucherEntity>> = dao.getVouchersByType(type)

  suspend fun insertVoucher(voucher: VoucherEntity): Long {
    val id = dao.insertVoucher(voucher)
    // Adjust account balance:
    // If RECEIPT (قبض): decreases debt of customer (balance - amount)
    // If PAYMENT (صرف): decreases debt to vendor (balance - amount)
    if (voucher.accountName.isNotBlank()) {
      val delta = if (voucher.type == "RECEIPT") -voucher.amount else -voucher.amount
      dao.adjustAccountBalance(voucher.accountName, delta)
    }
    return id
  }

  suspend fun updateVoucher(voucher: VoucherEntity) {
    require(voucher.amount > 0.0 && voucher.amount.isFinite()) {
      "قيمة السند يجب أن تكون أكبر من صفر"
    }
    val previous = dao.getVoucherById(voucher.id)
      ?: throw IllegalArgumentException("السند المراد تعديله غير موجود")

    // Reverse the old balance effect before applying the edited voucher effect.
    // This also handles changing the linked account or amount.
    if (previous.accountName.isNotBlank()) {
      dao.adjustAccountBalance(previous.accountName, previous.amount)
    }
    dao.updateVoucher(voucher)
    if (voucher.accountName.isNotBlank()) {
      dao.adjustAccountBalance(voucher.accountName, -voucher.amount)
    }
  }

  suspend fun deleteVoucher(voucher: VoucherEntity) {
    // Reverse account balance adjustment
    if (voucher.accountName.isNotBlank()) {
      val delta = voucher.amount
      dao.adjustAccountBalance(voucher.accountName, delta)
    }
    dao.deleteVoucher(voucher)
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
