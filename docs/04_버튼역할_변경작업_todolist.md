# 스케줄 그룹 버튼 역할 변경 작업 계획서

> **관련 PRD**: [04_버튼역할변경_prd.md](./04_버튼역할변경_prd.md)  
> **작성일**: 2025-12-10  
> **예상 총 소요**: 5일  
> **상태**: 작업 대기

---

## 📋 작업 규칙

### 사전작업 규칙
- 이전 단계의 작업결과서(`working_history/`)를 반드시 확인
- 이전 작업에서 발생한 이슈나 교훈을 현재 작업에 반영
- 관련 코드의 현재 상태를 grep/read_file로 확인
- 의존성 있는 파일들의 변경사항 체크

### 작업 규칙
- PRD의 설계 내용을 기반으로 구현
- 한 번에 하나의 파일/기능씩 수정
- 수정 후 즉시 컴파일 확인 (`./gradlew compileDebugKotlin`)
- 복잡한 로직은 단위 테스트 우선 작성

### 작업후처리 규칙
- 작업 완료 후 `working_history/version_2.0/{단계번호}_{작업타이틀}_{날짜}.md` 파일 작성
- [working_history_template.md](../working_history/version_2.0/working_history_template.md) 형식 준수
- 다음 작업을 위한 주의사항/교훈 반드시 기록
- 관련 커밋 ID 기록

---

## 🔧 단계별 작업 계획

---

### 📌 단계 1: DB 마이그레이션 (v7→v8)

**예상 소요**: 0.5일

#### 사전작업
- [x] 현재 Room 버전 확인 (`DetoxyDatabase.kt`의 `version` 확인) → v7
- [x] 기존 마이그레이션 파일 구조 확인 (`data/local/migration/`)
- [x] `ScheduleGroup.kt` 현재 필드 목록 확인
- [x] 이전 마이그레이션 작업 결과서 참조 (있는 경우)

#### 작업
- [x] **1.1** `ScheduleGroup.kt` 엔티티에 새 필드 추가
  ```kotlin
  // 추가할 필드
  val manualOverrideState: String? = null  // "INACTIVE", "PAUSED", null
  val pauseUntil: Long? = null
  ```

- [x] **1.2** `MIGRATION_7_8` 클래스 생성
  - 파일 위치: `data/local/migration/Migration_7_8.kt`
  - ALTER TABLE 쿼리 작성
  - 기존 `isActive=false` 데이터 마이그레이션

- [x] **1.3** `DetoxyDatabase.kt` 수정
  - `version = 8` 으로 변경
  - DatabaseModule.kt에 `addMigrations(MIGRATION_7_8)` 추가

- [x] **1.4** `ScheduleGroupDao.kt`에 새 쿼리 추가
  ```kotlin
  @Query("UPDATE schedule_group SET manualOverrideState = :state, pauseUntil = :pauseUntil WHERE id = :groupId")
  suspend fun updateManualOverride(groupId: String, state: String?, pauseUntil: Long?)
  
  // + clearExpiredPauses() 쿼리도 추가
  ```

- [x] **1.5** 컴파일 및 마이그레이션 테스트
  - `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL ✅
  - 에뮬레이터에서 앱 실행하여 마이그레이션 확인 (다음 단계에서)

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/01_DB마이그레이션_v8_2025-12-10.md`
- [x] 기록할 내용:
  - 마이그레이션 쿼리 전문
  - 테스트 결과 (기존 데이터 보존 확인)
  - 다음 단계 주의사항

---

### 📌 단계 2: ScheduleControlButton 컴포넌트 구현

**예상 소요**: 1일

#### 사전작업
- [ ] 단계 1 작업결과서 확인
- [ ] 기존 UI 컴포넌트 스타일 확인 (`presentation/ui/autorun/components/`)
- [ ] Material3 DropdownMenu 사용법 확인
- [ ] `combinedClickable` modifier 사용법 확인

#### 작업
- [ ] **2.1** `ScheduleGroupControlState.kt` enum 생성
  - 파일 위치: `domain/model/ScheduleGroupControlState.kt`
  ```kotlin
  enum class ScheduleGroupControlState {
      ACTIVE,    // 활성
      PAUSED,    // 일시중지
      INACTIVE   // 비활성
  }
  ```

