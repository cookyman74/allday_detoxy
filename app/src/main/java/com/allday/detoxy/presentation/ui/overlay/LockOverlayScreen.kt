package com.allday.detoxy.presentation.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 오버레이 잠금 화면
 *
 * 타이머 실행 중 차단 앱 접근 시 표시되는 전체 화면 오버레이입니다.
 * MVP 버전은 남은 시간 표시와 포기 버튼만 제공합니다.
 *
 * @param remainingSeconds 남은 시간 (초)
 * @param totalSeconds 전체 시간 (초)
 * @param onGiveUp 포기 버튼 클릭 콜백
 */
@Composable
fun LockOverlayScreen(
    remainingSeconds: Int,
    totalSeconds: Int,
    onGiveUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // 잠금 아이콘/메시지
            Text(
                text = "🔒",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "집중 모드 실행 중",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "차단된 앱입니다",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 남은 시간 표시
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "남은 시간",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Text(
                    text = formatTime(remainingSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 64.sp
                )

                // 진행률 표시
                LinearProgressIndicator(
                    progress = { getProgress(remainingSeconds, totalSeconds) },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 안내 메시지
            Text(
                text = "타이머가 끝나면 다시 사용할 수 있습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 포기 버튼 (하단)
            OutlinedButton(
                onClick = onGiveUp,
                modifier = Modifier
                    .width(200.dp)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "타이머 포기하기",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 시간을 MM:SS 형식으로 포맷
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

/**
 * 진행률 계산 (0.0 ~ 1.0)
 */
private fun getProgress(remainingSeconds: Int, totalSeconds: Int): Float {
    if (totalSeconds == 0) return 0f
    val elapsed = totalSeconds - remainingSeconds
    return elapsed.toFloat() / totalSeconds.toFloat()
}
