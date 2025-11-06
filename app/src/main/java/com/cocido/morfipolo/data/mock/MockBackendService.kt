package com.cocido.morfipolo.data.mock

import com.cocido.morfipolo.domain.model.*
import java.util.*
import kotlin.random.Random

class MockBackendService {
    
    // Usuarios mock
    private val mockUsers = mutableListOf(
        User(
            id = 1,
            dni = "12345678",
            nombre = "Matías Federico",
            email = "matias@example.com",
            passwordHash = hashPassword("Ab12345678")
        ),
        User(
            id = 2,
            dni = "87654321",
            nombre = "Juan Pérez",
            email = "juan@example.com",
            passwordHash = hashPassword("Ab87654321")
        )
    )

    // Menús mock - se generan para la semana actual
    private val mockMenus = mutableListOf<Menu>()
    private val mockSelections = mutableListOf<MenuSelection>()

    init {
        generateWeeklyMenus()
    }

    private fun generateWeeklyMenus() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Asegurar que empezamos desde el lunes
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysToMonday)

        val menuDescriptions = listOf(
            "Arrollado de pollo con pure de papas",
            "Milanesa de carne con ensalada",
            "Pescado a la plancha con verduras",
            "Pastas con salsa bolognesa",
            "Pollo al horno con papas",
            "Empanadas de carne",
            "Asado con ensalada"
        )

        for (i in 0 until 7) {
            val menuDate = calendar.time
            val menu = Menu(
                id = (i + 1).toLong(),
                fecha = menuDate,
                descripcion = menuDescriptions[i],
                horarioInicio = "08:00",
                horarioFin = "11:00",
                estado = if (isWithinSelectionTime()) MenuStatus.ABIERTO else MenuStatus.CERRADO
            )
            mockMenus.add(menu)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun isWithinSelectionTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= 8 && hour < 11
    }

    // Simulación de login
    suspend fun login(dni: String, password: String): User? {
        // Simular delay de red
        Thread.sleep(500)
        
        val user = mockUsers.find { it.dni == dni }
        return if (user != null && verifyPassword(password, user.passwordHash)) {
            user
        } else {
            null
        }
    }

    // Obtener menú por fecha
    suspend fun getMenuByDate(date: Date): Menu? {
        Thread.sleep(200)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val targetTime = calendar.timeInMillis

        return mockMenus.find {
            val menuCalendar = Calendar.getInstance()
            menuCalendar.time = it.fecha
            menuCalendar.set(Calendar.HOUR_OF_DAY, 0)
            menuCalendar.set(Calendar.MINUTE, 0)
            menuCalendar.set(Calendar.SECOND, 0)
            menuCalendar.set(Calendar.MILLISECOND, 0)
            menuCalendar.timeInMillis == targetTime
        }
    }

    // Obtener menús de la semana
    suspend fun getWeeklyMenus(): List<Menu> {
        Thread.sleep(300)
        return mockMenus.toList()
    }

    // Seleccionar menú
    suspend fun selectMenu(userId: Long, menuId: Long): Boolean {
        Thread.sleep(300)
        
        val menu = mockMenus.find { it.id == menuId }
        if (menu == null || menu.estado != MenuStatus.ABIERTO) {
            return false
        }

        // Verificar si ya existe la selección
        if (mockSelections.any { it.userId == userId && it.menuId == menuId }) {
            return false
        }

        val selection = MenuSelection(
            id = Random.nextLong(1000, 9999),
            userId = userId,
            menuId = menuId,
            fechaSeleccion = Date()
        )
        mockSelections.add(selection)
        return true
    }

    // Deseleccionar menú
    suspend fun deselectMenu(userId: Long, menuId: Long): Boolean {
        Thread.sleep(300)
        val selection = mockSelections.find { it.userId == userId && it.menuId == menuId }
        return if (selection != null) {
            mockSelections.remove(selection)
            true
        } else {
            false
        }
    }

    // Verificar si el usuario ya seleccionó un menú
    suspend fun hasUserSelectedMenu(userId: Long, menuId: Long): Boolean {
        Thread.sleep(100)
        return mockSelections.any { it.userId == userId && it.menuId == menuId }
    }

    // Simular carga de menú por cocineros (para notificaciones)
    suspend fun loadMenuForToday(description: String): Menu? {
        Thread.sleep(500)
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val existingMenu = mockMenus.find {
            val menuCalendar = Calendar.getInstance()
            menuCalendar.time = it.fecha
            menuCalendar.set(Calendar.HOUR_OF_DAY, 0)
            menuCalendar.set(Calendar.MINUTE, 0)
            menuCalendar.set(Calendar.SECOND, 0)
            menuCalendar.set(Calendar.MILLISECOND, 0)
            menuCalendar.timeInMillis == today.timeInMillis
        }

        return if (existingMenu != null) {
            val updatedMenu = existingMenu.copy(descripcion = description, estado = MenuStatus.ABIERTO)
            mockMenus.remove(existingMenu)
            mockMenus.add(updatedMenu)
            updatedMenu
        } else {
            val newMenu = Menu(
                id = Random.nextLong(100, 999),
                fecha = today.time,
                descripcion = description,
                horarioInicio = "08:00",
                horarioFin = "11:00",
                estado = MenuStatus.ABIERTO
            )
            mockMenus.add(newMenu)
            newMenu
        }
    }

    // Obtener usuario por ID
    suspend fun getUserById(id: Long): User? {
        Thread.sleep(100)
        return mockUsers.find { it.id == id }
    }

    // Cambiar contraseña
    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): Boolean {
        Thread.sleep(500)
        val user = mockUsers.find { it.id == userId }
        return if (user != null && verifyPassword(oldPassword, user.passwordHash)) {
            val updatedUser = user.copy(passwordHash = hashPassword(newPassword))
            mockUsers.remove(user)
            mockUsers.add(updatedUser)
            true
        } else {
            false
        }
    }

    // Utilidades de hash de contraseña (simplificado para mock)
    private fun hashPassword(password: String): String {
        // En producción usar BCrypt o similar
        return password.hashCode().toString()
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}


