package com.nmdlock.app.feature.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*

/**
 * Game profile screen for Free Fire optimization.
 * Provides device-specific profiles, graphics tuning, and crosshair overlay.
 * NOTE: All optimizations are device-side only - no gameplay manipulation.
 */
@Composable
fun GameProfileScreen(
    onBack: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedProfile by remember { mutableStateOf("Mượt") }
    var showCrosshairSettings by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = DarkText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Game Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText,
                fontWeight = FontWeight.Bold,
            )
        }

        // Tab selection: Free Fire | Free Fire Max
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Free Fire", "Free Fire Max").forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == index) Brush.horizontalGradient(listOf(Purple600, Purple400))
                            else DarkSurface2
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        color = if (selectedTab == index) Color.White else DarkTextSecondary,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Game name header
        Text(
            text = if (selectedTab == 0) "Free Fire" else "Free Fire Max",
            style = MaterialTheme.typography.titleLarge,
            color = DarkText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "Tối ưu trải nghiệm chơi game",
            style = MaterialTheme.typography.bodySmall,
            color = DarkTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Optimization profiles
        SectionHeader(title = "Chọn profile tối ưu")
        Spacer(modifier = Modifier.height(8.dp))

        val profiles = listOf(
            GameProfileItem("Mượt", "Giảm đồ họa, tăng FPS, ưu tiên mượt mà", Icons.Default.Speed, "Tối ưu cho máy cấu hình thấp"),
            GameProfileItem("Cân bằng", "Đồ họa trung bình, FPS ổn định", Icons.Default.Balance, "Phù hợp đa số thiết bị"),
            GameProfileItem("Hiệu năng", "Đồ họa cao, FPS tối đa, giảm tác vụ nền", Icons.Default.Bolt, "Máy cấu hình cao"),
            GameProfileItem("Tiết kiệm pin", "Giảm sáng, giảm FPS, tắt rung", Icons.Default.BatterySaver, "Chơi lâu, tiết kiệm pin"),
        )

        profiles.forEach { profile ->
            val isSelected = selectedProfile == profile.name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Purple600.copy(alpha = 0.15f) else DarkSurface
                ),
                shape = RoundedCornerShape(14.dp),
                onClick = { selectedProfile = profile.name },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Purple600.copy(alpha = 0.3f) else DarkSurface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            profile.icon,
                            null,
                            tint = if (isSelected) Purple400 else DarkTextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            profile.name,
                            color = DarkText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            profile.description,
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                        )
                        Text(
                            profile.detail,
                            color = DarkTextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Success),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Crosshair settings
        SectionHeader(title = "Tâm ngắm (Crosshair)")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showCrosshairSettings = !showCrosshairSettings },
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Crosshair, null, tint = Purple400, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tâm ngắm ảo", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Hiển thị tâm ngắm phụ hỗ trợ ngắm bắn", color = DarkTextSecondary, fontSize = 12.sp)
                }
                Icon(
                    if (showCrosshairSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = DarkTextSecondary,
                )
            }
        }

        // Crosshair settings panel
        AnimatedVisibility(visible = showCrosshairSettings) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Crosshair preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Crosshair visual
                        Box(contentAlignment = Alignment.Center) {
                            // Outer circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(1.5.dp, Color.Red.copy(alpha = 0.7f), CircleShape)
                            )
                            // Center dot
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            // Cross lines
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(1.5.dp)
                                    .background(Color.Red.copy(alpha = 0.5f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(24.dp)
                                    .background(Color.Red.copy(alpha = 0.5f))
                            )
                        }
                        Text(
                            "Xem trước tâm ngắm",
                            color = DarkTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Size slider
                    Text("Kích thước", color = DarkTextSecondary, fontSize = 13.sp)
                    Slider(
                        value = 0.5f,
                        onValueChange = { },
                        colors = SliderDefaults.colors(
                            thumbColor = Purple400,
                            activeTrackColor = Purple600,
                            inactiveTrackColor = DarkSurface2,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color options
                    Text("Màu sắc", color = DarkTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Red, Color.Green, Color.Cyan, Color.Yellow, Color.White).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if (color == Color.Red) Color.White.copy(alpha = 0.5f) else Color.Transparent, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opacity
                    Text("Độ trong suốt", color = DarkTextSecondary, fontSize = 13.sp)
                    Slider(
                        value = 0.8f,
                        onValueChange = { },
                        colors = SliderDefaults.colors(
                            thumbColor = Purple400,
                            activeTrackColor = Purple600,
                            inactiveTrackColor = DarkSurface2,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(
                            text = "Áp dụng",
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Check,
                        )
                        OutlinedButton(
                            onClick = { showCrosshairSettings = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary),
                        ) {
                            Text("Hoàn tác")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action buttons
        GradientButton(
            text = "Áp dụng cấu hình",
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            icon = Icons.Default.PlayArrow,
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}

private data class GameProfileItem(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val detail: String,
)
