package com.allday.detoxy.presentation.ui.report.components

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.manager.GiveUpPattern
import com.allday.detoxy.domain.manager.GiveUpPointAnalysis

/**
 * 분산 회피율 카드 (포기 지점 분석)
 *
 * 사용자가 집중 세션을 언제 포기하는지 분석:
 * - 초반/중반/후반 포기 비율
 * - 도넛 차트로 시각화
 * - 포기 패턴 분석
 *
 * Week 2B: Task 2B.3.3 - 추가 구현
 */
@Composable
fun DistractionAvoidanceCard(
    giveUpAnalysis: GiveUpPointAnalysis?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "포기 지점 분석",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "포기 지점 분석",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (giveUpAnalysis != null && giveUpAnalysis.totalFailures > 0) {
                // 평균 포기 시점
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "평균 포기 시점",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.0f", giveUpAnalysis.avgGiveUpPercentage)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 도넛 차트
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 도넛 차트
                    GiveUpDonutChart(
                        earlyCount = giveUpAnalysis.earlyGiveUpCount,
                        midCount = giveUpAnalysis.midGiveUpCount,
                        lateCount = giveUpAnalysis.lateGiveUpCount,
                        modifier = Modifier.size(120.dp)
                    )

                    // 범례
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LegendItem(
                            color = Color(0xFFF44336), // Red
                            label = "초반 (0-25%)",
                            value = giveUpAnalysis.earlyGiveUpCount
                        )
                        LegendItem(
                            color = Color(0xFFFFA726), // Orange
                            label = "중반 (25-75%)",
                            value = giveUpAnalysis.midGiveUpCount
                        )
                        LegendItem(
                            color = Color(0xFFFFEB3B), // Yellow
                            label = "후반 (75-100%)",
                            value = giveUpAnalysis.lateGiveUpCount
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 포기 패턴 설명
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = giveUpAnalysis.getGiveUpPattern().toDisplayString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = getGiveUpPatternAdvice(giveUpAnalysis.getGiveUpPattern()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                // 데이터 없음
                Text(
                    text = "실패한 세션이 없어요.\n계속 집중 모드를 유지해보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 포기 지점 도넛 차트
 */
@Composable
private fun GiveUpDonutChart(
    earlyCount: Int,
    midCount: Int,
    lateCount: Int,
    modifier: Modifier = Modifier
) {
    val total = earlyCount + midCount + lateCount
    if (total == 0) return

    val earlyPercentage = earlyCount.toFloat() / total
    val midPercentage = midCount.toFloat() / total
    val latePercentage = lateCount.toFloat() / total

    Canvas(
        modifier = modifier
            .graphicsLayer() // 하드웨어 가속 에러 방지
    ) {
        val canvasSize = size.minDimension
        val strokeWidth = canvasSize / 6
        val radius = (canvasSize - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // 도넛 차트 그리기
        var startAngle = -90f // 12시 방향부터 시작

        // 초반 (빨강)
        if (earlyCount > 0) {
            val sweepAngle = 360f * earlyPercentage
            drawArc(
                color = Color(0xFFF44336),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }

        // 중반 (주황)
        if (midCount > 0) {
            val sweepAngle = 360f * midPercentage
            drawArc(
                color = Color(0xFFFFA726),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }

        // 후반 (노랑)
        if (lateCount > 0) {
            val sweepAngle = 360f * latePercentage
            drawArc(
                color = Color(0xFFFFEB3B),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * 범례 항목
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(16.dp),
            color = color,
            shape = RoundedCornerShape(4.dp)
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${value}회",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 포기 패턴별 조언
 */
private fun getGiveUpPatternAdvice(pattern: GiveUpPattern): String {
    return when (pattern) {
        GiveUpPattern.EARLY -> "시작 후 금방 포기하는 경향이 있어요. 더 짧은 목표 시간으로 시작해보세요."
        GiveUpPattern.MID -> "중간에 집중력이 흐트러지는 경향이 있어요. 5분 휴식을 활용해보세요."
        GiveUpPattern.LATE -> "거의 완주하다가 포기하는 경향이 있어요. 조금만 더 버텨보세요!"
    }
}

