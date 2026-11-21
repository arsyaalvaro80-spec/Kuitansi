package com.example.domain

import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InternetPlanEntity
import com.example.data.local.PaymentMethod
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class BillingStatus(val label: String, val sortOrder: Int) {
    MENUNGGAK("Menunggak", 0),
    HARI_INI("Jatuh Tempo Hari Ini", 1),
    MENDEKATI("Mendekati (H-3)", 2),
    BELUM_BAYAR("Belum Bayar", 3),
    LUNAS("Lunas", 4)
}

enum class BillFilter(val label: String) {
    SEMUA("Semua"),
    MENUNGGAK("Menunggak"),
    HARI_INI("Hari Ini"),
    MENDEKATI("Mendekati"),
    BELUM_BAYAR("Belum Bayar"),
    LUNAS("Lunas"),
    LUNAS_TUNAI("Lunas Tunai"),
    LUNAS_TRANSFER("Lunas Transfer")
}

enum class PaymentBookFilter(val label: String) {
    SEMUA("Semua"),
    TUNAI("Tunai"),
    TRANSFER("Transfer")
}

data class CustomerBillItem(
    val customer: CustomerEntity,
    val plan: InternetPlanEntity?,
    val transaction: BillTransactionEntity?,
    val status: BillingStatus,
    val billingMonth: Int,
    val billingYear: Int,
    val dueDateLabel: String,
    val daysUntilDue: Int
) {
    val monthlyAmount: Long
        get() = transaction?.amountPaid ?: plan?.price ?: 0L

    val paymentMethod: PaymentMethod?
        get() = transaction?.let { PaymentMethod.fromString(it.paymentMethod) }
}

data class DashboardSummary(
    val totalIncome: Long = 0L,
    val expectedTotalIncome: Long = 0L,
    val unpaidAmount: Long = 0L,
    val cashIncome: Long = 0L,
    val cashPaidCount: Int = 0,
    val transferIncome: Long = 0L,
    val transferPaidCount: Int = 0,
    val paidCount: Int = 0,
    val totalActiveCount: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val upcomingCount: Int = 0,
    val unpaidCount: Int = 0
)

data class BookkeepingSummary(
    val totalIncome: Long = 0L,
    val totalCount: Int = 0,
    val cashIncome: Long = 0L,
    val cashCount: Int = 0,
    val transferIncome: Long = 0L,
    val transferCount: Int = 0
)

data class PlanWithCount(
    val plan: InternetPlanEntity,
    val subscriberCount: Int
)

object IndonesianFormatters {
    private val indonesianLocale = Locale.forLanguageTag("id-ID")

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val shortMonthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(indonesianLocale)
        return "Rp ${formatter.format(amount)}"
    }

    fun formatRupiahPlain(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(indonesianLocale)
        return "Rp${formatter.format(amount)}"
    }

    fun formatMonthYear(month: Int, year: Int): String {
        val safeIndex = (month - 1).coerceIn(0, 11)
        return "${monthNames[safeIndex]} $year"
    }

    fun formatShortMonthYear(month: Int, year: Int): String {
        val safeIndex = (month - 1).coerceIn(0, 11)
        return "${shortMonthNames[safeIndex]} $year"
    }

    fun formatDueDate(dueDay: Int, month: Int, year: Int): String {
        val clampedDay = clampDayForMonth(dueDay, month, year)
        val safeIndex = (month - 1).coerceIn(0, 11)
        return "$clampedDay ${shortMonthNames[safeIndex]} $year"
    }

    fun formatFullDateTime(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(timestampMillis))
    }

    fun formatShortDate(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", indonesianLocale)
        return sdf.format(Date(timestampMillis))
    }

    fun clampDayForMonth(day: Int, month: Int, year: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return day.coerceIn(1, maxDay)
    }

    /**
     * Converts Indonesian phone numbers like "0812-3456-7890", "+62 812...", "62812..." into "6281234567890".
     */
    fun normalizeIndonesianPhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            digits.startsWith("62") -> digits
            digits.startsWith("0") -> "62" + digits.drop(1)
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }
}

object BillingCalculator {

