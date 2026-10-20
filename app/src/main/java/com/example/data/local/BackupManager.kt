package com.example.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Local backup / restore of the whole Room database file through the Storage Access Framework. */
object BackupManager {
  const val DB_NAME = "mohaseb_soft_db"
  private const val SQLITE_HEADER = "SQLite format 3"

  suspend fun backupTo(context: Context, database: MohasebDatabase, target: Uri) {
    withContext(Dispatchers.IO) {
      // Flush the write-ahead log into the main database file before copying it.
      database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
      val dbFile = context.getDatabasePath(DB_NAME)
      require(dbFile.exists()) { "ملف قاعدة البيانات غير موجود" }
      val output = context.contentResolver.openOutputStream(target, "wt")
        ?: throw IllegalStateException("تعذر فتح ملف الوجهة للكتابة")
      output.use { out -> dbFile.inputStream().use { input -> input.copyTo(out) } }
    }
  }

  suspend fun restoreFrom(context: Context, source: Uri) {
    withContext(Dispatchers.IO) {
      // Copy to a temporary file first and validate it, so a bad file never replaces the live data.
      val temp = File(context.cacheDir, "restore_candidate.db")
      val input = context.contentResolver.openInputStream(source)
        ?: throw IllegalStateException("تعذر قراءة ملف النسخة الاحتياطية")
      input.use { stream -> temp.outputStream().use { stream.copyTo(it) } }
      val header = ByteArray(SQLITE_HEADER.length)
      val read = temp.inputStream().use { it.read(header) }
      if (read != header.size || String(header, Charsets.US_ASCII) != SQLITE_HEADER) {
        temp.delete()
        throw IllegalArgumentException("الملف المحدد ليس نسخة احتياطية صالحة لقاعدة البيانات")
      }
      MohasebDatabase.closeInstance()
      val dbFile = context.getDatabasePath(DB_NAME)
      File(dbFile.path + "-wal").delete()
      File(dbFile.path + "-shm").delete()
      temp.copyTo(dbFile, overwrite = true)
      temp.delete()
    }
  }
}
