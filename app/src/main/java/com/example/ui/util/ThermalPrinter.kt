package com.example.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * ESC/POS printing over a paired Bluetooth (SPP) thermal printer.
 * The receipt is drawn as a bitmap and sent as a raster image, so Arabic text is always shaped correctly.
 */
object ThermalPrinter {
  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

  fun hasPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

  /** Paired devices as (name, address); empty when Bluetooth is off or permission is missing. */
  @SuppressLint("MissingPermission")
  fun pairedDevices(context: Context): List<Pair<String, String>> {
    if (!hasPermission(context)) return emptyList()
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return emptyList()
    val adapter = manager.adapter ?: return emptyList()
    return runCatching { adapter.bondedDevices.map { (it.name ?: it.address) to it.address } }.getOrDefault(emptyList())
  }

  fun renderBitmap(table: ReportTable, widthPx: Int): Bitmap {
    val pad = 8f
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 30f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; color = AndroidColor.BLACK
    }
    val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 24f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT; color = AndroidColor.BLACK
    }
    val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 24f; textAlign = Paint.Align.RIGHT; color = AndroidColor.BLACK
    }
    val rule = Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1.5f }

    val cols = table.headers.size.coerceAtLeast(1)
    val weights = List(cols) { if (it == 0) 2f else 1f }
    val widths = weights.map { (widthPx - 2 * pad) * it / weights.sum() }
    fun fit(text: String, paint: Paint, w: Float): String {
      var t = text
      while (t.length > 1 && paint.measureText(t) > w - 4f) t = t.dropLast(1)
      return t
    }

    val rowH = 34f
    val noteLines = if (table.headerNote.isBlank()) 0 else 1
    val totalRows = 1 + table.rows.size + (if (table.totals != null) 1 else 0)
    val height = (50f + noteLines * 34f + totalRows * rowH + 70f).toInt()
    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    var y = 36f
    canvas.drawText(table.title, widthPx / 2f, y, title)
    y += 20f
    if (table.headerNote.isNotBlank()) {
      y += 24f
      canvas.drawText(fit(table.headerNote, normal, widthPx - 2 * pad), widthPx - pad, y, normal)
    }
    y += 12f
    canvas.drawLine(pad, y, widthPx - pad, y, rule)

    fun row(cells: List<String>, paint: Paint) {
      y += rowH - 6f
      var xRight = widthPx - pad
      for (i in 0 until cols) {
        canvas.drawText(fit(cells.getOrElse(i) { "" }, paint, widths[i]), xRight, y, paint)
        xRight -= widths[i]
      }
      y += 6f
    }
    row(table.headers, bold)
    canvas.drawLine(pad, y, widthPx - pad, y, rule)
    table.rows.forEach { row(it, normal) }
    table.totals?.let {
      canvas.drawLine(pad, y, widthPx - pad, y, rule)
      row(it, bold)
    }
    return bitmap
  }

  private fun toEscPos(bitmap: Bitmap, cut: Boolean): ByteArray {
    val w = bitmap.width
    val h = bitmap.height
    val bytesPerRow = (w + 7) / 8
    val out = ByteArrayOutputStream()
    out.write(byteArrayOf(0x1B, 0x40)) // initialise
    var startRow = 0
    while (startRow < h) {
      val rows = minOf(200, h - startRow)
      out.write(
        byteArrayOf(
          0x1D, 0x76, 0x30, 0x00,
          (bytesPerRow and 0xFF).toByte(), ((bytesPerRow shr 8) and 0xFF).toByte(),
          (rows and 0xFF).toByte(), ((rows shr 8) and 0xFF).toByte()
        )
      )
      for (r in startRow until startRow + rows) {
        for (b in 0 until bytesPerRow) {
          var value = 0
          for (bit in 0 until 8) {
            val x = b * 8 + bit
            val dark = x < w && AndroidColor.red(bitmap.getPixel(x, r)) < 128
            if (dark) value = value or (0x80 shr bit)
          }
          out.write(value)
        }
      }
      startRow += rows
    }
    out.write(byteArrayOf(0x1B, 0x64, 0x04)) // feed 4 lines
    if (cut) out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
    return out.toByteArray()
  }

  @SuppressLint("MissingPermission")
  suspend fun print(context: Context, address: String, table: ReportTable, wide80: Boolean, cut: Boolean) {
    withContext(Dispatchers.IO) {
      require(hasPermission(context)) { "صلاحية البلوتوث غير ممنوحة" }
      val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      val adapter = manager?.adapter ?: throw IllegalStateException("البلوتوث غير متاح على هذا الجهاز")
      require(adapter.isEnabled) { "فعّل البلوتوث ثم أعد المحاولة" }
      val device = adapter.getRemoteDevice(address)
      val bitmap = renderBitmap(table, if (wide80) 576 else 384)
      val data = toEscPos(bitmap, cut)
      val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      try {
        adapter.cancelDiscovery()
        socket.connect()
        socket.outputStream.write(data)
        socket.outputStream.flush()
        Thread.sleep(400)
      } finally {
        runCatching { socket.close() }
      }
    }
  }
}
