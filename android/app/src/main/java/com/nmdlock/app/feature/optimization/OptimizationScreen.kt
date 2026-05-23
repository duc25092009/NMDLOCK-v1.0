package com.nmdlock.app.feature.optimization

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Optimization screen — REAL system optimization with Cyber Slate design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen() {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val viewModel: OptimizationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(title = "Phan tich & Toi uu hoa")
        Spacer(modifier = Modifier.height(12.dp))

        // Status chips — REAL data, matches preview .status-bar2
        val stats = uiState.systemStats
        if (stats != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusChip(
                    value = "${stats.ramUsedPercent.toInt()}%",
                    label = "RAM ban",
                    modifier = Modifier.weight(1f),
                    valueColor = if (stats.ramUsedPercent > 75) Error else AccentCyan,
                )
                StatusChip(
                    value = "${stats.cpuUsage.toInt()}%",
                    label = "CPU load",
                    modifier = Modifier.weight(1f),
                    valueColor = if (stats.cpuUsage > 80) Error else AccentCyan,
                )
                StatusChip(
                    value = "${stats.storageUsedPercent.toInt()}%",
                    label = "Bo nho ban",
                    modifier = Modifier.weight(1f),
                    valueColor = if (stats.storageUsedPercent > 85) Error else AccentCyan,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Quick optimize button — matches preview
        GradientButton(
            text = if (uiState.isOptimizing) "Dang toi uu..." else "Chay don dep he thong ngay",
            onClick = { scope.launch { viewModel.quickOptimize() } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            icon = Icons.Default.Bolt,
            enabled = !uiState.isOptimizing,
        )

        val lastResult = uiState.lastResult
        if (lastResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AlertBanner(message = lastResult, type = AlertType.SUCCESS, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Tools grid
        SectionHeader(title = "Cong cu")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Scan RAM
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                onClick = { scope.launch { viewModel.scanRam() } },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (uiState.isScanningRam) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Memory, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Quet RAM", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Clean cache
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                onClick = { scope.launch { viewModel.cleanCache() } },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (uiState.isCleaningCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.CleaningServices, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Don cache", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Heavy apps
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                onClick = { scope.launch { viewModel.scanHeavyApps() } },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (uiState.isScanningApps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Apps, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("App nang", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        val scanResult = uiState.scanResult
        if (scanResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AlertBanner(message = scanResult, type = AlertType.INFO, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Heavy apps list
        if (uiState.heavyApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            GlowCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Ung dung nang", color = DarkText, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.heavyApps.take(5).forEach { app ->
                    DetailRow(
                        label = app.appName,
                        value = "${app.cacheSizeKB / 1024}MB",
                        valueColor = Warning,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Full optimization
        SectionHeader(title = "Ho so dieu tiet phan cung")
        Spacer(modifier = Modifier.height(12.dp))

        GradientButton(
            text = "Toi uu toan bo",
            onClick = { scope.launch { viewModel.fullOptimize() } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            icon = Icons.Default.CleaningServices,
            enabled = !uiState.isOptimizing,
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}
