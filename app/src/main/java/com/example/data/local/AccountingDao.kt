package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountEntity
import com.example.data.model.CurrencyEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.ItemEntity
import com.example.data.model.VoucherEntity
import com.example.data.model.WarehouseEntity
import com.example.data.model.WarehouseOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {
  // Accounts
  @Query("SELECT * FROM accounts ORDER BY name ASC")
  fun getAllAccounts(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts WHERE parentCategory = :category ORDER BY name ASC")
  fun getAccountsByCategory(category: String): Flow<List<AccountEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccount(account: AccountEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccounts(accounts: List<AccountEntity>)

  @Update
  suspend fun updateAccount(account: AccountEntity)

  @Delete
  suspend fun deleteAccount(account: AccountEntity)

  // Items
  @Query("SELECT * FROM items ORDER BY name ASC")
  fun getAllItems(): Flow<List<ItemEntity>>

  @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'")
  fun searchItems(query: String): Flow<List<ItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItem(item: ItemEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItems(items: List<ItemEntity>)

  @Update
  suspend fun updateItem(item: ItemEntity)

  @Delete
  suspend fun deleteItem(item: ItemEntity)

  @Query("SELECT * FROM items WHERE id = :id")
  suspend fun getItemById(id: Long): ItemEntity?

  @Query("UPDATE items SET currentStock = currentStock + :qtyChange WHERE id = :id")
  suspend fun adjustItemStock(id: Long, qtyChange: Double)

  @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
  suspend fun getAccountByName(name: String): AccountEntity?

  @Query("UPDATE accounts SET balance = balance + :balanceChange WHERE name = :name")
  suspend fun adjustAccountBalance(name: String, balanceChange: Double)

  // Warehouses
  @Query("SELECT * FROM warehouses ORDER BY id ASC")
  fun getAllWarehouses(): Flow<List<WarehouseEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWarehouse(warehouse: WarehouseEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWarehouses(warehouses: List<WarehouseEntity>)

  @Update
  suspend fun updateWarehouse(warehouse: WarehouseEntity)

  @Delete
  suspend fun deleteWarehouse(warehouse: WarehouseEntity)

  // Invoices
  @Query("SELECT * FROM invoices ORDER BY id DESC")
  fun getAllInvoices(): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoices WHERE type = :type ORDER BY id DESC")
  fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
  fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItemEntity>>

  @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
  suspend fun getInvoiceItemsSync(invoiceId: Long): List<InvoiceItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoice(invoice: InvoiceEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

  @Update
  suspend fun updateInvoice(invoice: InvoiceEntity)

  @Delete
  suspend fun deleteInvoice(invoice: InvoiceEntity)

  @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
  suspend fun deleteInvoiceItems(invoiceId: Long)

  // Vouchers
  @Query("SELECT * FROM vouchers ORDER BY id DESC")
  fun getAllVouchers(): Flow<List<VoucherEntity>>

  @Query("SELECT * FROM vouchers WHERE type = :type ORDER BY id DESC")
  fun getVouchersByType(type: String): Flow<List<VoucherEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVoucher(voucher: VoucherEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVouchers(vouchers: List<VoucherEntity>)

  @Update
  suspend fun updateVoucher(voucher: VoucherEntity)

  @Delete
  suspend fun deleteVoucher(voucher: VoucherEntity)

  // Currencies
  @Query("SELECT * FROM currencies ORDER BY id ASC")
  fun getAllCurrencies(): Flow<List<CurrencyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCurrency(currency: CurrencyEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

  @Update
  suspend fun updateCurrency(currency: CurrencyEntity)

  @Delete
  suspend fun deleteCurrency(currency: CurrencyEntity)

  // Warehouse Operations
  @Query("SELECT * FROM warehouse_operations ORDER BY id DESC")
  fun getAllWarehouseOperations(): Flow<List<WarehouseOperationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWarehouseOperation(op: WarehouseOperationEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWarehouseOperations(ops: List<WarehouseOperationEntity>)

  @Update
  suspend fun updateWarehouseOperation(op: WarehouseOperationEntity)

  @Delete
  suspend fun deleteWarehouseOperation(op: WarehouseOperationEntity)
}
