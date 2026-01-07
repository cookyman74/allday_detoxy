package com.allday.detoxy.presentation.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.presentation.viewmodel.TimerViewModel
import com.allday.detoxy.presentation.ui.timer.components.*
import com.allday.detoxy.presentation.ui.component.GlassSurface
import com.allday.detoxy.presentation.ui.component.GlassDialog
import com.allday.detoxy.presentation.ui.component.liquidGlass
import com.allday.detoxy.presentation.ui.theme.GlassWhite

/**
 * 타이머 메인 화면 (도넛 그래프 통합)
 *
 * 도넛 그래프로 시간을 선택하고 프리셋 버튼을 제공합니다.
 *
 * ## 주요 기능
 * - 도넛 그래프 타이머 선택 (5-180분)
 * - 기본 프리셋 (25/45/60분)
 * - 커스텀 프리셋 저장 및 관리
 * - 원형 프로그레스 바 표시
 * - 다음 예약 정보 표시
 *
 * @param viewModel 타이머 ViewModel
 */
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val permissionError by viewModel.permissionError.collectAsState()
    val nextAutoRunInfo by viewModel.nextAutoRunInfo.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val showSuccessAnimation by viewModel.showSuccessAnimation.collectAsState() // 🆕 성공 애니메이션
    val selectedTimerStyleIndex by viewModel.selectedTimerStyleIndex.collectAsState() // 🆕 저장된 스타일 인덱스
    
    // 🆕 v9: 세션 종료 다이얼로그 상태
    val showTodoDialog by viewModel.showTodoDialog.collectAsState()
    val showGoalDialog by viewModel.showGoalDialog.collectAsState()
    val currentScheduleInfo by viewModel.currentScheduleInfo.collectAsState()
    
    // 도넛 그래프 선택 시간
    var selectedMinutes by remember { mutableStateOf(25) }
    
    // 다이얼로그 상태
    var showSaveDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<com.allday.detoxy.data.local.entity.CustomTimerPreset?>(null) }
    
    // 🆕 v9: 세션 종료 할일 체크 다이얼로그
    if (showTodoDialog && currentScheduleInfo != null) {
        SessionEndTodoDialog(
            scheduleInfo = currentScheduleInfo!!,
            onComplete = { responses -> viewModel.onTodoDialogComplete(responses) },
            onDismiss = { viewModel.onDialogDismiss() }
        )
    }
    
    // 🆕 v9: 세션 종료 목표 달성 다이얼로그
    if (showGoalDialog && currentScheduleInfo != null) {
        SessionEndGoalDialog(
            goal = currentScheduleInfo!!.title,
            onComplete = { completed -> viewModel.onGoalDialogComplete(completed) },
            onDismiss = { viewModel.onDialogDismiss() }
        )
    }
    
    // 🆕 성공 축하 다이얼로그
    if (showSuccessAnimation) {
        SuccessCelebrationDialog(
            onDismiss = { viewModel.onSuccessAnimationShown() }
        )
    }
    
    // 권한 에러 다이얼로그
    permissionError?.let { error ->
        PermissionErrorDialog(
            error = error,
            onDismiss = { viewModel.clearPermissionError() },
            onOpenSettings = {
                when (error) {
                    is TimerViewModel.PermissionError.AccessibilityServiceDisabled,
                    is TimerViewModel.PermissionError.AccessibilityServiceCrashed -> {
                        // 둘 다 접근성 설정으로 이동
                        PermissionUtils.openAccessibilitySettings(context)
                    }
                    is TimerViewModel.PermissionError.OverlayPermissionDenied -> {
                        PermissionUtils.openOverlaySettings(context)
                    }
                }
                viewModel.clearPermissionError()
            }
        )
    }
    
    // 프리셋 저장/업데이트 다이얼로그
    if (showSaveDialog) {
        val existingPresetForSave = customPresets.find { it.durationMinutes == selectedMinutes }
        SavePresetDialog(
            durationMinutes = selectedMinutes,
            existingPresetName = existingPresetForSave?.name,
            existingPresetType = existingPresetForSave?.presetType,
            onSave = { name, presetType ->
                if (existingPresetForSave != null) {
                    // 기존 프리셋 업데이트
                    viewModel.updateCustomPreset(
                        existingPresetForSave.copy(name = name, presetType = presetType)
                    )
                } else {
                    // 새 프리셋 저장
                viewModel.saveCustomPreset(name, selectedMinutes, presetType)
                }
            },
            onDismiss = { showSaveDialog = false }
        )
    }
    
    // 프리셋 편집 다이얼로그
    if (showEditDialog && selectedPreset != null) {
        EditPresetDialog(
            presetName = selectedPreset!!.name,
            presetType = selectedPreset!!.presetType,
            onSave = { name, presetType ->
                viewModel.updateCustomPreset(
                    selectedPreset!!.copy(
                        name = name,
                        presetType = presetType
                    )
                )
            },
            onDismiss = { showEditDialog = false }
        )
    }
    
    // 프리셋 삭제 확인 다이얼로그
    if (showDeleteDialog && selectedPreset != null) {
        DeletePresetDialog(
            presetName = selectedPreset!!.name,
            onConfirm = {
                viewModel.deleteCustomPreset(selectedPreset!!.id)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    // 프리셋 관리 BottomSheet
    if (showPresetSheet && selectedPreset != null) {
        PresetManagementBottomSheet(
            preset = selectedPreset!!,
            onEdit = {
                showPresetSheet = false
                showEditDialog = true
            },
            onDelete = {
                showPresetSheet = false
                showDeleteDialog = true
            },
            onDismiss = { showPresetSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // 1. System Bar Padding
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 제목
        Text(
            text = "집중 타이머",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 🆕 흑백 모드 팁 배너 (Phase 6)
        // 조건: Android 14 이하 + 흑백 모드 ON + "다시 보지 않기" 상태가 false
        val grayscaleModeEnabled by viewModel.grayscaleModeEnabled.collectAsState()
        val grayscaleTipDismissed by viewModel.grayscaleTipDismissed.collectAsState()
        
        if (grayscaleModeEnabled && !grayscaleTipDismissed) {
            GrayscaleTipBanner(
                onDismiss = { viewModel.dismissGrayscaleTip() },
                onNavigateToSettings = {
                    try {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    } catch (e: Exception) {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                        )
                    }
                }
            )
        }

        // 타이머 상태에 따른 UI 표시
        when (timerState) {
            FocusState.IDLE -> {
                // Glass Panel for Timer Controls
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    alpha = 0.3f
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 16.dp), // 24dp → 8dp
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Multi-style Timer Pager (IDLE 상태)
                        val pagerState = rememberPagerState(
                            initialPage = selectedTimerStyleIndex,
                            pageCount = { 3 }
                        )
                        val coroutineScope = rememberCoroutineScope()

                        // 1. Pager 스와이프 → ViewModel 저장
                        LaunchedEffect(pagerState.currentPage) {
                            viewModel.onTimerStyleChanged(pagerState.currentPage)
                        }

                        // 2. ViewModel 상태 변경 → Pager 이동 (초기 로딩 시)
                        LaunchedEffect(selectedTimerStyleIndex) {
                            if (pagerState.currentPage != selectedTimerStyleIndex) {
                                pagerState.scrollToPage(selectedTimerStyleIndex)
                            }
                        }

                        // 🆕 상단 스와이프 영역 (타이머 스타일 선택용)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp) // 40dp → 16dp
                                .pointerInput(pagerState) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        coroutineScope.launch {
                                            val targetPage = if (dragAmount < -50) {
                                                (pagerState.currentPage + 1).coerceAtMost(2)
                                            } else if (dragAmount > 50) {
                                                (pagerState.currentPage - 1).coerceAtLeast(0)
                                            } else {
                                                pagerState.currentPage
                                            }
                                            if (targetPage != pagerState.currentPage) {
                                                pagerState.animateScrollToPage(targetPage)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 스와이프 힌트 (선택적)
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 16.dp,
                            userScrollEnabled = false // 🆕 타이머 내부 스와이프 비활성화
                        ) { page ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (page) {
                                    0 -> {
                                        // Type A: Liquid Ring (Interactive - 드래그 포인터 포함)
                                        TimerStyleLiquidRing(
                                            selectedMinutes = selectedMinutes,
                                            onMinutesChange = { selectedMinutes = it },
                                            state = FocusState.IDLE,
                                            modifier = Modifier.padding(vertical = 4.dp) // 16dp → 4dp
                                        )
                                    }
                                    1 -> {
                                        // Type B: Minimal Flux (Preview + Gesture Handler)
                                        var isDragging by remember { mutableStateOf(false) }
                                        
                                        Box(contentAlignment = Alignment.Center) {
                                            TimerStyleMinimalFlux(
                                                state = FocusState.IDLE,
                                                remainingSeconds = selectedMinutes * 60,
                                                totalSeconds = selectedMinutes * 60,
                                                isDragging = isDragging
                                            )
                                            // 투명 제스처 핸들러 오버레이
                                            TimerGestureHandler(
                                                selectedMinutes = selectedMinutes,
                                                onMinutesChange = { selectedMinutes = it },
                                                onDragStateChange = { isDragging = it }
                                            )
                                        }
                                    }
                                    2 -> {
                                        // Type C: Glass Sector (Interactive - 부채꼴 드래그)
                                            TimerStyleGlassSector(
                                                selectedMinutes = selectedMinutes,
                                            onMinutesChange = { selectedMinutes = it },
                                            state = FocusState.IDLE,
                                            modifier = Modifier.padding(vertical = 4.dp) // 16dp → 4dp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 🆕 하단 스와이프 영역 + Page Indicator (클릭 가능)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .pointerInput(pagerState) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        coroutineScope.launch {
                                            val targetPage = if (dragAmount < -50) {
                                                (pagerState.currentPage + 1).coerceAtMost(2)
                                            } else if (dragAmount > 50) {
                                                (pagerState.currentPage - 1).coerceAtLeast(0)
                                            } else {
                                                pagerState.currentPage
                                            }
                                            if (targetPage != pagerState.currentPage) {
                                                pagerState.animateScrollToPage(targetPage)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Page Indicator (클릭 가능)
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                Box(
                                    modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                                .size(10.dp) // 크기 약간 확대
                                                .clickable {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(iteration)
                                                    }
                                                }
                                )
                            }
                        }
                        
                        // 다음 예약 정보 표시
                        if (nextAutoRunInfo != null) {
                            Text(
                                text = nextAutoRunInfo!!,
                                style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                            )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // 프리셋 저장/업데이트 버튼 (항상 표시)
                        // 기존 프리셋이 있으면 업데이트, 없으면 새로 저장
                        val existingPreset = customPresets.find { it.durationMinutes == selectedMinutes }
                            OutlinedButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                            Text(if (existingPreset != null) "프리셋 업데이트" else "프리셋으로 저장")
                        }
                        
                        // 프리셋 버튼
                        PresetButtonRow(
                            customPresets = customPresets,
                            selectedMinutes = selectedMinutes,
                            onPresetClick = { minutes, presetId ->
                                selectedMinutes = minutes
                                viewModel.incrementPresetUsage(presetId)
                            },
                            onPresetLongClick = { preset ->
                                // 길게 누르면 바로 삭제 확인 다이얼로그 표시
                                selectedPreset = preset
                                showDeleteDialog = true
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 시작 버튼
                        Button(
                            onClick = { viewModel.startTimer(selectedMinutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "시작하기",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            FocusState.RUNNING -> {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    alpha = 0.3f
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Multi-style Timer Pager
                        val pagerState = rememberPagerState(
                            initialPage = selectedTimerStyleIndex,
                            pageCount = { 3 }
                        )

                        // 1. Pager 스와이프 → ViewModel 저장
                        LaunchedEffect(pagerState.currentPage) {
                            viewModel.onTimerStyleChanged(pagerState.currentPage)
                        }

                        // 2. ViewModel 상태 변경 → Pager 이동 (초기 로딩 시)
                        LaunchedEffect(selectedTimerStyleIndex) {
                            if (pagerState.currentPage != selectedTimerStyleIndex) {
                                pagerState.scrollToPage(selectedTimerStyleIndex)
                            }
                        }
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (page) {
                                    0 -> TimerStyleLiquidRing(
                                        state = timerState,
                                        remainingSeconds = remainingSeconds,
                                        totalSeconds = totalSeconds
                                    )
                                    1 -> TimerStyleMinimalFlux(
                                        state = timerState,
                                        remainingSeconds = remainingSeconds,
                                        totalSeconds = totalSeconds
                                    )
                                    2 -> TimerStyleGlassSector(
                                        state = timerState,
                                        remainingSeconds = remainingSeconds,
                                        totalSeconds = totalSeconds
                                    )
                                    else -> TimerStyleLiquidRing(
                                        state = timerState,
                                        remainingSeconds = remainingSeconds,
                                        totalSeconds = totalSeconds
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Page Indicator
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 포기 버튼
                        Button(
                            onClick = { viewModel.giveUpTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // 버튼 너비 조정
                                .height(56.dp)
                        ) {
                            Text("포기하기")
                        }
                    }
                }
            }

            FocusState.FINISHED -> {
                // 완료 메시지
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎉 타이머 완료!",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "집중 시간을 성공적으로 완료했습니다.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("새 타이머 시작")
                    }
                }
            }

            FocusState.FAILED -> {
                // 포기 메시지
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "타이머 포기",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "다음엔 더 잘할 수 있어요!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("다시 시작")
                    }
                }
            }
        }
    }
}

// CircularTimerDisplay removed and moved to TimerStyleLiquidRing.kt

/**
 * 성공 축하 다이얼로그
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun SuccessCelebrationDialog(
    onDismiss: () -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎉 집중 성공!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "목표를 달성했습니다!",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "집중 시간을 성공적으로 완료했어요.\n계속해서 좋은 습관을 만들어가세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("확인")
            }
        }
    }
}

/**
 * 권한 에러 다이얼로그
 *
 * @param error 권한 에러 타입
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onOpenSettings 설정 화면으로 이동 콜백
 */
@Composable
fun PermissionErrorDialog(
    error: TimerViewModel.PermissionError,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showBatteryGuide by remember { mutableStateOf(false) }
    
    // 배터리 최적화 가이드 다이얼로그
    if (showBatteryGuide) {
        com.allday.detoxy.presentation.ui.component.BatteryOptimizationGuideDialog(
            onDismiss = { showBatteryGuide = false }
        )
    }
    
    val (title, message) = when (error) {
        is TimerViewModel.PermissionError.AccessibilityServiceDisabled -> {
            "앱 차단 기능 권한 필요" to "집중 타이머를 사용하려면 앱 차단 기능(접근성 서비스)을 활성화해야 합니다.\n\n" +
                    "설정 화면에서 'ScreenSence'를 찾아 활성화해주세요."
        }
        is TimerViewModel.PermissionError.AccessibilityServiceCrashed -> {
            // 🆕 v0.10.4: 크래시 상태 안내 (v0.10.7: 배터리 최적화 안내 추가)
            "⚠️ 앱 차단 기능 재시작 필요" to "앱 차단 기능(접근성 서비스)이 일시적으로 중지되었습니다.\n\n" +
                    "1️⃣ 설정 화면에서 'ScreenSence'를 꺼다가 다시 켜주세요.\n\n" +
                    "2️⃣ 이 문제가 반복되면 아래 '배터리 설정 가이드'를 확인하세요."
        }
        is TimerViewModel.PermissionError.OverlayPermissionDenied -> {
            "잠금 화면 표시 권한 필요" to "집중 타이머를 사용하려면 잠금 화면 표시 권한(다른 앱 위에 표시)이 필요합니다.\n\n" +
                    "설정 화면에서 권한을 허용해주세요."
        }
    }

    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // 🆕 v0.10.7: 크래시 상태에서 배터리 최적화 가이드 버튼 표시
            if (error is TimerViewModel.PermissionError.AccessibilityServiceCrashed) {
                val isAggressiveManufacturer = com.allday.detoxy.core.utils.BatteryOptimizationUtils.isAggressiveBatteryOptimizationManufacturer()
                if (isAggressiveManufacturer) {
                    OutlinedButton(
                        onClick = { showBatteryGuide = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔋 배터리 설정 가이드")
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onOpenSettings) {
                    Text("설정으로 이동")
                }
            }
        }
    }
}

