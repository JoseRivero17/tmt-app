package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.mapper.normalizado
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.time.DateTimeException
import java.time.LocalDate

private val FECHA = Regex("""(\d{1,2}) de (\p{L}+) de (\d{4})""", RegexOption.IGNORE_CASE)
private val DIVISION = Regex("""(\d+(?:ra|da|ta|ma|va|na)) division""")
private val CANCELADO = Regex("cancelad|suspendid")
private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** Busca "27 de Noviembre de 2026" dentro de un texto más largo. */
internal fun fechaEn(texto: String): LocalDate? {
    val (dia, nombreMes, anio) = FECHA.find(texto)?.destructured ?: return null
    val mes = when (val nombre = nombreMes.lowercase()) {
        "setiembre" -> 9
        else -> MESES.indexOf(nombre) + 1
    }
    return try {
        LocalDate.of(anio.toInt(), mes, dia.toInt())
    } catch (e: DateTimeException) {
        null
    }
}

/** El sitio escribe "7MA DIVISIÓN" o "7ma división": las dos quedan como "7ma". */
internal fun nombreDivision(texto: String): String =
    DIVISION.find(texto.normalizado())?.groupValues?.get(1) ?: texto.limpio()

internal fun indicaCancelacion(texto: String): Boolean = CANCELADO.containsMatchIn(texto.normalizado())

/** Sin espacios repetidos ni espacios duros (&nbsp;). */
internal fun String.limpio(): String = replace('\u00A0', ' ').trim().replace(Regex("\\s+"), " ")

/** "VILLA URQUIZA" → "Villa Urquiza". */
internal fun String.aTitulo(): String =
    limpio().lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

/** Líneas de un elemento separadas por <br>. */
internal fun Element.lineas(): List<String> =
    childNodes().filterIsInstance<TextNode>().map { it.text().limpio() }.filter { it.isNotEmpty() }
