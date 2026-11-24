package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.local.entity.SyncInfoEntity
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Recurso
import com.uade.tmt.domain.model.ResultadoSync
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.time.Clock
import java.time.Duration

/**
 * Regla offline first común a todos los repositorios: decide si hace falta consultar la fuente
 * remota y convierte cualquier falla en un [ResultadoSync]. Si falla, lo guardado no se toca.
 */
class Sincronizador(
    private val syncInfo: SyncInfoDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** [bajarYGuardar] consulta la fuente remota y guarda el resultado en Room. */
    suspend fun sincronizar(
        recurso: Recurso,
        forzar: Boolean,
        bajarYGuardar: suspend () -> Unit,
    ): ResultadoSync {
        if (!forzar && estaVigente(recurso)) return ResultadoSync.Vigente

        return try {
            bajarYGuardar()
            syncInfo.guardar(SyncInfoEntity(recurso.clave, clock.instant()))
            ResultadoSync.Actualizado
        } catch (e: InterruptedIOException) {
            // Incluye SocketTimeoutException. Va antes que IOException porque es una subclase.
            ResultadoSync.Fallo(MotivoFallo.TIMEOUT)
        } catch (e: IOException) {
            ResultadoSync.Fallo(MotivoFallo.SIN_CONEXION)
        } catch (e: HttpException) {
            ResultadoSync.Fallo(MotivoFallo.SERVIDOR)
        } catch (e: LecturaException) {
            ResultadoSync.Fallo(MotivoFallo.LECTURA)
        }
    }

    private suspend fun estaVigente(recurso: Recurso): Boolean {
        val ultima = syncInfo.obtener(recurso.clave) ?: return false
        return Duration.between(ultima, clock.instant()) < recurso.intervaloMinimo
    }
}
