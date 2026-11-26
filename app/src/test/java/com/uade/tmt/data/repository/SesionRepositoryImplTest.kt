package com.uade.tmt.data.repository

import com.uade.tmt.data.remote.AccesoRemoteDataSource
import com.uade.tmt.data.remote.AccesoService
import com.uade.tmt.data.remote.CookieJarEnMemoria
import com.uade.tmt.data.remote.EstadoAcceso
import com.uade.tmt.data.remote.FakeAccesoRemoteDataSource
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.data.remote.SitioAccesoRemoteDataSource
import com.uade.tmt.data.remote.parser.html
import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.SesionJugador
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import kotlin.time.Duration

class SesionRepositoryImplTest {

    private val fake = FakeAccesoRemoteDataSource(demora = Duration.ZERO)
    private val repositorio = SesionRepositoryImpl(fake)

    @Test
    fun `el jugador de demostracion inicia sesion`() = runBlocking {
        val resultado = repositorio.login("99999", "demo")

        val esperada = SesionJugador("99999", FakeAccesoRemoteDataSource.NOMBRE_DEMO)
        assertEquals(ResultadoLogin.Ok(esperada), resultado)
        assertEquals(esperada, repositorio.sesion.value)
    }

    @Test
    fun `el codigo se toma sin espacios alrededor`() = runBlocking {
        assertTrue(repositorio.login(" 99999 ", "demo") is ResultadoLogin.Ok)
    }

    @Test
    fun `otra combinacion son credenciales invalidas y no hay sesion`() = runBlocking {
        assertEquals(ResultadoLogin.CredencialesInvalidas, repositorio.login("99999", "otra"))
        assertEquals(ResultadoLogin.CredencialesInvalidas, repositorio.login("54321", "demo"))
        assertNull(repositorio.sesion.value)
    }

    @Test
    fun `campos vacios son datos incompletos sin consultar la fuente remota`() = runBlocking {
        val remoto = RemotoFalso { error("No debería consultarse") }
        val repo = SesionRepositoryImpl(remoto)

        assertEquals(ResultadoLogin.DatosIncompletos, repo.login("", "demo"))
        assertEquals(ResultadoLogin.DatosIncompletos, repo.login("   ", "demo"))
        assertEquals(ResultadoLogin.DatosIncompletos, repo.login("99999", ""))
        assertEquals(0, remoto.ingresos)
    }

    @Test
    fun `logout borra la sesion y las cookies`() = runBlocking {
        val remoto = RemotoFalso { EstadoAcceso.Exito(nombre = null) }
        val repo = SesionRepositoryImpl(remoto)
        repo.login("99999", "demo")

        repo.logout()

        assertNull(repo.sesion.value)
        assertEquals(1, remoto.cierres)
    }

    @Test
    fun `un login fallido despues de uno exitoso deja sin sesion`() = runBlocking {
        repositorio.login("99999", "demo")

        repositorio.login("99999", "otra")

        assertNull(repositorio.sesion.value)
    }

    @Test
    fun `sin red es sin conexion`() = runBlocking {
        val repo = SesionRepositoryImpl(RemotoFalso { throw UnknownHostException() })

        assertEquals(ResultadoLogin.SinConexion, repo.login("99999", "demo"))
        assertNull(repo.sesion.value)
    }

    @Test
    fun `una respuesta ilegible es error del sitio`() = runBlocking {
        val repo = SesionRepositoryImpl(RemotoFalso { throw LecturaException("ilegible") })

        assertEquals(ResultadoLogin.ErrorSitio, repo.login("99999", "demo"))
    }

    @Test
    fun `una excepcion inesperada no crashea`() = runBlocking {
        val repo = SesionRepositoryImpl(RemotoFalso { throw IllegalStateException() })

        assertEquals(ResultadoLogin.ErrorSitio, repo.login("99999", "demo"))
    }

    @Test
    fun `sesion requerida o sin sesion despues del POST son error del sitio`() = runBlocking {
        for (estado in listOf(EstadoAcceso.SesionRequerida, EstadoAcceso.SinSesion)) {
            val repo = SesionRepositoryImpl(RemotoFalso { estado })

            assertEquals(ResultadoLogin.ErrorSitio, repo.login("99999", "demo"))
            assertNull(repo.sesion.value)
        }
    }

    @Test
    fun `con la fuente real, un HTML alterado es error del sitio sin excepcion`() = runBlocking {
        val alterado = html("buscarJugador.html").replace("ACCESO DE JUGADORES", "OTRA SECCION")
            .replace("columna_barra2", "otra_barra")
        val repo = SesionRepositoryImpl(sitioQueResponde(alterado))

        assertEquals(ResultadoLogin.ErrorSitio, repo.login("99999", "demo"))
    }

    @Test
    fun `con la fuente real, el aviso de datos incompletos llega al dominio`() = runBlocking {
        val repo = SesionRepositoryImpl(sitioQueResponde(html("buscarJugador.html")))

        assertEquals(ResultadoLogin.DatosIncompletos, repo.login("99999", "demo"))
    }

    private fun sitioQueResponde(respuesta: String) = SitioAccesoRemoteDataSource(
        object : AccesoService {
            override suspend fun ingresar(codigo: String, clave: String) = respuesta
        },
        CookieJarEnMemoria(),
    )

    private class RemotoFalso(private val respuesta: () -> EstadoAcceso) : AccesoRemoteDataSource {
        var ingresos = 0
        var cierres = 0

        override suspend fun ingresar(codigo: String, clave: String): EstadoAcceso {
            ingresos++
            return respuesta()
        }

        override suspend fun cerrarSesion() {
            cierres++
        }
    }
}
