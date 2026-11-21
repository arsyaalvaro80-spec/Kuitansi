package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.BillingStatus
import com.example.domain.CustomerBillItem
import com.example.domain.IndonesianFormatters

object BillingNotificationHelper {

    private const val CHANNEL_ID = "netbill_due_reminders"
    private const val NOTIFICATION_ID = 20261

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendDueReminderNotification(
        context: Context,
        ispName: String,
        billItems: List<CustomerBillItem>
    ): String {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            return "Izin notifikasi belum aktif. Aktifkan izin notifikasi terlebih dahulu."
        }

        val overdue = billItems.filter { it.status == BillingStatus.MENUNGGAK }
        val dueToday = billItems.filter { it.status == BillingStatus.HARI_INI }
        val upcoming = billItems.filter { it.status == BillingStatus.MENDEKATI }

        val title = if (overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty()) {
            "$ispName • Semua Tagihan Aman"
        } else {
            "$ispName • Pengingat Jatuh Tempo"
        }

        val summaryLine = if (overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty()) {
            "Tidak ada pelanggan menunggak, jatuh tempo hari ini, atau H-3."
        } else {
            "Menunggak: ${overdue.size} • Hari Ini: ${dueToday.size} • H-3: ${upcoming.size}"
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(summaryLine)

        val prioritized = (overdue + dueToday + upcoming).take(6)
        if (prioritized.isEmpty()) {
            inboxStyle.addLine("Semua tagihan pelanggan aktif dalam kondisi terkendali.")
        } else {
            prioritized.forEach { item ->
                val tag = when (item.status) {
                    BillingStatus.MENUNGGAK -> "[MENUNGGAK]"
                    BillingStatus.HARI_INI -> "[HARI INI]"
                    BillingStatus.MENDEKATI -> "[H-${item.daysUntilDue}]"
                    else -> ""
                }
                inboxStyle.addLine(
                    "$tag ${item.customer.name} • ${IndonesianFormatters.formatRupiah(item.monthlyAmount)}"
                )
            }
            val remaining = (overdue.size + dueToday.size + upcoming.size) - prioritized.size
            if (remaining > 0) {
                inboxStyle.addLine("+$remaining pelanggan lainnya perlu ditindaklanjuti")
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            "Notifikasi pengingat terkirim ($summaryLine)"
        } catch (e: SecurityException) {
            "Gagal mengirim notifikasi: izin ditolak."
        }
    }
}
