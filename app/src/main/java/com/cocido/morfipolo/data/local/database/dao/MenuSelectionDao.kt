package com.cocido.morfipolo.data.local.database.dao

import androidx.room.*
import com.cocido.morfipolo.data.local.database.entity.MenuSelectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuSelectionDao {
    @Query("SELECT * FROM menu_selections WHERE userId = :userId AND menuId = :menuId LIMIT 1")
    suspend fun getSelection(userId: String, menuId: String): MenuSelectionEntity?

    @Query("SELECT * FROM menu_selections WHERE userId = :userId AND menuId = :menuId LIMIT 1")
    fun getSelectionFlow(userId: String, menuId: String): Flow<MenuSelectionEntity?>

    @Query("SELECT * FROM menu_selections WHERE userId = :userId ORDER BY fechaSeleccion DESC")
    suspend fun getUserSelections(userId: String): List<MenuSelectionEntity>

    @Query("SELECT * FROM menu_selections WHERE userId = :userId ORDER BY fechaSeleccion DESC")
    fun getUserSelectionsFlow(userId: String): Flow<List<MenuSelectionEntity>>

    @Query("SELECT * FROM menu_selections WHERE menuId = :menuId")
    suspend fun getMenuSelections(menuId: String): List<MenuSelectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelection(selection: MenuSelectionEntity)

    @Delete
    suspend fun deleteSelection(selection: MenuSelectionEntity)

    @Query("DELETE FROM menu_selections WHERE userId = :userId AND menuId = :menuId")
    suspend fun deleteSelection(userId: String, menuId: String)
}




