package com.nmdlock.app.core.di

import android.content.Context
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.local.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context
    ): DataStoreManager = DataStoreManager(context)

    @Provides
    @Singleton
    fun provideDeviceIdManager(
        @ApplicationContext context: Context
    ): DeviceIdManager = DeviceIdManager(context)
}
