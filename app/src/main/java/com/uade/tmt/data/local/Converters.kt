package com.uade.tmt.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Room solo guarda tipos básicos: las fechas se guardan como números para poder ordenarlas. */
class Converters {
    @TypeConverter
    fun fechaANumero(fecha: LocalDate?): Long? = fecha?.toEpochDay()

    @TypeConverter
    fun numeroAFecha(dias: Long?): LocalDate? = dias?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instanteANumero(instante: Instant?): Long? = instante?.toEpochMilli()

    @TypeConverter
    fun numeroAInstante(milis: Long?): Instant? = milis?.let(Instant::ofEpochMilli)
}
