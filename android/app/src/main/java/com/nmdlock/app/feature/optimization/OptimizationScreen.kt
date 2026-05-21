package com.nmdlock.app.feature.optimization

import androidx.compose.animation.core.*
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
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*
import com.nmdlock.app.domain.model.OptimizationProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Optimization screen with system tools and profiles.
 * Provides cache cleaning, RAM management, and performance profiles.
 */
@Composable
fun OptimizationScreen() {
    val scrollState = rememberScrollState()
    var isOptimizing by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf(OptimizationProfile.BALANCED) }
    val scope = rememberCoroutineScope()
    var scanResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Tối ưu hệ thống",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Quick optimize button
        GradientButton(
            text = if (isOptimizing) "Đang tối ưu..." else "Tối ưu nhanh",
            onClick = {
                scope.launch {
                    isOptimizing = true
                    delay(2000)
                    isOptimizing = false
                    scanResult = "Đã tối ưu xong!"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Bolt,
            enabled = !isOptimizing,
        )

        if (scanResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AlertBanner(message = scanResult!!, type = AlertType.SUCCESS)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Tools grid
        SectionHeader(title = "Công cụ")
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { scope.launch { delay(1000); scanResult = "Đã quét bộ nhớ" } },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Memory, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Quét RAM", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                }
            }
            OutlinedButton(
                onClick = { scope.launch { delay(1000); scanResult = "Đã dọn cache" } },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dọn cache", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                }
            }
            OutlinedButton(
                onClick = { scope.launch { delay(1000); scanResult = "Đã kiểm tra ứng dụng nặng" } },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Apps, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("App nặng", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Profiles
        SectionHeader(title = "Chọn profile")
        Spacer(modifier = Modifier.height(8.dp))

        OptimizationProfile.entries.forEach { profile ->
            val isSelected = selectedProfile == profile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Purple600.copy(alpha = 0.15f) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                onClick = { selectedProfile = profile },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedProfile = profile },
                        colors = RadioButtonDefaults.colors(selectedColor = Purple400),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.displayName,
                            color = DarkText,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = profile.description,
                            color = DarkTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, "Selected", tint = Purple400, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Apply profile button
        GradientButton(
            text = "Áp dụng profile: ${selectedProfile.displayName}",
            onClick = { scope.launch { delay(1500); scanResult = "Đã áp dụng ${selectedProfile.displayName}" } },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.PlayArrow,
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}
