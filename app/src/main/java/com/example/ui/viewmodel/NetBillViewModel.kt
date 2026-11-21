package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BillTransactionEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.DefaultPrinterConfig
import com.example.data.local.InternetPlanEntity
import com.example.data.local.IspProfileEntity
import com.example.data.local.PaymentMethod
import com.example.data.local.ThemeMode
import com.example.data.repository.NetBillRepository
import com.example.domain.BillFilter
import com.example.domain.BillingCalculator
import com.example.domain.BillingStatus
import com.example.domain.BookkeepingSummary
import com.example.domain.CustomerBillItem
import com.example.domain.DashboardSummary
import com.example.domain.IndonesianFormatters
import com.example.domain.PaymentBookFilter
import com.example.domain.PlanWithCount
import com.example.util.BackupRestoreHelper
import com.example.util.BillingNotificationHelper
import com.example.util.BluetoothThermalPrinter
import com.example.util.BookkeepingExportHelper
import com.example.util.ReceiptBitmapGenerator
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class BillingPeriod(
    val month: Int,
    val year: Int
)

data class DashboardUiState(
    val period: BillingPeriod,
    val ispProfile: IspProfileEntity = IspProfileEntity(),
    val summary: DashboardSummary = DashboardSummary(),
    val allBills: List<CustomerBillItem> = emptyList(),
    val filteredBills: List<CustomerBillItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: BillFilter = BillFilter.SEMUA
)

