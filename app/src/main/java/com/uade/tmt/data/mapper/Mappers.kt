package com.uade.tmt.data.mapper

import com.uade.tmt.data.local.entity.CategoriaTorneoEntity
import com.uade.tmt.data.local.entity.ClubEntity
import com.uade.tmt.data.local.entity.JugadorEntity
import com.uade.tmt.data.local.entity.JugadorSeguido
import com.uade.tmt.data.local.entity.TorneoConCategorias
import com.uade.tmt.data.local.entity.TorneoEntity
import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Seguido
import com.uade.tmt.domain.model.Torneo
import java.time.Instant

fun Torneo.aEntidad() = TorneoEntity(
    codigo = codigo,
    nombre = nombre,
    fecha = fecha,
    localidad = localidad,
    club = club,
    sede = sede,
    direccion = direccion,
    cancelado = cancelado,
    inscripcionOnline = inscripcionOnline,
)

fun Torneo.categoriasAEntidad(): List<CategoriaTorneoEntity> =
    categorias.mapIndexed { indice, categoria ->
        CategoriaTorneoEntity(
            torneoCodigo = codigo,
            orden = indice,
            division = categoria.division,
            detalle = categoria.detalle,
            hora = categoria.hora,
            cupos = categoria.cupos,
        )
    }

fun TorneoConCategorias.aDominio() = Torneo(
    codigo = torneo.codigo,
    nombre = torneo.nombre,
    fecha = torneo.fecha,
    localidad = torneo.localidad,
    club = torneo.club,
    sede = torneo.sede,
    direccion = torneo.direccion,
    cancelado = torneo.cancelado,
    inscripcionOnline = torneo.inscripcionOnline,
    categorias = categorias.sortedBy { it.orden }.map {
        CategoriaTorneo(division = it.division, detalle = it.detalle, hora = it.hora, cupos = it.cupos)
    },
)

fun Jugador.aEntidad(actualizadoEn: Instant) = JugadorEntity(
    codigo = codigo,
    nombre = nombre,
    nombreBusqueda = nombre.normalizado(),
    division = division,
    puesto = puesto,
    rating = rating,
    variacion = variacion,
    clubCodigo = clubCodigo,
    club = club,
    provincia = provincia,
    actualizadoEn = actualizadoEn,
)

fun JugadorEntity.aDominio() = Jugador(
    codigo = codigo,
    nombre = nombre,
    division = division,
    puesto = puesto,
    rating = rating,
    variacion = variacion,
    clubCodigo = clubCodigo,
    club = club,
    provincia = provincia,
    actualizadoEn = actualizadoEn,
)

fun JugadorSeguido.aDominio() = Seguido(jugador = jugador.aDominio(), esPropio = esPropio)

fun Club.aEntidad() = ClubEntity(
    codigo = codigo,
    nombre = nombre,
    direccion = direccion,
    localidad = localidad,
    telefono = telefono,
    mail = mail,
    latitud = latitud,
    longitud = longitud,
)

fun ClubEntity.aDominio() = Club(
    codigo = codigo,
    nombre = nombre,
    direccion = direccion,
    localidad = localidad,
    telefono = telefono,
    mail = mail,
    latitud = latitud,
    longitud = longitud,
)
