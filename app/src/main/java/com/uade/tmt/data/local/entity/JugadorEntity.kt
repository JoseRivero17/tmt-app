package com.uade.tmt.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

@Entity(tableName = "jugadores", indices = [Index("division")])
data class JugadorEntity(
    @PrimaryKey val codigo: Int,
    val nombre: String,
    /** Nombre sin tildes y en minúsculas, para la búsqueda de RF03. */
    val nombreBusqueda: String,
    val division: String,
    val puesto: Int?,
    val rating: Int?,
    val variacion: Int?,
    val clubCodigo: Int?,
    /** Abreviatura que publica el ranking. */
    val club: String?,
    val provincia: String?,
    val actualizadoEn: Instant,
)

data class JugadorConClub(
    @Embedded val jugador: JugadorEntity,
    @Relation(parentColumn = "clubCodigo", entityColumn = "codigo")
    val club: ClubEntity?,
)

/** Sin clave foránea a propósito: dejar de figurar en el ranking no borra lo que el usuario marcó. */
@Entity(tableName = "seguidos")
data class SeguidoEntity(
    @PrimaryKey val jugadorCodigo: Int,
    val esPropio: Boolean,
)

data class JugadorSeguido(
    @Embedded val jugador: JugadorEntity,
    val esPropio: Boolean,
    @Relation(parentColumn = "clubCodigo", entityColumn = "codigo")
    val club: ClubEntity?,
)
