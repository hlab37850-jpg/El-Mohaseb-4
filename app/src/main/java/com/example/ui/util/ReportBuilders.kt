package com.example.ui.util

import com.example.data.model.AccountEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.JournalEntryEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseOperationEntity
import com.example.data.model.WarehouseStockEntity
import com.example.data.model.WarehouseEntity

/** A simple table that both the on-screen report and the PDF exporter render. */
data class ReportTable(
  val title: String,
  val headers: List<String>,
  val rows: List<List<String>>,
  val totals: List<String>? = null,
  val headerNote: String = ""
)

/** Snapshot of everything the reports need, so builders stay pure functions. */
data class ReportData(
  val invoices: List<InvoiceEntity>,
  val invoiceItems: List<InvoiceItemEntity>,
  val items: List<ItemEntity>,
  val accounts: List<AccountEntity>,
  val vouchers: List<VoucherEntity>,
  val journal: List<JournalEntryEntity>,
  val currencies: List<CurrencyEntity>,
  val today: String,
  val warehouses: List<WarehouseEntity> = emptyList(),
  val warehouseStock: List<WarehouseStockEntity> = emptyList()
)

private fun money(v: Double): String = "%,.2f".format(v)
private fun qty(v: Double): String = if (v == Math.rint(v)) "%,.0f".format(v) else "%,.2f".format(v)

private fun invoiceNet(i: InvoiceEntity): Double = i.subtotal - i.discount + i.tax + i.otherFees

/** Signed quantity delta of an invoice line on stock: positive = stock in, negative = stock out. */
private fun stockDelta(inv: InvoiceEntity, line: InvoiceItemEntity): Double = when {
  inv.type == "SALE" && !inv.isReturn -> -line.quantity
  inv.type == "SALE" && inv.isReturn -> line.quantity
  inv.type == "PURCHASE" && !inv.isReturn -> line.quantity
  else -> -line.quantity
}

private fun linesWithInvoice(d: ReportData): List<Pair<InvoiceEntity, InvoiceItemEntity>> {
  val byId = d.invoices.associateBy { it.id }
  return d.invoiceItems.mapNotNull { line -> byId[line.invoiceId]?.let { it to line } }
}

object ReportBuilders {

  val otherReportNames = listOf(
    "المخزون المتبقي",
    "المخزون حسب المخزن",
    "أرصدة الحسابات",
    "أرباح الأصناف",
    "العمليات اليومية",
    "القيود اليومية",
    "حركة الصندوق",
    "حركة الحسابات",
    "إجمالي التصنيفات",
    "المبيعات والمشتريات حسب العملة",
    "رأس المال العامل",
    "الربح على مستوى العميل",
    "المبيعات حسب الصنف",
    "المشتريات حسب الصنف",
    "تفاصيل حركة الأصناف"
  )

  fun itemMovements(d: ReportData): ReportTable {
    val lines = linesWithInvoice(d)
    val rows = d.items.map { item ->
      var incoming = 0.0
      var outgoing = 0.0
      lines.filter { it.second.itemId == item.id }.forEach { (inv, line) ->
        val delta = stockDelta(inv, line)
        if (delta >= 0) incoming += delta else outgoing += -delta
      }
      listOf(item.name, qty(item.openingQuantity), qty(incoming), qty(outgoing), qty(item.currentStock))
    }
    return ReportTable("حركة الأصناف", listOf("الصنف", "الافتتاحي", "وارد", "منصرف", "المتاح"), rows)
  }

  fun trialBalance(d: ReportData): ReportTable {
    val creditNature = setOf("الموردون", "الإيرادات", "الالتزامات")
    var totalDebit = 0.0
    var totalCredit = 0.0
    val rows = d.accounts.map { acc ->
      // Balances of credit-nature accounts are stored as positive magnitudes on the credit side.
      val onCreditSide = if (acc.parentCategory in creditNature) acc.balance >= 0 else acc.balance < 0
      val magnitude = Math.abs(acc.balance)
      val debit = if (onCreditSide) 0.0 else magnitude
      val credit = if (onCreditSide) magnitude else 0.0
      totalDebit += debit
      totalCredit += credit
      listOf(acc.name, money(debit), money(credit))
    }
    return ReportTable("ميزان المراجعة", listOf("الحساب", "مدين", "دائن"), rows,
      listOf("الإجمالي", money(totalDebit), money(totalCredit)))
  }

