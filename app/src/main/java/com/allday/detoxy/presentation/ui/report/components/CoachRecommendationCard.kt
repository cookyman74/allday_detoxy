package com.allday.detoxy.presentation.ui.report.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.manager.ActionItem
import com.allday.detoxy.domain.manager.CoachRecommendation
import com.allday.detoxy.domain.manager.RiskLevel

/**
 * 디톡시 코치 추천 카드
 *
 * Week 2B: Task 2B.3.4 - RiskLevel 분리 후 재구현
 */
@Composable
fun CoachRecommendationCard(
    recommendation: CoachRecommendation?,
    onDetailClick: () -> Unit,
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
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "디톡시 코치",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "디톡시 코치 추천",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recommendation != null) {
                // 위험 단계별 배경색
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = getRecommendationBgColor(recommendation.level).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 제목
                        Text(
                            text = recommendation.title.asString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = getRecommendationColor(recommendation.level)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 메시지
                        Text(
                            text = recommendation.message.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 행동 제안 Top 3
                val topActions = recommendation.actionItems.take(3)
                if (topActions.isNotEmpty()) {
                    Text(
                        text = "추천 행동",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topActions.forEach { action ->
                            ActionItemRow(action = action)
                        }
                    }
                }

                // 상세 보기 버튼
                if (recommendation.actionItems.size > 3) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onDetailClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("전체 보기")
                    }
                }
            } else {
                // 데이터 없음
                Text(
                    text = "집중 세션 데이터가 쌓이면\n맞춤형 코치 추천을 받을 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 행동 제안 항목 Row
 */
@Composable
private fun ActionItemRow(
    action: ActionItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = getPriorityColor(action.priority),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 텍스트
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title.asString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (action.description.asString().isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.description.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 위험 단계별 색상
 */
@Composable
private fun getRecommendationColor(level: RiskLevel): Color {
    return when (level) {
        RiskLevel.RECOVERY -> Color(0xFF4CAF50) // Green
        RiskLevel.WARNING -> Color(0xFFFFA726) // Orange
        RiskLevel.HIGH_RISK -> Color(0xFFF44336) // Red
    }
}

/**
 * 위험 단계별 배경색
 */
@Composable
private fun getRecommendationBgColor(level: RiskLevel): Color {
    return when (level) {
        RiskLevel.RECOVERY -> Color(0xFF4CAF50)
        RiskLevel.WARNING -> Color(0xFFFFA726)
        RiskLevel.HIGH_RISK -> Color(0xFFF44336)
    }
}

/**
 * 우선순위별 색상
 */
@Composable
private fun getPriorityColor(priority: Int): Color {
    return when (priority) {
        1 -> Color(0xFFF44336) // Red (High)
        2 -> Color(0xFFFFA726) // Orange (Medium)
        else -> Color(0xFF4CAF50) // Green (Low)
    }
}

