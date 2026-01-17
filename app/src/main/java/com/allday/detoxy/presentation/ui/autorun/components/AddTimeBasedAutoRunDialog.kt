package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.R
import com.allday.detoxy.data.local.converter.ScheduleInfoConverter
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.ScheduleTodo
import com.allday.detoxy.domain.validation.validateScheduleInfo
import com.allday.detoxy.domain.validation.ValidationResult
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import org.json.JSONArray
import java.util.*

/**
 * 시간 기반 자동 실행 추가/편집 다이얼로그
 *
 * 시간, 타이머 시간, 차단 프리셋, 요일, 라벨을 입력받아 TimeBasedAutoRun을 생성합니다.
 *
 * @param existingAutoRun 편집할 TimeBasedAutoRun (null이면 추가 모드)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onSave 저장 버튼 클릭 콜백 (생성된 TimeBasedAutoRun 전달)
 * @param scheduleViewModel ScheduleGroupViewModel (3차 고도화: 시간표 연동)
 * @param initialScheduleGroupId 초기 스케줄 그룹 ID (특정 스케줄 그룹에 시간대 추가 시 사용)
 * @param isLocationBased 위치기반 스케쥴 여부 (true인 경우 scheduleGroupId 변경 불가)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeBasedAutoRunDialog(
    existingAutoRun: TimeBasedAutoRun? = null,
    onDismiss: () -> Unit,
    onSave: (TimeBasedAutoRun) -> Unit,
    scheduleViewModel: ScheduleGroupViewModel = hiltViewModel(),  // 🆕 3차 고도화
    initialScheduleGroupId: String? = null,  // 🆕 특정 스케줄 그룹에 시간대 추가 시 사용
    @Suppress("UNUSED_PARAMETER") isLocationBased: Boolean = false  // ⏸️ API 호환성 유지 (UI 제거됨)
) {
    // 상태 관리
    var selectedHour by remember { mutableStateOf(existingAutoRun?.hour ?: 9) }
    var selectedMinute by remember { mutableStateOf(existingAutoRun?.minute ?: 0) }
    var durationMinutes by remember { mutableStateOf(existingAutoRun?.durationMinutes ?: 25) }
    var selectedPreset by remember { mutableStateOf(existingAutoRun?.presetType ?: "STANDARD") }
    var label by remember { mutableStateOf(existingAutoRun?.label ?: "") }
    
    // 🆕 v9: 목표/할일 상태
    val scheduleInfoConverter = remember { ScheduleInfoConverter() }
    var scheduleInfo by remember { 
        mutableStateOf(
            existingAutoRun?.scheduleInfoJson?.let { scheduleInfoConverter.toScheduleInfo(it) } 
                ?: ScheduleInfo.EMPTY
        ) 
    }
    var showTodoSection by remember { mutableStateOf(scheduleInfo.todos.isNotEmpty()) }
    
    // 🆕 3차 고도화: 시간표 목록 (기본 요일 계산에 사용)
    val scheduleGroups by scheduleViewModel.scheduleGroups.collectAsStateWithLifecycle()
    
    // 요일 선택 상태 (MON, TUE, WED, THU, FRI, SAT, SUN)
    // 🔧 리뷰 반영: scheduleGroups를 키에 포함하여 로딩 완료 후 기본값 재계산
    val targetGroupId = existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId
    val targetGroup = scheduleGroups.find { it.id == targetGroupId }
    val isDaily = targetGroup?.name?.contains("매일") == true || 
                  targetGroup?.name?.contains("Daily", ignoreCase = true) == true
    
    val enabledDays = remember(existingAutoRun, isDaily) {
        mutableStateMapOf<String, Boolean>().apply {
            val existing = existingAutoRun?.let { parseEnabledDays(it.enabledDays) } ?: emptySet()
            
            val defaultDays = if (isDaily) {
                // 매일 스케줄이면 월~일 모두 선택
                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            } else {
                // 기본값은 평일(월~금)
                listOf("MON", "TUE", "WED", "THU", "FRI")
            }

            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                this[day] = existing.contains(day) || (existingAutoRun == null && day in defaultDays)
            }
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingAutoRun != null) stringResource(R.string.timeslot_edit) else stringResource(R.string.timeslot_add),
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
                // 1. 시간 선택
                Text(
                    text = stringResource(R.string.timeslot_start_time),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                TimePickerSection(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it }
                )

                // 2. 타이머 시간 선택
                Text(
                    text = stringResource(R.string.location_timer_duration),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                DurationSelector(
                    durationMinutes = durationMinutes,
                    onDurationChange = { durationMinutes = it }
                )

                // 3. 차단 프리셋 선택
                Text(
                    text = stringResource(R.string.timeslot_block_intensity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                PresetSelector(
                    selectedPreset = selectedPreset,
                    onPresetChange = { selectedPreset = it }
                )

                // 4. 요일 선택
                Text(
                    text = stringResource(R.string.schedule_repeat_day),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                DayOfWeekSelector(
                    enabledDays = enabledDays,
                    onDayToggle = { day, enabled ->
                        enabledDays[day] = enabled
                    }
                )

                // 5. 라벨 입력 (선택 사항)
                Text(
                    text = stringResource(R.string.schedule_label_optional),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.autorun_label_placeholder)) },
                    singleLine = true
                )
                
                // 🆕 v9: 목표/할일 성정 섹션
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                GoalInputSection(
                    title = scheduleInfo.title,
                    onTitleChange = { newTitle ->
                        scheduleInfo = scheduleInfo.copy(title = newTitle)
                    },
                    showTodoSection = showTodoSection,
                    onToggleTodoSection = { showTodoSection = true }
                )
                
                if (showTodoSection) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TodoListSection(
                        todos = scheduleInfo.todos,
                        onTodoChange = { index, updatedTodo ->
                            val newTodos = scheduleInfo.todos.toMutableList()
                            newTodos[index] = updatedTodo
                            scheduleInfo = scheduleInfo.copy(todos = newTodos)
                        },
                        onTodoDelete = { index ->
                            val newTodos = scheduleInfo.todos.toMutableList()
                            newTodos.removeAt(index)
                            // 🔧 리뷰 반영: 삭제 후 orderIndex 재정렬
                            val reorderedTodos = newTodos.mapIndexed { newIndex, todo ->
                                todo.copy(orderIndex = newIndex)
                            }
                            scheduleInfo = scheduleInfo.copy(todos = reorderedTodos)
                        },
                        onTodoAdd = {
                            if (scheduleInfo.todos.size < ScheduleTodo.MAX_TODO_COUNT) {
                                // 🔧 리뷰 반영: 새 항목의 orderIndex는 현재 목록의 최대값 + 1
                                val maxOrderIndex = scheduleInfo.todos.maxOfOrNull { it.orderIndex } ?: -1
                                val newTodos = scheduleInfo.todos + ScheduleTodo(
                                    content = "",
                                    orderIndex = maxOrderIndex + 1
                                )
                                scheduleInfo = scheduleInfo.copy(todos = newTodos)
                            }
                        },
                        onHide = { showTodoSection = false }
                    )
                }
                

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedDays = enabledDays.filter { it.value }.keys.toList()
                    // 🔧 리뷰 반영: 직접 계산 (불필요한 상태 변수 제거)
                    val finalScheduleGroupId = existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId
                    val finalIsIndependent = existingAutoRun?.isIndependent ?: (initialScheduleGroupId == null)

                    
                    // 디버깅 로그 (필요시 활성화)
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "저장: scheduleGroupId=$finalScheduleGroupId, isIndependent=$finalIsIndependent")
                    

                    // 🔧 리뷰 반영: 빈 할일 필터링 및 검증
                    val filteredInfo = if (scheduleInfo.hasContent()) {
                        val validTodos = scheduleInfo.todos.filter { it.content.isNotBlank() }
                        scheduleInfo.copy(todos = validTodos)
                    } else {
                        ScheduleInfo.EMPTY
                    }
                    
                    // 검증 수행
                    val validationResult = validateScheduleInfo(filteredInfo)
                    if (validationResult is ValidationResult.Error) {
                        android.util.Log.w("AddTimeBasedAutoRunDialog", "검증 실패: ${validationResult.message}")
                        // 검증 실패 시 저장 중단 (토스트 등 UI 피드백은 Phase 6에서 구현)
                        return@Button
                    }
                    
                    val newAutoRun = TimeBasedAutoRun(
                        id = existingAutoRun?.id ?: UUID.randomUUID().toString(),
                        hour = selectedHour,
                        minute = selectedMinute,
                        durationMinutes = durationMinutes,
                        presetType = selectedPreset,
                        enabledDays = JSONArray(selectedDays).toString(),
                        label = label.takeIf { it.isNotBlank() },
                        isEnabled = existingAutoRun?.isEnabled ?: true,
                        createdAt = existingAutoRun?.createdAt ?: System.currentTimeMillis(),
                        // 🆕 3차 고도화: 시간표 연결 필드
                        scheduleGroupId = finalScheduleGroupId,
                        isIndependent = finalIsIndependent,
                        // 🆕 v9: 목표/할일 정보 저장 (필터링된 정보 사용)
                        scheduleInfoJson = if (filteredInfo.hasContent()) {
                            scheduleInfoConverter.fromScheduleInfo(filteredInfo)
                        } else {
                            null
                        }
                    )
                    
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "✅ 최종 저장 데이터: scheduleGroupId=${newAutoRun.scheduleGroupId}, isIndependent=${newAutoRun.isIndependent}")
                    
                    onSave(newAutoRun)
                },
                enabled = enabledDays.any { it.value }
            ) {
                Text(if (existingAutoRun != null) stringResource(R.string.btn_edit) else stringResource(R.string.btn_save))
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
 * 시간 선택 섹션 (Hour/Minute Picker)
 */
