package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Seguido
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** RF02 + RF03: el perfil propio sale de la sesión, como en el sitio. */
class PerfilPropioUseCasesTest {

    private val sesiones = SesionFalsa()
    private val jugadores = JugadoresFalsos()
    private val iniciarSesion = IniciarSesion(sesiones, jugadores)
    private val cerrarSesion = CerrarSesion(sesiones, jugadores)

    @Test
    fun `iniciar sesion deja ese codigo como perfil propio`() = runBlocking {
        sesiones.respuesta = ResultadoLogin.Ok(SesionJugador("10077"))

        iniciarSesion("10077", "clave")

        assertEquals(10077, jugadores.perfilPropio)
    }

    @Test
    fun `un login fallido no cambia el perfil propio`() = runBlocking {
        jugadores.perfilPropio = 5
        sesiones.respuesta = ResultadoLogin.CredencialesInvalidas

        val resultado = iniciarSesion("10077", "mal")

        assertEquals(ResultadoLogin.CredencialesInvalidas, resultado)
        assertEquals(5, jugadores.perfilPropio)
    }

    @Test
    fun `cerrar sesion quita el perfil propio`() = runBlocking {
        sesiones.respuesta = ResultadoLogin.Ok(SesionJugador("10077"))
        iniciarSesion("10077", "clave")

        cerrarSesion()

        assertNull(sesiones.sesion.value)
        assertNull(jugadores.perfilPropio)
    }
}

private class SesionFalsa : SesionRepository {
    override val sesion = MutableStateFlow<SesionJugador?>(null)
    var respuesta: ResultadoLogin = ResultadoLogin.ErrorSitio

    override suspend fun login(codigo: String, clave: String): ResultadoLogin =
        respuesta.also { if (it is ResultadoLogin.Ok) sesion.value = it.sesion }

    override suspend fun logout() {
        sesion.value = null
    }
}

/** Solo registra el perfil propio; el resto no lo usan los casos de uso de sesión. */
internal class JugadoresFalsos : JugadorRepository {
    var perfilPropio: Int? = null

    override suspend fun establecerPerfilPropio(codigo: Int) {
        perfilPropio = codigo
    }

    override suspend fun quitarPerfilPropio() {
        perfilPropio = null
    }

    override fun observarCodigoPropio(): Flow<Int?> = MutableStateFlow(perfilPropio)
    override fun observarRanking(division: String): Flow<List<Jugador>> = emptyFlow()
    override fun observarActualizacionRanking(division: String): Flow<Instant?> = emptyFlow()
    override fun buscar(texto: String): Flow<List<Jugador>> = emptyFlow()
    override fun observarJugador(codigo: Int): Flow<Jugador?> = emptyFlow()
    override fun observarSeguidos(): Flow<List<Seguido>> = emptyFlow()
    override suspend fun seguir(codigo: Int) = Unit
    override suspend fun dejarDeSeguir(codigo: Int) = Unit
    override suspend fun sincronizarRanking(division: String, forzar: Boolean) = ResultadoSync.Vigente
    override suspend fun precargarRanking() = ResultadoSync.Vigente
    override suspend fun sincronizarFicha(codigo: Int, forzar: Boolean) = ResultadoSync.Vigente
    override suspend fun sincronizarPerfilPropio(forzar: Boolean) = ResultadoSync.Vigente
}
