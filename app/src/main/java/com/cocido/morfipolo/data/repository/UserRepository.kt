package com.cocido.morfipolo.data.repository

import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.database.entity.UserEntity
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.TokenManager
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.domain.model.ChangePasswordRequest
import com.cocido.morfipolo.domain.model.LoginRequest
import com.cocido.morfipolo.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

class UserRepository(
    private val database: AppDatabase,
    private val apiService: MorfiPoloApiService,
    private val sessionManager: SessionManager,
    private val tokenManager: TokenManager
) {
    private val userDao = database.userDao()

    suspend fun login(dni: String, password: String): Result<User> {
        return try {
            val request = LoginRequest(dni, password)
            val response = apiService.login(request)
            
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    val user = loginResponse.user
                    
                    // Guardar tokens
                    sessionManager.saveTokens(
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )
                    
                    // Guardar sesión con password temporal para refresh token
                    sessionManager.saveSession(
                        user.id,
                        user.dni,
                        "${user.name} ${user.lastName}",
                        password
                    )
                    
                    // Guardar en base de datos local
                    val userEntity = UserEntity(
                        id = user.id,
                        dni = user.dni,
                        nombre = "${user.name} ${user.lastName}",
                        email = user.email,
                        passwordHash = "" // No guardamos password hash del API
                    )
                    userDao.insertUser(userEntity)
                    android.util.Log.d("UserRepository", "Usuario guardado en BD: ${user.id}, nombre: ${userEntity.nombre}")
                    
                    // Verificar que se guardó correctamente
                    val savedUser = userDao.getUserById(user.id)
                    if (savedUser == null) {
                        android.util.Log.e("UserRepository", "ERROR: Usuario no se guardó en BD después de insertar")
                    } else {
                        android.util.Log.d("UserRepository", "Usuario verificado en BD: ${savedUser.id}")
                    }
                    
                    Result.success(user)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "DNI o contraseña incorrectos"
                    400 -> "Datos inválidos"
                    else -> "Error al iniciar sesión: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.code()} ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
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
                android.util.Log.d("UserRepository", "Usuario encontrado en BD local: ${userEntity.id}")
                // Construir User desde entity (puede no tener todos los campos)
                val nameParts = userEntity.nombre.split(" ", limit = 2)
                User(
                    id = userEntity.id,
                    name = nameParts.firstOrNull() ?: "",
                    lastName = nameParts.getOrNull(1) ?: "",
                    email = userEntity.email ?: "",
                    dni = userEntity.dni,
                    dependence = "",
                    birthDate = "",
                    isActive = true,
                    roles = emptyList(),
                    createdAt = "",
                    updatedAt = ""
                )
            } else {
                android.util.Log.w("UserRepository", "Usuario no encontrado en BD local para ID: $userId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error al obtener usuario", e)
            null
        }
    }

    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Boolean> {
        return try {
            android.util.Log.d("UserRepository", "Cambiando contraseña para usuario: $userId")
            
            // CRÍTICO: Asegurar que el token esté actualizado antes de hacer la petición
            val validToken = tokenManager.getValidAccessToken()
            if (validToken == null) {
                android.util.Log.e("UserRepository", "No hay token válido para cambiar contraseña")
                return Result.failure(Exception("Sesión expirada. Por favor, inicia sesión nuevamente."))
            }
            
            val request = ChangePasswordRequest(
                password = oldPassword,
                newPassword = newPassword
            )
            
            android.util.Log.d("UserRepository", "Enviando petición de cambio de contraseña...")
            val response = apiService.changePassword(request)
            
            if (response.isSuccessful) {
                android.util.Log.d("UserRepository", "✅ Contraseña cambiada exitosamente")
                // Actualizar la contraseña guardada en SessionManager para el refresh token
                sessionManager.saveSession(
                    userId = userId,
                    dni = sessionManager.getCurrentUserDni() ?: "",
                    name = sessionManager.getCurrentUserName() ?: "",
                    password = newPassword
                )
                Result.success(true)
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Contraseña actual incorrecta"
                    400 -> "Datos inválidos"
                    403 -> "No tienes permiso para realizar esta acción"
                    404 -> {
                        android.util.Log.e("UserRepository", "⚠️ Endpoint no encontrado. Verifica que el endpoint /user/change-password esté disponible en el servidor.")
                        "El endpoint de cambio de contraseña no está disponible. Por favor, contacta al administrador."
                    }
                    else -> "Error al cambiar la contraseña: ${response.code()}"
                }
                android.util.Log.e("UserRepository", "Error al cambiar contraseña: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Contraseña actual incorrecta"
                400 -> "Datos inválidos"
                403 -> "No tienes permiso para realizar esta acción"
                404 -> {
                    android.util.Log.e("UserRepository", "⚠️ Endpoint no encontrado (404). Verifica que el endpoint /user/change-password esté disponible en el servidor.")
                    "El endpoint de cambio de contraseña no está disponible. Por favor, contacta al administrador."
                }
                else -> "Error HTTP: ${e.code()} ${e.message()}"
            }
            android.util.Log.e("UserRepository", "Error HTTP al cambiar contraseña", e)
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            android.util.Log.e("UserRepository", "Error de conexión al cambiar contraseña", e)
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error inesperado al cambiar contraseña", e)
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
