package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.OrderEntity

@Database(entities = [OrderEntity::class], version = 1, exportSchema = false)
abstract class WarungDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: WarungDatabase? = null

        fun getInstance(context: Context): WarungDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarungDatabase::class.java,
                    "warung_nurbaiti_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
