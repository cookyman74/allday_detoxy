package com.allday.detoxy.presentation.ui.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.manager.DistractionItem

/**
 * 방해요인 Top 3 카드
 *
 * Week 2B: Task 2B.3.2
 */
@Composable
fun DistractionTopCard(
    distractions: List<DistractionItem>,
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
                    contentDescription = "방해요인",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "방해요인 Top 3",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "가장 많이 차단된 앱 카테고리",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (distractions.isNotEmpty()) {
                // Top 3 표시
                val top3 = distractions.take(3)
                val totalCount = distractions.sumOf { it.count }

                top3.forEachIndexed { index, item ->
                    DistractionItem(
                        rank = index + 1,
                        name = item.name,
                        count = item.count,
                        percentage = if (totalCount > 0) (item.count.toFloat() / totalCount * 100) else 0f
                    )
                    
                    if (index < top3.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 총 차단 횟수
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "총 차단 횟수",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${totalCount}회",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // 데이터 없음
                Text(
                    text = "아직 차단된 앱이 없어요.\n집중 세션을 시작해보세요!",
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
private fun DistractionItem(
    rank: Int,
    name: String,
    count: Int,
    percentage: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 뱃지
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(getRankColor(rank)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 카테고리 및 횟수
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCategoryDisplayName(name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${count}회 차단",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 비율
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 프로그레스 바
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = getRankColor(rank),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun getRankColor(rank: Int): Color {
    return when (rank) {
        1 -> Color(0xFFEF5350)     // Red
        2 -> Color(0xFFFFA726)     // Orange
        3 -> Color(0xFFFFCA28)     // Yellow
        else -> Color.Gray
    }
}

private fun getCategoryDisplayName(category: String): String {
    return when (category.uppercase()) {
        "SNS" -> "SNS (소셜 미디어)"
        "MESSENGER" -> "메신저"
        "WEB" -> "웹 브라우저"
        "VIDEO" -> "동영상"
        "OTHER" -> "기타"
        else -> category
    }
}

