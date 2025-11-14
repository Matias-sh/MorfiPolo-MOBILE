package com.cocido.morfipolo.data.repository

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
                    Result.failure(Exception("Respuesta vacía del servidor"))
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
                    401 -> "No autorizado"
                    404 -> "Opción o menú no encontrado"
                    else -> "Error al crear voto: ${response.code()}"
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
                            "No se puede eliminar el voto en este momento."
                        }
                    }
                    401 -> "No autorizado"
                    404 -> "Voto no encontrado"
                    else -> "Error al eliminar voto: ${response.code()}"
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
    
    suspend fun getVotesForToday(): Result<List<Vote>> {
        return try {
            val response = apiService.getVotes()
            
            if (response.isSuccessful) {
                val votesResponse = response.body()
                
                if (votesResponse != null) {
                    Result.success(votesResponse.data)
                } else {
                    // Si el body es null, significa que Moshi no pudo parsear la respuesta
                    Result.failure(Exception("Error al parsear respuesta del servidor"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "No autorizado"
                    else -> "Error al obtener votos: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: com.squareup.moshi.JsonDataException) {
            Result.failure(Exception("Error al parsear respuesta: ${e.message}"))
        } catch (e: retrofit2.HttpException) {
            Result.failure(Exception("Error HTTP: ${e.code()} ${e.message()}"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
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

