package com.apktoolai.companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Curated Material 3 Color Palette
val StudioPrimary = Color(0xFF4F46E5)
val StudioPrimaryLight = Color(0xFF6366F1)
val StudioSuccess = Color(0xFF10B981)
val StudioWarning = Color(0xFFF59E0B)
val StudioDanger = Color(0xFFEF4444)
val StudioDarkBg = Color(0xFF0F172A)
val StudioSurface = Color(0xFF1E293B)
val StudioCardBg = Color(0xFFFFFFFF)
val StudioTextPrimary = Color(0xFF0F172A)
val StudioTextSecondary = Color(0xFF475569)
val StudioTextMuted = Color(0xFF94A3B8)

@Composable
fun StudioDashboardHeader(
    projectName: String?,
    decompileUsage: Int,
    decompileLimit: Int,
    compileUsage: Int,
    compileLimit: Int,
    keystoreUsage: Int,
    keystoreLimit: Int,
    onOpenEditor: () -> Unit,
    onBuildApk: () -> Unit,
    onUploadFirebase: () -> Unit
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Active Project Banner with Gradient
            AnimatedVisibility(
                visible = !projectName.isNullOrEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(StudioPrimary, StudioPrimaryLight)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTIVE WORKSPACE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC7D2FE)
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = StudioSuccess
                                ) {
                                    Text(
                                        text = "READY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = projectName ?: "MyProject.apk",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Text(
                                text = "Decompiled APK • Scoped & Isolated",
                                fontSize = 12.sp,
                                color = Color(0xFFE0E7FF),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenEditor,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = StudioPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = StudioPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Editor", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = onBuildApk,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioSuccess,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Recompile", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Quotas Quick Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    title = "DECOMPILES",
                    usage = decompileUsage,
                    limit = decompileLimit,
                    color = StudioPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "COMPILES",
                    usage = compileUsage,
                    limit = compileLimit,
                    color = StudioSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "KEYSTORES",
                    usage = keystoreUsage,
                    limit = keystoreLimit,
                    color = StudioWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricChip(
    title: String,
    usage: Int,
    limit: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = StudioTextSecondary
            )
            Text(
                text = "$usage / $limit",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            val progress = if (limit > 0) (usage.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}
