package com.vividtv.data.remote

import com.vividtv.data.source.overseas.OverseasHomeResponse
import com.vividtv.data.source.overseas.OverseasSearchResponse
import com.vividtv.data.source.overseas.OverseasStreamResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OverseasApiService {

    @GET("api/v1/home")
    suspend fun getHomePage(@retrofit2.http.Url baseUrl: String): OverseasHomeResponse

    @GET("api/v1/search")
    suspend fun search(
        @retrofit2.http.Url baseUrl: String,
        @Query("q") query: String,
    ): OverseasSearchResponse

    @GET("api/v1/stream/{id}")
    suspend fun resolveStream(
        @retrofit2.http.Url baseUrl: String,
        @Path("id") mediaId: String,
    ): OverseasStreamResponse

    @GET("api/v1/health")
    suspend fun healthCheck(@retrofit2.http.Url baseUrl: String)
}
