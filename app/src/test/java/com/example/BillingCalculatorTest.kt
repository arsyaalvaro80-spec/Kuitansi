package com.example

import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InternetPlanEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.domain.BillingCalculator
import com.example.util.BackupRestoreHelper
import com.example.util.BookkeepingExportHelper
import com.example.util.NetBillBackupSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BillingCalculatorTest {

    @Test
    fun `bookkeeping summary accurately separates Tunai and Transfer payments`() {
        val transactions = listOf(
            sampleTx(id = 1, customerId = 1, name = "Budi", amount = 150_000L, method = PaymentMethod.TUNAI),
            sampleTx(id = 2, customerId = 2, name = "Siti", amount = 200_000L, method = PaymentMethod.TRANSFER_BANK),
            sampleTx(id = 3, customerId = 3, name = "Agus", amount = 150_000L, method = PaymentMethod.TUNAI),
            sampleTx(id = 4, customerId = 4, name = "Dewi", amount = 275_000L, method = PaymentMethod.QRIS)
        )

        val summary = BillingCalculator.calculateBookkeepingSummary(transactions)

        assertEquals(775_000L, summary.totalIncome)
        assertEquals(4, summary.totalCount)
        assertEquals(300_000L, summary.cashIncome)
        assertEquals(2, summary.cashCount)
        assertEquals(475_000L, summary.transferIncome)
        assertEquals(2, summary.transferCount)
    }

    @Test
    fun `dashboard summary computes target income, unpaid piutang, and keeps inactive paid customer consistent`() {
        val plan = InternetPlanEntity(id = 1, name = "Home 25", speedMbps = 25, price = 150_000L)
        val customers = listOf(
            CustomerEntity(id = 1, name = "Budi", phone = "0811", address = "A1", planId = 1, dueDay = 10, isActive = true),
            CustomerEntity(id = 2, name = "Siti", phone = "0812", address = "A2", planId = 1, dueDay = 10, isActive = false),
            CustomerEntity(id = 3, name = "Rudi", phone = "0813", address = "A3", planId = 1, dueDay = 25, isActive = true),
            CustomerEntity(id = 4, name = "Nonaktif Belum Bayar", phone = "0814", address = "A4", planId = 1, dueDay = 25, isActive = false)
        )
        val transactions = listOf(
            sampleTx(id = 1, customerId = 1, name = "Budi", amount = 150_000L, method = PaymentMethod.TUNAI),
            sampleTx(id = 2, customerId = 2, name = "Siti", amount = 150_000L, method = PaymentMethod.TRANSFER_BANK)
        )

        val bills = BillingCalculator.calculateCustomerBills(
            customers = customers,
            plans = listOf(plan),
            periodTransactions = transactions,
            billingMonth = 11,
            billingYear = 2026
        )
        val dashboard = BillingCalculator.calculateSummary(bills, transactions)

        assertEquals(3, bills.size) // Active (1, 3) + already-paid inactive (2); unpaid inactive (4) excluded
        assertEquals(300_000L, dashboard.totalIncome)
        assertEquals(150_000L, dashboard.unpaidAmount)
        assertEquals(450_000L, dashboard.expectedTotalIncome)
        assertEquals(150_000L, dashboard.cashIncome)
        assertEquals(1, dashboard.cashPaidCount)
        assertEquals(150_000L, dashboard.transferIncome)
        assertEquals(1, dashboard.transferPaidCount)
        assertEquals(2, dashboard.paidCount)
        assertEquals(3, dashboard.totalActiveCount)
        assertEquals(1, dashboard.unpaidCount)

        val budiBill = bills.first { it.customer.id == 1L }
        val sitiBill = bills.first { it.customer.id == 2L }
        assertTrue(budiBill.paymentMethod?.isCash == true)
        assertTrue(sitiBill.paymentMethod?.isTransfer == true)
    }

    @Test
    fun `csv export includes separated Tunai and Transfer totals`() {
        val cashTx = listOf(sampleTx(id = 1, customerId = 1, name = "Budi, RT 01", amount = 150_000L, method = PaymentMethod.TUNAI))
        val transferTx = listOf(sampleTx(id = 2, customerId = 2, name = "Siti", amount = 200_000L, method = PaymentMethod.TRANSFER_BANK))
        val csv = BookkeepingExportHelper.buildBookkeepingCsv(
            periodLabel = "November 2026",
            cashTransactions = cashTx,
            transferTransactions = transferTx,
            profile = IspProfileEntity(businessName = "NetBill RT/RW Net")
        )

        assertTrue(csv.contains("\"Budi, RT 01\""))
        assertTrue(csv.contains("TOTAL KAS TUNAI (1 org),150000,0,150000"))
        assertTrue(csv.contains("TOTAL TRANSFER (1 org),0,200000,200000"))
        assertTrue(csv.contains("GRAND TOTAL (2 org),150000,200000,350000"))
    }

    @Test
    fun `json backup and restore round trip preserves plans, customers, and transactions`() {
        val snapshot = NetBillBackupSnapshot(
            exportedAt = 123456789L,
            profile = IspProfileEntity(businessName = "WiFi Warga Net", managerPhone = "0812345"),
            plans = listOf(InternetPlanEntity(id = 1, name = "Home 25", speedMbps = 25, price = 150_000L)),
            customers = listOf(CustomerEntity(id = 10, name = "Pak Ahmad", phone = "0812", address = "Blok C", planId = 1, dueDay = 5)),
            transactions = listOf(sampleTx(id = 99, customerId = 10, name = "Pak Ahmad", amount = 150_000L, method = PaymentMethod.TRANSFER_BANK))
        )

        val json = BackupRestoreHelper.encodeToJson(snapshot)
        val restored = BackupRestoreHelper.decodeFromJson(json)

        assertEquals("WiFi Warga Net", restored.profile.businessName)
        assertEquals(1, restored.plans.size)
        assertEquals(1, restored.customers.size)
        assertEquals("Pak Ahmad", restored.customers.first().name)
        assertEquals(1, restored.transactions.size)
        assertEquals(PaymentMethod.TRANSFER_BANK.name, restored.transactions.first().paymentMethod)
    }

    private fun sampleTx(
        id: Long,
        customerId: Long,
        name: String,
        amount: Long,
        method: PaymentMethod
    ) = BillTransactionEntity(
        id = id,
        invoiceNumber = "INV/202611/000$id",
        customerId = customerId,
        customerNameSnapshot = name,
        customerPhoneSnapshot = "08123456789",
        customerAddressSnapshot = "Blok A",
        planNameSnapshot = "Home 25",
        planSpeedMbpsSnapshot = 25,
        billingMonth = 11,
        billingYear = 2026,
        amountPaid = amount,
        paymentMethod = method.name
    )
}