@Composable
private fun TimePickerSection(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    // 초기값만 설정하고 이후에는 사용자 입력으로만 업데이트
    var hourText by remember { mutableStateOf(hour.toString()) }
    var minuteText by remember { mutableStateOf(minute.toString()) }
    
    // prop 변경 시 초기화 (다이얼로그가 다시 열릴 때만)
    LaunchedEffect(hour, minute) {
        if (hourText.isEmpty() || hourText.toIntOrNull() != hour) {
            hourText = hour.toString()
        }
        if (minuteText.isEmpty() || minuteText.toIntOrNull() != minute) {
            minuteText = minute.toString()
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour Selector
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.timeslot_hour),
                style = MaterialTheme.typography.labelSmall
            )
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
                                onHourChange(h)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0") }
            )
        }

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Minute Selector
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.timeslot_minute),
                style = MaterialTheme.typography.labelSmall
            )
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
                                onMinuteChange(m)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0") }
            )
        }
    }
}

/**
 * 타이머 시간 선택 섹션
 */
@Composable
private fun DurationSelector(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit
) {
    val presetDurations = listOf(15, 25, 30, 45, 60, 90, 120)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 프리셋 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetDurations.take(4).forEach { duration ->
                FilterChip(
                    selected = durationMinutes == duration,
                    onClick = { onDurationChange(duration) },
                    label = { Text("${duration}분") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetDurations.drop(4).forEach { duration ->
                FilterChip(
                    selected = durationMinutes == duration,
                    onClick = { onDurationChange(duration) },
                    label = { Text("${duration}분") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 직접 입력
        var durationText by remember { mutableStateOf(durationMinutes.toString()) }
        
        // prop 변경 시 초기화
        LaunchedEffect(durationMinutes) {
            if (durationText.isEmpty() || durationText.toIntOrNull() != durationMinutes) {
                durationText = durationMinutes.toString()
            }
        }
        
        OutlinedTextField(
            value = durationText,
            onValueChange = { newValue ->
                // 빈 문자열 허용
                if (newValue.isEmpty()) {
                    durationText = ""
                } else {
                    // 숫자만 허용하고 1-180 범위 체크
                    newValue.toIntOrNull()?.let { d ->
                        if (d in 1..180) {
                            durationText = newValue
                            onDurationChange(d)
                        }
                    }
                }
            },
            label = { Text(stringResource(R.string.schedule_duration_custom)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("25") }
        )
    }
}

/**
 * 차단 프리셋 선택 섹션
 */
@Composable
private fun PresetSelector(
    selectedPreset: String,
    onPresetChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetOption(
            title = stringResource(R.string.preset_full_block),
            description = stringResource(R.string.preset_full_block_desc),
            value = "FULL_BLOCK",
            selected = selectedPreset == "FULL_BLOCK",
            onClick = { onPresetChange("FULL_BLOCK") }
        )
        
        PresetOption(
            title = stringResource(R.string.preset_standard_detoxy),
            description = stringResource(R.string.timer_mode_standard_desc),
            value = "STANDARD",
            selected = selectedPreset == "STANDARD",
            onClick = { onPresetChange("STANDARD") }
        )
        
        PresetOption(
            title = stringResource(R.string.preset_relaxed_detoxy),
            description = stringResource(R.string.timer_mode_light_desc),
            value = "RELAXED",
            selected = selectedPreset == "RELAXED",
            onClick = { onPresetChange("RELAXED") }
        )
    }
}

/**
 * 프리셋 옵션 아이템
 */
@Composable
private fun PresetOption(
    title: String,
    description: String,
    @Suppress("UNUSED_PARAMETER") value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = { onClick() },
                role = Role.RadioButton
            ),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

/**
 * 요일 선택 섹션
 */
@Composable
private fun DayOfWeekSelector(
    enabledDays: Map<String, Boolean>,
    onDayToggle: (String, Boolean) -> Unit
) {
    val days = listOf(
        "MON" to stringResource(R.string.timeslot_day_mon),
        "TUE" to stringResource(R.string.timeslot_day_tue),
        "WED" to stringResource(R.string.timeslot_day_wed),
        "THU" to stringResource(R.string.timeslot_day_thu),
        "FRI" to stringResource(R.string.timeslot_day_fri),
        "SAT" to stringResource(R.string.timeslot_day_sat),
        "SUN" to stringResource(R.string.timeslot_day_sun)
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { (code, displayName) ->
            FilterChip(
                selected = enabledDays[code] == true,
                onClick = { onDayToggle(code, enabledDays[code] != true) },
                label = { Text(displayName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * enabledDays JSON 문자열 파싱 Helper
 */
private fun parseEnabledDays(enabledDaysJson: String): Set<String> {
    return try {
        val jsonArray = JSONArray(enabledDaysJson)
        val days = mutableSetOf<String>()
        for (i in 0 until jsonArray.length()) {
            days.add(jsonArray.getString(i))
        }
        days
    } catch (e: Exception) {
        emptySet()
    }
}

