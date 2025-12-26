# Phase 5. 세션 종료 다이얼로그 + 개별 체크 상세 작업 지시서

> **상위 문서**: [할 일 기능 작업 계획](../10_추가기능_할일관리기능_todolist.md)  
> **기능 설계**: [할 일 기능 설계 문서](../10_추가기능_할일관리기능.md)

---

## 5.1 종료 다이얼로그 표시 조건

### 5.1.1 조건별 분기 로직

**파일**: `app/src/main/java/com/allday/detoxy/domain/util/SessionEndDialogResolver.kt`

```kotlin
sealed class SessionEndDialogType {
    object Skip : SessionEndDialogType()  // 다이얼로그 생략
    data class GoalOnly(val goal: String) : SessionEndDialogType()  // 예/아니오
    data class TodoChecklist(val scheduleInfo: ScheduleInfo) : SessionEndDialogType()  // 체크리스트
}

fun resolveSessionEndDialogType(scheduleInfo: ScheduleInfo?): SessionEndDialogType {
    if (scheduleInfo == null) return SessionEndDialogType.Skip
    
    return when {
        scheduleInfo.title.isBlank() && scheduleInfo.todos.isEmpty() -> 
            SessionEndDialogType.Skip
        scheduleInfo.todos.isEmpty() && scheduleInfo.title.isNotBlank() -> 
            SessionEndDialogType.GoalOnly(scheduleInfo.title)
        else -> 
            SessionEndDialogType.TodoChecklist(scheduleInfo)
    }
}
```

---

## 5.2 체크리스트 다이얼로그 UI

### 5.2.1 SessionEndTodoDialog

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/SessionEndTodoDialog.kt`

```kotlin
@Composable
fun SessionEndTodoDialog(
    scheduleInfo: ScheduleInfo,
    onComplete: (Map<String, TodoCompletionStatus>) -> Unit,
    onDismiss: () -> Unit
) {
    var responses by remember { 
        mutableStateOf(scheduleInfo.todos.associate { it.id to false })
    }
    var remainingSeconds by remember { mutableStateOf(30) }
    // 🔧 이중 저장 방지: 완료 상태 플래그
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
                if (checked) TodoCompletionStatus.COMPLETED
                else if (todo?.isRequired == true) TodoCompletionStatus.NOT_COMPLETED
                else TodoCompletionStatus.NO_RESPONSE
            }
            safeComplete(statusMap)
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 48.sp)
                Text("집중 세션 완료!", style = MaterialTheme.typography.headlineSmall)
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text("✅ 완료한 할 일을 체크하세요", style = MaterialTheme.typography.titleMedium)
                
                // 할일 체크리스트
                scheduleInfo.todos.forEach { todo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                responses = responses + (todo.id to !responses[todo.id]!!)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = responses[todo.id] ?: false,
                            onCheckedChange = { responses = responses + (todo.id to it) }
                        )
                        Text(todo.content, modifier = Modifier.weight(1f))
                        if (todo.isRequired) {
                            Badge { Text("필수") }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 타이머 표시
                Text(
                    text = "⏱️ ${remainingSeconds}초 후 현재 상태로 저장",
                    style = MaterialTheme.typography.bodySmall
                )
                LinearProgressIndicator(
                    progress = remainingSeconds / 30f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "🔄 체크 안 한 항목: 필수=미완료, 일반=미응답",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val statusMap = responses.mapValues { (id, checked) ->
                            if (checked) TodoCompletionStatus.COMPLETED
                            else {
                                val todo = scheduleInfo.todos.find { it.id == id }
                                if (todo?.isRequired == true) TodoCompletionStatus.NOT_COMPLETED
                                else TodoCompletionStatus.NO_RESPONSE
                            }
                        }
                        safeComplete(statusMap)  // 🔧 이중 저장 방지
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCompleted  // 🔧 완료 후 버튼 비활성화
                ) {
                    Text("완료")
                }
            }
        }
    }
}
```

### 5.2.2 SessionEndGoalDialog (목표만)

```kotlin
@Composable
fun SessionEndGoalDialog(
    goal: String,
    onComplete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var remainingSeconds by remember { mutableStateOf(30) }
    // 🔧 이중 저장 방지: 완료 상태 플래그
    var isCompleted by remember { mutableStateOf(false) }
    
    // 안전한 완료 처리 함수
    fun safeComplete(completed: Boolean) {
        if (!isCompleted) {
            isCompleted = true
            onComplete(completed)
        }
    }
    
    // 30초 타이머
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isCompleted) {
            delay(1000)
            remainingSeconds--
        }
        safeComplete(false)  // 타임아웃 시 미완료
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 48.sp)
                Text("집중 세션 완료!", style = MaterialTheme.typography.headlineSmall)
                
                Text("목표: \"$goal\"", style = MaterialTheme.typography.titleMedium)
                
                Text("✅ 목표를 달성하셨나요?")
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { safeComplete(true) },
                        enabled = !isCompleted
                    ) {
                        Text("예 ✅")
                    }
                    OutlinedButton(
                        onClick = { safeComplete(false) },
                        enabled = !isCompleted
                    ) {
                        Text("아니오")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("⏱️ ${remainingSeconds}초 후 \"미완료\"로 기록됩니다")
                Text(
                    "(목표는 필수 항목이므로 미응답 시 미완료 처리)",
                    style = MaterialTheme.typography.bodySmall
                )
                
                LinearProgressIndicator(progress = remainingSeconds / 30f)
            }
        }
    }
}
```

---

## 5.3 30초 무응답 처리

### 5.3.1 handleNoResponse 함수

**파일**: `app/src/main/java/com/allday/detoxy/domain/util/TodoResultBuilder.kt`

```kotlin
fun handleNoResponse(todos: List<ScheduleTodo>): Map<String, TodoCompletionStatus> {
    return todos.associate { todo ->
        todo.id to if (todo.isRequired) {
            TodoCompletionStatus.NOT_COMPLETED  // 필수 항목은 미완료
        } else {
            TodoCompletionStatus.NO_RESPONSE    // 일반 항목은 미응답
        }
    }
}
```

---

## 5.4 결과 저장

### 5.4.1 TimerViewModel 확장

**파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt`

