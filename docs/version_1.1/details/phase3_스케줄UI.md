# Phase 3. 스케줄 생성/편집 UI 상세 작업 지시서

> **상위 문서**: [할 일 기능 작업 계획](../10_추가기능_할일관리기능_todolist.md)  
> **기능 설계**: [할 일 기능 설계 문서](../10_추가기능_할일관리기능.md)

---

## 3.1 입력 UI 컴포넌트

### 3.1.1 GoalInputSection

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/GoalInputSection.kt`

```kotlin
@Composable
fun GoalInputSection(
    title: String,
    onTitleChange: (String) -> Unit,
    showTodoSection: Boolean,
    onToggleTodoSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 목표 입력
        OutlinedTextField(
            value = title,
            onValueChange = { if (it.length <= 50) onTitleChange(it) },
            label = { Text("🎯 집중 목표 (선택)") },
            placeholder = { Text("오늘 할 일을 입력하세요...") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 글자 수 표시
        Text(
            text = "${title.length}/50",
            style = MaterialTheme.typography.bodySmall,
            color = if (title.length > 45) MaterialTheme.colorScheme.error 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 세부 항목 추가 버튼
        if (!showTodoSection) {
            TextButton(onClick = onToggleTodoSection) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("세부 항목 추가")
            }
        }
    }
}
```

### 3.1.2 TodoListSection

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/TodoListSection.kt`

```kotlin
@Composable
fun TodoListSection(
    todos: List<ScheduleTodo>,
    onTodoChange: (Int, ScheduleTodo) -> Unit,
    onTodoDelete: (Int) -> Unit,
    onTodoAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📋 세부 할일 (${todos.size}개 중 ${ScheduleTodo.MAX_TODO_COUNT}개)",
                style = MaterialTheme.typography.titleSmall
            )
        }
        
        // 할일 목록
        todos.forEachIndexed { index, todo ->
            TodoItemRow(
                todo = todo,
                index = index,
                onTodoChange = { onTodoChange(index, it) },
                onDelete = { onTodoDelete(index) }
            )
        }
        
        // 추가 버튼
        if (todos.size < ScheduleTodo.MAX_TODO_COUNT) {
            TextButton(onClick = onTodoAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("할 일 추가 (${ScheduleTodo.MAX_TODO_COUNT - todos.size}개 남음)")
            }
        }
    }
}
```

### 3.1.3 TodoItemRow

```kotlin
@Composable
fun TodoItemRow(
    todo: ScheduleTodo,
    index: Int,
    onTodoChange: (ScheduleTodo) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순번
        Text("${index + 1}.", modifier = Modifier.width(24.dp))
        
        // 내용 입력
        OutlinedTextField(
            value = todo.content,
            onValueChange = { 
                if (it.length <= ScheduleTodo.MAX_CONTENT_LENGTH) {
                    onTodoChange(todo.copy(content = it))
                }
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            isError = todo.content.length > 90
        )
        
        // 필수 토글
        FilterChip(
            selected = todo.isRequired,
            onClick = { onTodoChange(todo.copy(isRequired = !todo.isRequired)) },
            label = { Text("필수") }
        )
        
        // 삭제 버튼
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "삭제")
        }
    }
}
```

---

## 3.2 다이얼로그 통합

### 3.2.1 AddTimeBasedAutoRunDialog 수정

**위치**: 기존 시간 설정 섹션 아래에 목표/할일 섹션 추가

```kotlin
@Composable
fun AddTimeBasedAutoRunDialog(
    // 기존 파라미터...
    viewModel: TimeBasedAutoRunViewModel = hiltViewModel()
) {
    val scheduleInfo by viewModel.scheduleInfo.collectAsState()
    val showTodoSection by viewModel.showTodoSection.collectAsState()
    
    // ...
    
    // 목표 섹션 추가
    GoalInputSection(
        title = scheduleInfo.title,
        onTitleChange = { viewModel.updateTitle(it) },
        showTodoSection = showTodoSection,
        onToggleTodoSection = { viewModel.toggleTodoSection() }
    )
    
    // 할일 섹션 (조건부)
    if (showTodoSection) {
        TodoListSection(
            todos = scheduleInfo.todos,
            onTodoChange = { index, todo -> viewModel.updateTodo(index, todo) },
            onTodoDelete = { viewModel.deleteTodo(it) },
            onTodoAdd = { viewModel.addTodo() }
        )
    }
}
```

---

## 3.3 ViewModel 확장

### 3.3.1 TimeBasedAutoRunViewModel

**파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimeBasedAutoRunViewModel.kt`

```kotlin
@HiltViewModel
class TimeBasedAutoRunViewModel @Inject constructor(
    // 기존 의존성...
    private val createUseCase: CreateTimeBasedAutoRunUseCase
) : ViewModel() {
    
    // 🆕 ScheduleInfo 상태
    private val _scheduleInfo = MutableStateFlow(ScheduleInfo())
    val scheduleInfo: StateFlow<ScheduleInfo> = _scheduleInfo.asStateFlow()
    
    private val _showTodoSection = MutableStateFlow(false)
    val showTodoSection: StateFlow<Boolean> = _showTodoSection.asStateFlow()
    
    // 목표 업데이트
    fun updateTitle(title: String) {
        _scheduleInfo.update { it.copy(title = title) }
    }
    
    // 할일 섹션 토글
    fun toggleTodoSection() {
        _showTodoSection.update { !it }
    }
    
    // 할일 추가
    fun addTodo() {
        val current = _scheduleInfo.value.todos
        if (current.size < ScheduleTodo.MAX_TODO_COUNT) {
            _scheduleInfo.update { 
                it.copy(todos = current + ScheduleTodo(content = "", orderIndex = current.size))
            }
        }
    }
    
    // 할일 수정
    fun updateTodo(index: Int, todo: ScheduleTodo) {
        _scheduleInfo.update { info ->
            val newTodos = info.todos.toMutableList()
            newTodos[index] = todo
            info.copy(todos = newTodos)
        }
    }
    
    // 할일 삭제
    fun deleteTodo(index: Int) {
        _scheduleInfo.update { info ->
            val newTodos = info.todos.toMutableList()
            newTodos.removeAt(index)
            info.copy(todos = newTodos)
        }
    }
    
    // 저장 시 검증
    fun save() {
        val validation = validateScheduleInfo(_scheduleInfo.value)
        if (validation is ValidationResult.Error) {
            // 에러 표시
            return
        }
        // 기존 저장 로직 + scheduleInfo 전달
    }
}
```

---

## 검증 체크리스트

```bash
# ViewModel 테스트
./gradlew test --tests "*AutoRunViewModel*"

# 빌드 확인
./gradlew assembleDebug
```

### 수동 QA

1. 스케줄 생성 다이얼로그 열기
2. 목표 입력 (50자 제한 확인)
3. 세부 항목 추가 클릭
4. 할일 5개까지 추가 확인
5. 필수 토글 동작 확인
6. 저장 후 상세 화면에서 확인

---

## 완료 기준

- [ ] GoalInputSection Composable 생성
- [ ] TodoListSection Composable 생성
- [ ] TodoItemRow Composable 생성
- [ ] AddTimeBasedAutoRunDialog에 섹션 통합
- [ ] AddLocationBasedAutoRunDialog에 동일 적용
- [ ] TimeBasedAutoRunViewModel 상태 확장
- [ ] LocationBasedAutoRunViewModel 동일 적용
- [ ] Validation 실시간 피드백 동작

---

*작성일: 2025-12-25 · Phase 3 상세 작업 지시서*
