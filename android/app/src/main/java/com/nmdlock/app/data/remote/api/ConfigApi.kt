package com.nmdlock.app.data.remote.api

import com.nmdlock.app.data.remote.dto.ApiResponse
import com.nmdlock.app.data.remote.dto.ConfigResponse
import retrofit2.http.GET

/**
 * Configuration sync API endpoints.
 */
interface ConfigApi {

    @GET("config/latest")
    suspend fun getLatest(): ApiResponse<ConfigResponse>
}
