package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountEntity
import com.example.data.model.AppSettingEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountingViewModel(private val repository: AccountingRepository) : ViewModel() {

  // Current Warehouse
  private val _activeWarehouse = MutableStateFlow("المخزن الرئيسي")
  val activeWarehouse: StateFlow<String> = _activeWarehouse.asStateFlow()

  fun setActiveWarehouse(name: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      runCatching { repository.saveSettings(mapOf("active_warehouse" to name)) }
        .onSuccess { _activeWarehouse.value = name }
        .onFailure { showMessage(it.message ?: "تعذر حفظ المخزن النشط") }
    }
  }

  // Active Currency
  private val _activeCurrency = MutableStateFlow("محلي")
  val activeCurrency: StateFlow<String> = _activeCurrency.asStateFlow()

  fun setActiveCurrency(curr: String) {
    if (curr.isBlank()) return
    viewModelScope.launch {
      runCatching { repository.saveSettings(mapOf("active_currency" to curr)) }
        .onSuccess { _activeCurrency.value = curr }
        .onFailure { showMessage(it.message ?: "تعذر حفظ العملة النشطة") }
    }
  }

  init {
    viewModelScope.launch {
      repository.allSettings.collect { settings ->
        settings.firstOrNull { it.settingKey == "active_warehouse" }
          ?.settingValue?.takeIf { it.isNotBlank() }?.let { _activeWarehouse.value = it }
        settings.firstOrNull { it.settingKey == "active_currency" }
          ?.settingValue?.takeIf { it.isNotBlank() }?.let { _activeCurrency.value = it }
      }
    }
  }

  // Data streams from Room
  val allInvoices: StateFlow<List<InvoiceEntity>> = repository.allInvoices.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allItems: StateFlow<List<ItemEntity>> = repository.allItems.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allVouchers: StateFlow<List<VoucherEntity>> = repository.allVouchers.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allWarehouses: StateFlow<List<WarehouseEntity>> = repository.allWarehouses.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allCurrencies: StateFlow<List<CurrencyEntity>> = repository.allCurrencies.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allSettings: StateFlow<List<AppSettingEntity>> = repository.allSettings.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allOperations: StateFlow<List<WarehouseOperationEntity>> = repository.allOperations.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun saveSettings(settings: Map<String, String>, onSuccess: () -> Unit = {}) {
    if (settings.keys.any { it.isBlank() }) {
      showMessage("تعذر حفظ إعداد غير صالح")
      return
    }
    viewModelScope.launch {
      runCatching { repository.saveSettings(settings) }
        .onSuccess {
          showMessage("تم حفظ الإعدادات")
          onSuccess()
        }
        .onFailure { error ->
          showMessage(error.message ?: "تعذر حفظ الإعدادات")
        }
    }
  }

  // Message / Notification Toast banner
  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  fun showMessage(msg: String) {
    _userMessage.value = msg
  }

  fun clearMessage() {
    _userMessage.value = null
  }

  private fun performDbAction(
    successMessage: String,
    onSuccess: () -> Unit = {},
    action: suspend () -> Unit
  ) {
    viewModelScope.launch {
      runCatching { action() }
        .onSuccess {
          showMessage(successMessage)
          onSuccess()
        }
        .onFailure { error ->
          showMessage(error.message ?: "تعذر تنفيذ العملية في قاعدة البيانات")
        }
    }
  }

  // Current Date string helper
  val todayFormatted: String
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

  // Invoice Items query
  fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItemEntity>> {
    return repository.getInvoiceItems(invoiceId)
  }

  // Invoice Creation Actions
  fun saveInvoice(
    type: String,
    invoiceNumber: String,
    isCredit: Boolean,
    isReturn: Boolean,
    partyName: String,
    date: String,
    warehouse: String,
    currency: String,
    items: List<InvoiceItemEntity>,
    discount: Double,
    tax: Double,
    otherFees: Double,
    paidAmount: Double,
    notes: String,
    onSuccess: () -> Unit
  ) {
    if (type != "SALE" && type != "PURCHASE") {
      showMessage("نوع الفاتورة غير صحيح")
      return
    }
    if (items.isEmpty()) {
      showMessage("أضف صنفًا واحدًا على الأقل قبل حفظ الفاتورة")
      return
    }
    if (items.any { it.itemId <= 0L || !it.quantity.isFinite() || it.quantity <= 0.0 ||
        !it.unitPrice.isFinite() || it.unitPrice < 0.0 || !it.total.isFinite() || it.total < 0.0 }) {
      showMessage("تحقق من الأصناف والكميات والأسعار في الفاتورة")
      return
    }
    if (listOf(discount, tax, otherFees, paidAmount).any { !it.isFinite() || it < 0.0 }) {
      showMessage("لا يمكن إدخال مبالغ سالبة أو غير صالحة")
      return
    }
    val subtotal = items.sumOf { it.total }
    val netTotal = subtotal - discount + tax + otherFees
    if (!netTotal.isFinite() || netTotal < 0.0 || paidAmount > netTotal) {
      showMessage("راجع الخصم والضريبة والرسوم والمبلغ المدفوع")
      return
    }
    val remaining = netTotal - paidAmount

    val invoice = InvoiceEntity(
      invoiceNumber = invoiceNumber.ifBlank { System.currentTimeMillis().toString().takeLast(4) },
      type = type,
      isCredit = isCredit,
      isReturn = isReturn,
      date = date.ifBlank { todayFormatted },
      partyName = partyName.ifBlank { if (type == "SALE") "عميل نقدي" else "مورد عام" },
      warehouseName = warehouse,
      currency = currency,
      subtotal = subtotal,
      discount = discount,
      tax = tax,
      otherFees = otherFees,
      paidAmount = paidAmount,
      remainingAmount = remaining,
      notes = notes
    )

    viewModelScope.launch {
      runCatching { repository.createInvoice(invoice, items) }
        .onSuccess {
          val typeLabel = if (type == "SALE") "فاتورة بيع" else "فاتورة شراء"
          showMessage("تم حفظ $typeLabel بنجاح وتحديث أرصدة المخزون")
          onSuccess()
        }
        .onFailure { error ->
          showMessage(error.message ?: "تعذر حفظ الفاتورة؛ لم يتم تأكيد نجاح العملية")
        }
    }
  }

  fun deleteInvoice(invoice: InvoiceEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف الفاتورة رقم ${invoice.invoiceNumber} وإرجاع الكميات للمخزن", onSuccess) {
      repository.deleteInvoice(invoice)
    }

  // Voucher Creation & Deletion
  fun saveVoucher(
    type: String,
    accountName: String,
    amount: Double,
    currency: String,
    statement: String,
    notes: String,
    onSuccess: () -> Unit
  ) {
    if (!amount.isFinite() || amount <= 0.0) {
      showMessage("يجب أن تكون قيمة السند أكبر من صفر")
      return
    }
    val voucher = VoucherEntity(
      voucherNumber = (System.currentTimeMillis() % 10000).toString(),
      type = type,
      accountName = accountName.ifBlank { "الصندوق الرئيسي" },
      amount = amount,
      currency = currency,
      date = todayFormatted,
      statement = statement,
      notes = notes
    )
    viewModelScope.launch {
      runCatching { repository.insertVoucher(voucher) }
        .onSuccess {
          val typeAr = if (type == "RECEIPT") "سند قبض" else "سند صرف"
          showMessage("تم حفظ $typeAr بمبلغ $amount بنجاح وتحديث الرصيد")
          onSuccess()
        }
        .onFailure { error ->
          showMessage(error.message ?: "تعذر حفظ السند")
        }
    }
  }

  fun updateVoucher(voucher: VoucherEntity, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      runCatching { repository.updateVoucher(voucher) }
        .onSuccess {
          showMessage("تم تحديث بيانات السند رقم ${voucher.voucherNumber}")
          onSuccess()
        }
        .onFailure { error ->
          showMessage(error.message ?: "تعذر تحديث السند")
        }
    }
  }

  fun deleteVoucher(voucher: VoucherEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف السند رقم ${voucher.voucherNumber} بنجاح", onSuccess) {
      repository.deleteVoucher(voucher)
    }

  // Accounts CRUD
  fun addAccount(
    name: String,
    category: String,
    phone: String,
    address: String,
    taxNumber: String,
    notifyWhatsApp: Boolean,
    notifySMS: Boolean,
    onSuccess: () -> Unit
  ) {
    if (name.isBlank() || category.isBlank()) {
      showMessage("اسم الحساب والتصنيف مطلوبان")
      return
    }
    viewModelScope.launch {
      repository.insertAccount(
        AccountEntity(
          name = name,
          parentCategory = category,
          phone = phone,
          address = address,
          taxNumber = taxNumber,
          notifyWhatsApp = notifyWhatsApp,
          notifySMS = notifySMS
        )
      )
      showMessage("تم إضافة الحساب: $name بنجاح")
      onSuccess()
    }
  }

  fun updateAccount(account: AccountEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم تحديث بيانات الحساب: ${account.name}", onSuccess) {
      repository.updateAccount(account)
    }

  fun deleteAccount(account: AccountEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف الحساب: ${account.name}", onSuccess) {
      repository.deleteAccount(account)
    }

  // Items CRUD
  fun addItem(
    code: String,
    name: String,
    category: String,
    unit: String,
    costPrice: Double,
    sellingPrice: Double,
    openingQty: Double,
    onSuccess: () -> Unit
  ) {
    if (name.isBlank() || category.isBlank() || unit.isBlank()) {
      showMessage("اسم الصنف والتصنيف والوحدة حقول مطلوبة")
      return
    }
    if (!costPrice.isFinite() || costPrice < 0.0 ||
        !sellingPrice.isFinite() || sellingPrice < 0.0 ||
        !openingQty.isFinite() || openingQty < 0.0) {
      showMessage("تحقق من أسعار الصنف والكمية الافتتاحية")
      return
    }
    viewModelScope.launch {
      repository.insertItem(
        ItemEntity(
          code = code.ifBlank { "ITM-" + (System.currentTimeMillis() % 1000) },
          name = name,
          category = category,
          unit = unit,
          costPrice = costPrice,
          sellingPrice = sellingPrice,
          openingQuantity = openingQty,
          currentStock = openingQty
        )
      )
      showMessage("تم إضافة الصنف: $name بنجاح")
      onSuccess()
    }
  }

  fun updateItem(item: ItemEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم تحديث بيانات الصنف: ${item.name}", onSuccess) {
      repository.updateItem(item)
    }

  fun deleteItem(item: ItemEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف الصنف: ${item.name}", onSuccess) {
      repository.deleteItem(item)
    }

  // Warehouses CRUD
  fun addWarehouse(name: String, phone: String, address: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      repository.insertWarehouse(WarehouseEntity(name = name, phone = phone, address = address))
      showMessage("تم إضافة المخزن: $name بنجاح")
      onSuccess()
    }
  }

  fun updateWarehouse(warehouse: WarehouseEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم تحديث بيانات المخزن: ${warehouse.name}", onSuccess) {
      repository.updateWarehouse(warehouse)
    }

  fun deleteWarehouse(warehouse: WarehouseEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف المخزن: ${warehouse.name}", onSuccess) {
      repository.deleteWarehouse(warehouse)
    }

  // Currencies CRUD
  fun addCurrency(name: String, symbol: String, subUnit: String, rate: Double, onSuccess: () -> Unit) {
    viewModelScope.launch {
      repository.insertCurrency(
        CurrencyEntity(name = name, symbol = symbol, subUnit = subUnit, exchangeRate = rate)
      )
      showMessage("تم إضافة العملة: $name بنجاح")
      onSuccess()
    }
  }

  fun updateCurrency(currency: CurrencyEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم تحديث سعر صرف عملة: ${currency.name}", onSuccess) {
      repository.updateCurrency(currency)
    }

  fun deleteCurrency(currency: CurrencyEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف عملة: ${currency.name}", onSuccess) {
      repository.deleteCurrency(currency)
    }

  // Warehouse Operations CRUD
  fun addWarehouseOperation(
    opType: String,
    warehouse: String,
    toWarehouse: String,
    party: String,
    notes: String,
    totalItems: Int,
    totalCost: Double,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      repository.insertOperation(
        WarehouseOperationEntity(
          operationNumber = (System.currentTimeMillis() % 1000).toString(),
          operationType = opType,
          warehouseName = warehouse,
          targetWarehouseName = toWarehouse,
          date = todayFormatted,
          partyOrAccount = party,
          totalItems = totalItems,
          totalCost = totalCost,
          notes = notes
        )
      )
      showMessage("تم تسجيل عملية $opType بنجاح")
      onSuccess()
    }
  }

  fun updateWarehouseOperation(op: WarehouseOperationEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم تحديث العملية المخزنية #${op.operationNumber}", onSuccess) {
      repository.updateOperation(op)
    }

  fun deleteWarehouseOperation(op: WarehouseOperationEntity, onSuccess: () -> Unit = {}) =
    performDbAction("تم حذف العملية المخزنية #${op.operationNumber}", onSuccess) {
      repository.deleteOperation(op)
    }
}

class AccountingViewModelFactory(private val repository: AccountingRepository) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AccountingViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return AccountingViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
