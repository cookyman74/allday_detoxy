package com.allday.detoxy.presentation.ui.report.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.allday.detoxy.R
import com.allday.detoxy.domain.manager.ActionItem
import com.allday.detoxy.domain.manager.CoachRecommendation
import com.allday.detoxy.domain.manager.RiskLevel

/**
 * 디톡시 코치 추천 상세 다이얼로그
 *
 * Week 2B: Task 2B.3.4 - RiskLevel 분리 후 재구현
 */
@Composable
fun CoachRecommendationDialog(
    recommendation: CoachRecommendation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "디톡시 코치 추천",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제목 및 메시지
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = getRecommendationBgColor(recommendation.level).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 위험 단계 배지
                        Surface(
                            color = getRecommendationColor(recommendation.level).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = recommendation.level.toUiText().asString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = getRecommendationColor(recommendation.level),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 제목
                        Text(
                            text = recommendation.title.asString(),
                            style = MaterialTheme.typography.titleMedium,
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

                Spacer(modifier = Modifier.height(20.dp))

                // 행동 제안 목록
                Text(
                    text = "추천 행동 목록",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 스크롤 가능한 행동 제안 목록
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommendation.actionItems) { action ->
                        DetailedActionItem(action = action)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 닫기 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("확인", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * 상세 행동 제안 항목
 */
@Composable
private fun DetailedActionItem(
    action: ActionItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 우선순위 아이콘
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = getPriorityColor(action.priority),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 텍스트
            Column(modifier = Modifier.weight(1f)) {
                // 우선순위 뱃지
                Surface(
                    color = getPriorityColor(action.priority).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(getPriorityTextResId(action.priority)),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = getPriorityColor(action.priority),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 제목
                Text(
                    text = action.title.asString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 설명
                Text(
                    text = action.description.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
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

/**
 * 우선순위 텍스트 리소스 ID
 */
private fun getPriorityTextResId(priority: Int): Int {
    return when (priority) {
        1 -> R.string.priority_high
        2 -> R.string.priority_medium
        else -> R.string.priority_low
    }
}

