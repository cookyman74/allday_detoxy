package com.allday.detoxy.presentation.ui.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.manager.DetoxyRiskIndex
import com.allday.detoxy.domain.manager.RiskLevel

/**
 * 디톡시 위험 지수 카드
 *
 * Week 2B: Task 2B.3.2
 */
@Composable
fun DetoxyRiskCard(
    riskIndex: DetoxyRiskIndex?,
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = "위험 지수",
                    tint = if (riskIndex != null) getRiskColor(riskIndex.level) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "디톡시 위험 지수",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (riskIndex != null) {
                // 위험 단계 및 스코어
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${riskIndex.score.toInt()}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = getRiskColor(riskIndex.level)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 위험 단계 라벨
                    Surface(
                        color = getRiskColor(riskIndex.level).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = getRiskLevelText(riskIndex.level),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = getRiskColor(riskIndex.level),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 프로그레스 바
                LinearProgressIndicator(
                    progress = { (riskIndex.score / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = getRiskColor(riskIndex.level),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 설명 텍스트
                Text(
                    text = getRiskDescription(riskIndex.level),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // 세부 지표
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RiskFactorItem(
                        label = "실패율",
                        value = "${(riskIndex.failureRate * 100).toInt()}%"
                    )
                    RiskFactorItem(
                        label = "포기 시점 점수",
                        value = "${riskIndex.avgGiveUpTime.toInt()}"
                    )
                    RiskFactorItem(
                        label = "차단 빈도 점수",
                        value = "${riskIndex.interruptionFrequency.toInt()}"
                    )
                }
            } else {
                // 데이터 없음
                Text(
                    text = "집중 세션 데이터가 없어요.\n세션을 시작하면 디톡시 위험 지수를 확인할 수 있어요.",
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
private fun RiskFactorItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getRiskColor(riskLevel: RiskLevel): Color {
    return when (riskLevel) {
        RiskLevel.RECOVERY -> Color(0xFF4CAF50)      // Green
        RiskLevel.WARNING -> Color(0xFFFFA726)       // Orange
        RiskLevel.HIGH_RISK -> Color(0xFFEF5350)     // Red
    }
}

private fun getRiskLevelText(riskLevel: RiskLevel): String {
    return when (riskLevel) {
        RiskLevel.RECOVERY -> "회복 중"
        RiskLevel.WARNING -> "주의 필요"
        RiskLevel.HIGH_RISK -> "고위험"
    }
}

private fun getRiskDescription(riskLevel: RiskLevel): String {
    return when (riskLevel) {
        RiskLevel.RECOVERY -> "훌륭해요! 디지털 디톡시가 잘 진행되고 있어요. 현재 패턴을 유지하세요."
        RiskLevel.WARNING -> "조금 더 주의가 필요해요. 집중 세션 성공률을 높여보세요."
        RiskLevel.HIGH_RISK -> "디지털 중독 위험이 높아요. 집중 시간을 늘리고 포기를 줄여보세요."
    }
}

