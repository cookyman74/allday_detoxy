package com.allday.detoxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.allday.detoxy.core.utils.PreferenceManager
import com.allday.detoxy.presentation.ui.autorun.TimeBasedAutoRunScreen
import com.allday.detoxy.presentation.ui.autorun.LocationBasedAutoRunScreen
import com.allday.detoxy.presentation.ui.autorun.ScheduleGroupScreen
import com.allday.detoxy.presentation.ui.schedule.ScheduleTabScreen
import com.allday.detoxy.presentation.ui.onboarding.WelcomeScreen
import com.allday.detoxy.presentation.ui.overlay.LockOverlayScreen
import com.allday.detoxy.presentation.ui.permission.PermissionCheckScreen
import com.allday.detoxy.presentation.ui.report.ReportScreen
import com.allday.detoxy.presentation.ui.settings.focus.DetoxyControlSettingsScreen
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
import com.allday.detoxy.presentation.ui.timer.TimerScreen
import com.allday.detoxy.presentation.viewmodel.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log

/**
 * 앱의 메인 액티비티
 *
 * @AndroidEntryPoint 어노테이션으로 Hilt가 의존성 주입 가능
 * Jetpack Compose를 사용하여 UI 렌더링
 * 타이머 화면과 접근성 권한 체크 기능 포함
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 타이머 포기로 돌아온 경우 처리
        handleTimerGiveUp(intent)

        setContent {
            DetoxyTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)  // Update the intent
        handleTimerGiveUp(intent)
    }

    private fun handleTimerGiveUp(intent: Intent?) {
        intent?.let {
            val timerGaveUp = it.getBooleanExtra("TIMER_GAVE_UP", false)
            if (timerGaveUp) {
                // 타이머가 포기된 경우, 타이머 리셋
                timerViewModel.giveUpTimer()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    // 온보딩 상태 관리
    var showOnboarding by remember { mutableStateOf(!preferenceManager.isOnboardingCompleted()) }
    var showPermissionCheck by remember { mutableStateOf(false) }

    when {
        // 1. 온보딩 미완료 -> 환영 화면
        showOnboarding -> {
            WelcomeScreen(
                onNextClick = {
                    preferenceManager.setOnboardingCompleted()
                    showOnboarding = false
                    showPermissionCheck = true
                }
            )
        }
        // 2. 온보딩 완료 후 권한 체크 -> 권한 안내 화면
        showPermissionCheck -> {
            PermissionCheckScreen(
                onAllPermissionsGranted = {
                    preferenceManager.setFirstLaunchCompleted()
                    showPermissionCheck = false
                }
            )
        }
        // 3. 모두 완료 -> 메인 화면
        else -> {
            MainScreenWithNavigation()
        }
    }
}

/**
 * 자동 실행 화면 타입
 */
enum class AutoRunScreenType {
    NONE,           // 자동 실행 화면 없음
    TIME_BASED,     // 시간 기반 자동 실행
    LOCATION_BASED, // 위치 기반 자동 실행
    SCHEDULE_GROUP  // 🆕 3차 고도화: 시간표 그룹 관리
}

/**
 * 네비게이션이 포함된 메인 화면
 *
 * Week 3.3.1: 타이머와 리포트 화면 간 탭 네비게이션 제공
 * 1차 고도화 (Week 1): 설정 탭 추가
 * 2차 고도화 (Week 2): 예약설정(시간 기반 자동 실행) 화면 추가
 * 2차 고도화 (Week 3): 위치 기반 자동 실행 화면 추가
 * 3차 고도화 (Week 4): 스케줄 그룹 관리 화면 추가
 * UI/UX 개선 (v0.10): 스케줄 탭 신설 (4개 탭 체제)
 * 구성: 타이머, 스케줄, 리포트, 설정 (4개 탭)
 */
