package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.utils.GeocoderUtils
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import kotlinx.coroutines.launch
import java.util.*

/**
 * 위치 다이얼로그 단계
 * 
 * 3차 고도화: SCHEDULE 단계 추가
 */
enum class LocationDialogStep {
    SEARCH,      // 위치 검색
    SETTINGS,    // 상세 설정
    SCHEDULE     // 🆕 시간표 연결 (3차 고도화)
}

/**
 * 위치 기반 자동 실행 추가/편집 다이얼로그 (3차 고도화)
 *
 * 위치 검색, 위치 설정, 시간표 연결을 통해 LocationBasedAutoRun을 생성합니다.
 *
 * ## 3차 고도화 추가 기능
 * - SCHEDULE 단계 추가: 시간표 자동 활성화/비활성화 설정
 * - ScheduleGroupViewModel 주입
 *
 * @param existingLocation 편집할 LocationBasedAutoRun (null이면 추가 모드)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onSave 저장 버튼 클릭 콜백 (생성된 LocationBasedAutoRun 전달)
 * @param scheduleViewModel ScheduleGroupViewModel (3차 고도화)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationAutoRunDialog(
    existingLocation: LocationBasedAutoRun? = null,
    onDismiss: () -> Unit,
    onSave: (LocationBasedAutoRun) -> Unit,
    scheduleViewModel: ScheduleGroupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 단계 관리
    var currentStep by remember { 
        mutableStateOf(
            if (existingLocation != null) LocationDialogStep.SETTINGS 
            else LocationDialogStep.SEARCH
        ) 
    }

    // 위치 검색 상태
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocoderUtils.LocationInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<GeocoderUtils.LocationInfo?>(null) }

    // 상세 설정 상태
    var label by remember {
        mutableStateOf(existingLocation?.label ?: selectedLocation?.name ?: "")
    }
    var radiusMeters by remember { mutableStateOf(existingLocation?.radiusMeters ?: 100) }
    var durationMinutes by remember { mutableStateOf(existingLocation?.durationMinutes ?: 45) }
    var selectedPreset by remember { mutableStateOf(existingLocation?.presetType ?: "STANDARD") }
    var triggerType by remember { mutableStateOf(existingLocation?.triggerType ?: "ENTER") }
    var dwellTimeMinutes by remember { mutableStateOf(existingLocation?.dwellTimeMinutes ?: 0) }
    var requiresUserConfirmation by remember { mutableStateOf(existingLocation?.requiresUserConfirmation ?: false) }

    // 🆕 3차 고도화: 시간표 연결 상태
    var enableScheduleLink by remember { mutableStateOf(existingLocation?.linkedScheduleGroupId != null) }
    var selectedScheduleGroupId by remember { mutableStateOf(existingLocation?.linkedScheduleGroupId) }
    var activateOnEnter by remember { mutableStateOf(existingLocation?.activateScheduleOnEnter ?: false) }
    var deactivateOnExit by remember { mutableStateOf(existingLocation?.deactivateScheduleOnExit ?: false) }
    
    // 🆕 3차 고도화: 시간표 목록
    val scheduleGroups by scheduleViewModel.scheduleGroups.collectAsStateWithLifecycle()

    // 위치 검색 함수
    val performSearch: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            scope.launch {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingLocation != null) "위치 편집" else "위치 추가",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    LocationDialogStep.SEARCH -> {
                        // 단계 1: 위치 검색
                        LocationSearchStep(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            searchResults = searchResults,
                            isSearching = isSearching,
                            onSearch = performSearch,
                            onLocationSelected = { location ->
                                selectedLocation = location
                                label = location.name
                                currentStep = LocationDialogStep.SETTINGS
                            }
                        )
                    }
                    LocationDialogStep.SETTINGS -> {
                        // 단계 2: 상세 설정
                        LocationSettingsStep(
                            selectedLocation = selectedLocation,
                            existingLocation = existingLocation,
                            label = label,
                            onLabelChange = { label = it },
                            radiusMeters = radiusMeters,
                            onRadiusChange = { radiusMeters = it },
                            durationMinutes = durationMinutes,
                            onDurationChange = { durationMinutes = it },
                            selectedPreset = selectedPreset,
                            onPresetChange = { selectedPreset = it },
                            triggerType = triggerType,
                            onTriggerTypeChange = { triggerType = it },
                            dwellTimeMinutes = dwellTimeMinutes,
                            onDwellTimeChange = { dwellTimeMinutes = it },
                            requiresUserConfirmation = requiresUserConfirmation,
                            onRequiresConfirmationChange = { requiresUserConfirmation = it }
                        )
                    }
                    LocationDialogStep.SCHEDULE -> {
                        // 🆕 단계 3: 시간표 연결 (3차 고도화)
                        ScheduleLinkSettingsStep(
                            enableScheduleLink = enableScheduleLink,
                            onEnableScheduleLinkChange = { enableScheduleLink = it },
                            selectedScheduleGroupId = selectedScheduleGroupId,
                            onScheduleGroupIdChange = { selectedScheduleGroupId = it },
                            activateOnEnter = activateOnEnter,
                            onActivateOnEnterChange = { activateOnEnter = it },
                            deactivateOnExit = deactivateOnExit,
                            onDeactivateOnExitChange = { deactivateOnExit = it },
                            scheduleGroups = scheduleGroups
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 이전 버튼 (SEARCH 단계가 아니고, 편집 모드가 아닐 때만 표시)
                if (currentStep != LocationDialogStep.SEARCH && existingLocation == null) {
                    TextButton(
                        onClick = {
                            currentStep = when (currentStep) {
                                LocationDialogStep.SETTINGS -> LocationDialogStep.SEARCH
                                LocationDialogStep.SCHEDULE -> LocationDialogStep.SETTINGS
                                else -> currentStep
                            }
                        }
                    ) {
                        Text("이전")
                    }
                }
                
                // 다음/저장 버튼
                Button(
                    onClick = {
                        when (currentStep) {
                            LocationDialogStep.SEARCH -> {
                                // SEARCH → SETTINGS
                                currentStep = LocationDialogStep.SETTINGS
                            }
                            LocationDialogStep.SETTINGS -> {
                                // SETTINGS → SCHEDULE
                                currentStep = LocationDialogStep.SCHEDULE
                            }
                            LocationDialogStep.SCHEDULE -> {
                                // SCHEDULE → 저장
                                val location = LocationBasedAutoRun(
                                    id = existingLocation?.id ?: UUID.randomUUID().toString(),
                                    label = label.ifBlank { selectedLocation?.name ?: "위치" },
                                    address = existingLocation?.address ?: selectedLocation?.address,
                                    latitude = existingLocation?.latitude ?: selectedLocation?.latitude ?: 0.0,
                                    longitude = existingLocation?.longitude ?: selectedLocation?.longitude ?: 0.0,
                                    radiusMeters = radiusMeters,
                                    durationMinutes = durationMinutes,
                                    presetType = selectedPreset,
                                    triggerType = triggerType,
                                    dwellTimeMinutes = dwellTimeMinutes,
                                    requiresUserConfirmation = requiresUserConfirmation,
                                    isEnabled = existingLocation?.isEnabled ?: true,
                                    createdAt = existingLocation?.createdAt ?: System.currentTimeMillis(),
                                    // 🆕 3차 고도화: 시간표 연결 필드
                                    linkedScheduleGroupId = if (enableScheduleLink) selectedScheduleGroupId else null,
                                    activateScheduleOnEnter = enableScheduleLink && activateOnEnter,
                                    deactivateScheduleOnExit = enableScheduleLink && deactivateOnExit
                                )
                                onSave(location)
                                onDismiss()
                            }
                        }
                    },
                    enabled = when (currentStep) {
                        LocationDialogStep.SEARCH -> selectedLocation != null
                        LocationDialogStep.SETTINGS -> label.isNotBlank() && (selectedLocation != null || existingLocation != null)
                        LocationDialogStep.SCHEDULE -> !enableScheduleLink || selectedScheduleGroupId != null
                    }
                ) {
                    Text(
                        when (currentStep) {
                            LocationDialogStep.SCHEDULE -> "저장"
                            else -> "다음"
                        }
                    )
                }
            }
        },
        dismissButton = {
            if (currentStep != LocationDialogStep.SEARCH) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

/**
 * 위치 검색 단계
 */