data class BookkeepingUiState(
    val period: BillingPeriod,
    val filterByPeriod: Boolean = true,
    val selectedPaymentFilter: PaymentBookFilter = PaymentBookFilter.SEMUA,
    val searchQuery: String = "",
    val summary: BookkeepingSummary = BookkeepingSummary(),
    val cashTransactions: List<BillTransactionEntity> = emptyList(),
    val transferTransactions: List<BillTransactionEntity> = emptyList(),
    val periodCashTransactions: List<BillTransactionEntity> = emptyList(),
    val periodTransferTransactions: List<BillTransactionEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class NetBillViewModel(
    private val repository: NetBillRepository
) : ViewModel() {

    private val currentCalendar = Calendar.getInstance()
    private val _selectedPeriod = MutableStateFlow(
        BillingPeriod(
            month = currentCalendar.get(Calendar.MONTH) + 1,
            year = currentCalendar.get(Calendar.YEAR)
        )
    )
    val selectedPeriod: StateFlow<BillingPeriod> = _selectedPeriod.asStateFlow()

    private val _dashboardSearch = MutableStateFlow("")
    private val _dashboardFilter = MutableStateFlow(BillFilter.SEMUA)
    private val _customerSearch = MutableStateFlow("")
    val customerSearch: StateFlow<String> = _customerSearch.asStateFlow()

    private val _receiptSearch = MutableStateFlow("")
    val receiptSearch: StateFlow<String> = _receiptSearch.asStateFlow()

    private val _receiptPaymentFilter = MutableStateFlow(PaymentBookFilter.SEMUA)
    private val _receiptFilterByPeriod = MutableStateFlow(true)

    private val _previewTransaction = MutableStateFlow<BillTransactionEntity?>(null)
    val previewTransaction: StateFlow<BillTransactionEntity?> = _previewTransaction.asStateFlow()

    private val _printerTransaction = MutableStateFlow<BillTransactionEntity?>(null)
    val printerTransaction: StateFlow<BillTransactionEntity?> = _printerTransaction.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val themeMode: StateFlow<ThemeMode> = repository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    val defaultPrinter: StateFlow<DefaultPrinterConfig?> = repository.defaultPrinter.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val ispProfile: StateFlow<IspProfileEntity> = repository.ispProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IspProfileEntity()
    )

    val plans: StateFlow<List<InternetPlanEntity>> = repository.allPlans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allTransactions: StateFlow<List<BillTransactionEntity>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val bookkeepingUiState: StateFlow<BookkeepingUiState> = combine(
        allTransactions,
        _selectedPeriod,
        _receiptFilterByPeriod,
        _receiptPaymentFilter,
        _receiptSearch
    ) { allTx, period, filterByPeriod, paymentFilter, query ->
        val scopedTransactions = if (filterByPeriod) {
            allTx.filter { it.billingYear == period.year && it.billingMonth == period.month }
        } else {
            allTx
        }

        val summary = BillingCalculator.calculateBookkeepingSummary(scopedTransactions)
        val periodCash = scopedTransactions.filter { PaymentMethod.fromString(it.paymentMethod).isCash }
        val periodTransfer = scopedTransactions.filter { PaymentMethod.fromString(it.paymentMethod).isTransfer }

        val q = query.trim().lowercase()
        val searchFiltered = if (q.isEmpty()) {
            scopedTransactions
        } else {
            scopedTransactions.filter { tx ->
                tx.invoiceNumber.lowercase().contains(q) ||
                    tx.customerNameSnapshot.lowercase().contains(q) ||
                    tx.planNameSnapshot.lowercase().contains(q) ||
                    tx.notes.lowercase().contains(q) ||
                    PaymentMethod.fromString(tx.paymentMethod).label.lowercase().contains(q)
            }
        }

        val cashList = searchFiltered.filter { PaymentMethod.fromString(it.paymentMethod).isCash }
        val transferList = searchFiltered.filter { PaymentMethod.fromString(it.paymentMethod).isTransfer }

        BookkeepingUiState(
            period = period,
            filterByPeriod = filterByPeriod,
            selectedPaymentFilter = paymentFilter,
            searchQuery = query,
            summary = summary,
            cashTransactions = cashList,
            transferTransactions = transferList,
            periodCashTransactions = periodCash,
            periodTransferTransactions = periodTransfer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookkeepingUiState(period = _selectedPeriod.value)
    )

    val plansWithCount: StateFlow<List<PlanWithCount>> = combine(
        plans,
        customers
    ) { planList, customerList ->
        val countsByPlan = customerList.groupingBy { it.planId }.eachCount()
        planList.map { plan ->
            PlanWithCount(
                plan = plan,
                subscriberCount = countsByPlan[plan.id] ?: 0
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(
        customers,
        _customerSearch
    ) { customerList, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            customerList
        } else {
            customerList.filter { c ->
                c.name.lowercase().contains(q) ||
                    c.phone.lowercase().contains(q) ||
                    c.address.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val periodTransactions: StateFlow<List<BillTransactionEntity>> = _selectedPeriod
        .flatMapLatest { period ->
            repository.observeTransactionsByPeriod(period.year, period.month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val baseDashboardState = combine(
        _selectedPeriod,
        ispProfile,
        customers,
        plans,
        periodTransactions
    ) { period, profile, customerList, planList, periodTx ->
        val allBills = BillingCalculator.calculateCustomerBills(
            customers = customerList,
            plans = planList,
            periodTransactions = periodTx,
            billingMonth = period.month,
            billingYear = period.year
        )
        val summary = BillingCalculator.calculateSummary(allBills, periodTx)
        Triple(Triple(period, profile, summary), allBills, Unit)
    }

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        baseDashboardState,
        _dashboardSearch,
        _dashboardFilter
    ) { base, query, filter ->
        val (meta, allBills) = base
        val (period, profile, summary) = meta
        val q = query.trim().lowercase()

        val filtered = allBills.filter { item ->
            val matchesFilter = when (filter) {
                BillFilter.SEMUA -> true
                BillFilter.MENUNGGAK -> item.status == BillingStatus.MENUNGGAK
                BillFilter.HARI_INI -> item.status == BillingStatus.HARI_INI
                BillFilter.MENDEKATI -> item.status == BillingStatus.MENDEKATI
                BillFilter.BELUM_BAYAR -> item.status != BillingStatus.LUNAS
                BillFilter.LUNAS -> item.status == BillingStatus.LUNAS
                BillFilter.LUNAS_TUNAI -> item.status == BillingStatus.LUNAS && item.paymentMethod?.isCash == true
                BillFilter.LUNAS_TRANSFER -> item.status == BillingStatus.LUNAS && item.paymentMethod?.isTransfer == true
            }
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                item.customer.name.lowercase().contains(q) ||
                    item.customer.address.lowercase().contains(q) ||
                    item.customer.phone.lowercase().contains(q)
            }
            matchesFilter && matchesQuery
        }

        DashboardUiState(
            period = period,
            ispProfile = profile,
            summary = summary,
            allBills = allBills,
            filteredBills = filtered,
            searchQuery = query,
            selectedFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(period = _selectedPeriod.value)
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    fun updateDashboardSearch(query: String) {
        _dashboardSearch.value = query
    }

    fun selectDashboardFilter(filter: BillFilter) {
        _dashboardFilter.value = filter
    }

    fun updateCustomerSearch(query: String) {
        _customerSearch.value = query
    }

    fun updateReceiptSearch(query: String) {
        _receiptSearch.value = query
    }

    fun selectReceiptPaymentFilter(filter: PaymentBookFilter) {
        _receiptPaymentFilter.value = filter
    }

    fun setReceiptFilterByPeriod(filterByPeriod: Boolean) {
        _receiptFilterByPeriod.value = filterByPeriod
    }

    fun shiftBillingPeriod(deltaMonths: Int) {
        val current = _selectedPeriod.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.year)
            set(Calendar.MONTH, current.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        _selectedPeriod.value = BillingPeriod(
            month = cal.get(Calendar.MONTH) + 1,
            year = cal.get(Calendar.YEAR)
        )
    }

    fun cycleThemeMode() {
        val next = when (themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
            emitMessage("Tema layar diubah ke ${mode.label}")
        }
    }

    fun clearDefaultPrinter() {
        viewModelScope.launch {
            repository.clearDefaultPrinter()
            emitMessage("Printer default dihapus")
        }
    }

    fun savePlan(plan: InternetPlanEntity) {
        viewModelScope.launch {
            val isNew = plan.id == 0L
            repository.savePlan(plan)
            emitMessage(if (isNew) "Paket ${plan.name} berhasil ditambahkan" else "Paket ${plan.name} diperbarui")
        }
    }

    fun deletePlan(plan: InternetPlanEntity) {
        viewModelScope.launch {
            val subscribers = customers.value.count { it.planId == plan.id }
            if (subscribers > 0) {
                emitMessage("Tidak dapat menghapus: masih digunakan $subscribers pelanggan")
                return@launch
            }
            repository.deletePlan(plan)
            emitMessage("Paket ${plan.name} dihapus")
        }
    }

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val isNew = customer.id == 0L
            repository.saveCustomer(customer)
            emitMessage(if (isNew) "Pelanggan ${customer.name} ditambahkan" else "Data ${customer.name} diperbarui")
        }
    }

    fun updateCustomerDueDay(customerId: Long, customerName: String, newDueDay: Int) {
        viewModelScope.launch {
            repository.updateCustomerDueDay(customerId, newDueDay)
            emitMessage("Tanggal jatuh tempo $customerName diubah ke tgl $newDueDay")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            emitMessage("Pelanggan ${customer.name} dihapus")
        }
    }

    fun confirmPayment(
        customer: CustomerEntity,
        plan: InternetPlanEntity?,
        billingMonth: Int,
        billingYear: Int,
        amountPaid: Long,
        paymentMethod: PaymentMethod,
        notes: String
    ) {
        viewModelScope.launch {
            val savedTx = repository.recordPayment(
                customer = customer,
                plan = plan,
                billingMonth = billingMonth,
                billingYear = billingYear,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod,
                notes = notes
            )
            emitMessage("Pembayaran ${customer.name} berhasil dicatat (LUNAS • ${paymentMethod.label})")
            _previewTransaction.value = savedTx
        }
    }

    fun updateTransactionDetails(
        transaction: BillTransactionEntity,
        amountPaid: Long,
        paymentMethod: PaymentMethod,
        notes: String
    ) {
        viewModelScope.launch {
            repository.updateTransactionDetails(
                transactionId = transaction.id,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod,
                notes = notes
            )
            emitMessage("Transaksi ${transaction.customerNameSnapshot} diperbarui (${paymentMethod.label})")
        }
    }

    fun deleteTransaction(transaction: BillTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            if (_previewTransaction.value?.id == transaction.id) {
                _previewTransaction.value = null
            }
            emitMessage("Status lunas ${transaction.customerNameSnapshot} dibatalkan")
        }
    }

    fun shareBookkeepingReport(context: Context) {
        val state = bookkeepingUiState.value
        val periodLabel = if (state.filterByPeriod) {
            IndonesianFormatters.formatMonthYear(state.period.month, state.period.year)
        } else {
            "Semua Periode"
        }
        val result = WhatsAppHelper.shareBookkeepingReport(
            context = context,
            periodLabel = periodLabel,
            cashTransactions = state.periodCashTransactions,
            transferTransactions = state.periodTransferTransactions,
            profile = ispProfile.value
        )
        result.onFailure {
            emitMessage("Gagal membagikan rekap pembukuan: ${it.message ?: "kesalahan"}")
        }
    }

    fun exportBookkeepingCsv(context: Context) {
        viewModelScope.launch {
            val state = bookkeepingUiState.value
            val periodLabel = if (state.filterByPeriod) {
                IndonesianFormatters.formatMonthYear(state.period.month, state.period.year)
            } else {
                "Semua Periode"
            }
            val result = withContext(Dispatchers.IO) {
                BookkeepingExportHelper.shareBookkeepingCsv(
                    context = context,
                    periodLabel = periodLabel,
                    cashTransactions = state.periodCashTransactions,
                    transferTransactions = state.periodTransferTransactions,
                    profile = ispProfile.value
                )
            }
            result.onSuccess { fileName ->
                emitMessage("Berkas CSV siap dibagikan ($fileName)")
            }.onFailure { err ->
                emitMessage("Gagal export CSV: ${err.message ?: "kesalahan"}")
            }
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val snapshot = repository.exportBackupSnapshot()
                BackupRestoreHelper.writeBackupToUri(context, uri, snapshot)
                    .map { snapshot }
            }
            result.onSuccess { snap ->
                emitMessage(
                    "Backup tersimpan (${snap.customers.size} pelanggan, ${snap.transactions.size} transaksi)"
                )
            }.onFailure { err ->
                emitMessage("Gagal menyimpan backup: ${err.message ?: "kesalahan"}")
            }
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupRestoreHelper.readBackupFromUri(context, uri).mapCatching { snapshot ->
                    repository.restoreBackupSnapshot(snapshot)
                    snapshot
                }
            }
            result.onSuccess { snap ->
                emitMessage(
                    "Restore berhasil (${snap.plans.size} paket, ${snap.customers.size} pelanggan, ${snap.transactions.size} transaksi)"
                )
            }.onFailure { err ->
                emitMessage("Gagal restore backup: ${err.message ?: "format file tidak sesuai"}")
            }
        }
    }

    fun saveProfile(profile: IspProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            emitMessage("Pengaturan profil & printer tersimpan")
        }
    }

    fun openReceiptPreview(transaction: BillTransactionEntity) {
        _previewTransaction.value = transaction
    }

    fun dismissReceiptPreview() {
        _previewTransaction.value = null
    }

    fun openPrinterPicker(transaction: BillTransactionEntity) {
        _printerTransaction.value = transaction
    }

    fun dismissPrinterPicker() {
        _printerTransaction.value = null
    }

    fun sendWhatsAppReminder(context: Context, item: CustomerBillItem) {
        val result = WhatsAppHelper.sendBillReminder(context, item, ispProfile.value)
        result.onFailure {
            emitMessage("Gagal membuka WhatsApp: ${it.message ?: "kesalahan tidak diketahui"}")
        }
    }

    fun shareReceiptPhotoToWhatsApp(context: Context, transaction: BillTransactionEntity) {
        viewModelScope.launch {
            val profile = ispProfile.value
            val result = withContext(Dispatchers.IO) {
                WhatsAppHelper.shareReceiptImageToWhatsApp(context, transaction, profile)
            }
            result.onFailure {
                emitMessage("Gagal membagikan foto struk: ${it.message ?: "kesalahan"}")
            }
        }
    }

    fun saveReceiptToGallery(context: Context, transaction: BillTransactionEntity) {
        viewModelScope.launch {
            val profile = ispProfile.value
            val result = withContext(Dispatchers.IO) {
                val bitmap = ReceiptBitmapGenerator.generateReceiptBitmap(transaction, profile)
                ReceiptBitmapGenerator.saveReceiptToGallery(context, bitmap, transaction.invoiceNumber)
            }
            result.onSuccess { fileName ->
                emitMessage("Struk disimpan ke Galeri ($fileName)")
            }.onFailure { err ->
                emitMessage("Gagal menyimpan ke Galeri: ${err.message ?: "kesalahan"}")
            }
        }
    }

    fun printToThermalDevice(
        context: Context,
        deviceAddress: String,
        deviceName: String,
        transaction: BillTransactionEntity
    ) {
        if (_isPrinting.value) return
        viewModelScope.launch {
            _isPrinting.value = true
            val result = BluetoothThermalPrinter.printReceipt(
                context = context,
                deviceAddress = deviceAddress,
                transaction = transaction,
                profile = ispProfile.value
            )
            _isPrinting.value = false
            result.onSuccess {
                repository.setDefaultPrinter(deviceName, deviceAddress)
                _printerTransaction.value = null
                emitMessage("Struk berhasil dicetak ke $deviceName")
            }.onFailure { err ->
                emitMessage("Gagal mencetak ke $deviceName: ${err.message ?: "periksa koneksi printer"}")
            }
        }
    }

    fun triggerDueNotification(context: Context) {
        val currentState = dashboardUiState.value
        val resultMsg = BillingNotificationHelper.sendDueReminderNotification(
            context = context,
            ispName = currentState.ispProfile.businessName,
            billItems = currentState.allBills
        )
        emitMessage(resultMsg)
    }

    fun notifyMessage(message: String) {
        emitMessage(message)
    }

    private fun emitMessage(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    class Factory(private val repository: NetBillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NetBillViewModel::class.java)) {
                return NetBillViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