- [ ] **2.2** `PauseDuration.kt` enum 생성
  - 파일 위치: `domain/model/PauseDuration.kt`
  ```kotlin
  enum class PauseDuration(val displayName: String, val durationMillis: Long) {
      ONE_HOUR("1시간", 3600000L),
      TWO_HOURS("2시간", 7200000L),
      TODAY("오늘 하루", -1L),
      TOMORROW("내일까지", -2L)
  }
  ```

- [ ] **2.3** `ScheduleControlButton.kt` 컴포넌트 구현
  - 파일 위치: `presentation/ui/autorun/components/ScheduleControlButton.kt`
  - 상태별 색상/아이콘 정의
  - 탭 동작 (토글)
  - 롱프레스/▾ 탭 동작 (드롭다운)
  - 드롭다운 메뉴 (활성/일시중지 옵션/비활성)

- [ ] **2.4** `formatRemainingTime()` 유틸 함수 구현
  - 남은 시간 표시 포맷 (예: "59분", "1시간 30분")

- [ ] **2.5** Preview 작성 및 UI 확인
  - `@Preview` 어노테이션으로 각 상태별 미리보기

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/02_ScheduleControlButton구현_{날짜}.md`
- [ ] 기록할 내용:
  - 컴포넌트 사용법
  - 스크린샷 (각 상태별)
  - 드롭다운 메뉴 동작 확인

---

### 📌 단계 3: ScheduleGroupCard 수정

**예상 소요**: 0.5일

#### 사전작업
- [ ] 단계 2 작업결과서 확인
- [ ] 현재 `ScheduleGroupCard.kt` 코드 확인
- [ ] 기존 Switch 제거 위치 파악
- [ ] `ScheduleGroupScreen.kt`에서 Card 사용 방식 확인

#### 작업
- [ ] **3.1** `ScheduleGroupCard.kt` 파라미터 수정
  - 기존: `isActive: Boolean`, `onActivate: () -> Unit`
  - 변경: `controlState: ScheduleGroupControlState`, `onStateChange`, `onPause`

- [ ] **3.2** Switch → ScheduleControlButton 교체
  - 헤더 영역의 Switch 제거
  - ScheduleControlButton 삽입

- [ ] **3.3** 카드 배경색 상태별 적용
  ```kotlin
  val cardBackgroundColor = when (controlState) {
      ACTIVE -> primaryContainer
      PAUSED -> tertiaryContainer
      INACTIVE -> surfaceVariant
  }
  ```

- [ ] **3.4** `ScheduleGroupScreen.kt` 수정
  - Card에 새로운 콜백 연결
  - ViewModel의 getControlState() 호출

- [ ] **3.5** UI 테스트
  - 각 상태별 카드 표시 확인
  - 버튼 동작 확인

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/03_ScheduleGroupCard수정_{날짜}.md`
- [ ] 기록할 내용:
  - 변경된 파라미터 목록
  - UI 스크린샷
  - 호환성 이슈 (있는 경우)

---

### 📌 단계 4: ViewModel/Repository 수정

**예상 소요**: 1일

#### 사전작업
- [ ] 단계 3 작업결과서 확인
- [ ] 현재 `ScheduleGroupViewModel.kt` 구조 확인
- [ ] 현재 `ScheduleGroupRepository.kt` 인터페이스 확인
- [ ] `ScheduleGroupManager.kt` 의존성 확인

#### 작업
- [ ] **4.1** `ScheduleGroupRepository.kt` 인터페이스 추가
  ```kotlin
  suspend fun updateManualOverride(groupId: String, overrideState: String?, pauseUntil: Long?)
  ```

- [ ] **4.2** `ScheduleGroupRepositoryImpl.kt` 구현
  - updateManualOverride 메서드 구현
  - DAO 호출

- [ ] **4.3** `ScheduleGroupViewModel.kt` 수정
  - `changeControlState(groupId, newState)` 함수 추가
  - `pauseScheduleGroup(groupId, duration)` 함수 추가
  - `getControlState(group)` 함수 추가
  - `calculateMidnight()`, `calculateTomorrowMidnight()` 유틸 함수

