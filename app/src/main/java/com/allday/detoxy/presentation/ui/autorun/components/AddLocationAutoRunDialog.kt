package com.allday.detoxy.presentation.ui.autorun.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Place
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
import com.allday.detoxy.core.utils.LocationUtils
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import com.google.android.gms.location.LocationServices
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
    
    // 🆕 3차 고도화 개선: 빠른 시간표 생성
    var showQuickCreateDialog by remember { mutableStateOf(false) }

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
                        // 단계 1: 위치 검색
                        // 🆕 현재 위치 찾기 로직
                        var isLocating by remember { mutableStateOf(false) }
                        
                        val fusedLocationClient = remember { 
                            LocationServices.getFusedLocationProviderClient(context) 
                        }

                        val requestPermissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestMultiplePermissions()
                        ) { permissions ->
                            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                            if (isGranted) {
                                // 권한 허용됨 -> 위치 조회 시작
                                scope.launch {
                                    isLocating = true
                                    val result = LocationUtils.getCurrentLocation(fusedLocationClient)
                                    result.onSuccess { location ->
                                        // 좌표 -> 주소 변환
                                        val addressResult = GeocoderUtils.getAddressFromCoordinates(
                                            context, location.latitude, location.longitude
                                        )
                                        addressResult.onSuccess { info ->
                                            val infoWithAccuracy = info.copy(accuracy = location.accuracy)
                                            // 결과 리스트에 현재 위치 추가 (최상단)
                                            searchResults = listOf(infoWithAccuracy) + searchResults
                                            searchQuery = info.address // 주소 자동 입력
                                        }.onFailure {
                                            // 주소 변환 실패해도 좌표만으로 등록 가능하게 처리 (TODO: Fallback UI)
                                            Toast.makeText(context, "주소를 가져오지 못했지만 좌표를 등록합니다.", Toast.LENGTH_SHORT).show()
                                            val fallbackInfo = GeocoderUtils.LocationInfo(
                                                name = "현재 위치",
                                                address = "위도: ${location.latitude}, 경도: ${location.longitude}",
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                accuracy = location.accuracy
                                            )
                                            searchResults = listOf(fallbackInfo) + searchResults
                                        }
                                    }.onFailure {
                                        Toast.makeText(context, "위치를 찾을 수 없습니다. GPS 설정을 확인해주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                    isLocating = false
                                }
                            } else {
                                Toast.makeText(context, "현재 위치를 찾으려면 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        LocationSearchContent(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            searchResults = searchResults,
                            isSearching = isSearching,
                            isLocatingCurrentPosition = isLocating,
                            onSearch = performSearch,
                            onCurrentLocationClick = {
                                requestPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
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
                            scheduleGroups = scheduleGroups,
                            onCreateNewSchedule = { showQuickCreateDialog = true }
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
    
    // 🆕 3.5차 고도화 Phase 1: 시간대 포함 시간표 생성
    if (showQuickCreateDialog) {
        QuickCreateScheduleDialog(
            onDismiss = { showQuickCreateDialog = false },
            onConfirm = { scheduleName, mode, data ->
                // 시간표 생성 (모드에 따라 분기)
                scope.launch {
                    val newGroupId = when (mode) {
                        com.allday.detoxy.domain.model.CreationMode.TEMPLATE -> {
                            // 📋 템플릿으로 생성 (Phase 2)
                            @Suppress("UNCHECKED_CAST")
                            val template = data as com.allday.detoxy.domain.model.ScheduleTemplate
                            scheduleViewModel.createFromTemplate(scheduleName, template, isLocationBased = true)  // 🐛 버그 수정: 위치기반 스케쥴
                        }
                        com.allday.detoxy.domain.model.CreationMode.CUSTOM -> {
                            // ✏️ 커스텀 시간대로 생성
                            @Suppress("UNCHECKED_CAST")
                            val timeSlots = data as List<com.allday.detoxy.domain.model.TimeSlot>
                            scheduleViewModel.createScheduleGroupWithTimeSlots(
                                name = scheduleName,
                                description = null,
                                timeSlots = timeSlots,
                                isLocationBased = true  // 🐛 버그 수정: 위치기반 스케쥴은 위치 진입 시 활성화
                            )
                        }
                    }
                    
                    // 생성된 시간표 자동 선택
                    selectedScheduleGroupId = newGroupId
                    enableScheduleLink = true
                    activateOnEnter = true  // 기본값: 도착 시 활성화
                    deactivateOnExit = true  // 기본값: 이탈 시 비활성화
                    
                    showQuickCreateDialog = false
                }
            },
            locationLabel = label
        )
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
                    
                    // 🆕 2.3 정확도 및 지도 확인 UI
                    val context = LocalContext.current // To launch intent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 정확도 (새로 검색된 위치인 경우에만 표시)
                        selectedLocation?.accuracy?.let { accuracy ->
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        text = "오차 ±${accuracy.toInt()}m",
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // 지도에서 확인 버튼
                        val lat = selectedLocation?.latitude ?: existingLocation?.latitude
                        val lng = selectedLocation?.longitude ?: existingLocation?.longitude
                        
                        if (lat != null && lng != null) {
                            TextButton(
                                onClick = {
                                    try {
                                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label.ifBlank { "위치" })})")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "지도 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "지도에서 확인",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
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

