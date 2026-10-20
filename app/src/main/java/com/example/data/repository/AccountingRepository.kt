package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountingDao
import com.example.data.local.MohasebDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AppSettingEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.JournalEntryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.WarehouseOperationLineEntity
import com.example.data.model.WarehouseStockEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AccountingRepository(
  private val dao: AccountingDao,
  private val database: MohasebDatabase? = null
) {
  private suspend fun ensureRow(itemId: Long, warehouseName: String) {
    dao.insertStockRowIgnore(WarehouseStockEntity(itemId, warehouseName, 0.0))
  }

  /** The main warehouse is the one created first (lowest id). */
  private suspend fun mainWarehouse(): com.example.data.model.WarehouseEntity? =
    dao.getAllWarehouses().first().minByOrNull { it.id }

  /** Quotations and purchase orders are documents only: they never touch stock or balances. */
  private fun isPosting(type: String): Boolean = type == "SALE" || type == "PURCHASE"

  /** Rejects postings dated inside a period that was closed by the annual closing. */
  private suspend fun assertPeriodOpen(date: String) {
    val closedUntil = dao.getSetting("closed_until")?.settingValue.orEmpty()
    require(closedUntil.isBlank() || date.isBlank() || date > closedUntil) {
      "السنة المالية مقفلة حتى $closedUntil؛ لا يمكن إضافة عمليات بتاريخ ضمنها"
    }
  }

  // Accounts
  val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
  fun getAccountsByCategory(cat: String): Flow<List<AccountEntity>> = dao.getAccountsByCategory(cat)
  suspend fun insertAccount(account: AccountEntity): Long {
    require(account.name.isNotBlank() && account.parentCategory.isNotBlank()) { "اسم الحساب والتصنيف مطلوبان" }
    require(account.balance.isFinite()) { "رصيد الحساب غير صالح" }
    require(dao.getAccountByName(account.name) == null) { "يوجد حساب بالاسم نفسه" }
    return dao.insertAccount(account)
  }
  suspend fun updateAccount(account: AccountEntity) {
    require(account.name.isNotBlank() && account.parentCategory.isNotBlank()) { "اسم الحساب والتصنيف مطلوبان" }
    require(account.balance.isFinite()) { "رصيد الحساب غير صالح" }
    val previous = dao.getAccountById(account.id)
      ?: throw IllegalArgumentException("الحساب المراد تعديله غير موجود")
    val update: suspend () -> Unit = {
      if (previous.name != account.name) {
        require(dao.getAccountByName(account.name) == null) { "يوجد حساب آخر بالاسم نفسه" }
        dao.renameInvoiceParty(previous.name, account.name)
        dao.renameVoucherAccount(previous.name, account.name)
      }
      dao.updateAccount(account)
    }
    if (database != null) database.withTransaction { update() } else update()
  }
  suspend fun deleteAccount(account: AccountEntity) {
    require(account.balance == 0.0) { "لا يمكن حذف حساب له رصيد؛ صفّر الرصيد أو سوّه أولًا" }
    require(dao.countInvoicesForParty(account.name) == 0 && dao.countVouchersForAccount(account.name) == 0) {
      "لا يمكن حذف الحساب لأنه مرتبط بفواتير أو سندات محفوظة"
    }
    dao.deleteAccount(account)
  }

  // Items
  val allItems: Flow<List<ItemEntity>> = dao.getAllItems()
  fun searchItems(query: String): Flow<List<ItemEntity>> = dao.searchItems(query)
  suspend fun insertItem(item: ItemEntity): Long {
    validateItem(item)
    val id = dao.insertItem(item)
    if (item.currentStock > 0.0) {
      val main = mainWarehouse()
      if (main != null) {
        ensureRow(id, main.name)
        dao.addStockDelta(id, main.name, item.currentStock)
      }
    }
    return id
  }
  suspend fun updateItem(item: ItemEntity) {
    validateItem(item)
    val previous = dao.getItemById(item.id)
    dao.updateItem(item)
    // A manual change of the stock figure is applied to the main warehouse so totals stay consistent.
    if (previous != null && kotlin.math.abs(item.currentStock - previous.currentStock) > 0.0001) {
      val main = mainWarehouse()
      if (main != null) {
        ensureRow(item.id, main.name)
        dao.addStockDelta(item.id, main.name, item.currentStock - previous.currentStock)
      }
    }
  }

  private fun validateItem(item: ItemEntity) {
    require(item.name.isNotBlank() && item.code.isNotBlank() &&
      item.category.isNotBlank() && item.unit.isNotBlank()) { "اسم الصنف ورمزه وتصنيفه ووحدته مطلوبة" }
    require(listOf(item.costPrice, item.sellingPrice, item.openingQuantity, item.currentStock)
      .all { it.isFinite() && it >= 0.0 }) { "أسعار الصنف وكمياته يجب أن تكون أرقامًا غير سالبة" }
  }
  suspend fun deleteItem(item: ItemEntity) {
    require(dao.countInvoiceLinesForItem(item.id) == 0) {
      "لا يمكن حذف الصنف لأنه مستخدم في فواتير محفوظة"
    }
    dao.deleteStockForItem(item.id)
    dao.deleteItem(item)
  }

  // Warehouses
  val allWarehouses: Flow<List<WarehouseEntity>> = dao.getAllWarehouses()
  suspend fun insertWarehouse(warehouse: WarehouseEntity): Long {
    require(warehouse.name.isNotBlank()) { "اسم المخزن مطلوب" }
    require(dao.getWarehouseByName(warehouse.name) == null) { "يوجد مخزن بالاسم نفسه" }
    return dao.insertWarehouse(warehouse)
  }
  suspend fun updateWarehouse(warehouse: WarehouseEntity) {
    require(warehouse.name.isNotBlank()) { "اسم المخزن مطلوب" }
    val previous = dao.getWarehouseById(warehouse.id)
      ?: throw IllegalArgumentException("المخزن المراد تعديله غير موجود")
    val update: suspend () -> Unit = {
      if (previous.name != warehouse.name) {
        require(dao.getWarehouseByName(warehouse.name) == null) { "يوجد مخزن آخر بالاسم نفسه" }
        dao.renameInvoiceWarehouse(previous.name, warehouse.name)
        dao.renameOperationWarehouse(previous.name, warehouse.name)
        dao.renameOperationTargetWarehouse(previous.name, warehouse.name)
        dao.renameActiveWarehouseSetting(previous.name, warehouse.name)
        dao.renameStockWarehouse(previous.name, warehouse.name)
      }
      dao.updateWarehouse(warehouse)
    }
    if (database != null) database.withTransaction { update() } else update()
  }
  suspend fun deleteWarehouse(warehouse: WarehouseEntity) {
    require(dao.getAllWarehouses().first().size > 1) { "يجب الاحتفاظ بمخزن واحد على الأقل" }
    require(dao.countInvoicesForWarehouse(warehouse.name) == 0 && dao.countOperationsForWarehouse(warehouse.name) == 0) {
      "لا يمكن حذف المخزن لأنه مرتبط بفواتير أو عمليات مخزنية"
    }
    require(dao.countNonZeroStock(warehouse.name) == 0) { "لا يمكن حذف المخزن لأنه يحتوي على كميات أصناف" }
    dao.deleteStockForWarehouse(warehouse.name)
    dao.deleteWarehouse(warehouse)
  }

  // Invoices
  val allInvoices: Flow<List<InvoiceEntity>> = dao.getAllInvoices()
  fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>> = dao.getInvoicesByType(type)
  fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItemEntity>> = dao.getInvoiceItems(invoiceId)
  suspend fun getInvoiceItemsSync(invoiceId: Long): List<InvoiceItemEntity> = dao.getInvoiceItemsSync(invoiceId)

  suspend fun createInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Long {
    return if (database != null) {
      database.withTransaction {
        validateInvoice(invoice, items)
        createInvoiceInTransaction(invoice, items)
      }
    } else {
      validateInvoice(invoice, items)
      createInvoiceInTransaction(invoice, items)
    }
  }

  private suspend fun createInvoiceInTransaction(
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>
  ): Long {
    val invoiceId = dao.insertInvoice(invoice)
    dao.insertInvoiceItems(items.map { it.copy(invoiceId = invoiceId) })
    if (!isPosting(invoice.type)) return invoiceId

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
        ensureRow(itemRow.itemId, invoice.warehouseName)
        dao.addStockDelta(itemRow.itemId, invoice.warehouseName, qtyDelta)
      }
    }

    if (invoice.isCredit && invoice.partyName.isNotBlank()) {
      val balanceDelta = if (invoice.isReturn) -invoice.remainingAmount else invoice.remainingAmount
      dao.adjustAccountBalance(invoice.partyName, balanceDelta)
    }
    return invoiceId
  }

  private suspend fun validateInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
    assertPeriodOpen(invoice.date)
    require(invoice.type in listOf("SALE", "PURCHASE", "QUOTE", "PURCHASE_ORDER")) { "نوع الفاتورة غير صحيح" }
    require(dao.getWarehouseByName(invoice.warehouseName) != null) {
      "المخزن المحدد غير موجود في قاعدة البيانات"
    }
    require(dao.getCurrencyByName(invoice.currency) != null) {
      "العملة المحددة غير موجودة في قاعدة البيانات"
    }
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
      items.groupBy { it.itemId }.forEach { (itemId, rows) ->
        val item = itemRecords.first { it.id == itemId }
        val requestedQuantity = rows.sumOf { it.quantity }
        require(item.currentStock >= requestedQuantity) {
          "الكمية المتاحة غير كافية للصنف: ${item.name}"
        }
        if (dao.countStockRows(itemId) > 0) {
          val inWarehouse = dao.getWarehouseQty(itemId, invoice.warehouseName) ?: 0.0
          require(inWarehouse >= requestedQuantity) {
            "الكمية المتاحة في المخزن ${invoice.warehouseName} غير كافية للصنف: ${item.name}"
          }
        }
      }
    }
    if (invoice.isCredit && isPosting(invoice.type)) {
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
      database.withTransaction {
        if (isPosting(invoice.type)) assertPeriodOpen(invoice.date)
        deleteInvoiceInTransaction(invoice)
      }
    } else {
      if (isPosting(invoice.type)) assertPeriodOpen(invoice.date)
      deleteInvoiceInTransaction(invoice)
    }
  }

  /** Edits a saved invoice: the old posting is reversed, then the new version is validated and posted. */
  suspend fun updateInvoice(old: InvoiceEntity, updated: InvoiceEntity, items: List<InvoiceItemEntity>) {
    val action: suspend () -> Unit = {
      if (isPosting(old.type)) assertPeriodOpen(old.date)
      deleteInvoiceInTransaction(old)
      val replacement = updated.copy(id = old.id, type = old.type)
      validateInvoice(replacement, items)
      createInvoiceInTransaction(replacement, items)
    }
    if (database != null) database.withTransaction { action() } else action()
  }

  private suspend fun deleteInvoiceInTransaction(invoice: InvoiceEntity) {
    if (!isPosting(invoice.type)) {
      dao.deleteInvoiceItems(invoice.id)
      dao.deleteInvoice(invoice)
      return
    }
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
        ensureRow(itemRow.itemId, invoice.warehouseName)
        dao.addStockDelta(itemRow.itemId, invoice.warehouseName, reverseQty)
      }
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
    assertPeriodOpen(voucher.date)
    require(voucher.amount > 0.0 && voucher.amount.isFinite()) {
      "قيمة السند يجب أن تكون أكبر من صفر"
    }
    require(voucher.accountName.isNotBlank() && dao.getAccountByName(voucher.accountName) != null) {
      "الحساب المحدد للسند غير موجود في قاعدة البيانات"
    }
    require(dao.getCurrencyByName(voucher.currency) != null) {
      "العملة المحددة للسند غير موجودة في قاعدة البيانات"
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
    require(voucher.accountName.isNotBlank() && dao.getAccountByName(voucher.accountName) != null) {
      "الحساب المحدد للسند غير موجود في قاعدة البيانات"
    }
    require(dao.getCurrencyByName(voucher.currency) != null) {
      "العملة المحددة للسند غير موجودة في قاعدة البيانات"
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
  suspend fun insertCurrency(currency: CurrencyEntity): Long {
    validateCurrency(currency)
    require(dao.getCurrencyByName(currency.name) == null) { "يوجد عملة بالاسم نفسه" }
    return dao.insertCurrency(currency)
  }
  suspend fun updateCurrency(currency: CurrencyEntity) {
    validateCurrency(currency)
    val previous = dao.getCurrencyById(currency.id)
      ?: throw IllegalArgumentException("العملة المراد تعديلها غير موجودة")
    val update: suspend () -> Unit = {
      if (previous.name != currency.name) {
        require(dao.getCurrencyByName(currency.name) == null) { "يوجد عملة أخرى بالاسم نفسه" }
        dao.renameInvoiceCurrency(previous.name, currency.name)
        dao.renameVoucherCurrency(previous.name, currency.name)
        dao.renameActiveCurrencySetting(previous.name, currency.name)
      }
      dao.updateCurrency(currency)
    }
    if (database != null) database.withTransaction { update() } else update()
  }

  private fun validateCurrency(currency: CurrencyEntity) {
    require(currency.name.isNotBlank() && currency.symbol.isNotBlank()) { "اسم العملة ورمزها مطلوبان" }
    require(currency.exchangeRate.isFinite() && currency.exchangeRate > 0.0) { "سعر الصرف يجب أن يكون رقمًا أكبر من صفر" }
  }
  suspend fun deleteCurrency(currency: CurrencyEntity) {
    require(dao.getAllCurrencies().first().size > 1) { "يجب الاحتفاظ بعملة واحدة على الأقل" }
    require(dao.countInvoicesForCurrency(currency.name) == 0 && dao.countVouchersForCurrency(currency.name) == 0) {
      "لا يمكن حذف العملة لأنها مستخدمة في فواتير أو سندات محفوظة"
    }
    dao.deleteCurrency(currency)
  }

  // Persisted application settings
  val allSettings: Flow<List<AppSettingEntity>> = dao.getAllSettings()

  suspend fun saveSettings(settings: Map<String, String>) {
    require(settings.keys.all { it.isNotBlank() }) { "مفتاح الإعداد غير صالح" }
    val rows = settings.map { (key, value) -> AppSettingEntity(key, value) }
    if (database != null) {
      database.withTransaction { dao.upsertSettings(rows) }
    } else {
      dao.upsertSettings(rows)
    }
  }

  // Warehouse Operations
  val allOperations: Flow<List<WarehouseOperationEntity>> = dao.getAllWarehouseOperations()
  suspend fun insertOperation(op: WarehouseOperationEntity): Long {
    validateWarehouseOperation(op)
    return dao.insertWarehouseOperation(op)
  }
  suspend fun updateOperation(op: WarehouseOperationEntity) {
    validateWarehouseOperation(op)
    dao.updateWarehouseOperation(op)
  }

  private suspend fun validateWarehouseOperation(op: WarehouseOperationEntity) {
    require(op.operationType.isNotBlank() && op.warehouseName.isNotBlank()) { "نوع العملية والمخزن مطلوبان" }
    require(dao.getWarehouseByName(op.warehouseName) != null) { "المخزن المحدد غير موجود في قاعدة البيانات" }
    if (op.targetWarehouseName.isNotBlank()) {
      require(dao.getWarehouseByName(op.targetWarehouseName) != null) { "المخزن الهدف غير موجود في قاعدة البيانات" }
    }
    require(op.totalItems >= 0 && op.totalCost.isFinite() && op.totalCost >= 0.0) { "كمية العملية أو تكلفتها غير صالحة" }
  }
  val allWarehouseStock: Flow<List<WarehouseStockEntity>> = dao.getAllWarehouseStock()
  val allOperationLines: Flow<List<WarehouseOperationLineEntity>> = dao.getAllOperationLines()
  suspend fun getOperationLines(operationId: Long): List<WarehouseOperationLineEntity> =
    dao.getOperationLines(operationId)

  /**
   * Records a stock operation and applies it to per-warehouse stock and to each item's total.
   * Quantity meaning per type: توريد/صرف/تحويل = positive amount; تسوية = signed change; جرد = counted quantity.
   */
  suspend fun createStockOperation(op: WarehouseOperationEntity, requests: List<OperationLineRequest>): Long {
    require(requests.isNotEmpty()) { "أضف صنفًا واحدًا على الأقل للعملية" }
    val action: suspend () -> Long = {
      assertPeriodOpen(op.date)
      val type = op.operationType
      val isTransfer = type.contains("تحويل")
      if (isTransfer) {
        require(op.targetWarehouseName.isNotBlank() && op.targetWarehouseName != op.warehouseName) {
          "حدد مخزنًا هدفًا مختلفًا عن مخزن المصدر"
        }
      }
      val lines = mutableListOf<WarehouseOperationLineEntity>()
      for (request in requests) {
        val item = dao.getItemById(request.itemId)
          ?: throw IllegalArgumentException("أحد أصناف العملية غير موجود")
        require(request.quantity.isFinite()) { "كمية غير صالحة للصنف ${item.name}" }
        val inSource = dao.getWarehouseQty(item.id, op.warehouseName) ?: 0.0
        val sourceDelta: Double
        var targetDelta = 0.0
        when {
          type.contains("توريد") -> {
            require(request.quantity > 0.0) { "كمية التوريد يجب أن تكون أكبر من صفر" }
            sourceDelta = request.quantity
          }
          type.contains("صرف") -> {
            require(request.quantity > 0.0) { "كمية الصرف يجب أن تكون أكبر من صفر" }
            sourceDelta = -request.quantity
          }
          isTransfer -> {
            require(request.quantity > 0.0) { "كمية التحويل يجب أن تكون أكبر من صفر" }
            sourceDelta = -request.quantity
            targetDelta = request.quantity
          }
          type.contains("تسوية") -> {
            require(request.quantity != 0.0) { "كمية التسوية لا يمكن أن تكون صفرًا" }
            sourceDelta = request.quantity
          }
          type.contains("جرد") -> {
            require(request.quantity >= 0.0) { "الكمية المجرودة لا يمكن أن تكون سالبة" }
            sourceDelta = request.quantity - inSource
          }
          else -> throw IllegalArgumentException("نوع العملية المخزنية غير معروف")
        }
        require(inSource + sourceDelta >= -0.0001) {
          "الكمية المتاحة في ${op.warehouseName} غير كافية للصنف: ${item.name}"
        }
        lines.add(
          WarehouseOperationLineEntity(
            operationId = 0, itemId = item.id, itemName = item.name, quantity = request.quantity,
            sourceDelta = sourceDelta, targetDelta = targetDelta, unitCost = item.costPrice
          )
        )
      }
      val totalCost = lines.sumOf { kotlin.math.abs(it.sourceDelta) * it.unitCost }
      val saved = op.copy(totalItems = lines.size, totalCost = totalCost)
      validateWarehouseOperation(saved)
      val opId = dao.insertWarehouseOperation(saved)
      lines.forEach { line ->
        ensureRow(line.itemId, op.warehouseName)
        dao.addStockDelta(line.itemId, op.warehouseName, line.sourceDelta)
        if (line.targetDelta != 0.0) {
          ensureRow(line.itemId, op.targetWarehouseName)
          dao.addStockDelta(line.itemId, op.targetWarehouseName, line.targetDelta)
        }
        val totalChange = line.sourceDelta + line.targetDelta
        if (totalChange != 0.0) dao.adjustItemStock(line.itemId, totalChange)
      }
      dao.insertOperationLines(lines.map { it.copy(operationId = opId) })
      opId
    }
    return if (database != null) database.withTransaction { action() } else action()
  }

  suspend fun deleteOperation(op: WarehouseOperationEntity) {
    val action: suspend () -> Unit = {
      val lines = dao.getOperationLines(op.id)
      if (lines.isNotEmpty()) assertPeriodOpen(op.date)
      lines.forEach { line ->
        ensureRow(line.itemId, op.warehouseName)
        dao.addStockDelta(line.itemId, op.warehouseName, -line.sourceDelta)
        if (line.targetDelta != 0.0) {
          ensureRow(line.itemId, op.targetWarehouseName)
          dao.addStockDelta(line.itemId, op.targetWarehouseName, -line.targetDelta)
        }
        val totalChange = line.sourceDelta + line.targetDelta
        if (totalChange != 0.0) dao.adjustItemStock(line.itemId, -totalChange)
        val sourceNow = dao.getWarehouseQty(line.itemId, op.warehouseName) ?: 0.0
        require(sourceNow >= -0.0001) { "لا يمكن حذف العملية لأن الكميات استُهلكت: ${line.itemName}" }
        if (line.targetDelta != 0.0) {
          val targetNow = dao.getWarehouseQty(line.itemId, op.targetWarehouseName) ?: 0.0
          require(targetNow >= -0.0001) { "لا يمكن حذف العملية لأن الكميات استُهلكت: ${line.itemName}" }
        }
      }
      dao.deleteOperationLines(op.id)
      dao.deleteWarehouseOperation(op)
    }
    if (database != null) database.withTransaction { action() } else action()
  }

  val allInvoiceItems: Flow<List<InvoiceItemEntity>> = dao.getAllInvoiceItems()

  // Journal entries (قيد يومي / قيد افتتاحي)
  val allJournalEntries: Flow<List<JournalEntryEntity>> = dao.getAllJournalEntries()

  /** Accounts whose natural balance is on the credit side: a credit entry increases them. */
  private fun isCreditNature(category: String): Boolean =
    category == "الموردون" || category == "الإيرادات" || category == "الالتزامات"

  private suspend fun applyJournalEffect(entry: JournalEntryEntity, sign: Double) {
    val debit = dao.getAccountByName(entry.debitAccount)
      ?: throw IllegalArgumentException("حساب المدين غير موجود")
    val credit = dao.getAccountByName(entry.creditAccount)
      ?: throw IllegalArgumentException("حساب الدائن غير موجود")
    val debitDelta = if (isCreditNature(debit.parentCategory)) -entry.amount else entry.amount
    val creditDelta = if (isCreditNature(credit.parentCategory)) entry.amount else -entry.amount
    dao.adjustAccountBalance(debit.name, debitDelta * sign)
    dao.adjustAccountBalance(credit.name, creditDelta * sign)
  }

  suspend fun insertJournalEntry(entry: JournalEntryEntity): Long {
    assertPeriodOpen(entry.date)
    require(entry.entryType == "DAILY" || entry.entryType == "OPENING") { "نوع القيد غير صحيح" }
    require(entry.amount.isFinite() && entry.amount > 0.0) { "مبلغ القيد يجب أن يكون أكبر من صفر" }
    require(entry.debitAccount.isNotBlank() && entry.creditAccount.isNotBlank()) { "حدد حساب المدين وحساب الدائن" }
    require(entry.debitAccount != entry.creditAccount) { "لا يمكن أن يكون المدين والدائن الحساب نفسه" }
    require(dao.getCurrencyByName(entry.currency) != null) { "العملة المحددة غير موجودة في قاعدة البيانات" }
    val operation: suspend () -> Long = {
      applyJournalEffect(entry, 1.0)
      dao.insertJournalEntry(entry)
    }
    return if (database != null) database.withTransaction { operation() } else operation()
  }

  suspend fun deleteJournalEntry(entry: JournalEntryEntity) {
    val operation: suspend () -> Unit = {
      applyJournalEffect(entry, -1.0)
      dao.deleteJournalEntry(entry)
    }
    if (database != null) database.withTransaction { operation() } else operation()
  }

  // Reminders
  val allReminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
  suspend fun insertReminder(reminder: ReminderEntity): Long {
    require(reminder.title.isNotBlank()) { "عنوان التنبيه مطلوب" }
    return dao.insertReminder(reminder)
  }
  suspend fun updateReminder(reminder: ReminderEntity) {
    require(reminder.title.isNotBlank()) { "عنوان التنبيه مطلوب" }
    dao.updateReminder(reminder)
  }
  suspend fun deleteReminder(reminder: ReminderEntity) = dao.deleteReminder(reminder)
}

/** One requested line of a stock operation (see [AccountingRepository.createStockOperation]). */
data class OperationLineRequest(val itemId: Long, val quantity: Double)
