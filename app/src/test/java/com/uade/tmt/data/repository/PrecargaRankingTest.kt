package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.JugadorDao
import com.uade.tmt.data.local.entity.JugadorConClub
import com.uade.tmt.data.local.entity.JugadorEntity
import com.uade.tmt.data.local.entity.JugadorSeguido
import com.uade.tmt.data.local.entity.SeguidoEntity
import com.uade.tmt.data.remote.MuestraRemoteDataSource
import com.uade.tmt.data.remote.TmtRemoteDataSource
import com.uade.tmt.domain.model.DIVISIONES
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.ResultadoSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PrecargaRankingTest {

    private val remoto = RemotoFake()
    private val dao = JugadorDaoFake()
    private val syncInfo = SyncInfoDaoFake()
    private val clock = Clock.fixed(Instant.parse("2026-11-24T18:00:00Z"), ZoneOffset.UTC)
    private val repositorio = JugadorRepositoryImpl(dao, syncInfo, remoto, Sincronizador(syncInfo, clock), clock)

    @Test
    fun `baja las ocho divisiones, de a una`() = runBlocking {
        assertEquals(ResultadoSync.Actualizado, repositorio.precargarRanking())

        assertEquals(DIVISIONES, remoto.pedidas)
        assertEquals(DIVISIONES.toSet(), dao.guardados.map { it.division }.toSet())
    }

    @Test
    fun `si se corta conserva lo bajado y la proxima vez sigue por donde quedo`() = runBlocking {
        remoto.fallaEn = "4ta"
        assertEquals(ResultadoSync.Fallo(MotivoFallo.SIN_CONEXION), repositorio.precargarRanking())
        assertEquals(setOf("1ra", "2da", "3ra"), dao.guardados.map { it.division }.toSet())

        remoto.fallaEn = null
        remoto.pedidas.clear()
        repositorio.precargarRanking()

        assertEquals(DIVISIONES.drop(3), remoto.pedidas)
    }

    @Test
    fun `si una division tarda demasiado sigue con las demas y despues pide solo esa`() = runBlocking {
        remoto.fallaEn = "4ta"
        remoto.falla = SocketTimeoutException()
        assertEquals(ResultadoSync.Fallo(MotivoFallo.TIMEOUT), repositorio.precargarRanking())
        assertEquals((DIVISIONES - "4ta").toSet(), dao.guardados.map { it.division }.toSet())

        remoto.fallaEn = null
        remoto.pedidas.clear()
        assertEquals(ResultadoSync.Actualizado, repositorio.precargarRanking())
        assertEquals(listOf("4ta"), remoto.pedidas)
    }

    @Test
    fun `si todo esta vigente no consulta el sitio`() = runBlocking {
        repositorio.precargarRanking()
        remoto.pedidas.clear()

        assertEquals(ResultadoSync.Vigente, repositorio.precargarRanking())
        assertEquals(emptyList<String>(), remoto.pedidas)
    }
}

private class RemotoFake : TmtRemoteDataSource by MuestraRemoteDataSource() {
    val pedidas = mutableListOf<String>()
    var fallaEn: String? = null
    var falla: Exception = IOException()

    override suspend fun ranking(division: String): List<Jugador> {
        if (division == fallaEn) throw falla
        pedidas += division
        return listOf(Jugador(division.hashCode(), "Jugador", division, 1, 1000))
    }
}

private class JugadorDaoFake : JugadorDao {
    val guardados = mutableListOf<JugadorEntity>()

    override suspend fun guardar(jugadores: List<JugadorEntity>) {
        guardados += jugadores
    }

    override fun observarRanking(division: String): Flow<List<JugadorConClub>> = emptyFlow()
    override fun observar(codigo: Int): Flow<JugadorConClub?> = emptyFlow()
    override fun buscar(texto: String): Flow<List<JugadorConClub>> = emptyFlow()
    override fun observarSeguidos(): Flow<List<JugadorSeguido>> = emptyFlow()
    override suspend fun seguir(seguido: SeguidoEntity) = Unit
    override suspend fun dejarDeSeguir(codigo: Int) = Unit
    override suspend fun buscarPorCodigo(codigo: Int): JugadorEntity? = null
    override fun observarCodigoPropio(): Flow<Int?> = emptyFlow()
    override suspend fun quitarPropio() = Unit
}