- [ ] **4.4** 상태 변경 로직 구현
  - ACTIVE: manualOverride 해제 + activateGroup
  - INACTIVE: manualOverride 설정 + deactivateGroup
  - PAUSED: manualOverride 설정 + pauseUntil 설정

- [ ] **4.5** 일시중지 만료 체크 로직
  - pauseUntil 비교하여 만료 시 ACTIVE 반환

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/04_ViewModel_Repository수정_{날짜}.md`
- [ ] 기록할 내용:
  - 새로 추가된 함수 목록
  - 상태 전환 플로우 다이어그램
  - 단위 테스트 결과 (있는 경우)

---

### 📌 단계 5: GeofenceTransitionsReceiver 수정

**예상 소요**: 0.5일

#### 사전작업
- [ ] 단계 4 작업결과서 확인
- [ ] 현재 `GeofenceTransitionsReceiver.kt` 코드 확인
- [ ] `handleGeofenceTrigger` 함수 흐름 파악
- [ ] `ScheduleGroupDao` 접근 방법 확인 (EntryPoint)

#### 작업
- [ ] **5.1** `GeofenceReceiverEntryPoint`에 ScheduleGroupDao 추가
  ```kotlin
  fun scheduleGroupDao(): ScheduleGroupDao
  ```

- [ ] **5.2** `handleGeofenceTrigger` 수정
  - 스케줄 그룹의 manualOverrideState 확인 로직 추가
  - INACTIVE: 위치 진입 무시 (로그만 기록)
  - PAUSED: pauseUntil 확인 후 처리
    - 만료 안됨: skipAlarmTrigger=true로 활성화
    - 만료됨: 정상 활성화
  - null: 정상 활성화 (기존 동작)

- [ ] **5.3** 로그 추가
  - 각 분기별 상세 로그 추가
  - Analytics 이벤트 추가 (선택)

- [ ] **5.4** 테스트
  - 각 상태에서 위치 진입 시뮬레이션
  - 로그 확인

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/05_GeofenceReceiver수정_{날짜}.md`
- [ ] 기록할 내용:
  - 수정된 분기 로직 플로우
  - 테스트 시나리오 및 결과
  - 위치 기반 테스트 방법 (에뮬레이터 위치 변경)

---

### 📌 단계 6: AutoRunAlarmReceiver 수정

**예상 소요**: 0.5일

#### 사전작업
- [ ] 단계 5 작업결과서 확인
- [ ] 현재 `AutoRunAlarmReceiver.kt` 코드 확인
- [ ] `handleTimeBasedAutoRun` 함수 흐름 파악
- [ ] 기존 일시중지 로직 확인 (`userSettingsRepository.isAutoRunEnabled()`)

#### 작업
- [ ] **6.1** `AutoRunAlarmReceiverEntryPoint`에 ScheduleGroupDao 추가

- [ ] **6.2** `handleTimeBasedAutoRun` 수정
  - scheduleGroupId가 있는 경우 그룹 상태 확인
  - INACTIVE: 건너뛰기 + 로그 기록
  - PAUSED: pauseUntil 확인 후 처리
  - 정상: 기존 로직 실행

- [ ] **6.3** Skip 로그 기록 함수 수정
  - `logAutoRunSkipped` 에 새로운 reason 추가
    - `SCHEDULE_GROUP_INACTIVE`
    - `SCHEDULE_GROUP_PAUSED`

