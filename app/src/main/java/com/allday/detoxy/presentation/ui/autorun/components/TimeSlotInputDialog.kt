package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.domain.model.TimeSlot
import com.allday.detoxy.presentation.util.formatDuration
import com.allday.detoxy.presentation.util.formatEnabledDays
import com.allday.detoxy.presentation.util.formatStartTime
import java.time.DayOfWeek

/**
 * 시간대 입력 다이얼로그 (Phase 1)
 *
 * 사용자가 시간대를 추가하거나 수정할 수 있는 다이얼로그입니다.
 *
 * ## 입력 항목
 * - 시작 시간 (시:분)
 * - 기간 (15분~12시간, Slider)
 * - 차단 강도 (표준/중간/완전)
 * - 요일 선택 (매일/평일/주말/개별 선택)
 *
 * @param existingSlot 수정할 기존 시간대 (null이면 새로 추가)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 확인 콜백 (TimeSlot 전달)
 */
@Composable
fun TimeSlotInputDialog(
    existingSlot: TimeSlot? = null,
    onDismiss: () -> Unit,
    onConfirm: (TimeSlot) -> Unit
) {
    var hour by remember { mutableIntStateOf(existingSlot?.startHour ?: 9) }
    var minute by remember { mutableIntStateOf(existingSlot?.startMinute ?: 0) }
    var duration by remember { mutableIntStateOf(existingSlot?.durationMinutes ?: TimeSlot.DEFAULT_DURATION) }
    var preset by remember { mutableStateOf(existingSlot?.presetType ?: "STANDARD") }
    var enabledDays by remember { mutableStateOf(existingSlot?.enabledDays ?: DayOfWeek.values().toList()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (existingSlot == null) "시간대 추가" else "시간대 수정",
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
                // 시작 시간
                Text(
                    text = "시작 시간",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 시간 선택
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { 
                            hour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0
                        },
                        label = { Text("시") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    
                    // 분 선택
                    OutlinedTextField(
                        value = minute.toString(),
                        onValueChange = { 
                            minute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        },
                        label = { Text("분") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                HorizontalDivider()
                
                // 기간
                Text(
                    text = "기간",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = TimeSlot(0, 0, duration, "", emptyList()).formatDuration(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = TimeSlot.MIN_DURATION.toFloat()..TimeSlot.MAX_DURATION.toFloat(),
                    steps = (TimeSlot.MAX_DURATION - TimeSlot.MIN_DURATION) / 15 - 1
                )
                
                HorizontalDivider()
                
                // 차단 강도
                Text(
                    text = "차단 강도",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(
                        text = "표준",
                        selected = preset == "STANDARD",
                        onClick = { preset = "STANDARD" },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        text = "중간",
                        selected = preset == "MEDIUM",
                        onClick = { preset = "MEDIUM" },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        text = "완전",
                        selected = preset == "COMPLETE",
                        onClick = { preset = "COMPLETE" },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider()
                
                // 요일 선택
                Text(
                    text = "요일 선택",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 빠른 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = enabledDays.size == 7,
                        onClick = { enabledDays = DayOfWeek.values().toList() },
                        label = { Text("매일") }
                    )
                    FilterChip(
                        selected = enabledDays.size == 5 && enabledDays.containsAll(listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        )),
                        onClick = { 
                            enabledDays = listOf(
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                            )
                        },
                        label = { Text("평일") }
                    )
                    FilterChip(
                        selected = enabledDays.size == 2 && enabledDays.containsAll(listOf(
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                        )),
                        onClick = { 
                            enabledDays = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        },
                        label = { Text("주말") }
                    )
                }
                
                // 개별 요일 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        val dayLabel = when (day) {
                            DayOfWeek.MONDAY -> "월"
                            DayOfWeek.TUESDAY -> "화"
                            DayOfWeek.WEDNESDAY -> "수"
                            DayOfWeek.THURSDAY -> "목"
                            DayOfWeek.FRIDAY -> "금"
                            DayOfWeek.SATURDAY -> "토"
                            DayOfWeek.SUNDAY -> "일"
                        }
                        
                        FilterChip(
                            selected = enabledDays.contains(day),
                            onClick = {
                                enabledDays = if (enabledDays.contains(day)) {
                                    enabledDays - day
                                } else {
                                    enabledDays + day
                                }
                            },
                            label = { Text(dayLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(TimeSlot(hour, minute, duration, preset, enabledDays))
                },
                enabled = enabledDays.isNotEmpty()  // 최소 1개 요일 필요
            ) {
                Text(if (existingSlot == null) "추가" else "수정")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 차단 강도 선택 칩
 */
@Composable
private fun PresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier
    )
}

/**
 * 시간대 아이템 (리스트 표시용)
 *
 * QuickCreateScheduleDialog의 커스텀 모드에서 추가된 시간대를 표시합니다.
 *
 * ## 표시 정보
 * - 시작 시간 및 기간 (예: "09:00 (3시간)")
 * - 차단 강도 (예: "표준 차단")
 * - 요일 (예: "평일")
 *
 * @param slot 표시할 시간대
 * @param onEdit 편집 버튼 클릭 콜백
 * @param onDelete 삭제 버튼 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun TimeSlotItem(
    slot: TimeSlot,
    onEdit: (TimeSlot) -> Unit,
    onDelete: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = "${slot.formatStartTime()} (${slot.formatDuration()})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (slot.presetType) {
                        "STANDARD" -> "표준 차단"
                        "MEDIUM" -> "중간 차단"
                        "COMPLETE" -> "완전 차단"
                        else -> slot.presetType
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = slot.formatEnabledDays(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row {
                IconButton(onClick = { onEdit(slot) }) {
                    Icon(Icons.Default.Edit, contentDescription = "편집")
                }
                IconButton(onClick = { onDelete(slot) }) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
            }
        }
    }
}

