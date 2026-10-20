package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.ItemEntity
import com.example.ui.viewmodel.AccountingViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Minimal .xlsx writer (one sheet, right-to-left, inline strings) so no external library is needed. */
object ExcelExporter {
  private fun esc(t: String): String =
    t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private fun colName(i: Int): String {
    var n = i
    var s = ""
    while (n >= 0) {
      s = ('A' + (n % 26)) + s
      n = n / 26 - 1
    }
    return s
  }

  fun write(out: OutputStream, table: ReportTable) {
    val sheet = StringBuilder()
    sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
    sheet.append("<sheetViews><sheetView rightToLeft=\"1\" workbookViewId=\"0\"/></sheetViews><sheetData>")
    val all = mutableListOf(table.headers)
    all.addAll(table.rows)
    table.totals?.let { all.add(it) }
    all.forEachIndexed { r, row ->
      sheet.append("<row r=\"${r + 1}\">")
      row.forEachIndexed { c, cell ->
        val ref = colName(c) + (r + 1)
        val number = if (r > 0) cell.replace(",", "").toDoubleOrNull() else null
        if (number != null) {
          sheet.append("<c r=\"$ref\"><v>$number</v></c>")
        } else {
          sheet.append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${esc(cell)}</t></is></c>")
        }
      }
      sheet.append("</row>")
    }
    sheet.append("</sheetData></worksheet>")

    ZipOutputStream(out).use { zip ->
      fun put(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }
      put(
        "[Content_Types].xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
          "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
          "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
          "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
          "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
          "</Types>"
      )
      put(
        "_rels/.rels",
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
          "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
          "</Relationships>"
      )
      put(
        "xl/workbook.xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
          "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
          "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
      )
      put(
        "xl/_rels/workbook.xml.rels",
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
          "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
          "</Relationships>"
      )
      put("xl/worksheets/sheet1.xml", sheet.toString())
    }
  }
}

/** Sends a table to the system print dialog (any installed printer service, including Bluetooth/thermal plugins). */
object PrintHelper {
  fun print(context: Context, table: ReportTable) {
    val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    manager.print(table.title, object : PrintDocumentAdapter() {
      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
      ) {
        if (cancellationSignal?.isCanceled == true) {
          callback?.onLayoutCancelled()
          return
        }
        val info = PrintDocumentInfo.Builder(table.title + ".pdf")
          .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
          .build()
        callback?.onLayoutFinished(info, true)
      }

      override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
      ) {
        try {
          FileOutputStream(destination!!.fileDescriptor).use { PdfExporter.write(it, table) }
          callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
          callback?.onWriteFailed(e.message)
        }
      }
    }, null)
  }
}

/** Item barcode labels (Code 128 of the item code) laid out as a 3 x 8 grid on A4 pages. */
object BarcodeLabels {
  fun write(out: OutputStream, items: List<ItemEntity>) {
    val doc = PdfDocument()
    val pageW = 595
    val pageH = 842
    val cols = 3
    val rows = 8
    val cellW = (pageW - 40) / cols
    val cellH = (pageH - 40) / rows
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 10f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; textAlign = Paint.Align.CENTER }
    val barPaint = Paint().apply { color = AndroidColor.BLACK }
    val perPage = cols * rows
    val pages = if (items.isEmpty()) 1 else (items.size + perPage - 1) / perPage
    for (p in 0 until pages) {
      val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, p + 1).create())
      val canvas: Canvas = page.canvas
      for (k in 0 until perPage) {
        val index = p * perPage + k
        if (index >= items.size) break
        val item = items[index]
        val left = 20f + (k % cols) * cellW
        val top = 20f + (k / cols) * cellH
        val cx = left + cellW / 2f
        var name = item.name
        while (name.length > 3 && namePaint.measureText(name) > cellW - 12f) name = name.dropLast(1)
        canvas.drawText(name, cx, top + 14f, namePaint)
        val matrix = runCatching {
          MultiFormatWriter().encode(item.code, BarcodeFormat.CODE_128, cellW - 24, cellH - 42)
        }.getOrNull()
        if (matrix != null) {
          val offsetX = left + 12f
          val barTop = top + 20f
          for (x in 0 until matrix.width) {
            if (matrix.get(x, 0)) {
              canvas.drawRect(offsetX + x, barTop, offsetX + x + 1f, barTop + (cellH - 42), barPaint)
            }
          }
        }
        canvas.drawText(item.code, cx, top + cellH - 6f, codePaint)
      }
      doc.finishPage(page)
    }
    try {
      doc.writeTo(out)
    } finally {
      doc.close()
    }
  }
}

