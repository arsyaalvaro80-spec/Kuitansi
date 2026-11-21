package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.BillTransactionEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.domain.IndonesianFormatters
import java.io.File
import java.io.FileOutputStream

object BookkeepingExportHelper {

    fun buildBookkeepingCsv(
        periodLabel: String,
        cashTransactions: List<BillTransactionEntity>,
        transferTransactions: List<BillTransactionEntity>,
        profile: IspProfileEntity
    ): String {
        val cashTotal = cashTransactions.sumOf { it.amountPaid }
        val transferTotal = transferTransactions.sumOf { it.amountPaid }
        val grandTotal = cashTotal + transferTotal
        val ordered = cashTransactions + transferTransactions

        return buildString {
            // UTF-8 BOM so Microsoft Excel reads Indonesian formatting cleanly
            append('\uFEFF')
            appendLine("REKAP PEMBUKUAN,${csvEscape(profile.businessName.ifBlank { "NetBill ISP" })}")
            appendLine("PERIODE,${csvEscape(periodLabel)}")
            appendLine()
            appendLine("No,No Struk,Tanggal Bayar,Periode Tagihan,Nama Pelanggan,No HP,Alamat,Paket,Kecepatan (Mbps),Metode Bayar,Kategori Pembukuan,Nominal Tunai,Nominal Transfer,Total Bayar,Catatan")

            ordered.forEachIndexed { idx, tx ->
                val method = PaymentMethod.fromString(tx.paymentMethod)
                val cashCol = if (method.isCash) tx.amountPaid else 0L
                val transferCol = if (method.isTransfer) tx.amountPaid else 0L
                val txPeriod = IndonesianFormatters.formatMonthYear(tx.billingMonth, tx.billingYear)
                val dateStr = IndonesianFormatters.formatShortDate(tx.paidAt)

                appendLine(
                    listOf(
                        (idx + 1).toString(),
                        csvEscape(tx.invoiceNumber),
                        csvEscape(dateStr),
                        csvEscape(txPeriod),
                        csvEscape(tx.customerNameSnapshot),
                        csvEscape(tx.customerPhoneSnapshot),
                        csvEscape(tx.customerAddressSnapshot),
                        csvEscape(tx.planNameSnapshot),
                        tx.planSpeedMbpsSnapshot.toString(),
                        csvEscape(method.label),
                        csvEscape(method.bookkeepingGroupLabel),
                        cashCol.toString(),
                        transferCol.toString(),
                        tx.amountPaid.toString(),
                        csvEscape(tx.notes)
                    ).joinToString(",")
                )
            }

            appendLine()
            appendLine(",,,,,,,,,,TOTAL KAS TUNAI (${cashTransactions.size} org),$cashTotal,0,$cashTotal,")
            appendLine(",,,,,,,,,,TOTAL TRANSFER (${transferTransactions.size} org),0,$transferTotal,$transferTotal,")
            appendLine(",,,,,,,,,,GRAND TOTAL (${ordered.size} org),$cashTotal,$transferTotal,$grandTotal,")
        }
    }

    fun shareBookkeepingCsv(
        context: Context,
        periodLabel: String,
        cashTransactions: List<BillTransactionEntity>,
        transferTransactions: List<BillTransactionEntity>,
        profile: IspProfileEntity
    ): Result<String> {
        return runCatching {
            val csvContent = buildBookkeepingCsv(
                periodLabel = periodLabel,
                cashTransactions = cashTransactions,
                transferTransactions = transferTransactions,
                profile = profile
            )
            val exportsDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }
            val safePeriod = periodLabel.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val fileName = "Pembukuan_${safePeriod}.csv"
            val file = File(exportsDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Pembukuan ${profile.businessName} - $periodLabel")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Export CSV Pembukuan").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            fileName
        }
    }

    private fun csvEscape(raw: String): String {
        val cleaned = raw.replace("\r\n", " ").replace("\n", " ")
        return if (cleaned.contains(',') || cleaned.contains('"') || cleaned.contains(';')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }
}
