package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.local.entity.AutoRunLog
import java.text.SimpleDateFormat
import java.util.*

/**
 * 자동 실행 이력 항목
 *
 * AutoRunLog 데이터를 카드 형태로 표시합니다.
 *
 * ## 표시 정보
 * - 트리거 타입 아이콘 (시간/위치)
 * - 트리거 시간
 * - 결과 및 실패 사유
 * - GPS 정확도 (위치 기반)
 * - 체류 시간 (위치 기반)
 *
 * @param log 자동 실행 로그
 * @param modifier Modifier
 */
@Composable
fun AutoRunHistoryItem(
    log: AutoRunLog,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 왼쪽: 트리거 타입 아이콘
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (log.triggerType) {
                        "TIME" -> Icons.Default.Notifications
                        "LOCATION" -> Icons.Default.LocationOn
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = when (log.triggerType) {
                        "TIME" -> "시간 기반"
                        "LOCATION" -> "위치 기반"
                        else -> "자동 실행"
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 중앙: 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 트리거 시간
                Text(
                    text = formatTriggerTime(log.triggerTime),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 트리거 타입
                Text(
                    text = when (log.triggerType) {
                        "TIME" -> "시간 기반 자동 실행"
                        "LOCATION" -> "위치 기반 자동 실행"
                        else -> "자동 실행"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // GPS 정확도 (위치 기반만)
                if (log.triggerType == "LOCATION" && log.gpsAccuracyMeters != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPS 정확도: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatGpsAccuracy(log.gpsAccuracyMeters),
                            style = MaterialTheme.typography.bodySmall,
                            color = getGpsAccuracyColor(log.gpsAccuracyMeters),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 체류 시간 (위치 기반만)
                if (log.triggerType == "LOCATION" && log.dwellSeconds != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "체류 시간: ${formatDwellTime(log.dwellSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 실패 사유 (실패 시만)
                if (log.result == "FAILED" && log.failureReason != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFailureReason(log.failureReason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 오른쪽: 결과 아이콘
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (log.result) {
                        "STARTED" -> Icons.Default.CheckCircle
                        "SKIPPED" -> Icons.Default.PlayArrow
                        "FAILED" -> Icons.Default.Close
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = when (log.result) {
                        "STARTED" -> "시작됨"
                        "SKIPPED" -> "건너뜀"
                        "FAILED" -> "실패"
                        else -> "결과"
                    },
                    tint = when (log.result) {
                        "STARTED" -> MaterialTheme.colorScheme.primary
                        "SKIPPED" -> MaterialTheme.colorScheme.onSurfaceVariant
                        "FAILED" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 트리거 시간 포맷
 *
 * @param triggerTime 트리거 시간 (timestamp)
 * @return 포맷된 문자열 (예: "오후 2:30")
 */
private fun formatTriggerTime(triggerTime: Long): String {
    val sdf = SimpleDateFormat("a h:mm", Locale.KOREAN)
    return sdf.format(Date(triggerTime))
}

/**
 * GPS 정확도 포맷
 *
 * @param accuracyMeters GPS 정확도 (미터)
 * @return 포맷된 문자열 (예: "높음 (15m)")
 */
private fun formatGpsAccuracy(accuracyMeters: Float): String {
    val level = when {
        accuracyMeters < 20 -> "높음"
        accuracyMeters < 50 -> "보통"
        else -> "낮음"
    }
    return "$level (${accuracyMeters.toInt()}m)"
}

/**
 * GPS 정확도 색상
 *
 * @param accuracyMeters GPS 정확도 (미터)
 * @return 색상
 */
@Composable
private fun getGpsAccuracyColor(accuracyMeters: Float): androidx.compose.ui.graphics.Color {
    return when {
        accuracyMeters < 20 -> MaterialTheme.colorScheme.primary
        accuracyMeters < 50 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}

/**
 * 체류 시간 포맷
 *
 * @param dwellSeconds 체류 시간 (초)
 * @return 포맷된 문자열 (예: "1분 5초")
 */
private fun formatDwellTime(dwellSeconds: Int): String {
    val minutes = dwellSeconds / 60
    val seconds = dwellSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}분 ${seconds}초"
        minutes > 0 -> "${minutes}분"
        else -> "${seconds}초"
    }
}

/**
 * 실패 사유 포맷
 *
 * @param failureReason 실패 사유
 * @return 사용자 친화적 메시지
 */
private fun formatFailureReason(failureReason: String): String {
    return when (failureReason) {
        "PERMISSION_DENIED" -> "권한이 없습니다"
        "TIMER_ALREADY_RUNNING" -> "이미 타이머가 실행 중입니다"
        "LOCATION_DISABLED" -> "위치 서비스가 꺼져 있습니다"
        "USER_PAUSED" -> "사용자가 일시중지했습니다"
        "GEOFENCE_ERROR" -> "지오펜스 오류가 발생했습니다"
        else -> "알 수 없는 오류: $failureReason"
    }
}

