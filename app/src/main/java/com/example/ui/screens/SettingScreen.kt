package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.DefaultPrinterConfig
import com.example.data.local.IspProfileEntity
import com.example.data.local.ThemeMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.BackupRestoreHelper

@Composable
fun SettingScreen(
    themeMode: ThemeMode,
    profile: IspProfileEntity,
    defaultPrinter: DefaultPrinterConfig?,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSaveProfile: (IspProfileEntity) -> Unit,
    onClearDefaultPrinter: () -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onRestoreBackupUri: (Uri) -> Unit,
    onTriggerNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var businessName by remember { mutableStateOf(profile.businessName) }
    var addressInfo by remember { mutableStateOf(profile.addressInfo) }
    var managerPhone by remember { mutableStateOf(profile.managerPhone) }
    var bankAccountInfo by remember { mutableStateOf(profile.bankAccountInfo) }
    var paperWidthMm by remember { mutableIntStateOf(profile.printerPaperWidthMm) }
    var receiptFooter by remember { mutableStateOf(profile.receiptFooter) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackupUri(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    LaunchedEffect(profile) {
        businessName = profile.businessName
        addressInfo = profile.addressInfo
        managerPhone = profile.managerPhone
        bankAccountInfo = profile.bankAccountInfo
        paperWidthMm = profile.printerPaperWidthMm
        receiptFooter = profile.receiptFooter
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Restore Data dari Backup?") },
            text = {
                Text("Seluruh data paket, pelanggan, profil usaha, dan riwayat pembukuan saat ini akan diganti dengan isi berkas backup yang dipilih. Pastikan Anda sudah membuat backup terbaru terlebih dahulu.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        onRestoreBackupUri(uri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_restore_backup_button")
                ) {
                    Text("Ya, Restore Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pengaturan Aplikasi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sesuaikan tampilan, identitas RT/RW Net, printer thermal, backup data, dan notifikasi pengingat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Theme & Display Mode
        item(key = "theme") {
            SettingSectionCard(
                icon = Icons.Rounded.Palette,
                title = "Tema & Tampilan Layar",
                subtitle = "Tersimpan permanen di memori perangkat"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionTile(
                        label = "Terang",
                        icon = Icons.Rounded.LightMode,
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                        testTag = "setting_theme_light",
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionTile(
                        label = "Gelap",
                        icon = Icons.Rounded.DarkMode,
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onSelectThemeMode(ThemeMode.DARK) },
                        testTag = "setting_theme_dark",
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionTile(
                        label = "Otomatis",
                        icon = Icons.Rounded.BrightnessAuto,
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onSelectThemeMode(ThemeMode.SYSTEM) },
                        testTag = "setting_theme_system",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Backup & Restore Data (Offline Safe)
        item(key = "backup_restore") {
            SettingSectionCard(
                icon = Icons.Rounded.Backup,
                title = "Backup & Restore Data",
                subtitle = "Amankan data pelanggan, paket & pembukuan dalam berkas JSON"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            exportBackupLauncher.launch(BackupRestoreHelper.suggestedBackupFileName())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setting_export_backup_button"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Backup")
                    }

                    OutlinedButton(
                        onClick = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setting_restore_backup_button"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore Data")
                    }
                }
            }
        }

        // 3. ISP Profile Settings
        item(key = "profile") {
            SettingSectionCard(
                icon = Icons.Rounded.Storefront,
                title = "Profil Usaha / RT-RW Net",
                subtitle = "Tampil pada header aplikasi, pesan WA, dan struk kuitansi"
            ) {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Nama Usaha / Provider") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_business_name_input")
                )
                OutlinedTextField(
                    value = addressInfo,
                    onValueChange = { addressInfo = it },
                    label = { Text("Alamat / Keterangan Usaha") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = managerPhone,
                    onValueChange = { managerPhone = it },
                    label = { Text("No. HP / WhatsApp Pengelola") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bankAccountInfo,
                    onValueChange = { bankAccountInfo = it },
                    label = { Text("Info Rekening Pembayaran") },
                    placeholder = { Text("BCA 1234567890 a.n. Nama\nBRI / Dana / QRIS") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Thermal Printer Settings
        item(key = "printer") {
            SettingSectionCard(
                icon = Icons.Rounded.Print,
                title = "Pengaturan Printer Thermal",
                subtitle = "Format ESC/POS Bluetooth 58 mm & 80 mm"
            ) {
                if (defaultPrinter != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Printer Default Tersimpan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = defaultPrinter.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = defaultPrinter.address,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onClearDefaultPrinter,
                                modifier = Modifier.testTag("clear_default_printer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Hapus printer default"
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Lebar Kertas Printer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = paperWidthMm == 58,
                        onClick = { paperWidthMm = 58 },
                        label = { Text("58 mm (32 kar)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setting_paper_58")
                    )
                    FilterChip(
                        selected = paperWidthMm == 80,
                        onClick = { paperWidthMm = 80 },
                        label = { Text("80 mm (48 kar)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setting_paper_80")
                    )
                }

                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Catatan Kaki Struk (Footer)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_open_bluetooth_button")
                ) {
                    Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Pengaturan Bluetooth HP")
                }

                Spacer(Modifier.height(2.dp))

                Button(
                    onClick = {
                        onSaveProfile(
                            IspProfileEntity(
                                id = 1,
                                businessName = businessName.trim().ifBlank { "NetBill RT/RW Net" },
                                addressInfo = addressInfo.trim(),
                                managerPhone = managerPhone.trim(),
                                bankAccountInfo = bankAccountInfo.trim(),
                                printerPaperWidthMm = paperWidthMm,
                                receiptFooter = receiptFooter.trim()
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_save_profile_button"),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Profil & Pengaturan Printer")
                }
            }
        }

        // 5. Due Reminder Notifications Test
        item(key = "notif") {
            SettingSectionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Notifikasi Pengingat Jatuh Tempo",
                subtitle = "Periksa daftar pelanggan H-3, jatuh tempo hari ini, dan menunggak"
            ) {
                FilledTonalButton(
                    onClick = onTriggerNotification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_trigger_notification_button"),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Picu Notifikasi Tagihan Sekarang")
                }
            }
        }
    }
}

@Composable
private fun SettingSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
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
                }
            }
            content()
        }
    }
}

@Composable
private fun ThemeOptionTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
