package fr.tchyze.vigie.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Couleurs fixes des états (indépendantes des couleurs dynamiques du système). */
data class StatusColors(
    val ok: Color, val onOk: Color,
    val warning: Color, val onWarning: Color,
    val danger: Color, val onDanger: Color,
    val unknown: Color, val onUnknown: Color,
)

private val LightStatus = StatusColors(
    ok = Color(0xFFD7F0DC), onOk = Color(0xFF0F5223),
    warning = Color(0xFFFFE0B8), onWarning = Color(0xFF7A3E00),
    danger = Color(0xFFFFD9D6), onDanger = Color(0xFF8C1D18),
    unknown = Color(0xFFE3E5E8), onUnknown = Color(0xFF3F454C),
)

private val DarkStatus = StatusColors(
    ok = Color(0xFF1F4D2B), onOk = Color(0xFFB4EBC0),
    warning = Color(0xFF5C3300), onWarning = Color(0xFFFFD3A1),
    danger = Color(0xFF6B1C17), onDanger = Color(0xFFFFC9C4),
    unknown = Color(0xFF3A3F45), onUnknown = Color(0xFFD6DAE0),
)

val statusColors: StatusColors
    @Composable get() = if (isSystemInDarkTheme()) DarkStatus else LightStatus
