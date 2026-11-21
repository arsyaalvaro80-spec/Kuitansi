package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.data.local.BillTransactionEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.domain.CustomerBillItem
import com.example.domain.IndonesianFormatters
import java.net.URLEncoder

object WhatsAppHelper {

    fun sendBillReminder(
        context: Context,
        item: CustomerBillItem,
        profile: IspProfileEntity
    ): Result<Unit> {
        return runCatching {
            val normalizedPhone = IndonesianFormatters.normalizeIndonesianPhone(item.customer.phone)
            val periodLabel = IndonesianFormatters.formatMonthYear(item.billingMonth, item.billingYear)
            val planText = item.plan?.let { "${it.name} (${it.speedMbps} Mbps)" } ?: "Paket Internet"
            val amountText = IndonesianFormatters.formatRupiah(item.monthlyAmount)

            val message = buildString {
                appendLine("Yth. Bapak/Ibu *${item.customer.name}*,")
                appendLine()
                appendLine("Berikut informasi tagihan internet *${profile.businessName}*:")
                appendLine("• Periode: $periodLabel")
                appendLine("• Paket: $planText")
                appendLine("• Jatuh Tempo: ${item.dueDateLabel}")
                appendLine("• Nominal: *$amountText*")
                appendLine("• Status: ${item.status.label}")
                if (profile.bankAccountInfo.isNotBlank()) {
                    appendLine()
                    appendLine("*Info Rekening Pembayaran:*")
                    appendLine(profile.bankAccountInfo.trim())
                }
                appendLine()
                append("Mohon konfirmasi setelah melakukan pembayaran. Terima kasih.")
            }

            val encoded = URLEncoder.encode(message, "UTF-8")
            val waUrl = if (normalizedPhone.isNotBlank()) {
                "https://wa.me/$normalizedPhone?text=$encoded"
            } else {
                "https://wa.me/?text=$encoded"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val waPackage = findInstalledWhatsAppPackage(context)
            if (waPackage != null) {
                intent.setPackage(waPackage)
            }

            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to chooser / plain text share if WhatsApp is not installed
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Kirim Pengingat Tagihan").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun shareReceiptImageToWhatsApp(
        context: Context,
        transaction: BillTransactionEntity,
        profile: IspProfileEntity
    ): Result<Unit> {
        return runCatching {
            val bitmap = ReceiptBitmapGenerator.generateReceiptBitmap(transaction, profile)
            val uri = ReceiptBitmapGenerator.saveReceiptToCacheUri(context, bitmap, transaction.invoiceNumber)
            val normalizedPhone = IndonesianFormatters.normalizeIndonesianPhone(transaction.customerPhoneSnapshot)
            val periodLabel = IndonesianFormatters.formatMonthYear(transaction.billingMonth, transaction.billingYear)
            val caption = buildString {
                appendLine("*KUITANSI PEMBAYARAN LUNAS*")
                appendLine("${profile.businessName}")
                appendLine("No. Struk: ${transaction.invoiceNumber}")
                appendLine("Pelanggan: ${transaction.customerNameSnapshot}")
                appendLine("Periode: $periodLabel")
                appendLine("Total: ${IndonesianFormatters.formatRupiah(transaction.amountPaid)} (${PaymentMethod.fromString(transaction.paymentMethod).label})")
                append("Terima kasih atas pembayarannya.")
            }

            val waPackage = findInstalledWhatsAppPackage(context)
            if (waPackage != null) {
                val waIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    setPackage(waPackage)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    if (normalizedPhone.isNotBlank()) {
                        putExtra("jid", "$normalizedPhone@s.whatsapp.net")
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(waIntent)
                    return@runCatching
                } catch (_: Exception) {
                    // Fall through to generic chooser
                }
            }

            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(fallbackIntent, "Kirim Foto Struk ke WhatsApp").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun shareBookkeepingReport(
        context: Context,
        periodLabel: String,
        cashTransactions: List<BillTransactionEntity>,
        transferTransactions: List<BillTransactionEntity>,
        profile: IspProfileEntity
    ): Result<Unit> {
        return runCatching {
            val cashTotal = cashTransactions.sumOf { it.amountPaid }
            val transferTotal = transferTransactions.sumOf { it.amountPaid }
            val grandTotal = cashTotal + transferTotal
            val totalCount = cashTransactions.size + transferTransactions.size

            val reportText = buildString {
                appendLine("*REKAP PEMBUKUAN ${profile.businessName.ifBlank { "NetBill ISP" }.uppercase()}*")
                appendLine("Periode: $periodLabel")
                appendLine("--------------------------------")
                appendLine("Total Pemasukan : *${IndonesianFormatters.formatRupiah(grandTotal)}* ($totalCount org)")
                appendLine("• Kas Tunai     : ${IndonesianFormatters.formatRupiah(cashTotal)} (${cashTransactions.size} org)")
                appendLine("• Transfer      : ${IndonesianFormatters.formatRupiah(transferTotal)} (${transferTransactions.size} org)")
                appendLine()
                appendLine("*1. DAFTAR BAYAR TUNAI (${cashTransactions.size} orang)*")
                if (cashTransactions.isEmpty()) {
                    appendLine("- Belum ada pembayaran tunai")
                } else {
                    cashTransactions.forEachIndexed { index, tx ->
                        val dateStr = IndonesianFormatters.formatShortDate(tx.paidAt)
                        appendLine("${index + 1}. ${tx.customerNameSnapshot} — ${IndonesianFormatters.formatRupiahPlain(tx.amountPaid)} ($dateStr)")
                    }
                }
                appendLine("Subtotal Tunai: *${IndonesianFormatters.formatRupiah(cashTotal)}*")
                appendLine()
                appendLine("*2. DAFTAR BAYAR TRANSFER (${transferTransactions.size} orang)*")
                if (transferTransactions.isEmpty()) {
                    appendLine("- Belum ada pembayaran transfer")
                } else {
                    transferTransactions.forEachIndexed { index, tx ->
                        val dateStr = IndonesianFormatters.formatShortDate(tx.paidAt)
                        val methodLabel = PaymentMethod.fromString(tx.paymentMethod).label
                        appendLine("${index + 1}. ${tx.customerNameSnapshot} — ${IndonesianFormatters.formatRupiahPlain(tx.amountPaid)} [$methodLabel] ($dateStr)")
                    }
                }
                append("Subtotal Transfer: *${IndonesianFormatters.formatRupiah(transferTotal)}*")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Rekap Pembukuan ${profile.businessName} - $periodLabel")
                putExtra(Intent.EXTRA_TEXT, reportText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Bagikan Rekap Pembukuan").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun findInstalledWhatsAppPackage(context: Context): String? {
        val candidates = listOf("com.whatsapp", "com.whatsapp.w4b")
        return candidates.firstOrNull { pkg -> isPackageInstalled(context, pkg) }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
