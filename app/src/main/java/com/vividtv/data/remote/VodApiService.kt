package com.vividtv.data.remote

import com.vividtv.data.source.vod.ApiHomeResponse
import com.vividtv.data.source.vod.ApiSearchResponse
import com.vividtv.data.source.vod.ApiStreamResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VodApiService {

    @GET("api/v1/home")
    suspend fun getHomePage(): ApiHomeResponse

    @GET("api/v1/search")
    suspend fun search(@Query("q") query: String): ApiSearchResponse

    @GET("api/v1/stream/{id}")
    suspend fun resolveStream(@Path("id") mediaId: String): ApiStreamResponse

    @GET("api/v1/health")
    suspend fun healthCheck()
}
