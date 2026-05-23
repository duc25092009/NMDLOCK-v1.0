package com.nmdlock.app.feature.support

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmdlock.app.core.ui.components.SectionHeader
import com.nmdlock.app.core.ui.theme.*

/**
 * Support screen with social links, FAQ, and contact info.
 * Design matches NMDLock-App-Preview-v2.html Cyber Slate theme.
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
        // ─── Header ───
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
                text = "Ho tro",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ─── Cong dong & Kenh truyen thong ───
        SectionHeader(title = "Cong dong & Kenh truyen thong")
        Spacer(modifier = Modifier.height(8.dp))

        // Zalo Group
        SocialCard(
            icon = Icons.Default.Chat,
            title = "Nhom Tro Chuyen Zalo",
            subtitle = "zalo.me/g/kqgqiq673",
            badge = "BOX",
            accentColor = Color(0xFF0068FF),
            onClick = { /* open Zalo */ },
        )
        Spacer(modifier = Modifier.height(2.dp))

        // Telegram Group
        SocialCard(
            icon = Icons.Default.Send,
            title = "Cong dong Telegram Box",
            subtitle = "t.me/+9ucZFEx2OOk2MDc1",
            badge = "JOIN",
            accentColor = Color(0xFF229ED9),
            onClick = { /* open Telegram */ },
        )
        Spacer(modifier = Modifier.height(2.dp))

        // TikTok 1
        SocialCard(
            icon = Icons.Default.MusicNote,
            title = "Kenh TikTok Chinh Thuc (1)",
            subtitle = "@mdisme.lovetiktok",
            badge = "WATCH",
            accentColor = Error,
            onClick = { /* open TikTok */ },
        )
        Spacer(modifier = Modifier.height(2.dp))

        // TikTok 2
        SocialCard(
            icon = Icons.Default.MusicNote,
            title = "Kenh TikTok Du Phong (2)",
            subtitle = "@mdlovetik",
            badge = "FOLLOW",
            accentColor = Error,
            onClick = { /* open TikTok */ },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Lien he truc tiep voi Admin ───
        SectionHeader(title = "Lien he truc tiep voi Admin")
        Spacer(modifier = Modifier.height(8.dp))

        // Admin Telegram
        SocialCard(
            icon = Icons.Default.Person,
            title = "Lien he qua Telegram Admin",
            subtitle = "@mdlvepa (Ho tro/Mua key)",
            badge = "CHAT",
            accentColor = Color(0xFF229ED9),
            onClick = { /* open Telegram admin */ },
        )
        Spacer(modifier = Modifier.height(2.dp))

        // Admin Zalo
        SocialCard(
            icon = Icons.Default.Phone,
            title = "Lien he qua Zalo Admin",
            subtitle = "Hotline: 0866.201.394",
            badge = "CALL",
            accentColor = Color(0xFF0068FF),
            onClick = { /* open Zalo admin */ },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Thong bao ho tro ───
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = "Moi phan hoi loi hoac yeu cau gia han License, quy khach xin vui long lien he cac kenh truyen thong chinh thuc phia tren de duoc phan hoi trong vong 24 gio.",
                color = DarkTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Cau hoi thuong gap ───
        SectionHeader(title = "Cau hoi thuong gap")
        Spacer(modifier = Modifier.height(8.dp))

        val faqs = listOf(
            "Lam the nao de kich hoat key?" to "Nhap key vao man hinh Key, nhan Kich hoat. Key se duoc lien ket voi thiet bi cua ban.",
            "Toi co the dung key tren nhieu may khong?" to "Tuy vao loai key. Key 1 thiet bi chi dung duoc 1 may. Key 3 hoac 5 thiet bi cho phep nhieu hon.",
            "Key bi het han thi lam sao?" to "Ban can mua key moi hoac lien he admin de gia han. Vao muc Key de kiem tra thoi han.",
            "Lam the nao de toi uu game?" to "Vao muc Game Profile, chon tab Free Fire hoac Free Fire Max, chon profile phu hop voi cau hinh may.",
            "Tinh nang tam ngam ao la gi?" to "Tam ngam ao hien thi mot crosshair tren man hinh giup ban ngam de dang hon. Co the tuy chinh kich thuoc va mau sac.",
            "Du lieu cua toi co an toan khong?" to "Chung toi ma hoa du lieu nhay cam, khong luu mat khau dang tho. Device ID duoc luu an toan tren thiet bi.",
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                faqs.forEachIndexed { index, (question, answer) ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedFaq = if (expandedFaq == index) null else index
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.HelpOutline, null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                question,
                                color = DarkText,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Icon(
                                if (expandedFaq == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        AnimatedVisibility(visible = expandedFaq == index) {
                            Text(
                                answer,
                                color = DarkTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 12.dp),
                            )
                        }
                        if (index < faqs.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(Color.White.copy(alpha = 0.06f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Huong dan nhanh ───
        SectionHeader(title = "Huong dan nhanh")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                QuickGuideRow(icon = "📱", title = "Cai dat ung dung", desc = "Tai APK, cai dat, cap quyen theo huong dan")
                GuideDivider()
                QuickGuideRow(icon = "🔑", title = "Nhap key", desc = "Vao muc Key > Nhap ma > Kich hoat > Kiem tra")
                GuideDivider()
                QuickGuideRow(icon = "⚡", title = "Toi uu thiet bi", desc = "Vao Toi uu > Chon profile > Ap dung")
                GuideDivider()
                QuickGuideRow(icon = "🎮", title = "Choi game", desc = "Vao Game Profile > Chon game > Chon cau hinh")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Social Card Component ──
@Composable
private fun SocialCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color = AccentPurple,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Accent left border
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = DarkTextSecondary, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface2)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    badge,
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun QuickGuideRow(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(28.dp))
        Column {
            Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
            Text(desc, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GuideDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}
