# Allday Detoxy 1차 고도화 작업 계획

- 기준 요약: [1차 고도화 PRD](./01_advanced_prd.md) 참고.
- 레퍼런스: MVP 진행 기록은 `working_history/` 내 문서를 확인.

## 0. 개요
- **목표**: 집중모드 설정 화면 신설과 리포트 고도화 기능을 3주 이내 구현하여 1차 고도화 PRD 요구사항을 충족.
- **주요 마일스톤** ([PRD §4](./01_advanced_prd.md#4-기능-요구사항)):
  1. 주차 1: 차단 카테고리 정의 및 데이터 기반 정비
  2. 주차 2: 집중모드 설정 UI/로직 완성
  3. 주차 3: 리포트 고도화, QA, 배포 준비
- **사전 조건**: MVP v4.3 코드 베이스, 주요 권한(Accessibility, Overlay, DND) 정상 작동 상태.

---

## 1. 준비 단계 (Week 0.5)
- [x] PRD 리뷰 및 화면 와이어프레임 작성 → [01_advanced_wireframe_spec.md](./01_advanced_wireframe_spec.md) (UI 스펙, 컴포넌트 상세)
- [x] 차단 카테고리별 패키지 리스트 초안 정리 → [01_advanced_app_category_mapping.md](./01_advanced_app_category_mapping.md) (40개 앱, 5개 카테고리)
- [x] Room 마이그레이션 전략 수립 → [01_advanced_room_migration_strategy.md](./01_advanced_room_migration_strategy.md) (v1→v2→v3)
- [x] 로깅 스키마 설계 → [01_advanced_analytics_schema.md](./01_advanced_analytics_schema.md) (15개 이벤트, KPI)
- [x] QA 기기 리스트 확정 → [01_advanced_qa_devices.md](./01_advanced_qa_devices.md) (5개 기기, 7개 시나리오)

**작업 기록**: [working_history/2025-10-12_advanced_1.0.md](../working_history/2025-10-12_advanced_1.0.md)

---

## 2. 집중모드 설정 화면 구축 (Week 1)

### 2.1 데이터 & 도메인 준비
- [x] `core/utils`에 카테고리 enum 및 패키지 매핑 정의 → [카테고리 목록](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[카테고리 매핑 스펙](./01_advanced_app_category_mapping.md)** (40개 앱, 5개 카테고리, 프리셋 정의) - `AppCategory.kt`, `AppCategoryMapper.kt` 생성
- [x] `FocusAccessibilityService`와 `LockOverlayService`에서 동적 차단 목록을 참조하도록 인터페이스 정리 → [동작 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [차단 로직](../working_history/2025-10-05_1.3.md), **[카테고리 매핑](./01_advanced_app_category_mapping.md#데이터-구조-설계)** - `updateBlockSettings()`, `applyPreset()` API 추가
- [x] DndManager는 세션 시작/종료 시 글로벌 DND 토글만 수행하고, 권한 상태 노출/안내 문구를 보강 → [DND 안내](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [DND 제어 작업](../working_history/2025-10-06_2.2.md) - `DndPermissionState` enum, `getPermissionState()`, `getDndCardInfo()` 추가
- [x] 집중모드 중 모니터링할 패키지/카테고리 범위를 정의하고, 허용 앱 이벤트 로깅 정책을 수립 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[Analytics 스키마](./01_advanced_analytics_schema.md#31-디톡시-제어-설정-이벤트)** - `MonitoringPolicy.kt` 생성 (범위, 이벤트, 파라미터 정의)

**작업 기록**: [working_history/2025-10-13_1st_advanced_2.1.md](../working_history/2025-10-13_1st_advanced_2.1.md)

### 2.2 UI/UX 구현
- [x] `presentation/ui/settings/focus/DetoxyControlSettingsScreen` Compose 레이아웃 구현 → [UI 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[Wireframe 스펙](./01_advanced_wireframe_spec.md#1-디톡시-제어-설정-화면-detoxycontrolsettingsscreen)** (레이아웃, 컴포넌트 상세), [네비게이션 구조](../working_history/2025-10-11_3.3.md) - `DetoxyControlSettingsScreen.kt` 구현 완료
- [x] 프리셋(완전 차단/표준 디톡시/완화) 데이터 모델 및 Preview UI 작성 → [레이아웃 섹션](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[프리셋 정의](./01_advanced_app_category_mapping.md#프리셋-정의-디톡시-제어-관점)** (완전 차단/표준/완화 스펙) - `PresetSelectionSection` 구현
- [x] 카테고리별 토글/슬라이더 상태를 `FocusSettingsViewModel`에서 StateFlow로 관리 → [동작 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[Wireframe 컴포넌트](./01_advanced_wireframe_spec.md#13-컴포넌트-상세)** - `FocusSettingsViewModel.kt` 구현 (DataStore 영속화)
- [x] 권한 상태 카드(DND, 접근성) 연결 및 재요청 Intent 처리 → [권한 경고](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [권한 온보딩](../working_history/2025-10-12_4.1.md), **[Wireframe 권한 카드](./01_advanced_wireframe_spec.md#13-컴포넌트-상세)** - `PermissionStatusSection` 구현 (Intent 연동)
- [x] 메신저 카테고리 초기 안내 배너/다이얼로그 구현 및 사용자 선택 흐름 정의 → [카테고리 구분 원칙](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[메신저 안내 UX](./01_advanced_app_category_mapping.md#2-메신저--커뮤니케이션)** (다이얼로그 flow), **[다이얼로그 스펙](./01_advanced_wireframe_spec.md#41-메신저-카테고리-안내-다이얼로그)** - `MessengerCategoryDialog` 구현
- [x] 디톡시 루틴(예: 야간 전체 차단) 토글 및 설명 UI 설계 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[루틴 연동](./01_advanced_app_category_mapping.md#2-메신저--커뮤니케이션)** - `DetoxyRoutineSection` 구현 (향후 활성화 예정)

**작업 기록**: [working_history/2025-10-15_1st_advanced_2.2.md](../working_history/2025-10-15_1st_advanced_2.2.md)

### 2.3 상태 저장 및 로직 연동
- [x] `FocusSettingsRepository` 설계(DataStore 기반) → [데이터 보강](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[Room 마이그레이션 v3](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)** (FocusSettings 엔티티) - `FocusSettingsRepository.kt` 구현 (196줄, Flow 기반)
- [x] 설정 저장 후 세션 시작 시 적용되는지 통합 테스트 → [사용자 시나리오 1~2](./01_advanced_prd.md#3-주요-사용자-시나리오), **[QA 시나리오 1-2](./01_advanced_qa_devices.md#41-디톡시-제어-설정-테스트)** - 빌드 및 Lint 테스트 통과
- [x] `TimerViewModel.startTimer`에서 최신 설정을 불러와 AccessibilityService/DndManager에 전달하도록 수정 → [동작 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[카테고리 매핑 로직](./01_advanced_app_category_mapping.md#데이터-구조-설계)** - `getCurrentSettings()` 호출 및 `updateBlockSettings()` 연동
- [x] 권한 미보유 시 동작 방어 로직 및 사용자 안내 구현 → [완료 기준](./01_advanced_prd.md#8-완료-기준dod), [Week1 권한 처리](../working_history/2025-10-05_1.2.md) - Task 2.2에서 이미 구현 (`PermissionErrorDialog`)
- [x] 메신저 카테고리 사용자 선택 상태를 저장/복원하고 프리셋 전환 시 충돌 로직 정의 → [카테고리 구분 원칙](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[메신저 UX](./01_advanced_app_category_mapping.md#긴급-연락-허용-ux)** - Task 2.2에서 이미 구현 (`messengerHasBeenEnabled` DataStore)
- [ ] UsageStats opt-in UI(권한 설명/동의/건너뛰기)를 설계하고 세션과의 연동 정책 정의 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[UsageStats 이벤트](./01_advanced_analytics_schema.md#usage_stats_opt_in-신규)** - Week 2B 작업
- [ ] 디톡시 루틴 스케줄/알림 설정 저장 구조 설계(`DetoxyRoutineLog` 초기 스펙) → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[DetoxyRoutineLog 엔티티](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)** - Week 2B 작업
- [x] Analytics 이벤트 로깅 구현 (`detoxy_settings_*`) → **[Analytics 스키마](./01_advanced_analytics_schema.md#31-디톡시-제어-설정-이벤트)**, **[AnalyticsHelper 구현](./01_advanced_analytics_schema.md#71-analytics-helper-클래스-업데이트)** - `AnalyticsHelper.kt` 생성 (249줄, 7개 이벤트)
- [x] 통합 테스트: Repository-ViewModel-AccessibilityService 연동 확인 - 빌드 성공, Lint 0 errors

**작업 기록**: [working_history/2025-10-15_1st_advanced_2.3.md](../working_history/2025-10-15_1st_advanced_2.3.md)

### 2.4 QA & 문서화
- [x] 설정 화면 네비게이션 추가 (MainActivity에 설정 탭 추가, 3탭 구조)
- [x] 통합 테스트 시나리오 정의 (설정 저장 → 타이머 시작 → 차단 확인)
- [x] Week 1 종합 보고서 작성 (`2025-10-15_1st_advanced_week1_complete.md`)
- [x] 빌드 검증 (compileDebugKotlin: SUCCESS, Lint: 0 errors)
- [x] `docs/01_advanced_prd.md` 업데이트 사항 확인 완료

**작업 기록**: [working_history/2025-10-15_1st_advanced_week1_complete.md](../working_history/2025-10-15_1st_advanced_week1_complete.md)

**✅ Week 1 완료**: 디톡시 제어 설정 화면 구축 완료 (7개 파일 생성, 5개 파일 수정, ~3,500줄)

---

## 3. 리포트 고도화 (Week 2A-2B, 11-13일)

> **⚠️ 중요**: Week 2는 작업량이 많아 2주로 분할합니다.
> - **Week 2A (5-6일)**: 데이터 기반 구축
> - **Week 2B (6-7일)**: 고급 통계 및 UI

---

### Week 2A: 데이터 기반 구축 (Day 1-6)

#### 2A.1 데이터 모델 확장 (Day 1-3)
- [x] Room 마이그레이션 v1→v2 작성 및 단위 테스트 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[마이그레이션 v1→v2](./01_advanced_room_migration_strategy.md#phase-1-v1--v2-week-2a)** (SQL, 테스트 전략) - `Migration_1_2.kt` (65줄)
- [x] `FocusSession`에 `interruptedSeconds`, `primaryDistractionCategory`, `giveUpReason` 필드 추가 → **[FocusSession 스키마](./01_advanced_room_migration_strategy.md#phase-1-v1--v2-week-2a)** - 3개 필드 추가
- [x] `FocusInterruption` 엔티티/DAO/Repository 설계 및 구현 (세션 중 차단 이벤트 로그) → **[FocusInterruption 엔티티](./01_advanced_room_migration_strategy.md#phase-1-v1--v2-week-2a)** - `FocusInterruption.kt` (48줄), `FocusInterruptionDao.kt` (99줄, 9개 함수)
- [x] 기존 데이터 백필 로직 (기존 세션 `interruptedSeconds = 0`) 적용 → **[마이그레이션 테스트](./01_advanced_room_migration_strategy.md#6-테스트-전략)** - `DEFAULT 0` 설정으로 자동 백필
- [x] 마이그레이션 통합 테스트: DB 버전 업그레이드 정상 동작 확인 → **[QA 시나리오 3-4](./01_advanced_qa_devices.md#42-room-마이그레이션-테스트)** - `assembleDebug` 성공, DB 버전 1→2

#### 2A.2 세션 종료 로직 개선 (Day 3-4)
- [x] 중도 포기 시 경과 시간(`elapsedSeconds`) 기록 로직 추가 → [정확성 요구사항](./01_advanced_prd.md#42-리포트-고도화), **[session_give_up 이벤트](./01_advanced_analytics_schema.md#session_give_up-확장)** - `endSessionWithDetails()` 메서드 추가
- [x] 차단/허용 이벤트 로그 → 세션과 연계 저장 구현 (카테고리 누락 방지) → [세션 기록 작업](../working_history/2025-10-06_2.3.md), **[session_interrupted](./01_advanced_analytics_schema.md#session_interrupted-신규)** - FocusAccessibilityService Repository 주입, handleBlockedApp() 수정
- [x] `TimerViewModel.giveUp()` 수정: 경과 시간 계산 및 저장 → **[FocusSession 필드](./01_advanced_room_migration_strategy.md#phase-1-v1--v2-week-2a)** - `interruptedSeconds = totalSeconds - remainingSeconds`
- [x] 통합 테스트: 세션 시작→중도 포기→DB 저장→리포트 반영 플로우 검증 → **[QA 핵심 기능](./01_advanced_qa_devices.md#61-mvp-기능-회귀-방지)** - `assembleDebug` 성공, Lint 0 errors

#### 2A.3 기본 통계 계산 모듈 (Day 5-6)
- [x] 총 집중 시간 계산 유틸 (성공+실패 세션 포함) → [지표 확장](./01_advanced_prd.md#42-리포트-고도화), `calculateTotalFocusTime()` - 성공: durationMinutes*60, 실패: interruptedSeconds
- [x] 집중률 계산 함수 (성공 세션 / 전체 세션) → `calculateFocusRate()` - (성공 세션 수 / 전체 세션 수) * 100
- [x] 평균 집중 유지 시간 계산 (성공/실패 분리) → `calculateAverageFocusDuration()` - Pair<성공 평균, 실패 평균> (초 단위)
- [x] 포인트 누적 추세 계산 (7일/30일 기준) → `calculateDailyPointsTrend()` - Map<날짜, 포인트>, 성공 세션만 포인트 획득
- [x] FocusSessionDao에 `getSessionsInLastDays()`, `getSessionsInRange()` 쿼리 추가 → **작업 문서**: [2025-10-19_1st_advanced_2A.3.md](../working_history/2025-10-19_1st_advanced_2A.3.md)
- [x] FocusStatisticsCalculator 유틸리티 생성 (@Singleton, 5개 함수, 163줄) → `calculateSummary()` 통합 통계 반환

---

### Week 2B: 고급 통계 및 UI (Day 7-13)

#### 2B.1 고급 데이터 모델 (Day 7-8)
- [x] Room 마이그레이션 v2→v3 작성 및 단위 테스트 → **[마이그레이션 v2→v3](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)** (SQL, 테스트) - `Migration_2_3.kt` (105줄), 3개 테이블 생성, 기본 레코드 삽입, 인덱스 2개
- [x] `FocusDistraction` 엔티티/DAO 설계 (허용/차단 이벤트 전체) → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[FocusDistraction 엔티티](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)** - 엔티티 48줄, DAO 136줄 (10개 메서드), sessionId nullable + SET_NULL, 인덱스 2개
- [x] `DetoxyRoutineLog` 엔티티/DAO 설계 (루틴 실행/실패 기록) → **[DetoxyRoutineLog 엔티티](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)**, **[detoxy_routine_completed](./01_advanced_analytics_schema.md#detoxy_routine_completed-신규)** - 엔티티 41줄, DAO 100줄 (8개 메서드), RoutineFailureReason enum
- [x] `FocusSettings` 엔티티/DAO 설계 (카테고리별 차단 설정 저장) → **[FocusSettings 엔티티](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)** - 엔티티 28줄 (Singleton), DAO 83줄 (11개 메서드), 프리셋 적용 메서드
- [x] UsageStats opt-in 사용자의 체류 시간 저장 구조 설계 → **[FocusDistraction dwellTimeSeconds](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)** - dwellTimeSeconds nullable 필드, 분산 회피율 계산 쿼리
- [x] 통합 테스트: 신규 테이블 CRUD 동작 확인 → **[마이그레이션 테스트](./01_advanced_room_migration_strategy.md#6-테스트-전략)** - compileDebugKotlin 성공, Lint 0 errors
- [x] DetoxyDatabase v3 업데이트 및 DatabaseModule MIGRATION_2_3 추가 → **작업 문서**: [2025-10-19_1st_advanced_2B.1.md](../working_history/2025-10-19_1st_advanced_2B.1.md)
- [x] 생성: 7개 파일 (~541줄), 수정: 2개 파일 (~66줄), 합계: ~607줄

#### 2B.2 고급 통계 계산 모듈 (Day 8-10) ✅
**작업 문서**: [2025-10-19_1st_advanced_2B.2.md](../working_history/2025-10-19_1st_advanced_2B.2.md)

- [x] **디톡시 위험 지수** 산식 정의 및 구현 (차단/허용 이벤트, 사용 시간 가중치) → [지표 확장](./01_advanced_prd.md#42-리포트-고도화), **[report_risk_index_calculated](./01_advanced_analytics_schema.md#report_risk_index_calculated)** (recovery/warning/high_risk)  
  ✅ `DetoxyRiskCalculator.kt` (4개 가중치: 실패율 40%, 연속실패 25%, 포기시점 20%, 차단빈도 15%)
- [x] **디톡시 회복률 추세** 계산 유틸 (7일, 30일 기준) → **[report_recovery_rate_calculated](./01_advanced_analytics_schema.md#report_recovery_rate_calculated-신규)** (주간 변화량)  
  ✅ `DetoxyRecoveryCalculator.kt` (일별/주간/월별 추세, IMPROVING/STABLE/DECLINING)
- [x] 집중률 성장세 계산 및 위험 지수 연동 → **[KPI](./01_advanced_analytics_schema.md#63-성과-지표-디톡시회복-관점)** (디톡시 성공률, 회복률 추세)  
  ✅ `FocusStatisticsCalculator.kt` 타입 수정 (Int→Long), `FocusStatisticsSummary` 파일레벨 이동
- [x] 유혹 저항 시간·포기 지점 분석 함수 → **[session_give_up 파라미터](./01_advanced_analytics_schema.md#session_give_up-확장)**  
  ✅ `FocusInterruptionAnalyzer.kt` (`ResistanceTimeAnalysis`, `GiveUpPointAnalysis`)
- [x] 방해요인 Top 3 집계 유틸 (`FocusInterruption` 기반) → **[FocusInterruptionDao](./01_advanced_room_migration_strategy.md#4-dao-업데이트-및-신규-메서드)**  
  ✅ `FocusInterruptionAnalyzer.kt` (카테고리/앱별 Top 3)
- [x] 분산 회피율·허용 앱 체류 시간 계산 (`FocusDistraction` 기반) → **[FocusDistractionDao](./01_advanced_room_migration_strategy.md#4-dao-업데이트-및-신규-메서드)**  
  ✅ `FocusInterruptionAnalyzer.kt` (준비 완료, Task 2B.3에서 UI 연동)
- [x] UsageStats 가중치 합산·보정 로직 → **[usage_stats_opt_in](./01_advanced_analytics_schema.md#usage_stats_opt_in-신규)**  
  ✅ `DetoxyAdvancedStatistics.kt` (종합 인사이트 통합)
- [x] 위험 지수 기반 코치 추천 매핑 (회복/주의/고위험) → **[코치 추천 메시지](./01_advanced_wireframe_spec.md#디톡시-코치-추천-카드)**, **[report_coach_recommendation_shown](./01_advanced_analytics_schema.md#report_coach_recommendation_shown)**  
  ✅ `DetoxyCoachRecommender.kt` (위험단계별 메시지 + 행동제안 3-5개)
- [x] 포인트/루틴 진행도 캐싱 전략 확정 → **[DetoxyRoutineLog](./01_advanced_room_migration_strategy.md#phase-2-v2--v3-week-2b)**  
  ✅ `DetoxyAdvancedStatistics.kt` (일일/주간/월별 인사이트)
- [x] 단위 테스트: 고급 통계 함수 검증  
  ✅ 빌드 성공 (5개 파일 생성, 1개 파일 수정, ~1,153줄)

#### 2B.3.1 ReportViewModel 리팩토링 & 데이터 연동 (Day 10) ✅
**목표**: 신규 고급 통계 계산기를 ReportViewModel에 통합하고 데이터 흐름 구성  
**작업 문서**: [2025-10-19_1st_advanced_2B.3.1.md](../working_history/2025-10-19_1st_advanced_2B.3.1.md)

- [x] ReportViewModel에 `DetoxyAdvancedStatistics` 의존성 주입 (Hilt) → [2B.2 작업 결과](../working_history/2025-10-19_1st_advanced_2B.2.md)  
  ✅ Hilt를 통한 의존성 주입 완료
- [x] `ReportUiState` 생성 및 신규 통계 필드 추가:  
  ✅ `riskIndex`, `recoveryTrend`, `topDistractions`, `resistanceAnalysis`, `giveUpAnalysis`, `coachRecommendation`, `hasData`
- [x] `loadReportData()` 메서드 확장: 7일 세션 데이터 로드 및 고급 통계 계산 → **[DetoxyAdvancedStatistics.calculateComprehensiveInsights()](../working_history/2025-10-19_1st_advanced_2B.2.md#5-detoxyadvancedstatistics-종합-통계-관리자)**  
  ✅ `loadAdvancedStatistics()` 구현, `getSessionsInRange()` 및 `getInterruptionsInLastDays()` 추가
- [x] 빈 상태 처리 로직 구현: 세션 데이터 없을 때 기본값 표시 → **[빈 상태 처리](./01_advanced_wireframe_spec.md#3-빈-상태-empty-state)**  
  ✅ `hasData` 플래그 기반 빈 상태 처리
- [x] 데이터 로딩 상태 관리 (`Loading`, `Success`, `Error`) → [기존 ReportViewModel 패턴](../working_history/2025-10-11_3.3.md)  
  ✅ `isLoading`, `error` 필드를 통한 상태 관리
- [x] 하위 호환성 유지: 기존 ReportScreen API 유지  
  ✅ 기존 `todaySessions`, `settings`, `isLoading` StateFlow 및 통계 메서드 유지 (Task 2B.3.2에서 제거 예정)

#### 2B.3.2 일간/주간 카드 UI 구현 (Day 11) ✅
**목표**: 핵심 리포트 카드 UI 구현 (위험 지수, 회복률, 방해요인)  
**작업 문서**: [2025-10-19_1st_advanced_2B.3.2.md](../working_history/2025-10-19_1st_advanced_2B.3.2.md)

- [x] **위험 지수 카드** Composable 구현:  
  ✅ `DetoxyRiskCard(riskIndex: DetoxyRiskIndex)` 생성  
  ✅ 위험 단계별 색상 표시 (RECOVERY/WARNING/HIGH_RISK) → **[위험 지수 카드](./01_advanced_wireframe_spec.md#디톡시-위험-지수-카드)**  
  ✅ 스코어(0-100) 프로그레스 바 + 텍스트 설명 + 세부 지표 3가지
- [x] **회복률 추세 카드** Composable 구현:  
  ✅ `RecoveryTrendCard(recoveryTrend: DetoxyRecoveryTrend)` 생성  
  ✅ 주간 변화량 표시 (↗️/→/↘️) → **[회복률 카드](./01_advanced_wireframe_spec.md#회복률-추세-카드)**  
  ✅ 일별 회복률 간단한 라인 차트 (Compose Canvas)
- [x] **주간 인사이트 그래프** Compose Canvas 구현:  
  ✅ 회복률 추세 그래프 (7일) - RecoveryTrendCard 내장 → **[주간 그래프](./01_advanced_wireframe_spec.md#주간-그래프-compose-canvas-기반)**
- [x] **방해요인 Top 3 카드** Composable 구현:  
  ✅ `DistractionTopCard(distractions: List<DistractionItem>)` 생성  
  ✅ 카테고리별 차단 횟수 + 비율(%) 표시 → **[방해요인 Top 3](./01_advanced_wireframe_spec.md#방해요인-top-3-카드)**  
  ✅ 순위별 색상 구분 및 프로그레스 바
- [x] ReportScreen에 신규 카드 통합 (기존 일간/주간 섹션에 추가) → [UI 요구사항](./01_advanced_prd.md#42-리포트-고도화)  
  ✅ 새로운 UiState 사용, 주간 인사이트 섹션 추가, 3개 신규 카드 배치
- [x] 빈 데이터 상태 UI 처리: "아직 집중 세션이 없어요" 메시지 표시  
  ✅ EmptyStateCard 구현, hasData 플래그 기반 분기

#### 2B.3.3 고급 카드 & 최종 통합 (Day 12)
**목표**: 분산 회피율, 코치 추천 UI 구현 및 전체 통합

- [ ] **분산 회피율 카드** Composable 구현:
  - `DistractionAvoidanceCard(giveUpAnalysis: GiveUpPointAnalysis)` 생성
  - 5초 이내 이탈 비율 표시 → **[Wireframe §2](./01_advanced_wireframe_spec.md#2-리포트-고도화-화면)**
  - 초반/중반/후반 포기 비율 도넛 차트
- [ ] **허용 앱 체류 시간 카드** Composable 구현 (추후 UsageStats 연동 준비):
  - `AllowedAppDwellCard()` 생성
  - 현재는 "데이터 수집 중" 상태 표시
  - UsageStats opt-in 유도 버튼 → **[usage_stats_opt_in](./01_advanced_analytics_schema.md#usage_stats_opt_in-신규)**
- [x] **디톡시 코치 추천 카드** Composable 구현 (⚠️ RiskLevel enum 참조 오류로 TODO 처리):
  - `CoachRecommendationCard(recommendation: CoachRecommendation)` 생성
  - 위험 단계별 메시지 + 행동 제안 리스트 → **[코치 추천 카드](./01_advanced_wireframe_spec.md#디톡시-코치-추천-카드)**
  - 우선순위별 ActionItem 표시 (아이콘 + 제목 + 설명)
- [x] **코치 추천 상세 다이얼로그** 구현 (⚠️ RiskLevel enum 참조 오류로 TODO 처리):
  - `CoachRecommendationDialog()` 생성
  - 행동 제안 상세 내용 + "실천하기" 버튼 → **[추천 메시지](./01_advanced_wireframe_spec.md#코치-추천-메시지-예시)**
- [x] ReportScreen 최종 레이아웃 조정:
  - 카드 순서: 일간 요약 → 위험 지수 → 회복률 → 방해요인 (코치 추천은 RiskLevel 오류로 주석 처리)
  - 스크롤 성능 최적화 (LazyColumn)
- [x] 전체 빈 상태 UI 완성: 데이터 부족 시 가이드 메시지 + 기능 리스트 → **[빈 상태 처리](./01_advanced_wireframe_spec.md#3-빈-상태-empty-state)**
- [x] Analytics 이벤트 연동 (TODO 표시 - 추후 AnalyticsHelper 메서드 구현 필요):
  - `report_risk_index_calculated` → **[report_risk_index_calculated](./01_advanced_analytics_schema.md#report_risk_index_calculated)**
  - `report_recovery_rate_calculated` → **[report_recovery_rate_calculated](./01_advanced_analytics_schema.md#report_recovery_rate_calculated-신규)**
  - `report_coach_recommendation_shown` → **[report_coach_recommendation_shown](./01_advanced_analytics_schema.md#report_coach_recommendation_shown)**
- [x] UI 테스트: 빌드 검증 완료 (코치 추천 카드는 향후 수정 필요) → **[작업 내역](../working_history/2025-10-19_1st_advanced_2B.3.3.md)**

#### 2B.3.4 코치 추천 카드 완성 (RiskLevel 문제 해결)
**목표**: RiskLevel enum 참조 오류 해결 및 코치 추천 카드/다이얼로그 완성  
**우선순위**: High (2B.3.3에서 미완성)

**RiskLevel 문제 분석**:
- Kotlin 컴파일러가 `DetoxyRiskCalculator.RiskLevel` nested enum을 함수 파라미터 타입으로 인식하지 못하는 문제
- 시도한 해결 방법 (모두 실패):
  1. Full qualified name: `DetoxyRiskCalculator.RiskLevel`
  2. typealias: `private typealias RiskLevel = DetoxyRiskCalculator.RiskLevel`
  3. Import: `import com.allday.detoxy.domain.manager.DetoxyRiskCalculator.RiskLevel`

**해결 방안 (우선순위순)**:
- [ ] **방안 1**: RiskLevel을 별도 파일로 분리 (top-level enum class)
  - `domain/manager/RiskLevel.kt` 생성
  - `DetoxyRiskCalculator`에서 RiskLevel 참조하도록 수정
  - 영향 범위: `DetoxyRiskCalculator.kt`, `DetoxyRiskIndex.kt`, `CoachRecommendation.kt`
- [ ] **방안 2**: 색상 매핑 로직을 data class 내부로 이동
  - `CoachRecommendation`에 `getColor()`, `getBgColor()` 메서드 추가
  - Composable에서 직접 타입 참조 회피
- [ ] **방안 3**: Compose Preview/Wrapper 함수 활용
  - `@Composable` 함수에서 RiskLevel을 직접 받지 않고 String으로 변환하여 전달
  - 내부에서 enum 변환 로직 구현

**구현 작업**:
- [ ] RiskLevel enum 분리 (방안 1 선택 시) 또는 대안 구현
- [ ] `CoachRecommendationCard.kt` 재구현:
  - 위험 단계별 색상 표시 (RECOVERY: 초록, WARNING: 주황, HIGH_RISK: 빨강)
  - 행동 제안 Top 3 표시 (아이콘 + 제목 + 설명)
  - "전체 보기" 버튼 → 다이얼로그 호출
- [ ] `CoachRecommendationDialog.kt` 재구현:
  - 위험 단계 배지 표시
  - 전체 행동 제안 목록 (스크롤 가능)
  - 우선순위별 색상 구분
  - "확인" 버튼
- [ ] `ReportScreen.kt` 주석 제거 및 카드 통합:
  - 코치 추천 카드 활성화 (주석 해제)
  - 다이얼로그 연동
  - 최종 카드 순서: 일간 요약 → 위험 지수 → 회복률 → 방해요인 → 코치 추천
- [ ] 빌드 검증: `./gradlew compileDebugKotlin`, Lint 확인
- [ ] UI 테스트: 각 위험 단계별 코치 추천 카드 시각적 검증

#### 2B.4 통합 테스트 & 품질 (Day 12-13)
- [ ] QA 시나리오: 성공/포기/실패 세션 5개 생성 후 리포트 검증 → **[QA 시나리오 5-8](./01_advanced_qa_devices.md#43-리포트-고도화-테스트)** (위험 지수, 회복률, 방해요인, 코치 추천)
- [ ] UsageStats opt-in/off 플로우 테스트 및 지표 표기 차이 검증 → **[QA 시나리오 10-11](./01_advanced_qa_devices.md#44-권한-플로우-테스트)** (UsageStats 권한)
- [ ] 디톡시 위험 지수 단계별 분포 QA → **[위험 지수 검증](./01_advanced_qa_devices.md#시나리오-5-디톡시-위험-지수-중독-진단)** (recovery/warning/high_risk)
- [ ] 디톡시 루틴 실행/실패 로그 QA → **[QA 시나리오 12-14](./01_advanced_qa_devices.md#45-디톡시-루틴-테스트-신규)** (스케줄, 자동 실행, 건너뛰기)
- [ ] 회귀 테스트: 기존 리포트 기능 정상 동작 확인 → [기존 리포트 구현](../working_history/2025-10-11_3.3.md), **[회귀 체크리스트](./01_advanced_qa_devices.md#61-mvp-기능-회귀-방지)**

---

## 4. 통합 QA 및 배포 준비 (Week 3)
- [ ] 신규 이벤트 로깅이 Analytics 콘솔/디버그 로그에서 확인되는지 검증 → [데이터 및 트래킹](./01_advanced_prd.md#5-데이터-및-트래킹), **[Analytics 전체 이벤트](./01_advanced_analytics_schema.md#3-이벤트-상세-정의)** (19개 이벤트), **[Funnel 분석](./01_advanced_analytics_schema.md#5-사용자-플로우-funnel-분석)**
- [ ] `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint` 실행 및 결과 기록 → [비기능 요구사항](./01_advanced_prd.md#6-비기능-요구사항), **[QA 기기](./01_advanced_qa_devices.md#1-필수-테스트-기기-최소-2종)** (Pixel 7, Galaxy S23)
- [ ] 성능 점검: 리포트 첫 로딩 시간 측정, 필요 시 캐싱 튜닝 → [비기능 요구사항](./01_advanced_prd.md#6-비기능-요구사항), [서비스 최적화 기록](../working_history/2025-10-12_4.3.md), **[성능 테스트](./01_advanced_qa_devices.md#5-성능-테스트)** (메모리, 배터리, APK 크기)
- [ ] 전체 QA 시나리오 실행 및 체크리스트 완료 → **[테스트 시나리오 14개](./01_advanced_qa_devices.md#4-테스트-시나리오)** (설정, 마이그레이션, 리포트, 권한, 루틴)
- [ ] 사용자 가이드/README/앱 내 도움말 텍스트 업데이트 → [완료 기준](./01_advanced_prd.md#8-완료-기준dod)
- [ ] 플레이스토어 릴리스 노트 초안 작성(디톡시 제어 설정, 위험 지수 리포트 강조) → [배경 및 목표](./01_advanced_prd.md#1-배경-및-목표)
- [ ] 내부 베타(5인) 배포 및 피드백 수집 계획 수립 → **[베타 테스트](./01_advanced_qa_devices.md#7-베타-테스트-및-릴리스-계획)** (내부 5인, 외부 20인)
- [ ] UsageStats 권한 안내/프라이버시 FAQ 업데이트 및 비허용 시 UI 경고 문구 검증 → [위험 요소 및 대응](./01_advanced_prd.md#7-위험-요소-및-대응), **[프라이버시 준수](./01_advanced_analytics_schema.md#12-개인정보-보호)**

---

## 5. 산출물 체크리스트
- [ ] `docs/01_advanced_prd.md` 최신화 여부 확인
- [ ] 디톡시 제어 설정 화면 UI 캡처 및 설명 문서 → [UI 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), **[Wireframe 전체 스펙](./01_advanced_wireframe_spec.md)**
- [ ] Room 마이그레이션 스크립트 및 테스트 보고 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화), **[마이그레이션 전략](./01_advanced_room_migration_strategy.md)** (v1→v2→v3)
- [ ] 리포트 고도화 결과 스크린샷/그래프 → [UI 요구사항](./01_advanced_prd.md#42-리포트-고도화), [기존 리포트 구현](../working_history/2025-10-11_3.3.2.md), **[리포트 UI 스펙](./01_advanced_wireframe_spec.md#2-리포트-고도화-화면)**
- [ ] 디톡시 코치 추천 메시지/카피 가이드 문서 → [지표 확장](./01_advanced_prd.md#42-리포트-고도화), **[코치 추천 메시지](./01_advanced_wireframe_spec.md#추천-메시지-예시-위험-지수-기반-중독회복-관점)**
- [ ] Analytics 이벤트 로깅 검증 보고서 → **[Analytics 스키마](./01_advanced_analytics_schema.md)** (19개 이벤트), **[KPI 리포트](./01_advanced_analytics_schema.md#6-핵심-지표-kpi)**
- [ ] QA 테스트 결과 보고서 → **[QA 시나리오](./01_advanced_qa_devices.md)** (14개 시나리오), **[회귀 테스트](./01_advanced_qa_devices.md#6-회귀-테스트-체크리스트)**
- [ ] `working_history/YYYY-MM-DD_x.y.md` 기록 + 커밋 ID
- [ ] 고도화 기능 릴리스 노트 초안
