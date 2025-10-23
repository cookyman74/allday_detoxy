package com.allday.detoxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.allday.detoxy.core.utils.PreferenceManager
import com.allday.detoxy.presentation.ui.autorun.TimeBasedAutoRunScreen
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
 * 네비게이션이 포함된 메인 화면
 *
 * Week 3.3.1: 타이머와 리포트 화면 간 탭 네비게이션 제공
 * 1차 고도화 (Week 1): 설정 탭 추가
 * 2차 고도화 (Week 2): 예약설정(시간 기반 자동 실행) 화면 추가
 * 구성: 타이머, 리포트, 설정, 예약설정 (3개 탭 + 1개 상세 화면)
 */
@Composable
fun MainScreenWithNavigation() {
    var selectedTab by remember { mutableStateOf(0) }
    var showTimeBasedAutoRun by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 예약설정 화면에서는 하단 네비게이션 숨김
            if (!showTimeBasedAutoRun) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        },
                        label = { Text("타이머") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null
                            )
                        },
                        label = { Text("리포트") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
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
            when {
                // 예약설정 화면
                showTimeBasedAutoRun -> {
                    TimeBasedAutoRunScreen(
                        onBack = { showTimeBasedAutoRun = false }
                    )
                }
                // 탭별 화면
                else -> {
                    when (selectedTab) {
                        0 -> TimerScreen()
                        1 -> ReportScreen()
                        2 -> DetoxyControlSettingsScreen(
                            onBack = { selectedTab = 0 },  // 뒤로 가기 시 타이머로
                            onNavigateToAutoRun = { showTimeBasedAutoRun = true }  // 예약설정으로
                        )
                    }
                }
            }
        }
    }
}
