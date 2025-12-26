package com.allday.detoxy.presentation.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.data.local.converter.TodoResultConverter
import com.allday.detoxy.data.local.entity.FocusSessionTodoResultEntity
import com.allday.detoxy.domain.model.TodoCompletionStatus
import com.allday.detoxy.domain.model.TodoStatus
import com.allday.detoxy.presentation.viewmodel.TodoManagementViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🆕 v9: 할일 관리 페이지
 * 
 * 세션 완료 후 미응답/미완료 항목을 수정할 수 있는 페이지입니다.
 * 세션 중에는 접근이 제한됩니다.
 * 
 * @param viewModel TodoManagementViewModel
 */
@Composable
fun TodoManagementScreen(
    viewModel: TodoManagementViewModel = hiltViewModel()
) {
    // 🔧 모든 State 수집을 함수 시작 시점에서 수행 (리컴포지션 안정성)
    val todoResults by viewModel.todoResults.collectAsState()
    val editableItemsCount by viewModel.editableItemsCount.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val selectedTodo by viewModel.selectedTodoForEdit.collectAsState()
    val plannedTodos by viewModel.plannedTodos.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 세션 중 안내 배너 (수정 불가 안내만, 목록은 표시)
        if (isSessionActive) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "집중 세션 중에는 결과 수정이 제한됩니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        // 수정 필요 항목 배너 (세션 중이 아닐 때만)
        if (!isSessionActive && editableItemsCount > 0) {
            EditableItemsBanner(count = editableItemsCount)
        }
        
        // 빈 상태 (계획된 할일도 없고 완료된 결과도 없을 때)
        if (todoResults.isEmpty() && plannedTodos.isEmpty()) {
            EmptyTodoResultsContent()
        } else {
            // 목록 (계획된 할일 + 완료된 결과)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 📋 계획된 할일 섹션
                if (plannedTodos.isNotEmpty()) {
                    item(key = "planned_header") {
                        Text(
                            text = "📋 계획된 할일",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(plannedTodos, key = { "planned_${it.scheduleId}" }) { planned ->
                        PlannedTodoCard(planned = planned)
                    }
                    
                    // 구분선
                    if (todoResults.isNotEmpty()) {
                        item(key = "divider") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                }
                
                // ✅ 완료된 세션 결과 섹션
                if (todoResults.isNotEmpty()) {
                    item(key = "results_header") {
                        Text(
                            text = "✅ 완료된 세션 결과",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // scheduleId로 그룹화하여 동일 제목이지만 다른 스케줄 구분
                    val groupedResults = todoResults.groupBy { it.scheduleId }
                    
                    groupedResults.forEach { (scheduleId, results) ->
                        val firstResult = results.firstOrNull() ?: return@forEach
                        val title = firstResult.scheduleTitleSnapshot
                        val dateRange = if (results.size > 1) {
                            val oldest = formatDateShort(results.minOf { it.completedAt })
                            val newest = formatDateShort(results.maxOf { it.completedAt })
                            "$oldest ~ $newest"
                        } else {
                            formatDateShort(firstResult.completedAt)
                        }
                        
                        item(key = "result_header_$scheduleId") {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📅 $dateRange · ${results.size}회",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        items(results, key = { "result_${it.sessionId}" }) { result ->
                            TodoResultCard(
                                result = result,
                                viewModel = viewModel,
                                isEditEnabled = !isSessionActive,  // 🔧 세션 중에는 수정 버튼 비활성화
                                onEditTodo = { todoStatus ->
                                    viewModel.startEditTodo(result, todoStatus)
                                }
                            )
                        }
                    }
                }
                
                // 하단 여백
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // 수정 다이얼로그
        selectedTodo?.let { todo ->
            EditTodoResultDialog(
                todoStatus = todo,
                onComplete = { newStatus -> viewModel.confirmEdit(newStatus) },
                onDismiss = { viewModel.dismissEditDialog() }
            )
        }
    }
}

/**
 * 세션 활성 배너
 */
@Composable
fun SessionActiveBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "집중 세션 중에는 할일 관리를 사용할 수 없습니다",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * 수정 필요 항목 배너
 */
@Composable
fun EditableItemsBanner(count: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "📝 수정 필요한 항목 ${count}개",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * 빈 상태 컨텐츠
 */
@Composable
fun EmptyTodoResultsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "아직 완료한 세션이 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "집중 세션을 완료하면 할일 결과가 여기에 표시됩니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 계획된 할일 카드
 */
@Composable
fun PlannedTodoCard(
    planned: TodoManagementViewModel.PlannedTodo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 스케줄 제목 & 타입
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planned.scheduleTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (planned.isLocationBased) "📍 위치 기반" else "⏰ 시간 기반",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 목표 표시 (title이 있는 경우)
            if (planned.scheduleInfo.title.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 ${planned.scheduleInfo.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 할일 목록 표시
            if (planned.scheduleInfo.todos.isNotEmpty()) {
                planned.scheduleInfo.todos.forEach { todo ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = todo.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 세션 할일 결과 카드
 */
@Composable
fun TodoResultCard(
    result: FocusSessionTodoResultEntity,
    viewModel: TodoManagementViewModel,
    isEditEnabled: Boolean = true,  // 🔧 세션 중일 때 false
    onEditTodo: (TodoStatus) -> Unit
) {
    val todoStatuses = remember(result.todoResultsJson) { 
        TodoResultConverter.fromJson(result.todoResultsJson) 
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 세션 날짜 표시
            Text(
                text = formatDate(result.completedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 각 할일 항목 표시
            todoStatuses.forEach { todoStatus ->
                val isEditable = isEditEnabled && viewModel.isEditable(todoStatus, todoStatuses)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isEditable) {
                                Modifier.clickable { onEditTodo(todoStatus) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 상태 아이콘
                    Icon(
                        imageVector = when (todoStatus.status) {
                            TodoCompletionStatus.COMPLETED -> Icons.Default.CheckCircle
                            TodoCompletionStatus.NOT_COMPLETED -> Icons.Default.Close
                            TodoCompletionStatus.NO_RESPONSE -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (todoStatus.status) {
                            TodoCompletionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            TodoCompletionStatus.NOT_COMPLETED -> MaterialTheme.colorScheme.error
                            TodoCompletionStatus.NO_RESPONSE -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // 항목 내용
                    Text(
                        text = if (todoStatus.isGoal) "🎯 ${todoStatus.content}" else todoStatus.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 수정 가능 표시
                    if (isEditable) {
                        Text(
                            text = "수정",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 날짜 포맷 (상세)
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREAN)
    return dateFormat.format(Date(timestamp))
}

/**
 * 날짜 포맷 (간략)
 */
private fun formatDateShort(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("M/d", Locale.KOREAN)
    return dateFormat.format(Date(timestamp))
}
