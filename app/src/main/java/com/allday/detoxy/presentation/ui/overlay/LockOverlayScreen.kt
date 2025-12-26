package com.allday.detoxy.presentation.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.runtime.MutableState
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.presentation.overlay.OverlayDisplayMode
import com.allday.detoxy.presentation.overlay.OverlayDisplayRules
import com.allday.detoxy.presentation.overlay.truncateForOverlay
import kotlinx.coroutines.delay

/**
 * 오버레이 잠금 화면
 *
 * 타이머 실행 중 차단 앱 접근 시 표시되는 전체 화면 오버레이입니다.
 * 🆕 v9: 목표/할일 표시 기능 추가
 *
 * @param remainingSeconds 남은 시간 (초)
 * @param totalSeconds 전체 시간 (초)
 * @param timerState 타이머 상태
 * @param onGiveUp 포기 버튼 클릭 콜백
 * @param scheduleInfo 스케줄 정보 (목표/할일)
 * @param displayMode 표시 모드
 */
@Composable
fun LockOverlayScreen(
    remainingSeconds: Int,
    totalSeconds: Int,
    @Suppress("UNUSED_PARAMETER") timerState: MutableState<Int>,
    onGiveUp: () -> Unit,
    scheduleInfo: ScheduleInfo? = null,
    displayMode: OverlayDisplayMode = OverlayDisplayMode.GOAL_ONLY
) {
    // 화면 자체에서 독립적으로 타이머 관리
    var currentSeconds by remember { mutableStateOf(remainingSeconds) }

    // LaunchedEffect로 자체 타이머 구동
    LaunchedEffect(Unit) {
        Log.d("LockOverlayScreen", "Starting independent timer - initial: $currentSeconds seconds")
        while (currentSeconds > 0) {
            delay(1000)
            currentSeconds--
            if (currentSeconds % 5 == 0 || currentSeconds <= 5) {
                Log.d("LockOverlayScreen", "🕒 Timer update: $currentSeconds seconds remaining")
            }
        }
        Log.d("LockOverlayScreen", "✅ Timer finished")
    }

    Log.d("LockOverlayScreen", "Screen rendered - currentSeconds: $currentSeconds, totalSeconds: $totalSeconds")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // 잠금 아이콘/메시지
            Text(
                text = "🔒",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "집중 모드 실행 중",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 🆕 v9: 목표/할일 섹션
            GoalTodoSection(
                scheduleInfo = scheduleInfo,
                displayMode = displayMode
            )

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
                    text = formatTime(currentSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 56.sp
                )

                // 진행률 표시
                LinearProgressIndicator(
                    progress = { getProgress(currentSeconds, totalSeconds) },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

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
 * 🆕 v9: 목표/할일 표시 섹션
 */
@Composable
private fun GoalTodoSection(
    scheduleInfo: ScheduleInfo?,
    displayMode: OverlayDisplayMode
) {
    when (displayMode) {
        OverlayDisplayMode.HIDDEN -> {
            // 아무것도 표시하지 않음
        }
        
        OverlayDisplayMode.EMOJI_ONLY -> {
            // 🎯 이모지만 표시 (프라이버시 보호)
            Text(
                text = "🎯",
                fontSize = 48.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        OverlayDisplayMode.GOAL_ONLY -> {
            // 목표만 표시
            if (scheduleInfo?.title?.isNotBlank() == true) {
                GoalCard(
                    title = scheduleInfo.title,
                    todoCount = scheduleInfo.todos.size
                )
            }
        }
        
        OverlayDisplayMode.GOAL_AND_TODOS -> {
            // 목표 + 할일 목록 표시
            if (scheduleInfo?.hasContent() == true) {
                GoalWithTodosCard(scheduleInfo = scheduleInfo)
            }
        }
    }
}

/**
 * 목표만 표시하는 카드
 */
@Composable
private fun GoalCard(
    title: String,
    todoCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯 오늘의 목표",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = truncateForOverlay(title, OverlayDisplayRules.MAX_GOAL_TEXT_LENGTH),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (todoCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "할일 ${todoCount}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 목표 + 할일 목록 표시하는 카드
 */
@Composable
private fun GoalWithTodosCard(
    scheduleInfo: ScheduleInfo
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더
            Text(
                text = "📋 지금 해야 할 일",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 목표
            if (scheduleInfo.title.isNotBlank()) {
                Text(
                    text = truncateForOverlay(scheduleInfo.title, OverlayDisplayRules.MAX_GOAL_TEXT_LENGTH),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 할일 목록 (최대 3개)
            val displayTodos = scheduleInfo.todos.take(OverlayDisplayRules.MAX_TODO_DISPLAY_COUNT)
            displayTodos.forEach { todo ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "□",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = truncateForOverlay(todo.content, OverlayDisplayRules.MAX_TODO_TEXT_LENGTH),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    if (todo.isRequired) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "필수",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // 더 있으면 "+N개 더" 표시
            val remaining = scheduleInfo.todos.size - OverlayDisplayRules.MAX_TODO_DISPLAY_COUNT
            if (remaining > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "...외 ${remaining}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
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
