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
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("VoteRepository", "Error al crear voto: ${response.code()}, body: $errorBody")
                val errorMessage = when (response.code()) {
                    400 -> {
                        // Si el error es que ya existe un voto, devolver un mensaje más claro
                        if (errorBody?.contains("already voted") == true) {
                            "Ya tienes un voto registrado para este menú"
                        } else {
                            "Datos inválidos"
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
                val errorMessage = when (response.code()) {
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
            
            android.util.Log.d("VoteRepository", "getVotesForToday: response.isSuccessful = ${response.isSuccessful}, code = ${response.code()}")
            
            if (response.isSuccessful) {
                val votesResponse = response.body()
                android.util.Log.d("VoteRepository", "Respuesta parseada: ${if (votesResponse != null) "Sí" else "No"}")
                
                if (votesResponse != null) {
                    android.util.Log.d("VoteRepository", "Votos en respuesta: ${votesResponse.data.size}")
                    if (votesResponse.data.isNotEmpty()) {
                        val firstVote = votesResponse.data[0]
                        android.util.Log.d("VoteRepository", "Primer voto: ${firstVote.id}, usuario: ${firstVote.user.id}, menu: ${firstVote.menu.id}, option: ${firstVote.option.id} (${firstVote.option.name})")
                        android.util.Log.d("VoteRepository", "menu.options es null: ${firstVote.menu.options == null}")
                    }
                    Result.success(votesResponse.data)
                } else {
                    android.util.Log.e("VoteRepository", "ERROR: Respuesta body es null - el parseo de Moshi falló")
                    // Si el body es null, significa que Moshi no pudo parsear la respuesta
                    // Esto puede pasar si hay un error en los modelos
                    Result.failure(Exception("Error al parsear respuesta del servidor"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("VoteRepository", "Error en respuesta: ${response.code()}, body: $errorBody")
                val errorMessage = when (response.code()) {
                    401 -> "No autorizado"
                    else -> "Error al obtener votos: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: com.squareup.moshi.JsonDataException) {
            android.util.Log.e("VoteRepository", "JsonDataException - Error de parseo de Moshi", e)
            android.util.Log.e("VoteRepository", "Mensaje: ${e.message}")
            android.util.Log.e("VoteRepository", "Stack trace:")
            e.printStackTrace()
            Result.failure(Exception("Error al parsear respuesta: ${e.message}"))
        } catch (e: retrofit2.HttpException) {
            android.util.Log.e("VoteRepository", "HttpException al obtener votos", e)
            Result.failure(Exception("Error HTTP: ${e.code()} ${e.message()}"))
        } catch (e: java.io.IOException) {
            android.util.Log.e("VoteRepository", "IOException al obtener votos", e)
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: Exception) {
            android.util.Log.e("VoteRepository", "Excepción al obtener votos: ${e.javaClass.simpleName}", e)
            android.util.Log.e("VoteRepository", "Mensaje: ${e.message}")
            android.util.Log.e("VoteRepository", "Stack trace completo:")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getUserVoteForMenu(menuId: String, userId: String): Vote? {
        return try {
            val votesResult = getVotesForToday()
            val votes = votesResult.getOrNull() ?: emptyList()
            android.util.Log.d("VoteRepository", "Buscando voto para menuId: $menuId, userId: $userId")
            android.util.Log.d("VoteRepository", "Total de votos obtenidos: ${votes.size}")
            val userVote = votes.find { vote ->
                vote.menu.id == menuId && vote.user.id == userId
            }
            android.util.Log.d("VoteRepository", "Voto encontrado: ${if (userVote != null) "Sí (${userVote.id})" else "No"}")
            userVote
        } catch (e: Exception) {
            android.util.Log.e("VoteRepository", "Error al obtener voto del usuario", e)
            null
        }
    }
    
    suspend fun createVoteOrReplace(optionId: String, menuId: String, userId: String): Result<Vote> {
        return try {
            // Primero verificar si ya existe un voto y eliminarlo
            val existingVote = getUserVoteForMenu(menuId, userId)
            existingVote?.let {
                val deleteResult = deleteVote(it.id)
                if (deleteResult.isFailure) {
                    android.util.Log.w("VoteRepository", "No se pudo eliminar voto existente: ${deleteResult.exceptionOrNull()?.message}")
                }
            }
            
            // Crear nuevo voto
            createVote(optionId, menuId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

