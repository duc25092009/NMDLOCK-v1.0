package com.nmdlock.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmdlock.app.core.ui.theme.*

// ──────────────────────────────────────────────
// GRADIENT BUTTON — matches preview HTML .btn-primary
// ──────────────────────────────────────────────
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp)),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = DarkSurface2,
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AccentPurple, AccentCyan)
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

// ──────────────────────────────────────────────
// GLOW CARD — glass morphism with subtle border glow
// matches preview HTML .key-status and .key-detail-card
// ──────────────────────────────────────────────
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

// ──────────────────────────────────────────────
// STAT CARD — matches preview HTML .stat-card
// ──────────────────────────────────────────────
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = DarkText,
    onClick: (() -> Unit)? = null,
) {
    val animatedColor by animateColorAsState(
        targetValue = valueColor,
        animationSpec = tween(300),
    )

    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = animatedColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextSecondary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ──────────────────────────────────────────────
// FEATURE CARD — matches preview HTML .feature-card
// ──────────────────────────────────────────────
@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = AccentCyan,
    isActive: Boolean = false,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) DarkSurface2 else DarkSurface,
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon box with rounded corners
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DarkText,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DarkTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────
// STATUS BADGE — matches preview HTML .badge
// ──────────────────────────────────────────────
@Composable
fun StatusBadge(
    text: String,
    color: Color = Success,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val paddingH = if (small) 8.dp else 11.dp
    val paddingV = if (small) 3.dp else 5.dp
    val fontSize = if (small) 9.sp else 10.sp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = paddingH, vertical = paddingV),
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

// ──────────────────────────────────────────────
// ALERT BANNER — matches preview HTML .alert
// ──────────────────────────────────────────────
@Composable
fun AlertBanner(
    message: String,
    type: AlertType = AlertType.WARNING,
    modifier: Modifier = Modifier,
) {
    val color = when (type) {
        AlertType.WARNING -> Warning
        AlertType.ERROR -> Error
        AlertType.INFO -> Info
        AlertType.SUCCESS -> Success
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (type) {
                    AlertType.WARNING -> Icons.Default.Warning
                    AlertType.ERROR -> Icons.Default.Error
                    AlertType.INFO -> Icons.Default.Info
                    AlertType.SUCCESS -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

enum class AlertType { WARNING, ERROR, INFO, SUCCESS }

// ──────────────────────────────────────────────
// TOGGLE SWITCH — matches preview HTML .toggle-switch
// ──────────────────────────────────────────────
@Composable
fun NMDToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    accentColor: Color = AccentPurple,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accentColor,
            uncheckedThumbColor = DarkTextSecondary,
            uncheckedTrackColor = DarkSurface2,
        ),
    )
}

// ──────────────────────────────────────────────
// SECTION HEADER — matches preview HTML .section-header
// ──────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(listOf(AccentPurple, AccentCyan))
                    ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DarkText,
                fontWeight = FontWeight.Bold,
            )
        }
        action?.invoke()
    }
}

// ──────────────────────────────────────────────
// LOADING OVERLAY — matches preview HTML .opt-modal-overlay
// ──────────────────────────────────────────────
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String = "Dang xu ly...",
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x0A060713).copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Spinner with gradient
                    Box(modifier = Modifier.size(50.dp)) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(50.dp),
                        )
                        CircularProgressIndicator(
                            color = AccentPurple.copy(alpha = 0.3f),
                            strokeWidth = 4.dp,
                            modifier = Modifier
                                .size(50.dp)
                                .padding(2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = message,
                        color = DarkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// EMPTY STATE
// ──────────────────────────────────────────────
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DarkTextSecondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = DarkText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ──────────────────────────────────────────────
// PROFILE CHIP — gradient selectable chip
// ──────────────────────────────────────────────
@Composable
fun ProfileChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.background(
                        Brush.horizontalGradient(listOf(AccentPurple, AccentCyan))
                    )
                } else {
                    Modifier.background(DarkSurface2)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else DarkTextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ──────────────────────────────────────────────
// TOAST — slide-up notification matching preview
// ──────────────────────────────────────────────
@Composable
fun NMDToast(
    message: String,
    type: AlertType = AlertType.SUCCESS,
    visible: Boolean = false,
) {
    val color = when (type) {
        AlertType.WARNING -> Warning
        AlertType.ERROR -> Error
        AlertType.INFO -> Info
        AlertType.SUCCESS -> Success
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(250)) { it } + fadeIn(tween(250)),
        exit = fadeOut(tween(250)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// STATUS CHIP — matches preview HTML .status-chip
// ──────────────────────────────────────────────
@Composable
fun StatusChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AccentCyan,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp, 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = DarkTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ──────────────────────────────────────────────
// DETAIL ROW — key-value pair
// ──────────────────────────────────────────────
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DarkText,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = DarkTextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

// ──────────────────────────────────────────────
// QUICK TOGGLE CARD — matches preview HTML .quick-toggle
// ──────────────────────────────────────────────
@Composable
fun QuickToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AccentPurple,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) accentColor.copy(alpha = 0.08f) else DarkSurface,
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isEnabled) accentColor.copy(alpha = 0.2f)
                        else DarkSurface2
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) accentColor else DarkTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isEnabled) accentColor else DarkText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = subtitle,
                    color = if (isEnabled) accentColor.copy(alpha = 0.8f) else DarkTextSecondary,
                    fontSize = 12.sp,
                )
            }
            NMDToggle(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                accentColor = accentColor,
            )
        }
    }
}

// ──────────────────────────────────────────────
// KEY-STATUS CARD — matches preview HTML .key-status
// ──────────────────────────────────────────────
@Composable
fun KeyStatusCard(
    title: String,
    subtitle: String,
    isValid: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isValid) Success.copy(alpha = 0.15f)
                        else Warning.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isValid) Icons.Default.VpnKey else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isValid) Success else Warning,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DarkText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = subtitle,
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                )
            }
            StatusBadge(
                text = if (isValid) "ACTIVE" else "INACTIVE",
                color = if (isValid) Success else Warning,
            )
        }
    }
}

// ──────────────────────────────────────────────
// DEVICE ID CARD — matches preview HTML .device-id-card
// ──────────────────────────────────────────────
@Composable
fun DeviceIdCard(
    deviceId: String,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = AccentPurple,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DEVICE ID CUA BAN",
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = deviceId,
                    color = DarkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// NETWORK CARD — matches preview HTML .network-card
// ──────────────────────────────────────────────
@Composable
fun NetworkCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AccentCyan,
    onRefresh: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp, 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = DarkTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (onRefresh != null) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(28.dp, 28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = DarkTextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}
