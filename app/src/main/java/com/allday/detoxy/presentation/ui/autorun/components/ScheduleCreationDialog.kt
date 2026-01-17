package com.allday.detoxy.presentation.ui.autorun.components

import android.Manifest // Added
import android.content.Intent // Added
import android.net.Uri // Added
import android.util.Log
import android.widget.Toast // Added
import androidx.activity.compose.rememberLauncherForActivityResult // Added
import androidx.activity.result.contract.ActivityResultContracts // Added
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.R
import com.allday.detoxy.presentation.ui.component.GlassDialog
import com.allday.detoxy.core.utils.GeocoderUtils
import com.allday.detoxy.core.utils.LocationUtils // Added
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import com.google.android.gms.location.LocationServices // Added
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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

    // 🆕 Shared Location Search State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocoderUtils.LocationInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
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
            scope.launch {
                isLocating = true
                val result = LocationUtils.getCurrentLocation(fusedLocationClient)
                result.onSuccess { location ->
                    val addressResult = GeocoderUtils.getAddressFromCoordinates(
                        context, location.latitude, location.longitude
                    )
                    addressResult.onSuccess { info ->
                        val infoWithAccuracy = info.copy(accuracy = location.accuracy)
                        searchResults = listOf(infoWithAccuracy) + searchResults
                        searchQuery = info.address
                    }.onFailure {
                        Toast.makeText(context, context.getString(R.string.toast_address_fallback), Toast.LENGTH_SHORT).show()
                        val fallbackInfo = GeocoderUtils.LocationInfo(
                            name = context.getString(R.string.location_current),
                            address = context.getString(R.string.location_lat_lng_format, location.latitude, location.longitude),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                        searchResults = listOf(fallbackInfo) + searchResults
                    }
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.toast_location_not_found), Toast.LENGTH_SHORT).show()
                }
                isLocating = false
            }
        } else {
            Toast.makeText(context, context.getString(R.string.toast_location_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    // 검색 함수
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
    
    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        // 내부 검색 모드인 경우 (showLocationSearch가 true인 경우)
        if (showLocationSearch) {
            Box(modifier = Modifier.padding(24.dp)) {
                // 🆕 Use Shared Component
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.location_search_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
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
                            showLocationSearch = false
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLocationSearch = false }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                }
            }
        } else {
            // 기본 단계 진행 화면
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    // .fillMaxSize() 제거: 다이얼로그 크기를 컨텐츠에 맞게
                    .wrapContentHeight(), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (currentStep) {
                        ScheduleCreationStep.LOCATION_CHOICE -> stringResource(R.string.schedule_creation_location_choice)
                        ScheduleCreationStep.LOCATION_SEARCH -> stringResource(R.string.schedule_creation_location_radius)
                        ScheduleCreationStep.SCHEDULE_SETUP -> stringResource(R.string.schedule_creation_create)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // 스크롤 가능한 컨텐츠 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                         // weight(1f) 제거 -> 고정 높이가 아닌 내용물 크기만큼
                        .heightIn(max = 400.dp) // 최대 높이 제한 (스크롤 생김)
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
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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
                        val btnText = if (currentStep == ScheduleCreationStep.LOCATION_CHOICE) 
                            stringResource(R.string.btn_cancel) 
                        else 
                            stringResource(R.string.nav_previous)
                        Text(btnText)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            when (currentStep) {
                                ScheduleCreationStep.LOCATION_CHOICE -> {
                                    if (hasLocation) {
                                        currentStep = ScheduleCreationStep.LOCATION_SEARCH
                                    } else {
                                        currentStep = ScheduleCreationStep.SCHEDULE_SETUP
                                    }
                                }
                                ScheduleCreationStep.LOCATION_SEARCH -> {
                                    if (selectedLocation != null) {
                                        if (mode == CreationMode.CUSTOM) {
                                            mode = CreationMode.TEMPLATE
                                        }
                                        currentStep = ScheduleCreationStep.SCHEDULE_SETUP
                                    }
                                }
                                ScheduleCreationStep.SCHEDULE_SETUP -> {
                                    val locationInfo = if (hasLocation && selectedLocation != null) {
                                        LocationInfo(
                                            name = selectedLocation!!.name,
                                            address = selectedLocation!!.address,
                                            latitude = selectedLocation!!.latitude,
                                            longitude = selectedLocation!!.longitude,
                                            radiusMeters = radiusMeters
                                        )
                                    } else {
                                        null
                                    }
                                    
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
                                ScheduleCreationStep.LOCATION_CHOICE -> stringResource(R.string.nav_next)
                                ScheduleCreationStep.LOCATION_SEARCH -> stringResource(R.string.nav_next)
                                ScheduleCreationStep.SCHEDULE_SETUP -> stringResource(R.string.btn_create)
                            }
                        )
                    }
                }
            }
        }
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
            text = stringResource(R.string.schedule_creation_location_question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 어디서나 적용
        RadioButtonOption(
            selected = !hasLocation,
            onClick = { onHasLocationChange(false) },
            title = stringResource(R.string.schedule_creation_anywhere),
            description = stringResource(R.string.schedule_creation_anywhere_desc)
        )
        
        // 특정 위치에서만
        RadioButtonOption(
            selected = hasLocation,
            onClick = { onHasLocationChange(true) },
            title = stringResource(R.string.schedule_creation_location_only),
            description = stringResource(R.string.schedule_creation_location_only_desc)
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
            text = stringResource(R.string.location_select_title),
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
                Text(stringResource(R.string.location_search_placeholder))
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            Icon(Icons.Default.Edit, stringResource(R.string.location_change))
                        }
                    }

                    // 🆕 지도에서 확인 & 정확도 표시 (LocationSettingsStep과 유사하게 추가)
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                         selectedLocation.accuracy?.let { accuracy ->
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        text = stringResource(R.string.location_accuracy_format, accuracy.toInt()),
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = {
                                try {
                                    val lat = selectedLocation.latitude
                                    val lng = selectedLocation.longitude
                                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(selectedLocation.name)})")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.toast_map_app_not_found), Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place, // Use Place as Map might be unavailable
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.location_open_map),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
        
        // 반경 설정
        if (selectedLocation != null) {
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.location_radius_format, radiusMeters),
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
                text = stringResource(R.string.location_radius_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Step 3: 시간표 설정 (unchanged)
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
            label = { Text(stringResource(R.string.schedule_name_label)) },
            placeholder = { Text(stringResource(R.string.schedule_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        HorizontalDivider()
        
        // 생성 방식 선택
        Text(
            text = stringResource(R.string.schedule_creation_method),
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
                        Icon(Icons.AutoMirrored.Filled.List, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.schedule_creation_template_select))
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
                    text = stringResource(R.string.schedule_creation_timeslot_list),
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
                            text = stringResource(R.string.schedule_creation_add_timeslot),
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
                    Text(stringResource(R.string.schedule_creation_add_timeslot_btn))
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
