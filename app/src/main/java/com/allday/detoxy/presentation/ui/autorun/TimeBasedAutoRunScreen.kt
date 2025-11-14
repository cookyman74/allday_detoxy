package com.allday.detoxy.presentation.ui.autorun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.core.utils.ExactAlarmPermissionUtil
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import com.allday.detoxy.presentation.ui.autorun.components.*
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import com.allday.detoxy.presentation.viewmodel.TimeBasedAutoRunViewModel
import kotlinx.coroutines.launch

/**
 * 예약설정 화면 (시간 기반 자동 실행)
 *
 * 사용자가 특정 시간에 자동으로 타이머를 시작하도록 설정할 수 있는 화면입니다.
 * - 정확 알람 권한 경고 배너
 * - 템플릿 선택 버튼
 * - 예약 목록 (LazyColumn)
 * - 예약 추가 버튼
 * - 배터리 영향 안내 카드
 *
 * @param onBack 뒤로가기 콜백
 * @param onNavigateToLocationBased 위치 기반 자동 실행으로 이동 콜백
 * @param viewModel TimeBasedAutoRunViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBasedAutoRunScreen(
    onBack: () -> Unit = {},
    onNavigateToLocationBased: () -> Unit = {},
    onNavigateToScheduleGroup: () -> Unit = {},  // 🆕 3차 고도화: 시간표 관리로 이동
    scheduleGroupId: String? = null,  // 🆕 스케줄 그룹 ID (특정 그룹의 시간표 수정 시)
    scheduleGroupName: String? = null,  // 🆕 스케줄 그룹 이름 (타이틀 표시용)
    viewModel: TimeBasedAutoRunViewModel = hiltViewModel(),
    scheduleGroupViewModel: ScheduleGroupViewModel = hiltViewModel()  // 🆕 3.5차 고도화: 스케줄 그룹 생성
) {
    val allAutoRuns by viewModel.autoRuns.collectAsStateWithLifecycle()
    val canScheduleExactAlarms by viewModel.canScheduleExactAlarms.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    
    // 🆕 3차 고도화: 시간표 그룹 맵
    val scheduleGroupMap by viewModel.scheduleGroupMap.collectAsStateWithLifecycle()
    
    // 🐛 버그 수정: 위치기반 스케쥴 여부 확인
    val locationViewModel: com.allday.detoxy.presentation.viewmodel.LocationBasedAutoRunViewModel = hiltViewModel()
    val locations by locationViewModel.locations.collectAsStateWithLifecycle()
    var isLocationBasedState by remember(scheduleGroupId) { mutableStateOf(false) }
    
    // 🐛 버그 수정: 위치기반 여부 확인
    LaunchedEffect(scheduleGroupId, locations) {
        if (scheduleGroupId != null) {
            isLocationBasedState = locations.any { it.linkedScheduleGroupId == scheduleGroupId }
            android.util.Log.d("TimeBasedAutoRunScreen", "위치기반 여부 확인: scheduleGroupId=$scheduleGroupId, isLocationBased=$isLocationBasedState, locations=${locations.map { "${it.label}:${it.linkedScheduleGroupId}" }}")
        } else {
            isLocationBasedState = false
            android.util.Log.d("TimeBasedAutoRunScreen", "위치기반 여부 확인: scheduleGroupId=null, isLocationBased=false")
        }
    }
    
    // 🆕 v0.10.1: 특정 스케줄 그룹의 시간대만 필터링
    val autoRuns = remember(allAutoRuns, scheduleGroupId) {
        if (scheduleGroupId != null) {
            allAutoRuns.filter { it.scheduleGroupId == scheduleGroupId }
        } else {
            allAutoRuns
        }
    }
    
    // 글로벌 옵션 상태
    val excludeWeekends by viewModel.excludeWeekends.collectAsStateWithLifecycle()
    val autoStartDelayMinutes by viewModel.autoStartDelayMinutes.collectAsStateWithLifecycle()
    val preNotificationMinutes by viewModel.preNotificationMinutes.collectAsStateWithLifecycle()
    
    // 자동 실행 제어 상태
    val masterEnabled by viewModel.masterEnabled.collectAsStateWithLifecycle()
    val pauseUntil by viewModel.pauseUntil.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()  // 🆕 3.5차 고도화: Coroutine scope
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingAutoRun by remember { mutableStateOf<TimeBasedAutoRun?>(null) }
    var deletingAutoRunId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    // 🆕 스케줄 그룹 이름이 있으면 "{그룹명} 예약설정", 없으면 "예약설정"
                    Text(
                        text = if (scheduleGroupName != null) {
                            "$scheduleGroupName 예약설정"
                        } else {
                            "예약설정"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    // 🆕 3차 고도화: 시간표 관리 버튼
                    IconButton(onClick = onNavigateToScheduleGroup) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "시간표 관리"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (autoRuns.size < 10) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "시간대 추가"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 자동 실행 제어 카드 (MVP)
            AutoRunControlCard(
                masterEnabled = masterEnabled,
                pauseUntil = pauseUntil,
                onMasterEnabledChange = { viewModel.setMasterEnabled(it) },
                onPauseForHours = { viewModel.pauseForHours(it) },
                onPauseUntilMidnight = { viewModel.pauseUntilMidnight() },
                onResume = { viewModel.resumeAutoRun() }
            )
            
            // 2. 정확 알람 권한 경고 배너
            if (!canScheduleExactAlarms) {
                ExactAlarmPermissionWarningBanner(
                    onSettingsClick = {
                        val intent = ExactAlarmPermissionUtil.createSettingsIntentSafe(context)
                        context.startActivity(intent)
                    }
                )
            }

            // 3. 시간표 생성 버튼 (PRD §3.1: 템플릿 vs 커스텀 분기)
            if (autoRuns.isEmpty()) {
                // Empty State: 두 가지 시작 방법 제공
                EmptyStateWithOptions(
                    onTemplateClick = { 
                        showTemplateDialog = true
                    },
                    onCustomClick = {
                        showAddDialog = true
                    }
                )
            } else {
                // 시간대가 있을 때: 두 가지 추가 방법 제공
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 템플릿으로 추가
                    OutlinedButton(
                        onClick = { showTemplateDialog = true },
                        enabled = autoRuns.size < 10,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("템플릿")
                    }
                    
                    // 커스텀 추가
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        enabled = autoRuns.size < 10,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("커스텀")
                    }
                }
                
                if (autoRuns.size >= 10) {
                    Text(
                        text = "최대 10개까지 등록 가능합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 4. 시간대 리스트
            if (autoRuns.isNotEmpty()) {
                Text(
                    text = "등록된 시간대 (${autoRuns.size}/10)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                autoRuns.forEach { autoRun ->
                    // 🆕 3차 고도화: 연결된 시간표 그룹 조회
                    val scheduleGroup = autoRun.scheduleGroupId?.let { scheduleGroupMap[it] }
                    
                    TimeBasedAutoRunCard(
                        autoRun = autoRun,
                        scheduleGroup = scheduleGroup,  // 🆕 3차 고도화
                        onToggle = { id, enabled ->
                            viewModel.toggleAutoRun(id, enabled)
                        },
                        onEdit = { editingAutoRun = it },
                        onDelete = { id ->
                            deletingAutoRunId = id
                        }
                    )
                }
            }

            // 5. 글로벌 옵션 섹션
            GlobalOptionsSection(
                excludeWeekends = excludeWeekends,
                autoStartDelayMinutes = autoStartDelayMinutes,
                preNotificationMinutes = preNotificationMinutes,
                onExcludeWeekendsChange = { viewModel.setExcludeWeekends(it) },
                onAutoStartDelayChange = { viewModel.setAutoStartDelayMinutes(it) },
                onPreNotificationChange = { viewModel.setPreNotificationMinutes(it) }
            )

            // 6. 배터리 영향 안내 카드
            BatteryImpactInfoCard(
                impactLevel = "최소",
                estimatedPercentage = "< 1%/일",
                description = "시간 기반 자동 실행은 배터리에 거의 영향을 주지 않습니다."
            )
        }
    }

    // 시간대 추가 다이얼로그
    // 🆕 특정 스케줄 그룹 화면에서는 AddTimeBasedAutoRunDialog 사용
    // 🆕 일반 화면에서는 ScheduleCreationDialog 사용 (3.5차 고도화)
    if (showAddDialog) {
        if (scheduleGroupId != null) {
            // 특정 스케줄 그룹에 시간대 추가
            AddTimeBasedAutoRunDialog(
                existingAutoRun = null,
                onDismiss = { showAddDialog = false },
                onSave = { newAutoRun ->
                    scope.launch {
                        viewModel.addAutoRun(newAutoRun)
                        showAddDialog = false
                    }
                },
                scheduleViewModel = scheduleGroupViewModel,
                initialScheduleGroupId = scheduleGroupId,  // 🆕 특정 스케줄 그룹 ID 전달
                isLocationBased = isLocationBasedState  // 🐛 버그 수정: 위치기반 여부 전달
            )
        } else {
            // 일반 화면: 위치 설정 + 시간표 생성 (ScheduleCreationDialog 사용)
        ScheduleCreationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { scheduleName, mode, timeSlots, template, locationInfo ->
                scope.launch {
                    if (locationInfo == null) {
                        // 어디서나 적용 (위치 없음)
                        when (mode) {
                            CreationMode.TEMPLATE -> {
                                scheduleGroupViewModel.createFromTemplate(scheduleName, template!!)
                            }
                            CreationMode.CUSTOM -> {
                                scheduleGroupViewModel.createScheduleGroupWithTimeSlots(
                                    name = scheduleName,
                                    description = null,
                                    timeSlots = timeSlots
                                )
                            }
                        }
                        snackbarHostState.showSnackbar(
                            message = "시간표가 생성되었습니다 (어디서나 적용)",
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        // 위치 기반 스케줄
                        val scheduleGroupId = when (mode) {
                            CreationMode.TEMPLATE -> {
                                scheduleGroupViewModel.createFromTemplate(scheduleName, template!!)
                            }
                            CreationMode.CUSTOM -> {
                                scheduleGroupViewModel.createScheduleGroupWithTimeSlots(
                                    name = scheduleName,
                                    description = null,
                                    timeSlots = timeSlots
                                )
                            }
                        }
                        
                        // TODO: 위치 정보를 LocationBasedAutoRun으로 저장
                        // locationViewModel.createLocationWithSchedule(locationInfo, scheduleGroupId)
                        
                        snackbarHostState.showSnackbar(
                            message = "위치 기반 시간표가 생성되었습니다",
                            duration = SnackbarDuration.Short
                        )
                    }
                    showAddDialog = false
                }
            },
            initialMode = CreationMode.CUSTOM  // 🔑 커스텀 모드로 시작
        )
        }
    }

    // 시간대 편집 다이얼로그
    editingAutoRun?.let { autoRun ->
        AddTimeBasedAutoRunDialog(
            existingAutoRun = autoRun,
            onDismiss = { editingAutoRun = null },
            onSave = { updatedAutoRun ->
                viewModel.updateAutoRun(updatedAutoRun)
                editingAutoRun = null
            },
            isLocationBased = isLocationBasedState  // 🐛 버그 수정: 위치기반 여부 전달
        )
    }

    // 🆕 3.5차 고도화: 템플릿으로 시간표 생성 (ScheduleCreationDialog 사용)
    if (showTemplateDialog) {
        ScheduleCreationDialog(
            onDismiss = { showTemplateDialog = false },
            onConfirm = { scheduleName, mode, timeSlots, template, locationInfo ->
                scope.launch {
                    if (locationInfo == null) {
                        // 어디서나 적용 (위치 없음)
                        when (mode) {
                            CreationMode.TEMPLATE -> {
                                scheduleGroupViewModel.createFromTemplate(scheduleName, template!!)
                            }
                            CreationMode.CUSTOM -> {
                                scheduleGroupViewModel.createScheduleGroupWithTimeSlots(
                                    name = scheduleName,
                                    description = null,
                                    timeSlots = timeSlots
                                )
                            }
                        }
                        snackbarHostState.showSnackbar(
                            message = "시간표가 생성되었습니다 (어디서나 적용)",
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        // 위치 기반 스케줄
                        val scheduleGroupId = when (mode) {
                            CreationMode.TEMPLATE -> {
                                scheduleGroupViewModel.createFromTemplate(scheduleName, template!!)
                            }
                            CreationMode.CUSTOM -> {
                                scheduleGroupViewModel.createScheduleGroupWithTimeSlots(
                                    name = scheduleName,
                                    description = null,
                                    timeSlots = timeSlots
                                )
                            }
                        }
                        
                        // TODO: 위치 정보를 LocationBasedAutoRun으로 저장
                        // locationViewModel.createLocationWithSchedule(locationInfo, scheduleGroupId)
                        
                        snackbarHostState.showSnackbar(
                            message = "위치 기반 시간표가 생성되었습니다",
                            duration = SnackbarDuration.Short
                        )
                    }
                    showTemplateDialog = false
                }
            },
            initialMode = CreationMode.TEMPLATE  // 🔑 템플릿 모드로 시작
        )
    }

    // 삭제 확인 다이얼로그
    deletingAutoRunId?.let { id ->
        val autoRun = autoRuns.find { it.id == id }
        AlertDialog(
            onDismissRequest = { deletingAutoRunId = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            title = { Text("삭제 확인") },
            text = {
                Text(
                    if (autoRun != null) {
                        val timeString = String.format("%02d:%02d", autoRun.hour, autoRun.minute)
                        "\"$timeString\" 자동 실행을 삭제하시겠습니까?\n알람도 함께 취소됩니다."
                    } else {
                        "이 자동 실행을 삭제하시겠습니까?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAutoRun(id)
                        deletingAutoRunId = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAutoRunId = null }) {
                    Text("취소")
                }
            }
        )
    }
}

