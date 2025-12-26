package com.allday.detoxy.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🆕 v9: 오버레이 프라이버시 설정 섹션
 * 
 * 차단 오버레이에서 목표/할일 표시 방식을 설정합니다.
 */
@Composable
fun OverlayPrivacySettingsSection(
    showTodoOnOverlay: Boolean,
    showDetailedTodo: Boolean,
    hideGoal: Boolean,
    onShowTodoChange: (Boolean) -> Unit,
    onShowDetailedChange: (Boolean) -> Unit,
    onHideGoalChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 섹션 헤더
            Text(
                text = "🔒 오버레이 프라이버시",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 옵션 1: 오버레이에 목표 표시
            SwitchPreference(
                title = "오버레이에 목표 표시",
                description = "차단 화면에 집중 목표를 표시합니다",
                checked = showTodoOnOverlay,
                onCheckedChange = onShowTodoChange
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 옵션 2: 세부 할일 표시 (목표 표시가 켜져 있을 때만 활성화)
            SwitchPreference(
                title = "세부 할일 표시",
                description = "개별 할일 항목까지 표시합니다",
                checked = showDetailedTodo,
                enabled = showTodoOnOverlay && !hideGoal,
                onCheckedChange = onShowDetailedChange
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 옵션 3: 목표 숨기기 (이모지만 표시)
            SwitchPreference(
                title = "목표 숨기기 (이모지만)",
                description = "민감한 목표를 숨기고 🎯 이모지만 표시합니다",
                checked = hideGoal,
                enabled = showTodoOnOverlay,
                onCheckedChange = onHideGoalChange
            )
        }
    }
}

/**
 * 스위치 설정 항목
 */
@Composable
private fun SwitchPreference(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
