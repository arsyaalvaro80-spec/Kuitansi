package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.BillTransactionEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.domain.IndonesianFormatters
import java.io.File
import java.io.FileOutputStream

object ReceiptBitmapGenerator {

    /**
     * Renders a crisp high-resolution thermal cashier paper receipt with serrated zigzag top/bottom edges,
     * official invoice details, dashed dividers, and a rotated emerald "LUNAS" rubber stamp.
     */
    fun generateReceiptBitmap(
        transaction: BillTransactionEntity,
        profile: IspProfileEntity
    ): Bitmap {
        val canvasWidth = 760
        val paperMarginX = 36f
        val paperMarginY = 36f
        val contentPaddingX = 38f
        val paperWidth = canvasWidth - (paperMarginX * 2)
        val contentLeft = paperMarginX + contentPaddingX
        val contentRight = canvasWidth - paperMarginX - contentPaddingX
        val contentWidth = contentRight - contentLeft
        val toothHeight = 14f

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 34f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val titleBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F4C81")
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F4C81")
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
        }

        val addressLines = wrapText(profile.addressInfo.ifBlank { "Layanan Internet Lokal" }, subHeaderPaint, contentWidth)
        val footerLines = profile.receiptFooter
            .ifBlank { "Terima kasih atas pembayaran Anda." }
            .split("\n")
            .flatMap { wrapText(it.trim(), footerPaint, contentWidth) }

        val customerAddressLines = wrapText(
            transaction.customerAddressSnapshot.ifBlank { "-" },
            valuePaint,
            contentWidth * 0.62f
        )
        val noteLines = if (transaction.notes.isNotBlank()) {
            wrapText(transaction.notes, valuePaint, contentWidth * 0.62f)
        } else {
            emptyList()
        }

        // Compute dynamic height
        var estimatedY = paperMarginY + toothHeight + 48f
        estimatedY += 40f // business name
        estimatedY += addressLines.size * 28f
        if (profile.managerPhone.isNotBlank()) estimatedY += 28f
        estimatedY += 24f // divider
        estimatedY += 42f // KUITANSI PEMBAYARAN INTERNET
        estimatedY += 24f // divider

        // Rows: Invoice, Tanggal, Periode, Pelanggan, HP, Alamat, Paket, Metode, Catatan
        val standardRows = 7
        estimatedY += standardRows * 38f
        estimatedY += maxOf(1, customerAddressLines.size) * 32f + 8f
        if (noteLines.isNotEmpty()) {
            estimatedY += noteLines.size * 32f + 10f
        }
        estimatedY += 24f // divider
        estimatedY += 56f // total box
        estimatedY += 28f // divider
        estimatedY += 116f // LUNAS stamp area
        estimatedY += 24f // divider
        estimatedY += footerLines.size * 28f + 40f
        estimatedY += toothHeight + paperMarginY

        val canvasHeight = estimatedY.toInt().coerceAtLeast(920)
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer soft slate backdrop so serrated paper edges pop clearly
        canvas.drawColor(Color.parseColor("#E6ECF4"))

        // Draw subtle shadow behind receipt paper
        val paperTop = paperMarginY
        val paperBottom = canvasHeight - paperMarginY
        val paperPath = buildSerratedPaperPath(
            left = paperMarginX,
            top = paperTop,
            right = canvasWidth - paperMarginX,
            bottom = paperBottom,
            toothWidth = 22f,
            toothHeight = toothHeight
        )
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#240F172A")
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.translate(0f, 6f)
        canvas.drawPath(paperPath, shadowPaint)
        canvas.restore()

