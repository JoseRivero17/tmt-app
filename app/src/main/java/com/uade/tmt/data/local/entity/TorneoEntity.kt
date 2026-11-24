package com.uade.tmt.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

@Entity(tableName = "torneos")
data class TorneoEntity(
    @PrimaryKey val codigo: Int,
    val nombre: String,
    val fecha: LocalDate,
    val localidad: String,
    val clubCodigo: Int?,
    val sede: String?,
    val direccion: String?,
    val precios: String?,
    val cancelado: Boolean,
    val inscripcionOnline: Boolean,
)

/** Relación 1–N con el torneo: si se borra el torneo, se borran sus categorías. */
@Entity(
    tableName = "categorias_torneo",
    primaryKeys = ["torneoCodigo", "orden"],
    foreignKeys = [
        ForeignKey(
            entity = TorneoEntity::class,
            parentColumns = ["codigo"],
            childColumns = ["torneoCodigo"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CategoriaTorneoEntity(
    val torneoCodigo: Int,
    /** Posición en la página del sitio, para mostrarlas en el mismo orden. */
    val orden: Int,
    val division: String,
    val detalle: String?,
    val hora: String?,
    val cupos: Int?,
)

data class TorneoConCategorias(
    @Embedded val torneo: TorneoEntity,
    @Relation(parentColumn = "codigo", entityColumn = "torneoCodigo")
    val categorias: List<CategoriaTorneoEntity>,
    @Relation(parentColumn = "clubCodigo", entityColumn = "codigo")
    val club: ClubEntity?,
)
