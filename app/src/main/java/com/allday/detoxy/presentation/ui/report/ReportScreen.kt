package com.allday.detoxy.presentation.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.presentation.viewmodel.ReportViewModel
import com.allday.detoxy.presentation.ui.component.GlassScaffold
import com.allday.detoxy.presentation.ui.component.GlassSurface
import com.allday.detoxy.presentation.ui.component.liquidGlass
import com.allday.detoxy.presentation.ui.report.components.DetoxyRiskCard
import com.allday.detoxy.presentation.ui.report.components.RecoveryTrendCard
import com.allday.detoxy.presentation.ui.report.components.DistractionTopCard
import com.allday.detoxy.presentation.ui.report.components.CoachRecommendationCard
import com.allday.detoxy.presentation.ui.report.components.CoachRecommendationDialog
import com.allday.detoxy.presentation.ui.report.components.DistractionAvoidanceCard
import com.allday.detoxy.presentation.ui.report.components.AllowedAppDwellCard
import java.text.SimpleDateFormat
import java.util.*

/**
 * 리포트 화면
 *
 * Week 2B: Task 2B.3.3 - 최종 통합 및 Analytics 연동
 * Week 2B: Task 2B.3.4 - 코치 추천 카드 활성화
 * Week 2B: Task 2B.3.3 추가 - 포기 지점 분석 & 허용 앱 체류 시간 카드
 */
@Composable
fun ReportScreen(
    onBack: () -> Unit = {},
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCoachDialog by remember { mutableStateOf(false) }

    // TODO: Analytics 이벤트 로깅 (Task 2B.3.3 - 추후 추가)
    // - report_risk_index_calculated
    // - report_recovery_rate_calculated
    // - report_coach_recommendation_shown

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        GlassScaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // 필수: 상단 잘림 해결
            ) {
                // 헤더 (뒤로 가기 버튼 포함)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "디톡시 리포트",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                GlassStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "총 집중 시간",
                                    value = "${uiState.getTotalFocusMinutes()}",
                                    unit = "분",
                                    icon = Icons.Default.PlayArrow,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // 성공 세션
                                GlassStatCard(
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
                                GlassStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "연속 성공",
                                    value = "${uiState.settings.currentStreak}",
                                    unit = "일",
                                    icon = Icons.Default.Favorite,
                                    color = Color(0xFFFF6B35)
                                )

                                // 총 포인트
                                GlassStatCard(
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
                                GlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    alpha = 0.3f
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "오늘의 성공률",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${String.format("%.0f", uiState.getSuccessRate())}%",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { uiState.getSuccessRate() / 100f },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .padding(start = 20.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = when {
                                                uiState.getSuccessRate() >= 80 -> Color(0xFF4CAF50)
                                                uiState.getSuccessRate() >= 50 -> Color(0xFFFFC107)
                                                else -> Color(0xFFFF5252)
                                            },
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // 신규 고급 통계 카드들 (Week 2B) - Task 2B.3.3 최종 통합
                        item {
                            Text(
                                text = "주간 인사이트",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // 1. 위험 지수 카드
                        item {
                            DetoxyRiskCard(riskIndex = uiState.riskIndex)
                        }

                        // 2. 회복률 추세 카드
                        item {
                            RecoveryTrendCard(recoveryTrend = uiState.recoveryTrend)
                        }

                        // 3. 방해요인 Top 3 카드
                        item {
                            DistractionTopCard(distractions = uiState.topDistractions)
                        }

                        // 4. 코치 추천 카드 (Task 2B.3.4 - RiskLevel 분리 후 활성화)
                        item {
                            CoachRecommendationCard(
                                recommendation = uiState.coachRecommendation,
                                onDetailClick = {
                                    showCoachDialog = true
                                    // TODO: Analytics 이벤트 추가 (report_coach_recommendation_shown)
                                }
                            )
                        }

                        // 5. 포기 지점 분석 카드 (Task 2B.3.3 - 추가 구현)
                        item {
                            DistractionAvoidanceCard(giveUpAnalysis = uiState.giveUpAnalysis)
                        }

                        // 6. 허용 앱 체류 시간 카드 (Task 2B.3.3 - 추가 구현, UsageStats 준비)
                        item {
                            AllowedAppDwellCard(
                                isUsageStatsEnabled = false, // TODO: UsageStats 권한 상태 연동
                                onEnableUsageStats = {
                                    // TODO: UsageStats 권한 요청 구현
                                }
                            )
                        }

                        // 세션 리스트 섹션 헤더
                        if (uiState.todaySessions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "오늘의 세션",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // 세션 리스트
                            items(uiState.todaySessions.sortedByDescending { it.startTime }) { session ->
                                GlassSessionCard(session = session)
                            }
                        }
                        
                        // 하단 여백 추가 (네비게이션 바 고려)
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }

    // 코치 추천 다이얼로그 (Task 2B.3.4 - RiskLevel 분리 후 활성화)
    uiState.coachRecommendation?.let { recommendation ->
        if (showCoachDialog) {
            CoachRecommendationDialog(
                recommendation = recommendation,
                onDismiss = { showCoachDialog = false }
            )
        }
    }
}

/**
 * 빈 상태 카드 (크기 최적화)
 */
@Composable
private fun EmptyStateCard() {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        alpha = 0.35f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이콘
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                    )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 제목
            Text(
                text = "첫 디톡시 세션을 시작해보세요!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 설명
            Text(
                text = "디톡시 세션을 시작하면\n다음과 같은 인사이트를 받을 수 있어요:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 기능 목록
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                EmptyStateFeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "위험 지수 분석",
                    color = Color(0xFFF44336)
                )
                EmptyStateFeatureItem(
                    icon = Icons.Default.PlayArrow,
                    text = "회복률 추세 그래프",
                    color = Color(0xFF4CAF50)
                )
                EmptyStateFeatureItem(
                    icon = Icons.Default.Info,
                    text = "방해요인 Top 3",
                    color = Color(0xFFFFA726)
                )
                EmptyStateFeatureItem(
                    icon = Icons.Default.Favorite,
                    text = "맞춤형 코치 추천",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 안내 텍스트
            Text(
                text = "타이머 탭에서 집중 모드를 시작해보세요!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 빈 상태 기능 항목 (크기 최적화)
 */
@Composable
private fun EmptyStateFeatureItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Glass 스타일 통계 카드 (크기 최적화)
 */
@Composable
private fun GlassStatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        alpha = 0.3f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Glass 스타일 세션 카드 (크기 최적화)
 */
@Composable
private fun GlassSessionCard(
    session: FocusSession,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        alpha = if (session.success) 0.3f else 0.2f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (session.success) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFEF5350).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (session.success) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (session.success) "성공" else "실패",
                    tint = if (session.success) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (session.success) "성공 세션" else "실패 세션",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${session.durationMinutes}분 • ${formatTime(session.startTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.success) {
                val points = session.durationMinutes * 10 // 1분당 10포인트
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "+${points}P",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
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
