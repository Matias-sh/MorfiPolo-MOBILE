package com.cocido.morfipolo.data.local.database.dao

import androidx.room.*
import com.cocido.morfipolo.data.local.database.entity.MenuEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus WHERE fecha = :fecha LIMIT 1")
    suspend fun getMenuByDate(fecha: Long): MenuEntity?

    @Query("SELECT * FROM menus WHERE fecha = :fecha LIMIT 1")
    fun getMenuByDateFlow(fecha: Long): Flow<MenuEntity?>

    @Query("SELECT * FROM menus WHERE fecha >= :startDate AND fecha <= :endDate ORDER BY fecha ASC")
    suspend fun getMenusByDateRange(startDate: Long, endDate: Long): List<MenuEntity>

    @Query("SELECT * FROM menus WHERE fecha >= :startDate AND fecha <= :endDate ORDER BY fecha ASC")
    fun getMenusByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus ORDER BY fecha DESC")
    fun getAllMenus(): Flow<List<MenuEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: MenuEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenus(menus: List<MenuEntity>)

    @Update
    suspend fun updateMenu(menu: MenuEntity)

    @Delete
    suspend fun deleteMenu(menu: MenuEntity)

    @Query("SELECT * FROM menus WHERE id = :id LIMIT 1")
    suspend fun getMenuById(id: Long): MenuEntity?
}


