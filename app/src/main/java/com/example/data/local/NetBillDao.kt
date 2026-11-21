package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InternetPlanDao {
    @Query("SELECT * FROM internet_plans ORDER BY speedMbps ASC, price ASC")
    fun observeAllPlans(): Flow<List<InternetPlanEntity>>

    @Query("SELECT * FROM internet_plans ORDER BY id ASC")
    suspend fun getAllPlansOnce(): List<InternetPlanEntity>

    @Query("SELECT COUNT(*) FROM internet_plans")
    suspend fun getPlanCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: InternetPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<InternetPlanEntity>)

    @Update
    suspend fun updatePlan(plan: InternetPlanEntity)

    @Delete
    suspend fun deletePlan(plan: InternetPlanEntity)

    @Query("DELETE FROM internet_plans")
    suspend fun deleteAllPlans()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY isActive DESC, dueDay ASC, name ASC")
    fun observeAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET dueDay = :dueDay WHERE id = :customerId")
    suspend fun updateCustomerDueDay(customerId: Long, dueDay: Int)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
}

@Dao
interface BillTransactionDao {
    @Query("SELECT * FROM bill_transactions ORDER BY paidAt DESC")
    fun observeAllTransactions(): Flow<List<BillTransactionEntity>>

    @Query("SELECT * FROM bill_transactions ORDER BY id ASC")
    suspend fun getAllTransactionsOnce(): List<BillTransactionEntity>

    @Query("SELECT * FROM bill_transactions WHERE billingYear = :year AND billingMonth = :month ORDER BY paidAt DESC")
    fun observeTransactionsByPeriod(year: Int, month: Int): Flow<List<BillTransactionEntity>>

    @Query("SELECT COUNT(*) FROM bill_transactions WHERE billingYear = :year AND billingMonth = :month")
    suspend fun countTransactionsInPeriod(year: Int, month: Int): Int

    @Query("SELECT * FROM bill_transactions WHERE customerId = :customerId AND billingYear = :year AND billingMonth = :month LIMIT 1")
    suspend fun findTransactionForCustomerPeriod(customerId: Long, year: Int, month: Int): BillTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BillTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<BillTransactionEntity>)

    @Query("UPDATE bill_transactions SET paymentMethod = :paymentMethod WHERE id = :transactionId")
    suspend fun updatePaymentMethod(transactionId: Long, paymentMethod: String)

    @Query("UPDATE bill_transactions SET amountPaid = :amountPaid, paymentMethod = :paymentMethod, notes = :notes WHERE id = :transactionId")
    suspend fun updateTransactionDetails(
        transactionId: Long,
        amountPaid: Long,
        paymentMethod: String,
        notes: String
    )

    @Delete
    suspend fun deleteTransaction(transaction: BillTransactionEntity)

    @Query("DELETE FROM bill_transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface IspProfileDao {
    @Query("SELECT * FROM isp_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<IspProfileEntity?>

    @Query("SELECT * FROM isp_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): IspProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: IspProfileEntity)
}
