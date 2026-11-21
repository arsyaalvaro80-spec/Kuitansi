package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.netBillDataStore: DataStore<Preferences> by preferencesDataStore(name = "netbill_prefs")

data class DefaultPrinterConfig(
    val name: String,
    val address: String
)

class ThemePreferences(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val printerNameKey = stringPreferencesKey("default_printer_name")
    private val printerAddressKey = stringPreferencesKey("default_printer_address")

    val themeModeFlow: Flow<ThemeMode> = context.netBillDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    val defaultPrinterFlow: Flow<DefaultPrinterConfig?> = context.netBillDataStore.data.map { prefs ->
        val address = prefs[printerAddressKey]
        if (address.isNullOrBlank()) {
            null
        } else {
            DefaultPrinterConfig(
                name = prefs[printerNameKey].orEmpty().ifBlank { "Printer Thermal" },
                address = address
            )
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.netBillDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setDefaultPrinter(name: String, address: String) {
        context.netBillDataStore.edit { prefs ->
            prefs[printerNameKey] = name
            prefs[printerAddressKey] = address
        }
    }

    suspend fun clearDefaultPrinter() {
        context.netBillDataStore.edit { prefs ->
            prefs.remove(printerNameKey)
            prefs.remove(printerAddressKey)
        }
    }
}
