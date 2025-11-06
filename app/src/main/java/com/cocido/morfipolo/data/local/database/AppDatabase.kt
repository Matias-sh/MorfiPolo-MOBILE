package com.cocido.morfipolo.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cocido.morfipolo.data.local.database.dao.MenuDao
import com.cocido.morfipolo.data.local.database.dao.MenuSelectionDao
import com.cocido.morfipolo.data.local.database.dao.UserDao
import com.cocido.morfipolo.data.local.database.entity.MenuEntity
import com.cocido.morfipolo.data.local.database.entity.MenuSelectionEntity
import com.cocido.morfipolo.data.local.database.entity.UserEntity

@Database(
    entities = [UserEntity::class, MenuEntity::class, MenuSelectionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun menuDao(): MenuDao
    abstract fun menuSelectionDao(): MenuSelectionDao
}


