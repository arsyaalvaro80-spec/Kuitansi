package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.DefaultPrinterConfig
import com.example.data.local.InternetPlanEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.domain.BillingStatus
import com.example.domain.CustomerBillItem
import com.example.domain.IndonesianFormatters
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNetBillColors
import com.example.util.BluetoothThermalPrinter
import com.example.util.PairedPrinterDevice
import com.example.util.ReceiptBitmapGenerator
import kotlin.math.roundToInt

@Composable
fun NetBillSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Cari",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus pencarian"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun BillingStatusBadge(
    status: BillingStatus,
    modifier: Modifier = Modifier
) {
    val semantic = LocalNetBillColors.current
    val (bgColor, fgColor) = when (status) {
        BillingStatus.LUNAS -> semantic.paidContainer to semantic.paid
        BillingStatus.MENUNGGAK -> semantic.overdueContainer to semantic.overdue
        BillingStatus.HARI_INI -> semantic.dueTodayContainer to semantic.dueToday
        BillingStatus.MENDEKATI -> semantic.upcomingContainer to semantic.upcoming
        BillingStatus.BELUM_BAYAR -> semantic.unpaidContainer to semantic.unpaid
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fgColor)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickDueDayDialog(
    customerName: String,
    currentDueDay: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var selectedDay by remember(currentDueDay) { mutableIntStateOf(currentDueDay.coerceIn(1, 28)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Edit Jatuh Tempo",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Tanggal $selectedDay setiap bulan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
                Slider(
                    value = selectedDay.toFloat(),
                    onValueChange = { selectedDay = it.roundToInt().coerceIn(1, 28) },
                    valueRange = 1f..28f,
                    steps = 26,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(1, 5, 10, 15, 20, 25, 28).forEach { preset ->
                        Surface(
                            color = if (selectedDay == preset) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { selectedDay = preset }
                        ) {
                            Text(
                                text = "$preset",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedDay == preset) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedDay) },
                modifier = Modifier.testTag("save_quick_due_day_button")
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentConfirmDialog(
    billItem: CustomerBillItem,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, year: Int, method: PaymentMethod, amount: Long, notes: String) -> Unit
) {
    var selectedMonth by remember { mutableIntStateOf(billItem.billingMonth) }
    var selectedYear by remember { mutableIntStateOf(billItem.billingYear) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var amountText by remember { mutableStateOf(billItem.monthlyAmount.toString()) }
    var notesText by remember { mutableStateOf("") }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val yearOptions = remember(billItem.billingYear) {
        ((billItem.billingYear - 1)..(billItem.billingYear + 2)).toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Konfirmasi Pembayaran",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = billItem.customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = billItem.plan?.let { "${it.name} • ${it.speedMbps} Mbps" } ?: "Paket Internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Month & Year selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = IndonesianFormatters.monthNames[selectedMonth - 1],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bulan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            IndonesianFormatters.monthNames.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedMonth = idx + 1
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it },
                        modifier = Modifier.weight(0.9f)
                    ) {
                        OutlinedTextField(
                            value = selectedYear.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tahun") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            yearOptions.forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr.toString()) },
                                    onClick = {
                                        selectedYear = yr
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Payment Method chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                leadingIcon = {
                                    Icon(
                                        imageVector = paymentMethodIcon(method),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(method.label) },
                                modifier = Modifier.testTag("payment_method_${method.name.lowercase()}")
                            )
                        }
                    }
                    val semantic = LocalNetBillColors.current
                    val groupAccent = if (selectedMethod.isCash) semantic.cash else semantic.transfer
                    val groupBg = if (selectedMethod.isCash) semantic.cashContainer else semantic.transferContainer
                    Surface(
                        color = groupBg.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = paymentMethodIcon(selectedMethod),
                                contentDescription = null,
                                tint = groupAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tercatat di pembukuan ${selectedMethod.bookkeepingGroupLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = groupAccent,
                                fontWeight = FontWeight.SemiBold
                            )
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
                        .testTag("payment_amount_input")
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan (Opsional)") },
                    placeholder = { Text("Misal: titip tetangga / transfer BCA") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null || amount <= 0L) {
                        amountError = "Masukkan nominal yang valid"
                    } else {
                        onConfirm(selectedMonth, selectedYear, selectedMethod, amount, notesText)
                    }
                },
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Bayar & Lunasi")
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
fun ReceiptPreviewDialog(
    transaction: BillTransactionEntity,
    profile: IspProfileEntity,
    onDismiss: () -> Unit,
    onPrintThermal: () -> Unit,
    onShareWhatsAppPhoto: () -> Unit,
    onSaveToGallery: () -> Unit
) {
    val semantic = LocalNetBillColors.current
    val receiptBitmap = remember(transaction, profile) {
        ReceiptBitmapGenerator.generateReceiptBitmap(transaction, profile)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preview Kuitansi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = transaction.invoiceNumber,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup preview")
                    }
                }
                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = receiptBitmap.asImageBitmap(),
                        contentDescription = "Preview struk kuitansi ${transaction.invoiceNumber}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onPrintThermal,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_print_thermal_button"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cetak Thermal", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = onShareWhatsAppPhoto,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = semantic.whatsappGreen,
                                contentColor = semantic.onWhatsappGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_share_wa_photo_button"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Foto WA", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    OutlinedButton(
                        onClick = onSaveToGallery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_save_gallery_button"),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Gambar ke Galeri HP")
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothPrinterPickerDialog(
    transaction: BillTransactionEntity,
    profile: IspProfileEntity,
    defaultPrinter: DefaultPrinterConfig?,
    isPrinting: Boolean,
    onDismiss: () -> Unit,
    onSelectDevice: (PairedPrinterDevice) -> Unit
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf<List<PairedPrinterDevice>>(emptyList()) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(BluetoothThermalPrinter.hasBluetoothPermissions(context)) }

    fun refreshDevices() {
        hasPermission = BluetoothThermalPrinter.hasBluetoothPermissions(context)
        if (!hasPermission) {
            statusError = "Aplikasi memerlukan izin Bluetooth (Android 12+) untuk mendeteksi printer thermal."
            pairedDevices = emptyList()
            return
        }
        val result = BluetoothThermalPrinter.getPairedDevices(context)
        result.onSuccess { list ->
            pairedDevices = list
            statusError = if (list.isEmpty()) {
                "Belum ada printer Bluetooth yang dipasangkan (paired). Silakan pasangkan printer di Pengaturan Bluetooth HP terlebih dahulu."
            } else {
                null
            }
        }.onFailure { err ->
            pairedDevices = emptyList()
            statusError = err.message ?: "Gagal membaca perangkat Bluetooth."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshDevices()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && BluetoothThermalPrinter.requiredPermissions().isNotEmpty()) {
            permissionLauncher.launch(BluetoothThermalPrinter.requiredPermissions())
        } else {
            refreshDevices()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isPrinting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Cetak Printer Thermal",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.customerNameSnapshot,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kertas ${profile.printerPaperWidthMm} mm (${if (profile.printerPaperWidthMm >= 80) 48 else 32} kar)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = IndonesianFormatters.formatRupiah(transaction.amountPaid),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isPrinting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Mengirim perintah cetak ESC/POS…")
                    }
                } else {
                    if (!hasPermission && BluetoothThermalPrinter.requiredPermissions().isNotEmpty()) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(BluetoothThermalPrinter.requiredPermissions())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.BluetoothSearching, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Izinkan Akses Bluetooth")
                        }
                    }

                    if (statusError != null) {
                        Text(
                            text = statusError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (defaultPrinter != null && hasPermission) {
                        Button(
                            onClick = {
                                onSelectDevice(
                                    PairedPrinterDevice(
                                        name = defaultPrinter.name,
                                        address = defaultPrinter.address
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_print_default_printer_button")
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Cetak Cepat: ${defaultPrinter.name}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (pairedDevices.isNotEmpty()) {
                        Text(
                            text = "Pilih Perangkat Terpasang (Paired):",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pairedDevices, key = { it.address }) { device ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectDevice(device) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Print,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = device.address,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { refreshDevices() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Segarkan")
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Bluetooth")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isPrinting
            ) {
                Text("Tutup")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormDialog(
    initialCustomer: CustomerEntity?,
    plans: List<InternetPlanEntity>,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit,
    onDelete: ((CustomerEntity) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var selectedPlanId by remember {
        mutableLongStateOf(initialCustomer?.planId ?: plans.firstOrNull()?.id ?: 0L)
    }
    var dueDayText by remember { mutableStateOf((initialCustomer?.dueDay ?: 10).toString()) }
    var isActive by remember { mutableStateOf(initialCustomer?.isActive ?: true) }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var planDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()

    if (showDeleteConfirm && initialCustomer != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Pelanggan?") },
            text = {
                Text("Pelanggan \"${initialCustomer.name}\" akan dihapus dari daftar. Riwayat kuitansi yang sudah lunas tetap aman tersimpan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialCustomer)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Hapus")
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
                    text = if (initialCustomer == null) "Tambah Pelanggan" else "Edit Pelanggan",
                    style = MaterialTheme.typography.titleLarge
                )
                if (initialCustomer != null && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_customer_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Hapus pelanggan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
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
                if (errorMsg != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama Pelanggan *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP / WhatsApp") },
                    placeholder = { Text("0812xxxxxxxx atau 62812xxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat / Blok / RT-RW") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input")
                )

                ExposedDropdownMenuBox(
                    expanded = planDropdownExpanded,
                    onExpandedChange = { planDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPlan?.let {
                            "${it.name} (${it.speedMbps} Mbps • ${IndonesianFormatters.formatRupiah(it.price)})"
                        } ?: "Belum ada paket",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paket Internet *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = planDropdownExpanded,
                        onDismissRequest = { planDropdownExpanded = false }
                    ) {
                        plans.forEach { plan ->
                            DropdownMenuItem(
                                text = {
                                    Text("${plan.name} (${plan.speedMbps} Mbps • ${IndonesianFormatters.formatRupiah(plan.price)})")
                                },
                                onClick = {
                                    selectedPlanId = plan.id
                                    planDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { input ->
                        dueDayText = input.filter { it.isDigit() }.take(2)
                        errorMsg = null
                    },
                    label = { Text("Tanggal Jatuh Tempo (1–28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_dueday_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Status Pelanggan",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isActive) "Aktif — masuk daftar tagihan bulanan" else "Nonaktif — sementara tidak ditagih",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            modifier = Modifier.testTag("customer_active_switch")
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (Opsional)") },
                    placeholder = { Text("Misal: ODP Tiang 04, Router TP-Link") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val parsedDue = dueDayText.toIntOrNull()
                    val resolvedPlanId = selectedPlan?.id ?: 0L
                    when {
                        trimmedName.isEmpty() -> errorMsg = "Nama pelanggan wajib diisi"
                        resolvedPlanId == 0L -> errorMsg = "Tambahkan paket internet terlebih dahulu"
                        parsedDue == null || parsedDue !in 1..28 -> errorMsg = "Tanggal jatuh tempo harus antara 1 sampai 28"
                        else -> {
                            onSave(
                                CustomerEntity(
                                    id = initialCustomer?.id ?: 0L,
                                    name = trimmedName,
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    pppoeOrIp = "",
                                    planId = resolvedPlanId,
                                    dueDay = parsedDue,
                                    isActive = isActive,
                                    notes = notes.trim(),
                                    createdAt = initialCustomer?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
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
