package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel

/**
 * ScheduleGroup 카드 컴포넌트 (3차 고도화)
 *
 * ScheduleGroup 정보를 표시하고 편집/삭제/활성화 기능을 제공합니다.
 *
 * ## 3차 고도화 추가 기능
 * - 연결된 시간대 목록 표시 (확장/축소 가능)
 * - 활성화/비활성화 버튼 (ScheduleGroupManager 사용)
 * - 아이콘 및 색상 표시
 *
 * @param group 표시할 ScheduleGroup
 * @param isActive 활성화 여부
 * @param onActivate 활성화/비활성화 콜백
 * @param onEdit 편집 콜백
 * @param onDelete 삭제 콜백
 * @param viewModel ScheduleGroupViewModel
 * @param modifier Modifier
 */
@Composable
fun ScheduleGroupCard(
    group: ScheduleGroup,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: ScheduleGroupViewModel,
    modifier: Modifier = Modifier
) {
    // 연결된 시간대 목록 조회
    val timeBasedAutoRuns by viewModel.getTimeBasedAutoRuns(group.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    // 확장/축소 상태
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 헤더: 아이콘, 이름, 활성화 배지
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 아이콘
                    Icon(
                        imageVector = getIconForType(group.iconType),
                        contentDescription = null,
                        tint = Color(android.graphics.Color.parseColor(group.colorHex))
                    )
                    
                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (group.description != null) {
                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // 활성화 배지
                if (isActive) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("활성화", modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 시간대 요약
            if (timeBasedAutoRuns.isNotEmpty()) {
                Text(
                    text = "${timeBasedAutoRuns.size}개 시간대",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 시간대 리스트 (확장 시)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        timeBasedAutoRuns.forEach { autoRun ->
                            Text(
                                text = "• ${formatTime(autoRun.hour, autoRun.minute)} - ${autoRun.durationMinutes}분",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "시간대 없음",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 액션 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 활성화/비활성화 버튼
                if (isActive) {
                    OutlinedButton(onClick = onActivate) {
                        Text("비활성화")
                    }
                } else {
                    Button(onClick = onActivate) {
                        Text("활성화")
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 확장/축소 버튼 (시간대가 있을 때만)
                    if (timeBasedAutoRuns.isNotEmpty()) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "접기" else "펼치기"
                            )
                        }
                    }
                    
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "편집")
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "삭제")
                    }
                }
            }
        }
    }
}

/**
 * 시간 포맷 유틸 함수
 *
 * @param hour 시간 (0-23)
 * @param minute 분 (0-59)
 * @return "오전 9:00" 형식의 문자열
 */
private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "오전" else "오후"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%s %d:%02d", period, displayHour, minute)
}

/**
 * 아이콘 타입에 따라 아이콘 반환
 *
 * @param iconType 아이콘 타입 (WORK, STUDY, GYM, HOME, CUSTOM)
 * @return ImageVector
 */
private fun getIconForType(iconType: String): ImageVector {
    return when (iconType) {
        "WORK" -> Icons.Default.Star
        "STUDY" -> Icons.Default.Star
        "GYM" -> Icons.Default.Star
        "HOME" -> Icons.Default.Home
        else -> Icons.Default.Star
    }
}

