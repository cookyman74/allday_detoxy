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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
import com.allday.detoxy.presentation.ui.timer.TimerScreen
import com.allday.detoxy.presentation.viewmodel.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint

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
    // MVP: 타이머 화면을 메인 화면으로 사용
    // TODO: Week 2 - 네비게이션 구조 추가 (타이머, 통계, 설정 탭)
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TimerScreen()
        }
    }
}
