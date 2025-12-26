package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allday.detoxy.domain.model.ScheduleTodo

/**
 * 할일 목록 섹션
 * 
 * 스케줄에 연결된 세부 할일 목록을 표시하고 편집합니다.
 * 
 * @param todos 현재 할일 목록
 * @param onTodoChange 할일 변경 콜백 (index, todo)
 * @param onTodoDelete 할일 삭제 콜백 (index)
 * @param onTodoAdd 할일 추가 콜백
 * @param onHide 섹션 숨기기 콜백
 * @param modifier Modifier
 */
@Composable
fun TodoListSection(
    todos: List<ScheduleTodo>,
    onTodoChange: (Int, ScheduleTodo) -> Unit,
    onTodoDelete: (Int) -> Unit,
    onTodoAdd: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxTodoCount = ScheduleTodo.MAX_TODO_COUNT
    val remainingCount = maxTodoCount - todos.size
    val canAddMore = remainingCount > 0
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 섹션 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 세부 할일 (${todos.size}/$maxTodoCount)",
                style = MaterialTheme.typography.titleSmall
            )
            
            // 섹션 숨기기 버튼
            TextButton(onClick = onHide) {
                Text("접기", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 할일 목록
        todos.forEachIndexed { index, todo ->
            TodoItemRow(
                todo = todo,
                index = index,
                onTodoChange = { updatedTodo -> onTodoChange(index, updatedTodo) },
                onDelete = { onTodoDelete(index) }
            )
            if (index < todos.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // 할일 추가 버튼
        if (canAddMore) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onTodoAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("할일 추가 (${remainingCount}개 남음)")
            }
        }
    }
}

/**
 * 개별 할일 항목 Row
 * 
 * @param todo 할일 데이터
 * @param index 순번 (표시용)
 * @param onTodoChange 변경 콜백
 * @param onDelete 삭제 콜백
 */
@Composable
fun TodoItemRow(
    todo: ScheduleTodo,
    index: Int,
    onTodoChange: (ScheduleTodo) -> Unit,
    onDelete: () -> Unit
) {
    val maxLength = ScheduleTodo.MAX_CONTENT_LENGTH
    val isNearLimit = todo.content.length > maxLength - 10
    val isOverLimit = todo.content.length > maxLength
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순번
        Text(
            text = "${index + 1}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        
        // 내용 입력
        OutlinedTextField(
            value = todo.content,
            onValueChange = { newContent ->
                if (newContent.length <= maxLength) {
                    onTodoChange(todo.copy(content = newContent))
                }
            },
            placeholder = { Text("할일 입력") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            isError = isOverLimit,
            colors = OutlinedTextFieldDefaults.colors(
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedBorderColor = if (isNearLimit) MaterialTheme.colorScheme.tertiary 
                                     else MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 필수 토글 칩
        FilterChip(
            selected = todo.isRequired,
            onClick = { onTodoChange(todo.copy(isRequired = !todo.isRequired)) },
            label = { Text("필수", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.height(32.dp)
        )
        
        // 삭제 버튼
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}
