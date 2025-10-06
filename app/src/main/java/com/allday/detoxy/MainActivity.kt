package com.allday.detoxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
import com.allday.detoxy.presentation.ui.timer.TimerScreen
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 타이머 포기로 돌아온 경우 처리
        val timerGaveUp = intent.getBooleanExtra("TIMER_GAVE_UP", false)
        if (timerGaveUp) {
            // 타이머가 포기된 경우, TimerScreen이 IDLE 상태로 시작됨
            // ViewModel의 BroadcastReceiver가 자동으로 처리
        }

        setContent {
            DetoxyTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { newIntent ->
            val timerGaveUp = newIntent.getBooleanExtra("TIMER_GAVE_UP", false)
            if (timerGaveUp) {
                // 앱이 이미 실행 중인 경우 새 인텐트 처리
                // ViewModel의 BroadcastReceiver가 자동으로 처리
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
