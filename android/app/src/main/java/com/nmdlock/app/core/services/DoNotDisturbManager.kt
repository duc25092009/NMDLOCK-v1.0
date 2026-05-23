package com.nmdlock.app.core.services

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages Do Not Disturb mode and notification blocking for gaming.
 * Uses Shizuku for shell-level control and NotificationManager for standard API.
 */
@Singleton
class DoNotDisturbManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
) {

    data class DndResult(
        val isSuccess: Boolean = false,
        val isEnabled: Boolean = false,
        val message: String = "",
        val blockedNotifications: Int = 0,
    )

    /**
     * Enable DND mode - blocks all notifications during gaming.
     * Priority only mode on Android 6+, Total silence via cmd on Android 8+.
     */
    suspend fun enableGameMode(): DndResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            // 1. Enable DND total silence (Android 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val dndResult = shizukuManager.executeCommand(
                    "cmd notification set_dnd_mode 2"
                )
                if (dndResult.isSuccess) {
                    actions.add("Đã bật DND total silence")
                }
            }

            // 2. Block heads-up notifications globally
            shizukuManager.executeCommand("settings put global heads_up_notifications_enabled 0")
            actions.add("Đã chặn thông báo nổi")

            // 3. Disable notification LED
            shizukuManager.executeCommand("settings put system notification_light_pulse 0")
            actions.add("Đã tắt đèn LED thông báo")

            // 4. Disable sound/vibration
            shizukuManager.executeCommand("settings put system vibrate_when_silent 0")
            actions.add("Đã tắt rung thông báo")

            // 5. Force immersive mode (hide status bar)
            shizukuManager.executeCommand(
                "settings put global policy_control immersive.preconfirms=apps"
            )
            actions.add("Đã bật chế độ toàn màn hình")

            DndResult(
                isSuccess = true,
                isEnabled = true,
                message = actions.joinToString("\n"),
            )
        } catch (e: Exception) {
            DndResult(isSuccess = false, message = "Lỗi: ${e.message}")
        }
    }

    /**
     * Disable DND mode - restore all notifications.
     */
    suspend fun disableGameMode(): DndResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            // 1. Turn off DND
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                shizukuManager.executeCommand("cmd notification set_dnd_mode 0")
                actions.add("Đã tắt DND")
            }

            // 2. Restore heads-up
            shizukuManager.executeCommand("settings put global heads_up_notifications_enabled 1")
            actions.add("Đã bật lại thông báo nổi")

            // 3. Restore LED
            shizukuManager.executeCommand("settings put system notification_light_pulse 1")
            actions.add("Đã bật lại đèn LED")

            // 4. Restore vibration
            shizukuManager.executeCommand("settings put system vibrate_when_silent 1")
            actions.add("Đã bật lại rung")

            // 5. Remove immersive mode
            shizukuManager.executeCommand("settings put global policy_control immersive.preconfirms=null")
            actions.add("Đã tắt chế độ toàn màn hình")

            DndResult(
                isSuccess = true,
                isEnabled = false,
                message = actions.joinToString("\n"),
            )
        } catch (e: Exception) {
            DndResult(isSuccess = false, message = "Lỗi: ${e.message}")
        }
    }

    /**
     * Get current DND state.
     */
    suspend fun getDndState(): String = withContext(Dispatchers.IO) {
        try {
            val result = shizukuManager.executeCommand("cmd notification get_dnd_mode")
            val mode = result.getOrNull()?.trim() ?: "unknown"
            when {
                mode.contains("2") -> "Total silence"
                mode.contains("1") -> "Priority only"
                mode.contains("3") -> "Alarms only"
                else -> "Tắt"
            }
        } catch (e: Exception) {
            "Không xác định"
        }
    }

    /**
     * Disable system auto updates during gaming.
     */
    suspend fun disableAutoUpdates(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Disable Play Store auto-update
            shizukuManager.executeCommand(
                "settings put global auto_update_enabled 0"
            )
            // Disable app auto-verify
            shizukuManager.executeCommand(
                "settings put global verifier_verify_adb_installs 0"
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Re-enable auto updates.
     */
    suspend fun enableAutoUpdates(): Boolean = withContext(Dispatchers.IO) {
        try {
            shizukuManager.executeCommand("settings put global auto_update_enabled 1")
            shizukuManager.executeCommand("settings put global verifier_verify_adb_installs 1")
            true
        } catch (e: Exception) {
            false
        }
    }
}
