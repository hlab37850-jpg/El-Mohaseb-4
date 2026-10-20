package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountingDao
import com.example.data.local.MohasebDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.JournalEntryEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import com.example.data.repository.OperationLineRequest
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountingDatabaseTest {

  private lateinit var db: MohasebDatabase
  private lateinit var dao: AccountingDao
  private lateinit var repository: AccountingRepository

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, MohasebDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = db.accountingDao()
    repository = AccountingRepository(dao, db)
    runBlocking {
      dao.insertWarehouse(WarehouseEntity(name = "المخزن الرئيسي"))
      dao.insertCurrency(CurrencyEntity(name = "محلي", symbol = "YR", subUnit = "جزء"))
    }
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun testAppSettingsPersistThroughRoom() = runBlocking {
    repository.saveSettings(
      mapOf(
        "company_name" to "اختبار المنشأة",
        "printer_thermal_enabled" to "true"
      )
    )
    assertEquals("اختبار المنشأة", dao.getSetting("company_name")?.settingValue)
    assertEquals("true", dao.getSetting("printer_thermal_enabled")?.settingValue)
  }

  @Test
  fun testAccountCRUD() = runBlocking {
    // 1. Create
    val id = repository.insertAccount(
      AccountEntity(
        name = "عميل تجريبي",
        parentCategory = "العملاء",
        balance = 5000.0,
        phone = "771234567"
      )
    )
    assertTrue(id > 0)

    // 2. Read
    val accounts = repository.allAccounts.first()
    assertEquals(1, accounts.size)
    val acc = accounts[0]
    assertEquals("عميل تجريبي", acc.name)
    assertEquals(5000.0, acc.balance, 0.001)

    // 3. Update
    repository.updateAccount(acc.copy(phone = "779999999", balance = 6000.0))
    val updated = repository.allAccounts.first()[0]
    assertEquals("779999999", updated.phone)
    assertEquals(6000.0, updated.balance, 0.001)

    // 4. Settle the balance before deletion; non-zero accounts must be protected.
    val settled = updated.copy(balance = 0.0)
    repository.updateAccount(settled)
    repository.deleteAccount(settled)
    val emptyList = repository.allAccounts.first()
    assertTrue(emptyList.isEmpty())
  }

  @Test
  fun testItemStockAdjustmentOnInvoice() = runBlocking {
    // Insert test item with 20 in stock
    val itemId = repository.insertItem(
      ItemEntity(
        code = "ITM-01",
        name = "قطع غيار تجريبية",
        costPrice = 100.0,
        sellingPrice = 150.0,
        currentStock = 20.0
      )
    )

    // Create Sale invoice of 5 items
    val invoice = InvoiceEntity(
      invoiceNumber = "101",
      type = "SALE",
      isCredit = false,
      date = "2026-10-19",
      partyName = "عميل نقدي",
      subtotal = 750.0,
      remainingAmount = 750.0
    )
    val lineItems = listOf(
      InvoiceItemEntity(
        invoiceId = 0,
        itemId = itemId,
        itemName = "قطع غيار تجريبية",
        quantity = 5.0,
        unitPrice = 150.0,
        total = 750.0
      )
    )

    val invId = repository.createInvoice(invoice, lineItems)
    assertTrue(invId > 0)

    // Stock should be 20 - 5 = 15
    val itemAfterSale = dao.getItemById(itemId)
    assertNotNull(itemAfterSale)
    assertEquals(15.0, itemAfterSale!!.currentStock, 0.001)

    // Deleting the invoice should restore stock back to 20
    val savedInvoice = dao.getAllInvoices().first()[0]
    repository.deleteInvoice(savedInvoice)

    val itemAfterDelete = dao.getItemById(itemId)
    assertEquals(20.0, itemAfterDelete!!.currentStock, 0.001)
  }

  @Test
  fun testVoucherBalanceAdjustment() = runBlocking {
    // Insert Customer Account with 10000 balance
    repository.insertAccount(
      AccountEntity(
        name = "مؤسسة الهدى",
        parentCategory = "العملاء",
        balance = 10000.0
      )
    )

    // Create Receipt voucher of 3000
    val vId = repository.insertVoucher(
      VoucherEntity(
        voucherNumber = "V-1",
        type = "RECEIPT",
        accountName = "مؤسسة الهدى",
        amount = 3000.0,
        date = "2026-10-19"
      )
    )
    assertTrue(vId > 0)

    val accAfterReceipt = dao.getAccountByName("مؤسسة الهدى")
    assertNotNull(accAfterReceipt)
    assertEquals(7000.0, accAfterReceipt!!.balance, 0.001)

    // Editing the amount must reverse the previous impact and apply the new amount.
    val savedVoucher = dao.getAllVouchers().first()[0]
    repository.updateVoucher(savedVoucher.copy(amount = 4000.0))
    val accAfterUpdate = dao.getAccountByName("مؤسسة الهدى")
    assertEquals(6000.0, accAfterUpdate!!.balance, 0.001)

    // Deleting the edited voucher reverses its latest impact exactly once.
    val updatedVoucher = dao.getAllVouchers().first()[0]
    repository.deleteVoucher(updatedVoucher)
    val accAfterDelete = dao.getAccountByName("مؤسسة الهدى")
    assertEquals(10000.0, accAfterDelete!!.balance, 0.001)
  }

  @Test
  fun testJournalEntryAdjustsBothAccountsAndCanBeReversed() = runBlocking {
    repository.insertAccount(AccountEntity(name = "الصندوق", parentCategory = "الصناديق", balance = 1000.0))
    repository.insertAccount(AccountEntity(name = "عميل قيد", parentCategory = "العملاء", balance = 500.0))
    val entry = JournalEntryEntity(
      entryNumber = "1",
      entryType = "DAILY",
      debitAccount = "الصندوق",
      creditAccount = "عميل قيد",
      amount = 200.0,
      date = "2026-10-19"
    )
    val id = repository.insertJournalEntry(entry)
    assertEquals(1200.0, dao.getAccountByName("الصندوق")!!.balance, 0.001)
    assertEquals(300.0, dao.getAccountByName("عميل قيد")!!.balance, 0.001)

    repository.deleteJournalEntry(entry.copy(id = id))
    assertEquals(1000.0, dao.getAccountByName("الصندوق")!!.balance, 0.001)
    assertEquals(500.0, dao.getAccountByName("عميل قيد")!!.balance, 0.001)
  }

  @Test
  fun testQuotationDoesNotTouchStockOrBalances() = runBlocking {
    val itemId = repository.insertItem(
      ItemEntity(code = "Q-1", name = "صنف عرض", costPrice = 10.0, sellingPrice = 20.0, currentStock = 7.0)
    )
    val invoice = InvoiceEntity(
      invoiceNumber = "Q1",
      type = "QUOTE",
      isCredit = true,
      date = "2026-10-19",
      partyName = "عميل غير مسجل",
      subtotal = 40.0,
      remainingAmount = 40.0
    )
    val lines = listOf(
      InvoiceItemEntity(invoiceId = 0, itemId = itemId, itemName = "صنف عرض", quantity = 2.0, unitPrice = 20.0, total = 40.0)
    )
    repository.createInvoice(invoice, lines)
    assertEquals(7.0, dao.getItemById(itemId)!!.currentStock, 0.001)
  }

  @Test
  fun testClosedPeriodRejectsOldPostings() = runBlocking {
    repository.insertAccount(AccountEntity(name = "الصندوق", parentCategory = "الصناديق"))
    repository.insertAccount(AccountEntity(name = "عميل", parentCategory = "العملاء"))
    repository.saveSettings(mapOf("closed_until" to "2025-12-31"))
    val oldEntry = JournalEntryEntity(
      entryNumber = "9", entryType = "DAILY", debitAccount = "الصندوق",
      creditAccount = "عميل", amount = 10.0, date = "2025-06-01"
    )
    val rejected = runCatching { repository.insertJournalEntry(oldEntry) }
    assertTrue(rejected.isFailure)
    // A posting after the closing date is still accepted.
    repository.insertJournalEntry(oldEntry.copy(date = "2026-01-02"))
  }

  @Test
  fun testStockOperationsMoveQuantitiesBetweenWarehouses() = runBlocking {
    dao.insertWarehouse(WarehouseEntity(name = "مخزن الفرع"))
    val itemId = repository.insertItem(
      ItemEntity(code = "W-1", name = "صنف مخزني", costPrice = 5.0, sellingPrice = 9.0, currentStock = 10.0)
    )
    assertEquals(10.0, dao.getWarehouseQty(itemId, "المخزن الرئيسي")!!, 0.001)

    val supply = WarehouseOperationEntity(
      operationNumber = "1", operationType = "توريد مخزني", warehouseName = "المخزن الرئيسي", date = "2026-10-19"
    )
    repository.createStockOperation(supply, listOf(OperationLineRequest(itemId, 5.0)))
    assertEquals(15.0, dao.getWarehouseQty(itemId, "المخزن الرئيسي")!!, 0.001)
    assertEquals(15.0, dao.getItemById(itemId)!!.currentStock, 0.001)

    val transfer = WarehouseOperationEntity(
      operationNumber = "2", operationType = "تحويل مخزني", warehouseName = "المخزن الرئيسي",
      targetWarehouseName = "مخزن الفرع", date = "2026-10-19"
    )
    val transferId = repository.createStockOperation(transfer, listOf(OperationLineRequest(itemId, 4.0)))
    assertEquals(11.0, dao.getWarehouseQty(itemId, "المخزن الرئيسي")!!, 0.001)
    assertEquals(4.0, dao.getWarehouseQty(itemId, "مخزن الفرع")!!, 0.001)
    assertEquals(15.0, dao.getItemById(itemId)!!.currentStock, 0.001)

    val tooMuch = runCatching {
      repository.createStockOperation(
        WarehouseOperationEntity(operationNumber = "3", operationType = "صرف مخزني", warehouseName = "مخزن الفرع", date = "2026-10-19"),
        listOf(OperationLineRequest(itemId, 50.0))
      )
    }
    assertTrue(tooMuch.isFailure)

    repository.deleteOperation(dao.getOperationById(transferId)!!)
    assertEquals(15.0, dao.getWarehouseQty(itemId, "المخزن الرئيسي")!!, 0.001)
    assertEquals(0.0, dao.getWarehouseQty(itemId, "مخزن الفرع")!!, 0.001)
  }

  @Test
  fun testInventoryCountSetsWarehouseQuantity() = runBlocking {
    val itemId = repository.insertItem(
      ItemEntity(code = "C-1", name = "صنف جرد", costPrice = 1.0, sellingPrice = 2.0, currentStock = 10.0)
    )
    repository.createStockOperation(
      WarehouseOperationEntity(operationNumber = "9", operationType = "جرد مخزني", warehouseName = "المخزن الرئيسي", date = "2026-10-19"),
      listOf(OperationLineRequest(itemId, 7.0))
    )
    assertEquals(7.0, dao.getWarehouseQty(itemId, "المخزن الرئيسي")!!, 0.001)
    assertEquals(7.0, dao.getItemById(itemId)!!.currentStock, 0.001)
  }

  @Test
  fun testUpdateInvoiceRecomputesStockAndBalance() = runBlocking {
    repository.insertAccount(AccountEntity(name = "عميل تعديل", parentCategory = "العملاء"))
    val itemId = repository.insertItem(
      ItemEntity(code = "U-1", name = "صنف تعديل", costPrice = 5.0, sellingPrice = 10.0, currentStock = 20.0)
    )
    val original = InvoiceEntity(
      invoiceNumber = "U1", type = "SALE", isCredit = true, date = "2026-10-19", partyName = "عميل تعديل",
      subtotal = 30.0, remainingAmount = 30.0
    )
    val id = repository.createInvoice(
      original,
      listOf(InvoiceItemEntity(invoiceId = 0, itemId = itemId, itemName = "صنف تعديل", quantity = 3.0, unitPrice = 10.0, total = 30.0))
    )
    assertEquals(17.0, dao.getItemById(itemId)!!.currentStock, 0.001)
    assertEquals(30.0, dao.getAccountByName("عميل تعديل")!!.balance, 0.001)

    val saved = original.copy(id = id)
    repository.updateInvoice(
      saved,
      saved.copy(subtotal = 50.0, remainingAmount = 50.0),
      listOf(InvoiceItemEntity(invoiceId = 0, itemId = itemId, itemName = "صنف تعديل", quantity = 5.0, unitPrice = 10.0, total = 50.0))
    )
    assertEquals(15.0, dao.getItemById(itemId)!!.currentStock, 0.001)
    assertEquals(50.0, dao.getAccountByName("عميل تعديل")!!.balance, 0.001)
    assertEquals(15.0, dao.getWarehouseQty(itemId, "المخزن الرئيسي")!!, 0.001)
  }
}
