package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BillTransactionEntity
import com.example.data.local.ThemeMode
import com.example.domain.BillFilter
import com.example.domain.BillingCalculator
import com.example.domain.BillingStatus
import com.example.domain.CustomerBillItem
import com.example.domain.IndonesianFormatters
import com.example.ui.components.BillingStatusBadge
import com.example.ui.components.NetBillSearchBar
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNetBillColors
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun TagihanScreen(
    uiState: DashboardUiState,
    themeMode: ThemeMode,
    onShiftPeriod: (Int) -> Unit,
    onCycleTheme: () -> Unit,
    onTestNotification: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (BillFilter) -> Unit,
    onTapEditDueDay: (CustomerBillItem) -> Unit,
    onPayBill: (CustomerBillItem) -> Unit,
    onEditPaymentMethod: (BillTransactionEntity) -> Unit,
    onSendWhatsAppReminder: (CustomerBillItem) -> Unit,
    onPrintReceipt: (BillTransactionEntity) -> Unit,
    onSendReceiptPhotoWa: (BillTransactionEntity) -> Unit,
    onAddCustomerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalNetBillColors.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ISP Top Header
            item(key = "header") {
                DashboardHeader(
                    ispName = uiState.ispProfile.businessName,
                    periodLabel = IndonesianFormatters.formatMonthYear(
                        uiState.period.month,
                        uiState.period.year
                    ),
                    themeMode = themeMode,
                    onPrevMonth = { onShiftPeriod(-1) },
                    onNextMonth = { onShiftPeriod(1) },
                    onCycleTheme = onCycleTheme,
                    onTestNotification = onTestNotification
                )
            }

            // 2. Monthly Summary Hero Card
            item(key = "summary") {
                MonthlySummaryHeroCard(
                    uiState = uiState,
                    onSelectFilter = onSelectFilter
                )
            }

            // 3. Search Bar
            item(key = "search") {
                NetBillSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = "Cari nama, alamat, atau no. HP…",
                    testTag = "dashboard_search_input"
                )
            }

            // 4. Filter Chips
            item(key = "filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillFilter.entries.forEach { filter ->
                        val count = when (filter) {
                            BillFilter.SEMUA -> uiState.summary.totalActiveCount
                            BillFilter.MENUNGGAK -> uiState.summary.overdueCount
                            BillFilter.HARI_INI -> uiState.summary.dueTodayCount
                            BillFilter.MENDEKATI -> uiState.summary.upcomingCount
                            BillFilter.BELUM_BAYAR -> uiState.summary.unpaidCount
                            BillFilter.LUNAS -> uiState.summary.paidCount
                            BillFilter.LUNAS_TUNAI -> uiState.summary.cashPaidCount
                            BillFilter.LUNAS_TRANSFER -> uiState.summary.transferPaidCount
                        }
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { onSelectFilter(filter) },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            // 5. Empty State or Customer Bill Cards
            if (uiState.filteredBills.isEmpty()) {
                item(key = "empty") {
                    EmptyBillsCard(
                        hasAnyCustomer = uiState.allBills.isNotEmpty(),
                        selectedFilter = uiState.selectedFilter,
                        onAddCustomerClick = onAddCustomerClick,
                        onResetFilter = {
                            onSearchChange("")
                            onSelectFilter(BillFilter.SEMUA)
                        }
                    )
                }
            } else {
                items(
                    items = uiState.filteredBills,
                    key = { it.customer.id }
                ) { item ->
                    CustomerBillCard(
                        item = item,
                        whatsappGreen = semantic.whatsappGreen,
                        onWhatsappGreen = semantic.onWhatsappGreen,
                        onEditDueDay = { onTapEditDueDay(item) },
                        onPay = { onPayBill(item) },
                        onEditPaymentMethod = onEditPaymentMethod,
                        onSendWaReminder = { onSendWhatsAppReminder(item) },
                        onPrintReceipt = { tx -> onPrintReceipt(tx) },
                        onSendPhotoWa = { tx -> onSendReceiptPhotoWa(tx) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddCustomerClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dashboard_add_customer_fab")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Tambah Pelanggan Baru"
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    ispName: String,
    periodLabel: String,
    themeMode: ThemeMode,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCycleTheme: () -> Unit,
    onTestNotification: () -> Unit
) {
    val themeIcon = when (themeMode) {
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ispName.ifBlank { "NetBill ISP" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Manajemen Tagihan RT/RW Net",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCycleTheme)
                        .testTag("dashboard_theme_toggle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Ganti tema (${themeMode.label})",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onTestNotification,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("dashboard_test_notif_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Tes Notif",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Period selector bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.testTag("period_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Bulan sebelumnya"
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PERIODE TAGIHAN",
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
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("period_next_button")
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

@Composable
private fun MonthlySummaryHeroCard(
    uiState: DashboardUiState,
    onSelectFilter: (BillFilter) -> Unit
) {
    val semantic = LocalNetBillColors.current
    val summary = uiState.summary
    val progress = if (summary.totalActiveCount > 0) {
        summary.paidCount.toFloat() / summary.totalActiveCount.toFloat()
    } else {
        0f
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(semantic.heroGradientStart, semantic.heroGradientEnd)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pemasukan Bulan Ini",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = IndonesianFormatters.formatRupiah(summary.totalIncome),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Lunas",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${summary.paidCount} / ${summary.totalActiveCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color(0xFF5EEAD4),
                    trackColor = Color.White.copy(alpha = 0.22f)
                )

                // Target & Unpaid Receivable (Piutang) Strip
                Surface(
                    color = Color.White.copy(alpha = 0.11f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectFilter(BillFilter.BELUM_BAYAR) }
                        .testTag("hero_piutang_strip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Target Bulan Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = IndonesianFormatters.formatRupiah(summary.expectedTotalIncome),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Sisa Belum Bayar (${summary.unpaidCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                            Text(
                                text = IndonesianFormatters.formatRupiah(summary.unpaidAmount),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.unpaidAmount > 0L) Color(0xFFFDE68A) else Color(0xFF5EEAD4)
                            )
                        }
                    }
                }

                // Separated Cash vs Transfer bookkeeping tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroPaymentMethodTile(
                        label = "Tunai",
                        amount = summary.cashIncome,
                        count = summary.cashPaidCount,
                        icon = Icons.Rounded.Payments,
                        accent = Color(0xFF5EEAD4),
                        selected = uiState.selectedFilter == BillFilter.LUNAS_TUNAI,
                        onClick = {
                            val next = if (uiState.selectedFilter == BillFilter.LUNAS_TUNAI) {
                                BillFilter.SEMUA
                            } else {
                                BillFilter.LUNAS_TUNAI
                            }
                            onSelectFilter(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_tunai_summary_tile")
                    )
                    HeroPaymentMethodTile(
                        label = "Transfer",
                        amount = summary.transferIncome,
                        count = summary.transferPaidCount,
                        icon = Icons.Rounded.AccountBalance,
                        accent = Color(0xFF93C5FD),
                        selected = uiState.selectedFilter == BillFilter.LUNAS_TRANSFER,
                        onClick = {
                            val next = if (uiState.selectedFilter == BillFilter.LUNAS_TRANSFER) {
                                BillFilter.SEMUA
                            } else {
                                BillFilter.LUNAS_TRANSFER
                            }
                            onSelectFilter(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_transfer_summary_tile")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMiniIndicator(
                        label = "Menunggak",
                        count = summary.overdueCount,
                        accent = Color(0xFFFCA5A5),
                        onClick = { onSelectFilter(BillFilter.MENUNGGAK) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMiniIndicator(
                        label = "Hari Ini",
                        count = summary.dueTodayCount,
                        accent = Color(0xFFFDE68A),
                        onClick = { onSelectFilter(BillFilter.HARI_INI) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMiniIndicator(
                        label = "H-3 Dekat",
                        count = summary.upcomingCount,
                        accent = Color(0xFFBAE6FD),
                        onClick = { onSelectFilter(BillFilter.MENDEKATI) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPaymentMethodTile(
    label: String,
    amount: Long,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else Color.White.copy(alpha = 0.16f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
                Text(
                    text = "$count org",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = IndonesianFormatters.formatRupiah(amount),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryMiniIndicator(
    label: String,
    count: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$count plg",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CustomerBillCard(
    item: CustomerBillItem,
    whatsappGreen: Color,
    onWhatsappGreen: Color,
    onEditDueDay: () -> Unit,
    onPay: () -> Unit,
    onEditPaymentMethod: (BillTransactionEntity) -> Unit,
    onSendWaReminder: () -> Unit,
    onPrintReceipt: (BillTransactionEntity) -> Unit,
    onSendPhotoWa: (BillTransactionEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bill_card_${item.customer.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Name + Status Badge + Payment Method Badge (when Lunas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.customer.address.isNotBlank()) {
                        Text(
                            text = item.customer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BillingStatusBadge(status = item.status)
                    val tx = item.transaction
                    val method = item.paymentMethod
                    if (item.status == BillingStatus.LUNAS && tx != null && method != null) {
                        PaymentMethodBadge(
                            method = method,
                            showEditHint = true,
                            onClick = { onEditPaymentMethod(tx) },
                            modifier = Modifier.testTag("bill_payment_method_badge_${item.customer.id}")
                        )
                    }
                }
            }

            // Middle Info: Plan Speed + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${item.plan?.speedMbps ?: 0} Mbps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = item.plan?.name ?: "Tanpa Paket",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = IndonesianFormatters.formatRupiah(item.monthlyAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Tappable Due Date Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onEditDueDay)
                    .testTag("bill_due_date_chip_${item.customer.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = "Edit tanggal jatuh tempo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Jatuh tempo: ${item.dueDateLabel}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text(
                        text = BillingCalculator.describeDueOffset(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            if (item.status != BillingStatus.LUNAS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPay,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("pay_bill_button_${item.customer.id}"),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bayar")
                    }

                    OutlinedButton(
                        onClick = onSendWaReminder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wa_reminder_button_${item.customer.id}"),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = whatsappGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ingatkan WA", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else if (item.transaction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onPrintReceipt(item.transaction) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_button_${item.customer.id}"),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak Struk", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { onSendPhotoWa(item.transaction) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = whatsappGreen,
                            contentColor = onWhatsappGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("photo_receipt_wa_button_${item.customer.id}"),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Foto Struk", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBillsCard(
    hasAnyCustomer: Boolean,
    selectedFilter: BillFilter,
    onAddCustomerClick: () -> Unit,
    onResetFilter: () -> Unit
) {
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
                imageVector = Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = if (!hasAnyCustomer) {
                    "Belum Ada Pelanggan Aktif"
                } else {
                    "Tidak Ada Tagihan (${selectedFilter.label})"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (!hasAnyCustomer) {
                    "Tambahkan pelanggan RT/RW Net pertama Anda dengan tombol + untuk mulai mengelola tagihan bulanan secara offline."
                } else {
                    "Tidak ditemukan data tagihan yang cocok dengan pencarian atau filter saat ini."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            if (!hasAnyCustomer) {
                Button(onClick = onAddCustomerClick) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah Pelanggan")
                }
            } else {
                OutlinedButton(onClick = onResetFilter) {
                    Text("Tampilkan Semua Tagihan")
                }
            }
        }
    }
}