  /** Sales − purchases view; cost of goods sold is computed from line quantity × item cost price. */
  fun incomeStatement(d: ReportData): ReportTable {
    val lines = linesWithInvoice(d)
    val costById = d.items.associate { it.id to it.costPrice }
    val sales = d.invoices.filter { it.type == "SALE" }.sumOf { if (it.isReturn) -invoiceNet(it) else invoiceNet(it) }
    val purchases = d.invoices.filter { it.type == "PURCHASE" }.sumOf { if (it.isReturn) -invoiceNet(it) else invoiceNet(it) }
    val cogs = lines.filter { it.first.type == "SALE" }.sumOf { (inv, line) ->
      val c = (costById[line.itemId] ?: 0.0) * line.quantity
      if (inv.isReturn) -c else c
    }
    val expenses = d.vouchers.filter { it.type == "PAYMENT" && d.accounts.firstOrNull { a -> a.name == it.accountName }?.parentCategory == "مصروفات" }
      .sumOf { it.amount }
    val grossProfit = sales - cogs
    val net = grossProfit - expenses
    return ReportTable(
      "قائمة الدخل",
      listOf("البند", "القيمة"),
      listOf(
        listOf("صافي المبيعات", money(sales)),
        listOf("تكلفة البضاعة المباعة (بسعر تكلفة الصنف الحالي)", money(cogs)),
        listOf("مجمل الربح", money(grossProfit)),
        listOf("المصروفات (سندات صرف لحسابات المصروفات)", money(expenses)),
        listOf("صافي الربح", money(net)),
        listOf("إجمالي المشتريات (للاطلاع)", money(purchases))
      )
    )
  }

  fun balanceSheet(d: ReportData): ReportTable {
    val stockValue = d.items.sumOf { it.currentStock * it.costPrice }
    val receivables = d.accounts.filter { it.parentCategory == "العملاء" && it.balance > 0 }.sumOf { it.balance }
    val cashAndBanks = d.accounts.filter { it.parentCategory == "الصناديق" || it.parentCategory == "البنوك" }.sumOf { it.balance }
    val payables = d.accounts.filter { it.parentCategory == "الموردون" && it.balance > 0 }.sumOf { it.balance }
    val assets = stockValue + receivables + cashAndBanks
    return ReportTable(
      "المركز المالي",
      listOf("البند", "القيمة"),
      listOf(
        listOf("قيمة المخزون (بسعر التكلفة)", money(stockValue)),
        listOf("ذمم العملاء المدينة", money(receivables)),
        listOf("الصناديق والبنوك", money(cashAndBanks)),
        listOf("إجمالي الأصول", money(assets)),
        listOf("ذمم الموردين الدائنة", money(payables)),
        listOf("صافي حقوق الملكية", money(assets - payables))
      )
    )
  }

  fun remainingStock(d: ReportData): ReportTable {
    var total = 0.0
    val rows = d.items.map {
      val v = it.currentStock * it.costPrice
      total += v
      listOf(it.name, qty(it.currentStock), money(it.costPrice), money(v))
    }
    return ReportTable("المخزون المتبقي", listOf("الصنف", "الكمية", "التكلفة", "القيمة"), rows,
      listOf("الإجمالي", "", "", money(total)))
  }

  fun accountBalances(d: ReportData): ReportTable =
    ReportTable("أرصدة الحسابات", listOf("الحساب", "التصنيف", "الرصيد"),
      d.accounts.map { listOf(it.name, it.parentCategory, money(it.balance)) })

  fun categoryTotals(d: ReportData): ReportTable {
    val rows = d.accounts.groupBy { it.parentCategory }.map { (cat, list) ->
      listOf(cat, list.size.toString(), money(list.sumOf { it.balance }))
    }
    return ReportTable("إجمالي التصنيفات", listOf("التصنيف", "عدد الحسابات", "إجمالي الأرصدة"), rows)
  }

  private fun salesPurchaseByItem(d: ReportData, type: String, title: String): ReportTable {
    val rows = linesWithInvoice(d).filter { it.first.type == type }
      .groupBy { it.second.itemName }
      .map { (name, list) ->
        val q = list.sumOf { (inv, l) -> if (inv.isReturn) -l.quantity else l.quantity }
        val t = list.sumOf { (inv, l) -> if (inv.isReturn) -l.total else l.total }
        listOf(name, qty(q), money(t))
      }
    return ReportTable(title, listOf("الصنف", "الكمية الصافية", "القيمة"), rows)
  }

  fun salesByItem(d: ReportData) = salesPurchaseByItem(d, "SALE", "المبيعات حسب الصنف")
  fun purchasesByItem(d: ReportData) = salesPurchaseByItem(d, "PURCHASE", "المشتريات حسب الصنف")