@Composable
private fun LocationSearchStep(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<GeocoderUtils.LocationInfo>,
    isSearching: Boolean,
    onSearch: () -> Unit,
    onLocationSelected: (GeocoderUtils.LocationInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "주소 또는 장소 이름을 검색하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 검색 입력
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("검색") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        // 검색 결과
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (searchResults.isNotEmpty()) {
            Text(
                text = "검색 결과:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { location ->
                    LocationSearchResultCard(
                        location = location,
                        onClick = { onLocationSelected(location) }
                    )
                }
            }
        } else if (searchQuery.isNotBlank()) {
            Text(
                text = "검색 결과가 없습니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 위치 검색 결과 카드
 */
@Composable
private fun LocationSearchResultCard(
    location: GeocoderUtils.LocationInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = location.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 상세 설정 단계
 */
@Composable
private fun LocationSettingsStep(
    selectedLocation: GeocoderUtils.LocationInfo?,
    existingLocation: LocationBasedAutoRun?,
    label: String,
    onLabelChange: (String) -> Unit,
    radiusMeters: Int,
    onRadiusChange: (Int) -> Unit,
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    selectedPreset: String,
    onPresetChange: (String) -> Unit,
    triggerType: String,
    onTriggerTypeChange: (String) -> Unit,
    dwellTimeMinutes: Int,
    onDwellTimeChange: (Int) -> Unit,
    requiresUserConfirmation: Boolean,
    onRequiresConfirmationChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 선택된 위치 정보 표시
        if (selectedLocation != null || existingLocation != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📍 ${selectedLocation?.name ?: existingLocation?.label ?: ""}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedLocation?.address ?: existingLocation?.address ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 라벨 입력
        Text(
            text = "라벨",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("예: 회사, 도서관") },
            singleLine = true
        )

        // 반경 선택
        Text(
            text = "반경",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(50, 100, 200, 500).forEach { radius ->
                FilterChip(
                    selected = radiusMeters == radius,
                    onClick = { onRadiusChange(radius) },
                    label = { Text("${radius}m") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 타이머 시간 선택
        Text(
            text = "타이머 시간",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(25, 45, 60).forEach { duration ->
                FilterChip(
                    selected = durationMinutes == duration,
                    onClick = { onDurationChange(duration) },
                    label = { Text("${duration}분") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 차단 프리셋 선택
        Text(
            text = "차단 강도",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetOption(
                label = "완전 차단",
                description = "모든 앱 차단",
                selected = selectedPreset == "FULL_BLOCK",
                onClick = { onPresetChange("FULL_BLOCK") }
            )
            PresetOption(
                label = "표준 디톡시",
                description = "메신저만 허용",
                selected = selectedPreset == "STANDARD",
                onClick = { onPresetChange("STANDARD") }
            )
            PresetOption(
                label = "완화 모드",
                description = "SNS 일부 허용",
                selected = selectedPreset == "RELAXED",
                onClick = { onPresetChange("RELAXED") }
            )
        }

        HorizontalDivider()

        // 진입 조건 섹션
        Text(
            text = "진입 조건",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // 체류 시간 설정
        Text(
            text = "체류 시간",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "위치에 도착한 후 N분 체류 확인 시 자동 실행",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1, 3, 5).forEach { dwell ->
                FilterChip(
                    selected = dwellTimeMinutes == dwell,
                    onClick = { onDwellTimeChange(dwell) },
                    label = { Text(if (dwell == 0) "즉시" else "${dwell}분") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 도착 후 확인 토글
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = requiresUserConfirmation,
                    onValueChange = onRequiresConfirmationChange,
                    role = Role.Switch
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (requiresUserConfirmation) {
                    MaterialTheme.colorScheme.secondaryContainer
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
                        text = "도착 후 알림으로 확인",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "알림을 탭하면 타이머가 시작됩니다 (자동 시작 OFF)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = requiresUserConfirmation,
                    onCheckedChange = null // toggleable에서 처리
                )
            }
        }

        Text(
            text = "💡 위치 정확도가 낮을 때 유용합니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 프리셋 옵션
 */
@Composable
private fun PresetOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = null // Card의 clickable에서 처리
            )
        }
    }
}

