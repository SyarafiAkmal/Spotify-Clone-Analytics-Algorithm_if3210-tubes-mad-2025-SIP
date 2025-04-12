package com.example.purrytify.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.purrytify.data.local.db.SongDao
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.data.local.db.entities.SongUploader
import androidx.room.Room
import android.content.Context
import com.example.purrytify.data.local.db.entities.LibraryEntity
import com.example.purrytify.data.local.db.entities.RecentPlaysEntity

@Database(entities = [SongEntity::class, SongUploader::class, LibraryEntity::class, RecentPlaysEntity::class], version = 13)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "purrytify_database"
                )   .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}