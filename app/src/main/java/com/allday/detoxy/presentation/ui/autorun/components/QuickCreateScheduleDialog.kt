package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot

/**
 * 빠른 시간표 생성 다이얼로그 (3.5차 고도화 Phase 1)
 *
 * ## Phase 1 개선 사항
 * - 커스텀 시간대 직접 추가 기능 구현 🎯
 * - 생성 모드 선택 (템플릿 vs 커스텀)
 * - TimeSlotInputDialog 통합
 *
 * ## 특징
 * - Option 1: 템플릿으로 시작 (Phase 2 구현 예정)
 * - Option 2: 시간대 직접 추가 (Phase 1 구현 완료) ✅
 * - 시간대 추가/수정/삭제 지원
 * - 요일별 활성화 지원
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 확인 콜백 (이름, 모드, 데이터 전달)
 * @param locationLabel 위치 라벨 (기본값 제안용)
 * @param initialMode 초기 생성 모드 (기본값: CUSTOM)
 */
@Composable
fun QuickCreateScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mode: CreationMode, data: Any) -> Unit,
    locationLabel: String = "",
    initialMode: CreationMode = CreationMode.CUSTOM
) {
    var name by remember { 
        mutableStateOf(
            if (locationLabel.isNotBlank()) {
                "$locationLabel 시간표"
            } else {
                ""
            }
        ) 
    }
    var mode by remember { mutableStateOf(initialMode) }  // 초기 모드 적용
    
    // 커스텀 모드 상태
    var timeSlots by remember { mutableStateOf(listOf<TimeSlot>()) }
    var showTimeSlotInput by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimeSlot?>(null) }
    
    // 템플릿 모드 상태 (Phase 2)
    var selectedTemplate by remember { mutableStateOf<ScheduleTemplate?>(null) }
    var showTemplateSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "시간표 만들기",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 안내 카드
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "💡 시간대를 직접 추가하거나 템플릿을 선택하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                // 시간표 이름
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("시간표 이름 *") },
                    placeholder = { Text("예: 업무 시간표, 공부 루틴") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                HorizontalDivider()
                
                // 시작 방법 선택
                Text(
                    text = "시작 방법 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                RadioButtonRow(
                    text = "📋 템플릿으로 시작",
                    description = "기본 시간대가 포함된 템플릿 사용",
                    selected = mode == CreationMode.TEMPLATE,
                    onClick = { mode = CreationMode.TEMPLATE },
                    enabled = true  // Phase 2에서 활성화 ✅
                )
                
                RadioButtonRow(
                    text = "✏️ 시간대 직접 추가",
                    description = "내가 원하는 시간대 직접 설정",
                    selected = mode == CreationMode.CUSTOM,
                    onClick = { mode = CreationMode.CUSTOM }
                )
                
                HorizontalDivider()
                
                // 모드별 UI
                when (mode) {
                    CreationMode.TEMPLATE -> {
                        // 템플릿 선택 UI (Phase 2)
                        Text(
                            text = "템플릿 선택",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (selectedTemplate == null) {
                            // 템플릿 미선택 상태
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "템플릿을 선택하세요.\n기본 시간대가 포함된 템플릿을 사용할 수 있습니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = { showTemplateSelector = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("템플릿 선택")
                            }
                        } else {
                            // 템플릿 선택 완료 상태
                            SelectedTemplateCard(
                                template = selectedTemplate!!,
                                onClear = { selectedTemplate = null }
                            )
                        }
                    }
                    CreationMode.CUSTOM -> {
                        // 커스텀 시간대 추가 UI
                        Text(
                            text = "시간대 목록",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (timeSlots.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "아직 추가된 시간대가 없습니다.\n[+ 시간대 추가] 버튼을 눌러 시간대를 추가해주세요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            timeSlots.forEach { slot ->
                                TimeSlotItem(
                                    slot = slot,
                                    onEdit = { 
                                        editingSlot = slot
                                        showTimeSlotInput = true
                                    },
                                    onDelete = { timeSlots = timeSlots - slot }
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                editingSlot = null
                                showTimeSlotInput = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("시간대 추가")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (mode) {
                        CreationMode.TEMPLATE -> {
                            // 템플릿으로 생성 (Phase 2)
                            onConfirm(name, mode, selectedTemplate!!)
                        }
                        CreationMode.CUSTOM -> {
                            // 커스텀 시간대로 생성
                            onConfirm(name, mode, timeSlots)
                        }
                    }
                },
                enabled = name.isNotBlank() && when (mode) {
                    CreationMode.TEMPLATE -> selectedTemplate != null  // 템플릿 선택 필요
                    CreationMode.CUSTOM -> timeSlots.isNotEmpty()     // 시간대 추가 필요
                }
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
    
    // 시간대 입력 다이얼로그
    if (showTimeSlotInput) {
        TimeSlotInputDialog(
            existingSlot = editingSlot,
            onDismiss = { 
                showTimeSlotInput = false
                editingSlot = null
            },
            onConfirm = { slot ->
                if (editingSlot != null) {
                    // 수정
                    timeSlots = timeSlots.map { 
                        if (it == editingSlot) slot else it 
                    }
                } else {
                    // 추가
                    timeSlots = timeSlots + slot
                }
                showTimeSlotInput = false
                editingSlot = null
            }
        )
    }
    
    // 템플릿 선택 Dialog (Phase 2)
    // ModalBottomSheet 대신 AlertDialog 사용 (중첩 다이얼로그 레이어링 문제 해결)
    if (showTemplateSelector) {
        TemplateSelectionDialog(
            onDismiss = { showTemplateSelector = false },
            onTemplateSelected = { template ->
                selectedTemplate = template
                showTemplateSelector = false
            }
        )
    }
}

/**
 * 라디오 버튼 행
 */
@Composable
private fun RadioButtonRow(
    text: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }
    }
}
