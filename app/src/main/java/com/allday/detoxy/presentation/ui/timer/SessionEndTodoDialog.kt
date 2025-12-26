package com.allday.detoxy.presentation.ui.timer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.TodoCompletionStatus
import kotlinx.coroutines.delay

/**
 * 🆕 v9: 세션 종료 할일 체크리스트 다이얼로그
 * 
 * 집중 세션 완료 시 할일 완료 여부를 체크하는 다이얼로그입니다.
 * 30초 타이머가 표시되며, 시간 초과 시 체크 상태에 따라 자동 저장됩니다.
 * 
 * @param scheduleInfo 스케줄 정보 (목표/할일)
 * @param onComplete 완료 버튼 클릭 또는 타임아웃 시 호출되는 콜백
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun SessionEndTodoDialog(
    scheduleInfo: ScheduleInfo,
    onComplete: (Map<String, TodoCompletionStatus>) -> Unit,
    onDismiss: () -> Unit
) {
    // 각 할일의 체크 상태
    var responses by remember { 
        mutableStateOf(scheduleInfo.todos.associate { it.id to false })
    }
    
    // 30초 카운트다운
    var remainingSeconds by remember { mutableStateOf(30) }
    
    // 이중 저장 방지 플래그
    var isCompleted by remember { mutableStateOf(false) }
    
    // 안전한 완료 처리 함수
    fun safeComplete(statusMap: Map<String, TodoCompletionStatus>) {
        if (!isCompleted) {
            isCompleted = true
            onComplete(statusMap)
        }
    }
    
    // 30초 타이머
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isCompleted) {
            delay(1000)
            remainingSeconds--
        }
        // 타임아웃 시 자동 저장 (아직 완료되지 않은 경우만)
        if (!isCompleted) {
            val statusMap = responses.mapValues { (id, checked) ->
                val todo = scheduleInfo.todos.find { it.id == id }
                when {
                    checked -> TodoCompletionStatus.COMPLETED
                    todo?.isRequired == true -> TodoCompletionStatus.NOT_COMPLETED
                    else -> TodoCompletionStatus.NO_RESPONSE
                }
            }
            safeComplete(statusMap)
        }
    }
    
    Dialog(onDismissRequest = { if (!isCompleted) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 헤더
                Text(
                    text = "🎉",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "집중 세션 완료!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 목표 표시 (있는 경우)
                if (scheduleInfo.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎯 ${scheduleInfo.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text(
                    text = "✅ 완료한 할 일을 체크하세요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 할일 체크리스트
                scheduleInfo.todos.forEach { todo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (!isCompleted) {
                                    // 리뷰 피드백: 강제 언랩 대신 안전한 접근
                                    val currentChecked = responses[todo.id] ?: false
                                    responses = responses + (todo.id to !currentChecked)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = responses[todo.id] ?: false,
                            onCheckedChange = { 
                                if (!isCompleted) {
                                    responses = responses + (todo.id to it) 
                                }
                            },
                            enabled = !isCompleted
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = todo.content,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (todo.isRequired) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Text("필수", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 타이머 표시
                Text(
                    text = "⏱️ ${remainingSeconds}초 후 현재 상태로 저장",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { remainingSeconds / 30f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "🔄 체크 안 한 항목: 필수=미완료, 일반=미응답",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 완료 버튼
                Button(
                    onClick = {
                        val statusMap = responses.mapValues { (id, checked) ->
                            if (checked) {
                                TodoCompletionStatus.COMPLETED
                            } else {
                                val todo = scheduleInfo.todos.find { it.id == id }
                                if (todo?.isRequired == true) {
                                    TodoCompletionStatus.NOT_COMPLETED
                                } else {
                                    TodoCompletionStatus.NO_RESPONSE
                                }
                            }
                        }
                        safeComplete(statusMap)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCompleted
                ) {
                    Text("완료")
                }
            }
        }
    }
}
