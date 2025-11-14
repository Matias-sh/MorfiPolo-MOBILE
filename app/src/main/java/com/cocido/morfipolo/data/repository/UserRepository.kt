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
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Boolean> {
        return try {
            // CRÍTICO: Asegurar que el token esté actualizado antes de hacer la petición
            val validToken = tokenManager.getValidAccessToken()
            if (validToken == null) {
                return Result.failure(Exception("Sesión expirada. Por favor, inicia sesión nuevamente."))
            }
            
            val request = ChangePasswordRequest(
                password = oldPassword,
                newPassword = newPassword
            )
            
            val response = apiService.changePassword(request)
            
            if (response.isSuccessful) {
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
                    404 -> "El endpoint de cambio de contraseña no está disponible. Por favor, contacta al administrador."
                    else -> "Error al cambiar la contraseña: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Contraseña actual incorrecta"
                400 -> "Datos inválidos"
                403 -> "No tienes permiso para realizar esta acción"
                404 -> "El endpoint de cambio de contraseña no está disponible. Por favor, contacta al administrador."
                else -> "Error HTTP: ${e.code()} ${e.message()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
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
