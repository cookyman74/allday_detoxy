package com.allday.detoxy.presentation.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.presentation.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val todaySessions by viewModel.todaySessions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 헤더
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "일일 리포트",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 메인 통계 카드들
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 총 집중 시간
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "총 집중 시간",
                            value = "${viewModel.getTotalFocusMinutes()}",
                            unit = "분",
                            icon = Icons.Default.PlayArrow,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 성공 세션
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "성공 세션",
                            value = "${viewModel.getSuccessSessionCount()}",
                            unit = "회",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 현재 스트릭
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "연속 성공",
                            value = "${settings.currentStreak}",
                            unit = "일",
                            icon = Icons.Default.Favorite,
                            color = Color(0xFFFF6B35)
                        )

                        // 총 포인트
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "총 포인트",
                            value = "${settings.totalPoints}",
                            unit = "P",
                            icon = Icons.Default.Star,
                            color = Color(0xFFFFC107)
                        )
                    }
                }

                // 성공률 표시
                if (todaySessions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "오늘의 성공률",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${String.format("%.0f", viewModel.getSuccessRate())}%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = viewModel.getSuccessRate() / 100f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .padding(start = 24.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = when {
                                        viewModel.getSuccessRate() >= 80 -> Color(0xFF4CAF50)
                                        viewModel.getSuccessRate() >= 50 -> Color(0xFFFFC107)
                                        else -> Color(0xFFFF5252)
                                    }
                                )
                            }
                        }
                    }
                }

                // 세션 리스트 섹션 헤더
                if (todaySessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "오늘의 세션",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // 세션 리스트
                    items(todaySessions.sortedByDescending { it.startTime }) { session ->
                        SessionCard(session = session)
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "아직 오늘의 세션이 없습니다",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "타이머를 시작하여 첫 세션을 만들어보세요!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SessionCard(session: FocusSession) {
    val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTimeStr = dateFormatter.format(Date(session.startTime))
    val endTimeStr = session.endTime?.let {
        dateFormatter.format(Date(it))
    } ?: "진행 중"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (session.success) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFFFF5252).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (session.success) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = if (session.success) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFFF5252)
                    },
                    modifier = Modifier.size(40.dp)
                )

                Column {
                    Text(
                        text = "${session.durationMinutes}분 세션",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (session.success) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+${session.durationMinutes}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107)
                    )
                    Text(
                        text = "P",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFC107)
                    )
                }
            } else {
                Text(
                    text = "포기",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF5252)
                )
            }
        }
    }
}