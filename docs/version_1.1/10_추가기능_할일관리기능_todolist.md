# 할 일(To-Do) 결합 스케줄 작업 계획 (v1.1)

> **기능 설계**: [10_추가기능_할일관리기능.md](./10_추가기능_할일관리기능.md)  
> **최종 수정일**: 2025-12-25 (7차 리뷰 반영)

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| **목표** | 스케줄에 목표(title) + 할일(todos) 추가하여 집중 목적 명확화 |
| **MVP 범위** | 입력/저장, 상세 표시, **차단 오버레이 표시**, 세션 종료 개별 체크 |
| **제외 범위** | description/memo UI, 통계 리포트 UI (v2) |
| **예상 기간** | 9일 (Day 0 ~ Day 8) |

### MVP 핵심 기능

| 기능 | 설명 |
|------|------|
| 차단 오버레이 할일 표시 | 차단된 앱 실행 시 목표/할일 표시로 동기 상기 |
| 세션 종료 개별 체크 | 30초 타이머 + 개별 할일 체크 UI |
| 30초 미응답 처리 | 필수=미완료, 일반=미응답 |
| 목표 상태 자동 도출 | 할일 결과에서 목표 달성 여부 자동 계산 |

---

## 📚 사전 작업 / 사후 작업 가이드

### 🔍 사전 작업 (Pre-Work)

각 Phase 시작 전에 반드시 수행해야 하는 작업입니다.

| 항목 | 설명 |
|------|------|
| **기획서 확인** | [10_추가기능_할일관리기능.md](./10_추가기능_할일관리기능.md) 관련 섹션 재확인 |
| **이전 작업 결과 확인** | `working_history/version_1.1/` 디렉토리에서 **직전 Phase 작업결과서** 확인 |
| **현재 코드 상태 파악** | 이전 작업에서 변경된 파일 및 구조 파악 |
| **빌드 상태 확인** | `./gradlew assembleDebug` 성공 확인 후 작업 시작 |

**참고할 작업결과서 패턴**: `{Phase번호}_{작업타이틀}_{작업일자}.md`

> **💡 목적**: 작업의 **일관성 유지** 및 이전 작업과의 **맥락 연결**

---

### 📝 사후 작업 (Post-Work)

각 Phase 완료 후에 반드시 수행해야 하는 작업입니다.

| 단계 | 설명 |
|------|------|
| **1. 검증** | 해당 Phase의 `검증` 섹션 항목 모두 통과 확인 |
| **2. 빌드 확인** | `./gradlew assembleDebug` 성공 확인 |
| **3. 작업결과서 작성** | 아래 템플릿 참고하여 작성 |
| **4. 커밋** | 의미 있는 단위로 Git 커밋 |

**작업결과서 저장 경로**: 
```
working_history/version_1.1/{Phase번호}_{작업타이틀}_{YYYY-MM-DD}.md
```

**예시**:
- `Phase1_데이터레이어확장_2025-12-26.md`
- `Phase2_도메인UseCase_2025-12-27.md`

**작업결과서 템플릿**: [working_history_template.md](../../working_history/version_1.1/working_history_template.md)

**작업결과서 필수 포함 내용**:
```markdown
# 작업 기록: {작업 제목}

**작업 일시**: YYYY-MM-DD  
**작업 범위**: {작업 범위 요약}

## 문제 상황 / 작업 배경
- 이전 작업 결과 참고 내용
- 본 Phase의 목적

## 수정 사항
- 변경된 파일 목록
- 주요 변경 내용 (코드 스니펫 포함)

## 검증 방법
- 실행한 테스트 명령
- 수동 QA 결과

## 관련 파일
- 수정된 파일 전체 목록

## 참고 사항
- 다음 Phase에서 주의할 점
- 알려진 이슈

---
**작업 완료일**: YYYY-MM-DD
**작업자**: {작업자}
```

> **💡 목적**: **작업 이력 추적** 및 다음 작업의 **사전 작업 자료**로 활용

---

## Phase 0. 준비 단계 (Day 0) ✅

