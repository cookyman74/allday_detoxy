package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun

/**
 * 위치 기반 자동 실행 카드
 *
 * 등록된 위치 기반 자동 실행 정보를 표시하는 카드입니다.
 *
 * @param location 위치 기반 자동 실행
 * @param successRate 성공률 (0.0~1.0, null이면 표시 안 함)
 * @param gpsAccuracy GPS 정확도 ("높음", "중간", "낮음", null이면 표시 안 함)
 * @param onToggle 활성화/비활성화 토글 콜백
 * @param onEdit 편집 버튼 클릭 콜백
 * @param onDelete 삭제 버튼 클릭 콜백
 */
@Composable
fun LocationBasedAutoRunCard(
    location: LocationBasedAutoRun,
    successRate: Float? = null,
    gpsAccuracy: String? = null,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (location.isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 헤더: 라벨 + 활성화 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 위치 라벨 + 아이콘
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getLocationIcon(location.label),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = location.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 활성화 토글
                Switch(
                    checked = location.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            // 주소 (축약)
            if (!location.address.isNullOrBlank()) {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 반경 + 타이머 시간
            Text(
                text = "반경 ${location.radiusMeters}m • ${location.durationMinutes}분",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 성공률 표시 (있을 경우)
            if (successRate != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "성공률: ${(successRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getSuccessRateIcon(successRate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // GPS 정확도 표시 (있을 경우)
            if (gpsAccuracy != null) {
                Text(
                    text = "GPS 정확도: $gpsAccuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 액션 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "편집",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("편집")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("삭제")
                }
            }
        }
    }
}

/**
 * 위치 라벨에 따라 아이콘 반환
 */
private fun getLocationIcon(label: String): String {
    return when {
        label.contains("회사", ignoreCase = true) -> "🏢"
        label.contains("학교", ignoreCase = true) -> "🏫"
        label.contains("도서관", ignoreCase = true) -> "📚"
        label.contains("카페", ignoreCase = true) -> "☕"
        label.contains("집", ignoreCase = true) -> "🏠"
        else -> "📍"
    }
}

/**
 * 성공률에 따라 아이콘 반환
 */
private fun getSuccessRateIcon(successRate: Float): String {
    return when {
        successRate >= 0.7f -> "🟢" // 70% 이상
        successRate >= 0.5f -> "🟡" // 50~70%
        else -> "🔴" // 50% 미만
    }
}

