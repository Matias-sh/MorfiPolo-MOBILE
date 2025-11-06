package com.cocido.morfipolo.data.repository

import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.database.entity.UserEntity
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.mock.MockBackendService
import com.cocido.morfipolo.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    private val database: AppDatabase,
    private val mockBackend: MockBackendService,
    private val sessionManager: SessionManager
) {
    private val userDao = database.userDao()

    suspend fun login(dni: String, password: String): Result<User> {
        return try {
            // Intentar login con mock backend
            val user = mockBackend.login(dni, password)
            if (user != null) {
                // Guardar en base de datos local
                val userEntity = UserEntity(
                    id = user.id,
                    dni = user.dni,
                    nombre = user.nombre,
                    email = user.email,
                    passwordHash = user.passwordHash
                )
                userDao.insertUser(userEntity)
                
                // Guardar sesión
                sessionManager.saveSession(user.id, user.dni, user.nombre)
                
                Result.success(user)
            } else {
                Result.failure(Exception("DNI o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        val userId = sessionManager.getCurrentUserId() ?: return null
        return try {
            // Primero intentar desde la base de datos local
            val userEntity = userDao.getUserById(userId)
            if (userEntity != null) {
                User(
                    id = userEntity.id,
                    dni = userEntity.dni,
                    nombre = userEntity.nombre,
                    email = userEntity.email,
                    passwordHash = userEntity.passwordHash
                )
            } else {
                // Si no está en local, obtener del backend
                mockBackend.getUserById(userId)?.also { user ->
                    val userEntity = UserEntity(
                        id = user.id,
                        dni = user.dni,
                        nombre = user.nombre,
                        email = user.email,
                        passwordHash = user.passwordHash
                    )
                    userDao.insertUser(userEntity)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): Result<Boolean> {
        return try {
            val success = mockBackend.changePassword(userId, oldPassword, newPassword)
            if (success) {
                // Actualizar en base de datos local
                val userEntity = userDao.getUserById(userId)
                if (userEntity != null) {
                    // En producción, hashear la nueva contraseña
                    val updatedEntity = userEntity.copy(passwordHash = newPassword.hashCode().toString())
                    userDao.updateUser(updatedEntity)
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Contraseña actual incorrecta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
}


