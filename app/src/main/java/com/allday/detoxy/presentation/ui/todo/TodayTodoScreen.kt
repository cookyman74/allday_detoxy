package com.allday.detoxy.presentation.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.domain.model.TodayScheduleGroup
import com.allday.detoxy.domain.model.TodayTodoItem
import com.allday.detoxy.domain.model.TodayTodoStatus
import com.allday.detoxy.domain.model.TodoFilter
import com.allday.detoxy.presentation.viewmodel.TodoManagementViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 🆕 v9.1: 오늘의 할일 통합 체크리스트 화면
 * 
 * 계획된 할일과 완료된 세션 결과를 통합하여 체크리스트 형태로 표시합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayTodoScreen(
    viewModel: TodoManagementViewModel = hiltViewModel()
) {
    val scheduleGroups by viewModel.todayScheduleGroups.collectAsState()
    val stats by viewModel.todayStats.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 헤더
        TodayHeader()
        
        // 세션 중 안내 배너
        if (isSessionActive) {
            TodaySessionActiveBanner()
        }
        
        // 필터 칩
        FilterChips(
            stats = stats,
            currentFilter = currentFilter,
            onFilterSelected = { viewModel.setFilter(it) }
        )
        
        // 할일 목록
        if (scheduleGroups.isEmpty()) {
            EmptyStateContent()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(scheduleGroups, key = { it.scheduleId }) { group ->
                    ScheduleGroupCard(
                        group = group,
                        isEditEnabled = !isSessionActive,
                        onItemClick = { item ->
                            // TODO: 수정 다이얼로그 표시
                        }
                    )
                }
                
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 오늘 날짜 헤더
 */
@Composable
private fun TodayHeader() {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "오늘의 할일",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = today.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 세션 진행 중 배너 (로컬)
 */
@Composable
private fun TodaySessionActiveBanner() {
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

/**
 * 필터 칩
 */
@Composable
private fun FilterChips(
    stats: TodoManagementViewModel.TodayStats,
    currentFilter: TodoFilter,
    onFilterSelected: (TodoFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == TodoFilter.ALL,
            onClick = { onFilterSelected(TodoFilter.ALL) },
            label = { Text("전체 ${stats.total}") }
        )
        FilterChip(
            selected = currentFilter == TodoFilter.PENDING,
            onClick = { onFilterSelected(TodoFilter.PENDING) },
            label = { Text("대기 ${stats.pending}") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        FilterChip(
            selected = currentFilter == TodoFilter.COMPLETED,
            onClick = { onFilterSelected(TodoFilter.COMPLETED) },
            label = { Text("완료 ${stats.completed}") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = currentFilter == TodoFilter.INCOMPLETE,
            onClick = { onFilterSelected(TodoFilter.INCOMPLETE) },
            label = { Text("미완료 ${stats.incomplete}") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
            )
        )
    }
}

/**
 * 스케줄별 그룹 카드
 */
@Composable
private fun ScheduleGroupCard(
    group: TodayScheduleGroup,
    isEditEnabled: Boolean,
    onItemClick: (TodayTodoItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 스케줄 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (group.isLocationBased) Icons.Default.Place else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.scheduleTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 시간 표시
                group.scheduledTime?.let { time ->
                    Text(
                        text = "⏰ ${time.hour}:${time.minute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 목표 표시 (있는 경우)
            if (group.goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎯", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.goals.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // 할일 목록
            if (group.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                group.items.forEach { item ->
                    TodoItemRow(
                        item = item,
                        isEditEnabled = isEditEnabled,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

/**
 * 개별 할일 아이템 행
 */
@Composable
private fun TodoItemRow(
    item: TodayTodoItem,
    isEditEnabled: Boolean,
    onClick: () -> Unit
) {
    val isEditable = isEditEnabled && (
        item.status == TodayTodoStatus.NOT_COMPLETED || 
        item.status == TodayTodoStatus.NO_RESPONSE
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEditable) Modifier.clickable { onClick() } else Modifier
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 상태 아이콘
        StatusIcon(status = item.status)
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 내용
        Text(
            text = if (item.isRequired) "${item.todoContent} [필수]" else item.todoContent,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textDecoration = if (item.status == TodayTodoStatus.COMPLETED) TextDecoration.LineThrough else null,
            color = when (item.status) {
                TodayTodoStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                TodayTodoStatus.PENDING -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.error
            }
        )
        
        // 상태 라벨
        StatusLabel(status = item.status, isEditable = isEditable)
    }
}

/**
 * 상태 아이콘
 */
@Composable
private fun StatusIcon(status: TodayTodoStatus) {
    val (icon, tint) = when (status) {
        TodayTodoStatus.PENDING -> Icons.Default.Info to MaterialTheme.colorScheme.outline
        TodayTodoStatus.COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        TodayTodoStatus.NOT_COMPLETED -> Icons.Default.Close to MaterialTheme.colorScheme.error
        TodayTodoStatus.NO_RESPONSE -> Icons.Default.Info to MaterialTheme.colorScheme.outline
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

/**
 * 상태 라벨
 */
@Composable
private fun StatusLabel(status: TodayTodoStatus, isEditable: Boolean) {
    val (text, color) = when (status) {
        TodayTodoStatus.PENDING -> "대기" to MaterialTheme.colorScheme.surfaceVariant
        TodayTodoStatus.COMPLETED -> "완료" to MaterialTheme.colorScheme.primaryContainer
        TodayTodoStatus.NOT_COMPLETED -> "미완료" to MaterialTheme.colorScheme.errorContainer
        TodayTodoStatus.NO_RESPONSE -> "미응답" to MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = when (status) {
                    TodayTodoStatus.NOT_COMPLETED -> MaterialTheme.colorScheme.onErrorContainer
                    TodayTodoStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (isEditable) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "수정",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 빈 상태 화면
 */
@Composable
private fun EmptyStateContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "오늘 등록된 할일이 없습니다",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "스케줄에 할일을 추가해보세요!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
