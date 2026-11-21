package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.BillTransactionDao
import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerDao
import com.example.data.local.CustomerEntity
import com.example.data.local.DefaultPrinterConfig
import com.example.data.local.InternetPlanDao
import com.example.data.local.InternetPlanEntity
import com.example.data.local.IspProfileDao
import com.example.data.local.IspProfileEntity
import com.example.data.local.NetBillDatabase
import com.example.data.local.PaymentMethod
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.util.NetBillBackupSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class NetBillRepository(
    private val database: NetBillDatabase,
    private val planDao: InternetPlanDao,
    private val customerDao: CustomerDao,
    private val transactionDao: BillTransactionDao,
    private val profileDao: IspProfileDao,
    private val themePreferences: ThemePreferences
) {
    val allPlans: Flow<List<InternetPlanEntity>> = planDao.observeAllPlans()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.observeAllCustomers()
    val allTransactions: Flow<List<BillTransactionEntity>> = transactionDao.observeAllTransactions()
    val ispProfile: Flow<IspProfileEntity> = profileDao.observeProfile().map { it ?: IspProfileEntity() }
    val themeMode: Flow<ThemeMode> = themePreferences.themeModeFlow
    val defaultPrinter: Flow<DefaultPrinterConfig?> = themePreferences.defaultPrinterFlow

    fun observeTransactionsByPeriod(year: Int, month: Int): Flow<List<BillTransactionEntity>> {
        return transactionDao.observeTransactionsByPeriod(year, month)
    }

    suspend fun ensureSeedData() {
        if (profileDao.getProfileOnce() == null) {
            profileDao.upsertProfile(IspProfileEntity())
        }
        if (planDao.getPlanCount() == 0) {
            planDao.insertPlans(
                listOf(
                    InternetPlanEntity(
                        name = "Paket Home 25 Mbps",
                        speedMbps = 25,
                        price = 150_000L,
                        description = "Unlimited tanpa FUP • Cocok untuk keluarga & streaming HD"
                    ),
                    InternetPlanEntity(
                        name = "Paket Home 35 Mbps",
                        speedMbps = 35,
                        price = 200_000L,
                        description = "Unlimited tanpa FUP • Ideal untuk WFH, gaming & multi perangkat"
                    ),
                    InternetPlanEntity(
                        name = "Paket Ultra 50 Mbps",
                        speedMbps = 50,
                        price = 275_000L,
                        description = "Unlimited tanpa FUP • Kecepatan tinggi untuk usaha & rumah besar"
                    )
                )
            )
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    suspend fun setDefaultPrinter(name: String, address: String) {
        themePreferences.setDefaultPrinter(name, address)
    }

    suspend fun clearDefaultPrinter() {
        themePreferences.clearDefaultPrinter()
    }

    suspend fun saveProfile(profile: IspProfileEntity) {
        profileDao.upsertProfile(profile.copy(id = 1))
    }

    suspend fun savePlan(plan: InternetPlanEntity) {
        if (plan.id == 0L) {
            planDao.insertPlan(plan)
        } else {
            planDao.updatePlan(plan)
        }
    }

    suspend fun deletePlan(plan: InternetPlanEntity) {
        planDao.deletePlan(plan)
    }

    suspend fun saveCustomer(customer: CustomerEntity) {
        if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
        }
    }

    suspend fun updateCustomerDueDay(customerId: Long, dueDay: Int) {
        customerDao.updateCustomerDueDay(customerId, dueDay.coerceIn(1, 28))
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun recordPayment(
        customer: CustomerEntity,
        plan: InternetPlanEntity?,
        billingMonth: Int,
        billingYear: Int,
        amountPaid: Long,
        paymentMethod: PaymentMethod,
        notes: String
    ): BillTransactionEntity {
        val existing = transactionDao.findTransactionForCustomerPeriod(customer.id, billingYear, billingMonth)
        val invoiceNumber = existing?.invoiceNumber ?: generateInvoiceNumber(billingYear, billingMonth)
        val transaction = BillTransactionEntity(
            id = existing?.id ?: 0L,
            invoiceNumber = invoiceNumber,
            customerId = customer.id,
            customerNameSnapshot = customer.name,
            customerPhoneSnapshot = customer.phone,
            customerAddressSnapshot = customer.address,
            customerPppoeSnapshot = "",
            planNameSnapshot = plan?.name ?: "Paket Internet",
            planSpeedMbpsSnapshot = plan?.speedMbps ?: 0,
            billingMonth = billingMonth,
            billingYear = billingYear,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod.name,
            notes = notes.trim(),
            paidAt = System.currentTimeMillis()
        )
        val insertedId = transactionDao.insertTransaction(transaction)
        return transaction.copy(id = if (existing != null) existing.id else insertedId)
    }

    suspend fun updateTransactionPaymentMethod(transactionId: Long, paymentMethod: PaymentMethod) {
        transactionDao.updatePaymentMethod(transactionId, paymentMethod.name)
    }

    suspend fun updateTransactionDetails(
        transactionId: Long,
        amountPaid: Long,
        paymentMethod: PaymentMethod,
        notes: String
    ) {
        transactionDao.updateTransactionDetails(
            transactionId = transactionId,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod.name,
            notes = notes.trim()
        )
    }

    suspend fun deleteTransaction(transaction: BillTransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun exportBackupSnapshot(): NetBillBackupSnapshot {
        return NetBillBackupSnapshot(
            exportedAt = System.currentTimeMillis(),
            profile = profileDao.getProfileOnce() ?: IspProfileEntity(),
            plans = planDao.getAllPlansOnce(),
            customers = customerDao.getAllCustomersOnce(),
            transactions = transactionDao.getAllTransactionsOnce()
        )
    }

    suspend fun restoreBackupSnapshot(snapshot: NetBillBackupSnapshot) {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            customerDao.deleteAllCustomers()
            planDao.deleteAllPlans()

            profileDao.upsertProfile(snapshot.profile.copy(id = 1))
            if (snapshot.plans.isNotEmpty()) {
                planDao.insertPlans(snapshot.plans)
            }
            if (snapshot.customers.isNotEmpty()) {
                customerDao.insertCustomers(snapshot.customers)
            }
            if (snapshot.transactions.isNotEmpty()) {
                transactionDao.insertTransactions(snapshot.transactions)
            }
        }
    }

    private suspend fun generateInvoiceNumber(year: Int, month: Int): String {
        val count = transactionDao.countTransactionsInPeriod(year, month) + 1
        val monthStr = String.format(Locale.US, "%02d", month)
        val seqStr = String.format(Locale.US, "%04d", count)
        val suffix = (System.currentTimeMillis() % 100).toString().padStart(2, '0')
        return "INV/$year$monthStr/$seqStr-$suffix"
    }
}
