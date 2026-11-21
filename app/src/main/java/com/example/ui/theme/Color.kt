package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Theme — Telecom Cobalt & Signal Teal
val CobaltPrimaryLight = Color(0xFF0F4C81)
val OnCobaltPrimaryLight = Color(0xFFFFFFFF)
val CobaltContainerLight = Color(0xFFD6E8FF)
val OnCobaltContainerLight = Color(0xFF001C37)

val SignalTealSecondaryLight = Color(0xFF00796B)
val OnSignalTealSecondaryLight = Color(0xFFFFFFFF)
val SignalTealContainerLight = Color(0xFFCCF2EC)
val OnSignalTealContainerLight = Color(0xFF00201C)

val CyanAccentTertiaryLight = Color(0xFF0284C7)
val OnCyanAccentTertiaryLight = Color(0xFFFFFFFF)
val CyanAccentContainerLight = Color(0xFFE0F2FE)
val OnCyanAccentTertiaryContainerLight = Color(0xFF0C4A6E)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFF5F7FB)
val OnBackgroundLight = Color(0xFF101828)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF101828)
val SurfaceVariantLight = Color(0xFFE8EEF6)
val OnSurfaceVariantLight = Color(0xFF475467)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF8FAFD)
val SurfaceContainerLight = Color(0xFFF0F4FA)
val SurfaceContainerHighLight = Color(0xFFE7EDF6)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)

// Dark Theme — Deep Slate Navy & Electric Sky
val CobaltPrimaryDark = Color(0xFF7CC4FF)
val OnCobaltPrimaryDark = Color(0xFF00325A)
val CobaltContainerDark = Color(0xFF0B3B66)
val OnCobaltContainerDark = Color(0xFFD6E8FF)

val SignalTealSecondaryDark = Color(0xFF5EEAD4)
val OnSignalTealSecondaryDark = Color(0xFF003731)
val SignalTealContainerDark = Color(0xFF004F47)
val OnSignalTealContainerDark = Color(0xFFCCF2EC)

val CyanAccentTertiaryDark = Color(0xFF38BDF8)
val OnCyanAccentTertiaryDark = Color(0xFF003549)
val CyanAccentContainerDark = Color(0xFF075985)
val OnCyanAccentTertiaryContainerDark = Color(0xFFE0F2FE)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF0B111C)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF111827)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val SurfaceContainerLowestDark = Color(0xFF080D16)
val SurfaceContainerLowDark = Color(0xFF131C2B)
val SurfaceContainerDark = Color(0xFF192334)
val SurfaceContainerHighDark = Color(0xFF222E42)
val OutlineDark = Color(0xFF334155)
val OutlineVariantDark = Color(0xFF1E293B)

@Immutable
data class NetBillSemanticColors(
    val paid: Color,
    val paidContainer: Color,
    val overdue: Color,
    val overdueContainer: Color,
    val dueToday: Color,
    val dueTodayContainer: Color,
    val upcoming: Color,
    val upcomingContainer: Color,
    val unpaid: Color,
    val unpaidContainer: Color,
    val cash: Color,
    val cashContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val whatsappGreen: Color,
    val onWhatsappGreen: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color
)

val LightNetBillSemanticColors = NetBillSemanticColors(
    paid = Color(0xFF15803D),
    paidContainer = Color(0xFFDCFCE7),
    overdue = Color(0xFFB91C1C),
    overdueContainer = Color(0xFFFEE2E2),
    dueToday = Color(0xFFB45309),
    dueTodayContainer = Color(0xFFFEF3C7),
    upcoming = Color(0xFF1D4ED8),
    upcomingContainer = Color(0xFFDBEAFE),
    unpaid = Color(0xFF475569),
    unpaidContainer = Color(0xFFE2E8F0),
    cash = Color(0xFF0F766E),
    cashContainer = Color(0xFFCCFBF1),
    transfer = Color(0xFF1D4ED8),
    transferContainer = Color(0xFFDBEAFE),
    whatsappGreen = Color(0xFF16A34A),
    onWhatsappGreen = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFF0F4C81),
    heroGradientEnd = Color(0xFF00796B)
)

val DarkNetBillSemanticColors = NetBillSemanticColors(
    paid = Color(0xFF4ADE80),
    paidContainer = Color(0xFF064E3B),
    overdue = Color(0xFFF87171),
    overdueContainer = Color(0xFF7F1D1D),
    dueToday = Color(0xFFFBBF24),
    dueTodayContainer = Color(0xFF78350F),
    upcoming = Color(0xFF60A5FA),
    upcomingContainer = Color(0xFF1E3A8A),
    unpaid = Color(0xFFCBD5E1),
    unpaidContainer = Color(0xFF334155),
    cash = Color(0xFF2DD4BF),
    cashContainer = Color(0xFF115E59),
    transfer = Color(0xFF60A5FA),
    transferContainer = Color(0xFF1E3A8A),
    whatsappGreen = Color(0xFF22C55E),
    onWhatsappGreen = Color(0xFF052E16),
    heroGradientStart = Color(0xFF0E3A63),
    heroGradientEnd = Color(0xFF0D5C54)
)

val LocalNetBillColors = staticCompositionLocalOf { LightNetBillSemanticColors }