### 📥 사전 작업
- [x] 기획서 전체 내용 숙지 (`10_추가기능_할일관리기능.md`)
- [x] 현재 프로젝트 빌드 상태 확인

### ✅ 작업 내용
- [x] 기획서 요구사항 재확인
- [x] 현재 Room DB 버전 확인 (`DetoxyDatabase.kt` - v8)
- [x] 기존 AutoRun 스케줄 생성/편집 플로우 QA
- [x] Migration 테스트용 dummy DB 덤프 준비

### 📤 사후 작업
- [x] Phase0_준비단계_{날짜}.md 작성
- [x] 확인된 DB 버전 및 Migration 번호 문서화 (v8 → v9)
- [x] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 1. 데이터 레이어 확장 (Day 1) ✅

> **📄 상세 작업 지시서**: [details/phase1_데이터레이어확장.md](./details/phase1_데이터레이어확장.md)

### 📥 사전 작업
- [x] Phase 0 작업결과서 확인 (`Phase0_준비단계_{날짜}.md`)
- [x] 확정된 DB 버전 및 Migration 번호 확인
- [x] 기획서 섹션 3 (데이터 스키마 설계) 재확인

### ✅ 작업 내용
- [x] `ScheduleInfo`, `ScheduleTodo` data class 생성
- [x] `TimeBasedAutoRun`에 `scheduleInfoJson` 컬럼 추가
- [x] `LocationBasedAutoRun`에 동일 컬럼 추가
- [x] `ScheduleInfoConverter` TypeConverter 구현
- [x] `FocusSessionTodoResultEntity` 생성
- [x] Enum TypeConverters 구현
- [x] Migration 클래스 작성 및 테스트 (v8 → v9)
- [x] `FocusSessionTodoResultDao` 구현

**검증**: ✅ Migration 테스트 통과, DB 스키마 변경 후 앱 정상 실행 (BUILD SUCCESSFUL)

### 📤 사후 작업
- [x] Phase1_데이터레이어확장_{날짜}.md 작성
- [x] 생성된 Entity/DAO 목록 문서화
- [x] Migration 버전 정보 기록 (v8 → v9)
- [x] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 2. 도메인 & UseCase (Day 2) ✅

> **📄 상세 작업 지시서**: [details/phase2_도메인UseCase.md](./details/phase2_도메인UseCase.md)

### 📥 사전 작업
- [x] Phase 1 작업결과서 확인 (`Phase1_데이터레이어확장_{date}.md`)
- [x] 생성된 Entity 구조 확인
- [x] 기획서 섹션 3.3-3.5 (핵심 함수) 재확인

### ✅ 작업 내용
- [x] `ScheduleInfoValidation` object 구현
- [x] `validateScheduleInfo()` 함수 구현
- [x] `TodoStatus` data class 정의 (Phase 1에서 완료)
- [x] `buildTodoResult()` 함수 구현 (목표 상태 자동 도출 포함)
- [x] `deriveGoalStatusFromTodos()` 함수 구현
- [x] `getStatusWithRequiredFallback()` 함수 구현
- [x] `getScheduleTitleSnapshot()` 함수 구현
- [x] UseCase 패턴 미적용 확인 (Repository 직접 사용)

**검증**: ✅ 빌드 성공 (BUILD SUCCESSFUL in 8s)

### 📤 사후 작업
- [x] Phase2_도메인UseCase_{date}.md 작성
- [x] 핵심 함수 시그니처 및 로직 문서화
- [x] UseCase 변경 사항 기록 (UseCase 패턴 미적용 확인)
- [x] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 3. 스케줄 생성/편집 UI (Day 3-4) ✅

> **📄 상세 작업 지시서**: [details/phase3_스케줄UI.md](./details/phase3_스케줄UI.md)

### 📥 사전 작업
- [x] Phase 2 작업결과서 확인 (`Phase2_도메인UseCase_{date}.md`)
- [x] 검증 로직 및 UseCase 인터페이스 확인
- [x] 기획서 섹션 6.1-6.2 (UI 설계) 재확인

