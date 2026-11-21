package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BillTransactionEntity
import com.example.data.local.PaymentMethod
import com.example.domain.IndonesianFormatters
import com.example.domain.PaymentBookFilter
import com.example.ui.components.BookkeepingBreakdownCard
import com.example.ui.components.NetBillSearchBar
import com.example.ui.components.PaymentGroupHeader
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNetBillColors
import com.example.ui.viewmodel.BookkeepingUiState

@Composable
fun KuitansiScreen(
    uiState: BookkeepingUiState,
    onShiftPeriod: (Int) -> Unit,
    onTogglePeriodFilter: (Boolean) -> Unit,
    onSelectPaymentFilter: (PaymentBookFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onShareBookkeepingReport: () -> Unit,
    onExportCsv: () -> Unit,
    onChangePaymentMethod: (BillTransactionEntity) -> Unit,
    onPreviewReceipt: (BillTransactionEntity) -> Unit,
    onPrintThermal: (BillTransactionEntity) -> Unit,
    onSendPhotoWa: (BillTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = IndonesianFormatters.formatMonthYear(uiState.period.month, uiState.period.year)
    val cashList = uiState.cashTransactions
    val transferList = uiState.transferTransactions
    val hasAnyVisibleResult = when (uiState.selectedPaymentFilter) {
        PaymentBookFilter.SEMUA -> cashList.isNotEmpty() || transferList.isNotEmpty()
        PaymentBookFilter.TUNAI -> cashList.isNotEmpty()
        PaymentBookFilter.TRANSFER -> transferList.isNotEmpty()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header + Scope Toggle (Per Bulan vs Semua Periode)
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pembukuan & Kuitansi",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pemisahan pembayaran Tunai dan Transfer untuk rekap kas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Period Scope Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.filterByPeriod,
                        onClick = { onTogglePeriodFilter(true) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Per Bulan") },
                        modifier = Modifier.testTag("bookkeeping_scope_month_chip")
                    )
                    FilterChip(
                        selected = !uiState.filterByPeriod,
                        onClick = { onTogglePeriodFilter(false) },
                        label = { Text("Semua Periode") },
                        modifier = Modifier.testTag("bookkeeping_scope_all_chip")
                    )
                }

                if (uiState.filterByPeriod) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { onShiftPeriod(-1) },
                                modifier = Modifier.testTag("bookkeeping_prev_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Bulan sebelumnya"
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "PERIODE PEMBUKUAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = periodLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onShiftPeriod(1) },
                                modifier = Modifier.testTag("bookkeeping_next_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Bulan berikutnya"
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Bookkeeping Breakdown Summary Card (Kas Tunai vs Transfer)
        item(key = "bookkeeping_summary") {
            BookkeepingBreakdownCard(
                summary = uiState.summary,
                selectedFilter = uiState.selectedPaymentFilter,
                onSelectFilter = onSelectPaymentFilter,
                onShareReport = onShareBookkeepingReport,
                onExportCsv = onExportCsv
            )
        }

        // 3. Payment Method Filter Bar (Semua | Tunai | Transfer)
        item(key = "payment_filter_chips") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentBookFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        PaymentBookFilter.SEMUA -> uiState.summary.totalCount
                        PaymentBookFilter.TUNAI -> uiState.summary.cashCount
                        PaymentBookFilter.TRANSFER -> uiState.summary.transferCount
                    }
                    FilterChip(
                        selected = uiState.selectedPaymentFilter == filter,
                        onClick = { onSelectPaymentFilter(filter) },
                        leadingIcon = when (filter) {
                            PaymentBookFilter.SEMUA -> null
                            PaymentBookFilter.TUNAI -> {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Payments,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            PaymentBookFilter.TRANSFER -> {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("bookkeeping_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // 4. Search Bar
        item(key = "search") {
            NetBillSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Cari no. struk, nama pelanggan, metode…",
                testTag = "receipts_search_input"
            )
        }

        // 5. Separated Tunai & Transfer Transaction Sections
        if (!hasAnyVisibleResult) {
            item(key = "empty") {
                EmptyBookkeepingCard(
                    selectedFilter = uiState.selectedPaymentFilter,
                    periodLabel = if (uiState.filterByPeriod) periodLabel else "Semua Periode",
                    hasSearchQuery = uiState.searchQuery.isNotBlank(),
                    onResetFilters = {
                        onSearchChange("")
                        onSelectPaymentFilter(PaymentBookFilter.SEMUA)
                    }
                )
            }
        } else {
            // Section 1: Pembayaran Tunai
            if (uiState.selectedPaymentFilter == PaymentBookFilter.SEMUA ||
                uiState.selectedPaymentFilter == PaymentBookFilter.TUNAI
            ) {
                item(key = "section_header_tunai") {
                    PaymentGroupHeader(
                        isCash = true,
                        count = cashList.size,
                        subtotal = cashList.sumOf { it.amountPaid },
                        modifier = Modifier.testTag("section_header_tunai")
                    )
                }

                if (cashList.isEmpty()) {
                    item(key = "empty_tunai_inline") {
                        CompactEmptyGroupNote(text = "Belum ada pelanggan yang membayar secara Tunai pada tampilan ini.")
                    }
                } else {
                    items(
                        items = cashList,
                        key = { "cash_${it.id}" }
                    ) { tx ->
                        ReceiptHistoryCard(
                            transaction = tx,
                            onChangePaymentMethod = { onChangePaymentMethod(tx) },
                            onPreview = { onPreviewReceipt(tx) },
                            onPrintThermal = { onPrintThermal(tx) },
                            onSendPhotoWa = { onSendPhotoWa(tx) }
                        )
                    }
                }
            }

            // Section 2: Pembayaran Transfer
            if (uiState.selectedPaymentFilter == PaymentBookFilter.SEMUA ||
                uiState.selectedPaymentFilter == PaymentBookFilter.TRANSFER
            ) {
                item(key = "section_header_transfer") {
                    PaymentGroupHeader(
                        isCash = false,
                        count = transferList.size,
                        subtotal = transferList.sumOf { it.amountPaid },
                        modifier = Modifier.testTag("section_header_transfer")
                    )
                }

                if (transferList.isEmpty()) {
                    item(key = "empty_transfer_inline") {
                        CompactEmptyGroupNote(text = "Belum ada pelanggan yang membayar lewat Transfer / QRIS pada tampilan ini.")
                    }
                } else {
                    items(
                        items = transferList,
                        key = { "transfer_${it.id}" }
                    ) { tx ->
                        ReceiptHistoryCard(
                            transaction = tx,
                            onChangePaymentMethod = { onChangePaymentMethod(tx) },
                            onPreview = { onPreviewReceipt(tx) },
                            onPrintThermal = { onPrintThermal(tx) },
                            onSendPhotoWa = { onSendPhotoWa(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactEmptyGroupNote(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun EmptyBookkeepingCard(
    selectedFilter: PaymentBookFilter,
    periodLabel: String,
    hasSearchQuery: Boolean,
    onResetFilters: () -> Unit
) {
    val title = when {
        hasSearchQuery -> "Transaksi Tidak Ditemukan"
        selectedFilter == PaymentBookFilter.TUNAI -> "Belum Ada Pembayaran Tunai"
        selectedFilter == PaymentBookFilter.TRANSFER -> "Belum Ada Pembayaran Transfer"
        else -> "Belum Ada Riwayat Pembukuan"
    }
    val subtitle = when {
        hasSearchQuery -> "Tidak ada transaksi yang cocok dengan pencarian Anda."
        selectedFilter == PaymentBookFilter.TUNAI -> "Belum ada pelanggan yang membayar secara Tunai untuk $periodLabel."
        selectedFilter == PaymentBookFilter.TRANSFER -> "Belum ada pelanggan yang membayar melalui Transfer/QRIS untuk $periodLabel."
        else -> "Setelah Anda mengonfirmasi pembayaran di tab Tagihan, pemasukan Tunai dan Transfer akan dipisahkan otomatis di sini."
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasSearchQuery || selectedFilter != PaymentBookFilter.SEMUA) {
                OutlinedButton(onClick = onResetFilters) {
                    Text("Tampilkan Semua")
                }
            }
        }
    }
}

@Composable
private fun ReceiptHistoryCard(
    transaction: BillTransactionEntity,
    onChangePaymentMethod: () -> Unit,
    onPreview: () -> Unit,
    onPrintThermal: () -> Unit,
    onSendPhotoWa: () -> Unit
) {
    val semantic = LocalNetBillColors.current
    val method = PaymentMethod.fromString(transaction.paymentMethod)
    val accentBorder = if (method.isCash) {
        semantic.cash.copy(alpha = 0.25f)
    } else {
        semantic.transfer.copy(alpha = 0.25f)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receipt_card_${transaction.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.invoiceNumber,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PaymentMethodBadge(
                    method = method,
                    showEditHint = true,
                    onClick = onChangePaymentMethod,
                    modifier = Modifier.testTag("receipt_method_badge_${transaction.id}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = transaction.customerNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.planNameSnapshot} (${transaction.planSpeedMbpsSnapshot} Mbps) • ${IndonesianFormatters.formatMonthYear(transaction.billingMonth, transaction.billingYear)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Dibayar: ${IndonesianFormatters.formatFullDateTime(transaction.paidAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.notes.isNotBlank()) {
                        Text(
                            text = "Catatan: ${transaction.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = IndonesianFormatters.formatRupiah(transaction.amountPaid),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (method.isCash) semantic.cash else semantic.transfer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("receipt_preview_btn_${transaction.id}")
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onPrintThermal,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("receipt_print_btn_${transaction.id}")
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak Thermal", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onSendPhotoWa,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semantic.whatsappGreen,
                        contentColor = semantic.onWhatsappGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("receipt_wa_photo_btn_${transaction.id}")
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Foto WA", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}