```kotlin
fun onSessionEnd() {
    val scheduleInfo = getCurrentScheduleInfo()
    val dialogType = resolveSessionEndDialogType(scheduleInfo)
    
    when (dialogType) {
        is SessionEndDialogType.Skip -> {
            // 다이얼로그 없이 바로 종료
            finishSession(null)
        }
        is SessionEndDialogType.GoalOnly -> {
            _showGoalDialog.value = true
        }
        is SessionEndDialogType.TodoChecklist -> {
            _showTodoDialog.value = true
        }
    }
}

fun onTodoDialogComplete(responses: Map<String, TodoCompletionStatus>) {
    viewModelScope.launch {
        val scheduleInfo = getCurrentScheduleInfo() ?: return@launch
        val scheduleId = getCurrentScheduleId() ?: return@launch
        // 🔧 스케줄 이름도 함께 조회 (빈 문자열 방지)
        val scheduleName = getCurrentScheduleName() ?: ""
        
        val todoResults = buildTodoResult(scheduleId, scheduleInfo, responses)
        
        if (todoResults != null) {
            val resultEntity = FocusSessionTodoResultEntity(
                sessionId = currentSessionId,
                scheduleId = scheduleId,
                scheduleType = currentScheduleType,
                // 🔧 scheduleName 전달로 "미지정 스케줄" fallback 방지
                scheduleTitleSnapshot = getScheduleTitleSnapshot(scheduleInfo, scheduleName),
                todoResultsJson = Gson().toJson(todoResults),
                completedAt = System.currentTimeMillis()
            )
            todoResultDao.insert(resultEntity)
        }
        
        finishSession(todoResults)
    }
}

fun onGoalDialogComplete(completed: Boolean) {
    // 🔧 null 가드 추가: scheduleId가 null이면 early return
    val scheduleId = getCurrentScheduleId() ?: run {
        Log.e(TAG, "onGoalDialogComplete: scheduleId is null")
        return
    }
    
    val goalStatus = if (completed) TodoCompletionStatus.COMPLETED 
                     else TodoCompletionStatus.NOT_COMPLETED
    
    // goalId에 대한 응답으로 변환
    val responses = mapOf("goal:$scheduleId" to goalStatus)
    onTodoDialogComplete(responses)
}
```

---

## 검증 체크리스트

```bash
# 테스트
./gradlew test --tests "*SessionEnd*"

# 빌드 확인
./gradlew assembleDebug
```

### 수동 QA

1. 할일 포함 스케줄 세션 완료
2. 체크리스트 다이얼로그 표시 확인
3. 항목 체크/해제 동작 확인
4. 30초 카운트다운 확인
5. 30초 무응답 후 자동 저장 확인
6. DB에 결과 저장 확인 (adb shell)
7. 목표만 있는 경우 예/아니오 다이얼로그 확인

---

## 완료 기준

- [x] resolveSessionEndDialogType() 함수 구현
- [x] SessionEndTodoDialog Composable 생성
- [x] SessionEndGoalDialog Composable 생성
- [x] 30초 타이머 동작
- [x] handleNoResponse() 함수 구현
- [x] buildTodoResult() 호출 및 저장
- [x] deriveGoalStatusFromTodos() 목표 상태 자동 도출
- [x] TimerScreen UI 통합 (2025-12-26)

---

*작성일: 2025-12-25 · Phase 5 상세 작업 지시서*
