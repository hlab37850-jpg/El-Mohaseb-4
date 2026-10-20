package com.example.ui.util

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** Renders a [ReportTable] to an A4 PDF (right-to-left layout) using the platform PdfDocument API. */
object PdfExporter {
  private const val PAGE_W = 595
  private const val PAGE_H = 842
  private const val MARGIN = 36f
  private const val ROW_H = 22f

  fun write(out: OutputStream, table: ReportTable) {
    val doc = PdfDocument()
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 16f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
    }
    val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = 11f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
    }
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f; textAlign = Paint.Align.RIGHT }
    val linePaint = Paint().apply { strokeWidth = 0.6f }

    val cols = table.headers.size.coerceAtLeast(1)
    val usable = PAGE_W - 2 * MARGIN
    // The first (label) column is wider than numeric columns.
    val weights = List(cols) { if (it == 0 || (cols > 2 && it == 1 && table.headers[1].length > 6)) 1.8f else 1f }
    val totalWeight = weights.sum()
    val widths = weights.map { usable * it / totalWeight }

    fun fit(text: String, paint: Paint, width: Float): String {
      if (paint.measureText(text) <= width - 6f) return text
      var t = text
      while (t.isNotEmpty() && paint.measureText("$t…") > width - 6f) t = t.dropLast(1)
      return "$t…"
    }

    var pageNumber = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
    var canvas = page.canvas
    var y = MARGIN + 16f
    canvas.drawText(table.title, PAGE_W - MARGIN, y, titlePaint)
    y += 20f
    if (table.headerNote.isNotBlank()) {
      canvas.drawText(table.headerNote, PAGE_W - MARGIN, y, cellPaint)
      y += 18f
    }
    y += 6f

    fun drawRow(cells: List<String>, paint: Paint) {
      var xRight = PAGE_W - MARGIN
      for (i in 0 until cols) {
        val text = fit(cells.getOrElse(i) { "" }, paint, widths[i])
        canvas.drawText(text, xRight - 4f, y, paint)
        xRight -= widths[i]
      }
      canvas.drawLine(MARGIN, y + 6f, PAGE_W - MARGIN, y + 6f, linePaint)
      y += ROW_H
    }

    fun newPageIfNeeded() {
      if (y > PAGE_H - MARGIN) {
        doc.finishPage(page)
        pageNumber += 1
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
        canvas = page.canvas
        y = MARGIN + 16f
        drawRow(table.headers, headPaint)
      }
    }

    drawRow(table.headers, headPaint)
    for (row in table.rows) {
      newPageIfNeeded()
      drawRow(row, cellPaint)
    }
    table.totals?.let {
      newPageIfNeeded()
      drawRow(it, headPaint)
    }
    doc.finishPage(page)
    try {
      doc.writeTo(out)
    } finally {
      doc.close()
    }
  }
}
