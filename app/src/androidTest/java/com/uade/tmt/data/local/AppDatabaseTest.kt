package com.uade.tmt.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uade.tmt.data.local.entity.SeguidoEntity
import com.uade.tmt.data.mapper.aDominio
import com.uade.tmt.data.mapper.aEntidad
import com.uade.tmt.data.mapper.categoriasAEntidad
import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Torneo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private val hoy = LocalDate.of(2026, 11, 24)

    @Before
    fun crear() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun cerrar() = db.close()

    private fun torneo(codigo: Int, fecha: LocalDate = hoy.plusDays(1)) =
        Torneo(codigo, "Torneo $codigo", fecha, "CABA", inscripcionOnline = true)

    private suspend fun calendario(vararg torneos: Torneo) = db.torneoDao().reemplazarCalendario(
        torneos = torneos.map { it.aEntidad() },
        categorias = torneos.flatMap { it.categoriasAEntidad() },
        desde = hoy,
    )

    @Test
    fun elCalendarioGuardaLasCategoriasParaPoderFiltrar() = runBlocking {
        calendario(torneo(1).copy(categorias = listOf(CategoriaTorneo("7ma", hora = "20:30"))))

        val guardado = db.torneoDao().observarDesde(hoy).first().single().aDominio()
        assertEquals(listOf("7ma"), guardado.categorias.map { it.division })
    }

    @Test
    fun elDetalleConservaLaLocalidadDelCalendario() = runBlocking {
        calendario(torneo(1))
        db.torneoDao().guardarDetalle(torneo(1).copy(localidad = "").aEntidad(), emptyList())

        assertEquals("CABA", db.torneoDao().observar(1).first()!!.torneo.localidad)
    }

    @Test
    fun elTorneoMuestraElNombreDelClubSiEstaGuardado() = runBlocking {
        db.clubDao().reemplazarTodos(listOf(Club(9, "Club Nueve").aEntidad()))
        calendario(torneo(1).copy(clubCodigo = 9), torneo(2).copy(clubCodigo = 8))

        assertEquals("Club Nueve", db.torneoDao().observar(1).first()!!.aDominio().club)
        assertNull(db.torneoDao().observar(2).first()!!.aDominio().club)
    }

    @Test
    fun actualizarElCalendarioConservaElDetalleYaBajado() = runBlocking {
        val dao = db.torneoDao()
        val detalle = torneo(1).copy(sede = "Sede", categorias = listOf(CategoriaTorneo("5ta", cupos = 24)))
        dao.guardarDetalle(detalle.aEntidad(), detalle.categoriasAEntidad())

        calendario(torneo(1).copy(cancelado = true, categorias = listOf(CategoriaTorneo("5ta", hora = "19:00"))))

        val guardado = dao.observar(1).first()!!.aDominio()
        assertTrue(guardado.cancelado)
        assertEquals("Sede", guardado.sede)
        assertEquals(listOf(CategoriaTorneo("5ta", hora = "19:00", cupos = 24)), guardado.categorias)
    }

    @Test
    fun actualizarElCalendarioQuitaLosFuturosQueYaNoFiguranYOrdenaPorFecha() = runBlocking {
        val dao = db.torneoDao()
        calendario(torneo(1), torneo(2))

        calendario(torneo(3, hoy.plusDays(9)), torneo(2))

        assertEquals(listOf(2, 3), dao.observarDesde(hoy).first().map { it.torneo.codigo })
    }

    @Test
    fun losTorneosPasadosNoSeMuestranEnElCalendario() = runBlocking {
        val dao = db.torneoDao()
        dao.guardarDetalle(torneo(1, hoy.minusDays(1)).aEntidad(), emptyList())

        assertTrue(dao.observarDesde(hoy).first().isEmpty())
    }

    @Test
    fun volverABajarElDetalleNoDuplicaCategorias() = runBlocking {
        val dao = db.torneoDao()
        val detalle = torneo(1).copy(categorias = listOf(CategoriaTorneo("5ta"), CategoriaTorneo("6ta")))

        repeat(2) { dao.guardarDetalle(detalle.aEntidad(), detalle.categoriasAEntidad()) }

        assertEquals(2, dao.observar(1).first()!!.categorias.size)
    }

    @Test
    fun laBusquedaEncuentraPorNombreSinTildesYPorCodigo() = runBlocking {
        val dao = db.jugadorDao()
        val ahora = Instant.parse("2026-11-24T18:00:00Z")
        dao.guardar(
            listOf(
                Jugador(123, "MUÑOZ, Ángela", "5ta", 1, 1400),
                Jugador(456, "Otro", "5ta", 2, 1300),
            ).map { it.aEntidad(ahora) },
        )

        assertEquals(listOf(123), dao.buscar("munoz").first().map { it.codigo })
        assertEquals(listOf(456), dao.buscar("456").first().map { it.codigo })
        assertTrue(dao.buscar("zzz").first().isEmpty())
    }

    @Test
    fun losSeguidosMuestranPrimeroElPerfilPropio() = runBlocking {
        val dao = db.jugadorDao()
        val ahora = Instant.parse("2026-11-24T18:00:00Z")
        dao.guardar(listOf(Jugador(1, "A", "5ta", 1, 1), Jugador(2, "B", "5ta", 2, 1)).map { it.aEntidad(ahora) })
        dao.seguir(SeguidoEntity(1, esPropio = false))
        dao.seguir(SeguidoEntity(2, esPropio = true))

        assertEquals(listOf(2, 1), dao.observarSeguidos().first().map { it.jugador.codigo })
    }

    @Test
    fun reemplazarClubesNoDejaLosAnteriores() = runBlocking {
        val dao = db.clubDao()
        dao.reemplazarTodos(listOf(Club(1, "A").aEntidad()))
        dao.reemplazarTodos(listOf(Club(2, "B").aEntidad()))

        assertEquals(listOf(2), dao.observarTodos().first().map { it.codigo })
        assertNull(dao.observar(1).first())
    }
}
