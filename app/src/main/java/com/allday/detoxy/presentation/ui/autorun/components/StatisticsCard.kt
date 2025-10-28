package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.domain.manager.AutoRunStatistics
import com.allday.detoxy.domain.manager.DailyStats

/**
 * 자동 실행 통계 카드
 *
 * 이번 주 자동 실행 통계를 표시합니다.
 *
 * ## 주요 구성요소
 * - 이번 주 자동 실행 횟수 (시간/위치 분리)
 * - 성공률 원형 그래프
 * - 자동 실행으로 얻은 총 집중 시간
 * - 주간 트렌드 미니 차트
 *
 * @param statistics 자동 실행 통계 데이터
 * @param modifier Modifier
 */
@Composable
fun StatisticsCard(
    statistics: AutoRunStatistics?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                .padding(20.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "통계",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "이번 주 자동 실행 통계",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (statistics != null) {
                // 자동 실행 횟수
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 시간 기반
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${statistics.weeklyTimeBasedCount}회",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "시간 기반",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 위치 기반
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${statistics.weeklyLocationBasedCount}회",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "위치 기반",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 성공률 원형 그래프
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 원형 그래프
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularSuccessRate(
                            successRate = statistics.successRate,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${(statistics.successRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 성공률 및 집중 시간 정보
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "성공률",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "총 집중 시간",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatFocusTime(statistics.totalFocusTimeMinutes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 주간 트렌드 미니 차트
                Column {
                    Text(
                        text = "주간 트렌드",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WeeklyTrendChart(
                        weeklyTrend = statistics.weeklyTrend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            } else {
                // Empty State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "이번 주 자동 실행 데이터가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "자동 실행을 설정하고 사용해보세요!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 성공률 원형 그래프
 *
 * @param successRate 성공률 (0.0 ~ 1.0)
 * @param modifier Modifier
 */
@Composable
private fun CircularSuccessRate(
    successRate: Float,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 8.dp.toPx()
        val diameter = size.minDimension
        val radius = diameter / 2f

        // 배경 원
        drawCircle(
            color = backgroundColor,
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // 성공률 원호
        val sweepAngle = 360f * successRate
        drawArc(
            color = color,
            startAngle = -90f, // 12시 방향부터 시작
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset.Zero,
            size = Size(diameter, diameter)
        )
    }
}

/**
 * 주간 트렌드 미니 차트
 *
 * 7일간의 자동 실행 횟수를 막대 그래프로 표시합니다.
 *
 * @param weeklyTrend 주간 트렌드 데이터
 * @param modifier Modifier
 */
@Composable
private fun WeeklyTrendChart(
    weeklyTrend: List<DailyStats>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        if (weeklyTrend.isEmpty()) return@Canvas

        val chartWidth = size.width
        val chartHeight = size.height
        val barWidth = chartWidth / (weeklyTrend.size * 2) // 간격 포함
        val maxCount = weeklyTrend.maxOfOrNull { it.totalCount } ?: 1

        weeklyTrend.forEachIndexed { index, dailyStats ->
            val x = index * (barWidth * 2) + barWidth / 2
            val barHeight = if (maxCount > 0) {
                (dailyStats.totalCount.toFloat() / maxCount) * chartHeight
            } else {
                0f
            }

            // 배경 막대 (회색)
            drawRect(
                color = surfaceVariant,
                topLeft = Offset(x, chartHeight - barHeight),
                size = Size(barWidth, barHeight)
            )

            // 시간 기반 막대 (파란색)
            val timeBasedHeight = if (maxCount > 0) {
                (dailyStats.timeBasedCount.toFloat() / maxCount) * chartHeight
            } else {
                0f
            }
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, chartHeight - timeBasedHeight),
                size = Size(barWidth * 0.5f, timeBasedHeight)
            )

            // 위치 기반 막대 (보라색)
            val locationBasedHeight = if (maxCount > 0) {
                (dailyStats.locationBasedCount.toFloat() / maxCount) * chartHeight
            } else {
                0f
            }
            drawRect(
                color = secondaryColor,
                topLeft = Offset(x + barWidth * 0.5f, chartHeight - locationBasedHeight),
                size = Size(barWidth * 0.5f, locationBasedHeight)
            )
        }
    }
}

/**
 * 집중 시간 포맷 (분 → 시간 분)
 *
 * @param minutes 집중 시간 (분)
 * @return 포맷된 문자열 (예: "7시간 30분")
 */
private fun formatFocusTime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}시간 ${remainingMinutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${remainingMinutes}분"
    }
}

