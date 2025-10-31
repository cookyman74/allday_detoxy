package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.local.entity.ScheduleGroup

/**
 * 시간표 연결 설정 단계 (3차 고도화)
 *
 * 위치에 도착했을 때 특정 시간표를 자동으로 활성화하고,
 * 위치에서 이탈했을 때 시간표를 비활성화하는 설정을 제공합니다.
 *
 * @param enableScheduleLink 시간표 연결 활성화 여부
 * @param onEnableScheduleLinkChange 시간표 연결 활성화 변경 콜백
 * @param selectedScheduleGroupId 선택된 시간표 그룹 ID
 * @param onScheduleGroupIdChange 시간표 그룹 선택 변경 콜백
 * @param activateOnEnter 위치 진입 시 활성화 여부
 * @param onActivateOnEnterChange 진입 시 활성화 변경 콜백
 * @param deactivateOnExit 위치 이탈 시 비활성화 여부
 * @param onDeactivateOnExitChange 이탈 시 비활성화 변경 콜백
 * @param scheduleGroups 사용 가능한 시간표 그룹 목록
 */
@Composable
fun ScheduleLinkSettingsStep(
    enableScheduleLink: Boolean,
    onEnableScheduleLinkChange: (Boolean) -> Unit,
    selectedScheduleGroupId: String?,
    onScheduleGroupIdChange: (String?) -> Unit,
    activateOnEnter: Boolean,
    onActivateOnEnterChange: (Boolean) -> Unit,
    deactivateOnExit: Boolean,
    onDeactivateOnExitChange: (Boolean) -> Unit,
    scheduleGroups: List<ScheduleGroup>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 안내 텍스트
        Text(
            text = "시간표 자동 활성화 설정",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "이 위치에 도착하면 특정 시간표를 자동으로 활성화하고, 이탈 시 비활성화할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        HorizontalDivider()
        
        // 시간표 연결 활성화 토글
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (enableScheduleLink) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "시간표 자동 활성화",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "위치 도착 시 선택한 시간표가 자동으로 활성화됩니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableScheduleLink,
                    onCheckedChange = onEnableScheduleLinkChange
                )
            }
        }
        
        // 시간표 연결이 활성화된 경우에만 표시
        if (enableScheduleLink) {
            // 시간표 선택
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "활성화할 시간표 선택",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (scheduleGroups.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "⚠️ 시간표 없음",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "아직 생성된 시간표가 없습니다.\n먼저 '스케줄 그룹' 화면에서 시간표를 만들어주세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    // 시간표 목록
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scheduleGroups.forEach { group ->
                            ScheduleGroupOption(
                                scheduleGroup = group,
                                selected = group.id == selectedScheduleGroupId,
                                onClick = { onScheduleGroupIdChange(group.id) }
                            )
                        }
                    }
                }
            }
            
            // 진입/이탈 옵션 (시간표가 선택된 경우에만 표시)
            if (selectedScheduleGroupId != null && scheduleGroups.isNotEmpty()) {
                HorizontalDivider()
                
                // 위치 진입 시 활성화
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activateOnEnter) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "위치 도착 시 활성화",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "이 위치에 도착하면 선택한 시간표를 활성화합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = activateOnEnter,
                            onCheckedChange = onActivateOnEnterChange
                        )
                    }
                }
                
                // 위치 이탈 시 비활성화
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (deactivateOnExit) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "위치 이탈 시 비활성화",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "이 위치에서 나가면 시간표를 비활성화합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = deactivateOnExit,
                            onCheckedChange = onDeactivateOnExitChange
                        )
                    }
                }
                
                // 안내 메시지
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "한 번에 하나의 시간표만 활성화할 수 있습니다. 새로운 시간표가 활성화되면 기존 시간표는 자동으로 비활성화됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        } else {
            // 시간표 연결 비활성화 시 안내
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ 시간표 연결 비활성화",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "시간표 자동 활성화를 사용하지 않습니다.\n위 스위치를 켜면 위치 도착 시 특정 시간표를 자동으로 활성화할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 시간표 그룹 선택 옵션
 */
@Composable
private fun ScheduleGroupOption(
    scheduleGroup: ScheduleGroup,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scheduleGroup.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (scheduleGroup.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scheduleGroup.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (scheduleGroup.isActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ 현재 활성화 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = null // Card의 selectable에서 처리
            )
        }
    }
}