### ✅ 작업 내용
- [x] `GoalInputSection` Composable 생성
- [x] `TodoListSection` Composable 생성
- [x] `TodoItemRow` Composable 생성
- [x] `AddTimeBasedAutoRunDialog`에 새 섹션 통합
- [x] `AddLocationAutoRunDialog`에 동일 적용
- [x] 로컬 상태로 scheduleInfo 관리 (ViewModel 확장 불필요)
- [x] Validation 실시간 피드백 UI (글자 수 제한 표시)

**검증**: ✅ 빌드 성공 (BUILD SUCCESSFUL in 7s)

### 📤 사후 작업
- [x] Phase3_스케줄UI_{date}.md 작성
- [x] 생성된 Composable 목록 문서화
- [x] 다이얼로그 통합 내용 기록
- [x] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 4. 차단 오버레이 할일 표시 (Day 5) ✅

> **📄 상세 작업 지시서**: [details/phase4_오버레이표시.md](./details/phase4_오버레이표시.md)

> **🎯 핵심 MVP 기능**: 차단된 앱 실행 시 목표/할일 표시

### 📥 사전 작업
- [x] Phase 3 작업결과서 확인 (`Phase3_스케줄UI_2025-12-26.md`)
- [x] ScheduleInfo 저장 구조 확인
- [x] 기획서 섹션 6.1 (오버레이 표시 규칙) 재확인

### ✅ 작업 내용
- [x] `OverlayDisplayRules` object 구현
- [x] `truncateForOverlay()` 함수 구현
- [x] `LockOverlayService`에 할일 표시 UI 추가
- [x] `PreferenceManager`에 프라이버시 설정 추가
- [x] 설정 화면에 프라이버시 옵션 UI 생성
- [x] 우선순위 규칙 구현 (OverlayDisplayMode)

**검증**: ✅ BUILD SUCCESSFUL in 6s

### 📤 사후 작업
- [x] Phase4_오버레이표시_{date}.md 작성
- [ ] 오버레이 스크린샷 첨부 (수동 QA 후)
- [x] 프라이버시 설정 키 목록 기록
- [x] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 5. 세션 종료 다이얼로그 + 개별 체크 (Day 6) ✅

> **📄 상세 작업 지시서**: [details/phase5_세션종료다이얼로그.md](./details/phase5_세션종료다이얼로그.md)

### 📥 사전 작업
- [x] Phase 4 작업결과서 확인 (`Phase4_오버레이표시_2025-12-26.md`)
- [x] 현재 세션 종료 플로우 분석
- [x] 기획서 섹션 6.3 (세션 종료 다이얼로그) 재확인

### ✅ 작업 내용
- [x] `resolveSessionEndDialogType()` 함수 구현
- [x] `SessionEndTodoDialog` Composable 생성
- [x] `SessionEndGoalDialog` Composable 생성
- [x] 30초 타이머 구현
- [x] `handleNoResponse()` 함수 구현
- [x] `deriveGoalStatusFromTodos()` 목표 상태 자동 도출 (Phase 2에서 구현됨)
- [ ] ViewModel 통합 및 `buildTodoResult()` 호출 (통합 작업 필요)

**검증**: ✅ BUILD SUCCESSFUL in 9s

### 📤 사후 작업
- [x] Phase5_세션종료다이얼로그_2025-12-26.md 작성
- [ ] 다이얼로그 UI 스크린샷 첨부 (수동 QA 후)
- [x] 30초 타이머 로직 및 결과 저장 흐름 기록
- [x] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 6. 할일 관리 페이지 (Day 7)

> **📄 상세 작업 지시서**: [details/phase6_할일관리페이지.md](./details/phase6_할일관리페이지.md)

### 📥 사전 작업
- [ ] Phase 5 작업결과서 확인 (`Phase5_세션종료다이얼로그_{날짜}.md`)
- [ ] FocusSessionTodoResult 저장 구조 확인
- [ ] 기획서 섹션 9 (할일 관리 페이지) 재확인

