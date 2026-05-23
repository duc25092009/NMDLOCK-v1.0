// File: app/src/main/java/com/nmdlock/app/data/repository/LicenseRepository.kt

package com.nmdlock.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor(
    private val licenseApi: LicenseApi,
    private val licenseDao: LicenseDao,
    private val prefs: AppPreferences
) {
    
    // ... existing code ...
    
    /**
     * FIX: Thêm hàm getLicenseCache nếu chưa có
     */
    fun getLicenseCache(): Flow<License?> = flow {
        emit(licenseDao.getLicense())
    }
    
    /**
     * FIX: Thêm hàm getMyLicense nếu chưa có
     */
    suspend fun getMyLicense(deviceId: String): Result<License> {
        return try {
            val response = licenseApi.getLicense(deviceId)
            if (response.isSuccessful && response.body() != null) {
                val license = response.body()!!
                licenseDao.insertLicense(license)
                Result.success(license)
            } else {
                // Fallback to cache
                val cached = licenseDao.getLicense()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("License not found"))
                }
            }
        } catch (e: Exception) {
            // Fallback to cache on error
            val cached = licenseDao.getLicense()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * FIX: Thêm hàm isLicenseValid nếu chưa có
     */
    suspend fun isLicenseValid(deviceId: String): Boolean {
        return try {
            val license = getMyLicense(deviceId).getOrNull()
            license?.isValid() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * FIX: Thêm hàm getHistory nếu chưa có
     */
    fun getHistory(): Flow<List<LicenseHistory>> = flow {
        emit(licenseDao.getLicenseHistory())
    }
    
    // ... existing code ...
}
