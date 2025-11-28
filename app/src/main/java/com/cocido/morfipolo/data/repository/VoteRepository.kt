package com.cocido.morfipolo.data.repository

import com.cocido.morfipolo.data.remote.SessionExpiredException
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.domain.model.CreateVoteRequest
import com.cocido.morfipolo.domain.model.Vote
import retrofit2.HttpException
import java.io.IOException

class VoteRepository(
    private val apiService: MorfiPoloApiService
) {
    
    suspend fun createVote(optionId: String, menuId: String): Result<Vote> {
        return try {
            val request = CreateVoteRequest(optionId, menuId)
            val response = apiService.createVote(request)
            
            if (response.isSuccessful) {
                val vote = response.body()
                if (vote != null) {
                    Result.success(vote)
                } else {
                    Result.failure(Exception("No se pudo registrar tu elección. Intenta de nuevo."))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    400 -> {
                        // Verificar diferentes tipos de errores 400
                        when {
                            errorBody.contains("already voted", ignoreCase = true) -> {
                                "Ya tienes un voto registrado para este menú"
                            }
                            errorBody.contains("cerrado", ignoreCase = true) ||
                            errorBody.contains("closed", ignoreCase = true) ||
                            errorBody.contains("horario", ignoreCase = true) ||
                            errorBody.contains("time", ignoreCase = true) ||
                            errorBody.contains("expired", ignoreCase = true) -> {
                                "El menú está cerrado. No puedes agregar votos fuera del horario de selección (08:00 - 11:00)."
                            }
                            else -> {
                                "No se puede agregar el voto en este momento."
                            }
                        }
                    }
                    401 -> throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    404 -> "No se encontró la opción o el menú seleccionado"
                    500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                    else -> "No se pudo registrar tu elección. Intenta de nuevo."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: SessionExpiredException) {
            // Propagar la excepción de sesión expirada
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            val errorMessage = when (e.code()) {
                500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                else -> "No se pudo registrar tu elección. Verifica tu conexión e intenta de nuevo."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. Verifica tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo registrar tu elección. Intenta de nuevo."))
        }
    }
    
    suspend fun deleteVote(voteId: String): Result<Boolean> {
        return try {
            val response = apiService.deleteVote(voteId)
            
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    400 -> {
                        // Si el error 400 es por horario cerrado o menú cerrado
                        if (errorBody.contains("cerrado", ignoreCase = true) ||
                            errorBody.contains("closed", ignoreCase = true) ||
                            errorBody.contains("horario", ignoreCase = true) ||
                            errorBody.contains("time", ignoreCase = true) ||
                            errorBody.contains("expired", ignoreCase = true)) {
                            "El menú está cerrado. No puedes quitar votos fuera del horario de selección (08:00 - 11:00)."
                        } else {
                            "No se puede quitar el voto en este momento."
                        }
                    }
                    401 -> throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    404 -> "No se encontró el voto"
                    500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                    else -> "No se pudo quitar tu elección. Intenta de nuevo."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: SessionExpiredException) {
            // Propagar la excepción de sesión expirada
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            val errorMessage = when (e.code()) {
                500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                else -> "No se pudo quitar tu elección. Verifica tu conexión e intenta de nuevo."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. Verifica tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo quitar tu elección. Intenta de nuevo."))
        }
    }
    
    suspend fun getVotesForToday(): Result<List<Vote>> {
        return try {
            val response = apiService.getVotes()
            
            if (response.isSuccessful) {
                val votesResponse = response.body()
                
                if (votesResponse != null) {
                    Result.success(votesResponse.data)
                } else {
                    Result.failure(Exception("No se pudo cargar la información. Intenta de nuevo."))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                    else -> "No se pudo cargar la información. Intenta de nuevo."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: SessionExpiredException) {
            // Propagar la excepción de sesión expirada
            throw e
        } catch (e: com.squareup.moshi.JsonDataException) {
            Result.failure(Exception("No se pudo cargar la información. Intenta de nuevo."))
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            val errorMessage = when (e.code()) {
                500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                else -> "No se pudo cargar la información. Verifica tu conexión e intenta de nuevo."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. Verifica tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar la información. Intenta de nuevo."))
        }
    }
    
    suspend fun getUserVoteForMenu(menuId: String, userId: String): Vote? {
        return try {
            val votesResult = getVotesForToday()
            val votes = votesResult.getOrNull() ?: emptyList()
            votes.find { vote ->
                vote.menu.id == menuId && vote.user.id == userId
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createVoteOrReplace(optionId: String, menuId: String, userId: String): Result<Vote> {
        return try {
            // Primero verificar si ya existe un voto y eliminarlo
            val existingVote = getUserVoteForMenu(menuId, userId)
            existingVote?.let {
                deleteVote(it.id)
            }
            
            // Crear nuevo voto
            createVote(optionId, menuId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

