package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.local.BillTransactionEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.domain.IndonesianFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

data class PairedPrinterDevice(
    val name: String,
    val address: String
)

object BluetoothThermalPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            emptyArray()
        }
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): Result<List<PairedPrinterDevice>> {
        if (!hasBluetoothPermissions(context)) {
            return Result.failure(SecurityException("Izin Bluetooth belum diberikan."))
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = manager?.adapter
        if (adapter == null) {
            return Result.failure(IllegalStateException("Perangkat ini tidak memiliki modul Bluetooth."))
        }
        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth sedang mati. Silakan aktifkan Bluetooth di pengaturan HP."))
        }
        val devices = adapter.bondedDevices
            ?.map { device ->
                PairedPrinterDevice(
                    name = device.name?.takeIf { it.isNotBlank() } ?: "Printer Bluetooth",
                    address = device.address
                )
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        return Result.success(devices)
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        context: Context,
        deviceAddress: String,
        transaction: BillTransactionEntity,
        profile: IspProfileEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasBluetoothPermissions(context)) {
                throw SecurityException("Izin Bluetooth diperlukan untuk mencetak struk.")
            }
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
                ?: throw IllegalStateException("Bluetooth tidak tersedia pada perangkat ini.")
            if (!adapter.isEnabled) {
                throw IllegalStateException("Aktifkan Bluetooth terlebih dahulu.")
            }

            val device = adapter.getRemoteDevice(deviceAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                adapter.cancelDiscovery()
                socket.connect()
                val payload = buildEscPosReceipt(transaction, profile)
                socket.outputStream.use { out ->
                    out.write(payload)
                    out.flush()
                }
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    /**
     * Builds ESC/POS formatted byte stream supporting 58mm (32 chars) and 80mm (48 chars) paper widths.
     */
    fun buildEscPosReceipt(
        transaction: BillTransactionEntity,
        profile: IspProfileEntity
    ): ByteArray {
        val lineChars = if (profile.printerPaperWidthMm >= 80) 48 else 32
        val divider = "-".repeat(lineChars)
        val doubleDivider = "=".repeat(lineChars)
        val out = ByteArrayOutputStream()

        fun writeCmd(vararg bytes: Int) {
            bytes.forEach { out.write(it) }
        }

        fun writeLine(text: String) {
            out.write("$text\n".toByteArray(Charsets.UTF_8))
        }

        fun alignLeft() = writeCmd(0x1B, 0x61, 0x00)
        fun alignCenter() = writeCmd(0x1B, 0x61, 0x01)
        fun boldOn() = writeCmd(0x1B, 0x45, 0x01)
        fun boldOff() = writeCmd(0x1B, 0x45, 0x00)
        fun normalSize() = writeCmd(0x1D, 0x21, 0x00)
        fun doubleHeightSize() = writeCmd(0x1D, 0x21, 0x01)

        // ESC @ Initialize printer
        writeCmd(0x1B, 0x40)

        // Centered ISP Header
        alignCenter()
        boldOn()
        doubleHeightSize()
        writeLine(profile.businessName.ifBlank { "NETBILL ISP" }.uppercase())
        normalSize()
        boldOff()

        if (profile.addressInfo.isNotBlank()) {
            wrapPlainText(profile.addressInfo, lineChars).forEach { writeLine(it) }
        }
        if (profile.managerPhone.isNotBlank()) {
            writeLine("WA/CS: ${profile.managerPhone}")
        }
        writeLine(doubleDivider)
        boldOn()
        writeLine("KUITANSI PEMBAYARAN")
        boldOff()
        writeLine(divider)

        // Left-Right detail rows
        alignLeft()
        writeLine(formatLeftRight("No. Struk", transaction.invoiceNumber, lineChars))
        writeLine(formatLeftRight("Tanggal", IndonesianFormatters.formatShortDate(transaction.paidAt), lineChars))
        writeLine(
            formatLeftRight(
                "Periode",
                IndonesianFormatters.formatShortMonthYear(transaction.billingMonth, transaction.billingYear),
                lineChars
            )
        )
        writeLine(divider)
        writeLine(formatLeftRight("Pelanggan", transaction.customerNameSnapshot, lineChars))
        val planDesc = if (transaction.planSpeedMbpsSnapshot > 0) {
            "${transaction.planNameSnapshot} ${transaction.planSpeedMbpsSnapshot}M"
        } else {
            transaction.planNameSnapshot
        }
        writeLine(formatLeftRight("Paket", planDesc, lineChars))
        writeLine(
            formatLeftRight(
                "Metode",
                PaymentMethod.fromString(transaction.paymentMethod).label,
                lineChars
            )
        )
        if (transaction.notes.isNotBlank()) {
            writeLine("Catatan: ${transaction.notes}")
        }
        writeLine(divider)

        boldOn()
        doubleHeightSize()
        writeLine(
            formatLeftRight(
                "TOTAL",
                IndonesianFormatters.formatRupiahPlain(transaction.amountPaid),
                lineChars
            )
        )
        normalSize()
        boldOff()
        writeLine(doubleDivider)

        alignCenter()
        boldOn()
        writeLine("[ STATUS: LUNAS ]")
        boldOff()
        writeLine(divider)

        profile.receiptFooter
            .ifBlank { "Terima kasih atas pembayaran Anda." }
            .split("\n")
            .flatMap { wrapPlainText(it.trim(), lineChars) }
            .forEach { writeLine(it) }

        // Feed 4 lines and partial cut (if supported)
        writeLine("")
        writeLine("")
        writeLine("")
        writeCmd(0x1D, 0x56, 0x42, 0x00)
        return out.toByteArray()
    }

    fun formatPreviewText(transaction: BillTransactionEntity, profile: IspProfileEntity): String {
        val lineChars = if (profile.printerPaperWidthMm >= 80) 48 else 32
        val divider = "-".repeat(lineChars)
        return buildString {
            appendLine(centerText(profile.businessName.ifBlank { "NETBILL ISP" }.uppercase(), lineChars))
            if (profile.addressInfo.isNotBlank()) {
                wrapPlainText(profile.addressInfo, lineChars).forEach { appendLine(centerText(it, lineChars)) }
            }
            appendLine(divider)
            appendLine(formatLeftRight("No", transaction.invoiceNumber, lineChars))
            appendLine(formatLeftRight("Tgl", IndonesianFormatters.formatShortDate(transaction.paidAt), lineChars))
            appendLine(formatLeftRight("Nama", transaction.customerNameSnapshot, lineChars))
            appendLine(formatLeftRight("Paket", "${transaction.planNameSnapshot} (${transaction.planSpeedMbpsSnapshot} Mbps)", lineChars))
            appendLine(formatLeftRight("Metode", PaymentMethod.fromString(transaction.paymentMethod).label, lineChars))
            appendLine(divider)
            appendLine(formatLeftRight("TOTAL", IndonesianFormatters.formatRupiahPlain(transaction.amountPaid), lineChars))
            appendLine(centerText("*** LUNAS ***", lineChars))
        }
    }

    private fun formatLeftRight(left: String, right: String, width: Int): String {
        val cleanLeft = left.trim()
        val cleanRight = right.trim()
        val spaceCount = width - cleanLeft.length - cleanRight.length
        return if (spaceCount >= 1) {
            cleanLeft + " ".repeat(spaceCount) + cleanRight
        } else {
            val maxRight = (width - cleanLeft.length - 1).coerceAtLeast(6)
            val truncatedRight = if (cleanRight.length > maxRight) {
                cleanRight.take(maxRight - 1) + "."
            } else {
                cleanRight
            }
            val pad = (width - cleanLeft.length - truncatedRight.length).coerceAtLeast(1)
            cleanLeft + " ".repeat(pad) + truncatedRight
        }
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val leftPad = (width - text.length) / 2
        return " ".repeat(leftPad) + text
    }

    private fun wrapPlainText(text: String, width: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val next = if (current.isEmpty()) word else "$current $word"
            if (next.length <= width) {
                current = next
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word.take(width)
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }
}