- [ ] **6.4** 테스트
  - 각 상태에서 알람 트리거 시뮬레이션
  - 로그 및 AutoRunLog 테이블 확인

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/06_AutoRunAlarmReceiver수정_{날짜}.md`
- [ ] 기록할 내용:
  - 수정된 분기 로직
  - 기존 마스터 스위치와의 우선순위 관계
  - 테스트 결과

---

### 📌 단계 7: 통합 테스트 및 버그 수정

**예상 소요**: 1일

#### 사전작업
- [ ] 단계 1~6 모든 작업결과서 확인
- [ ] 각 단계에서 기록된 주의사항/교훈 정리
- [ ] 테스트 시나리오 체크리스트 준비

#### 작업

##### 7.1 기능 테스트
- [ ] **TC-01**: 활성 상태에서 버튼 탭 → 비활성 전환
- [ ] **TC-02**: 비활성 상태에서 버튼 탭 → 활성 전환
- [ ] **TC-03**: 일시중지 상태에서 버튼 탭 → 활성 전환
- [ ] **TC-04**: 드롭다운 → 1시간 일시중지 → 1시간 후 자동 해제
- [ ] **TC-05**: 일시중지 중 시간 도래 → 타이머 시작 안됨
- [ ] **TC-06**: 일시중지 만료 후 시간 도래 → 정상 타이머 시작

##### 7.2 위치기반 통합 테스트
- [ ] **TC-07**: 비활성 그룹 + 위치 진입 → 활성화 안됨
- [ ] **TC-08**: 일시중지 그룹 + 위치 진입 → 알람 트리거 안됨
- [ ] **TC-09**: 활성 그룹 + 위치 이탈 → 자동 비활성화
- [ ] **TC-10**: 비활성 → 활성화 → Geofence 재등록 확인

##### 7.3 엣지 케이스 테스트
- [ ] **TC-11**: 앱 강제 종료 후 재시작 시 상태 유지
- [ ] **TC-12**: 마스터 스위치 OFF + 그룹 활성 조합
- [ ] **TC-13**: 일시중지 중 앱 업데이트/재설치

##### 7.4 버그 수정
- [ ] 발견된 버그 수정
- [ ] 회귀 테스트

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/07_통합테스트_버그수정_{날짜}.md`
- [ ] 기록할 내용:
  - 테스트 결과 요약표
  - 발견된 버그 및 수정 내용
  - 최종 릴리스 체크리스트

---

## 📊 진행 상황 추적

| 단계 | 작업명 | 상태 | 시작일 | 완료일 | 작업결과서 |
|------|--------|------|--------|--------|-----------|
| 1 | DB 마이그레이션 | ✅ 완료 | 2025-12-10 | 2025-12-10 | [01_DB마이그레이션_v8_2025-12-10.md](../working_history/version_2.0/01_DB마이그레이션_v8_2025-12-10.md) |
| 2 | ScheduleControlButton 구현 | ⬜ 대기 | - | - | - |
| 3 | ScheduleGroupCard 수정 | ⬜ 대기 | - | - | - |
| 4 | ViewModel/Repository 수정 | ⬜ 대기 | - | - | - |
| 5 | GeofenceTransitionsReceiver 수정 | ⬜ 대기 | - | - | - |
| 6 | AutoRunAlarmReceiver 수정 | ⬜ 대기 | - | - | - |
| 7 | 통합 테스트 및 버그 수정 | ⬜ 대기 | - | - | - |

**상태 범례**: ⬜ 대기 | 🔄 진행중 | ✅ 완료 | ⏸️ 보류

---

## 🔗 관련 문서

- **PRD**: [04_버튼역할변경_prd.md](./04_버튼역할변경_prd.md)
- **작업결과서 템플릿**: [working_history_template.md](../working_history/version_2.0/working_history_template.md)

### 작업결과서 파일명 규칙

```
working_history/version_2.0/{단계번호}_{작업타이틀}_{YYYY-MM-DD}.md

예시:
- 01_DB마이그레이션_v8_2025-12-11.md
- 02_ScheduleControlButton구현_2025-12-11.md
- 03_ScheduleGroupCard수정_2025-12-12.md
- 04_ViewModel_Repository수정_2025-12-12.md
- 05_GeofenceReceiver수정_2025-12-13.md
- 06_AutoRunAlarmReceiver수정_2025-12-13.md
- 07_통합테스트_버그수정_2025-12-14.md
```

---

## ⚠️ 주의사항

### 의존성 순서
```
단계 1 (DB) → 단계 2 (UI 컴포넌트) → 단계 3 (Card 수정)
                                          ↓
단계 4 (ViewModel) → 단계 5 (Geofence) → 단계 6 (Alarm)
                                          ↓
                                    단계 7 (테스트)
```

### 롤백 계획
- 각 단계 완료 후 커밋 생성
- 문제 발생 시 해당 단계 커밋으로 롤백
- 마이그레이션 실패 시 `fallbackToDestructiveMigration()` 고려 (개발 단계만)

### 테스트 디바이스
- 최소 Android 8.0 (API 26) 이상
- 위치 기반 테스트: 에뮬레이터 위치 설정 또는 실제 디바이스

---

**최종 수정일**: 2025-12-10

