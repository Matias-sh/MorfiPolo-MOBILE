package com.cocido.morfipolo.data.repository

import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.database.entity.MenuEntity
import com.cocido.morfipolo.data.local.database.entity.MenuSelectionEntity
import com.cocido.morfipolo.data.mock.MockBackendService
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.domain.model.MenuSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class MenuRepository(
    private val database: AppDatabase,
    private val mockBackend: MockBackendService
) {
    private val menuDao = database.menuDao()
    private val menuSelectionDao = database.menuSelectionDao()

    suspend fun getMenuByDate(date: Date): Menu? {
        val timestamp = date.time
        return try {
            // Primero intentar desde la base de datos local
            val menuEntity = menuDao.getMenuByDate(timestamp)
            if (menuEntity != null) {
                entityToMenu(menuEntity)
            } else {
                // Si no está en local, obtener del backend
                mockBackend.getMenuByDate(date)?.also { menu ->
                    val menuEntity = menuToEntity(menu)
                    menuDao.insertMenu(menuEntity)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getMenuByDateFlow(date: Date): Flow<Menu?> {
        val timestamp = date.time
        return menuDao.getMenuByDateFlow(timestamp).map { entity ->
            entity?.let { entityToMenu(it) }
        }
    }

    suspend fun getWeeklyMenus(): List<Menu> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Obtener lunes de la semana
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        val startDate = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val endDate = calendar.timeInMillis

        return try {
            // Obtener desde backend y sincronizar con local
            val menus = mockBackend.getWeeklyMenus()
            val menuEntities = menus.map { menuToEntity(it) }
            menuDao.insertMenus(menuEntities)
            
            // Retornar desde local
            menuDao.getMenusByDateRange(startDate, endDate).map { entityToMenu(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getWeeklyMenusFlow(): Flow<List<Menu>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        val startDate = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val endDate = calendar.timeInMillis

        return menuDao.getMenusByDateRangeFlow(startDate, endDate).map { entities ->
            entities.map { entityToMenu(it) }
        }
    }

    suspend fun selectMenu(userId: Long, menuId: Long): Result<Boolean> {
        return try {
            val success = mockBackend.selectMenu(userId, menuId)
            if (success) {
                val selection = MenuSelectionEntity(
                    userId = userId,
                    menuId = menuId,
                    fechaSeleccion = Date().time
                )
                menuSelectionDao.insertSelection(selection)
                Result.success(true)
            } else {
                Result.failure(Exception("No se pudo seleccionar el menú"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deselectMenu(userId: Long, menuId: Long): Result<Boolean> {
        return try {
            val success = mockBackend.deselectMenu(userId, menuId)
            if (success) {
                menuSelectionDao.deleteSelection(userId, menuId)
                Result.success(true)
            } else {
                Result.failure(Exception("No se pudo deseleccionar el menú"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasUserSelectedMenu(userId: Long, menuId: Long): Boolean {
        return try {
            val selection = menuSelectionDao.getSelection(userId, menuId)
            selection != null || mockBackend.hasUserSelectedMenu(userId, menuId)
        } catch (e: Exception) {
            false
        }
    }

    fun hasUserSelectedMenuFlow(userId: Long, menuId: Long): Flow<Boolean> {
        return menuSelectionDao.getSelectionFlow(userId, menuId).map { it != null }
    }

    suspend fun loadMenuForToday(description: String): Menu? {
        return try {
            val menu = mockBackend.loadMenuForToday(description)
            menu?.let {
                val menuEntity = menuToEntity(it)
                menuDao.insertMenu(menuEntity)
                it
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun menuToEntity(menu: Menu): MenuEntity {
        return MenuEntity(
            id = menu.id,
            fecha = menu.fecha.time,
            descripcion = menu.descripcion,
            horarioInicio = menu.horarioInicio,
            horarioFin = menu.horarioFin,
            estado = menu.estado
        )
    }

    private fun entityToMenu(entity: MenuEntity): Menu {
        return Menu(
            id = entity.id,
            fecha = Date(entity.fecha),
            descripcion = entity.descripcion,
            horarioInicio = entity.horarioInicio,
            horarioFin = entity.horarioFin,
            estado = entity.estado
        )
    }
}


