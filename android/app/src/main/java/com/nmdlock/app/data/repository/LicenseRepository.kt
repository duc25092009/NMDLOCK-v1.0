package com.nmdlock.app.data.repository

import android.os.Build
import com.nmdlock.app.data.remote.api.LicenseApi
import com.nmdlock.app.data.remote.dto.ActivateLicenseRequest
import com.nmdlock.app.data.remote.dto.ValidateLicenseRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor(
    private val licenseApi: LicenseApi
) {

    suspend fun activateLicense(
        key: String,
        deviceId: String
    ): Result<Boolean> {
        return try {

            val request = ActivateLicenseRequest(
                key = key,
                hwid = deviceId,
                device_model = "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            val response = licenseApi.activate(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(
                    Exception(
                        response.body()?.message ?: "Activation failed"
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateLicense(
        key: String,
        deviceId: String
    ): Result<Boolean> {

        return try {

            val request = ValidateLicenseRequest(
                key = key,
                hwid = deviceId
            )

            val response = licenseApi.validate(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(
                    Exception(
                        response.body()?.message ?: "Validation failed"
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
