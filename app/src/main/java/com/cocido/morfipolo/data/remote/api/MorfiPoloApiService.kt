package com.cocido.morfipolo.data.remote.api

import com.cocido.morfipolo.domain.model.*
import retrofit2.Response
import retrofit2.http.*

interface MorfiPoloApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>
    
    @GET("menus")
    suspend fun getMenus(): Response<MenusResponse>
    
    @POST("votes")
    suspend fun createVote(@Body request: CreateVoteRequest): Response<Vote>
    
    @GET("votes")
    suspend fun getVotes(): Response<VotesResponse>
    
    @DELETE("votes/{voteId}")
    suspend fun deleteVote(@Path("voteId") voteId: String): Response<Unit>
    
    @PATCH("user/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}

