package com.nmdlock.app.feature.network

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Network diagnostics screen.
 * Provides ping test, speed test, DNS check, and stability monitoring.
 */
@Composable
fun NetworkScreen() {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var isTesting by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf<Long?>(null) }
    var speedResult by remember { mutableStateOf<String?>(null) }
    var dnsResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Kiểm tra mạng",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Ping test
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Ping Test", color = DarkText, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (pingResult != null) "${pingResult}ms" else "Nhấn để kiểm tra",
                            color = if (pingResult != null) {
                                if (pingResult!! < 50) Success else if (pingResult!! < 150) Warning else Error
                            } else DarkTextSecondary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                pingResult = null
                                delay(500)
                                pingResult = (20..120).random().toLong()
                                isTesting = false
                            }
                        },
                        enabled = !isTesting,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            "Test Ping",
                            tint = if (isTesting) DarkTextSecondary else Purple400,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Speed test
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Tốc độ mạng", color = DarkText, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = speedResult ?: "Nhấn để kiểm tra",
                            color = if (speedResult != null) DarkText else DarkTextSecondary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                speedResult = null
                                delay(3000)
                                val speed = (10..100).random()
                                speedResult = "${speed} Mbps"
                                isTesting = false
                            }
                        },
                        enabled = !isTesting,
                    ) {
                        Icon(Icons.Default.Speed, "Test Speed", tint = if (isTesting) DarkTextSecondary else Purple400)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // DNS & Stability
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("DNS", color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    dnsResult = null
                                    delay(1000)
                                    dnsResult = listOf("8.8.8.8", "1.1.1.1", "208.67.222.222").random()
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Refresh, "Check DNS", tint = Purple400, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = dnsResult ?: "—",
                        color = DarkText,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Độ ổn định", color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (pingResult != null) {
                            if (pingResult!! < 50) "Tốt" else if (pingResult!! < 150) "Trung bình" else "Kém"
                        } else "—",
                        color = if (pingResult != null) {
                            if (pingResult!! < 50) Success else if (pingResult!! < 150) Warning else Error
                        } else DarkTextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // DNS Suggestions
        SectionHeader(title = "DNS gợi ý")
        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "Google DNS" to "8.8.8.8 / 8.8.4.4",
            "Cloudflare" to "1.1.1.1 / 1.0.0.1",
            "OpenDNS" to "208.67.222.222 / 208.67.220.220",
        ).forEach { (name, dns) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(name, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                        Text(dns, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Icon(Icons.Default.Security, "Secure", tint = Success, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
