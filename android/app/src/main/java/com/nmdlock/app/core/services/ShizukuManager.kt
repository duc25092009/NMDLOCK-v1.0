package com.nmdlock.app.core.services

import android.content.Context
import android.util.Log
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manages Shizuku integration for elevated shell command execution.
 * Provides both Shizuku (elevated) and fallback Runtime.exec (non-root) modes.
 */
@Singleton
class ShizukuManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "ShizukuManager"
    }

    private var shizukuAvailable = false

    /**
     * Check if Shizuku is running and we have permission.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val isRunning = clazz.getMethod("isRunning").invoke(null) as? Boolean ?: false
            val hasPermission = clazz.getMethod("checkSelfPermission").invoke(null) as? Int ?: -1
            shizukuAvailable = isRunning && hasPermission == 0
            shizukuAvailable
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku not available: ${e.message}")
            shizukuAvailable = false
            false
        }
    }

    /**
     * Execute a shell command - uses Shizuku if available, falls back to Runtime.exec.
     * Returns stdout of the command.
     */
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (isShizukuAvailable()) {
                executeViaShizuku(command)
            } else {
                executeViaRuntime(command)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command failed: $command", e)
            Result.failure(e)
        }
    }

    /**
     * Execute command via Shizuku newProcess API (elevated).
     */
    private fun executeViaShizuku(command: String): Result<String> {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcess = shizukuClass.getMethod(
                "newProcess",
                Array<String>::class.java,
                String::class.java,
                Array<String>::class.java
            )

            // Split command into parts
            val cmdParts = if (command.contains("|") || command.contains(">") || command.contains(";")) {
                arrayOf("sh", "-c", command)
            } else {
                command.split("\\s+".toRegex()).toTypedArray()
            }

            val process = newProcess.invoke(null, cmdParts, null, null) as? Process
                ?: return Result.failure(Exception("Failed to create Shizuku process"))

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()

            if (stderr.isNotBlank() && stdout.isBlank()) {
                Result.failure(Exception(stderr.trim()))
            } else {
                Result.success(stdout.trim())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku execution failed, falling back: ${e.message}")
            executeViaRuntime(command)
        }
    }

    /**
     * Execute command via standard Runtime.exec (no root).
     */
    private fun executeViaRuntime(command: String): Result<String> {
        return try {
            val cmdParts = arrayOf("sh", "-c", command)
            val process = Runtime.getRuntime().exec(cmdParts)
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()

            if (stderr.isNotBlank() && stdout.isBlank()) {
                Result.failure(Exception(stderr.trim()))
            } else {
                Result.success(stdout.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Execute multiple commands sequentially and return all results.
     */
    suspend fun executeCommands(vararg commands: String): List<Result<String>> {
        val results = mutableListOf<Result<String>>()
        for (cmd in commands) {
            results.add(executeCommand(cmd))
        }
        return results
    }

    /**
     * Alias for executeCommand — used by ShizukuCommandQueue.
     */
    suspend fun executeShellCommand(command: String): Result<String> {
        return executeCommand(command)
    }
}
