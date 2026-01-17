package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allday.detoxy.R
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
    var hourText by remember { mutableStateOf((existingSlot?.startHour ?: 9).toString()) }
    var minuteText by remember { mutableStateOf((existingSlot?.startMinute ?: 0).toString()) }
    var duration by remember { mutableIntStateOf(existingSlot?.durationMinutes ?: TimeSlot.DEFAULT_DURATION) }
    var preset by remember { mutableStateOf(existingSlot?.presetType ?: "STANDARD") }
    var enabledDays by remember { mutableStateOf(existingSlot?.enabledDays ?: DayOfWeek.values().toList()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (existingSlot == null) stringResource(R.string.timeslot_add) else stringResource(R.string.timeslot_edit),
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
                    text = stringResource(R.string.timeslot_start_time),
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
                        value = hourText,
                        onValueChange = { newValue ->
                            // 빈 문자열 허용
                            if (newValue.isEmpty()) {
                                hourText = ""
                            } else {
                                // 숫자만 허용하고 0-23 범위 체크
                                newValue.toIntOrNull()?.let { h ->
                                    if (h in 0..23) {
                                        hourText = newValue
                                    }
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.timeslot_hour)) },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    
                    // 분 선택
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { newValue ->
                            // 빈 문자열 허용
                            if (newValue.isEmpty()) {
                                minuteText = ""
                            } else {
                                // 숫자만 허용하고 0-59 범위 체크
                                newValue.toIntOrNull()?.let { m ->
                                    if (m in 0..59) {
                                        minuteText = newValue
                                    }
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.timeslot_minute)) },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                HorizontalDivider()
                
                // 기간
                Text(
                    text = stringResource(R.string.timeslot_duration),
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
                    text = stringResource(R.string.timeslot_block_intensity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(
                        text = stringResource(R.string.timeslot_preset_standard),
                        selected = preset == "STANDARD",
                        onClick = { preset = "STANDARD" },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        text = stringResource(R.string.timeslot_preset_medium),
                        selected = preset == "MEDIUM",
                        onClick = { preset = "MEDIUM" },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        text = stringResource(R.string.timeslot_preset_complete),
                        selected = preset == "COMPLETE",
                        onClick = { preset = "COMPLETE" },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider()
                
                // 요일 선택
                Text(
                    text = stringResource(R.string.timeslot_day_selection),
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
                        label = { Text(stringResource(R.string.timeslot_everyday)) }
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
                        label = { Text(stringResource(R.string.timeslot_weekdays)) }
                    )
                    FilterChip(
                        selected = enabledDays.size == 2 && enabledDays.containsAll(listOf(
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                        )),
                        onClick = { 
                            enabledDays = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        },
                        label = { Text(stringResource(R.string.timeslot_weekend)) }
                    )
                }
                
                // 개별 요일 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        val dayLabel = when (day) {
                            DayOfWeek.MONDAY -> stringResource(R.string.timeslot_day_mon)
                            DayOfWeek.TUESDAY -> stringResource(R.string.timeslot_day_tue)
                            DayOfWeek.WEDNESDAY -> stringResource(R.string.timeslot_day_wed)
                            DayOfWeek.THURSDAY -> stringResource(R.string.timeslot_day_thu)
                            DayOfWeek.FRIDAY -> stringResource(R.string.timeslot_day_fri)
                            DayOfWeek.SATURDAY -> stringResource(R.string.timeslot_day_sat)
                            DayOfWeek.SUNDAY -> stringResource(R.string.timeslot_day_sun)
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
                    val hour = hourText.toIntOrNull() ?: 0
                    val minute = minuteText.toIntOrNull() ?: 0
                    onConfirm(TimeSlot(hour, minute, duration, preset, enabledDays))
                },
                enabled = enabledDays.isNotEmpty() && hourText.isNotEmpty() && minuteText.isNotEmpty()
            ) {
                Text(if (existingSlot == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_edit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
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
                        "STANDARD" -> stringResource(R.string.timeslot_preset_standard_block)
                        "MEDIUM" -> stringResource(R.string.timeslot_preset_medium_block)
                        "COMPLETE" -> stringResource(R.string.timeslot_preset_complete_block)
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
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = { onDelete(slot) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

