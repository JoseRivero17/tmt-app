package com.uade.tmt.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.uade.tmt.data.local.entity.JugadorConClub
import com.uade.tmt.data.local.entity.JugadorEntity
import com.uade.tmt.data.local.entity.JugadorSeguido
import com.uade.tmt.data.local.entity.SeguidoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JugadorDao {

    @Transaction
    @Query("SELECT * FROM jugadores WHERE division = :division ORDER BY puesto IS NULL, puesto, nombre")
    fun observarRanking(division: String): Flow<List<JugadorConClub>>

    @Transaction
    @Query("SELECT * FROM jugadores WHERE codigo = :codigo")
    fun observar(codigo: Int): Flow<JugadorConClub?>

    /** [texto] llega ya normalizado. */
    @Transaction
    @Query(
        """
        SELECT * FROM jugadores
        WHERE nombreBusqueda LIKE '%' || :texto || '%' OR CAST(codigo AS TEXT) LIKE :texto || '%'
        ORDER BY rating DESC, nombre
        LIMIT 100
        """,
    )
    fun buscar(texto: String): Flow<List<JugadorConClub>>

    @Transaction
    @Query(
        """
        SELECT j.*, s.esPropio FROM seguidos s
        JOIN jugadores j ON j.codigo = s.jugadorCodigo
        ORDER BY s.esPropio DESC, j.nombre
        """,
    )
    fun observarSeguidos(): Flow<List<JugadorSeguido>>

    @Upsert
    suspend fun guardar(jugadores: List<JugadorEntity>)

    @Upsert
    suspend fun seguir(seguido: SeguidoEntity)

    @Query("DELETE FROM seguidos WHERE jugadorCodigo = :codigo")
    suspend fun dejarDeSeguir(codigo: Int)
}
