# Phase 2. 도메인 & UseCase 상세 작업 지시서

> **상위 문서**: [할 일 기능 작업 계획](../10_추가기능_할일관리기능_todolist.md)  
> **기능 설계**: [할 일 기능 설계 문서](../10_추가기능_할일관리기능.md)

---

## 📥 사전 작업 (Pre-Work)

| 항목 | 확인 |
|------|------|
| Phase 1 작업결과서 확인 | `working_history/version_1.1/Phase1_데이터레이어확장_{날짜}.md` |
| 생성된 Entity 구조 확인 | `ScheduleInfo`, `FocusSessionTodoResultEntity` |
| 기획서 섹션 3.3-3.5 (핵심 함수) 재확인 | `buildTodoResult`, `deriveGoalStatusFromTodos` |
| 현재 빌드 상태 확인 | `./gradlew assembleDebug` 성공 |

**Phase 1에서 확인할 내용**:
- [ ] ScheduleInfo 클래스 경로 및 데이터 구조
- [ ] TodoCompletionStatus enum 정의
- [ ] Migration 버전 번호

---

## 2.1 검증 로직

### 2.1.1 ScheduleInfoValidation

**파일**: `app/src/main/java/com/allday/detoxy/domain/validation/ScheduleInfoValidation.kt`

