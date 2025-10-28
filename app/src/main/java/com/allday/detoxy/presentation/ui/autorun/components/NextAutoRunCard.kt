package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.manager.NextAutoRunInfo

/**
 * 다음 예정 자동 실행 카드
 *
 * 2.5차 고도화 Week 1, Day 4-6: NextAutoRunCard UI 구현
 *
 * ## 표시 정보
 * - 트리거 타입 아이콘 (🕐 시간 / 📍 위치)
 * - 예정 시간: "오후 2:00 (1시간 30분 후)"
 * - 타이머 시간 + 차단 프리셋
 * - "이 회차 건너뛰기" 버튼
 *
 * @param nextAutoRunInfo 다음 예정 자동 실행 정보 (null이면 예정 없음)
 * @param onSkipClicked 이 회차 건너뛰기 버튼 클릭 핸들러
 * @param modifier Modifier
 * 
 * @see NextAutoRunInfo
 * @see docs/02.5_autosetting_todolist.md §1.2.1
 * @see docs/02_advanced_wireframe_spec.md §5.3
 */
@Composable
fun NextAutoRunCard(
    nextAutoRunInfo: NextAutoRunInfo?,
    onSkipClicked: () -> Unit,
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
                    imageVector = if (nextAutoRunInfo?.triggerType == "TIME") {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.LocationOn
                    },
                    contentDescription = if (nextAutoRunInfo?.triggerType == "TIME") {
                        "시간 기반"
                    } else {
                        "위치 기반"
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "다음 자동 실행",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (nextAutoRunInfo != null) {
                // 예정 시간 표시
                Column {
                    // 라벨 (오후 업무, 회사 등)
                    Text(
                        text = nextAutoRunInfo.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 시간 표시 (1시간 30분 후)
                    Text(
                        text = nextAutoRunInfo.timeUntilTrigger,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 타이머 시간 + 차단 프리셋
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${nextAutoRunInfo.durationMinutes}분",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getPresetDisplayName(nextAutoRunInfo.presetType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 위치 기반일 경우 신뢰도 표시
                    if (nextAutoRunInfo.triggerType == "LOCATION" && nextAutoRunInfo.confidence != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "예측 신뢰도: ${(nextAutoRunInfo.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 건너뛰기 버튼
                    OutlinedButton(
                        onClick = onSkipClicked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "이 회차 건너뛰기",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // 예정된 자동 실행이 없을 때
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "예정된 자동 실행이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "시간 기반 또는 위치 기반 자동 실행을 설정하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 프리셋 타입을 한글 표시명으로 변환
 */
private fun getPresetDisplayName(presetType: String): String {
    return when (presetType) {
        "FULL_BLOCK" -> "완전 차단"
        "STANDARD" -> "표준 디톡시"
        "RELAXED" -> "완화 모드"
        "CUSTOM" -> "커스텀"
        else -> presetType
    }
}

