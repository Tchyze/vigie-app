package fr.tchyze.vigie.ui.common

import fr.tchyze.vigie.data.model.parseDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH)

fun formatDate(date: LocalDate?): String = date?.format(dayFormat) ?: "date inconnue"

fun formatDate(value: String?): String = formatDate(parseDate(value))

fun formatDateTime(instant: Instant): String = instant.atZone(ZoneId.systemDefault()).format(dateTimeFormat)

/** Horodatage du schéma (`AAAA-MM-JJThh:mm:ssZ`) en heure locale. */
fun formatTimestamp(value: String?): String =
    value?.let { runCatching { formatDateTime(Instant.parse(it)) }.getOrNull() } ?: "date inconnue"
