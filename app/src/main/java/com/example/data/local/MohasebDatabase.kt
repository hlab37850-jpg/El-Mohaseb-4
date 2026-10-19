package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    AccountEntity::class,
    ItemEntity::class,
    WarehouseEntity::class,
    InvoiceEntity::class,
    InvoiceItemEntity::class,
    VoucherEntity::class,
    CurrencyEntity::class,
    WarehouseOperationEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class MohasebDatabase : RoomDatabase() {
  abstract fun accountingDao(): AccountingDao

  companion object {
    @Volatile
    private var INSTANCE: MohasebDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): MohasebDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          MohasebDatabase::class.java,
          "mohaseb_soft_db"
        )
          .fallbackToDestructiveMigration()
          .addCallback(PrepopulateCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class PrepopulateCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database.accountingDao())
          }
        }
      }

      private suspend fun populateInitialData(dao: AccountingDao) {
        // 1. Warehouses
        dao.insertWarehouses(
          listOf(
            WarehouseEntity(id = 1, name = "المخزن الرئيسي", phone = "770000001", address = "شارع الستين - المركز التجاري"),
            WarehouseEntity(id = 2, name = "مخزن فرع المبيعات", phone = "770000002", address = "شارع تعز")
          )
        )

        // 2. Currencies
        dao.insertCurrencies(
          listOf(
            CurrencyEntity(id = 1, name = "محلي", symbol = "YR", subUnit = "فلس", exchangeRate = 1.0, accountCount = 2),
            CurrencyEntity(id = 2, name = "دولار", symbol = "USD", subUnit = "سنت", exchangeRate = 530.0, accountCount = 1)
          )
        )

        // 3. Accounts
        dao.insertAccounts(
          listOf(
            AccountEntity(id = 1, name = "الصندوق الرئيسي", parentCategory = "الصناديق", balance = 350000.0, currency = "محلي", phone = "771111111", address = "الإدارة العامة"),
            AccountEntity(id = 2, name = "بنك التضامن الإسلامي", parentCategory = "البنوك", balance = 1200000.0, currency = "محلي"),
            AccountEntity(id = 3, name = "مؤسسة الأمل التجارية", parentCategory = "العملاء", balance = -4500.0, currency = "محلي", phone = "772222222", address = "صنعاء"),
            AccountEntity(id = 4, name = "شركة النور للمعدات", parentCategory = "الموردون", balance = 8200.0, currency = "محلي", phone = "773333333", address = "الحديدة"),
            AccountEntity(id = 5, name = "فلا للتجارة", parentCategory = "الموردون", balance = 4000.0, currency = "محلي", phone = "774444444"),
            AccountEntity(id = 6, name = "مصروفات إيجار وكهرباء", parentCategory = "مصروفات", balance = 0.0, currency = "محلي")
          )
        )

        // 4. Items
        dao.insertItems(
          listOf(
            ItemEntity(id = 1, code = "ITM-001", name = "ركب أصلي ممتاز", category = "عام", unit = "حبة", costPrice = 200.0, sellingPrice = 500.0, openingQuantity = 10.0, currentStock = 8.0),
            ItemEntity(id = 2, code = "ITM-002", name = "زيت محرك 5W-30 جيلون", category = "زيوت", unit = "حبة", costPrice = 1800.0, sellingPrice = 2400.0, openingQuantity = 25.0, currentStock = 20.0),
            ItemEntity(id = 3, code = "ITM-003", name = "فلتر هواء سيارات", category = "فلاتر", unit = "حبة", costPrice = 350.0, sellingPrice = 600.0, openingQuantity = 40.0, currentStock = 38.0),
            ItemEntity(id = 4, code = "ITM-004", name = "بطارية جافة 70 أمبير", category = "بطاريات", unit = "حبة", costPrice = 14000.0, sellingPrice = 17500.0, openingQuantity = 12.0, currentStock = 9.0)
          )
        )

        // 5. Invoices
        val invId = dao.insertInvoice(
          InvoiceEntity(
            id = 1,
            invoiceNumber = "1",
            type = "PURCHASE",
            isCredit = true,
            isReturn = false,
            date = "2026-10-08",
            partyName = "فا - أجل",
            warehouseName = "المخزن الرئيسي",
            currency = "محلي",
            subtotal = 2500.0,
            discount = 0.0,
            tax = 0.0,
            otherFees = 0.0,
            paidAmount = 1000.0,
            remainingAmount = 1500.0,
            notes = "توريد قطع ركب للمخزن"
          )
        )
        dao.insertInvoiceItems(
          listOf(
            InvoiceItemEntity(id = 1, invoiceId = invId, itemId = 1, itemName = "ركب", quantity = 5.0, unitPrice = 500.0, total = 2500.0)
          )
        )

        // 6. Vouchers
        dao.insertVouchers(
          listOf(
            VoucherEntity(
              id = 1,
              voucherNumber = "1",
              type = "RECEIPT",
              accountName = "مؤسسة الأمل التجارية",
              amount = 3000.0,
              currency = "محلي",
              date = "2026-10-08",
              statement = "سداد دفعة من الحساب",
              notes = "نقداً في الصندوق الرئيسي"
            ),
            VoucherEntity(
              id = 2,
              voucherNumber = "2",
              type = "PAYMENT",
              accountName = "شركة النور للمعدات",
              amount = 2000.0,
              currency = "محلي",
              date = "2026-10-08",
              statement = "سداد دفعة فاتورة مشتريات",
              notes = "صرف نقدي"
            )
          )
        )

        // 7. Warehouse Operations
        dao.insertWarehouseOperations(
          listOf(
            WarehouseOperationEntity(
              id = 1,
              operationNumber = "1",
              operationType = "توريد مخزني",
              warehouseName = "المخزن الرئيسي",
              date = "2026-10-08",
              partyOrAccount = "شركة النور للمعدات",
              totalItems = 5,
              totalCost = 2500.0,
              notes = "استلام بضاعة جديدة"
            )
          )
        )
      }
    }
  }
}
