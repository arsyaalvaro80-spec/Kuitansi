package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InternetPlanEntity
import com.example.data.local.IspProfileEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NetBillBackupSnapshot(
    val exportedAt: Long = System.currentTimeMillis(),
    val profile: IspProfileEntity,
    val plans: List<InternetPlanEntity>,
    val customers: List<CustomerEntity>,
    val transactions: List<BillTransactionEntity>
)

object BackupRestoreHelper {

    private const val BACKUP_SCHEMA_VERSION = 1

    fun suggestedBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "NetBill_Backup_${sdf.format(Date())}.json"
    }

    fun encodeToJson(snapshot: NetBillBackupSnapshot): String {
        val root = JSONObject().apply {
            put("app", "NetBillISP")
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("exportedAt", snapshot.exportedAt)

            put(
                "profile",
                JSONObject().apply {
                    put("id", snapshot.profile.id)
                    put("businessName", snapshot.profile.businessName)
                    put("addressInfo", snapshot.profile.addressInfo)
                    put("managerPhone", snapshot.profile.managerPhone)
                    put("bankAccountInfo", snapshot.profile.bankAccountInfo)
                    put("printerPaperWidthMm", snapshot.profile.printerPaperWidthMm)
                    put("receiptFooter", snapshot.profile.receiptFooter)
                }
            )

            put(
                "plans",
                JSONArray().apply {
                    snapshot.plans.forEach { p ->
                        put(
                            JSONObject().apply {
                                put("id", p.id)
                                put("name", p.name)
                                put("speedMbps", p.speedMbps)
                                put("price", p.price)
                                put("description", p.description)
                                put("createdAt", p.createdAt)
                            }
                        )
                    }
                }
            )

            put(
                "customers",
                JSONArray().apply {
                    snapshot.customers.forEach { c ->
                        put(
                            JSONObject().apply {
                                put("id", c.id)
                                put("name", c.name)
                                put("phone", c.phone)
                                put("address", c.address)
                                put("planId", c.planId)
                                put("dueDay", c.dueDay)
                                put("isActive", c.isActive)
                                put("notes", c.notes)
                                put("createdAt", c.createdAt)
                            }
                        )
                    }
                }
            )

            put(
                "transactions",
                JSONArray().apply {
                    snapshot.transactions.forEach { t ->
                        put(
                            JSONObject().apply {
                                put("id", t.id)
                                put("invoiceNumber", t.invoiceNumber)
                                put("customerId", t.customerId)
                                put("customerNameSnapshot", t.customerNameSnapshot)
                                put("customerPhoneSnapshot", t.customerPhoneSnapshot)
                                put("customerAddressSnapshot", t.customerAddressSnapshot)
                                put("planNameSnapshot", t.planNameSnapshot)
                                put("planSpeedMbpsSnapshot", t.planSpeedMbpsSnapshot)
                                put("billingMonth", t.billingMonth)
                                put("billingYear", t.billingYear)
                                put("amountPaid", t.amountPaid)
                                put("paymentMethod", t.paymentMethod)
                                put("notes", t.notes)
                                put("paidAt", t.paidAt)
                            }
                        )
                    }
                }
            )
        }
        return root.toString(2)
    }

    fun decodeFromJson(rawJson: String): NetBillBackupSnapshot {
        val root = JSONObject(rawJson)
        require(root.has("plans") && root.has("customers") && root.has("transactions")) {
            "Format berkas backup tidak valid."
        }

        val profileObj = root.optJSONObject("profile")
        val profile = if (profileObj != null) {
            IspProfileEntity(
                id = 1,
                businessName = profileObj.optString("businessName", "NetBill RT/RW Net"),
                addressInfo = profileObj.optString("addressInfo", ""),
                managerPhone = profileObj.optString("managerPhone", ""),
                bankAccountInfo = profileObj.optString("bankAccountInfo", ""),
                printerPaperWidthMm = profileObj.optInt("printerPaperWidthMm", 58),
                receiptFooter = profileObj.optString("receiptFooter", "")
            )
        } else {
            IspProfileEntity()
        }

        val plansArr = root.getJSONArray("plans")
        val plans = buildList {
            for (i in 0 until plansArr.length()) {
                val obj = plansArr.getJSONObject(i)
                add(
                    InternetPlanEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        speedMbps = obj.optInt("speedMbps", 10),
                        price = obj.optLong("price", 0L),
                        description = obj.optString("description", "Unlimited tanpa FUP"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val customersArr = root.getJSONArray("customers")
        val customers = buildList {
            for (i in 0 until customersArr.length()) {
                val obj = customersArr.getJSONObject(i)
                add(
                    CustomerEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        pppoeOrIp = "",
                        planId = obj.optLong("planId", 0L),
                        dueDay = obj.optInt("dueDay", 10).coerceIn(1, 28),
                        isActive = obj.optBoolean("isActive", true),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val txArr = root.getJSONArray("transactions")
        val transactions = buildList {
            for (i in 0 until txArr.length()) {
                val obj = txArr.getJSONObject(i)
                add(
                    BillTransactionEntity(
                        id = obj.optLong("id", 0L),
                        invoiceNumber = obj.getString("invoiceNumber"),
                        customerId = obj.optLong("customerId", 0L),
                        customerNameSnapshot = obj.optString("customerNameSnapshot", ""),
                        customerPhoneSnapshot = obj.optString("customerPhoneSnapshot", ""),
                        customerAddressSnapshot = obj.optString("customerAddressSnapshot", ""),
                        customerPppoeSnapshot = "",
                        planNameSnapshot = obj.optString("planNameSnapshot", "Paket Internet"),
                        planSpeedMbpsSnapshot = obj.optInt("planSpeedMbpsSnapshot", 0),
                        billingMonth = obj.optInt("billingMonth", 1).coerceIn(1, 12),
                        billingYear = obj.optInt("billingYear", 2026),
                        amountPaid = obj.optLong("amountPaid", 0L),
                        paymentMethod = obj.optString("paymentMethod", "TUNAI"),
                        notes = obj.optString("notes", ""),
                        paidAt = obj.optLong("paidAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return NetBillBackupSnapshot(
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            profile = profile,
            plans = plans,
            customers = customers,
            transactions = transactions
        )
    }

    fun writeBackupToUri(
        context: Context,
        uri: Uri,
        snapshot: NetBillBackupSnapshot
    ): Result<Unit> = runCatching {
        val json = encodeToJson(snapshot)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Tidak dapat menulis ke lokasi berkas backup.")
    }

    fun readBackupFromUri(
        context: Context,
        uri: Uri
    ): Result<NetBillBackupSnapshot> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Tidak dapat membaca berkas backup.")
        decodeFromJson(raw)
    }
}
