package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.NetBillDatabase
import com.example.data.local.ThemePreferences
import com.example.data.repository.NetBillRepository
import com.example.domain.CustomerBillItem
import com.example.ui.components.BluetoothPrinterPickerDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.CustomerHistoryDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.PaymentConfirmDialog
import com.example.ui.components.QuickDueDayDialog
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.screens.KuitansiScreen
import com.example.ui.screens.PaketScreen
import com.example.ui.screens.PelangganScreen
import com.example.ui.screens.SettingScreen
import com.example.ui.screens.TagihanScreen
import com.example.ui.theme.NetBillTheme
import com.example.ui.viewmodel.NetBillViewModel
import com.example.util.BillingNotificationHelper

enum class NetBillTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TAGIHAN("Tagihan", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_tab_tagihan"),
    PELANGGAN("Pelanggan", Icons.Rounded.People, Icons.Outlined.People, "nav_tab_pelanggan"),
    PAKET("Paket Mbps", Icons.Rounded.Speed, Icons.Outlined.Speed, "nav_tab_paket"),
    KUITANSI("Pembukuan", Icons.Rounded.Description, Icons.Outlined.Description, "nav_tab_kuitansi"),
    SETTING("Setting", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_tab_setting")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BillingNotificationHelper.ensureChannel(this)

        val database = NetBillDatabase.getInstance(applicationContext)
        val repository = NetBillRepository(
            database = database,
            planDao = database.internetPlanDao(),
            customerDao = database.customerDao(),
            transactionDao = database.billTransactionDao(),
            profileDao = database.ispProfileDao(),
            themePreferences = ThemePreferences(applicationContext)
        )
        val factory = NetBillViewModel.Factory(repository)

        setContent {
            val viewModel: NetBillViewModel = viewModel(factory = factory)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            NetBillTheme(themeMode = themeMode) {
                NetBillRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun NetBillRootApp(viewModel: NetBillViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val defaultPrinter by viewModel.defaultPrinter.collectAsStateWithLifecycle()
    val ispProfile by viewModel.ispProfile.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val plansWithCount by viewModel.plansWithCount.collectAsStateWithLifecycle()
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val customerSearch by viewModel.customerSearch.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val bookkeepingState by viewModel.bookkeepingUiState.collectAsStateWithLifecycle()
    val previewTransaction by viewModel.previewTransaction.collectAsStateWithLifecycle()
    val printerTransaction by viewModel.printerTransaction.collectAsStateWithLifecycle()
    val isPrinting by viewModel.isPrinting.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NetBillTab.TAGIHAN) }
    var showCreateCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var historyCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var quickDueTarget by remember { mutableStateOf<CustomerEntity?>(null) }
    var payingBillItem by remember { mutableStateOf<CustomerBillItem?>(null) }
    var editingTransaction by remember { mutableStateOf<BillTransactionEntity?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.triggerDueNotification(context)
        } else {
            viewModel.notifyMessage("Izin notifikasi ditolak. Aktifkan di Pengaturan Aplikasi untuk menerima pengingat.")
        }
    }

    fun triggerNotificationWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !BillingNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.triggerDueNotification(context)
        }
    }

    // Return to Tagihan tab on system back when viewing any secondary tab
    BackHandler(enabled = currentTab != NetBillTab.TAGIHAN) {
        currentTab = NetBillTab.TAGIHAN
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        NetBillTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        NetBillTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()

                    when (currentTab) {
                        NetBillTab.TAGIHAN -> TagihanScreen(
                            uiState = dashboardState,
                            themeMode = themeMode,
                            onShiftPeriod = viewModel::shiftBillingPeriod,
                            onCycleTheme = viewModel::cycleThemeMode,
                            onTestNotification = ::triggerNotificationWithPermissionCheck,
                            onSearchChange = viewModel::updateDashboardSearch,
                            onSelectFilter = viewModel::selectDashboardFilter,
                            onTapEditDueDay = { item -> quickDueTarget = item.customer },
                            onPayBill = { item -> payingBillItem = item },
                            onEditPaymentMethod = { tx -> editingTransaction = tx },
                            onSendWhatsAppReminder = { item ->
                                viewModel.sendWhatsAppReminder(context, item)
                            },
                            onPrintReceipt = { tx -> viewModel.openReceiptPreview(tx) },
                            onSendReceiptPhotoWa = { tx ->
                                viewModel.shareReceiptPhotoToWhatsApp(context, tx)
                            },
                            onAddCustomerClick = { showCreateCustomerDialog = true },
                            modifier = contentModifier
                        )

                        NetBillTab.PELANGGAN -> PelangganScreen(
                            customers = filteredCustomers,
                            plans = plans,
                            allTransactions = allTransactions,
                            searchQuery = customerSearch,
                            onSearchChange = viewModel::updateCustomerSearch,
                            onAddCustomer = { showCreateCustomerDialog = true },
                            onViewHistory = { customer -> historyCustomer = customer },
                            onEditCustomer = { customer -> editingCustomer = customer },
                            onQuickEditDueDay = { customer -> quickDueTarget = customer },
                            modifier = contentModifier
                        )

                        NetBillTab.PAKET -> PaketScreen(
                            plansWithCount = plansWithCount,
                            onSavePlan = viewModel::savePlan,
                            onDeletePlan = viewModel::deletePlan,
                            modifier = contentModifier
                        )

                        NetBillTab.KUITANSI -> KuitansiScreen(
                            uiState = bookkeepingState,
                            onShiftPeriod = viewModel::shiftBillingPeriod,
                            onTogglePeriodFilter = viewModel::setReceiptFilterByPeriod,
                            onSelectPaymentFilter = viewModel::selectReceiptPaymentFilter,
                            onSearchChange = viewModel::updateReceiptSearch,
                            onShareBookkeepingReport = {
                                viewModel.shareBookkeepingReport(context)
                            },
                            onExportCsv = {
                                viewModel.exportBookkeepingCsv(context)
                            },
                            onChangePaymentMethod = { tx -> editingTransaction = tx },
                            onPreviewReceipt = { tx -> viewModel.openReceiptPreview(tx) },
                            onPrintThermal = { tx -> viewModel.openPrinterPicker(tx) },
                            onSendPhotoWa = { tx ->
                                viewModel.shareReceiptPhotoToWhatsApp(context, tx)
                            },
                            modifier = contentModifier
                        )

                        NetBillTab.SETTING -> SettingScreen(
                            themeMode = themeMode,
                            profile = ispProfile,
                            defaultPrinter = defaultPrinter,
                            onSelectThemeMode = viewModel::setThemeMode,
                            onSaveProfile = viewModel::saveProfile,
                            onClearDefaultPrinter = viewModel::clearDefaultPrinter,
                            onExportBackupUri = { uri -> viewModel.exportBackupToUri(context, uri) },
                            onRestoreBackupUri = { uri -> viewModel.restoreBackupFromUri(context, uri) },
                            onTriggerNotification = ::triggerNotificationWithPermissionCheck,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }

    // Shared Dialogs
    if (showCreateCustomerDialog) {
        CustomerFormDialog(
            initialCustomer = null,
            plans = plans,
            onDismiss = { showCreateCustomerDialog = false },
            onSave = { customer ->
                viewModel.saveCustomer(customer)
                showCreateCustomerDialog = false
            }
        )
    }

    historyCustomer?.let { target ->
        val customerTx = remember(allTransactions, target.id) {
            allTransactions.filter { it.customerId == target.id }
        }
        CustomerHistoryDialog(
            customer = target,
            plan = plans.firstOrNull { it.id == target.planId },
            transactions = customerTx,
            onDismiss = { historyCustomer = null },
            onEditCustomer = {
                historyCustomer = null
                editingCustomer = target
            },
            onPreviewReceipt = { tx ->
                viewModel.openReceiptPreview(tx)
            },
            onEditTransaction = { tx ->
                editingTransaction = tx
            }
        )
    }

    editingCustomer?.let { target ->
        CustomerFormDialog(
            initialCustomer = target,
            plans = plans,
            onDismiss = { editingCustomer = null },
            onSave = { updated ->
                viewModel.saveCustomer(updated)
                editingCustomer = null
            },
            onDelete = { toDelete ->
                viewModel.deleteCustomer(toDelete)
                editingCustomer = null
            }
        )
    }

    quickDueTarget?.let { target ->
        QuickDueDayDialog(
            customerName = target.name,
            currentDueDay = target.dueDay,
            onDismiss = { quickDueTarget = null },
            onSave = { newDay ->
                viewModel.updateCustomerDueDay(target.id, target.name, newDay)
                quickDueTarget = null
            }
        )
    }

    payingBillItem?.let { item ->
        PaymentConfirmDialog(
            billItem = item,
            onDismiss = { payingBillItem = null },
            onConfirm = { month, year, method, amount, notes ->
                viewModel.confirmPayment(
                    customer = item.customer,
                    plan = item.plan,
                    billingMonth = month,
                    billingYear = year,
                    amountPaid = amount,
                    paymentMethod = method,
                    notes = notes
                )
                payingBillItem = null
            }
        )
    }

    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            onDismiss = { editingTransaction = null },
            onSave = { amount, newMethod, notes ->
                viewModel.updateTransactionDetails(tx, amount, newMethod, notes)
                editingTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(tx)
                editingTransaction = null
            }
        )
    }

    previewTransaction?.let { tx ->
        ReceiptPreviewDialog(
            transaction = tx,
            profile = ispProfile,
            onDismiss = viewModel::dismissReceiptPreview,
            onPrintThermal = {
                viewModel.dismissReceiptPreview()
                viewModel.openPrinterPicker(tx)
            },
            onShareWhatsAppPhoto = {
                viewModel.shareReceiptPhotoToWhatsApp(context, tx)
            },
            onSaveToGallery = {
                viewModel.saveReceiptToGallery(context, tx)
            }
        )
    }

    printerTransaction?.let { tx ->
        BluetoothPrinterPickerDialog(
            transaction = tx,
            profile = ispProfile,
            defaultPrinter = defaultPrinter,
            isPrinting = isPrinting,
            onDismiss = viewModel::dismissPrinterPicker,
            onSelectDevice = { device ->
                viewModel.printToThermalDevice(
                    context = context,
                    deviceAddress = device.address,
                    deviceName = device.name,
                    transaction = tx
                )
            }
        )
    }
}
