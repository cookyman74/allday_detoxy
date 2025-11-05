package com.allday.detoxy.presentation.ui.autorun.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.core.utils.GeocoderUtils
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import kotlinx.coroutines.launch

/**
 * 스케줄 생성 다이얼로그 (PRD §3.1 준수)
 *
 * ## 프로세스 흐름 (03.5_예약설정 프로세스.md)
 * 1. 위치 설정 여부 선택
 *    - 어디서나 적용 (위치 없음)
 *    - 특정 위치에서만 (위치 있음)
 * 2. 위치 있음 선택 시: 주소/좌표 검색 → 반경 설정
 * 3. 시간표 생성 방식 선택 (템플릿 vs 커스텀)
 * 4. 시간대 추가
 * 5. 저장
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 확인 콜백 (이름, 모드, 시간대, 위치 정보)
 * @param initialMode 초기 생성 모드 (TEMPLATE or CUSTOM)
 */
@Composable
fun ScheduleCreationDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        mode: CreationMode,
        timeSlots: List<TimeSlot>,
        template: ScheduleTemplate?,
        locationInfo: LocationInfo?
    ) -> Unit,
    initialMode: CreationMode = CreationMode.CUSTOM
) {
    var currentStep by remember { mutableStateOf(ScheduleCreationStep.LOCATION_CHOICE) }
    
    // 위치 설정
    var hasLocation by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<GeocoderUtils.LocationInfo?>(null) }
    var radiusMeters by remember { mutableIntStateOf(100) }
    
    // 시간표 설정
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(initialMode) }
    var timeSlots by remember { mutableStateOf(listOf<TimeSlot>()) }
    var selectedTemplate by remember { mutableStateOf<ScheduleTemplate?>(null) }
    
    // UI 상태
    var showLocationSearch by remember { mutableStateOf(false) }
    var showTimeSlotInput by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var showTemplateSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (currentStep) {
                    ScheduleCreationStep.LOCATION_CHOICE -> "위치 설정 여부"
                    ScheduleCreationStep.LOCATION_SEARCH -> "위치 검색"
                    ScheduleCreationStep.SCHEDULE_SETUP -> "시간표 만들기"
                },
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
                when (currentStep) {
                    ScheduleCreationStep.LOCATION_CHOICE -> {
                        LocationChoiceStep(
                            hasLocation = hasLocation,
                            onHasLocationChange = { hasLocation = it }
                        )
                    }
                    ScheduleCreationStep.LOCATION_SEARCH -> {
                        LocationSearchStep(
                            selectedLocation = selectedLocation,
                            radiusMeters = radiusMeters,
                            onLocationSelect = { showLocationSearch = true },
                            onRadiusChange = { radiusMeters = it }
                        )
                    }
                    ScheduleCreationStep.SCHEDULE_SETUP -> {
                        ScheduleSetupStep(
                            name = name,
                            onNameChange = { name = it },
                            mode = mode,
                            onModeChange = { mode = it },
                            timeSlots = timeSlots,
                            selectedTemplate = selectedTemplate,
                            onAddTimeSlot = { showTimeSlotInput = true },
                            onEditTimeSlot = { slot ->
                                editingSlot = slot
                                showTimeSlotInput = true
                            },
                            onDeleteTimeSlot = { slot ->
                                timeSlots = timeSlots - slot
                            },
                            onTemplateSelect = { showTemplateSelector = true }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (currentStep) {
                        ScheduleCreationStep.LOCATION_CHOICE -> {
                            Log.d("ScheduleCreationDialog", "📍 Location choice: hasLocation=$hasLocation")
                            if (hasLocation) {
                                Log.d("ScheduleCreationDialog", "→ Moving to LOCATION_SEARCH step")
                                currentStep = ScheduleCreationStep.LOCATION_SEARCH
                            } else {
                                Log.d("ScheduleCreationDialog", "→ Moving to SCHEDULE_SETUP step (no location)")
                                currentStep = ScheduleCreationStep.SCHEDULE_SETUP
                            }
                        }
                        ScheduleCreationStep.LOCATION_SEARCH -> {
                            Log.d("ScheduleCreationDialog", "📍 Location search step: selectedLocation=$selectedLocation")
                            if (selectedLocation != null) {
                                Log.d("ScheduleCreationDialog", "→ Moving to SCHEDULE_SETUP step with location: ${selectedLocation!!.name}")
                                currentStep = ScheduleCreationStep.SCHEDULE_SETUP
                            }
                        }
                        ScheduleCreationStep.SCHEDULE_SETUP -> {
                            Log.d("ScheduleCreationDialog", "=== 🔍 Schedule Creation Debug ===")
                            Log.d("ScheduleCreationDialog", "hasLocation: $hasLocation")
                            Log.d("ScheduleCreationDialog", "selectedLocation: $selectedLocation")
                            Log.d("ScheduleCreationDialog", "name: $name")
                            Log.d("ScheduleCreationDialog", "mode: $mode")
                            Log.d("ScheduleCreationDialog", "selectedTemplate: $selectedTemplate")
                            Log.d("ScheduleCreationDialog", "timeSlots: ${timeSlots.size}")
                            
                            val locationInfo = if (hasLocation && selectedLocation != null) {
                                LocationInfo(
                                    name = selectedLocation!!.name,  // 🆕 위치 이름 전달
                                    address = selectedLocation!!.address,
                                    latitude = selectedLocation!!.latitude,
                                    longitude = selectedLocation!!.longitude,
                                    radiusMeters = radiusMeters
                                ).also {
                                    Log.d("ScheduleCreationDialog", "✅ LocationInfo created: ${it.name} (${it.address})")
                                }
                            } else {
                                Log.d("ScheduleCreationDialog", "❌ LocationInfo is NULL (hasLocation=$hasLocation, selectedLocation=$selectedLocation)")
                                null
                            }
                            
                            Log.d("ScheduleCreationDialog", "📤 Calling onConfirm with locationInfo: $locationInfo")
                            
                            onConfirm(
                                name,
                                mode,
                                timeSlots,
                                selectedTemplate,
                                locationInfo
                            )
                        }
                    }
                },
                enabled = when (currentStep) {
                    ScheduleCreationStep.LOCATION_CHOICE -> true
                    ScheduleCreationStep.LOCATION_SEARCH -> selectedLocation != null
                    ScheduleCreationStep.SCHEDULE_SETUP -> {
                        name.isNotBlank() && when (mode) {
                            CreationMode.TEMPLATE -> selectedTemplate != null
                            CreationMode.CUSTOM -> timeSlots.isNotEmpty()
                        }
                    }
                }
            ) {
                Text(
                    when (currentStep) {
                        ScheduleCreationStep.LOCATION_CHOICE -> "다음"
                        ScheduleCreationStep.LOCATION_SEARCH -> "다음"
                        ScheduleCreationStep.SCHEDULE_SETUP -> "만들기"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (currentStep == ScheduleCreationStep.LOCATION_CHOICE) {
                        onDismiss()
                    } else {
                        // 이전 단계로
                        currentStep = when (currentStep) {
                            ScheduleCreationStep.LOCATION_SEARCH -> ScheduleCreationStep.LOCATION_CHOICE
                            ScheduleCreationStep.SCHEDULE_SETUP -> {
                                if (hasLocation) {
                                    ScheduleCreationStep.LOCATION_SEARCH
                                } else {
                                    ScheduleCreationStep.LOCATION_CHOICE
                                }
                            }
                            else -> currentStep
                        }
                    }
                }
            ) {
                Text(if (currentStep == ScheduleCreationStep.LOCATION_CHOICE) "취소" else "이전")
            }
        }
    )
    
    // 위치 검색 다이얼로그
    if (showLocationSearch) {
        LocationSearchDialog(
            onDismiss = { 
                Log.d("ScheduleCreationDialog", "❌ Location search dismissed without selection")
                showLocationSearch = false 
            },
            onLocationSelected = { location ->
                Log.d("ScheduleCreationDialog", "📍 Location selected: ${location.name} (${location.address})")
                selectedLocation = location
                showLocationSearch = false
            }
        )
    }
    
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
                    timeSlots = timeSlots.map {
                        if (it == editingSlot) slot else it
                    }
                } else {
                    timeSlots = timeSlots + slot
                }
                showTimeSlotInput = false
                editingSlot = null
            }
        )
    }
    
    // 템플릿 선택 다이얼로그
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
 * 스케줄 생성 단계
 */
