# Phase 6. 할일 관리 페이지 상세 작업 지시서

> **상위 문서**: [할 일 기능 작업 계획](../10_추가기능_할일관리기능_todolist.md)  
> **기능 설계**: [할 일 기능 설계 문서](../10_추가기능_할일관리기능.md)

---

## 6.1 페이지 UI 구현

### 6.1.1 스케줄 탭 내 서브 탭 추가

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/schedule/ScheduleScreen.kt`

```kotlin
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("전체 스케줄", "📋 할일 관리")
    
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> ScheduleListContent()
            1 -> TodoManagementScreen()
        }
    }
}
```

### 6.1.2 TodoManagementScreen

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/todo/TodoManagementScreen.kt`

```kotlin
@Composable
fun TodoManagementScreen(
    viewModel: TodoManagementViewModel = hiltViewModel()
) {
    val todoResults by viewModel.todoResults.collectAsState()
    val editableItems by viewModel.editableItems.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 세션 중 접근 제한
        if (isSessionActive) {
            SessionActiveBanner()
            return
        }
        
        // 수정 필요 항목 배너
        if (editableItems.isNotEmpty()) {
            EditableItemsBanner(count = editableItems.size)
        }
        
        // 결과 목록 (스케줄별 그룹화)
        LazyColumn {
            todoResults.groupBy { it.scheduleTitleSnapshot }.forEach { (title, results) ->
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                items(results) { result ->
                    // 🔧 수정: 개별 할일 항목까지 표시하고 선택 가능하게
                    TodoResultCard(
                        result = result,
                        // 🔧 수정: result + todoStatus를 전달
                        onEditTodo = { todoStatus -> 
                            viewModel.startEditTodo(result, todoStatus) 
                        }
                    )
                }
            }
        }
        
        // 🔧 수정 다이얼로그 표시
        val selectedTodo by viewModel.selectedTodoForEdit.collectAsState()
        selectedTodo?.let { todo ->
            EditTodoResultDialog(
                todoStatus = todo,
                onComplete = { newStatus -> viewModel.confirmEdit(newStatus) },
                onDismiss = { viewModel.dismissEditDialog() }
            )
        }
    }
}

@Composable
fun SessionActiveBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("집중 세션 중에는 할일 관리를 사용할 수 없습니다")
        }
    }
}

// 🔧 8차 리뷰: TodoResultCard Composable 정의 추가
@Composable
fun TodoResultCard(
    result: FocusSessionTodoResultEntity,
    onEditTodo: (TodoStatus) -> Unit
) {
    val todoStatuses = remember(result) { parseTodoStatuses(result.todoResultsJson) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 세션 날짜 표시
            Text(
                text = formatDate(result.completedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 각 할일 항목 표시
            todoStatuses.forEach { todoStatus ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditable(todoStatus, todoStatuses)) { 
                            onEditTodo(todoStatus) 
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (todoStatus.status) {
                            TodoCompletionStatus.COMPLETED -> Icons.Default.CheckCircle
                            TodoCompletionStatus.NOT_COMPLETED -> Icons.Default.Cancel
                            TodoCompletionStatus.NO_RESPONSE -> Icons.Default.Help
                        },
                        contentDescription = null,
                        tint = when (todoStatus.status) {
                            TodoCompletionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            TodoCompletionStatus.NOT_COMPLETED -> MaterialTheme.colorScheme.error
                            TodoCompletionStatus.NO_RESPONSE -> MaterialTheme.colorScheme.outline
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (todoStatus.isGoal) "🎯 ${todoStatus.content}" else todoStatus.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
```

---

## 6.2 수정 기능 구현

```kotlin
// 🔧 7차 리뷰: 수정 가능 여부 판단 (자동 도출 규칙 고려)
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

// 🔧 TypeToken을 사용한 안전한 JSON 파싱 헬퍼
private val todoStatusListType = object : TypeToken<List<TodoStatus>>() {}.type

fun parseTodoStatuses(json: String): List<TodoStatus> {
    return try {
        Gson().fromJson(json, todoStatusListType)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse todoResultsJson", e)
        emptyList()
    }
}

// ViewModel에서
val editableItems: StateFlow<List<TodoStatus>> = todoResults
    .map { results ->
        results.flatMap { result ->
            // 🔧 TypeToken 기반 파싱으로 타입 소거 문제 해결
            val statuses = parseTodoStatuses(result.todoResultsJson)
            // 🔧 8차 리뷰: isEditable 시그니처에 맞게 호출 수정
            statuses.filter { todoStatus -> isEditable(todoStatus, statuses) }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
```

