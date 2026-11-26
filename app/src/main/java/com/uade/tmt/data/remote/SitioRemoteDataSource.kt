package com.uade.tmt.data.remote

import com.uade.tmt.data.remote.parser.CalendarioParser
import com.uade.tmt.data.remote.parser.ClubParser
import com.uade.tmt.data.remote.parser.ClubesParser
import com.uade.tmt.data.remote.parser.TorneoParser
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Torneo

/**
 * Datos reales del sitio. Lo que todavía no tiene parser se delega en [pendiente] (datos inventados):
 * cada RF sobrescribe acá su método y, cuando estén todos, se elimina la delegación.
 */
class SitioRemoteDataSource(
    private val sitio: SitioService,
    pendiente: TmtRemoteDataSource = MuestraRemoteDataSource(),
) : TmtRemoteDataSource by pendiente {

    override suspend fun calendario(): List<Torneo> = CalendarioParser.parsear(sitio.calendario())

    override suspend fun torneo(codigo: Int): Torneo = TorneoParser.parsear(codigo, sitio.torneo(codigo))

    override suspend fun clubes(): List<Club> = ClubesParser.parsear(sitio.clubes())

    override suspend fun club(codigo: Int): Club = ClubParser.parsear(codigo, sitio.club(codigo))
}