### ✅ 작업 내용
- [ ] 스케줄 탭 내 서브 탭으로 "할일 관리" 추가
- [ ] `TodoManagementScreen` Composable 생성
- [ ] 수정 가능 항목 필터링 (NO_RESPONSE + NOT_COMPLETED)
- [ ] 목표(isGoal=true) 수정 가능
- [ ] `EditTodoResultDialog` 구현
- [ ] `lastModifiedAt` 업데이트
- [ ] 세션 중 접근 제한

**검증**: 할일 관리 페이지 정상 동작, 미응답/미완료 항목 수정 가능

### 📤 사후 작업
- [ ] Phase6_할일관리페이지_{날짜}.md 작성
- [ ] 관리 페이지 UI 스크린샷 첨부
- [ ] 수정 가능 항목 정책 문서화
- [ ] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## Phase 7. QA & 문서화 (Day 8)

> **📄 상세 작업 지시서**: [details/phase7_QA문서화.md](./details/phase7_QA문서화.md)

### 📥 사전 작업
- [ ] Phase 1-6 작업결과서 전체 확인
- [ ] 각 Phase에서 발생한 이슈 목록 취합
- [ ] 기획서 섹션 8 (MVP 범위) 재확인

### ✅ 작업 내용
- [ ] 생성 플로우 QA
- [ ] 오버레이 플로우 QA
- [ ] 세션 종료 플로우 QA
- [ ] 관리 페이지 플로우 QA
- [ ] Migration 테스트
- [ ] JSON 파싱 fallback 확인
- [ ] 기획서 MVP 완료 현황 업데이트
- [ ] 릴리스 노트 초안 작성

**검증**: 모든 QA 항목 통과, 문서 업데이트 완료

### 📤 사후 작업
- [ ] Phase7_QA문서화_{날짜}.md 작성
- [ ] QA 체크리스트 결과 기록
- [ ] 발견된 버그 및 해결 내역 문서화
- [ ] 릴리스 노트 최종 버전 저장
- [ ] ✅ 본 문서 및 상세 작업 지시서의 체크리스트 완료 표시

---

## 🚨 리스크 및 대응

| 리스크 | 영향도 | 대응 방안 |
|--------|--------|-----------|
| Migration 실패 | 높음 | fallback 처리 정의, Migration Test 필수 |
| JSON 파싱 오류 | 중간 | try-catch + 빈 목록 fallback |
| 오버레이 성능 | 중간 | 최대 3개 + 캐싱 |
| 프라이버시 노출 | 중간 | 표시 옵션 제공 |
| 통계 왜곡 | 중간 | 목표 상태 자동 도출 규칙 정의 |

---

## 📝 핵심 규칙 요약

### 목표 상태 자동 도출 규칙
| 조건 | 목표 상태 결정 | 수정 가능 |
|------|---------------|-----------|
| 목표만 있음 | 사용자 입력 (예/아니오) | ✅ 수정 가능 |
| 목표 + 할일 | 할일 결과에서 자동 도출 | ❌ 직접 수정 불가 (할일 수정으로 간접 변경) |
| 할일만 있음 | 목표 없음 | - |

> **🔄 7차 리뷰 반영**: 목표+할일 동시 존재 시 **목표 직접 수정 불가**.
> 목표 상태를 바꾸려면 할일 항목을 수정해야 함 (자동 도출 규칙 유지).

### 30초 무응답 처리 규칙
| 항목 | 처리 |
|------|------|
| 필수 항목 | NOT_COMPLETED |
| 일반 항목 | NO_RESPONSE |
| 목표 (목표만) | NOT_COMPLETED |
| 목표 (목표+할일) | 자동 도출 |

### 관리 페이지 수정 가능 항목
| 상태 | 수정 가능 |
|------|----------|
| NO_RESPONSE | ✅ |
| NOT_COMPLETED | ✅ |
| COMPLETED | ❌ |

---

*최종 수정일: 2025-12-25 · 작성자: AI Assistant*