### 6.2.2 수정 UI

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/todo/EditTodoResultDialog.kt`

```kotlin
@Composable
fun EditTodoResultDialog(
    todoStatus: TodoStatus,
    onComplete: (TodoCompletionStatus) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(24.dp)) {
                // 헤더
                if (todoStatus.isGoal) {
                    Text("🎯 목표 수정", style = MaterialTheme.typography.titleLarge)
                } else {
                    Text("📝 할일 수정", style = MaterialTheme.typography.titleLarge)
                }
                
                Text(todoStatus.content, style = MaterialTheme.typography.bodyLarge)
                
                // 현재 상태 표시
                val statusText = when (todoStatus.status) {
                    TodoCompletionStatus.NOT_COMPLETED -> "미완료"
                    TodoCompletionStatus.NO_RESPONSE -> "미응답"
                    else -> "완료"
                }
                Text("현재 상태: $statusText")
                
                Divider()
                
                // 수정 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { onComplete(TodoCompletionStatus.COMPLETED) }) {
                        Text("✅ 완료")
                    }
                    OutlinedButton(onClick = { onComplete(TodoCompletionStatus.NOT_COMPLETED) }) {
                        Text("❌ 미완료")
                    }
                }
            }
        }
    }
}
```

### 6.2.3 ViewModel 수정 로직

```kotlin
@HiltViewModel
class TodoManagementViewModel @Inject constructor(
    private val todoResultDao: FocusSessionTodoResultDao,
    private val sessionManager: FocusSessionManager
) : ViewModel() {
    
    val isSessionActive: StateFlow<Boolean> = sessionManager.isActive
    
    // 🔧 8차 리뷰: todoResults StateFlow 정의 추가
    val todoResults: StateFlow<List<FocusSessionTodoResultEntity>> = 
        todoResultDao.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 🔧 TypeToken 기반 파싱
    private val todoStatusListType = object : TypeToken<List<TodoStatus>>() {}.type
    
    // 🔧 수정 플로우를 위한 상태 추가
    private val _selectedResultForEdit = MutableStateFlow<FocusSessionTodoResultEntity?>(null)
    private val _selectedTodoForEdit = MutableStateFlow<TodoStatus?>(null)
    val selectedTodoForEdit: StateFlow<TodoStatus?> = _selectedTodoForEdit.asStateFlow()
    
    val showEditDialog: StateFlow<Boolean> = _selectedTodoForEdit
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    // 🔧 1단계: 수정할 항목 선택 (TodoResultCard에서 호출)
    fun startEditTodo(result: FocusSessionTodoResultEntity, todoStatus: TodoStatus) {
        _selectedResultForEdit.value = result
        _selectedTodoForEdit.value = todoStatus
    }
    
    // 🔧 2단계: 수정 확정 (EditTodoResultDialog에서 호출)
    fun confirmEdit(newStatus: TodoCompletionStatus) {
        val result = _selectedResultForEdit.value ?: return
        val todoStatus = _selectedTodoForEdit.value ?: return
        
        viewModelScope.launch {
            val statuses: List<TodoStatus> = try {
                Gson().fromJson(result.todoResultsJson, todoStatusListType)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON", e)
                return@launch
            }
            
            val updatedStatuses = statuses.map { 
                if (it.id == todoStatus.id) it.copy(status = newStatus) else it
            }
            
            // 🔧 7차 리뷰: 목표+할일 존재 시 목표 상태 재계산
            // - 목표 직접 수정 시에도 할일 기반으로 재도출 (자동 도출 규칙 유지)
            // - 목표만 있는 경우에만 직접 수정 허용
            val hasTodos = statuses.any { !it.isGoal }
            val finalStatuses = if (todoStatus.isGoal && hasTodos) {
                // 목표+할일: 목표 직접 수정 무시, 할일 기반 재계산
                recalculateGoalStatus(updatedStatuses)
            } else if (!todoStatus.isGoal) {
                // 할일 수정: 목표 상태 재계산
                recalculateGoalStatus(updatedStatuses)
            } else {
                // 목표만: 직접 수정 허용
                updatedStatuses
            }
            
            todoResultDao.updateResults(
                sessionId = result.sessionId,
                json = Gson().toJson(finalStatuses),
                time = System.currentTimeMillis()
            )
            
            // 상태 초기화
            dismissEditDialog()
        }
    }
    
    fun dismissEditDialog() {
        _selectedResultForEdit.value = null
        _selectedTodoForEdit.value = null
    }
    
    private fun recalculateGoalStatus(statuses: List<TodoStatus>): List<TodoStatus> {
        val goal = statuses.find { it.isGoal } ?: return statuses
        val todos = statuses.filter { !it.isGoal }
        
        if (todos.isEmpty()) return statuses
        
        val newGoalStatus = deriveGoalStatusFromTodos(todos)
        
        return statuses.map { 
            if (it.isGoal) it.copy(status = newGoalStatus) else it
        }
    }
}
```

---

## 6.3 디톡스 보호 장치

### 6.3.1 세션 중 접근 제한

```kotlin
// TodoManagementScreen에서 isSessionActive 확인
if (isSessionActive) {
    SessionActiveBanner()
    // 모든 인터랙션 비활성화
    return
}
```

### 6.3.2 확인 vs 관리 분리 원칙

| 기능 | 위치 |
|------|------|
| 할일 확인 | 오버레이 (읽기 전용) |
| 할일 체크 | 세션 종료 다이얼로그 |
| 결과 수정 | 할일 관리 페이지 (세션 외) |

---

## 검증 체크리스트

```bash
# 빌드 확인
./gradlew assembleDebug
```

### 수동 QA

1. 미응답/미완료 항목이 있는 결과 생성
2. 스케줄 탭 → 할일 관리 서브탭 진입
3. 수정 필요 항목 배너 확인
4. 항목 클릭 → 수정 다이얼로그 표시
5. 완료/미완료 선택 → 저장 확인
6. 세션 중 접근 제한 확인

---

## 완료 기준

- [ ] 스케줄 탭 내 서브 탭으로 "할일 관리" 추가
- [ ] TodoManagementScreen Composable 생성
- [ ] 수정 가능 항목 필터링 (NO_RESPONSE + NOT_COMPLETED)
- [ ] 목표(isGoal=true) 수정 가능
- [ ] EditTodoResultDialog 구현
- [ ] lastModifiedAt 업데이트
- [ ] 세션 중 접근 제한 동작

---

*작성일: 2025-12-25 · Phase 6 상세 작업 지시서*
