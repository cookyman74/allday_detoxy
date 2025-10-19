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
import com.allday.detoxy.presentation.ui.report.components.DetoxyRiskCard
import com.allday.detoxy.presentation.ui.report.components.RecoveryTrendCard
import com.allday.detoxy.presentation.ui.report.components.DistractionTopCard
import java.text.SimpleDateFormat
import java.util.*

/**
 * 리포트 화면
 *
 * Week 2B: Task 2B.3.2 - 신규 카드 통합
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
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
                        text = "디톡시 리포트",
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
                // 빈 상태 처리
                if (!uiState.hasData && uiState.todaySessions.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
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
                                value = "${uiState.getTotalFocusMinutes()}",
                                unit = "분",
                                icon = Icons.Default.PlayArrow,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 성공 세션
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "성공 세션",
                                value = "${uiState.getSuccessSessionCount()}",
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
                                value = "${uiState.settings.currentStreak}",
                                unit = "일",
                                icon = Icons.Default.Favorite,
                                color = Color(0xFFFF6B35)
                            )

                            // 총 포인트
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "총 포인트",
                                value = "${uiState.settings.totalPoints}",
                                unit = "P",
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFC107)
                            )
                        }
                    }

                    // 성공률 표시
                    if (uiState.todaySessions.isNotEmpty()) {
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
                                            text = "${String.format("%.0f", uiState.getSuccessRate())}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { uiState.getSuccessRate() / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .padding(start = 24.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = when {
                                            uiState.getSuccessRate() >= 80 -> Color(0xFF4CAF50)
                                            uiState.getSuccessRate() >= 50 -> Color(0xFFFFC107)
                                            else -> Color(0xFFFF5252)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 신규 고급 통계 카드들 (Week 2B)
                    item {
                        Text(
                            text = "주간 인사이트",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // 위험 지수 카드
                    item {
                        DetoxyRiskCard(riskIndex = uiState.riskIndex)
                    }

                    // 회복률 추세 카드
                    item {
                        RecoveryTrendCard(recoveryTrend = uiState.recoveryTrend)
                    }

                    // 방해요인 Top 3 카드
                    item {
                        DistractionTopCard(distractions = uiState.topDistractions)
                    }

                    // 세션 리스트 섹션 헤더
                    if (uiState.todaySessions.isNotEmpty()) {
                        item {
                            Text(
                                text = "오늘의 세션",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // 세션 리스트
                        items(uiState.todaySessions.sortedByDescending { it.startTime }) { session ->
                            SessionCard(session = session)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 빈 상태 카드
 */
@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "아직 집중 세션이 없어요",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "첫 디톡시 세션을 시작하면\n주간 인사이트와 회복률 추세를 확인할 수 있어요!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 통계 카드
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
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
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 세션 카드
 */
@Composable
private fun SessionCard(
    session: FocusSession,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (session.success) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (session.success) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (session.success) "성공" else "실패",
                tint = if (session.success) Color(0xFF4CAF50) else Color(0xFFEF5350),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (session.success) "성공 세션" else "실패 세션",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${session.durationMinutes}분 • ${formatTime(session.startTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.success) {
                val points = session.durationMinutes * 10 // 1분당 10포인트
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+${points}P",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 시간 포맷팅
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
