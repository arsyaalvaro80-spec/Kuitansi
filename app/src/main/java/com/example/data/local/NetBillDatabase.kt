package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InternetPlanEntity::class,
        CustomerEntity::class,
        BillTransactionEntity::class,
        IspProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NetBillDatabase : RoomDatabase() {
    abstract fun internetPlanDao(): InternetPlanDao
    abstract fun customerDao(): CustomerDao
    abstract fun billTransactionDao(): BillTransactionDao
    abstract fun ispProfileDao(): IspProfileDao

    companion object {
        @Volatile
        private var INSTANCE: NetBillDatabase? = null

        fun getInstance(context: Context): NetBillDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetBillDatabase::class.java,
                    "netbill_isp.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
