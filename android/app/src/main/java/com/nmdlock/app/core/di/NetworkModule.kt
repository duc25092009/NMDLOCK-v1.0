package com.nmdlock.app.core.di

import com.nmdlock.app.BuildConfig
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.remote.api.AuthApi
import com.nmdlock.app.data.remote.api.DeviceApi
import com.nmdlock.app.data.remote.api.LicenseApi
import com.nmdlock.app.data.remote.api.ConfigApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.LOG_ENABLED) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        deviceIdManager: DeviceIdManager,
    ): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val deviceInfo = deviceIdManager.getDeviceInfo()

        val request = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("X-Device-ID", deviceInfo.deviceId)
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .header("X-Platform", "android")
            .apply {
                // Add auth token if available (managed by AuthRepository)
                val token = AuthTokenHolder.token
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideDeviceApi(retrofit: Retrofit): DeviceApi = retrofit.create(DeviceApi::class.java)

    @Provides
    @Singleton
    fun provideLicenseApi(retrofit: Retrofit): LicenseApi = retrofit.create(LicenseApi::class.java)

    @Provides
    @Singleton
    fun provideConfigApi(retrofit: Retrofit): ConfigApi = retrofit.create(ConfigApi::class.java)
}

/**
 * Singleton holder for JWT auth token.
 */
object AuthTokenHolder {
    var token: String? = null
    var refreshToken: String? = null
}
