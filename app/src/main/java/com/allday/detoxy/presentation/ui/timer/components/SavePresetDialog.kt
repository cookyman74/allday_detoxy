package com.allday.detoxy.presentation.ui.timer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.ui.component.GlassDialog

/**
 * 프리셋 저장 다이얼로그
 *
 * 사용자가 도넛 그래프로 선택한 시간을 커스텀 프리셋으로 저장합니다.
 *
 * ## 주요 기능
 * - 프리셋 이름 입력
 * - 차단 프리셋 연결 (옵션)
 * - 유효성 검증 (이름 필수)
 *
 * ## 예시
 * ```kotlin
 * SavePresetDialog(
 *     durationMinutes = 35,
 *     onSave = { name, presetType ->
 *         viewModel.savePreset(name, 35, presetType)
 *     },
 *     onDismiss = { showDialog = false }
 * )
 * ```
 *
 * @param durationMinutes 저장할 타이머 시간 (분)
 * @param onSave 저장 콜백 (이름, 차단 프리셋)
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun SavePresetDialog(
    durationMinutes: Int,
    onSave: (name: String, presetType: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    var selectedPresetType by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    
    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "프리셋 저장",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 안내 텍스트
                Text(
                    text = "${durationMinutes}분 타이머를 프리셋으로 저장합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 프리셋 이름 입력
                OutlinedTextField(
                    value = presetName,
                    onValueChange = {
                        presetName = it
                        showError = false
                    },
                    label = { Text("프리셋 이름") },
                    placeholder = { Text("예: 오후 집중") },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("이름을 입력해주세요", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 차단 프리셋 선택
                Text(
                    text = "차단 프리셋 연결 (선택)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                PresetTypeSelector(
                    selectedType = selectedPresetType,
                    onTypeSelected = { selectedPresetType = it }
                )
            }
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (presetName.isBlank()) {
                            showError = true
                        } else {
                            onSave(presetName.trim(), selectedPresetType)
                            onDismiss()
                        }
                    }
                ) {
                    Text("저장")
                }
            }
        }
    }
}

/**
 * 차단 프리셋 선택기
 *
 * @param selectedType 선택된 프리셋 타입
 * @param onTypeSelected 타입 선택 콜백
 */
@Composable
private fun PresetTypeSelector(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit
) {
    val presetTypes = listOf(
        null to "연결 안 함",
        "FULL_BLOCK" to "완전 차단",
        "STANDARD" to "표준 디톡시",
        "RELAXED" to "완화 모드"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presetTypes.forEach { (type, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 프리셋 편집 다이얼로그
 *
 * 기존 커스텀 프리셋의 이름과 차단 프리셋을 수정합니다.
 *
 * @param presetName 현재 프리셋 이름
 * @param presetType 현재 차단 프리셋
 * @param onSave 저장 콜백 (새 이름, 새 차단 프리셋)
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun EditPresetDialog(
    presetName: String,
    presetType: String?,
    onSave: (name: String, presetType: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember { mutableStateOf(presetName) }
    var editedType by remember { mutableStateOf(presetType) }
    var showError by remember { mutableStateOf(false) }
    
    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "프리셋 편집",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 프리셋 이름 입력
                OutlinedTextField(
                    value = editedName,
                    onValueChange = {
                        editedName = it
                        showError = false
                    },
                    label = { Text("프리셋 이름") },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("이름을 입력해주세요", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 차단 프리셋 선택
                Text(
                    text = "차단 프리셋 연결",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                PresetTypeSelector(
                    selectedType = editedType,
                    onTypeSelected = { editedType = it }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (editedName.isBlank()) {
                            showError = true
                        } else {
                            onSave(editedName.trim(), editedType)
                            onDismiss()
                        }
                    }
                ) {
                    Text("저장")
                }
            }
        }
    }
}

/**
 * 프리셋 삭제 확인 다이얼로그
 *
 * @param presetName 삭제할 프리셋 이름
 * @param onConfirm 삭제 확인 콜백
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun DeletePresetDialog(
    presetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "프리셋 삭제",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "'$presetName' 프리셋을 삭제하시겠습니까?\n\n삭제하면 복구할 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("삭제")
                }
            }
        }
    }
}