@Composable
fun MainScreenWithNavigation() {
    // 🆕 상태 보존 (프로세스 재시작 대응)
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAutoRunScreen by remember { mutableStateOf(AutoRunScreenType.NONE) }
    
    // 스케줄 그룹 정보 (TimeBasedAutoRunScreen으로 전달용)
    var selectedScheduleGroupId by remember { mutableStateOf<String?>(null) }
    var selectedScheduleGroupName by remember { mutableStateOf<String?>(null) }
    
    // 🆕 뒤로가기 처리
    BackHandler(enabled = showAutoRunScreen != AutoRunScreenType.NONE) {
        showAutoRunScreen = AutoRunScreenType.NONE
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 자동 실행 화면에서는 하단 네비게이션 숨김
            if (showAutoRunScreen == AutoRunScreenType.NONE) {
                NavigationBar {
                    // 탭 0: 타이머
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "타이머"
                            )
                        },
                        label = { Text("타이머") }
                    )
                    
                    // 🆕 탭 1: 스케줄 (신설)
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "스케줄"
                            )
                        },
                        label = { Text("스케줄") }
                    )
                    
                    // 탭 2: 리포트 (기존 1 → 2)
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "리포트"
                            )
                        },
                        label = { Text("리포트") }
                    )
                    
                    // 탭 3: 설정 (기존 2 → 3)
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "설정"
                            )
                        },
                        label = { Text("설정") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (showAutoRunScreen) {
                // 시간 기반 자동 실행 화면
                AutoRunScreenType.TIME_BASED -> {
                    TimeBasedAutoRunScreen(
                        onBack = {
                            // 🆕 스케줄 그룹에서 온 경우 다시 스케줄 그룹으로, 아니면 설정으로
                            if (selectedScheduleGroupId != null) {
                                showAutoRunScreen = AutoRunScreenType.SCHEDULE_GROUP
                                selectedScheduleGroupId = null
                                selectedScheduleGroupName = null
                            } else {
                                showAutoRunScreen = AutoRunScreenType.NONE
                            }
                        },
                        onNavigateToLocationBased = { showAutoRunScreen = AutoRunScreenType.LOCATION_BASED },
                        onNavigateToScheduleGroup = { showAutoRunScreen = AutoRunScreenType.SCHEDULE_GROUP },
                        scheduleGroupId = selectedScheduleGroupId,  // 🆕 스케줄 그룹 ID 전달
                        scheduleGroupName = selectedScheduleGroupName  // 🆕 스케줄 그룹 이름 전달
                    )
                }
                // 위치 기반 자동 실행 화면
                AutoRunScreenType.LOCATION_BASED -> {
                    LocationBasedAutoRunScreen(
                        onBack = { showAutoRunScreen = AutoRunScreenType.NONE },
                        onNavigateToTimeBased = { showAutoRunScreen = AutoRunScreenType.TIME_BASED },
                        onNavigateToScheduleGroup = { showAutoRunScreen = AutoRunScreenType.SCHEDULE_GROUP }  // 🆕 3차 고도화
                    )
                }
                // 🆕 3차 고도화: 시간표 그룹 관리 화면
                AutoRunScreenType.SCHEDULE_GROUP -> {
                    ScheduleGroupScreen(
                        onBack = { showAutoRunScreen = AutoRunScreenType.NONE },
                        onNavigateToTimeBasedAutoRun = { scheduleGroupId, scheduleGroupName ->
                            // 🆕 스케줄 그룹 정보 저장 후 TimeBasedAutoRunScreen으로 이동
                            selectedScheduleGroupId = scheduleGroupId
                            selectedScheduleGroupName = scheduleGroupName
                            showAutoRunScreen = AutoRunScreenType.TIME_BASED
                        }
                    )
                }
                // 탭별 화면
                AutoRunScreenType.NONE -> {
                    when (selectedTab) {
                        0 -> TimerScreen()
                        1 -> {
                            // 🆕 스케줄 탭 (v0.10 UI/UX 개선)
                            ScheduleTabScreen(
                                onNavigateToDetail = { groupId ->
                                    selectedScheduleGroupId = groupId
                                    showAutoRunScreen = AutoRunScreenType.SCHEDULE_GROUP
                                }
                            )
                        }
                        2 -> ReportScreen()  // 기존 1 → 2
                        3 -> DetoxyControlSettingsScreen(  // 기존 2 → 3
                            onBack = { selectedTab = 0 }  // 뒤로 가기 시 타이머로
                        )
                    }
                }
            }
        }
    }
}
