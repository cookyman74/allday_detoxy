package com.allday.detoxy.presentation.ui.schedule

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.util.Log
import com.allday.detoxy.presentation.ui.component.GlassScaffold
import com.allday.detoxy.presentation.ui.component.GlassSurface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleCreationDialog
import com.allday.detoxy.presentation.ui.autorun.components.LocationInfo
import com.allday.detoxy.presentation.ui.autorun.components.BackgroundLocationRationaleDialog
import com.allday.detoxy.presentation.ui.autorun.components.LocationPermissionDeniedDialog
import com.allday.detoxy.presentation.ui.autorun.components.OpenSettingsDialog
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import com.allday.detoxy.presentation.viewmodel.LocationBasedAutoRunViewModel
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.core.utils.PermissionUtils
import kotlinx.coroutines.launch

/**
 * 스케줄 탭 화면 (v0.10 UI/UX 개선)
 *
 * 하단 네비게이션의 "스케줄" 탭에서 표시되는 메인 화면입니다.
 * 모든 스케줄 그룹을 한눈에 보여주고, 활성 스케줄 및 다음 예약 정보를 제공합니다.
 *
 * ## 주요 기능
 * - 활성 스케줄 요약 카드 표시
 * - 다음 예약 요약 카드 표시
 * - 모든 스케줄 그룹 리스트 표시
 * - FAB를 통한 빠른 스케줄 생성
 * - 스케줄 카드 클릭 시 상세 화면으로 이동
 *
 * @param onNavigateToDetail 스케줄 상세 화면으로 이동하는 콜백 (groupId 전달)
 * @param viewModel ScheduleGroupViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTabScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ScheduleGroupViewModel = hiltViewModel(),
    locationViewModel: LocationBasedAutoRunViewModel = hiltViewModel()
) {
    val scheduleGroups by viewModel.scheduleGroups.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()  // 🆕 v0.10.1: 복수형으로 변경
    val linkedTimeBasedAutoRuns by viewModel.linkedTimeBasedAutoRuns.collectAsState()
    val linkedLocationCounts by viewModel.linkedLocationCounts.collectAsState()
    val linkedLocations by viewModel.linkedLocations.collectAsState()  // 🆕 v0.10.1
    
    // 🆕 v0.10.1: 타이머 작동 상태 및 작동 중인 스케줄 그룹
    val timerState by com.allday.detoxy.service.timer.FocusTimerService.state.collectAsState()
    val runningScheduleGroupId by com.allday.detoxy.service.timer.FocusTimerService.currentScheduleGroupId.collectAsState()
    val runningScheduleGroup = scheduleGroups.find { it.id == runningScheduleGroupId }
    val remainingSeconds by com.allday.detoxy.service.timer.FocusTimerService.remainingSeconds.collectAsState()
    
    // 🆕 위치 권한 상태
    val locationPermissionGranted by locationViewModel.locationPermissionGranted.collectAsState()
    val backgroundLocationPermissionGranted by locationViewModel.backgroundLocationPermissionGranted.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 🆕 권한 요청 관련 상태
    var pendingLocationInfo by remember { mutableStateOf<LocationInfo?>(null) }
    var pendingScheduleName by remember { mutableStateOf<String?>(null) }
    var pendingMode by remember { mutableStateOf<CreationMode?>(null) }
    var pendingTimeSlots by remember { mutableStateOf<List<TimeSlot>?>(null) }
    var pendingTemplate by remember { mutableStateOf<ScheduleTemplate?>(null) }
    var showBackgroundLocationRationaleDialog by remember { mutableStateOf(false) }
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    var showOpenSettingsDialog by remember { mutableStateOf(false) }
    
    // 🆕 Pending 데이터 초기화 (함수들을 먼저 정의)
    val clearPendingData: () -> Unit = {
        pendingLocationInfo = null
        pendingScheduleName = null
        pendingMode = null
        pendingTimeSlots = null
        pendingTemplate = null
    }
    
    // 🆕 스케줄 생성 진행
    val proceedWithScheduleCreation: () -> Unit = {
        val locationInfo = pendingLocationInfo
        val scheduleName = pendingScheduleName
        val mode = pendingMode
        val timeSlots = pendingTimeSlots
        val template = pendingTemplate
        
        if (locationInfo != null && scheduleName != null && mode != null && timeSlots != null) {
            scope.launch {
                try {
                    Log.d("ScheduleTabScreen", "🔵 Creating location-based schedule: $scheduleName")
                    Log.d("ScheduleTabScreen", "📍 Location: ${locationInfo.name} (${locationInfo.address})")
                    
                    val scheduleGroupId = when (mode) {
                        CreationMode.TEMPLATE -> {
                            if (template == null) {
                                Log.e("ScheduleTabScreen", "❌ ERROR: template is null but mode is TEMPLATE!")
                                return@launch
                            }
                            viewModel.createFromTemplate(scheduleName, template, isLocationBased = true)  // 🐛 버그 수정: 위치기반 스케쥴
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
                    
                    Log.d("ScheduleTabScreen", "✅ ScheduleGroup created: $scheduleGroupId")
                    
                    // 위치 정보를 LocationBasedAutoRun으로 저장
                    val location = LocationBasedAutoRun(
                        label = locationInfo.name,
                        address = locationInfo.address,
                        latitude = locationInfo.latitude,
                        longitude = locationInfo.longitude,
                        radiusMeters = locationInfo.radiusMeters,
                        durationMinutes = 90,
                        presetType = "STANDARD",
                        triggerType = "ENTER",
                        linkedScheduleGroupId = scheduleGroupId,
                        activateScheduleOnEnter = true,
                        deactivateScheduleOnExit = true,
                        isEnabled = true
                    )
                    
                    Log.d("ScheduleTabScreen", "💾 Saving location: ${location.label} → $scheduleGroupId")
                    
                    // 위치 정보 저장
                    locationViewModel.addLocation(location)
                    
                    Log.d("ScheduleTabScreen", "✅ Location-based schedule created successfully")
                    
                    // 저장 확인
                    kotlinx.coroutines.delay(500)
                    viewModel.loadLinkedLocations(scheduleGroupId)
                    viewModel.loadAllLinkedCounts()
                    
                    // Pending 데이터 초기화
                    clearPendingData()
                    
                } catch (e: Exception) {
                    Log.e("ScheduleTabScreen", "❌ Exception in proceedWithScheduleCreation: ${e.message}", e)
                    clearPendingData()
                }
            }
        }
    }
    
    // 🆕 위치 권한 요청 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("ScheduleTabScreen", "✅ Location permission granted")
            locationViewModel.checkPermissions()
            // 백그라운드 위치 권한 설명 다이얼로그 표시
            showBackgroundLocationRationaleDialog = true
        } else {
            Log.w("ScheduleTabScreen", "❌ Location permission denied")
            // 권한 거부 다이얼로그 표시
            showLocationDeniedDialog = true
            // Pending 데이터 초기화
            clearPendingData()
        }
    }
    
    // 🆕 백그라운드 위치 권한 요청 런처
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationViewModel.checkPermissions()
        if (isGranted) {
            Log.d("ScheduleTabScreen", "✅ Background location permission granted")
            // 모든 권한 획득 → 스케줄 생성 진행
            proceedWithScheduleCreation()
        } else {
            Log.w("ScheduleTabScreen", "❌ Background location permission denied")
            // 설정 화면으로 이동 안내
            showOpenSettingsDialog = true
        }
    }
    
    // 🆕 권한 요청 핸들러
    val requestLocationPermission: () -> Unit = {
        when {
            !locationPermissionGranted -> {
                Log.d("ScheduleTabScreen", "📍 Requesting ACCESS_FINE_LOCATION")
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            !backgroundLocationPermissionGranted -> {
                Log.d("ScheduleTabScreen", "📍 Requesting ACCESS_BACKGROUND_LOCATION")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    showBackgroundLocationRationaleDialog = true
                } else {
                    // Android 10 미만에서는 백그라운드 권한 불필요
                    proceedWithScheduleCreation()
                }
            }
            else -> {
                // 모든 권한 있음
                Log.d("ScheduleTabScreen", "✅ All permissions granted")
                proceedWithScheduleCreation()
            }
        }
    }
    
    // 화면 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadAllLinkedCounts()
        locationViewModel.checkPermissions()
    }

    GlassScaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 헤더 (리포트 페이지와 동일한 스타일)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "스케줄",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // 🆕 v0.10.1: 1. 작동 중인 스케줄 카드 (타이머 실행 중)
            if (timerState == com.allday.detoxy.domain.model.FocusState.RUNNING && runningScheduleGroup != null) {
                item {
                    RunningScheduleCard(
                        schedule = runningScheduleGroup,
                        remainingSeconds = remainingSeconds,
                        linkedLocations = linkedLocations[runningScheduleGroup.id] ?: emptyList()
                    )
                }
            }
            
            // 2. 활성화된 스케줄 카드들 (대기 상태) - 작동 중인 것 제외
            val waitingGroups = activeGroups.filter { it.id != runningScheduleGroupId }
            items(waitingGroups) { group ->
                ActiveScheduleSummaryCard(
                    activeSchedule = group,
                    linkedLocations = linkedLocations[group.id] ?: emptyList()
                )
            }
            
            // 3. 다음 예약 카드
            item {
                NextScheduleSummaryCard(
                    scheduleGroups = scheduleGroups,
                    activeGroupId = activeGroups.firstOrNull()?.id  // 첫 번째 활성화 그룹 (호환성 유지)
                )
            }
            
            // 섹션 헤더
            item {
                Text(
                    text = "모든 시간표 (${scheduleGroups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            // 빈 상태
            if (scheduleGroups.isEmpty()) {
                item {
                    EmptyScheduleCard(
                        onCreateClick = { showCreateDialog = true }
                    )
                }
            } else {
                // 시간표 목록
                items(scheduleGroups) { group ->
                    val timeSlots = linkedTimeBasedAutoRuns[group.id] ?: emptyList()
                    val locationCount = linkedLocationCounts[group.id] ?: 0
                    
                    ScheduleSummaryCard(
                        group = group,
                        isActive = activeGroups.any { it.id == group.id },  // 🆕 v0.10.1: 여러 활성화 그룹 지원
                        timeSlotCount = timeSlots.size,
                        linkedLocationCount = locationCount,
                        onClick = { onNavigateToDetail(group.id) }
                    )
                }
                
                // 하단 여백 (FAB과 겹치지 않도록)
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } // LazyColumn 끝
        } // Column 끝
        
        // FAB (GlassScaffold 내 BoxScope)
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, contentDescription = "시간표 추가")
        }
    } // GlassScaffold 끝
    
    // 🆕 시간표 생성 다이얼로그 (위치 정보 등록 포함)
    if (showCreateDialog) {
        ScheduleCreationDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { scheduleName, mode, timeSlots, template, locationInfo ->
                Log.d("ScheduleTabScreen", "📥 onConfirm called: name=$scheduleName, mode=$mode, locationInfo=$locationInfo")
                Log.d("ScheduleTabScreen", "   template=$template, timeSlots=${timeSlots.size}")
                
                if (locationInfo == null) {
                    // 어디서나 적용 (위치 없음) → 바로 생성
                    scope.launch {
                        try {
                            Log.d("ScheduleTabScreen", "🌐 Creating schedule without location")
                            when (mode) {
                                CreationMode.TEMPLATE -> {
                                    if (template == null) {
                                        Log.e("ScheduleTabScreen", "❌ ERROR: template is null but mode is TEMPLATE!")
                                        return@launch
                                    }
                                    viewModel.createFromTemplate(scheduleName, template, isLocationBased = false)  // 🐛 버그 수정: 일반 시간 스케쥴
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
                            showCreateDialog = false
                        } catch (e: Exception) {
                            Log.e("ScheduleTabScreen", "❌ Exception in onConfirm: ${e.message}", e)
                        }
                    }
                } else {
                    // 🆕 위치 기반 스케줄 → 권한 체크
                    Log.d("ScheduleTabScreen", "📍 Location-based schedule requested")
                    Log.d("ScheduleTabScreen", "   Location: ${locationInfo.name} (${locationInfo.address})")
                    Log.d("ScheduleTabScreen", "   Permission status: fine=$locationPermissionGranted, background=$backgroundLocationPermissionGranted")
                    
                    if (locationPermissionGranted && backgroundLocationPermissionGranted) {
                        // 권한 있음 → 바로 생성
                        Log.d("ScheduleTabScreen", "✅ All permissions granted, proceeding with creation")
                        pendingLocationInfo = locationInfo
                        pendingScheduleName = scheduleName
                        pendingMode = mode
                        pendingTimeSlots = timeSlots
                        pendingTemplate = template
                        showCreateDialog = false
                        proceedWithScheduleCreation()
                    } else {
                        // 권한 없음 → Pending 데이터 저장 후 권한 요청
                        Log.d("ScheduleTabScreen", "⚠️ Permissions missing, requesting permissions")
                        pendingLocationInfo = locationInfo
                        pendingScheduleName = scheduleName
                        pendingMode = mode
                        pendingTimeSlots = timeSlots
                        pendingTemplate = template
                        showCreateDialog = false
                        requestLocationPermission()
                    }
                }
            },
            initialMode = CreationMode.CUSTOM  // 🔑 커스텀 모드로 시작
        )
    }
    
    // 🆕 백그라운드 위치 권한 설명 다이얼로그
    if (showBackgroundLocationRationaleDialog) {
        BackgroundLocationRationaleDialog(
            onProceed = {
                showBackgroundLocationRationaleDialog = false
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                } else {
                    // Android 10 미만에서는 백그라운드 권한 불필요
                    proceedWithScheduleCreation()
                }
            },
            onDismiss = {
                showBackgroundLocationRationaleDialog = false
                clearPendingData()
            }
        )
    }
    
    // 🆕 위치 권한 거부 다이얼로그
    if (showLocationDeniedDialog) {
        LocationPermissionDeniedDialog(
            onRetry = {
                showLocationDeniedDialog = false
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onNavigateToTimeBased = {
                showLocationDeniedDialog = false
                clearPendingData()
                // 시간 기반 자동 실행으로 이동 (향후 구현)
            },
            onDismiss = {
                showLocationDeniedDialog = false
                clearPendingData()
            }
        )
    }
    
    // 🆕 설정 화면 이동 안내 다이얼로그
    if (showOpenSettingsDialog) {
        OpenSettingsDialog(
            onOpenSettings = {
                showOpenSettingsDialog = false
                PermissionUtils.openAppLocationSettings(context)
            },
            onDismiss = {
                showOpenSettingsDialog = false
                clearPendingData()
            }
        )
    }
}

/**
 * 🔴 작동 중 스케줄 카드 (v0.10.1)
 *
 * 타이머가 실제로 실행 중인 스케줄 그룹을 표시합니다.
 * "작동 중" = 지금 이 순간 타이머가 돌고 있는 상태
 *
 * @param schedule 작동 중인 스케줄 그룹
 * @param remainingSeconds 남은 시간 (초)
 * @param linkedLocations 연결된 위치 목록
 */
