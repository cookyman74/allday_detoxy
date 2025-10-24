package com.allday.detoxy.presentation.ui.report.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.manager.DetoxyRecoveryTrend
import com.allday.detoxy.domain.manager.RecoveryTrendType
import java.text.SimpleDateFormat
import java.util.*

/**
 * 회복률 추세 카드
 *
 * Week 2B: Task 2B.3.2
 */
@Composable
fun RecoveryTrendCard(
    recoveryTrend: DetoxyRecoveryTrend?,
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
                    imageVector = Icons.Default.Star,
                    contentDescription = "회복률 추세",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "회복률 추세",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recoveryTrend != null && recoveryTrend.dailyRates.isNotEmpty()) {
                // 전체 회복률 및 추세
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.0f", recoveryTrend.overallRate),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 추세 아이콘 및 주간 변화량
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getTrendIcon(recoveryTrend.trend),
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (recoveryTrend.weeklyChange >= 0) "+" else ""}${String.format("%.0f", recoveryTrend.weeklyChange)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = getTrendColor(recoveryTrend.trend)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getTrendDescription(recoveryTrend.trend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 일별 회복률 라인 차트
                DailyRecoveryLineChart(
                    dailyRates = recoveryTrend.dailyRates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 범례
                Text(
                    text = "최근 7일 회복률 추이",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // 데이터 없음
                Text(
                    text = "집중 세션 데이터가 없어요.\n세션을 시작하면 회복률 추세를 확인할 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyRecoveryLineChart(
    dailyRates: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // 날짜 정렬 (오래된 순)
    val sortedEntries = dailyRates.entries.sortedBy { it.key }
    
    if (sortedEntries.isEmpty()) return

    Canvas(
        modifier = modifier
            .graphicsLayer() // 하드웨어 가속 에러 방지
    ) {
        val width = size.width
        val height = size.height
        val padding = 40f

        // 최대값 계산 (dailyRates는 0~100 범위)
        val dataMaxValue = sortedEntries.maxOfOrNull { it.value } ?: 100f
        val maxValue = (dataMaxValue * 1.1f).coerceAtLeast(100f)  // 여유 10% + 최소 100
        val minValue = 0f

        // 데이터 포인트 수
        val pointCount = sortedEntries.size
        if (pointCount < 2) return@Canvas

        // X축 간격
        val xInterval = (width - padding * 2) / (pointCount - 1)

        // Y축 높이
        val chartHeight = height - padding * 2

        // 배경 그리드 (가로선)
        for (i in 0..4) {
            val y = padding + chartHeight * i / 4f
            drawLine(
                color = surfaceVariant,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // 라인 경로 생성
        val path = Path()
        val points = mutableListOf<Offset>()

        sortedEntries.forEachIndexed { index, entry ->
            val x = padding + xInterval * index
            val normalizedValue = (entry.value - minValue) / (maxValue - minValue)
            val y = padding + chartHeight * (1 - normalizedValue)
            
            points.add(Offset(x, y))
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // 라인 그리기
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round
            )
        )

        // 데이터 포인트 그리기
        points.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 5f,
                center = point
            )
        }

        // X축 라벨 (날짜)
        sortedEntries.forEachIndexed { index, entry ->
            val x = padding + xInterval * index
            // 실제 앱에서는 drawContext.canvas.nativeCanvas.drawText() 사용
            // 여기서는 생략 (Canvas에서 텍스트는 별도 Composable로 처리)
        }
    }
}

private fun getTrendIcon(trend: RecoveryTrendType): String {
    return when (trend) {
        RecoveryTrendType.IMPROVING -> "↗️"
        RecoveryTrendType.STABLE -> "→"
        RecoveryTrendType.DECLINING -> "↘️"
    }
}

private fun getTrendColor(trend: RecoveryTrendType): Color {
    return when (trend) {
        RecoveryTrendType.IMPROVING -> Color(0xFF4CAF50)      // Green
        RecoveryTrendType.STABLE -> Color(0xFFFFA726)         // Orange
        RecoveryTrendType.DECLINING -> Color(0xFFEF5350)      // Red
    }
}

private fun getTrendDescription(trend: RecoveryTrendType): String {
    return when (trend) {
        RecoveryTrendType.IMPROVING -> "지난주 대비 회복률이 개선되고 있어요!"
        RecoveryTrendType.STABLE -> "회복률이 안정적으로 유지되고 있어요."
        RecoveryTrendType.DECLINING -> "회복률이 낮아지고 있어요. 조금 더 노력해봐요!"
    }
}

