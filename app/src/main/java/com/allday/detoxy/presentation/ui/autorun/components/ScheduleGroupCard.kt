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
import com.allday.detoxy.data.local.entity.ScheduleGroup

/**
 * ScheduleGroup 카드 컴포넌트
 *
 * ScheduleGroup 정보를 표시하고 편집/삭제/활성화 기능을 제공합니다.
 *
 * @param scheduleGroup 표시할 ScheduleGroup
 * @param linkedTimeCount 연결된 시간 기반 자동 실행 개수
 * @param linkedLocationCount 연결된 위치 기반 자동 실행 개수
 * @param onToggle 활성화/비활성화 콜백
 * @param onEdit 편집 콜백
 * @param onDelete 삭제 콜백
 * @param onClick 카드 클릭 콜백 (상세 정보 보기)
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGroupCard(
    scheduleGroup: ScheduleGroup,
    linkedTimeCount: Int = 0,
    linkedLocationCount: Int = 0,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (scheduleGroup.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 헤더: 이름, 활성화 스위치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = scheduleGroup.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!scheduleGroup.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scheduleGroup.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Switch(
                    checked = scheduleGroup.isActive,
                    onCheckedChange = onToggle
                )
            }
            
            // 연결된 설정 개수
            if (linkedTimeCount > 0 || linkedLocationCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (linkedTimeCount > 0) {
                        AssistChip(
                            onClick = { /* 클릭 시 상세 보기 */ },
                            label = { Text("시간표 ${linkedTimeCount}개") }
                        )
                    }
                    if (linkedLocationCount > 0) {
                        AssistChip(
                            onClick = { /* 클릭 시 상세 보기 */ },
                            label = { Text("위치 ${linkedLocationCount}개") }
                        )
                    }
                }
            }
            
            // 액션 버튼들
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "편집",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("편집")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("삭제")
                }
            }
        }
    }
}

