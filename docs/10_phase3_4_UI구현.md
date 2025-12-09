# Phase 3-4. UI 구현 상세 작업 지시서

> 상위 문서: [할 일 기능 작업 계획](./10_추가_할일기능_todolist.md)  
> 기능 설계: [할 일 기능 설계 문서](./10_추가기능_할일관리기능.md)

---

## Phase 3. UI 생성/편집 (Day 4-5)

### 3.1 입력 UI 컴포넌트

#### 3.1.1 ScheduleInfoSection 컴포넌트

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleInfoSection.kt`

```kotlin
@Composable
fun ScheduleInfoSection(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    isExpanded: Boolean = true,
    onExpandChange: (Boolean) -> Unit = {}
) {
    CollapsibleCard(
        title = "📝 기본 정보",
        isExpanded = isExpanded,
        onExpandChange = onExpandChange
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 제목 입력
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("제목") },
                placeholder = { Text("스케줄 제목을 입력하세요") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 설명 입력
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("설명") },
                placeholder = { Text("이 스케줄에 대한 간단한 설명") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 메모 입력 (선택)
            OutlinedTextField(
                value = memo,
                onValueChange = onMemoChange,
                label = { Text("메모 (선택)") },
                placeholder = { Text("추가 메모나 참고 사항") },
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

#### 3.1.2 TodoListSection 컴포넌트

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/TodoListSection.kt`

```kotlin
@Composable
fun TodoListSection(
    todos: List<ScheduleTodo>,
    onAddTodo: () -> Unit,
    onUpdateTodo: (index: Int, ScheduleTodo) -> Unit,
    onDeleteTodo: (index: Int) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,  // 후순위: 드래그 정렬
    isExpanded: Boolean = true,
    onExpandChange: (Boolean) -> Unit = {}
) {
    val maxTodos = 10
    
    CollapsibleCard(
        title = "✅ 할 일 목록 (${todos.size}/$maxTodos)",
        isExpanded = isExpanded,
        onExpandChange = onExpandChange
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 안내 텍스트
            Text(
                text = "집중 세션 동안 완료할 할 일을 추가하세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Todo 리스트
            todos.forEachIndexed { index, todo ->
                TodoItemRow(
                    todo = todo,
                    onContentChange = { newContent ->
                        onUpdateTodo(index, todo.copy(content = newContent))
                    },
                    onRequiredChange = { isRequired ->
                        onUpdateTodo(index, todo.copy(isRequired = isRequired))
                    },
                    onDelete = { onDeleteTodo(index) }
                )
            }
            
            // 추가 버튼
            if (todos.size < maxTodos) {
                OutlinedButton(
                    onClick = onAddTodo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("할 일 추가")
                }
            }
        }
    }
}

@Composable
private fun TodoItemRow(
    todo: ScheduleTodo,
    onContentChange: (String) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(todo.content) { mutableStateOf(todo.content) }
    val isValid = editText.isNotBlank() && editText.length <= 120
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순서 표시
        Text(
            text = "${todo.orderIndex + 1}.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(24.dp)
        )
        
        // 내용 입력/표시
        if (isEditing) {
            OutlinedTextField(
                value = editText,
                onValueChange = { 
                    if (it.length <= 120) editText = it 
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = !isValid,
                supportingText = if (!isValid) {
                    { Text("1~120자 입력") }
                } else null,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (isValid) {
                                onContentChange(editText)
                                isEditing = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, "저장")
                    }
                }
            )
        } else {
            Text(
                text = todo.content.ifBlank { "(내용 없음)" },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isEditing = true },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 필수 토글
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "필수",
                style = MaterialTheme.typography.labelSmall,
                color = if (todo.isRequired) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Checkbox(
                checked = todo.isRequired,
                onCheckedChange = onRequiredChange
            )
        }
        
        // 삭제 버튼
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Close,
                contentDescription = "삭제",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
```

#### 3.1.3 AddTimeBasedAutoRunDialog 수정

기존 `AddTimeBasedAutoRunDialog.kt`에 추가할 내용:

```kotlin
// State 추가
var scheduleTitle by remember { mutableStateOf("") }
var scheduleDescription by remember { mutableStateOf("") }
var scheduleMemo by remember { mutableStateOf("") }
var todos by remember { mutableStateOf(listOf<ScheduleTodo>()) }

// UI에 섹션 추가 (시간 설정 전에)
ScheduleInfoSection(
    title = scheduleTitle,
    onTitleChange = { scheduleTitle = it },
    description = scheduleDescription,
    onDescriptionChange = { scheduleDescription = it },
    memo = scheduleMemo,
    onMemoChange = { scheduleMemo = it }
)

TodoListSection(
    todos = todos,
    onAddTodo = { 
        todos = todos + ScheduleTodo(
            content = "",
            orderIndex = todos.size
        )
    },
    onUpdateTodo = { index, updatedTodo ->
        todos = todos.toMutableList().apply {
            this[index] = updatedTodo.copy(orderIndex = index)
        }
    },
    onDeleteTodo = { index ->
        todos = todos.toMutableList().apply {
            removeAt(index)
        }.mapIndexed { i, todo -> todo.copy(orderIndex = i) }
    },
    onReorder = { from, to -> /* 후순위 구현 */ }
)
```

#### 3.1.4 Validation 로직

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/validation/ScheduleTodoValidator.kt`

```kotlin
object ScheduleTodoValidator {
    
    const val MIN_CONTENT_LENGTH = 1
    const val MAX_CONTENT_LENGTH = 120
    const val MAX_TODO_COUNT = 10
    
    fun validateContent(content: String): ValidationResult {
        return when {
            content.isBlank() -> ValidationResult.Error("내용을 입력하세요")
            content.length < MIN_CONTENT_LENGTH -> ValidationResult.Error("최소 ${MIN_CONTENT_LENGTH}자 이상 입력하세요")
            content.length > MAX_CONTENT_LENGTH -> ValidationResult.Error("최대 ${MAX_CONTENT_LENGTH}자까지 입력 가능합니다")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateTodoList(todos: List<ScheduleTodo>): ValidationResult {
        if (todos.size > MAX_TODO_COUNT) {
            return ValidationResult.Error("할 일은 최대 ${MAX_TODO_COUNT}개까지 추가할 수 있습니다")
        }
        
        todos.forEachIndexed { index, todo ->
            val contentResult = validateContent(todo.content)
            if (contentResult is ValidationResult.Error) {
                return ValidationResult.Error("${index + 1}번째 할 일: ${contentResult.message}")
            }
        }
        
        return ValidationResult.Valid
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

---

### 3.2 Preview & ViewModel

#### 3.2.1 SchedulePreviewCard

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/SchedulePreviewCard.kt`

```kotlin
@Composable
fun SchedulePreviewCard(
    title: String,
    description: String,
    memo: String?,
    todos: List<ScheduleTodo>,
    timeInfo: String,  // "09:00 ~ 11:00 (매일)"
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 미리보기 라벨
            Text(
                text = "👁️ 미리보기",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider()
            
            // 제목
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            // 설명
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 시간 정보
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeInfo,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 메모
            if (!memo.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // 할 일 미리보기
            if (todos.isNotEmpty()) {
                Text(
                    text = "할 일 (${todos.size})",
                    style = MaterialTheme.typography.labelMedium
                )
                todos.take(3).forEach { todo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = todo.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                if (todos.size > 3) {
                    Text(
                        text = "외 ${todos.size - 3}개",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

#### 3.2.2 ViewModel 수정

기존 `TimeBasedAutoRunViewModel.kt`에 추가:

```kotlin
// StateFlow 추가
private val _scheduleTitle = MutableStateFlow("")
val scheduleTitle: StateFlow<String> = _scheduleTitle.asStateFlow()

private val _scheduleDescription = MutableStateFlow("")
val scheduleDescription: StateFlow<String> = _scheduleDescription.asStateFlow()

private val _scheduleMemo = MutableStateFlow("")
val scheduleMemo: StateFlow<String> = _scheduleMemo.asStateFlow()

private val _todos = MutableStateFlow<List<ScheduleTodo>>(emptyList())
val todos: StateFlow<List<ScheduleTodo>> = _todos.asStateFlow()

// 업데이트 함수
fun updateScheduleTitle(title: String) { _scheduleTitle.value = title }
fun updateScheduleDescription(description: String) { _scheduleDescription.value = description }
fun updateScheduleMemo(memo: String) { _scheduleMemo.value = memo }

fun addTodo() {
    if (_todos.value.size < 10) {
        _todos.value = _todos.value + ScheduleTodo(
            content = "",
            orderIndex = _todos.value.size
        )
    }
}

fun updateTodo(index: Int, todo: ScheduleTodo) {
    _todos.value = _todos.value.toMutableList().apply {
        if (index in indices) this[index] = todo.copy(orderIndex = index)
    }
}

fun deleteTodo(index: Int) {
    _todos.value = _todos.value.toMutableList().apply {
        if (index in indices) removeAt(index)
    }.mapIndexed { i, todo -> todo.copy(orderIndex = i) }
}

// 저장 시 ScheduleInfo와 Todos 포함
fun saveTimeBasedAutoRun() {
    viewModelScope.launch {
        val scheduleInfo = ScheduleInfo(
            scheduleTitle = _scheduleTitle.value,
            scheduleDescription = _scheduleDescription.value,
            scheduleMemo = _scheduleMemo.value.ifBlank { null }
        )
        
        // UseCase 호출 시 scheduleInfo와 todos 전달
        createTimeBasedAutoRunUseCase(
            // ... 기존 파라미터 ...
            scheduleInfo = scheduleInfo,
            todos = _todos.value
        )
    }
}
```

---

## Phase 4. 상세/체크 UI (Day 6)

### 4.1 스케줄 상세 Checklist 표시

#### 4.1.1 ScheduleDetailScreen 수정

기존 상세 화면에 추가할 섹션:

```kotlin
@Composable
fun ScheduleDetailContent(
    scheduleInfo: ScheduleInfo,
    todos: List<ScheduleTodo>,
    isSessionRunning: Boolean,
    onTodoCheck: (todoId: String, isChecked: Boolean) -> Unit,
    // ... 기존 파라미터
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: 제목 & 설명
        item {
            ScheduleHeader(
                title = scheduleInfo.scheduleTitle,
                description = scheduleInfo.scheduleDescription
            )
        }
        
        // Meta: 시간/위치 정보 (기존)
        item {
            ScheduleMetaInfo(/* ... */)
        }
        
        // Memo Card
        if (!scheduleInfo.scheduleMemo.isNullOrBlank()) {
            item {
                MemoCard(memo = scheduleInfo.scheduleMemo)
            }
        }
        
        // Todo Checklist
        if (todos.isNotEmpty()) {
            item {
                Text(
                    text = "✅ 할 일 목록",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            items(todos, key = { it.id }) { todo ->
                TodoChecklistItem(
                    todo = todo,
                    enabled = isSessionRunning,  // 세션 실행 중에만 체크 가능
                    onCheckedChange = { isChecked ->
                        onTodoCheck(todo.id, isChecked)
                    }
                )
            }
        }
    }
}

@Composable
private fun TodoChecklistItem(
    todo: ScheduleTodo,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (todo.isChecked) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(enabled = enabled) { onCheckedChange(!todo.isChecked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isChecked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.content,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (todo.isChecked) TextDecoration.LineThrough else null
            )
            
            if (todo.isRequired) {
                Text(
                    text = "필수",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MemoCard(memo: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📝 메모",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = memo,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### 4.2 세션 실행 중 체크 저장

#### 4.2.1 TimerViewModel 수정

```kotlin
// StateFlow 추가
private val _sessionTodos = MutableStateFlow<List<ScheduleTodo>>(emptyList())
val sessionTodos: StateFlow<List<ScheduleTodo>> = _sessionTodos.asStateFlow()

// 세션 시작 시 Todo 로드
private fun loadSessionTodos(sessionId: String, scheduleId: String) {
    viewModelScope.launch {
        scheduleTodoRepository.initializeProgress(sessionId, scheduleId)
        
        combine(
            scheduleTodoRepository.getTodoItems(scheduleId),
            scheduleTodoRepository.getProgress(sessionId)
        ) { items, progress ->
            val progressMap = progress.associate { it.todoId to it.checked }
            items.toDomainList(progressMap)
        }.collect { todos ->
            _sessionTodos.value = todos
        }
    }
}

// 체크 상태 업데이트
fun updateTodoCheck(todoId: String, isChecked: Boolean) {
    val currentSessionId = _currentSessionId.value ?: return
    
    viewModelScope.launch {
        scheduleTodoRepository.updateProgress(currentSessionId, todoId, isChecked)
    }
}

// 세션 종료 시 결과 저장
private fun finalizeSessionTodos(sessionId: String, scheduleId: String) {
    viewModelScope.launch {
        scheduleTodoRepository.finalizeResults(sessionId, scheduleId)
    }
}
```

#### 4.2.2 LockOverlayScreen 수정 (후순위)

오버레이에 간략한 Todo 표시 (MVP 범위 외, 후속 작업으로 분리):

```kotlin
// 오버레이 상단에 할 일 요약 표시
@Composable
fun TodoSummaryBadge(
    completedCount: Int,
    totalCount: Int
) {
    if (totalCount > 0) {
        Text(
            text = "할 일: $completedCount/$totalCount",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

---

## 검증 체크리스트

### UI 컴포넌트 테스트
```bash
./gradlew test --tests "com.allday.detoxy.presentation.ui.autorun.components.*Test"
```

### ViewModel 테스트
```bash
./gradlew test --tests "com.allday.detoxy.presentation.viewmodel.TimeBasedAutoRunViewModelTest"
```

### 스냅샷 테스트 (선택)
```bash
./gradlew test --tests "*SnapshotTest"
```

### 통합 빌드 확인
```bash
./gradlew assembleDebug
```

---

## 작업 완료 후 문서화

Phase 3 완료 시:
```
working_history/version_2.0/{작업날짜}_phase3_UI생성편집.md
```

Phase 4 완료 시:
```
working_history/version_2.0/{작업날짜}_phase4_상세체크UI.md
```

**포함 내용**:
- 추가/수정된 Composable 목록
- ViewModel 변경 사항
- Validation 규칙
- 스크린샷 또는 UI 테스트 결과

---
*작성일: 2025-12-07 · Phase 3-4 상세 작업 지시서*

