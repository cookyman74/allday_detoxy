# 할 일(To-Do) 결합 스케줄 작업 계획 (v1.0)

> **기능 설계**: [할 일 기능 설계 문서](./10_추가기능_할일관리기능.md)  
> **템플릿 참고**: [03.5 템플릿 AI 스케줄 To-Do 리스트](./version_1.0/03.5_template_ai_schedule_todolist_v09.md)  
> **선행 참고**: [@version_1.0 Autosetting PRD](./version_1.0/02_advanced_autosetting_prd.md)  
> **작업결과 템플릿**: [working_history 템플릿](../working_history/version_2.0/working_history_template.md)

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| **목표** | 스케줄에 타이틀/설명/메모/할일 목록 추가하여 집중 목적 명확화 |
| **MVP 범위** | 입력/저장, 상세 표시, 세션 중 체크 저장 |
| **제외 범위** | 런타임 알림 연동, 통계 리포트 UI (차기 단계) |
| **예상 기간** | 8일 (Day 0 ~ Day 8) |

---

## 준비 단계 (Day 0)

### 작업 내용
- [ ] `docs/10_추가기능_할일관리기능.md` 요구사항 재확인
- [ ] 엔티티/테이블 구조 확정 (ScheduleInfo, ScheduleTodoItem, Progress, Result)
- [ ] 기존 AutoRun 스케줄 생성/편집 플로우 QA
  - Baseline 스크린샷 확보 (생성 다이얼로그, 상세 화면)
- [ ] Migration 테스트용 dummy DB 덤프 준비
- [ ] 현재 Room DB 버전 확인 (`AppDatabase.kt`)

### 검증 명령
```bash
./gradlew test --tests "*BaselineSmokeTest"
./gradlew assembleDebug  # 현재 빌드 상태 확인
```

### 완료 기준
- 기존 기능 정상 동작 확인
- DB 버전 및 Migration 번호 결정

---

## Phase 1. 데이터 레이어 확장 (Day 1-2)

> **📄 상세 작업 지시서**: [10_phase1_데이터레이어확장.md](./10_phase1_데이터레이어확장.md)

### 1.1 Schema & Migration
- [ ] `ScheduleInfo` Embedded 클래스 생성
  - `scheduleTitle`, `scheduleDescription`, `scheduleMemo` 필드
- [ ] `TimeBasedAutoRun`, `LocationBasedAutoRun` 엔티티에 `@Embedded scheduleInfo` 추가
- [ ] 새 테이블 엔티티 생성:
  - `ScheduleTodoItem` (스케줄별 할 일 정의)
  - `FocusSessionTodoProgress` (세션 중 체크 상태)
  - `FocusSessionTodoResult` (세션 종료 후 결과 스냅샷)
- [ ] Migration 클래스 작성 (`Migration_X_Y`)
  - ALTER TABLE로 기존 테이블에 컬럼 추가
  - CREATE TABLE로 새 테이블 생성
  - 인덱스 및 FK 설정
- [ ] `AppDatabase`에 새 엔티티 및 DAO 등록, 버전 증가

### 1.2 DAO / Repository
- [ ] `ScheduleTodoItemDao` 구현
  - `getByScheduleId()`, `insertAll()`, `replaceAll()`, `deleteByScheduleId()`
- [ ] `FocusSessionTodoProgressDao` 구현
  - `getBySessionId()`, `upsert()`, `updateChecked()`, `deleteBySessionId()`
- [ ] `FocusSessionTodoResultDao` 구현
  - `getBySessionId()`, `insertAll()`, `getCompletionStats()`
- [ ] `ScheduleTodoRepository` 구현
  - Todo CRUD, Progress 초기화/업데이트, Result 확정 로직

### 1.3 Converter & Mapper
- [ ] `ScheduleTodo` 도메인 모델 정의
- [ ] Entity ↔ Domain 매퍼 함수 작성
- [ ] 단위 테스트 작성

