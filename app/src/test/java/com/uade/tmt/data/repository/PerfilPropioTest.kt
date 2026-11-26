package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.JugadorDao
import com.uade.tmt.data.local.entity.JugadorConClub
import com.uade.tmt.data.local.entity.JugadorEntity
import com.uade.tmt.data.local.entity.JugadorSeguido
import com.uade.tmt.data.local.entity.SeguidoEntity
import com.uade.tmt.data.mapper.aEntidad
import com.uade.tmt.data.remote.MuestraRemoteDataSource
import com.uade.tmt.data.remote.TmtRemoteDataSource
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.ResultadoSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** RF02 + RF03: perfil propio por sesión y ficha bajada por código. */
class PerfilPropioTest {

    private val ahora = Instant.parse("2026-11-26T12:00:00Z")
    private val clock = Clock.fixed(ahora, ZoneOffset.UTC)
    private val dao = JugadorDaoEnMemoria()
    private val remoto = FichasFake()
    private val syncInfo = SyncInfoDaoFake()
    private val repositorio = JugadorRepositoryImpl(dao, syncInfo, remoto, Sincronizador(syncInfo, clock), clock)

    private val ficha = Jugador(10, "Ana Muñoz", "5ta", puesto = null, rating = 1242, clubCodigo = 90, club = "CLU")

    @Test
    fun `el perfil propio nuevo reemplaza al anterior`() = runBlocking {
        repositorio.establecerPerfilPropio(1)
        repositorio.establecerPerfilPropio(2)

        assertEquals(listOf(SeguidoEntity(2, esPropio = true)), dao.seguidos.values.toList())
        assertEquals(2, repositorio.observarCodigoPropio().first())
    }

    @Test
    fun `quitar el perfil propio no toca los seguidos comunes`() = runBlocking {
        repositorio.seguir(5)
        repositorio.establecerPerfilPropio(1)

        repositorio.quitarPerfilPropio()

        assertEquals(listOf(SeguidoEntity(5, esPropio = false)), dao.seguidos.values.toList())
        assertNull(repositorio.observarCodigoPropio().first())
    }

    @Test
    fun `la ficha de un jugador que no estaba guardado se guarda tal cual`() = runBlocking {
        remoto.fichas[10] = ficha

        assertEquals(ResultadoSync.Actualizado, repositorio.sincronizarFicha(10))

        assertEquals(ficha.aEntidad(ahora), dao.jugadores[10])
    }

    @Test
    fun `si ya estaba por el ranking conserva nombre, provincia y puesto en la division`() = runBlocking {
        dao.jugadores[10] = Jugador(10, "Muñoz, Ana", "5ta", 40, 1200, club = "CLU", provincia = "BUE")
            .aEntidad(Instant.EPOCH)
        remoto.fichas[10] = ficha

        repositorio.sincronizarFicha(10)

        val guardado = dao.jugadores.getValue(10)
        assertEquals("Muñoz, Ana", guardado.nombre)
        assertEquals("BUE", guardado.provincia)
        assertEquals(40, guardado.puesto)
        assertEquals(1242, guardado.rating)
        assertEquals(ahora, guardado.actualizadoEn)
    }

    @Test
    fun `si cambio de division el puesto viejo no se conserva`() = runBlocking {
        dao.jugadores[10] = Jugador(10, "Muñoz, Ana", "6ta", 3, 1190).aEntidad(Instant.EPOCH)
        remoto.fichas[10] = ficha

        repositorio.sincronizarFicha(10)

        assertEquals("5ta", dao.jugadores.getValue(10).division)
        assertNull(dao.jugadores.getValue(10).puesto)
    }

    @Test
    fun `sin perfil propio no se consulta el sitio`() = runBlocking {
        assertEquals(ResultadoSync.Vigente, repositorio.sincronizarPerfilPropio())
        assertEquals(emptyList<Int>(), remoto.pedidas)
    }

    @Test
    fun `la ficha propia se pide una vez por dia salvo que se fuerce`() = runBlocking {
        remoto.fichas[10] = ficha
        repositorio.establecerPerfilPropio(10)

        repositorio.sincronizarPerfilPropio()
        assertEquals(ResultadoSync.Vigente, repositorio.sincronizarPerfilPropio())
        assertEquals(ResultadoSync.Actualizado, repositorio.sincronizarPerfilPropio(forzar = true))

        assertEquals(listOf(10, 10), remoto.pedidas)
    }
}

private class FichasFake : TmtRemoteDataSource by MuestraRemoteDataSource() {
    val fichas = mutableMapOf<Int, Jugador>()
    val pedidas = mutableListOf<Int>()

    override suspend fun ficha(codigo: Int): Jugador {
        pedidas += codigo
        return fichas.getValue(codigo)
    }
}

private class JugadorDaoEnMemoria : JugadorDao {
    val jugadores = mutableMapOf<Int, JugadorEntity>()
    val seguidos = linkedMapOf<Int, SeguidoEntity>()

    override suspend fun guardar(jugadores: List<JugadorEntity>) {
        jugadores.forEach { this.jugadores[it.codigo] = it }
    }

    override suspend fun buscarPorCodigo(codigo: Int): JugadorEntity? = jugadores[codigo]

    override suspend fun seguir(seguido: SeguidoEntity) {
        seguidos[seguido.jugadorCodigo] = seguido
    }

    override suspend fun dejarDeSeguir(codigo: Int) {
        seguidos.remove(codigo)
    }

    override fun observarCodigoPropio(): Flow<Int?> = flow { emit(seguidos.values.firstOrNull { it.esPropio }?.jugadorCodigo) }

    override suspend fun quitarPropio() {
        seguidos.values.removeAll { it.esPropio }
    }

    override fun observarRanking(division: String): Flow<List<JugadorConClub>> = emptyFlow()
    override fun observar(codigo: Int): Flow<JugadorConClub?> = emptyFlow()
    override fun buscar(texto: String): Flow<List<JugadorConClub>> = emptyFlow()
    override fun observarSeguidos(): Flow<List<JugadorSeguido>> = emptyFlow()
}
