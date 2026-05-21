package com.nmdlock.app.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmdlock.app.core.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Onboarding screen with 3 introduction slides.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val slides = listOf(
        OnboardingSlide(
            icon = Icons.Default.Speed,
            title = "Tối ưu hiệu năng",
            description = "Dọn cache, quản lý RAM, tối ưu pin và CPU cho thiết bị của bạn",
            color = Success,
        ),
        OnboardingSlide(
            icon = Icons.Default.VpnKey,
            title = "Quản lý key thông minh",
            description = "Hệ thống key/licensing gắn với thiết bị, bảo mật đa lớp",
            color = Warning,
        ),
        OnboardingSlide(
            icon = Icons.Default.WifiTethering,
            title = "Tăng ổn định & Mạng",
            description = "Kiểm tra ping, đo tốc độ, tối ưu kết nối mạng toàn diện",
            color = Info,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onComplete) {
                Text("Bỏ qua", color = DarkTextSecondary, fontSize = 14.sp)
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(slide.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        slide.icon,
                        contentDescription = null,
                        tint = slide.color,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    slide.title,
                    color = DarkText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    slide.description,
                    color = DarkTextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // Indicators + Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Purple400
                                else DarkSurface2
                            )
                    )
                }
            }

            // Next / Start button
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.horizontalGradient(listOf(Purple600, Purple400)),
                            shape = RoundedCornerShape(16.dp),
                        )
                )
                Text(
                    text = if (pagerState.currentPage < 2) "Tiếp theo" else "Bắt đầu",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color,
)
