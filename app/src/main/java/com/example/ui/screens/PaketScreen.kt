package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.InternetPlanEntity
import com.example.domain.IndonesianFormatters
import com.example.domain.PlanWithCount
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun PaketScreen(
    plansWithCount: List<PlanWithCount>,
    onSavePlan: (InternetPlanEntity) -> Unit,
    onDeletePlan: (InternetPlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPlan by remember { mutableStateOf<InternetPlanEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Paket Internet (Mbps)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Atur kecepatan, tarif bulanan, dan pantau jumlah pengguna tiap paket.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(
                items = plansWithCount,
                key = { it.plan.id }
            ) { entry ->
                PlanCard(
                    entry = entry,
                    onEdit = { editingPlan = entry.plan }
                )
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("plans_add_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah Paket Internet")
        }
    }

    if (showCreateDialog) {
        PlanFormDialog(
            initialPlan = null,
            subscriberCount = 0,
            onDismiss = { showCreateDialog = false },
            onSave = { plan ->
                onSavePlan(plan)
                showCreateDialog = false
            },
            onDelete = null
        )
    }

    editingPlan?.let { target ->
        val subCount = plansWithCount.firstOrNull { it.plan.id == target.id }?.subscriberCount ?: 0
        PlanFormDialog(
            initialPlan = target,
            subscriberCount = subCount,
            onDismiss = { editingPlan = null },
            onSave = { updated ->
                onSavePlan(updated)
                editingPlan = null
            },
            onDelete = { toDelete ->
                onDeletePlan(toDelete)
                editingPlan = null
            }
        )
    }
}

@Composable
private fun PlanCard(
    entry: PlanWithCount,
    onEdit: () -> Unit
) {
    val plan = entry.plan
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${plan.speedMbps}",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mbps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${IndonesianFormatters.formatRupiah(plan.price)} / bulan",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = plan.description.ifBlank { "Unlimited tanpa FUP" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.subscriberCount} pelanggan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_plan_btn_${plan.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit paket ${plan.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PlanFormDialog(
    initialPlan: InternetPlanEntity?,
    subscriberCount: Int,
    onDismiss: () -> Unit,
    onSave: (InternetPlanEntity) -> Unit,
    onDelete: ((InternetPlanEntity) -> Unit)?
) {
    var name by remember { mutableStateOf(initialPlan?.name ?: "") }
    var speedText by remember { mutableStateOf(initialPlan?.speedMbps?.toString() ?: "") }
    var priceText by remember { mutableStateOf(initialPlan?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(initialPlan?.description ?: "Unlimited tanpa FUP") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && initialPlan != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Paket Internet?") },
            text = {
                Text(
                    if (subscriberCount > 0) {
                        "Paket ini sedang digunakan oleh $subscriberCount pelanggan. Pindahkan pelanggan ke paket lain terlebih dahulu sebelum menghapus."
                    } else {
                        "Paket \"${initialPlan.name}\" akan dihapus permanen."
                    }
                )
            },
            confirmButton = {
                if (subscriberCount == 0) {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(initialPlan)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Hapus")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (subscriberCount > 0) "Mengerti" else "Batal")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialPlan == null) "Tambah Paket" else "Edit Paket",
                    style = MaterialTheme.typography.titleLarge
                )
                if (initialPlan != null && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Hapus paket",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama Paket *") },
                    placeholder = { Text("Misal: Paket Home 35 Mbps") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = speedText,
                        onValueChange = { speedText = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Kecepatan *") },
                        suffix = { Text("Mbps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_speed_input")
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Harga / Bln *") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("plan_price_input")
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi / Keterangan") },
                    placeholder = { Text("Unlimited tanpa FUP") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val speed = speedText.toIntOrNull()
                    val price = priceText.toLongOrNull()
                    when {
                        trimmedName.isEmpty() -> errorMsg = "Nama paket wajib diisi"
                        speed == null || speed <= 0 -> errorMsg = "Kecepatan Mbps harus lebih dari 0"
                        price == null || price <= 0L -> errorMsg = "Harga paket harus lebih dari Rp 0"
                        else -> onSave(
                            InternetPlanEntity(
                                id = initialPlan?.id ?: 0L,
                                name = trimmedName,
                                speedMbps = speed,
                                price = price,
                                description = description.trim().ifBlank { "Unlimited tanpa FUP" },
                                createdAt = initialPlan?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_plan_button")
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
