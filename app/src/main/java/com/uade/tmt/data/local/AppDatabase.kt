package com.uade.tmt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uade.tmt.data.local.dao.ClubDao
import com.uade.tmt.data.local.dao.InscripcionDao
import com.uade.tmt.data.local.dao.JugadorDao
import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.local.dao.TorneoDao
import com.uade.tmt.data.local.entity.CategoriaTorneoEntity
import com.uade.tmt.data.local.entity.ClubEntity
import com.uade.tmt.data.local.entity.InscripcionEntity
import com.uade.tmt.data.local.entity.JugadorEntity
import com.uade.tmt.data.local.entity.SeguidoEntity
import com.uade.tmt.data.local.entity.SyncInfoEntity
import com.uade.tmt.data.local.entity.TorneoEntity

/** Base local: única fuente de verdad de la interfaz (4.13). */
@Database(
    entities = [
        TorneoEntity::class,
        CategoriaTorneoEntity::class,
        JugadorEntity::class,
        SeguidoEntity::class,
        ClubEntity::class,
        InscripcionEntity::class,
        SyncInfoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun torneoDao(): TorneoDao
    abstract fun jugadorDao(): JugadorDao
    abstract fun clubDao(): ClubDao
    abstract fun inscripcionDao(): InscripcionDao
    abstract fun syncInfoDao(): SyncInfoDao

    companion object {
        fun crear(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "tmt.db").build()
    }
}
