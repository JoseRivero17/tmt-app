package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.FiltrosTorneo
import com.uade.tmt.domain.model.Torneo

/** RF01, criterio 1: quedan solo los torneos que cumplen TODOS los filtros elegidos. */
class FiltrarTorneos {
    operator fun invoke(torneos: List<Torneo>, filtros: FiltrosTorneo): List<Torneo> =
        torneos.filter { torneo ->
            (filtros.division == null || torneo.categorias.any { it.division == filtros.division }) &&
                (filtros.localidad == null || torneo.localidad == filtros.localidad) &&
                (filtros.desde == null || torneo.fecha >= filtros.desde) &&
                (filtros.hasta == null || torneo.fecha <= filtros.hasta)
        }
}