    fun calculateCustomerBills(
        customers: List<CustomerEntity>,
        plans: List<InternetPlanEntity>,
        periodTransactions: List<BillTransactionEntity>,
        billingMonth: Int,
        billingYear: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): List<CustomerBillItem> {
        val planMap = plans.associateBy { it.id }
        val txMap = periodTransactions.associateBy { it.customerId }

        val todayStart = startOfDayMillis(nowMillis)

        return customers
            .filter { it.isActive || txMap.containsKey(it.id) }
            .map { customer ->
                val plan = planMap[customer.planId]
                val tx = txMap[customer.id]
                val clampedDueDay = IndonesianFormatters.clampDayForMonth(customer.dueDay, billingMonth, billingYear)
                val dueDateMillis = startOfDayMillisForDate(billingYear, billingMonth, clampedDueDay)
                val diffDays = ((dueDateMillis - todayStart) / MILLIS_PER_DAY).toInt()

                val status = when {
                    tx != null -> BillingStatus.LUNAS
                    diffDays < 0 -> BillingStatus.MENUNGGAK
                    diffDays == 0 -> BillingStatus.HARI_INI
                    diffDays in 1..3 -> BillingStatus.MENDEKATI
                    else -> BillingStatus.BELUM_BAYAR
                }

                CustomerBillItem(
                    customer = customer,
                    plan = plan,
                    transaction = tx,
                    status = status,
                    billingMonth = billingMonth,
                    billingYear = billingYear,
                    dueDateLabel = IndonesianFormatters.formatDueDate(clampedDueDay, billingMonth, billingYear),
                    daysUntilDue = diffDays
                )
            }
            .sortedWith(
                compareBy<CustomerBillItem> { it.status.sortOrder }
                    .thenBy { it.customer.dueDay }
                    .thenBy { it.customer.name.lowercase() }
            )
    }

    fun calculateSummary(
        billItems: List<CustomerBillItem>,
        periodTransactions: List<BillTransactionEntity>
    ): DashboardSummary {
        val bookkeeping = calculateBookkeepingSummary(periodTransactions)
        val unpaidBills = billItems.filter { it.status != BillingStatus.LUNAS }
        val unpaidAmount = unpaidBills.sumOf { it.monthlyAmount }
        val expectedTotalIncome = bookkeeping.totalIncome + unpaidAmount
        val paidActiveCount = billItems.count { it.status == BillingStatus.LUNAS }
        val overdueCount = billItems.count { it.status == BillingStatus.MENUNGGAK }
        val dueTodayCount = billItems.count { it.status == BillingStatus.HARI_INI }
        val upcomingCount = billItems.count { it.status == BillingStatus.MENDEKATI }
        val unpaidCount = unpaidBills.size

        // Ensure total target customer count accounts for any already-paid inactive customer this period
        val extraInactivePaid = (bookkeeping.totalCount - paidActiveCount).coerceAtLeast(0)
        val totalPaidCount = paidActiveCount + extraInactivePaid
        val totalTargetCount = billItems.size + extraInactivePaid

        return DashboardSummary(
            totalIncome = bookkeeping.totalIncome,
            expectedTotalIncome = expectedTotalIncome,
            unpaidAmount = unpaidAmount,
            cashIncome = bookkeeping.cashIncome,
            cashPaidCount = bookkeeping.cashCount,
            transferIncome = bookkeeping.transferIncome,
            transferPaidCount = bookkeeping.transferCount,
            paidCount = totalPaidCount,
            totalActiveCount = totalTargetCount,
            overdueCount = overdueCount,
            dueTodayCount = dueTodayCount,
            upcomingCount = upcomingCount,
            unpaidCount = unpaidCount
        )
    }

    fun calculateBookkeepingSummary(
        transactions: List<BillTransactionEntity>
    ): BookkeepingSummary {
        var cashIncome = 0L
        var cashCount = 0
        var transferIncome = 0L
        var transferCount = 0

        for (tx in transactions) {
            val method = PaymentMethod.fromString(tx.paymentMethod)
            if (method.isCash) {
                cashIncome += tx.amountPaid
                cashCount++
            } else {
                transferIncome += tx.amountPaid
                transferCount++
            }
        }

        return BookkeepingSummary(
            totalIncome = cashIncome + transferIncome,
            totalCount = transactions.size,
            cashIncome = cashIncome,
            cashCount = cashCount,
            transferIncome = transferIncome,
            transferCount = transferCount
        )
    }

    fun describeDueOffset(item: CustomerBillItem): String {
        return when (item.status) {
            BillingStatus.LUNAS -> "Sudah Lunas"
            BillingStatus.MENUNGGAK -> "Terlambat ${abs(item.daysUntilDue)} hari"
            BillingStatus.HARI_INI -> "Jatuh tempo hari ini"
            BillingStatus.MENDEKATI -> "H-${item.daysUntilDue} jatuh tempo"
            BillingStatus.BELUM_BAYAR -> "Tgl ${item.customer.dueDay} setiap bulan"
        }
    }

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    private fun startOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun startOfDayMillisForDate(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