@Composable
fun RunningScheduleCard(
    schedule: ScheduleGroup,
    remainingSeconds: Int,
    linkedLocations: List<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.6f,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 작동 인디케이터 (재생 아이콘)
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔴 작동 중",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 남은 시간 표시
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val timeText = when {
                    hours > 0 -> "${hours}시간 ${minutes}분 남음"
                    minutes > 0 -> "${minutes}분 남음"
                    else -> "${remainingSeconds}초 남음"
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // 위치 정보 (있는 경우)
                if (linkedLocations.isNotEmpty()) {
                    Text(
                        text = "📍 ${linkedLocations.first().label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Icon(
                imageVector = getIconForType(schedule.iconType),
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(schedule.colorHex)),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 🟢 활성화된 스케줄 카드 (v0.10.1 개선)
 *
 * 활성화되어 대기 중인 스케줄 그룹을 표시합니다.
 * "활성화됨" = 스케줄이 켜져있어서 조건이 맞으면 자동으로 작동할 준비가 된 상태
 *
 * @param activeSchedule 활성화된 스케줄 그룹
 * @param linkedLocations 연결된 위치 목록
 */
@Composable
fun ActiveScheduleSummaryCard(
    activeSchedule: ScheduleGroup,
    linkedLocations: List<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.5f,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 활성 인디케이터
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🟢 활성화됨 (대기 중)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeSchedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 위치 기반인 경우 트리거 조건 표시
                if (linkedLocations.isNotEmpty()) {
                    Text(
                        text = "📍 ${linkedLocations.first().label}에 진입하면 작동",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "⏰ 예정된 시간에 자동 작동",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                imageVector = getIconForType(activeSchedule.iconType),
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(activeSchedule.colorHex)),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 다음 예약 요약 카드
 *
 * 오늘 예정된 다음 시간대를 표시합니다.
 */
@Composable
fun NextScheduleSummaryCard(
    @Suppress("UNUSED_PARAMETER") scheduleGroups: List<ScheduleGroup>,
    @Suppress("UNUSED_PARAMETER") activeGroupId: String?,
    viewModel: ScheduleGroupViewModel = hiltViewModel()
) {
    val nextSchedule by viewModel.getNextScheduleToday().collectAsState(initial = null)
    
    if (nextSchedule != null) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            alpha = 0.4f,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "다음 예약",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${nextSchedule!!.hour}:${String.format("%02d", nextSchedule!!.minute)} - ${nextSchedule!!.durationMinutes}분",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 스케줄 요약 카드
 *
 * 각 스케줄 그룹의 간단한 정보를 표시합니다.
 */
@Composable
fun ScheduleSummaryCard(
    group: ScheduleGroup,
    isActive: Boolean,
    timeSlotCount: Int,
    linkedLocationCount: Int,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        alpha = if (isActive) 0.5f else 0.3f,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘
            Icon(
                imageVector = getIconForType(group.iconType),
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(group.colorHex)),
                modifier = Modifier.size(32.dp)
            )
            
            // 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 시간대 개수
                    Text(
                        text = "⏰ ${timeSlotCount}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // 연결된 위치
                    if (linkedLocationCount > 0) {
                        Text(
                            text = "📍 위치 ${linkedLocationCount}개",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "어디서나 적용",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            // 활성 배지
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "활성화",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 빈 상태 카드
 *
 * 스케줄이 없을 때 표시되는 안내 카드입니다.
 */
@Composable
fun EmptyScheduleCard(
    onCreateClick: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        alpha = 0.3f,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Text(
                text = "아직 시간표가 없습니다",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "자동 실행 스케줄을 추가하여\n매일 반복되는 집중 루틴을 만들어보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Button(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("첫 시간표 만들기")
            }
        }
    }
}

/**
 * 아이콘 타입에 따른 Material Icon 반환
 */
@Composable
fun getIconForType(iconType: String): ImageVector {
    return when (iconType) {
        "HOME" -> Icons.Default.Home
        "WORK" -> Icons.Default.Star
        "STUDY" -> Icons.Default.Place
        "CALENDAR" -> Icons.Default.DateRange
        "PLACE" -> Icons.Default.Place
        else -> Icons.Default.DateRange
    }
}

