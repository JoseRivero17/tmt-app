package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.local.entity.SyncInfoEntity
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Recurso
import com.uade.tmt.domain.model.ResultadoSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class SincronizadorTest {

    private val ahora = Instant.parse("2026-11-24T18:00:00Z")
    private val dao = SyncInfoDaoFake()
    private val sincronizador = Sincronizador(dao, Clock.fixed(ahora, ZoneOffset.UTC))
    private val recurso = Recurso.CALENDARIO

    @Test
    fun `sin sincronizacion previa baja los datos y registra la fecha`() = runBlocking {
        var llamadas = 0

        val resultado = sincronizador.sincronizar(recurso, forzar = false) { llamadas++ }

        assertEquals(ResultadoSync.Actualizado, resultado)
        assertEquals(1, llamadas)
        assertEquals(ahora, dao.fechas[recurso.clave])
    }

    @Test
    fun `dentro del intervalo no consulta el sitio`() = runBlocking {
        dao.fechas[recurso.clave] = ahora - Duration.ofMinutes(29)
        var consulto = false

        val resultado = sincronizador.sincronizar(recurso, forzar = false) { consulto = true }

        assertEquals(ResultadoSync.Vigente, resultado)
        assertFalse(consulto)
    }

    @Test
    fun `cumplido el intervalo vuelve a consultar`() = runBlocking {
        dao.fechas[recurso.clave] = ahora - Duration.ofMinutes(30)

        assertEquals(ResultadoSync.Actualizado, sincronizador.sincronizar(recurso, forzar = false) {})
    }

    @Test
    fun `la actualizacion manual ignora el intervalo`() = runBlocking {
        dao.fechas[recurso.clave] = ahora - Duration.ofMinutes(1)
        var consulto = false

        sincronizador.sincronizar(recurso, forzar = true) { consulto = true }

        assertTrue(consulto)
    }

    @Test
    fun `cada falla se informa con su motivo`() = runBlocking {
        suspend fun motivo(falla: Throwable) =
            (sincronizador.sincronizar(recurso, forzar = true) { throw falla } as ResultadoSync.Fallo).motivo

        assertEquals(MotivoFallo.SIN_CONEXION, motivo(UnknownHostException()))
        assertEquals(MotivoFallo.TIMEOUT, motivo(SocketTimeoutException()))
        assertEquals(MotivoFallo.LECTURA, motivo(LecturaException("cambió el HTML")))
    }

    @Test
    fun `una falla no cambia la fecha de la ultima actualizacion`() = runBlocking {
        val anterior = ahora - Duration.ofHours(2)
        dao.fechas[recurso.clave] = anterior

        sincronizador.sincronizar(recurso, forzar = false) { throw UnknownHostException() }

        assertEquals(anterior, dao.fechas[recurso.clave])
    }

    @Test(expected = CancellationException::class)
    fun `la cancelacion de la corrutina no se trata como falla`() {
        runBlocking {
            sincronizador.sincronizar(recurso, forzar = true) { throw CancellationException() }
        }
    }

    @Test
    fun `cada division del ranking se sincroniza por separado`() = runBlocking {
        sincronizador.sincronizar(Recurso.ranking("5ta"), forzar = false) {}

        assertNull(dao.fechas[Recurso.ranking("6ta").clave])
    }
}

private class SyncInfoDaoFake : SyncInfoDao {
    val fechas = mutableMapOf<String, Instant>()

    override fun observar(recurso: String): Flow<Instant?> = flowOf(fechas[recurso])
    override suspend fun obtener(recurso: String): Instant? = fechas[recurso]
    override suspend fun guardar(info: SyncInfoEntity) {
        fechas[info.recurso] = info.actualizadoEn
    }
}