```kotlin
object ScheduleInfoValidation {
    const val MAX_TITLE_LENGTH = 50
    // 🔄 7차: MVP에서 description/memo 미사용으로 상수 제거
    // v2에서 UI 추가 시 아래 상수 복원:
    // const val MAX_DESCRIPTION_LENGTH = 200
    // const val MAX_MEMO_LENGTH = 500
}

fun validateScheduleInfo(info: ScheduleInfo): ValidationResult {
    if (info.title.length > ScheduleInfoValidation.MAX_TITLE_LENGTH) {
        return ValidationResult.Error("목표는 ${ScheduleInfoValidation.MAX_TITLE_LENGTH}자 이내로 입력해주세요")
    }
    if (info.todos.size > ScheduleTodo.MAX_TODO_COUNT) {
        return ValidationResult.Error("할일은 최대 ${ScheduleTodo.MAX_TODO_COUNT}개까지 등록 가능합니다")
    }
    info.todos.forEach { todo ->
        if (todo.content.length > ScheduleTodo.MAX_CONTENT_LENGTH) {
            return ValidationResult.Error("할일 내용은 ${ScheduleTodo.MAX_CONTENT_LENGTH}자 이내로 입력해주세요")
        }
        if (todo.content.isBlank()) {
            return ValidationResult.Error("할일 내용을 입력해주세요")
        }
    }
    return ValidationResult.Success
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

---

## 2.2 핵심 함수 구현

### 2.2.1 TodoStatus data class

**파일**: `app/src/main/java/com/allday/detoxy/domain/model/TodoStatus.kt`

> **🔧 8차 리뷰**: domain 레이어 모델이므로 data 레이어에서 직접 참조 시 계층 위반 가능.  
> JSON 파싱은 data 레이어에서 수행하고, `TodoStatusMapper`로 domain 모델로 변환 권장.

```kotlin
data class TodoStatus(
    val id: String,
    val content: String,
    val isRequired: Boolean,
    val status: TodoCompletionStatus,
    val isGoal: Boolean = false
)
```

### 2.2.2 buildTodoResult 함수

**파일**: `app/src/main/java/com/allday/detoxy/domain/util/TodoResultBuilder.kt`

```kotlin
fun buildTodoResult(
    scheduleId: String,
    info: ScheduleInfo,
    userResponses: Map<String, TodoCompletionStatus>
): List<TodoStatus>? {
    
    // 목표/할일 없는 경우: 저장 생략
    if (info.todos.isEmpty() && info.title.isBlank()) {
        return null
    }
    
    val result = mutableListOf<TodoStatus>()
    
    // 세부 할일 추가
    val todoStatuses = info.todos.map { todo ->
        TodoStatus(
            id = todo.id,
            content = todo.content,
            isRequired = todo.isRequired,
            status = getStatusWithRequiredFallback(userResponses[todo.id], todo.isRequired),
            isGoal = false
        )
    }
    
    // 목표 처리
    if (info.title.isNotBlank()) {
        val goalId = "goal:$scheduleId"
        val goalStatus = if (todoStatuses.isEmpty()) {
            getStatusWithRequiredFallback(userResponses[goalId], isRequired = true)
        } else {
            deriveGoalStatusFromTodos(todoStatuses)
        }
        result.add(TodoStatus(
            id = goalId,
            content = info.title,
            isRequired = true,
            status = goalStatus,
            isGoal = true
        ))
    }
    
    result.addAll(todoStatuses)
    return result
}
```

### 2.2.3 deriveGoalStatusFromTodos 함수

```kotlin
fun deriveGoalStatusFromTodos(todos: List<TodoStatus>): TodoCompletionStatus {
    val requiredTodos = todos.filter { it.isRequired }
    
    return if (requiredTodos.isNotEmpty()) {
        // 필수 할일 기준
        if (requiredTodos.all { it.status == TodoCompletionStatus.COMPLETED }) {
            TodoCompletionStatus.COMPLETED
        } else {
            TodoCompletionStatus.NOT_COMPLETED
        }
    } else {
        // 필수 할일 없으면 전체 기준 (🔧 8차 리뷰: 명확한 분기 추가)
        when {
            todos.all { it.status == TodoCompletionStatus.COMPLETED } -> 
                TodoCompletionStatus.COMPLETED
            todos.all { it.status == TodoCompletionStatus.NO_RESPONSE } -> 
                TodoCompletionStatus.NO_RESPONSE  // 모두 미응답 시 목표도 미응답
            todos.any { it.status == TodoCompletionStatus.NOT_COMPLETED } -> 
                TodoCompletionStatus.NOT_COMPLETED
            else -> 
                TodoCompletionStatus.NO_RESPONSE  // 혼합 상태 (COMPLETED + NO_RESPONSE 등)
        }
    }
}
```

### 2.2.4 getStatusWithRequiredFallback 함수

```kotlin
fun getStatusWithRequiredFallback(
    userResponse: TodoCompletionStatus?,
    isRequired: Boolean
): TodoCompletionStatus {
    return userResponse ?: if (isRequired) {
        TodoCompletionStatus.NOT_COMPLETED
    } else {
        TodoCompletionStatus.NO_RESPONSE
    }
}
```

### 2.2.5 getScheduleTitleSnapshot 함수

```kotlin
fun getScheduleTitleSnapshot(info: ScheduleInfo, scheduleName: String): String {
    return when {
        info.title.isNotBlank() -> info.title
        scheduleName.isNotBlank() -> scheduleName
        else -> "미지정 스케줄"
    }
}
```

---

## 2.3 UseCase 확장

### 2.3.1 CreateTimeBasedAutoRunUseCase 수정

**파일**: `app/src/main/java/com/allday/detoxy/domain/usecase/CreateTimeBasedAutoRunUseCase.kt`

```kotlin
class CreateTimeBasedAutoRunUseCase @Inject constructor(
    private val repository: AutoRunRepository
) {
    suspend operator fun invoke(
        // 기존 파라미터...
        scheduleInfo: ScheduleInfo = ScheduleInfo()  // 🆕 추가
    ): Result<String> {
        // 검증
        val validation = validateScheduleInfo(scheduleInfo)
        if (validation is ValidationResult.Error) {
            return Result.failure(IllegalArgumentException(validation.message))
        }
        
        // 기존 로직 + scheduleInfo 저장
        return repository.createTimeBasedAutoRun(
            // 기존 파라미터...
            scheduleInfoJson = Gson().toJson(scheduleInfo)
        )
    }
}
```

### 2.3.2 동일하게 수정할 UseCase 목록

- [ ] `UpdateTimeBasedAutoRunUseCase`
- [ ] `CreateLocationBasedAutoRunUseCase`
- [ ] `UpdateLocationBasedAutoRunUseCase`

---

## 검증 체크리스트

```bash
# UseCase 테스트
./gradlew test --tests "*AutoRunUseCase*"

# Validation 테스트
./gradlew test --tests "*TodoValidation*"

# 빌드 확인
./gradlew assembleDebug
```

---

## 완료 기준

- [ ] ScheduleInfoValidation object 구현
- [ ] validateScheduleInfo() 단위 테스트 통과
- [ ] TodoStatus data class 정의
- [ ] buildTodoResult() 함수 구현 및 테스트
- [ ] deriveGoalStatusFromTodos() 함수 구현 및 테스트
- [ ] getStatusWithRequiredFallback() 함수 구현
- [ ] getScheduleTitleSnapshot() 함수 구현
- [ ] 4개 UseCase에 scheduleInfo 파라미터 확장

---

*작성일: 2025-12-25 · Phase 2 상세 작업 지시서*
