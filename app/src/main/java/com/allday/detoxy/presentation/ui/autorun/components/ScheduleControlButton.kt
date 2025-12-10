package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.model.PauseDuration
import com.allday.detoxy.domain.model.ScheduleGroupControlState

/**
 * 스케줄 그룹 통합 제어 버튼 (v8+)
 *
 * 활성/일시중지/비활성 상태를 표시하고 제어하는 버튼입니다.
 *
 * ## 상호작용
 * - **탭**: 상태 토글 (ACTIVE ↔ INACTIVE, PAUSED → ACTIVE)
 * - **롱프레스 또는 ▾ 탭**: 드롭다운 메뉴 표시
 *
 * ## 드롭다운 메뉴
 * - 활성화
 * - 1시간/2시간/오늘하루/내일까지 일시중지
 * - 비활성화
 *
 * @param controlState 현재 제어 상태
 * @param pauseUntil 일시중지 해제 시각 (PAUSED 상태일 때만 사용)
 * @param onStateChange 상태 변경 콜백
 * @param onPause 일시중지 콜백 (기간 전달)
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleControlButton(
    controlState: ScheduleGroupControlState,
    pauseUntil: Long? = null,
    onStateChange: (ScheduleGroupControlState) -> Unit,
    onPause: (PauseDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // 상태별 색상 및 아이콘 정의
    val appearance = getStateAppearance(controlState)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(appearance.backgroundColor)
                .combinedClickable(
                    onClick = {
                        // 탭: 상태 토글
                        when (controlState) {
                            ScheduleGroupControlState.ACTIVE -> onStateChange(ScheduleGroupControlState.INACTIVE)
                            ScheduleGroupControlState.INACTIVE -> onStateChange(ScheduleGroupControlState.ACTIVE)
                            ScheduleGroupControlState.PAUSED -> onStateChange(ScheduleGroupControlState.ACTIVE)
                        }
                    },
                    onLongClick = {
                        // 롱프레스: 드롭다운 메뉴
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDropdown = true
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 아이콘
            Icon(
                imageVector = appearance.icon,
                contentDescription = appearance.label,
                tint = appearance.contentColor,
                modifier = Modifier.size(20.dp)
            )

            // 레이블 (PAUSED일 때 남은 시간 표시)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appearance.label,
                    color = appearance.contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // 일시중지 남은 시간 표시
                if (controlState == ScheduleGroupControlState.PAUSED && pauseUntil != null) {
                    Text(
                        text = formatRemainingTime(pauseUntil),
                        color = appearance.contentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 드롭다운 표시 아이콘
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "메뉴",
                tint = appearance.contentColor,
                modifier = Modifier
                    .size(16.dp)
                    .combinedClickable(
                        onClick = { showDropdown = true },
                        onLongClick = null
                    )
            )
        }

        // 드롭다운 메뉴
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            // 활성화 옵션
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (controlState == ScheduleGroupControlState.ACTIVE) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "활성화",
                            fontWeight = if (controlState == ScheduleGroupControlState.ACTIVE) 
                                FontWeight.Bold 
                            else 
                                FontWeight.Normal
                        )
                    }
                },
                onClick = {
                    onStateChange(ScheduleGroupControlState.ACTIVE)
                    showDropdown = false
                }
            )

            HorizontalDivider()

            // 일시중지 옵션들
            Text(
                text = "일시중지",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            PauseDuration.entries.forEach { duration ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(duration.displayName)
                        }
                    },
                    onClick = {
                        onPause(duration)
                        showDropdown = false
                    }
                )
            }

            HorizontalDivider()

            // 비활성화 옵션
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = if (controlState == ScheduleGroupControlState.INACTIVE) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "비활성화",
                            fontWeight = if (controlState == ScheduleGroupControlState.INACTIVE) 
                                FontWeight.Bold 
                            else 
                                FontWeight.Normal,
                            color = if (controlState == ScheduleGroupControlState.INACTIVE) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                onClick = {
                    onStateChange(ScheduleGroupControlState.INACTIVE)
                    showDropdown = false
                }
            )
        }
    }
}

/**
 * 상태별 외관 정의
 *
 * @param state 현재 제어 상태
 * @return StateAppearance (배경색, 콘텐츠색, 아이콘, 레이블)
 */
@Composable
private fun getStateAppearance(state: ScheduleGroupControlState): StateAppearance {
    return when (state) {
        ScheduleGroupControlState.ACTIVE -> StateAppearance(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.Check,
            label = "활성"
        )
        ScheduleGroupControlState.PAUSED -> StateAppearance(
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Default.DateRange,
            label = "일시중지"
        )
        ScheduleGroupControlState.INACTIVE -> StateAppearance(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.Close,
            label = "비활성"
        )
    }
}

/**
 * 상태별 외관 데이터 클래스
 */
private data class StateAppearance(
    val backgroundColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val label: String
)

/**
 * 남은 시간 포맷팅
 *
 * @param pauseUntil 일시중지 해제 시각 (timestamp)
 * @return 포맷팅된 남은 시간 문자열
 */
fun formatRemainingTime(pauseUntil: Long): String {
    val now = System.currentTimeMillis()
    val diff = pauseUntil - now

    if (diff <= 0) {
        return "곧 해제"
    }

    val totalMinutes = diff / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        minutes > 0 -> "${minutes}분"
        else -> "1분 미만"
    }
}

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun ScheduleControlButtonActivePreview() {
    MaterialTheme {
        ScheduleControlButton(
            controlState = ScheduleGroupControlState.ACTIVE,
            onStateChange = {},
            onPause = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleControlButtonPausedPreview() {
    MaterialTheme {
        ScheduleControlButton(
            controlState = ScheduleGroupControlState.PAUSED,
            pauseUntil = System.currentTimeMillis() + 3600_000L, // 1시간 후
            onStateChange = {},
            onPause = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleControlButtonInactivePreview() {
    MaterialTheme {
        ScheduleControlButton(
            controlState = ScheduleGroupControlState.INACTIVE,
            onStateChange = {},
            onPause = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleControlButtonAllStatesPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScheduleControlButton(
                controlState = ScheduleGroupControlState.ACTIVE,
                onStateChange = {},
                onPause = {}
            )
            ScheduleControlButton(
                controlState = ScheduleGroupControlState.PAUSED,
                pauseUntil = System.currentTimeMillis() + 7200_000L, // 2시간 후
                onStateChange = {},
                onPause = {}
            )
            ScheduleControlButton(
                controlState = ScheduleGroupControlState.INACTIVE,
                onStateChange = {},
                onPause = {}
            )
        }
    }
}

