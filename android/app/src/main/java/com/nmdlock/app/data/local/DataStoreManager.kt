package com.nmdlock.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nmdlock_settings")

/**
 * Manages local app preferences via DataStore.
 * Stores auth tokens, device ID, theme, language, etc.
 */
class DataStoreManager(private val context: Context) {

    // ---- Auth Tokens ----
    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = access
            prefs[REFRESH_TOKEN_KEY] = refresh
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }

    // ---- Onboarding ----
    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE_KEY] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE_KEY] = true
        }
    }

    // ---- Theme ----
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_THEME_KEY] ?: true // Default to dark
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME_KEY] = isDark
        }
    }

    // ---- Language ----
    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: "vi"
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = lang
        }
    }

    // ---- Sound/Vibration ----
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SOUND_ENABLED_KEY] ?: true
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SOUND_ENABLED_KEY] = enabled
        }
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIBRATION_ENABLED_KEY] ?: true
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    // ---- Notifications ----
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    // ---- Auto Sync ----
    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_SYNC_KEY] ?: true
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_SYNC_KEY] = enabled
        }
    }

    // ---- Logging ----
    val loggingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LOGGING_ENABLED_KEY] ?: false
    }

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[LOGGING_ENABLED_KEY] = enabled
        }
    }

    // ---- License Cache (extended) ----
    val cachedLicenseStatus: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CACHED_LICENSE_STATUS]
    }

    val cachedLicenseKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CACHED_LICENSE_KEY]
    }

    val cachedLicenseType: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CACHED_LICENSE_TYPE]
    }

    val cachedLicenseExpiry: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CACHED_LICENSE_EXPIRY]
    }

    val cachedLicenseMaxDevices: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[CACHED_LICENSE_MAX_DEVICES] ?: 1
    }

    suspend fun setCachedLicenseStatus(status: String) {
        context.dataStore.edit { prefs ->
            prefs[CACHED_LICENSE_STATUS] = status
        }
    }

    suspend fun setFullLicenseCache(
        status: String,
        keyValue: String?,
        type: String?,
        expiresAt: String?,
        maxDevices: Int,
    ) {
        context.dataStore.edit { prefs ->
            prefs[CACHED_LICENSE_STATUS] = status
            if (keyValue != null) prefs[CACHED_LICENSE_KEY] = keyValue
            if (type != null) prefs[CACHED_LICENSE_TYPE] = type
            if (expiresAt != null) prefs[CACHED_LICENSE_EXPIRY] = expiresAt
            prefs[CACHED_LICENSE_MAX_DEVICES] = maxDevices
        }
    }

    suspend fun clearLicenseCache() {
        context.dataStore.edit { prefs ->
            prefs.remove(CACHED_LICENSE_STATUS)
            prefs.remove(CACHED_LICENSE_KEY)
            prefs.remove(CACHED_LICENSE_TYPE)
            prefs.remove(CACHED_LICENSE_EXPIRY)
            prefs.remove(CACHED_LICENSE_MAX_DEVICES)
        }
    }

    // ---- Active Profile ----
    val activeProfile: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_PROFILE_KEY] ?: "balanced"
    }

    suspend fun setActiveProfile(profile: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_PROFILE_KEY] = profile
        }
    }

    // ---- Clear all data (reset) ----
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val AUTO_SYNC_KEY = booleanPreferencesKey("auto_sync")
        private val LOGGING_ENABLED_KEY = booleanPreferencesKey("logging_enabled")
        private val CACHED_LICENSE_STATUS = stringPreferencesKey("cached_license_status")
        private val CACHED_LICENSE_KEY = stringPreferencesKey("cached_license_key")
        private val CACHED_LICENSE_TYPE = stringPreferencesKey("cached_license_type")
        private val CACHED_LICENSE_EXPIRY = stringPreferencesKey("cached_license_expiry")
        private val CACHED_LICENSE_MAX_DEVICES = intPreferencesKey("cached_license_max_devices")
        private val ACTIVE_PROFILE_KEY = stringPreferencesKey("active_profile")
    }
}
