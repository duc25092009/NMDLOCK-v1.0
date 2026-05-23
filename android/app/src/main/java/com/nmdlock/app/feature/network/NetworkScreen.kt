package com.nmdlock.app.feature.network

import androidx.compose.foundation.clickable
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
 * Network diagnostics screen — REAL tests with Cyber Slate design.
 */
@Composable
fun NetworkScreen() {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val viewModel: NetworkViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(title = "Kiem tra mang duong truyen")
        Spacer(modifier = Modifier.height(12.dp))

        // Ping test — REAL, matches preview .network-card
        NetworkCard(
            title = "Do tre he thong (Ping)",
            value = if (uiState.pingAvgMs != null) "${uiState.pingAvgMs} ms" else "Nhan de kiem tra",
            subtitle = "Mat goi tin: ${uiState.pingLoss ?: 0}% • Do on dinh truyen tai: ${
                when {
                    uiState.pingAvgMs != null && uiState.pingAvgMs!! < 50 -> "Xuat sac"
                    uiState.pingAvgMs != null && uiState.pingAvgMs!! < 150 -> "Tot"
                    uiState.pingAvgMs != null -> "Trung binh"
                    else -> "—"
                }
            }",
            modifier = Modifier.padding(horizontal = 16.dp),
            valueColor = when {
                uiState.pingAvgMs != null && uiState.pingAvgMs!! < 50 -> Success
                uiState.pingAvgMs != null && uiState.pingAvgMs!! < 150 -> Warning
                else -> AccentCyan
            },
            onRefresh = if (!uiState.isTesting) ({ scope.launch { viewModel.testPing() } }) else null,
            isLoading = uiState.isTesting,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Speed test — REAL
        NetworkCard(
            title = "Bang thong Download",
            value = uiState.speedMbps ?: "Nhan de kiem tra",
            subtitle = uiState.speedMessage ?: "Phu hop muot ma cho trai nghiem game truc tuyen lien tuc.",
            modifier = Modifier.padding(horizontal = 16.dp),
            valueColor = if (uiState.speedMbps != null) Success else DarkTextSecondary,
            onRefresh = if (!uiState.isTesting) ({ scope.launch { viewModel.testSpeed() } }) else null,
            isLoading = uiState.isTesting,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // DNS + Stability row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // DNS card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp, 18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("DNS", color = DarkTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (uiState.isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(
                                onClick = { scope.launch { viewModel.testDns() } },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Default.Refresh, "Check DNS", tint = AccentCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.dnsIps.ifEmpty { "—" },
                        color = DarkText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                    if (uiState.dnsTimeMs != null) {
                        Text("${uiState.dnsTimeMs}ms", color = DarkTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            // Stability card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp, 18.dp)) {
                    Text("Do on dinh", color = DarkTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.stabilityText,
                        color = uiState.stabilityColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // DNS suggestions — matches preview
        SectionHeader(title = "He thong DNS toi uu khuyen dung")
        Spacer(modifier = Modifier.height(12.dp))

        listOf(
            "Google Public DNS" to "8.8.8.8 / 8.8.4.4",
            "Cloudflare DNS Premium" to "1.1.1.1 / 1.0.0.1",
            "OpenDNS" to "208.67.222.222 / 208.67.220.220",
        ).forEach { (name, dns) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(name, color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(dns, color = DarkTextSecondary, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    StatusBadge(
                        text = "SU DUNG",
                        color = Success,
                        modifier = Modifier.clickable {
                            scope.launch { viewModel.testCustomDns(name) }
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
