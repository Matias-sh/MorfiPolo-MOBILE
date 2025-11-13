package com.cocido.morfipolo.data.repository

import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.database.entity.MenuEntity
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.domain.model.Menu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MenuRepository(
    private val database: AppDatabase,
    private val apiService: MorfiPoloApiService
) {
    private val menuDao = database.menuDao()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun getMenuByDate(date: Date): Menu? {
        val dateString = dateFormat.format(date)
        return try {
            // Obtener todos los menús desde API
            val response = apiService.getMenus()
            
            if (response.isSuccessful) {
                val menusResponse = response.body()
                val menu = menusResponse?.data?.find { it.date == dateString }
                
                menu?.let {
                           android.util.Log.d("MenuRepository", "Menú encontrado: ${it.id}, opciones: ${it.getOptionsOrEmpty().size}")
                    // Guardar en base de datos local si es necesario
                    val menuEntity = menuToEntity(it)
                    menuDao.insertMenu(menuEntity)
                    it
                } ?: run {
                    android.util.Log.w("MenuRepository", "No se encontró menú para la fecha: $dateString")
                    null
                }
            } else {
                android.util.Log.e("MenuRepository", "Error en respuesta API: ${response.code()}")
                // Si falla, intentar desde base de datos local
                val timestamp = date.time
                val menuEntity = menuDao.getMenuByDate(timestamp)
                menuEntity?.let { entityToMenu(it) }
            }
        } catch (e: HttpException) {
            // Si falla, intentar desde base de datos local
            val timestamp = date.time
            val menuEntity = menuDao.getMenuByDate(timestamp)
            menuEntity?.let { entityToMenu(it) }
        } catch (e: IOException) {
            // Si no hay conexión, usar base de datos local
            val timestamp = date.time
            val menuEntity = menuDao.getMenuByDate(timestamp)
            menuEntity?.let { entityToMenu(it) }
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
        return try {
            android.util.Log.d("MenuRepository", "Obteniendo menús semanales desde API...")
            val response = apiService.getMenus()
            
            if (response.isSuccessful) {
                val menusResponse = response.body()
                val menus = menusResponse?.data ?: emptyList()
                
                android.util.Log.d("MenuRepository", "Menús obtenidos desde API: ${menus.size}")
                
                // Ordenar menús por fecha (más recientes primero)
                val sortedMenus = menus.sortedByDescending { menu ->
                    try {
                        dateFormat.parse(menu.date)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
                
                android.util.Log.d("MenuRepository", "Menús ordenados: ${sortedMenus.size}")
                
                // Guardar en base de datos local
                val menuEntities = sortedMenus.map { menuToEntity(it) }
                menuDao.insertMenus(menuEntities)
                
                android.util.Log.d("MenuRepository", "Menús guardados en BD local")
                
                sortedMenus
            } else {
                android.util.Log.w("MenuRepository", "Error en respuesta API: ${response.code()}, obteniendo desde BD local")
                // Si falla, obtener desde base de datos local
                getWeeklyMenusFromLocal()
            }
        } catch (e: Exception) {
            android.util.Log.e("MenuRepository", "Error al obtener menús semanales", e)
            // Si falla, obtener desde base de datos local
            getWeeklyMenusFromLocal()
        }
    }
    
    private suspend fun getWeeklyMenusFromLocal(): List<Menu> {
        android.util.Log.d("MenuRepository", "Obteniendo menús desde BD local...")
        
        // Obtener todos los menús de la BD local (ya están ordenados por fecha DESC en el query)
        val menuEntities = menuDao.getAllMenusSync()
        val menus = menuEntities.map { entityToMenu(it) }
        
        android.util.Log.d("MenuRepository", "Menús obtenidos desde BD local: ${menus.size}")
        
        return menus
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

    private fun menuToEntity(menu: Menu): MenuEntity {
        // Convertir date string a timestamp
        val date = try {
            dateFormat.parse(menu.date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        return MenuEntity(
            id = menu.id,
            fecha = date.time,
            descripcion = menu.description,
            horarioInicio = menu.start_time,
            horarioFin = menu.end_time,
            estado = menu.status
        )
    }

    private fun entityToMenu(entity: MenuEntity): Menu {
        // Convertir timestamp a date string
        val dateString = dateFormat.format(Date(entity.fecha))
        
        return Menu(
            id = entity.id,
            date = dateString,
            description = entity.descripcion,
            start_time = entity.horarioInicio,
            end_time = entity.horarioFin,
            status = entity.estado,
            created_at = "",
            updated_at = "",
            options = emptyList() // Las opciones no se guardan en la entidad por ahora
        )
    }
}