  fun itemProfits(d: ReportData): ReportTable {
    val costById = d.items.associate { it.id to it.costPrice }
    var totalProfit = 0.0
    val rows = linesWithInvoice(d).filter { it.first.type == "SALE" }
      .groupBy { it.second.itemName }
      .map { (name, list) ->
        val sign = { inv: InvoiceEntity -> if (inv.isReturn) -1.0 else 1.0 }
        val q = list.sumOf { (inv, l) -> sign(inv) * l.quantity }
        val revenue = list.sumOf { (inv, l) -> sign(inv) * l.total }
        val cost = list.sumOf { (inv, l) -> sign(inv) * l.quantity * (costById[l.itemId] ?: 0.0) }
        totalProfit += revenue - cost
        listOf(name, qty(q), money(revenue), money(cost), money(revenue - cost))
      }
    return ReportTable("أرباح الأصناف", listOf("الصنف", "الكمية", "المبيعات", "التكلفة", "الربح"), rows,
      listOf("الإجمالي", "", "", "", money(totalProfit)))
  }

  fun customerProfits(d: ReportData): ReportTable {
    val costById = d.items.associate { it.id to it.costPrice }
    val byInvoice = d.invoiceItems.groupBy { it.invoiceId }
    val rows = d.invoices.filter { it.type == "SALE" }.groupBy { it.partyName }.map { (party, list) ->
      var revenue = 0.0
      var cost = 0.0
      list.forEach { inv ->
        val sign = if (inv.isReturn) -1.0 else 1.0
        revenue += sign * invoiceNet(inv)
        cost += sign * (byInvoice[inv.id] ?: emptyList()).sumOf { (costById[it.itemId] ?: 0.0) * it.quantity }
      }
      listOf(party, money(revenue), money(cost), money(revenue - cost))
    }
    return ReportTable("الربح على مستوى العميل", listOf("العميل", "المبيعات", "التكلفة", "الربح"), rows)
  }

  fun journalEntries(d: ReportData): ReportTable =
    ReportTable("القيود اليومية", listOf("التاريخ", "مدين", "دائن", "المبلغ"),
      d.journal.map { listOf(it.date, it.debitAccount, it.creditAccount, money(it.amount)) })

  fun dailyOperations(d: ReportData): ReportTable {
    val rows = mutableListOf<List<String>>()
    d.invoices.filter { it.date == d.today }.forEach {
      val kind = (if (it.type == "SALE") "فاتورة بيع" else "فاتورة شراء") + if (it.isReturn) " (مرتجع)" else ""
      rows.add(listOf(kind, it.partyName, money(invoiceNet(it))))
    }
    d.vouchers.filter { it.date == d.today }.forEach {
      rows.add(listOf(if (it.type == "RECEIPT") "سند قبض" else "سند صرف", it.accountName, money(it.amount)))
    }
    d.journal.filter { it.date == d.today }.forEach {
      rows.add(listOf("قيد", "${it.debitAccount} ← ${it.creditAccount}", money(it.amount)))
    }
    return ReportTable("العمليات اليومية (${d.today})", listOf("العملية", "الطرف", "المبلغ"), rows)
  }

  fun cashMovement(d: ReportData): ReportTable {
    data class Row(val date: String, val text: String, val inAmount: Double, val outAmount: Double)
    val list = mutableListOf<Row>()
    d.invoices.filter { it.paidAmount > 0.0 }.forEach { inv ->
      val incoming = (inv.type == "SALE") != inv.isReturn
      val label = (if (inv.type == "SALE") "بيع" else "شراء") + (if (inv.isReturn) " مرتجع" else "") + " #${inv.invoiceNumber}"
      list.add(Row(inv.date, label, if (incoming) inv.paidAmount else 0.0, if (incoming) 0.0 else inv.paidAmount))
    }
    d.vouchers.forEach { v ->
      val incoming = v.type == "RECEIPT"
      list.add(Row(v.date, "سند ${if (incoming) "قبض" else "صرف"} #${v.voucherNumber} - ${v.accountName}",
        if (incoming) v.amount else 0.0, if (incoming) 0.0 else v.amount))
    }
    val sorted = list.sortedBy { it.date }
    val rows = sorted.map { listOf(it.date, it.text, money(it.inAmount), money(it.outAmount)) }
    val totalIn = sorted.sumOf { it.inAmount }
    val totalOut = sorted.sumOf { it.outAmount }
    return ReportTable("حركة الصندوق", listOf("التاريخ", "البيان", "داخل", "خارج"), rows,
      listOf("الإجمالي", "صافي: " + money(totalIn - totalOut), money(totalIn), money(totalOut)))
  }

