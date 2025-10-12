package com.allday.detoxy.presentation.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.presentation.viewmodel.TimerViewModel

/**
 * 타이머 메인 화면
 *
 * 프리셋 버튼, 원형 프로그레스 바, 시작/포기 버튼을 제공합니다.
 * MVP 버전으로 간소화되어 커스텀 시간 입력과 일시정지 기능은 제외됩니다.
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

    // 권한 에러 다이얼로그
    permissionError?.let { error ->
        PermissionErrorDialog(
            error = error,
            onDismiss = { viewModel.clearPermissionError() },
            onOpenSettings = {
                when (error) {
                    is TimerViewModel.PermissionError.AccessibilityServiceDisabled -> {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 제목
        Text(
            text = "집중 타이머",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 원형 프로그레스 바 & 타이머 표시
        CircularTimerDisplay(
            state = timerState,
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 타이머 상태에 따른 UI 표시
        when (timerState) {
            FocusState.IDLE -> {
                // 프리셋 버튼
                PresetButtons(
                    presets = viewModel.presetDurations,
                    onPresetClick = { duration ->
                        viewModel.startTimer(duration)
                    }
                )
            }

            FocusState.RUNNING -> {
                // 포기 버튼
                Button(
                    onClick = { viewModel.giveUpTimer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("포기하기")
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
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "집중 시간을 성공적으로 완료했습니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier.fillMaxWidth()
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
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "다음엔 더 잘할 수 있어요!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다시 시작")
                    }
                }
            }
        }
    }
}

/**
 * 원형 타이머 표시
 *
 * @param state 타이머 상태
 * @param remainingSeconds 남은 시간 (초)
 * @param totalSeconds 전체 시간 (초)
 */
@Composable
fun CircularTimerDisplay(
    state: FocusState,
    remainingSeconds: Int,
    totalSeconds: Int
) {
    // 포맷된 시간 계산
    val formattedTime = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // 진행률 계산
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds == 0) 0f
        else {
            val elapsed = totalSeconds - remainingSeconds
            elapsed.toFloat() / totalSeconds.toFloat()
        }
    }
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 배경 원
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 12.dp,
        )

        // 진행률 원
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = when (state) {
                FocusState.RUNNING -> MaterialTheme.colorScheme.primary
                FocusState.FINISHED -> MaterialTheme.colorScheme.tertiary
                FocusState.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            },
            strokeWidth = 12.dp,
        )

        // 시간 텍스트
        Text(
            text = if (state == FocusState.IDLE) "00:00" else formattedTime,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 프리셋 타이머 버튼
 *
 * @param presets 프리셋 시간 리스트 (분 단위)
 * @param onPresetClick 프리셋 클릭 콜백
 */
@Composable
fun PresetButtons(
    presets: List<Int>,
    onPresetClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "집중 시간 선택",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        presets.forEach { duration ->
            Button(
                onClick = { onPresetClick(duration) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "${duration}분",
                    style = MaterialTheme.typography.titleMedium
                )
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
    val (title, message) = when (error) {
        is TimerViewModel.PermissionError.AccessibilityServiceDisabled -> {
            "앱 차단 기능 권한 필요" to "집중 타이머를 사용하려면 앱 차단 기능(접근성 서비스)을 활성화해야 합니다.\n\n" +
                    "설정 화면에서 'Allday Detoxy'를 찾아 활성화해주세요."
        }
        is TimerViewModel.PermissionError.OverlayPermissionDenied -> {
            "잠금 화면 표시 권한 필요" to "집중 타이머를 사용하려면 잠금 화면 표시 권한(다른 앱 위에 표시)이 필요합니다.\n\n" +
                    "설정 화면에서 권한을 허용해주세요."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
