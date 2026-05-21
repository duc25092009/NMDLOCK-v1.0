package com.nmdlock.app.feature.support

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

/**
 * Support screen with FAQ, contact info, and troubleshooting guides.
 */
@Composable
fun SupportScreen(
    onBack: () -> Unit = {},
) {
    var expandedFaq by remember { mutableStateOf<Int?>(null) }
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
                text = "Hỗ trợ",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Contact card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.HeadsetMic, null, tint = Purple400, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Liên hệ hỗ trợ", color = DarkText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("support@nmdlock.com", color = Purple400, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Phản hồi trong vòng 24h", color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
                    ) {
                        Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi email")
                    }
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
                    ) {
                        Icon(Icons.Default.BugReport, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Báo lỗi")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // FAQ section
        SectionHeader(title = "Câu hỏi thường gặp")
        Spacer(modifier = Modifier.height(8.dp))

        val faqs = listOf(
            "Làm thế nào để kích hoạt key?" to "Nhập key vào màn hình Key, nhấn Kích hoạt. Key sẽ được liên kết với thiết bị của bạn.",
            "Tôi có thể dùng key trên nhiều máy không?" to "Tùy vào loại key. Key 1 thiết bị chỉ dùng được 1 máy. Key 3 hoặc 5 thiết bị cho phép nhiều hơn.",
            "Key bị hết hạn thì làm sao?" to "Bạn cần mua key mới hoặc liên hệ admin để gia hạn. Vào mục Key để kiểm tra thời hạn.",
            "Làm thế nào để tối ưu game?" to "Vào mục Game Profile, chọn tab Free Fire hoặc Free Fire Max, chọn profile phù hợp với cấu hình máy.",
            "Tính năng tâm ngắm ảo là gì?" to "Tâm ngắm ảo hiển thị một crosshair trên màn hình giúp bạn ngắm dễ dàng hơn. Có thể tùy chỉnh kích thước và màu sắc.",
            "Dữ liệu của tôi có an toàn không?" to "Chúng tôi mã hóa dữ liệu nhạy cảm, không lưu mật khẩu dạng thô. Device ID được lưu an toàn trên thiết bị.",
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column {
                faqs.forEachIndexed { index, (question, answer) ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.HelpOutline, null, tint = Purple400, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                question,
                                color = DarkText,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(
                                onClick = { expandedFaq = if (expandedFaq == index) null else index },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    if (expandedFaq == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = DarkTextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (expandedFaq == index) {
                            Text(
                                answer,
                                color = DarkTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 14.dp),
                            )
                        }
                        if (index < faqs.size - 1) {
                            Divider(color = DarkSurface2, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Quick guides
        SectionHeader(title = "Hướng dẫn nhanh")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(
                    "📱 Cài đặt ứng dụng" to "Tải APK, cài đặt, cấp quyền theo hướng dẫn",
                    "🔑 Nhập key" to "Vào mục Key > Nhập mã > Kích hoạt > Kiểm tra",
                    "⚡ Tối ưu thiết bị" to "Vào Tối ưu > Chọn profile > Áp dụng",
                    "🎮 Chơi game" to "Vào Game Profile > Chọn game > Chọn cấu hình",
                ).forEach { (title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp))
                        Text(desc, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    Divider(color = DarkSurface2, thickness = 0.5.dp)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