  fun accountStatement(d: ReportData, accountName: String): ReportTable {
    data class Row(val date: String, val text: String, val debit: Double, val credit: Double)
    val list = mutableListOf<Row>()
    d.invoices.filter { it.partyName == accountName && it.isCredit }.forEach { inv ->
      val grows = !inv.isReturn
      val label = (if (inv.type == "SALE") "فاتورة بيع" else "فاتورة شراء") + (if (inv.isReturn) " مرتجع" else "") + " #${inv.invoiceNumber}"
      list.add(Row(inv.date, label, if (grows) inv.remainingAmount else 0.0, if (grows) 0.0 else inv.remainingAmount))
    }
    d.vouchers.filter { it.accountName == accountName }.forEach { v ->
      list.add(Row(v.date, "سند ${if (v.type == "RECEIPT") "قبض" else "صرف"} #${v.voucherNumber}", 0.0, v.amount))
    }
    d.journal.filter { it.debitAccount == accountName || it.creditAccount == accountName }.forEach { e ->
      val isDebit = e.debitAccount == accountName
      list.add(Row(e.date, "قيد #${e.entryNumber}", if (isDebit) e.amount else 0.0, if (isDebit) 0.0 else e.amount))
    }
    val sorted = list.sortedBy { it.date }
    val rows = sorted.map { listOf(it.date, it.text, money(it.debit), money(it.credit)) }
    val current = d.accounts.firstOrNull { it.name == accountName }?.balance ?: 0.0
    return ReportTable("كشف حساب: $accountName", listOf("التاريخ", "البيان", "زيادة", "تخفيض"), rows,
      listOf("الرصيد الحالي", "", "", money(current)))
  }

  fun currencyTotals(d: ReportData): ReportTable {
    val rows = d.currencies.map { c ->
      val sales = d.invoices.filter { it.type == "SALE" && it.currency == c.name }
        .sumOf { if (it.isReturn) -invoiceNet(it) else invoiceNet(it) }
      val purchases = d.invoices.filter { it.type == "PURCHASE" && it.currency == c.name }
        .sumOf { if (it.isReturn) -invoiceNet(it) else invoiceNet(it) }
      listOf(c.name, money(c.exchangeRate), money(sales), money(purchases))
    }
    return ReportTable("المبيعات والمشتريات حسب العملة", listOf("العملة", "سعر الصرف", "المبيعات", "المشتريات"), rows)
  }

  fun workingCapital(d: ReportData): ReportTable {
    val stockValue = d.items.sumOf { it.currentStock * it.costPrice }
    val receivables = d.accounts.filter { it.parentCategory == "العملاء" && it.balance > 0 }.sumOf { it.balance }
    val cash = d.accounts.filter { it.parentCategory == "الصناديق" || it.parentCategory == "البنوك" }.sumOf { it.balance }
    val payables = d.accounts.filter { it.parentCategory == "الموردون" && it.balance > 0 }.sumOf { it.balance }
    return ReportTable(
      "رأس المال العامل",
      listOf("البند", "القيمة"),
      listOf(
        listOf("المخزون", money(stockValue)),
        listOf("ذمم العملاء", money(receivables)),
        listOf("النقدية والبنوك", money(cash)),
        listOf("ذمم الموردين", money(payables)),
        listOf("رأس المال العامل", money(stockValue + receivables + cash - payables))
      )
    )
  }

  fun itemLines(d: ReportData): ReportTable {
    val rows = linesWithInvoice(d).sortedBy { it.first.date }.map { (inv, line) ->
      val kind = (if (inv.type == "SALE") "بيع" else "شراء") + if (inv.isReturn) " مرتجع" else ""
      listOf(inv.date, kind + " #" + inv.invoiceNumber, line.itemName, qty(line.quantity))
    }
    return ReportTable("تفاصيل حركة الأصناف", listOf("التاريخ", "الفاتورة", "الصنف", "الكمية"), rows)
  }

  fun build(name: String, d: ReportData, accountName: String = ""): ReportTable = when (name) {
    "المخزون المتبقي" -> remainingStock(d)
    "المخزون حسب المخزن" -> ListTables.warehouseStock(d.items, d.warehouseStock, d.warehouses)
    "أرصدة الحسابات" -> accountBalances(d)
    "أرباح الأصناف" -> itemProfits(d)
    "العمليات اليومية" -> dailyOperations(d)
    "القيود اليومية" -> journalEntries(d)
    "حركة الصندوق" -> cashMovement(d)
    "حركة الحسابات" -> accountStatement(d, accountName)
    "إجمالي التصنيفات" -> categoryTotals(d)
    "المبيعات والمشتريات حسب العملة" -> currencyTotals(d)
    "رأس المال العامل" -> workingCapital(d)
    "الربح على مستوى العميل" -> customerProfits(d)
    "المبيعات حسب الصنف" -> salesByItem(d)
    "المشتريات حسب الصنف" -> purchasesByItem(d)
    "تفاصيل حركة الأصناف" -> itemLines(d)
    else -> ReportTable(name, listOf("البند"), emptyList())
  }
}


