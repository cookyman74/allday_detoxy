package com.allday.detoxy.presentation.ui.autorun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.PauseDuration
import com.allday.detoxy.domain.model.ScheduleGroupControlState
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import android.util.Log
import com.allday.detoxy.presentation.ui.autorun.components.AddScheduleGroupDialog
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleCreationDialog
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleGroupCard
import com.allday.detoxy.presentation.ui.autorun.components.LocationEditDialog  // 🆕
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import com.allday.detoxy.presentation.viewmodel.LocationBasedAutoRunViewModel  // 🆕
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun  // 🆕
import kotlinx.coroutines.launch

/**
 * 스케줄 그룹 관리 화면
 *
 * 여러 시간 기반 자동 실행을 그룹으로 묶어 위치에 따라 자동으로 활성화/비활성화할 수 있는 화면입니다.
 *
 * ## 주요 기능
 * - ScheduleGroup 목록 표시
 * - ScheduleGroup 생성/수정/삭제
 * - ScheduleGroup 활성화/비활성화
 * - 연결된 시간표 및 위치 개수 표시
 * - 시간표 관리 버튼을 통해 TimeBasedAutoRunScreen으로 이동
 *
 * @param onBack 뒤로가기 콜백
 * @param onNavigateToTimeBasedAutoRun 시간 기반 자동 실행 화면으로 이동 콜백
 * @param viewModel ScheduleGroupViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGroupScreen(
    onBack: () -> Unit = {},
    onNavigateToTimeBasedAutoRun: (scheduleGroupId: String, scheduleGroupName: String) -> Unit = { _, _ -> },  // 🆕 스케줄 그룹 정보 전달
    initialScrollToGroupId: String? = null, // 🆕 초기 스크롤 위치 (특정 스케줄 그룹 ID)
    viewModel: ScheduleGroupViewModel = hiltViewModel(),
    locationViewModel: LocationBasedAutoRunViewModel = hiltViewModel()  // 🆕
) {
    val scheduleGroups by viewModel.scheduleGroups.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val linkedTimeBasedAutoRuns by viewModel.linkedTimeBasedAutoRuns.collectAsStateWithLifecycle()
    val linkedLocations by viewModel.linkedLocations.collectAsStateWithLifecycle()  // 🆕
    val linkedLocationCounts by viewModel.linkedLocationCounts.collectAsStateWithLifecycle()
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState() // 🆕 스크롤 상태 관리
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<ScheduleGroup?>(null) }
    var deletingGroupId by remember { mutableStateOf<String?>(null) }
    
    // 🆕 위치 정보 수정 다이얼로그 상태
    var showLocationEditDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<LocationBasedAutoRun?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 화면 초기 로딩 시 모든 그룹의 연결된 설정 개수 로드
    LaunchedEffect(scheduleGroups) {
        if (scheduleGroups.isNotEmpty()) {
            viewModel.loadAllLinkedCounts()
            
            // 🆕 초기 진입 시 특정 그룹으로 스크롤 이동
            if (initialScrollToGroupId != null) {
                val index = scheduleGroups.indexOfFirst { it.id == initialScrollToGroupId }
                if (index >= 0) {
                    // 🆕 인덱스 보정: LazyColumn에 "안내 카드"가 0번째 항목으로 추가되어 있으므로 index + 1 필요
                    val adjustedIndex = index + 1
                    Log.d("ScheduleGroupScreen", "📜 Scrolling to group: $initialScrollToGroupId (original: $index, adjusted: $adjustedIndex)")
                    listState.animateScrollToItem(adjustedIndex)
                }
            }
        }
    }
    
    // 에러 표시
    LaunchedEffect(errorState) {
        errorState?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    // 삭제 확인 다이얼로그
    if (deletingGroupId != null) {
        AlertDialog(
            onDismissRequest = { deletingGroupId = null },
            title = { Text("스케줄 그룹 삭제") },
            text = {
                Column {
                    Text("이 스케줄 그룹을 삭제하시겠습니까?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "연결된 시간표와 위치의 참조는 해제되지만, 설정 자체는 삭제되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingGroupId?.let { viewModel.deleteScheduleGroup(it) }
                        deletingGroupId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroupId = null }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 추가 다이얼로그 (3.5차 고도화: ScheduleCreationDialog 사용 - 위치 설정 포함)
    if (showAddDialog) {
        ScheduleCreationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { scheduleName, mode, timeSlots, template, locationInfo ->
                scope.launch {
                    if (locationInfo == null) {
                        // 어디서나 적용 (위치 없음) - 즉시 활성화
                        when (mode) {
                            CreationMode.TEMPLATE -> {
                                viewModel.createFromTemplate(scheduleName, template!!, isLocationBased = false)
                            }
                            CreationMode.CUSTOM -> {
                                viewModel.createScheduleGroupWithTimeSlots(
                                    name = scheduleName,
                                    description = null,
                                    timeSlots = timeSlots,
                                    isLocationBased = false  // 🐛 버그 수정: 일반 시간 스케쥴은 즉시 활성화
                                )
                            }
                        }
                    } else {
                        // 위치 기반 스케줄 - 위치 진입 시 활성화
                        val scheduleGroupId = when (mode) {
                            CreationMode.TEMPLATE -> {
                                viewModel.createFromTemplate(scheduleName, template!!, isLocationBased = true)
                            }
                            CreationMode.CUSTOM -> {
                                viewModel.createScheduleGroupWithTimeSlots(
                                    name = scheduleName,
                                    description = null,
                                    timeSlots = timeSlots,
                                    isLocationBased = true  // 🐛 버그 수정: 위치기반 스케쥴은 위치 진입 시 활성화
                                )
                            }
                        }
                        
                        // 🆕 위치 정보를 LocationBasedAutoRun으로 저장
                        val location = LocationBasedAutoRun(
                            label = locationInfo.name,  // 🆕 name 필드 사용
                            address = locationInfo.address,
                            latitude = locationInfo.latitude,
                            longitude = locationInfo.longitude,
                            radiusMeters = locationInfo.radiusMeters,
                            durationMinutes = 90,  // 기본값 (ScheduleGroup 연결 시 무시됨)
                            presetType = "STANDARD",  // 기본값 (ScheduleGroup 연결 시 무시됨)
                            triggerType = "ENTER",  // ScheduleGroup 연결 시 activateScheduleOnEnter로 제어
                            linkedScheduleGroupId = scheduleGroupId,
                            activateScheduleOnEnter = true,
                            deactivateScheduleOnExit = true,
                            isEnabled = true
                        )
                        
                        // 위치 정보 저장 (완료까지 대기)
                        locationViewModel.addLocation(location)
                        
                        // 🆕 저장 완료 후 해당 그룹의 위치 정보 갱신
                        viewModel.loadLinkedLocations(scheduleGroupId)
                        
                        Log.d("ScheduleGroupScreen", "✅ Location saved: ${location.label} (${location.address}) → ScheduleGroup: $scheduleGroupId")
                    }
                    showAddDialog = false
                }
            },
            initialMode = CreationMode.TEMPLATE  // 🐛 버그 수정: 위치기반 스케쥴 생성 시 템플릿이 기본값
        )
    }
    
    // 편집 다이얼로그 (이름/설명만 수정)
    if (editingGroup != null) {
        AddScheduleGroupDialog(
            onDismiss = { editingGroup = null },
            onConfirm = { name, description ->
                viewModel.updateScheduleGroup(
                    editingGroup!!.copy(
                        name = name,
                        description = description
                    )
                )
                editingGroup = null
            },
            existingGroup = editingGroup
        )
    }
    
    // 🆕 위치 정보 수정 다이얼로그
    if (showLocationEditDialog && editingLocation != null) {
        LocationEditDialog(
            location = editingLocation!!,
            onDismiss = {
                showLocationEditDialog = false
                editingLocation = null
            },
            onSave = { updatedLocation ->
                Log.d("ScheduleGroupScreen", "📍 onSave called for location: ${updatedLocation.label}")
                
                // 🆕 즉시 다이얼로그 닫기 (비동기 작업 전)
                showLocationEditDialog = false
                editingLocation = null
                
                // 비동기 작업 시작
                scope.launch {
                    try {
                        // 1. 🆕 LocationBasedAutoRun 업데이트 (완료될 때까지 대기)
                        locationViewModel.updateLocation(updatedLocation)
                        Log.d("ScheduleGroupScreen", "✅ Location updated successfully")
                        
                        // 2. 🆕 연결된 스케줄 그룹의 위치 정보 갱신 (업데이트 완료 후!)
                        updatedLocation.linkedScheduleGroupId?.let { groupId ->
                            viewModel.loadLinkedLocations(groupId)
                            Log.d("ScheduleGroupScreen", "✅ Linked locations reloaded")
                        }
                        
                        // 3. 성공 메시지 표시
                        snackbarHostState.showSnackbar(
                            message = "위치 정보가 수정되었습니다",
                            duration = SnackbarDuration.Short
                        )
                    } catch (e: Exception) {
                        // 오류 발생 시 오류 메시지 표시
                        snackbarHostState.showSnackbar(
                            message = "위치 정보 수정 실패: ${e.message}",
                            duration = SnackbarDuration.Long
                        )
                        Log.e("ScheduleGroupScreen", "❌ Failed to update location", e)
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("스케줄 그룹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (scheduleGroups.size < 10) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "스케줄 그룹 추가"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && scheduleGroups.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (scheduleGroups.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "스케줄 그룹이 없습니다",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "여러 시간표를 그룹으로 묶어\n위치에 따라 자동으로 활성화할 수 있습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("첫 그룹 만들기")
                    }
                }
            } else {
                LazyColumn(
                    state = listState, // 🆕 스크롤 상태 연결
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 🆕 3.5차 고도화: 다중 활성화 지원 - 각 카드에 활성화 상태 표시
                    // (이전의 "현재 활성화된 시간표" 카드는 단일 활성화 가정으로 제거)
                    
                    // 안내 카드
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "💡 스케줄 그룹 사용 방법",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "1. 스케줄 그룹을 만듭니다\n" +
                                            "2. 시간 기반 자동 실행 설정에서 그룹을 선택합니다\n" +
                                            "3. 위치 기반 자동 실행에서 그룹을 연결합니다\n" +
                                            "4. 해당 위치에 진입하면 그룹의 시간표가 자동으로 활성화됩니다",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // ScheduleGroup 목록
                    items(
                        items = scheduleGroups,
                        key = { it.id }
                    ) { group ->
                        // 🆕 3차 고도화 개선: ViewModel에서 데이터를 미리 준비해서 전달
                        val timeBasedAutoRunsList = linkedTimeBasedAutoRuns[group.id] ?: emptyList()
                        val linkedLocationsList = linkedLocations[group.id] ?: emptyList()  // 🆕
                        val locationCount = linkedLocationCounts[group.id] ?: 0
                        
                        // v8: controlState 계산
                        val controlState = ScheduleGroupControlState.fromEntity(
                            manualOverrideState = group.manualOverrideState,
                            pauseUntil = group.pauseUntil
                        )
                        
                        ScheduleGroupCard(
                            group = group,
                            controlState = controlState,  // v8: 통합 제어 상태
                            pauseUntil = group.pauseUntil,  // v8: 일시중지 해제 시각
                            timeBasedAutoRuns = timeBasedAutoRunsList,
                            linkedLocations = linkedLocationsList,
                            linkedLocationCount = locationCount,
                            onStateChange = { newState ->
                                // v8: 상태 변경 처리
                                viewModel.changeControlState(group.id, newState)
                            },
                            onPause = { duration ->
                                // v8: 일시중지 처리
                                viewModel.pauseScheduleGroup(group.id, duration)
                            },
                            onEdit = {
                                editingGroup = group
                            },
                            onDelete = {
                                deletingGroupId = group.id
                            },
                            onLocationClick = {
                                // 위치 정보 수정 다이얼로그 표시
                                val location = linkedLocationsList.firstOrNull()
                                if (location != null) {
                                    editingLocation = location
                                    showLocationEditDialog = true
                                }
                            },
                            onTimeClick = {
                                // 스케줄 그룹 정보와 함께 TimeBasedAutoRunScreen으로 이동
                                Log.d("ScheduleGroupScreen", "Time clicked: ${group.name} (ID: ${group.id})")
                                onNavigateToTimeBasedAutoRun(group.id, group.name)
                            }
                        )
                    }
                    
                    // 하단 여백 (FAB 가리지 않도록)
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

