package com.allday.detoxy.presentation.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val timerState by viewModel.timerState.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()

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
            formattedTime = viewModel.getFormattedTime(),
            progress = viewModel.getProgress()
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
 * @param formattedTime 포맷된 시간 (MM:SS)
 * @param progress 진행률 (0.0 ~ 1.0)
 */
@Composable
fun CircularTimerDisplay(
    state: FocusState,
    formattedTime: String,
    progress: Float
) {
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
