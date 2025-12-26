package com.allday.detoxy.presentation.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.allday.detoxy.domain.model.TodoCompletionStatus
import com.allday.detoxy.domain.model.TodoStatus

/**
 * 🆕 v9: 할일 결과 수정 다이얼로그
 * 
 * 미응답/미완료 상태의 할일 항목을 완료/미완료로 수정할 수 있습니다.
 * 
 * @param todoStatus 수정할 할일 상태
 * @param onComplete 수정 완료 콜백 (새 상태 전달)
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun EditTodoResultDialog(
    todoStatus: TodoStatus,
    onComplete: (TodoCompletionStatus) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    text = if (todoStatus.isGoal) "🎯 목표 수정" else "📝 할일 수정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 할일 내용
                Text(
                    text = todoStatus.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 현재 상태 표시
                val statusText = when (todoStatus.status) {
                    TodoCompletionStatus.NOT_COMPLETED -> "❌ 미완료"
                    TodoCompletionStatus.NO_RESPONSE -> "❓ 미응답"
                    TodoCompletionStatus.COMPLETED -> "✅ 완료"
                }
                val statusColor = when (todoStatus.status) {
                    TodoCompletionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    TodoCompletionStatus.NOT_COMPLETED -> MaterialTheme.colorScheme.error
                    TodoCompletionStatus.NO_RESPONSE -> MaterialTheme.colorScheme.outline
                }
                
                Text(
                    text = "현재 상태: $statusText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "새 상태 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 수정 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onComplete(TodoCompletionStatus.COMPLETED) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("✅ 완료")
                    }
                    
                    OutlinedButton(
                        onClick = { onComplete(TodoCompletionStatus.NOT_COMPLETED) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("❌ 미완료")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 취소 버튼
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("취소")
                }
            }
        }
    }
}