### 검증 명령
```bash
# Migration 테스트
./gradlew test --tests "com.allday.detoxy.data.local.migration.*"

# DAO 테스트
./gradlew test --tests "com.allday.detoxy.data.local.dao.ScheduleTodo*"
./gradlew test --tests "com.allday.detoxy.data.local.dao.FocusSessionTodo*"

# Repository 테스트
./gradlew test --tests "com.allday.detoxy.data.repository.ScheduleTodoRepositoryTest"

# 빌드 확인
./gradlew assembleDebug
```

### 완료 기준
- Migration 테스트 통과
- DB 스키마 변경 후 앱 정상 실행
- DAO CRUD 테스트 통과

### 작업 결과 문서
```
working_history/version_2.0/{YYYY-MM-DD}_phase1_데이터레이어확장.md
```

---

## Phase 2. 도메인 & UseCase (Day 3)

### 2.1 스케줄 생성/수정 UseCase 확장
- [ ] `CreateTimeBasedAutoRunUseCase` 파라미터 확장
  - `scheduleInfo: ScheduleInfo`, `todos: List<ScheduleTodo>` 추가
  - 내부에서 `ScheduleTodoRepository.saveTodoItems()` 호출
- [ ] `UpdateTimeBasedAutoRunUseCase` 동일 적용
- [ ] `CreateLocationBasedAutoRunUseCase` 동일 적용
- [ ] `UpdateLocationBasedAutoRunUseCase` 동일 적용
- [ ] UseCase 단위 테스트 작성

### 2.2 세션 Progress→Result 플로우
- [ ] `FocusSessionTodoManager` (또는 기존 Manager 확장)
  - `initializeProgress(sessionId, scheduleId)`: 세션 시작 시 Progress 생성
  - `finalizeResults(sessionId, scheduleId)`: 세션 종료 시 Result로 이동
  - `cleanupProgress(sessionId)`: 취소/실패 시 Progress 삭제
- [ ] `FocusTimerService` 또는 관련 Manager에서 위 메서드 호출 연동
- [ ] 앱 강제 종료 후 재실행 시 Progress 복원 로직 확인

### 검증 명령
```bash
# UseCase 테스트
./gradlew test --tests "com.allday.detoxy.domain.usecase.*AutoRun*Test"

# Manager 테스트
./gradlew test --tests "com.allday.detoxy.domain.manager.FocusSessionTodoManagerTest"

# 통합 테스트
./gradlew connectedAndroidTest --tests "*SessionTodo*"
```

### 완료 기준
- UseCase에서 scheduleInfo, todos 파라미터 처리
- 세션 시작→체크→종료 플로우 데이터 정합성 확인

### 작업 결과 문서
```
working_history/version_2.0/{YYYY-MM-DD}_phase2_도메인UseCase.md
```

---

## Phase 3. UI 생성/편집 (Day 4-5)

> **📄 상세 작업 지시서**: [10_phase3_4_UI구현.md](./10_phase3_4_UI구현.md)

### 3.1 입력 UI 컴포넌트
- [ ] `ScheduleInfoSection` Composable 생성
  - 제목(TextField), 설명(Multiline), 메모(Optional) 입력
- [ ] `TodoListSection` Composable 생성
  - Todo 리스트 (최대 10개), 추가/삭제/편집
  - 각 항목: 내용 입력, 필수 토글, 삭제 버튼
- [ ] `TodoItemRow` Composable 생성
  - 인라인 편집, Validation 표시
- [ ] `AddTimeBasedAutoRunDialog`에 새 섹션 통합
- [ ] `AddLocationBasedAutoRunDialog`에 동일 적용

### 3.2 Validation 로직
- [ ] `ScheduleTodoValidator` 구현
  - 내용: 1~120자, 공백 금지
  - 목록: 최대 10개
- [ ] 실시간 Validation 피드백 UI

### 3.3 Preview & ViewModel
- [ ] `SchedulePreviewCard` Composable 생성
  - 입력 내용 요약 미리보기
- [ ] `TimeBasedAutoRunViewModel` 상태 확장
  - `scheduleTitle`, `scheduleDescription`, `scheduleMemo`, `todos` StateFlow
  - 업데이트 함수들
- [ ] `LocationBasedAutoRunViewModel` 동일 적용