class ExportActions(
  val pdf: (ReportTable) -> Unit,
  val excel: (ReportTable) -> Unit,
  val print: (ReportTable) -> Unit,
  val barcodes: (List<ItemEntity>) -> Unit
)

/** Registers the file pickers once per screen and returns ready-to-call export actions. */
@Composable
fun rememberExportActions(viewModel: AccountingViewModel): ExportActions {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val settings by viewModel.allSettings.collectAsState()
  val headerNote = settings.firstOrNull { it.settingKey == "print_header" }?.settingValue.orEmpty()
  var pendingTable by remember { mutableStateOf<ReportTable?>(null) }
  var pendingItems by remember { mutableStateOf<List<ItemEntity>>(emptyList()) }

  fun save(uri: android.net.Uri?, writer: (OutputStream) -> Unit, okMessage: String) {
    if (uri == null) return
    runCatching {
      val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IllegalStateException("تعذر فتح الملف للكتابة")
      stream.use(writer)
    }.onSuccess { viewModel.showMessage(okMessage) }
      .onFailure { viewModel.showMessage(it.message ?: "تعذر إنشاء الملف") }
  }

  val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
    pendingTable?.let { table -> save(uri, { PdfExporter.write(it, table) }, "تم حفظ ملف PDF") }
    pendingTable = null
  }
  val excelLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  ) { uri ->
    pendingTable?.let { table -> save(uri, { ExcelExporter.write(it, table) }, "تم حفظ ملف Excel") }
    pendingTable = null
  }
  val barcodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
    val list = pendingItems
    save(uri, { BarcodeLabels.write(it, list) }, "تم حفظ ملصقات الباركود")
    pendingItems = emptyList()
  }

  fun withNote(t: ReportTable): ReportTable =
    if (t.headerNote.isBlank() && headerNote.isNotBlank()) t.copy(headerNote = headerNote) else t

  return ExportActions(
    pdf = { table ->
      pendingTable = withNote(table)
      pdfLauncher.launch(table.title + ".pdf")
    },
    excel = { table ->
      pendingTable = withNote(table)
      excelLauncher.launch(table.title + ".xlsx")
    },
    print = { table ->
      val noted = withNote(table)
      val thermalOn = settings.boolSetting("printer_thermal_enabled", false)
      val address = settings.stringSetting(SettingKeys.PRINTER_ADDRESS)
      if (thermalOn && address.isNotBlank() && ThermalPrinter.hasPermission(context)) {
        scope.launch {
          runCatching {
            ThermalPrinter.print(
              context, address, noted,
              settings.boolSetting(SettingKeys.PRINTER_WIDTH_80, true),
              settings.boolSetting("printer_auto_cut", false)
            )
          }.onSuccess { viewModel.showMessage("تم إرسال المستند إلى الطابعة الحرارية") }
            .onFailure { viewModel.showMessage(it.message ?: "تعذر الاتصال بالطابعة الحرارية") }
        }
      } else {
        runCatching { PrintHelper.print(context, noted) }
          .onFailure { viewModel.showMessage("تعذر بدء الطباعة") }
      }
    },
    barcodes = { list ->
      if (list.isEmpty()) {
        viewModel.showMessage("لا توجد أصناف لطباعة باركودها")
      } else {
        pendingItems = list
        barcodeLauncher.launch("barcode_labels.pdf")
      }
    }
  )
}
