package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.local.entity.ScheduleGroup

/**
 * ScheduleGroup 추가/편집 다이얼로그
 *
 * ScheduleGroup 생성 또는 수정을 위한 다이얼로그입니다.
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 확인 콜백 (name, description)
 * @param existingGroup 편집할 기존 그룹 (null이면 신규 생성)
 * @param modifier Modifier
 */
@Composable
fun AddScheduleGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
    existingGroup: ScheduleGroup? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(existingGroup?.name ?: "") }
    var description by remember { mutableStateOf(existingGroup?.description ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingGroup != null) "스케줄 그룹 편집" else "스케줄 그룹 추가"
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 안내 텍스트
                Text(
                    text = "여러 시간표를 그룹으로 묶어 위치에 따라 자동으로 활성화/비활성화할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 그룹 이름
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("그룹 이름 *") },
                    placeholder = { Text("예: 업무 시간표, 공부 루틴") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                
                // 그룹 설명 (옵션)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명 (선택)") },
                    placeholder = { Text("예: 회사에 있을 때 사용할 시간표") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                // 안내 메시지
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "💡 그룹을 만든 후, 시간 기반 자동 실행 설정에서 이 그룹에 연결할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "그룹 이름을 입력해주세요"
                    } else {
                        onConfirm(name.trim(), description.trim().takeIf { it.isNotEmpty() })
                        onDismiss()
                    }
                }
            ) {
                Text(if (existingGroup != null) "수정" else "추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        modifier = modifier
    )
}

