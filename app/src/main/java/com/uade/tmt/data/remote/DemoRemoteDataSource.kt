package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.Jugador

/**
 * Modo demo (`BuildConfig.SITIO_REAL = false`): el jugador de demostración es inventado de punta a
 * punta, así que su ficha también. Todo lo demás (calendario, torneos, ranking y otras fichas) se
 * sigue leyendo del sitio real.
 */
class DemoRemoteDataSource(private val sitio: TmtRemoteDataSource) : TmtRemoteDataSource by sitio {

    override suspend fun ficha(codigo: Int): Jugador =
        if (codigo == CODIGO_DEMO) JUGADOR_DEMO else sitio.ficha(codigo)

    companion object {
        private val CODIGO_DEMO = FakeAccesoRemoteDataSource.CODIGO_DEMO.toInt()

        val JUGADOR_DEMO = Jugador(
            codigo = CODIGO_DEMO,
            nombre = FakeAccesoRemoteDataSource.NOMBRE_DEMO,
            division = "6ta",
            puesto = null,
            rating = 1300,
        )
    }
}
