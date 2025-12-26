package com.allday.detoxy.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.data.local.converter.ScheduleInfoConverter
import com.allday.detoxy.data.local.converter.TodoResultConverter
import com.allday.detoxy.data.local.dao.FocusSessionTodoResultDao
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.dao.ScheduleGroupDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.data.local.entity.FocusSessionTodoResultEntity
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.TodoCompletionStatus
import com.allday.detoxy.domain.model.TodoStatus
import com.allday.detoxy.domain.util.TodoResultBuilder
import com.allday.detoxy.service.timer.FocusTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🆕 v9: 할일 관리 페이지 ViewModel
 * 
 * 계획된 할일과 완료된 세션 결과를 모두 보여주는 페이지의 ViewModel입니다.
 */
@HiltViewModel
class TodoManagementViewModel @Inject constructor(
    private val todoResultDao: FocusSessionTodoResultDao,
    private val timeBasedAutoRunDao: TimeBasedAutoRunDao,
    private val locationBasedAutoRunDao: LocationBasedAutoRunDao,
    private val scheduleGroupDao: ScheduleGroupDao
) : ViewModel() {
    
    companion object {
        private const val TAG = "TodoManagementViewModel"
    }
    
    private val scheduleInfoConverter = ScheduleInfoConverter()
    
    // 세션 활성 상태 (세션 중에는 수정 불가)
    val isSessionActive: StateFlow<Boolean> = FocusTimerService.state
        .map { it == FocusState.RUNNING }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // 🆕 계획된 할일 (스케줄에 정의된 할일)
    data class PlannedTodo(
        val scheduleId: String,
        val scheduleTitle: String,
        val scheduleInfo: ScheduleInfo,
        val isLocationBased: Boolean
    )
    
    val plannedTodos: StateFlow<List<PlannedTodo>> = combine(
        timeBasedAutoRunDao.getAll(),
        locationBasedAutoRunDao.getAll(),
        scheduleGroupDao.getAll()
    ) { timeBasedList, locationBasedList, scheduleGroups ->
        val result = mutableListOf<PlannedTodo>()
        
        // ScheduleGroup ID -> Name 맵
        val scheduleGroupNames = scheduleGroups.associate { it.id to it.name }
        
        // 시간 기반 스케줄에서 할일 추출
        timeBasedList.forEach { autoRun ->
            autoRun.scheduleInfoJson?.let { json ->
                try {
                    val scheduleInfo = scheduleInfoConverter.toScheduleInfo(json)
                    if (scheduleInfo != null && scheduleInfo.hasContent()) {
                        val groupId = autoRun.scheduleGroupId
                        val title = if (groupId != null) {
                            scheduleGroupNames[groupId] ?: autoRun.label ?: "알 수 없는 스케줄"
                        } else {
                            autoRun.label ?: "독립 스케줄"
                        }
                        
                        result.add(PlannedTodo(
                            scheduleId = groupId ?: autoRun.id,
                            scheduleTitle = title,
                            scheduleInfo = scheduleInfo,
                            isLocationBased = false
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse scheduleInfoJson: ${e.message}")
                }
            }
        }
        
        // 위치 기반 스케줄에서 할일 추출
        locationBasedList.forEach { autoRun ->
            autoRun.scheduleInfoJson?.let { json ->
                try {
                    val scheduleInfo = scheduleInfoConverter.toScheduleInfo(json)
                    if (scheduleInfo != null && scheduleInfo.hasContent()) {
                        val groupId = autoRun.linkedScheduleGroupId
                        val title = if (groupId != null) {
                            scheduleGroupNames[groupId] ?: autoRun.label
                        } else {
                            autoRun.label
                        }
                        
                        result.add(PlannedTodo(
                            scheduleId = groupId ?: autoRun.id,
                            scheduleTitle = title,
                            scheduleInfo = scheduleInfo,
                            isLocationBased = true
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse scheduleInfoJson: ${e.message}")
                }
            }
        }
        
        // 🔧 동일 scheduleId의 여러 시간대 할일을 병합
        result.groupBy { it.scheduleId }
            .map { (scheduleId, todos) ->
                if (todos.size == 1) {
                    todos.first()
                } else {
                    // 첫 번째 항목 기준으로 병합
                    val first = todos.first()
                    // 모든 목표와 할일을 합침
                    val mergedGoals = todos.mapNotNull { it.scheduleInfo.title.takeIf { t -> t.isNotBlank() } }.distinct()
                    val mergedTodos = todos.flatMap { it.scheduleInfo.todos }.distinctBy { it.content }
                    
                    PlannedTodo(
                        scheduleId = scheduleId,
                        scheduleTitle = first.scheduleTitle,
                        scheduleInfo = ScheduleInfo(
                            title = mergedGoals.joinToString(", "),  // 여러 목표 합침
                            todos = mergedTodos.take(10)  // 최대 10개까지
                        ),
                        isLocationBased = todos.any { it.isLocationBased }
                    )
                }
            }
    }
        .flowOn(Dispatchers.IO)  // 🔧 IO 스레드에서 실행하여 ANR 방지
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 완료된 세션 결과
    val todoResults: StateFlow<List<FocusSessionTodoResultEntity>> = 
        todoResultDao.getAllFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 🆕 오늘 완료된 세션 결과만 필터링
    private val todayResults: StateFlow<List<FocusSessionTodoResultEntity>> = todoResults
        .map { results ->
            val todayStart = java.time.LocalDate.now().atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val todayEnd = java.time.LocalDate.now().plusDays(1).atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            results.filter { it.completedAt in todayStart until todayEnd }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 🆕 필터 상태
    private val _currentFilter = MutableStateFlow(com.allday.detoxy.domain.model.TodoFilter.ALL)
    val currentFilter: StateFlow<com.allday.detoxy.domain.model.TodoFilter> = _currentFilter.asStateFlow()
    
    fun setFilter(filter: com.allday.detoxy.domain.model.TodoFilter) {
        _currentFilter.value = filter
    }
    
    // 🆕 오늘의 할일 통합 데이터 (스케줄별 그룹화)
    val todayScheduleGroups: StateFlow<List<com.allday.detoxy.domain.model.TodayScheduleGroup>> = combine(
        plannedTodos,
        todayResults,
        _currentFilter
    ) { planned, results, filter ->
        buildTodayScheduleGroups(planned, results, filter)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 🆕 통계용 카운트
    val todayStats: StateFlow<TodayStats> = todayScheduleGroups
        .map { groups ->
            val allItems = groups.flatMap { it.items }
            TodayStats(
                total = allItems.size,
                pending = allItems.count { it.status == com.allday.detoxy.domain.model.TodayTodoStatus.PENDING },
                completed = allItems.count { it.status == com.allday.detoxy.domain.model.TodayTodoStatus.COMPLETED },
                incomplete = allItems.count { 
                    it.status == com.allday.detoxy.domain.model.TodayTodoStatus.NOT_COMPLETED ||
                    it.status == com.allday.detoxy.domain.model.TodayTodoStatus.NO_RESPONSE
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats())
    
    data class TodayStats(
        val total: Int = 0,
        val pending: Int = 0,
        val completed: Int = 0,
        val incomplete: Int = 0
    )
    
    /**
     * 오늘의 할일 통합 데이터 빌드
     */
    private fun buildTodayScheduleGroups(
        planned: List<PlannedTodo>,
        results: List<FocusSessionTodoResultEntity>,
        filter: com.allday.detoxy.domain.model.TodoFilter
    ): List<com.allday.detoxy.domain.model.TodayScheduleGroup> {
        val groupMap = mutableMapOf<String, MutableList<com.allday.detoxy.domain.model.TodayTodoItem>>()
        val titleMap = mutableMapOf<String, String>()
        val goalsMap = mutableMapOf<String, MutableList<String>>()
        val completedScheduleIds = results.map { it.scheduleId }.toSet()
        
        // 1. 오늘 완료된 세션 결과 추가
        results.forEach { sessionResult ->
            val todoStatuses = TodoResultConverter.fromJson(sessionResult.todoResultsJson)
            val scheduleId = sessionResult.scheduleId
            
            titleMap[scheduleId] = sessionResult.scheduleTitleSnapshot
            
            todoStatuses.forEach { status ->
                val todayStatus = when (status.status) {
                    TodoCompletionStatus.COMPLETED -> com.allday.detoxy.domain.model.TodayTodoStatus.COMPLETED
                    TodoCompletionStatus.NOT_COMPLETED -> com.allday.detoxy.domain.model.TodayTodoStatus.NOT_COMPLETED
                    TodoCompletionStatus.NO_RESPONSE -> com.allday.detoxy.domain.model.TodayTodoStatus.NO_RESPONSE
                }
                
                // 목표는 goalsMap에 추가
                if (status.isGoal) {
                    goalsMap.getOrPut(scheduleId) { mutableListOf() }.add(status.content)
                }
                
                val item = com.allday.detoxy.domain.model.TodayTodoItem(
                    scheduleId = scheduleId,
                    scheduleTitle = sessionResult.scheduleTitleSnapshot,
                    scheduledTime = null,
                    todoContent = status.content,
                    status = todayStatus,
                    isRequired = status.isRequired,
                    isGoal = status.isGoal,
                    sourceType = com.allday.detoxy.domain.model.TodoSourceType.SESSION_RESULT,
                    sessionId = sessionResult.sessionId,
                    originalTodoId = status.id
                )
                
                groupMap.getOrPut(scheduleId) { mutableListOf() }.add(item)
            }
        }
        
        // 2. 계획된 할일 중 아직 세션 안 한 것 추가 (대기 상태)
        planned.forEach { plannedTodo ->
            val scheduleId = plannedTodo.scheduleId
            
            // 이미 오늘 세션 완료된 스케줄은 제외
            if (scheduleId in completedScheduleIds) return@forEach
            
            titleMap[scheduleId] = plannedTodo.scheduleTitle
            
            // 목표 추가
            if (plannedTodo.scheduleInfo.title.isNotBlank()) {
                goalsMap.getOrPut(scheduleId) { mutableListOf() }.add(plannedTodo.scheduleInfo.title)
                
                val goalItem = com.allday.detoxy.domain.model.TodayTodoItem(
                    scheduleId = scheduleId,
                    scheduleTitle = plannedTodo.scheduleTitle,
                    scheduledTime = null, // TODO: 시간 정보 추가
                    todoContent = plannedTodo.scheduleInfo.title,
                    status = com.allday.detoxy.domain.model.TodayTodoStatus.PENDING,
                    isRequired = true,
                    isGoal = true,
                    sourceType = com.allday.detoxy.domain.model.TodoSourceType.PLANNED,
                    originalTodoId = "goal:$scheduleId"
                )
                groupMap.getOrPut(scheduleId) { mutableListOf() }.add(goalItem)
            }
            
            // 할일 추가
            plannedTodo.scheduleInfo.todos.forEach { todo ->
                val todoItem = com.allday.detoxy.domain.model.TodayTodoItem(
                    scheduleId = scheduleId,
                    scheduleTitle = plannedTodo.scheduleTitle,
                    scheduledTime = null,
                    todoContent = todo.content,
                    status = com.allday.detoxy.domain.model.TodayTodoStatus.PENDING,
                    isRequired = todo.isRequired,
                    isGoal = false,
                    sourceType = com.allday.detoxy.domain.model.TodoSourceType.PLANNED,
                    originalTodoId = todo.id
                )
                groupMap.getOrPut(scheduleId) { mutableListOf() }.add(todoItem)
            }
        }
        
        // 3. 그룹화 및 필터 적용
        return groupMap.map { (scheduleId, items) ->
            val filteredItems = when (filter) {
                com.allday.detoxy.domain.model.TodoFilter.ALL -> items
                com.allday.detoxy.domain.model.TodoFilter.PENDING -> 
                    items.filter { it.status == com.allday.detoxy.domain.model.TodayTodoStatus.PENDING }
                com.allday.detoxy.domain.model.TodoFilter.COMPLETED -> 
                    items.filter { it.status == com.allday.detoxy.domain.model.TodayTodoStatus.COMPLETED }
                com.allday.detoxy.domain.model.TodoFilter.INCOMPLETE -> 
                    items.filter { 
                        it.status == com.allday.detoxy.domain.model.TodayTodoStatus.NOT_COMPLETED ||
                        it.status == com.allday.detoxy.domain.model.TodayTodoStatus.NO_RESPONSE
                    }
            }
            
            com.allday.detoxy.domain.model.TodayScheduleGroup(
                scheduleId = scheduleId,
                scheduleTitle = titleMap[scheduleId] ?: "알 수 없는 스케줄",
                scheduledTime = null, // TODO: 시간 정보 추가
                isLocationBased = planned.any { it.scheduleId == scheduleId && it.isLocationBased },
                goals = goalsMap[scheduleId]?.distinct() ?: emptyList(),
                items = filteredItems.filter { !it.isGoal } // 목표는 별도 표시하므로 제외
            )
        }.filter { it.items.isNotEmpty() || it.goals.isNotEmpty() }
    }
    
    // 수정 가능한 항목 수 (배너용)
    val editableItemsCount: StateFlow<Int> = todoResults
        .map { results ->
            results.sumOf { result ->
                val statuses = TodoResultConverter.fromJson(result.todoResultsJson)
                statuses.count { todoStatus -> isEditable(todoStatus, statuses) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 수정 플로우용 상태
    private val _selectedResultForEdit = MutableStateFlow<FocusSessionTodoResultEntity?>(null)
    private val _selectedTodoForEdit = MutableStateFlow<TodoStatus?>(null)
    val selectedTodoForEdit: StateFlow<TodoStatus?> = _selectedTodoForEdit.asStateFlow()
    
    /**
     * 수정할 항목 선택 (TodoResultCard에서 호출)
     */
    fun startEditTodo(result: FocusSessionTodoResultEntity, todoStatus: TodoStatus) {
        _selectedResultForEdit.value = result
        _selectedTodoForEdit.value = todoStatus
    }
    
    /**
     * 수정 확정 (EditTodoResultDialog에서 호출)
     */
    fun confirmEdit(newStatus: TodoCompletionStatus) {
        val result = _selectedResultForEdit.value ?: return
        val todoStatus = _selectedTodoForEdit.value ?: return
        
        viewModelScope.launch {
            try {
                val statuses = TodoResultConverter.fromJson(result.todoResultsJson)
                
                val updatedStatuses = statuses.map { 
                    if (it.id == todoStatus.id) it.copy(status = newStatus) else it
                }
                
                // 목표+할일 존재 시 목표 상태 재계산
                val hasTodos = statuses.any { !it.isGoal }
                val finalStatuses = if (!todoStatus.isGoal && hasTodos) {
                    // 할일 수정 시 목표 상태 재계산
                    recalculateGoalStatus(updatedStatuses)
                } else {
                    updatedStatuses
                }
                
                todoResultDao.updateResults(
                    sessionId = result.sessionId,
                    json = TodoResultConverter.toJson(finalStatuses),
                    time = System.currentTimeMillis()
                )
                
                Log.i(TAG, "✅ Todo status updated: ${todoStatus.id} -> $newStatus")
                
                // 상태 초기화
                dismissEditDialog()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update todo status: ${e.message}", e)
            }
        }
    }
    
    /**
     * 수정 다이얼로그 닫기
     */
    fun dismissEditDialog() {
        _selectedResultForEdit.value = null
        _selectedTodoForEdit.value = null
    }
    
    /**
     * 목표 상태 재계산 (할일 기반)
     */
    private fun recalculateGoalStatus(statuses: List<TodoStatus>): List<TodoStatus> {
        val goal = statuses.find { it.isGoal } ?: return statuses
        val todos = statuses.filter { !it.isGoal }
        
        if (todos.isEmpty()) return statuses
        
        val newGoalStatus = TodoResultBuilder.deriveGoalStatusFromTodos(todos)
        
        return statuses.map { 
            if (it.isGoal) it.copy(status = newGoalStatus) else it
        }
    }
    
    /**
     * 항목 수정 가능 여부 판단
     * 
     * - 완료 상태는 수정 불필요
     * - 목표+할일 존재 시 목표 직접 수정 불가 (자동 도출)
     * - NO_RESPONSE 또는 NOT_COMPLETED만 수정 가능
     */
    fun isEditable(todoStatus: TodoStatus, allStatuses: List<TodoStatus>): Boolean {
        val hasTodos = allStatuses.any { !it.isGoal }
        
        // 목표+할일 존재 시: 목표 직접 수정 불가 (할일만 수정 가능)
        if (todoStatus.isGoal && hasTodos) {
            return false
        }
        
        // 완료 상태는 수정 불필요
        return todoStatus.status == TodoCompletionStatus.NO_RESPONSE ||
               todoStatus.status == TodoCompletionStatus.NOT_COMPLETED
    }
}
