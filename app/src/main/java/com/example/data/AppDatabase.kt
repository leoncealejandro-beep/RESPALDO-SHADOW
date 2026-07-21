package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class WindowContentTypeConverters {
    @TypeConverter
    fun fromContentType(value: WindowContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): WindowContentType = WindowContentType.valueOf(value)
}

@Database(
    entities = [
        VRWindowConfig::class,
        VRSetting::class,
        BrowserBookmark::class,
        BrowserHistory::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(WindowContentTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vrWindowDao(): VRWindowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shadow_vr_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}