package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AppSettingEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.JournalEntryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import com.example.data.model.WarehouseOperationLineEntity
import com.example.data.model.WarehouseStockEntity
import kotlinx.coroutines.CoroutineScope

@Database(
  entities = [
    AccountEntity::class,
    ItemEntity::class,
    WarehouseEntity::class,
    InvoiceEntity::class,
    InvoiceItemEntity::class,
    VoucherEntity::class,
    CurrencyEntity::class,
    WarehouseOperationEntity::class,
    AppSettingEntity::class,
    JournalEntryEntity::class,
    ReminderEntity::class,
    WarehouseStockEntity::class,
    WarehouseOperationLineEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class MohasebDatabase : RoomDatabase() {
  abstract fun accountingDao(): AccountingDao

  companion object {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS app_settings (settingKey TEXT NOT NULL, settingValue TEXT NOT NULL, PRIMARY KEY(settingKey))"
        )
      }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS journal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, entryNumber TEXT NOT NULL, entryType TEXT NOT NULL, debitAccount TEXT NOT NULL, creditAccount TEXT NOT NULL, amount REAL NOT NULL, currency TEXT NOT NULL, date TEXT NOT NULL, statement TEXT NOT NULL)"
        )
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS reminders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, note TEXT NOT NULL, phone TEXT NOT NULL, isDone INTEGER NOT NULL)"
        )
      }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS warehouse_stock (itemId INTEGER NOT NULL, warehouseName TEXT NOT NULL, quantity REAL NOT NULL, PRIMARY KEY(itemId, warehouseName))"
        )
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS warehouse_operation_lines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, operationId INTEGER NOT NULL, itemId INTEGER NOT NULL, itemName TEXT NOT NULL, quantity REAL NOT NULL, sourceDelta REAL NOT NULL, targetDelta REAL NOT NULL, unitCost REAL NOT NULL)"
        )
        // Existing single-total stock is assigned to the first warehouse so nothing is lost.
        db.execSQL(
          "INSERT OR IGNORE INTO warehouse_stock (itemId, warehouseName, quantity) " +
            "SELECT i.id, (SELECT name FROM warehouses ORDER BY id ASC LIMIT 1), i.currentStock FROM items i " +
            "WHERE (SELECT COUNT(*) FROM warehouses) > 0"
        )
      }
    }

    @Volatile
    private var INSTANCE: MohasebDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): MohasebDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          MohasebDatabase::class.java,
          "mohaseb_soft_db"
        )
          // Preserve existing accounting data and migrate schema changes explicitly.
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
          .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              // Minimal system defaults required by the existing invoice pickers; no demo transactions.
              db.execSQL(
                "INSERT INTO warehouses (id, name, phone, address) VALUES (1, 'المخزن الرئيسي', '', '')"
              )
              db.execSQL(
                "INSERT INTO currencies (id, name, symbol, subUnit, exchangeRate, accountCount) VALUES (1, 'محلي', 'YR', 'جزء', 1.0, 0)"
              )
            }
          })
          .build()
          .also { INSTANCE = it }
      }
    }

    /** Closes and forgets the singleton so the database file can be replaced (restore). */
    fun closeInstance() {
      synchronized(this) {
        INSTANCE?.close()
        INSTANCE = null
      }
    }
  }
}
