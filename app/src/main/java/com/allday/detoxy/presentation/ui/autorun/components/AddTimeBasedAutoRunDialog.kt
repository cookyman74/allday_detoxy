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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
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
    isLocationBased: Boolean = false  // 🐛 버그 수정: 위치기반 스케쥴 여부
) {
    // 상태 관리
    var selectedHour by remember { mutableStateOf(existingAutoRun?.hour ?: 9) }
    var selectedMinute by remember { mutableStateOf(existingAutoRun?.minute ?: 0) }
    var durationMinutes by remember { mutableStateOf(existingAutoRun?.durationMinutes ?: 25) }
    var selectedPreset by remember { mutableStateOf(existingAutoRun?.presetType ?: "STANDARD") }
    var label by remember { mutableStateOf(existingAutoRun?.label ?: "") }
    
    // 🆕 3차 고도화: 시간표 연결 상태
    // 🆕 특정 스케줄 그룹에서 호출된 경우 해당 그룹 ID로 초기화
    // 🐛 버그 수정: initialScheduleGroupId가 있으면 위치기반으로 간주 (isLocationBased가 false여도)
    val actualIsLocationBased = remember(initialScheduleGroupId, isLocationBased, existingAutoRun?.scheduleGroupId) {
        isLocationBased || (initialScheduleGroupId != null) || (existingAutoRun?.scheduleGroupId != null && !existingAutoRun.isIndependent)
    }
    
    var selectedScheduleGroupId by remember { 
        mutableStateOf(existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId) 
    }
    var isIndependent by remember { 
        mutableStateOf(existingAutoRun?.isIndependent ?: (initialScheduleGroupId == null)) 
    }
    
    // 🆕 3차 고도화: 시간표 목록
    val scheduleGroups by scheduleViewModel.scheduleGroups.collectAsStateWithLifecycle()
    
    // 요일 선택 상태 (MON, TUE, WED, THU, FRI, SAT, SUN)
    val enabledDays = remember {
        mutableStateMapOf<String, Boolean>().apply {
            val existing = existingAutoRun?.let { parseEnabledDays(it.enabledDays) } ?: emptySet()
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                this[day] = existing.contains(day) || existingAutoRun == null && day in listOf("MON", "TUE", "WED", "THU", "FRI")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingAutoRun != null) "시간대 편집" else "시간대 추가",
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
                    text = "시작 시간",
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
                    text = "타이머 시간",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                DurationSelector(
                    durationMinutes = durationMinutes,
                    onDurationChange = { durationMinutes = it }
                )

                // 3. 차단 프리셋 선택
                Text(
                    text = "차단 강도",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                PresetSelector(
                    selectedPreset = selectedPreset,
                    onPresetChange = { selectedPreset = it }
                )

                // 4. 요일 선택
                Text(
                    text = "반복 요일",
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
                    text = "라벨 (선택)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 오전 업무 집중") },
                    singleLine = true
                )
                
                // 🆕 3차 고도화: 시간표 연결 설정
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "시간표 연결",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                ScheduleLinkSection(
                    scheduleGroups = scheduleGroups,
                    selectedScheduleGroupId = selectedScheduleGroupId,
                    onScheduleGroupSelected = { groupId ->
                        // 🐛 버그 수정: 위치기반 스케쥴인 경우 scheduleGroupId 변경 불가
                        if (!actualIsLocationBased) {
                        selectedScheduleGroupId = groupId
                        isIndependent = groupId == null
                        }
                    },
                    isIndependent = isIndependent,
                    onIndependentChange = { independent ->
                        // 🐛 버그 수정: 위치기반 스케쥴인 경우 독립 실행 모드 변경 불가
                        if (!actualIsLocationBased) {
                        isIndependent = independent
                        if (independent) {
                            selectedScheduleGroupId = null
                        }
                    }
                    },
                    isLocationBased = actualIsLocationBased  // 🐛 버그 수정: 위치기반 여부 전달 (actualIsLocationBased 사용)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedDays = enabledDays.filter { it.value }.keys.toList()
                    // 🐛 버그 수정: 위치기반 스케쥴인 경우 scheduleGroupId와 isIndependent 강제 설정
                    // actualIsLocationBased 사용 (initialScheduleGroupId가 있으면 위치기반으로 간주)
                    val finalScheduleGroupId = if (actualIsLocationBased) {
                        // 위치기반 스케쥴인 경우: 기존 값이 있으면 유지, 없으면 initialScheduleGroupId 사용
                        existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId
                    } else {
                        selectedScheduleGroupId
                    }
                    val finalIsIndependent = if (actualIsLocationBased) {
                        // 위치기반 스케쥴인 경우: 그룹에 종속되어야 하므로 false
                        existingAutoRun?.isIndependent ?: false
                    } else {
                        isIndependent
                    }
                    
                    // 🐛 버그 수정: 디버깅 로그 추가
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "=== 저장 데이터 확인 ===")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "isLocationBased 파라미터: $isLocationBased")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "actualIsLocationBased: $actualIsLocationBased")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "existingAutoRun?.scheduleGroupId: ${existingAutoRun?.scheduleGroupId}")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "initialScheduleGroupId: $initialScheduleGroupId")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "selectedScheduleGroupId: $selectedScheduleGroupId")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "finalScheduleGroupId: $finalScheduleGroupId")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "existingAutoRun?.isIndependent: ${existingAutoRun?.isIndependent}")
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "finalIsIndependent: $finalIsIndependent")
                    
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
                        isIndependent = finalIsIndependent
                    )
                    
                    android.util.Log.d("AddTimeBasedAutoRunDialog", "✅ 최종 저장 데이터: scheduleGroupId=${newAutoRun.scheduleGroupId}, isIndependent=${newAutoRun.isIndependent}")
                    
                    onSave(newAutoRun)
                },
                enabled = enabledDays.any { it.value }
            ) {
                Text(if (existingAutoRun != null) "수정" else "저장")
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
                text = "시",
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
                text = "분",
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
            label = { Text("직접 입력 (1-180분)") },
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
            title = "완전 차단",
            description = "모든 앱 차단",
            value = "FULL_BLOCK",
            selected = selectedPreset == "FULL_BLOCK",
            onClick = { onPresetChange("FULL_BLOCK") }
        )
        
        PresetOption(
            title = "표준 디톡시",
            description = "SNS, 영상, Web 차단",
            value = "STANDARD",
            selected = selectedPreset == "STANDARD",
            onClick = { onPresetChange("STANDARD") }
        )
        
        PresetOption(
            title = "완화 디톡시",
            description = "SNS만 차단",
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
        "MON" to "월",
        "TUE" to "화",
        "WED" to "수",
        "THU" to "목",
        "FRI" to "금",
        "SAT" to "토",
        "SUN" to "일"
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

/**
 * 🆕 3차 고도화: 시간표 연결 섹션
 */
@Composable
private fun ScheduleLinkSection(
    scheduleGroups: List<com.allday.detoxy.data.local.entity.ScheduleGroup>,
    selectedScheduleGroupId: String?,
    onScheduleGroupSelected: (String?) -> Unit,
    isIndependent: Boolean,
    onIndependentChange: (Boolean) -> Unit,
    isLocationBased: Boolean = false  // 🐛 버그 수정: 위치기반 여부
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 🐛 버그 수정: 위치기반 스케쥴인 경우 안내 메시지 표시
        if (isLocationBased) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "📍 위치기반 스케쥴입니다. 시간표 연결은 변경할 수 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        // 독립 실행 옵션
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isIndependent,
                    role = Role.Checkbox,
                    onValueChange = onIndependentChange,
                    enabled = !isLocationBased  // 🐛 버그 수정: 위치기반인 경우 비활성화
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "독립 실행",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "시간표와 관계없이 항상 실행",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = isIndependent,
                onCheckedChange = null
            )
        }
        
        // 시간표 목록 (독립 실행이 아닐 때만 표시, 위치기반이 아닐 때만 편집 가능)
        if (!isIndependent) {
            if (scheduleGroups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ 시간표가 없습니다. '스케줄 그룹' 화면에서 시간표를 만들어주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = "시간표 선택",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                scheduleGroups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = group.id == selectedScheduleGroupId,
                                role = Role.RadioButton,
                                onValueChange = { selected ->
                                    if (selected && !isLocationBased) {  // 🐛 버그 수정: 위치기반인 경우 변경 불가
                                        onScheduleGroupSelected(group.id)
                                    }
                                },
                                enabled = !isLocationBased  // 🐛 버그 수정: 위치기반인 경우 비활성화
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (group.isActive) {
                                Text(
                                    text = "⚡ 현재 활성화 중",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        RadioButton(
                            selected = group.id == selectedScheduleGroupId,
                            onClick = null
                        )
                    }
                }
            }
        }
    }
}