/** Tables for the list screens' PDF / Excel / print actions. */
object ListTables {
  private fun money(v: Double): String = "%,.2f".format(v)
  private fun qty(v: Double): String = if (v == Math.rint(v)) "%,.0f".format(v) else "%,.2f".format(v)

  fun accounts(list: List<AccountEntity>) = ReportTable(
    "دليل الحسابات", listOf("الحساب", "التصنيف", "الهاتف", "الرصيد"),
    list.map { listOf(it.name, it.parentCategory, it.phone, money(it.balance)) }
  )

  fun items(list: List<ItemEntity>) = ReportTable(
    "دليل الأصناف", listOf("الصنف", "الرمز", "الوحدة", "التكلفة", "البيع", "المتاح"),
    list.map { listOf(it.name, it.code, it.unit, money(it.costPrice), money(it.sellingPrice), qty(it.currentStock)) }
  )

  fun vouchers(list: List<VoucherEntity>) = ReportTable(
    "كشف السندات", listOf("الرقم", "النوع", "الحساب", "التاريخ", "المبلغ"),
    list.map { listOf(it.voucherNumber, if (it.type == "RECEIPT") "قبض" else "صرف", it.accountName, it.date, money(it.amount)) },
    listOf("الإجمالي", "", "", "", money(list.sumOf { it.amount }))
  )

  fun invoices(list: List<InvoiceEntity>, title: String) = ReportTable(
    title, listOf("الرقم", "الطرف", "التاريخ", "الإجمالي", "المتبقي"),
    list.map {
      val net = it.subtotal - it.discount + it.tax + it.otherFees
      listOf(it.invoiceNumber + if (it.isReturn) " (مرتجع)" else "", it.partyName, it.date, money(net), money(it.remainingAmount))
    }
  )

  fun operations(list: List<WarehouseOperationEntity>) = ReportTable(
    "العمليات المخزنية", listOf("العملية", "المخزن", "التاريخ", "عدد الأصناف", "التكلفة"),
    list.map { listOf(it.operationType + " #" + it.operationNumber, it.warehouseName, it.date, it.totalItems.toString(), money(it.totalCost)) }
  )

  fun invoiceDocument(inv: InvoiceEntity, lines: List<InvoiceItemEntity>): ReportTable {
    val net = inv.subtotal - inv.discount + inv.tax + inv.otherFees
    val kind = when (inv.type) {
      "SALE" -> "فاتورة بيع"
      "PURCHASE" -> "فاتورة شراء"
      "QUOTE" -> "فاتورة عرض سعر"
      else -> "طلب شراء"
    } + if (inv.isReturn) " (مرتجع)" else ""
    val rows = lines.map { listOf(it.itemName, qty(it.quantity), money(it.unitPrice), money(it.total)) }.toMutableList()
    rows.add(listOf("المجموع", "", "", money(inv.subtotal)))
    if (inv.discount > 0.0) rows.add(listOf("الخصم", "", "", money(inv.discount)))
    if (inv.tax > 0.0) rows.add(listOf("الضريبة", "", "", money(inv.tax)))
    if (inv.otherFees > 0.0) rows.add(listOf("رسوم أخرى", "", "", money(inv.otherFees)))
    rows.add(listOf("المدفوع", "", "", money(inv.paidAmount)))
    return ReportTable(
      "$kind #${inv.invoiceNumber}", listOf("الصنف", "الكمية", "السعر", "الإجمالي"), rows,
      listOf("الصافي: ${inv.partyName}", inv.date, "المتبقي ${money(inv.remainingAmount)}", money(net)),
      headerNote = "${inv.partyName} - ${inv.date} - ${inv.warehouseName}"
    )
  }

  fun warehouseStock(items: List<ItemEntity>, stock: List<WarehouseStockEntity>, warehouses: List<WarehouseEntity>): ReportTable {
    val names = warehouses.map { it.name }
    val byKey = stock.associate { (it.itemId to it.warehouseName) to it.quantity }
    val rows = items.map { item ->
      listOf(item.name) + names.map { qty(byKey[item.id to it] ?: 0.0) } + listOf(qty(item.currentStock))
    }
    return ReportTable("المخزون حسب المخزن", listOf("الصنف") + names + listOf("الإجمالي"), rows)
  }
}
