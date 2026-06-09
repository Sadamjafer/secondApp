package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportHelper {

    fun generateAndSharePdf(
        context: Context,
        title: String,
        subTitle: String,
        incomes: List<ReportTransactionItem>,
        expenses: List<ReportTransactionItem>
    ) {
        try {
            val pdfDocument = PdfDocument()
            // A4 size: 595 x 842 dots
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint()
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
            }

            var currentY = 40f

            // --- Draw Header ---
            // Draw a stylish dark green/blue banner at the top
            paint.color = Color.parseColor("#1B5E20") // Rich dark green
            canvas.drawRect(RectF(30f, currentY, 565f, currentY + 70f), paint)

            // Header Title
            textPaint.apply {
                color = Color.WHITE
                textSize = 20f
                isFakeBoldText = true
            }
            drawArabicText(canvas, title, 545f, currentY + 25f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            // Header Subtitle
            textPaint.apply {
                color = Color.parseColor("#E8F5E9")
                textSize = 11f
                isFakeBoldText = false
            }
            drawArabicText(canvas, subTitle, 545f, currentY + 52f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            currentY += 90f

            // --- Summary Cards Block ---
            val totalIncomes = incomes.sumOf { it.amount }
            val totalExpenses = expenses.sumOf { it.amount }
            val netProfit = totalIncomes - totalExpenses

            paint.color = Color.parseColor("#F5F5F5") // Light neutral container
            canvas.drawRoundRect(RectF(30f, currentY, 565f, currentY + 75f), 10f, 10f, paint)

            paint.color = Color.parseColor("#4CAF50") // Green for Incomes
            canvas.drawRoundRect(RectF(40f, currentY + 10f, 195f, currentY + 65f), 6f, 6f, paint)
            textPaint.apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
            drawArabicText(canvas, "إجمالي الإيرادات", 185f, currentY + 20f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "${formatAmount(totalIncomes)} ج.س", 185f, currentY + 40f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            paint.color = Color.parseColor("#E53935") // Red for Expenses
            canvas.drawRoundRect(RectF(210f, currentY + 10f, 365f, currentY + 65f), 6f, 6f, paint)
            textPaint.apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
            drawArabicText(canvas, "إجمالي المصروفات", 355f, currentY + 20f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "${formatAmount(totalExpenses)} ج.س", 355f, currentY + 40f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            paint.color = Color.parseColor("#1565C0") // Blue for Profit
            canvas.drawRoundRect(RectF(380f, currentY + 10f, 535f, currentY + 65f), 6f, 6f, paint)
            textPaint.apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
            drawArabicText(canvas, "صافي الأرباح/الفائض", 525f, currentY + 20f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "${formatAmount(netProfit)} ج.س", 525f, currentY + 40f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            currentY += 95f

            // --- Incomes Section ---
            textPaint.apply { color = Color.parseColor("#1B5E20"); textSize = 14f; isFakeBoldText = true }
            drawArabicText(canvas, "📥 جدول الإيرادات والتوريدات:", 545f, currentY, textPaint, Layout.Alignment.ALIGN_NORMAL)
            currentY += 15f

            // Table Headings
            paint.color = Color.parseColor("#C8E6C9") // Soft Green for Table Header
            canvas.drawRect(RectF(30f, currentY, 565f, currentY + 24f), paint)
            textPaint.apply { color = Color.parseColor("#1B5E20"); textSize = 10f; isFakeBoldText = true }
            drawArabicText(canvas, "البيان / البند", 545f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "الفئة", 340f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "التاريخ / الوقت", 220f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "المبلغ (ج.س)", 130f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            currentY += 24f

            // Render Income Items
            textPaint.apply { color = Color.BLACK; textSize = 9f; isFakeBoldText = false }
            if (incomes.isEmpty()) {
                drawArabicText(canvas, "لا توجد إيرادات مسجلة", 545f, currentY + 10f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                currentY += 25f
            } else {
                incomes.forEachIndexed { i, item ->
                    // Alternate background color
                    if (i % 2 == 1) {
                        paint.color = Color.parseColor("#F9F9F9")
                        canvas.drawRect(RectF(30f, currentY, 565f, currentY + 22f), paint)
                    }
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.date))
                    drawArabicText(canvas, item.title, 545f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    drawArabicText(canvas, item.category, 340f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    drawArabicText(canvas, dateFormatted, 220f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    drawArabicText(canvas, formatAmount(item.amount), 130f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    
                    canvas.drawLine(30f, currentY + 22f, 565f, currentY + 22f, Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 0.5f })
                    currentY += 22f

                    // Safety check to start a new page
                    if (currentY > 780f) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                    }
                }
            }

            currentY += 25f
            if (currentY > 750f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
            }

            // --- Expenses Section ---
            textPaint.apply { color = Color.parseColor("#B71C1C"); textSize = 14f; isFakeBoldText = true }
            drawArabicText(canvas, "📤 جدول المصروفات:", 545f, currentY, textPaint, Layout.Alignment.ALIGN_NORMAL)
            currentY += 15f

            // Table Headings
            paint.color = Color.parseColor("#FFCDD2") // Soft Red for Table Header
            canvas.drawRect(RectF(30f, currentY, 565f, currentY + 24f), paint)
            textPaint.apply { color = Color.parseColor("#B71C1C"); textSize = 10f; isFakeBoldText = true }
            drawArabicText(canvas, "البيان / البند", 545f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "الفئة", 340f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "التاريخ / الوقت", 220f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)
            drawArabicText(canvas, "المبلغ (ج.س)", 130f, currentY + 6f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            currentY += 24f

            // Render Expense Items
            textPaint.apply { color = Color.BLACK; textSize = 9f; isFakeBoldText = false }
            if (expenses.isEmpty()) {
                drawArabicText(canvas, "لا توجد مصروفات مسجلة", 545f, currentY + 10f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                currentY += 25f
            } else {
                expenses.forEachIndexed { i, item ->
                    if (i % 2 == 1) {
                        paint.color = Color.parseColor("#F9F9F9")
                        canvas.drawRect(RectF(30f, currentY, 565f, currentY + 22f), paint)
                    }
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.date))
                    drawArabicText(canvas, item.title, 545f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    drawArabicText(canvas, item.category, 340f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    drawArabicText(canvas, dateFormatted, 220f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    drawArabicText(canvas, formatAmount(item.amount), 130f, currentY + 5f, textPaint, Layout.Alignment.ALIGN_NORMAL)
                    
                    canvas.drawLine(30f, currentY + 22f, 565f, currentY + 22f, Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 0.5f })
                    currentY += 22f

                    if (currentY > 780f) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                    }
                }
            }

            // Footer info
            currentY += 30f
            if (currentY > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
            }
            textPaint.apply { color = Color.GRAY; textSize = 8f; isFakeBoldText = false }
            drawArabicText(canvas, "تنبيه: تم إنشاء هذا المستند والتقرير المالي آلياً بواسطة تطبيق إدارة المالية السحابي الخزينة.", 545f, 810f, textPaint, Layout.Alignment.ALIGN_NORMAL)

            pdfDocument.finishPage(page)

            // Save PDF to cache dir and open standard share sheet
            val cacheFile = File(context.cacheDir, "Financial_Report_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(cacheFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            // Sharing
            val fileUri = FileProvider.getUriForFile(context, "com.example.fileprovider", cacheFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "تصدير ومشاركة مستند PDF عبر:"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل إنشاء تقرير PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawArabicText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textPaint: TextPaint,
        alignment: Layout.Alignment
    ) {
        canvas.save()
        // Transferred coordinate
        canvas.translate(x, y)
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 450)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.0f)
            .build()
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun formatAmount(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val str = absAmount.toLong().toString()
        val dec = String.format(Locale.getDefault(), "%.1f", absAmount - absAmount.toLong()).substring(1)
        val sb = java.lang.StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            sb.append(str[i])
            count++
            if (count % 3 == 0 && i > 0) {
                sb.append(",")
            }
        }
        val formattedAbs = sb.reverse().toString()
        return if (isNegative) "-$formattedAbs$dec" else "$formattedAbs$dec"
    }
}