enum class ScheduleCreationStep {
    LOCATION_CHOICE,    // 1. 위치 설정 여부 선택
    LOCATION_SEARCH,    // 2. 위치 검색 (hasLocation == true일 때)
    SCHEDULE_SETUP      // 3. 시간표 설정
}

/**
 * 위치 정보
 */
data class LocationInfo(
    val name: String,  // 🆕 위치 이름 (예: "내곡중학교")
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int
)

/**
 * Step 1: 위치 설정 여부 선택
 */
@Composable
private fun LocationChoiceStep(
    hasLocation: Boolean,
    onHasLocationChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "이 시간표를 특정 위치에서만 실행하시겠습니까?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 어디서나 적용
        RadioButtonOption(
            selected = !hasLocation,
            onClick = { onHasLocationChange(false) },
            title = "⚪ 어디서나 적용",
            description = "위치와 관계없이 지정된 시간에 자동 실행"
        )
        
        // 특정 위치에서만
        RadioButtonOption(
            selected = hasLocation,
            onClick = { onHasLocationChange(true) },
            title = "📍 특정 위치에서만",
            description = "지정한 위치 반경 내에서만 자동 실행"
        )
    }
}

/**
 * Step 2: 위치 검색
 */
@Composable
private fun LocationSearchStep(
    selectedLocation: GeocoderUtils.LocationInfo?,
    radiusMeters: Int,
    onLocationSelect: () -> Unit,
    onRadiusChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 위치 선택
        Text(
            text = "위치 선택",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        if (selectedLocation == null) {
            OutlinedButton(
                onClick = onLocationSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("주소 또는 장소 검색")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedLocation.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedLocation.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onLocationSelect) {
                        Icon(Icons.Default.Edit, "변경")
                    }
                }
            }
        }
        
        // 반경 설정
        if (selectedLocation != null) {
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "반경: ${radiusMeters}m",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Slider(
                value = radiusMeters.toFloat(),
                onValueChange = { onRadiusChange(it.toInt()) },
                valueRange = 50f..500f,
                steps = 9
            )
            
            Text(
                text = "이 반경 내에 진입하면 자동으로 시간표가 활성화됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Step 3: 시간표 설정
 */
@Composable
private fun ScheduleSetupStep(
    name: String,
    onNameChange: (String) -> Unit,
    mode: CreationMode,
    onModeChange: (CreationMode) -> Unit,
    timeSlots: List<TimeSlot>,
    selectedTemplate: ScheduleTemplate?,
    onAddTimeSlot: () -> Unit,
    onEditTimeSlot: (TimeSlot) -> Unit,
    onDeleteTimeSlot: (TimeSlot) -> Unit,
    onTemplateSelect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 시간표 이름
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("시간표 이름 *") },
            placeholder = { Text("예: 업무 시간표, 공부 루틴") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        HorizontalDivider()
        
        // 생성 방식 선택
        Text(
            text = "시작 방법 선택",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == CreationMode.TEMPLATE,
                onClick = { onModeChange(CreationMode.TEMPLATE) },
                label = { Text("📋 템플릿") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = mode == CreationMode.CUSTOM,
                onClick = { onModeChange(CreationMode.CUSTOM) },
                label = { Text("✏️ 커스텀") },
                modifier = Modifier.weight(1f)
            )
        }
        
        HorizontalDivider()
        
        // 모드별 UI
        when (mode) {
            CreationMode.TEMPLATE -> {
                if (selectedTemplate == null) {
                    OutlinedButton(
                        onClick = onTemplateSelect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.List, null)
                        Spacer(Modifier.width(4.dp))
                        Text("템플릿 선택")
                    }
                } else {
                    SelectedTemplateCard(
                        template = selectedTemplate,
                        onClear = { /* selectedTemplate = null */ }
                    )
                }
            }
            CreationMode.CUSTOM -> {
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
                            text = "시간대를 추가해주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    timeSlots.forEach { slot ->
                        TimeSlotItem(
                            slot = slot,
                            onEdit = { onEditTimeSlot(slot) },
                            onDelete = { onDeleteTimeSlot(slot) }
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onAddTimeSlot,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("시간대 추가")
                }
            }
        }
    }
}

/**
 * 라디오 버튼 옵션
 */
@Composable
private fun RadioButtonOption(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 위치 검색 다이얼로그
 */
@Composable
private fun LocationSearchDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (GeocoderUtils.LocationInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocoderUtils.LocationInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("위치 검색") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("주소 또는 장소 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (searchQuery.isNotBlank()) {
                                        isSearching = true
                                        val result = GeocoderUtils.searchLocation(context, searchQuery)
                                        result.onSuccess { locations ->
                                            searchResults = locations
                                        }.onFailure {
                                            searchResults = emptyList()
                                        }
                                        isSearching = false
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, "검색")
                        }
                    }
                )
                
                if (isSearching) {
                    CircularProgressIndicator()
                }
                
                if (searchResults.isNotEmpty()) {
                    Column {
                        searchResults.take(5).forEach { location ->
                            ListItem(
                                headlineContent = { Text(location.name) },
                                supportingContent = { Text(location.address) },
                                modifier = Modifier.clickable {
                                    onLocationSelected(location)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

