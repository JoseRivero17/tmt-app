package com.uade.tmt.domain.model

import java.time.LocalDate

/** Filtros del calendario (RF01). Un valor nulo significa "todos". */
data class FiltrosTorneo(
    val division: String? = null,
    val localidad: String? = null,
    val desde: LocalDate? = null,
    val hasta: LocalDate? = null,
) {
    val hayAlguno: Boolean get() = this != FiltrosTorneo()
}
