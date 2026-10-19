package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountEntity
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
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          MohasebDatabase::class.java,
          "mohaseb_soft_db"
        )
          // Never silently delete a user's accounting data when a schema migration is missing.
          // Add an explicit Migration before incrementing the database version.
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
