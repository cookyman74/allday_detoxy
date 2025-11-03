package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.clickable
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
import com.allday.detoxy.data.local.entity.ScheduleGroup

/**
 * ScheduleGroup 카드 컴포넌트 (3.5차 고도화 - 간소화 버전)
 *
 * ScheduleGroup 정보를 표시하고 편집/삭제/활성화 기능을 제공합니다.
 *
 * ## UI 구조
 * - 헤더: 아이콘 + 이름 + 활성화 토글
 * - 설명
 * - 위치 정보 (클릭 시 상세/수정)
 * - 시간 정보 (클릭 시 상세/수정)
 * - 하단: 수정/삭제 버튼
 *
 * @param group 표시할 ScheduleGroup
 * @param isActive 활성화 여부
 * @param timeBasedAutoRuns 연결된 시간대 목록
 * @param linkedLocations 연결된 위치 목록
 * @param onActivate 활성화/비활성화 콜백
 * @param onEdit 편집 콜백 (타이틀/설명만)
 * @param onDelete 삭제 콜백
 * @param onLocationClick 위치 정보 클릭 콜백
 * @param onTimeClick 시간 정보 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun ScheduleGroupCard(
    group: ScheduleGroup,
    isActive: Boolean,
    timeBasedAutoRuns: List<com.allday.detoxy.data.local.entity.TimeBasedAutoRun>,
    linkedLocations: List<com.allday.detoxy.data.local.entity.LocationBasedAutoRun> = emptyList(),
    linkedLocationCount: Int = linkedLocations.size,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLocationClick: () -> Unit = {},
    onTimeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더: 아이콘 + 이름 + 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 아이콘
                    Icon(
                        imageVector = getIconForType(group.iconType),
                        contentDescription = null,
                        tint = Color(android.graphics.Color.parseColor(group.colorHex)),
                        modifier = Modifier.size(32.dp)
                    )
                    
                    // 이름
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 활성화 토글
                Switch(
                    checked = isActive,
                    onCheckedChange = { onActivate() }
                )
            }
            
            // 설명
            if (group.description != null) {
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider()
            
            // 위치 정보 (클릭 가능)
            if (linkedLocations.isNotEmpty()) {
                linkedLocations.forEach { location ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLocationClick),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "위치",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "위치",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "상세보기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Text(
                                text = location.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${location.address} (${location.radiusMeters}m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // 어디서나 적용
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "위치 없음",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "어디서나 적용 (위치 없음)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider()
            
            // 시간 정보 (클릭 가능)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = timeBasedAutoRuns.isNotEmpty(), onClick = onTimeClick),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "시간",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "시간",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.weight(1f))
                        if (timeBasedAutoRuns.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "상세보기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    if (timeBasedAutoRuns.isEmpty()) {
                        Text(
                            text = "설정된 시간대 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        timeBasedAutoRuns.forEach { autoRun ->
                            Text(
                                text = "• ${formatTime(autoRun.hour, autoRun.minute)} - ${autoRun.durationMinutes}분",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // 하단: 수정/삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "수정",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("수정")
                }
                
                Spacer(Modifier.width(8.dp))
                
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("삭제")
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