### 검증 명령
```bash
# UI 컴포넌트 테스트
./gradlew test --tests "com.allday.detoxy.presentation.ui.autorun.components.*Test"

# ViewModel 테스트
./gradlew test --tests "com.allday.detoxy.presentation.viewmodel.*AutoRunViewModelTest"

# 빌드 및 수동 확인
./gradlew assembleDebug
```

### 완료 기준
- 스케줄 생성/편집 다이얼로그에 새 필드 표시
- Validation 동작 확인
- Preview 카드에 입력 내용 반영

### 작업 결과 문서
```
working_history/version_2.0/{YYYY-MM-DD}_phase3_UI생성편집.md
```

---

## Phase 4. 상세/체크 UI (Day 6)

> **📄 상세 작업 지시서**: [10_phase3_4_UI구현.md](./10_phase3_4_UI구현.md#phase-4-상세체크-ui-day-6)

### 4.1 스케줄 상세 Checklist 표시
- [ ] `ScheduleDetailContent` 수정
  - Header: 제목, 설명 표시
  - Meta: 기존 시간/위치 정보
  - MemoCard: 메모 표시
  - TodoChecklist: 할 일 목록 체크박스
- [ ] `TodoChecklistItem` Composable
  - 체크박스, 내용, 필수 뱃지
  - 완료 시 취소선 표시
- [ ] `ScheduleGroupCard` 등 목록 화면에 Todo 개수 뱃지 추가 (선택)

### 4.2 세션 실행 중 체크 저장
- [ ] `TimerViewModel` 확장
  - `sessionTodos` StateFlow
  - `loadSessionTodos()`: 세션 시작 시 Todo + Progress 로드
  - `updateTodoCheck()`: 체크 상태 즉시 DB 저장
  - `finalizeSessionTodos()`: 세션 종료 시 Result 확정
- [ ] 상세 화면에서 세션 실행 중일 때만 체크 가능하도록 `enabled` 제어
- [ ] 앱 강제 종료 후 재실행 시 체크 상태 복원 확인

### 검증 명령
```bash
# 상세 화면 테스트
./gradlew test --tests "com.allday.detoxy.presentation.ui.autorun.*DetailTest"

# TimerViewModel 테스트
./gradlew test --tests "com.allday.detoxy.presentation.viewmodel.TimerViewModelTest"

# 수동 QA
# 1. 스케줄 생성 (Todo 포함)
# 2. 세션 시작 → 체크 → 앱 종료 → 재실행 → 체크 상태 확인
# 3. 세션 완료 → DB에 Result 저장 확인
```

### 완료 기준
- 상세 화면에 Todo 체크리스트 표시
- 세션 중 체크/해제 즉시 저장
- 세션 종료 후 Result 테이블에 스냅샷 저장

### 작업 결과 문서
```
working_history/version_2.0/{YYYY-MM-DD}_phase4_상세체크UI.md
```

---

## Phase 5. 세션 종료 & 리포트 연동 (Day 7)

### 5.1 종료 다이얼로그
- [ ] 세션 종료 다이얼로그에 Todo 완료 현황 표시
  - "완료한 할 일: 3/5"
  - 필수 항목 미완료 시 경고 메시지 (선택)
- [ ] `TimerViewModel.onTimerFinish()`에서 완료율 계산
- [ ] 성공/실패/포기에 따른 분기 처리

### 5.2 리포트 데이터 준비 (UI는 차기 단계)
- [ ] `FocusSessionTodoResultDao.getCompletionStats()` 활용
- [ ] `ReportViewModel`에 Todo 완료율 데이터 파이프라인 연결
  - `todoCompletionRate: Float` 계산
  - `requiredCompletionRate: Float` 계산
- [ ] 데이터만 준비, UI 표시는 Feature Flag로 숨김

### 검증 명령
```bash
# TimerViewModel 테스트
./gradlew test --tests "com.allday.detoxy.presentation.viewmodel.TimerViewModelTest"

# ReportViewModel 테스트
./gradlew test --tests "com.allday.detoxy.presentation.viewmodel.ReportViewModelTest"

# 수동 QA
# 1. Todo 포함 스케줄로 세션 완료
# 2. 종료 다이얼로그에 완료 현황 표시 확인
# 3. DB에서 Result 데이터 확인
```

### 완료 기준
- 종료 다이얼로그에 "n/m" 형식 표시
- ReportViewModel에 완료율 데이터 흐름

### 작업 결과 문서
```
working_history/version_2.0/{YYYY-MM-DD}_phase5_세션종료리포트.md
```

---

## Phase 6. QA & 문서화 (Day 8)

### 6.1 수동 QA 체크리스트
- [ ] **생성 플로우**
  - [ ] 시간 기반 스케줄 + Todo 생성
  - [ ] 위치 기반 스케줄 + Todo 생성
  - [ ] Validation 동작 (글자 수, 개수 제한)
  - [ ] 저장 후 상세 화면 확인
- [ ] **편집 플로우**
  - [ ] 기존 스케줄에 Todo 추가/수정/삭제
  - [ ] scheduleInfo 수정 후 저장
- [ ] **세션 플로우**
  - [ ] 세션 시작 → Todo 체크 → 완료
  - [ ] 세션 중 앱 강제 종료 → 재실행 → 체크 상태 복원
  - [ ] 세션 포기 → Progress 삭제 확인
- [ ] **UI/UX**
  - [ ] 다크모드 확인
  - [ ] RTL 레이아웃 확인 (선택)
  - [ ] 긴 텍스트 입력 시 UI 깨짐 없음

### 6.2 Migration 테스트
- [ ] 이전 버전 DB에서 업그레이드 테스트
- [ ] 기존 데이터 보존 확인
- [ ] 새 필드 기본값 적용 확인

### 6.3 문서 업데이트
- [ ] [기능 설계 문서](./10_추가기능_할일관리기능.md) 상태 업데이트
  - MVP 완료 항목 체크
  - 남은 작업 (런타임 알림, 통계 UI) 명시
- [ ] 릴리스 노트 초안 작성

### 결과 기록
```
docs/QA/2025-할일기능-체크리스트.md
```

### 최종 작업 결과 문서
```
working_history/version_2.0/{YYYY-MM-DD}_phase6_QA문서화.md
```

---

## 📝 작업 결과 문서 작성 가이드

각 Phase 완료 시 `working_history/version_2.0/` 폴더에 아래 형식으로 작업 결과 문서를 작성합니다.

### 파일명 규칙
```
{YYYY-MM-DD}_{phase번호}_{작업타이틀}.md
```

예시:
- `2025-12-08_phase1_데이터레이어확장.md`
- `2025-12-09_phase2_도메인UseCase.md`
- `2025-12-10_phase3_UI생성편집.md`

### 문서 구조 (템플릿 참고)
```markdown
# 작업 기록: {작업 제목}

**작업 일시**: YYYY-MM-DD  
**작업 범위**: {작업 범위 요약}

## 작업 내용

### 변경 파일 목록
- `path/to/file1.kt` - 변경 내용 요약
- `path/to/file2.kt` - 변경 내용 요약

### 주요 변경 사항
{코드 변경의 핵심 내용 설명}

### DB 스키마 변경 (해당 시)
{테이블/컬럼 추가/수정 내용}

## 검증 방법

### 실행한 테스트
```bash
./gradlew test --tests "..."
```

### 수동 QA 결과
{확인한 시나리오와 결과}

## 관련 파일
- {수정된 파일 전체 목록}

## 참고 사항
- {추가 메모, 후속 작업 등}

---
**작업 완료일**: YYYY-MM-DD  
**작업자**: {작업자}
```

---

## 🚨 리스크 및 대응

| 리스크 | 영향도 | 대응 방안 |
|--------|--------|-----------|
| Migration 실패 | 높음 | `fallbackToDestructiveMigration` 금지, Migration Test 필수 |
| UI 복잡도 증가 | 중간 | `CollapsibleCard`로 섹션 분리, 기본값 접힘 |
| 데이터 동기화 | 중간 | `orderIndex` 기준 정렬, Diff 적용 |
| 국제화 | 낮음 | 입력 안내문구 다국어 리소스 준비 |

---

*최종 수정일: 2025-12-07 · 작성자: AI Assistant*
