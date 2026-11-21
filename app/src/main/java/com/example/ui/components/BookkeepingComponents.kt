package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InternetPlanEntity
import com.example.data.local.PaymentMethod
import com.example.domain.BookkeepingSummary
import com.example.domain.IndonesianFormatters
import com.example.domain.PaymentBookFilter
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNetBillColors
import kotlin.math.roundToInt

fun paymentMethodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.TUNAI -> Icons.Rounded.Payments
    PaymentMethod.TRANSFER_BANK -> Icons.Rounded.AccountBalance
    PaymentMethod.QRIS -> Icons.Rounded.QrCode2
}

@Composable
fun PaymentMethodBadge(
    method: PaymentMethod,
    modifier: Modifier = Modifier,
    showEditHint: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val semantic = LocalNetBillColors.current
    val bgColor = if (method.isCash) semantic.cashContainer else semantic.transferContainer
    val fgColor = if (method.isCash) semantic.cash else semantic.transfer

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = paymentMethodIcon(method),
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = method.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fgColor,
                maxLines = 1
            )
            if (showEditHint && onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit atau batalkan pembayaran",
                    tint = fgColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun BookkeepingBreakdownCard(
    summary: BookkeepingSummary,
    selectedFilter: PaymentBookFilter,
    onSelectFilter: (PaymentBookFilter) -> Unit,
    onShareReport: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalNetBillColors.current
    val cashFraction = if (summary.totalIncome > 0L) {
        (summary.cashIncome.toFloat() / summary.totalIncome.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val transferFraction = if (summary.totalIncome > 0L) {
        (1f - cashFraction).coerceIn(0f, 1f)
    } else {
        0f
    }
    val cashPercent = (cashFraction * 100f).roundToInt()
    val transferPercent = if (summary.totalIncome > 0L) 100 - cashPercent else 0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Total Row
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TOTAL PEMBUKUAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = IndonesianFormatters.formatRupiah(summary.totalIncome),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${summary.totalCount} transaksi lunas tercatat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Export Actions: Share Text Recap + Export CSV Excel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onShareReport,
                    enabled = summary.totalCount > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_bookkeeping_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Rekap Teks",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onExportCsv,
                    enabled = summary.totalCount > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_bookkeeping_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Export CSV",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }

            // Two-Column Split: Kas Tunai vs Rekening Transfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BookkeepingBucketCard(
                    title = "Kas Tunai",
                    amount = summary.cashIncome,
                    count = summary.cashCount,
                    percent = cashPercent,
                    icon = Icons.Rounded.Payments,
                    accent = semantic.cash,
                    containerColor = semantic.cashContainer,
                    isSelected = selectedFilter == PaymentBookFilter.TUNAI,
                    onClick = {
                        val next = if (selectedFilter == PaymentBookFilter.TUNAI) {
                            PaymentBookFilter.SEMUA
                        } else {
                            PaymentBookFilter.TUNAI
                        }
                        onSelectFilter(next)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bookkeeping_card_tunai")
                )

                BookkeepingBucketCard(
                    title = "Transfer",
                    amount = summary.transferIncome,
                    count = summary.transferCount,
                    percent = transferPercent,
                    icon = Icons.Rounded.AccountBalance,
                    accent = semantic.transfer,
                    containerColor = semantic.transferContainer,
                    isSelected = selectedFilter == PaymentBookFilter.TRANSFER,
                    onClick = {
                        val next = if (selectedFilter == PaymentBookFilter.TRANSFER) {
                            PaymentBookFilter.SEMUA
                        } else {
                            PaymentBookFilter.TRANSFER
                        }
                        onSelectFilter(next)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bookkeeping_card_transfer")
                )
            }

            // Proportional Split Bar (Tunai vs Transfer)
            if (summary.totalIncome > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        if (cashFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(cashFraction)
                                    .fillMaxHeight()
                                    .background(semantic.cash)
                            )
                        }
                        if (transferFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(transferFraction)
                                    .fillMaxHeight()
                                    .background(semantic.transfer)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(semantic.cash)
                            )
                            Text(
                                text = "Tunai $cashPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Transfer $transferPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(semantic.transfer)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookkeepingBucketCard(
    title: String,
    amount: Long,
    count: Int,
    percent: Int,
    icon: ImageVector,
    accent: Color,
    containerColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) containerColor else containerColor.copy(alpha = 0.55f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent else accent.copy(alpha = 0.28f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1
                    )
                }
                if (percent > 0) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            Text(
                text = IndonesianFormatters.formatRupiah(amount),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$count orang bayar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PaymentGroupHeader(
    isCash: Boolean,
    count: Int,
    subtotal: Long,
    modifier: Modifier = Modifier
) {
    val semantic = LocalNetBillColors.current
    val accent = if (isCash) semantic.cash else semantic.transfer
    val container = if (isCash) semantic.cashContainer else semantic.transferContainer
    val title = if (isCash) "Pembayaran Tunai" else "Pembayaran Transfer"
    val subtitle = if (isCash) "Uang kas langsung" else "Transfer Bank & QRIS"
    val icon = if (isCash) Icons.Rounded.Payments else Icons.Rounded.AccountBalance

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "$title ($count)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = IndonesianFormatters.formatRupiah(subtotal),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
fun EditTransactionDialog(
    transaction: BillTransactionEntity,
    onDismiss: () -> Unit,
    onSave: (amountPaid: Long, method: PaymentMethod, notes: String) -> Unit,
    onDelete: () -> Unit
) {
    val semantic = LocalNetBillColors.current
    val initialMethod = PaymentMethod.fromString(transaction.paymentMethod)
    var selectedMethod by remember(transaction.id) { mutableStateOf(initialMethod) }
    var amountText by remember(transaction.id) { mutableStateOf(transaction.amountPaid.toString()) }
    var notesText by remember(transaction.id) { mutableStateOf(transaction.notes) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Batalkan Status Lunas?") },
            text = {
                Text(
                    "Transaksi ${transaction.invoiceNumber} atas nama \"${transaction.customerNameSnapshot}\" (${IndonesianFormatters.formatRupiah(transaction.amountPaid)}) akan dihapus dan tagihan kembali berstatus belum lunas."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_undo_payment_button")
                ) {
                    Text("Ya, Batalkan Lunas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Transaksi",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("undo_payment_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Batalkan status lunas",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = transaction.customerNameSnapshot,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${transaction.invoiceNumber} • ${IndonesianFormatters.formatMonthYear(transaction.billingMonth, transaction.billingYear)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PaymentMethod.entries.forEach { method ->
                    val isSelected = selectedMethod == method
                    val accent = if (method.isCash) semantic.cash else semantic.transfer
                    val container = if (method.isCash) semantic.cashContainer else semantic.transferContainer

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) container else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMethod = method }
                            .testTag("change_method_option_${method.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = paymentMethodIcon(method),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = method.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pembukuan ${method.bookkeepingGroupLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Dipilih",
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }
                        amountError = null
                    },
                    label = { Text("Nominal Bayar (Rp)") },
                    prefix = { Text("Rp ") },
                    isError = amountError != null,
                    supportingText = amountError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_transaction_amount_input")
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan (Opsional)") },
                    placeholder = { Text("Misal: transfer BCA a.n. Ibu Ani") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_transaction_notes_input")
                )

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("undo_payment_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Batalkan Lunas / Hapus Transaksi")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null || amount <= 0L) {
                        amountError = "Masukkan nominal yang valid"
                    } else {
                        onSave(amount, selectedMethod, notesText)
                    }
                },
                modifier = Modifier.testTag("confirm_change_method_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun CustomerHistoryDialog(
    customer: CustomerEntity,
    plan: InternetPlanEntity?,
    transactions: List<BillTransactionEntity>,
    onDismiss: () -> Unit,
    onEditCustomer: () -> Unit,
    onPreviewReceipt: (BillTransactionEntity) -> Unit,
    onEditTransaction: (BillTransactionEntity) -> Unit
) {
    val semantic = LocalNetBillColors.current
    val totalPaid = transactions.sumOf { it.amountPaid }
    val cashCount = transactions.count { PaymentMethod.fromString(it.paymentMethod).isCash }
    val transferCount = transactions.count { PaymentMethod.fromString(it.paymentMethod).isTransfer }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = plan?.let { "${it.name} • ${it.speedMbps} Mbps" } ?: "Riwayat Pembayaran",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup riwayat")
                    }
                }

                HorizontalDivider()

                // Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Total Terbayar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = IndonesianFormatters.formatRupiah(totalPaid),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        color = semantic.cashContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Tunai",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.cash
                            )
                            Text(
                                text = "${cashCount}x",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = semantic.cash
                            )
                        }
                    }
                    Surface(
                        color = semantic.transferContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Transfer",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.transfer
                            )
                            Text(
                                text = "${transferCount}x",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = semantic.transfer
                            )
                        }
                    }
                }

                // Transaction list
                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Belum Ada Riwayat Pembayaran",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pembayaran yang dikonfirmasi untuk pelanggan ini akan muncul berurutan per bulan di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions, key = { it.id }) { tx ->
                            val method = PaymentMethod.fromString(tx.paymentMethod)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = IndonesianFormatters.formatMonthYear(tx.billingMonth, tx.billingYear),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = IndonesianFormatters.formatFullDateTime(tx.paidAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        PaymentMethodBadge(
                                            method = method,
                                            showEditHint = true,
                                            onClick = { onEditTransaction(tx) }
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = IndonesianFormatters.formatRupiah(tx.amountPaid),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (method.isCash) semantic.cash else semantic.transfer
                                        )
                                        OutlinedButton(
                                            onClick = { onPreviewReceipt(tx) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Struk", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }
                    Button(
                        onClick = onEditCustomer,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("history_edit_customer_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Pelanggan")
                    }
                }
            }
        }
    }
}
