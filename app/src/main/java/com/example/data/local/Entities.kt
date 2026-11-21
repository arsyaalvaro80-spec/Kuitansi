package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ThemeMode(val label: String) {
    LIGHT("Terang"),
    DARK("Gelap"),
    SYSTEM("Otomatis")
}

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    TRANSFER_BANK("Transfer Bank"),
    QRIS("QRIS");

    val isCash: Boolean
        get() = this == TUNAI

    val isTransfer: Boolean
        get() = this == TRANSFER_BANK || this == QRIS

    val bookkeepingGroupLabel: String
        get() = if (isCash) "Kas Tunai" else "Rekening Transfer"

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { it.name == value } ?: TUNAI
        }
    }
}

@Entity(tableName = "internet_plans")
data class InternetPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val speedMbps: Int,
    val price: Long,
    val description: String = "Unlimited tanpa FUP",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [Index("planId")]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String,
    val pppoeOrIp: String = "",
    val planId: Long,
    val dueDay: Int = 10,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bill_transactions",
    indices = [
        Index("customerId"),
        Index(value = ["billingYear", "billingMonth"])
    ]
)
data class BillTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long,
    val customerNameSnapshot: String,
    val customerPhoneSnapshot: String,
    val customerAddressSnapshot: String,
    val customerPppoeSnapshot: String = "",
    val planNameSnapshot: String,
    val planSpeedMbpsSnapshot: Int,
    val billingMonth: Int,
    val billingYear: Int,
    val amountPaid: Long,
    val paymentMethod: String = PaymentMethod.TUNAI.name,
    val notes: String = "",
    val paidAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "isp_profile")
data class IspProfileEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "NetBill RT/RW Net",
    val addressInfo: String = "Layanan Internet Fiber & Wireless Lokal",
    val managerPhone: String = "081234567890",
    val bankAccountInfo: String = "BCA 1234567890 a.n. Pengelola NetBill\nBRI 0987654321 a.n. Pengelola NetBill",
    val printerPaperWidthMm: Int = 58,
    val receiptFooter: String = "Terima kasih atas pembayaran Anda.\nMohon simpan struk ini sebagai bukti sah."
)
