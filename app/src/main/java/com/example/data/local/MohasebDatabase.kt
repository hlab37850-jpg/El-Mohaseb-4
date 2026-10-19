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
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
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
    AppSettingEntity::class
  ],
  version = 2,
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
          .addMigrations(MIGRATION_1_2)
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
  }
}
