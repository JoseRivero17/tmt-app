package com.uade.tmt.presentation

import androidx.annotation.StringRes
import com.uade.tmt.R
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.ResultadoSync

/** Estado del intento de actualización de una pantalla. El contenido guardado se muestra igual. */
data class EstadoSync(
    val enCurso: Boolean = false,
    val fallo: MotivoFallo? = null,
)

fun ResultadoSync.aEstado() = EstadoSync(fallo = (this as? ResultadoSync.Fallo)?.motivo)

@get:StringRes
val MotivoFallo.mensaje: Int
    get() = when (this) {
        MotivoFallo.SIN_CONEXION -> R.string.fallo_sin_conexion
        MotivoFallo.TIMEOUT -> R.string.fallo_timeout
        MotivoFallo.SERVIDOR -> R.string.fallo_servidor
        MotivoFallo.LECTURA -> R.string.fallo_lectura
    }
