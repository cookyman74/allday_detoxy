# 작업 기록: Phase 2 도메인 & UseCase

**작업 일시**: 2025-12-26  
**작업 범위**: 할 일(To-Do) 기능을 위한 검증 로직 및 핵심 함수 구현

## 문제 상황 / 작업 배경

Phase 1에서 생성된 Entity와 TypeConverter를 기반으로 도메인 계층의 검증 로직과 핵심 비즈니스 함수를 구현해야 함.

**Phase 1 확인 결과**:
- ScheduleInfo 경로: `domain/model/ScheduleInfo.kt`
- TodoStatus, TodoCompletionStatus 경로: `domain/model/ScheduleInfo.kt`
- DB 버전: v9

## 수정 사항

### 1. 검증 로직 구현

**파일**: `app/src/main/java/com/allday/detoxy/domain/validation/ScheduleInfoValidation.kt`

```kotlin
object ScheduleInfoValidation {
    const val MAX_TITLE_LENGTH = 50
}

fun validateScheduleInfo(info: ScheduleInfo): ValidationResult

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

**검증 규칙**:
| 항목 | 규칙 |
|------|------|
| 목표(title) | 최대 50자 |
| 할일 개수 | 최대 5개 |
| 할일 내용 | 비어있지 않고, 최대 100자 |

### 2. 핵심 함수 구현

**파일**: `app/src/main/java/com/allday/detoxy/domain/util/TodoResultBuilder.kt`

| 함수 | 설명 |
|------|------|
| `buildTodoResult()` | 세션 종료 시 사용자 응답과 ScheduleInfo를 기반으로 TodoStatus 목록 생성 |
| `deriveGoalStatusFromTodos()` | 할일 결과에서 목표 상태 자동 도출 |
| `getStatusWithRequiredFallback()` | 무응답 시 fallback 상태 결정 (필수→NOT_COMPLETED, 일반→NO_RESPONSE) |
| `getScheduleTitleSnapshot()` | 스케줄 제목 스냅샷 생성 (목표 > 스케줄명 > 기본값) |

**목표 상태 도출 규칙**:
```
필수 할일 있음:
  - 모든 필수 할일 COMPLETED → 목표 COMPLETED
  - 그 외 → 목표 NOT_COMPLETED

필수 할일 없음:
  - 모두 COMPLETED → 목표 COMPLETED
  - 모두 NO_RESPONSE → 목표 NO_RESPONSE
  - 하나라도 NOT_COMPLETED → 목표 NOT_COMPLETED
  - 혼합 상태 → 목표 NO_RESPONSE
```

### 3. 프로젝트 구조 노트

**UseCase 패턴 미적용**:
- 프로젝트에서 UseCase 계층 없이 Repository 직접 사용
- 검증 로직은 ViewModel에서 호출하는 방식으로 통합 예정

## 검증 방법

```bash
./gradlew assembleDebug
# BUILD SUCCESSFUL in 8s
```

- [x] 컴파일 오류 없음
- [x] ScheduleInfoValidation 구현 완료
- [x] TodoResultBuilder 핵심 함수 구현 완료

## 관련 파일

**신규 생성**:
- `domain/validation/ScheduleInfoValidation.kt`
- `domain/util/TodoResultBuilder.kt`

**참조**:
- `domain/model/ScheduleInfo.kt` (Phase 1에서 생성)

## Phase 3 준비 사항

| 항목 | 값 |
|------|-----|
| 검증 함수 | `validateScheduleInfo(info: ScheduleInfo): ValidationResult` |
| 결과 빌더 | `TodoResultBuilder.buildTodoResult(scheduleId, info, responses)` |
| 스냅샷 함수 | `TodoResultBuilder.getScheduleTitleSnapshot(info, name)` |

### 다음 작업 (Phase 3)

1. `GoalInputSection` Composable 생성
2. `TodoListSection` Composable 생성  
3. 스케줄 생성/편집 다이얼로그에 통합
4. ViewModel에서 검증 로직 호출

### 리뷰 피드백 (2025-12-26)

**결과**: ✅ 코드 이슈 없음

**테스트 권고사항** (향후 개선):
| 함수 | 테스트 케이스 |
|------|--------------|
| `validateScheduleInfo()` | 빈 할일, 길이 초과, 개수 초과 |
| `deriveGoalStatusFromTodos()` | 빈 목록, 전체 NO_RESPONSE, 혼합 상태 |

**설계 가정**:
- `deriveGoalStatusFromTodos()`는 `buildTodoResult()` 내에서만 호출되며, 항상 비어있지 않은 목록이 전달됨

---

**작업 완료일**: 2025-12-26  
**작업자**: AI Assistant
