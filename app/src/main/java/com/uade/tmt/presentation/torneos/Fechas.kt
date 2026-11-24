package com.uade.tmt.presentation.torneos

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// La app está en español: el nombre del día no depende del idioma del teléfono.
private val ES_AR = Locale.forLanguageTag("es-AR")
private val LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_AR)
private val CORTA = DateTimeFormatter.ofPattern("d/M", ES_AR)

/** "Viernes 27 de noviembre" */
internal fun LocalDate.larga(): String = format(LARGA).replaceFirstChar { it.titlecase(ES_AR) }

/** "27/11" */
internal fun LocalDate.corta(): String = format(CORTA)