        // Draw warm white thermal receipt paper
        val paperFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFDF9")
            style = Paint.Style.FILL
        }
        canvas.drawPath(paperPath, paperFillPaint)

        val paperBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawPath(paperPath, paperBorderPaint)

        val centerX = canvasWidth / 2f
        var y = paperTop + toothHeight + 54f

        // ISP Header
        canvas.drawText(
            profile.businessName.ifBlank { "NetBill ISP" }.uppercase(),
            centerX,
            y,
            headerPaint
        )
        y += 32f

        for (line in addressLines) {
            canvas.drawText(line, centerX, y, subHeaderPaint)
            y += 26f
        }
        if (profile.managerPhone.isNotBlank()) {
            canvas.drawText("CS / WA: ${profile.managerPhone}", centerX, y, subHeaderPaint)
            y += 28f
        }

        y += 10f
        canvas.drawLine(contentLeft, y, contentRight, y, dashedLinePaint)
        y += 36f

        canvas.drawText("STRUK PEMBAYARAN INTERNET", centerX, y, titleBandPaint)
        y += 22f
        canvas.drawLine(contentLeft, y, contentRight, y, dashedLinePaint)
        y += 40f

        fun drawKeyValueRow(label: String, value: String) {
            canvas.drawText(label, contentLeft, y, labelPaint)
            val maxValueWidth = contentWidth * 0.62f
            val clippedValue = ellipsize(value, valuePaint, maxValueWidth)
            canvas.drawText(clippedValue, contentRight, y, valuePaint)
            y += 38f
        }

        drawKeyValueRow("No. Struk", transaction.invoiceNumber)
        drawKeyValueRow("Tanggal", IndonesianFormatters.formatShortDate(transaction.paidAt))
        drawKeyValueRow(
            "Periode",
            IndonesianFormatters.formatMonthYear(transaction.billingMonth, transaction.billingYear)
        )
        drawKeyValueRow("Pelanggan", transaction.customerNameSnapshot)
        drawKeyValueRow("No. HP/WA", transaction.customerPhoneSnapshot.ifBlank { "-" })

        // Multiline address
        canvas.drawText("Alamat", contentLeft, y, labelPaint)
        for ((idx, addrLine) in customerAddressLines.withIndex()) {
            canvas.drawText(addrLine, contentRight, y + (idx * 30f), valuePaint)
        }
        y += maxOf(1, customerAddressLines.size) * 30f + 12f

        val planLabel = if (transaction.planSpeedMbpsSnapshot > 0) {
            "${transaction.planNameSnapshot} (${transaction.planSpeedMbpsSnapshot} Mbps)"
        } else {
            transaction.planNameSnapshot
        }
        drawKeyValueRow("Paket", planLabel)
        drawKeyValueRow("Metode", PaymentMethod.fromString(transaction.paymentMethod).label)

        if (noteLines.isNotEmpty()) {
            canvas.drawText("Catatan", contentLeft, y, labelPaint)
            for ((idx, nLine) in noteLines.withIndex()) {
                canvas.drawText(nLine, contentRight, y + (idx * 30f), valuePaint)
            }
            y += noteLines.size * 30f + 10f
        }

        y += 6f
        canvas.drawLine(contentLeft, y, contentRight, y, dashedLinePaint)
        y += 46f

        // Total highlighted band
        val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EEF6FF")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(contentLeft - 12f, y - 36f, contentRight + 12f, y + 18f),
            12f,
            12f,
            totalBgPaint
        )
        canvas.drawText("TOTAL BAYAR", contentLeft, y, totalLabelPaint)
        canvas.drawText(
            IndonesianFormatters.formatRupiah(transaction.amountPaid),
            contentRight,
            y + 2f,
            totalValuePaint
        )
        y += 38f

        canvas.drawLine(contentLeft, y, contentRight, y, dashedLinePaint)
        y += 66f

        // Rotated Emerald "LUNAS" Rubber Stamp
        canvas.save()
        canvas.rotate(-6f, centerX, y)
        val stampColor = Color.parseColor("#15803D")
        val stampFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1816A34A")
            style = Paint.Style.FILL
        }
        val stampOuterStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val stampInnerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            textSize = 44f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.2f
            textAlign = Paint.Align.CENTER
        }

        val outerRect = RectF(centerX - 145f, y - 40f, centerX + 145f, y + 36f)
        val innerRect = RectF(centerX - 137f, y - 32f, centerX + 137f, y + 28f)
        canvas.drawRoundRect(outerRect, 12f, 12f, stampFill)
        canvas.drawRoundRect(outerRect, 12f, 12f, stampOuterStroke)
        canvas.drawRoundRect(innerRect, 8f, 8f, stampInnerStroke)
        canvas.drawText("★ LUNAS ★", centerX, y + 14f, stampTextPaint)
        canvas.restore()

        y += 64f
        canvas.drawLine(contentLeft, y, contentRight, y, dashedLinePaint)
        y += 34f

        for (fLine in footerLines) {
            canvas.drawText(fLine, centerX, y, footerPaint)
            y += 28f
        }

        return bitmap
    }

    fun saveReceiptToCacheUri(context: Context, bitmap: Bitmap, invoiceNumber: String): Uri {
        val receiptsDir = File(context.cacheDir, "receipts").apply {
            if (!exists()) mkdirs()
        }
        val safeName = invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(receiptsDir, "Struk_${safeName}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun saveReceiptToGallery(context: Context, bitmap: Bitmap, invoiceNumber: String): Result<String> {
        return runCatching {
            val safeName = invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val fileName = "NetBill_${safeName}_${System.currentTimeMillis() % 10000}.png"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NetBillISP")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Gagal membuat berkas gambar di galeri")

            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    error("Gagal menyimpan gambar struk")
                }
            } ?: error("Tidak dapat membuka aliran penyimpanan galeri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            fileName
        }
    }

    private fun buildSerratedPaperPath(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        toothWidth: Float,
        toothHeight: Float
    ): Path {
        val path = Path()
        path.moveTo(left, top + toothHeight)
        var x = left
        var up = true
        while (x < right) {
            val nextX = (x + toothWidth / 2f).coerceAtMost(right)
            val nextY = if (up) top else top + toothHeight
            path.lineTo(nextX, nextY)
            x = nextX
            up = !up
        }
        path.lineTo(right, top + toothHeight)
        path.lineTo(right, bottom - toothHeight)

        x = right
        var down = true
        while (x > left) {
            val nextX = (x - toothWidth / 2f).coerceAtLeast(left)
            val nextY = if (down) bottom else bottom - toothHeight
            path.lineTo(nextX, nextY)
            x = nextX
            down = !down
        }
        path.lineTo(left, bottom - toothHeight)
        path.close()
        return path
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }
}
