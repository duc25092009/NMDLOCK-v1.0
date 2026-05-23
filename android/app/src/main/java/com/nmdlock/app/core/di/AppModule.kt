package com.nmdlock.app.core.di

import android.content.Context
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.core.services.*
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

    @Provides
    @Singleton
    fun provideShizukuManager(
        @ApplicationContext context: Context
    ): ShizukuManager = ShizukuManager(context)

    @Provides
    @Singleton
    fun provideSystemInfoProvider(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
    ): SystemInfoProvider = SystemInfoProvider(context, shizukuManager)

    @Provides
    @Singleton
    fun provideNetworkDiagnostics(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
    ): NetworkDiagnostics = NetworkDiagnostics(context, shizukuManager)

    @Provides
    @Singleton
    fun provideOptimizationEngine(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
        systemInfoProvider: SystemInfoProvider,
    ): OptimizationEngine = OptimizationEngine(context, shizukuManager, systemInfoProvider)

    @Provides
    @Singleton
    fun provideGameProfileEngine(
        shizukuManager: ShizukuManager,
        optimizationEngine: OptimizationEngine,
    ): GameProfileEngine = GameProfileEngine(shizukuManager, optimizationEngine)

    @Provides
    @Singleton
    fun provideDnsManager(
        shizukuManager: ShizukuManager,
    ): DnsManager = DnsManager(shizukuManager)

    @Provides
    @Singleton
    fun provideThermalMonitor(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
    ): ThermalMonitor = ThermalMonitor(context, shizukuManager)

    @Provides
    @Singleton
    fun provideDoNotDisturbManager(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
    ): DoNotDisturbManager = DoNotDisturbManager(context, shizukuManager)

    // ── v3.0 New Services ──

    @Provides
    @Singleton
    fun providePredictiveThermalEngine(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
    ): PredictiveThermalEngine = PredictiveThermalEngine(context, shizukuManager)

    @Provides
    @Singleton
    fun provideShizukuCommandQueue(
        shizukuManager: ShizukuManager,
    ): ShizukuCommandQueue = ShizukuCommandQueue(shizukuManager)

    @Provides
    @Singleton
    fun provideCpuGovernorPID(
        commandQueue: ShizukuCommandQueue,
    ): CpuGovernorPID = CpuGovernorPID(commandQueue)

    @Provides
    @Singleton
    fun provideBurstSpeedTester(): BurstSpeedTester = BurstSpeedTester()

    @Provides
    @Singleton
    fun provideNetworkKernelTuner(
        shizukuManager: ShizukuManager,
    ): NetworkKernelTuner = NetworkKernelTuner(shizukuManager)

    @Provides
    @Singleton
    fun provideGameLifecycleManager(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
        commandQueue: ShizukuCommandQueue,
        profileEngine: GameProfileEngine,
        thermalEngine: PredictiveThermalEngine,
    ): GameLifecycleManager = GameLifecycleManager(context, shizukuManager, commandQueue, profileEngine, thermalEngine)

    @Provides
    @Singleton
    fun provideProfileAutoLearner(
        @ApplicationContext context: Context,
    ): ProfileAutoLearner = ProfileAutoLearner(context)

    @Provides
    @Singleton
    fun provideSmartAppKiller(
        @ApplicationContext context: Context,
        shizukuManager: ShizukuManager,
    ): SmartAppKiller = SmartAppKiller(context, shizukuManager)

    // ── Touch Sensitivity Modules (qwen v2) ──

    @Provides
    @Singleton
    fun provideTouchSensitivityBooster(
        shizukuManager: ShizukuManager,
        commandQueue: ShizukuCommandQueue,
    ): TouchSensitivityBooster = TouchSensitivityBooster(shizukuManager, commandQueue)

    @Provides
    @Singleton
    fun provideInputLatencyReducer(
        shizukuManager: ShizukuManager,
        commandQueue: ShizukuCommandQueue,
    ): InputLatencyReducer = InputLatencyReducer(shizukuManager, commandQueue)

    @Provides
    @Singleton
    fun provideGameTouchOptimizer(
        sensitivityBooster: TouchSensitivityBooster,
        latencyReducer: InputLatencyReducer,
        shizukuManager: ShizukuManager,
    ): GameTouchOptimizer = GameTouchOptimizer(sensitivityBooster, latencyReducer, shizukuManager)
}
