package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.domain.repository.TorneoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration

class FakeInscripcionRemoteDataSourceTest {

    private val torneo = torneoDePrueba(codigo = 11036)
    private val fuente = FakeInscripcionRemoteDataSource(TorneosFalsos(torneo), demora = Duration.ZERO)

    @Test
    fun `las opciones salen de las categorias del torneo y la primera esta deshabilitada`() = runBlocking {
        val esperadas = listOf(
            OpcionInscripcion("5ta", "10:00", habilitada = false, FakeInscripcionRemoteDataSource.MOTIVO_RATING),
            OpcionInscripcion("6ta", "12:00", habilitada = true, motivo = null),
            OpcionInscripcion("7ma", "14:00", habilitada = true, motivo = null),
        )

        assertEquals(ResultadoOpciones.Disponibles(esperadas), fuente.opciones(11036))
    }

    @Test
    fun `la ultima categoria va a lista de espera y las demas se reservan`() = runBlocking {
        assertEquals(ResultadoReserva.Reservada, fuente.reservar(11036, "6ta"))
        assertEquals(ResultadoReserva.ListaEspera, fuente.reservar(11036, "7ma"))
    }

    @Test
    fun `la categoria deshabilitada o inexistente se rechaza`() = runBlocking {
        assertEquals(
            ResultadoReserva.Rechazada(FakeInscripcionRemoteDataSource.MOTIVO_RATING),
            fuente.reservar(11036, "5ta"),
        )
        assertTrue(fuente.reservar(11036, "1ra") is ResultadoReserva.Rechazada)
    }

    @Test
    fun `un torneo cancelado no habilita ninguna categoria`() = runBlocking {
        val cancelado = torneo.copy(cancelado = true)
        val fuente = FakeInscripcionRemoteDataSource(TorneosFalsos(cancelado), demora = Duration.ZERO)

        val opciones = (fuente.opciones(11036) as ResultadoOpciones.Disponibles).opciones
        assertTrue(opciones.none { it.habilitada })
        assertTrue(fuente.reservar(11036, "6ta") is ResultadoReserva.Rechazada)
    }

    @Test
    fun `un torneo que no esta guardado es error del sitio`() = runBlocking {
        assertEquals(ResultadoOpciones.ErrorSitio, fuente.opciones(1))
        assertEquals(ResultadoReserva.ErrorSitio, fuente.reservar(1, "6ta"))
    }
}

internal fun torneoDePrueba(codigo: Int) = Torneo(
    codigo = codigo,
    nombre = "Torneo de prueba",
    fecha = LocalDate.of(2026, 12, 5),
    localidad = "Ciudad Inventada",
    inscripcionOnline = true,
    categorias = listOf(
        CategoriaTorneo("5ta", hora = "10:00"),
        CategoriaTorneo("6ta", hora = "12:00"),
        CategoriaTorneo("7ma", hora = "14:00"),
    ),
)

/** Solo responde [observarTorneo]: la fuente falsa no usa nada más. */
internal class TorneosFalsos(vararg torneos: Torneo) : TorneoRepository {
    private val porCodigo = torneos.associateBy { it.codigo }

    override fun observarTorneo(codigo: Int): Flow<Torneo?> = flowOf(porCodigo[codigo])
    override fun observarCalendario(): Flow<List<Torneo>> = flowOf(porCodigo.values.toList())
    override fun observarActualizacionCalendario(): Flow<Instant?> = flowOf(null)
    override fun observarActualizacionTorneo(codigo: Int): Flow<Instant?> = flowOf(null)
    override suspend fun sincronizarCalendario(forzar: Boolean): ResultadoSync = error("Sin red en tests")
    override suspend fun sincronizarTorneo(codigo: Int, forzar: Boolean): ResultadoSync = error("Sin red en tests")
}
