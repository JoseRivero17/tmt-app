package com.uade.tmt.presentation.clubes

import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Ubicacion
import com.uade.tmt.domain.repository.ClubRepository
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.UbicacionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Sin Android ni `Dispatchers.Main`: el ViewModel corre en un scope Unconfined (RNF11). */
class ClubesViewModelTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val repositorio = ClubesFalsos()
    private val conectividad = ConectividadFalsa()
    private val ubicacion = UbicacionFalsa(Ubicacion(-34.6037, -58.3816)) // Obelisco

    @After
    fun cerrar() = scope.cancel()

    /** Con un observador activo, como cuando la pantalla está a la vista. */
    private fun viewModel(): ClubesViewModel =
        ClubesViewModel(repositorio, conectividad, ubicacion, scope = scope).also { vm ->
            scope.launch { vm.uiState.collect {} }
        }

    private val ClubesViewModel.contenido get() = uiState.value as ClubesUiState.Contenido

    @Test
    fun `antes de decidir sobre la ubicacion explica para que se usa`() {
        assertEquals(ClubesUiState.PedirPermiso, viewModel().uiState.value)
    }

    @Test
    fun `con permiso ordena por distancia y lee la ubicacion una sola vez`() {
        val vm = viewModel()

        vm.permisoConcedido()
        vm.revisarPermiso(concedido = true) // volver a la pantalla no vuelve a leer

        assertEquals(listOf(2, 3, 1), vm.contenido.clubes.map { it.club.codigo })
        assertTrue(vm.contenido.ordenadosPorDistancia)
        assertEquals(1, ubicacion.lecturas)
    }

    @Test
    fun `si ya tenia el permiso no pasa por la explicacion`() {
        val vm = viewModel()

        vm.revisarPermiso(concedido = true)

        assertEquals(AccesoUbicacion.CONCEDIDO, vm.contenido.acceso)
    }

    @Test
    fun `con el permiso rechazado sigue funcionando la busqueda por localidad`() {
        val vm = viewModel()

        vm.permisoRechazado()
        vm.buscar("villa crespo")

        assertEquals(AccesoUbicacion.RECHAZADO, vm.contenido.acceso)
        assertEquals(listOf(2), vm.contenido.clubes.map { it.club.codigo })
        assertFalse(vm.contenido.ordenadosPorDistancia)
        assertEquals(0, ubicacion.lecturas)
    }

    @Test
    fun `si quita el permiso desde Ajustes deja de usar la ubicacion`() {
        val vm = viewModel()
        vm.permisoConcedido()

        vm.revisarPermiso(concedido = false)

        assertEquals(AccesoUbicacion.RECHAZADO, vm.contenido.acceso)
        assertTrue(vm.contenido.clubes.all { it.distanciaKm == null })
    }

    @Test
    fun `si el telefono no da la ubicacion lo avisa y lista por nombre`() {
        ubicacion.respuesta = null
        val vm = viewModel()

        vm.permisoConcedido()

        assertTrue(vm.contenido.sinUbicacion)
        assertEquals(listOf("Adrogue Sith Spin", "Club La Plata", "Club Villa Crespo"), vm.contenido.clubes.map { it.club.nombre })
    }

    @Test
    fun `sin conexion muestra los clubes guardados`() {
        conectividad.conectado.value = false
        repositorio.resultado = ResultadoSync.Fallo(MotivoFallo.SIN_CONEXION)
        val vm = viewModel()
        vm.continuarSinUbicacion()

        vm.actualizar(forzar = false)

        assertTrue(vm.contenido.sinConexion)
        assertNull(vm.contenido.fallo)
        assertEquals(3, vm.contenido.clubes.size)
    }

    @Test
    fun `primer uso sin conexion y sin datos muestra el error`() {
        repositorio.actualizadoEn.value = null
        repositorio.resultado = ResultadoSync.Fallo(MotivoFallo.SIN_CONEXION)
        val vm = viewModel()
        vm.continuarSinUbicacion()

        vm.actualizar(forzar = false)

        assertEquals(ClubesUiState.Error(MotivoFallo.SIN_CONEXION), vm.uiState.value)
    }

    private class ClubesFalsos : ClubRepository {
        val clubes = MutableStateFlow(
            listOf(
                Club(1, "Club La Plata", localidad = "La Plata", latitud = -34.9214, longitud = -57.9545),
                Club(2, "Club Villa Crespo", localidad = "Villa Crespo", latitud = -34.5990, longitud = -58.4380),
                Club(3, "Adrogue Sith Spin", localidad = "Adrogue", latitud = -34.796361, longitud = -58.386436),
            ),
        )
        val actualizadoEn = MutableStateFlow<Instant?>(Instant.EPOCH)
        var resultado: ResultadoSync = ResultadoSync.Actualizado

        override fun observarClubes(): Flow<List<Club>> = clubes
        override fun observarClub(codigo: Int): Flow<Club?> = clubes.map { l -> l.firstOrNull { it.codigo == codigo } }
        override fun observarActualizacion(): Flow<Instant?> = actualizadoEn
        override fun observarActualizacionClub(codigo: Int): Flow<Instant?> = actualizadoEn
        override suspend fun sincronizar(forzar: Boolean): ResultadoSync = resultado
        override suspend fun sincronizarClub(codigo: Int, forzar: Boolean): ResultadoSync = resultado
    }

    private class UbicacionFalsa(var respuesta: Ubicacion?) : UbicacionProvider {
        var lecturas = 0
        override suspend fun ubicacionActual(): Ubicacion? {
            lecturas++
            return respuesta
        }
    }

    private class ConectividadFalsa : Conectividad {
        override val conectado = MutableStateFlow(true)
    }
}
