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
import com.example.data.model.VoucherEntity
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
    repository = AccountingRepository(dao)
  }

  @After
  fun closeDb() {
    db.close()
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

    // 4. Delete
    repository.deleteAccount(updated)
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
      subtotal = 750.0
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

    // Delete voucher reverses the balance back to 10000
    val savedVoucher = dao.getAllVouchers().first()[0]
    repository.deleteVoucher(savedVoucher)

    val accAfterDelete = dao.getAccountByName("مؤسسة الهدى")
    assertEquals(10000.0, accAfterDelete!!.balance, 0.001)
  }
}
