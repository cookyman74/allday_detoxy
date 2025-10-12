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
- [ ] `core/utils`에 카테고리 enum 및 패키지 매핑 정의 → [카테고리 목록](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] `FocusAccessibilityService`와 `LockOverlayService`에서 동적 차단 목록을 참조하도록 인터페이스 정리 → [동작 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [차단 로직](../working_history/2025-10-05_1.3.md)
- [ ] DndManager는 세션 시작/종료 시 글로벌 DND 토글만 수행하고, 권한 상태 노출/안내 문구를 보강 → [DND 안내](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [DND 제어 작업](../working_history/2025-10-06_2.2.md)
- [ ] 집중모드 중 모니터링할 패키지/카테고리 범위를 정의하고, 허용 앱 이벤트 로깅 정책을 수립 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)

### 2.2 UI/UX 구현
- [ ] `presentation/ui/settings/focus/FocusModeSettingsScreen` Compose 레이아웃 구현 → [UI 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [네비게이션 구조](../working_history/2025-10-11_3.3.md)
- [ ] 프리셋(전체/집중/완화) 데이터 모델 및 Preview UI 작성 → [레이아웃 섹션](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] 카테고리별 토글/슬라이더 상태를 `FocusSettingsViewModel`에서 StateFlow로 관리 → [동작 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] 권한 상태 카드(DND, 접근성) 연결 및 재요청 Intent 처리 → [권한 경고](./01_advanced_prd.md#41-디톡시-제어-설정-화면), [권한 온보딩](../working_history/2025-10-12_4.1.md)
- [ ] 메신저 카테고리 초기 안내 배너/다이얼로그 구현 및 사용자 선택 흐름 정의 → [카테고리 구분 원칙](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] 디톡시 루틴(예: 야간 전체 차단) 토글 및 설명 UI 설계 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)

### 2.3 상태 저장 및 로직 연동
- [ ] `FocusSettingsRepository` 설계(Room + DataStore 조합 검토) → [데이터 보강](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] 설정 저장 후 세션 시작 시 적용되는지 통합 테스트 → [사용자 시나리오 1~2](./01_advanced_prd.md#3-주요-사용자-시나리오)
- [ ] `TimerViewModel.startTimer`에서 최신 설정을 불러와 AccessibilityService/DndManager에 전달하도록 수정 → [동작 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] 권한 미보유 시 동작 방어 로직 및 사용자 안내 구현 → [완료 기준](./01_advanced_prd.md#8-완료-기준dod), [Week1 권한 처리](../working_history/2025-10-05_1.2.md)
- [ ] 메신저 카테고리 사용자 선택 상태를 저장/복원하고 프리셋 전환 시 충돌 로직 정의 → [카테고리 구분 원칙](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] UsageStats opt-in UI(권한 설명/동의/건너뛰기)를 설계하고 세션과의 연동 정책 정의 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 디톡시 루틴 스케줄/알림 설정 저장 구조 설계(`DetoxyRoutineLog` 초기 스펙) → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 단위 테스트: ViewModel 상태 변환, Repository 기본 CRUD

### 2.4 QA & 문서화
- [ ] UI 스냅샷 캡처 및 동작 체크리스트 작성
- [ ] `docs/01_advanced_prd.md` 업데이트 사항 반영 확인
- [ ] `working_history` 로그 생성(빌드/테스트 결과 포함)

---

## 3. 리포트 고도화 (Week 2A-2B, 11-13일)

> **⚠️ 중요**: Week 2는 작업량이 많아 2주로 분할합니다.
> - **Week 2A (5-6일)**: 데이터 기반 구축
> - **Week 2B (6-7일)**: 고급 통계 및 UI

---

### Week 2A: 데이터 기반 구축 (Day 1-6)

#### 2A.1 데이터 모델 확장 (Day 1-3)
- [ ] Room 마이그레이션 v2→v3 작성 및 단위 테스트 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)
- [ ] `FocusSession`에 `interruptedSeconds`, `primaryDistractionCategory`, `giveUpReason` 필드 추가
- [ ] `FocusInterruption` 엔티티/DAO/Repository 설계 및 구현 (세션 중 차단 이벤트 로그)
- [ ] 기존 데이터 백필 로직 (기존 세션 `interruptedSeconds = 0`) 적용
- [ ] 마이그레이션 통합 테스트: DB 버전 업그레이드 정상 동작 확인

#### 2A.2 세션 종료 로직 개선 (Day 3-4)
- [ ] 중도 포기 시 경과 시간(`elapsedSeconds`) 기록 로직 추가 → [정확성 요구사항](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 차단/허용 이벤트 로그 → 세션과 연계 저장 구현 (카테고리 누락 방지) → [세션 기록 작업](../working_history/2025-10-06_2.3.md)
- [ ] `TimerViewModel.giveUp()` 수정: 경과 시간 계산 및 저장
- [ ] 통합 테스트: 세션 시작→중도 포기→DB 저장→리포트 반영 플로우 검증

#### 2A.3 기본 통계 계산 모듈 (Day 5-6)
- [ ] 총 집중 시간 계산 유틸 (성공+실패 세션 포함) → [지표 확장](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 집중률 계산 함수 (성공 세션 / 전체 세션)
- [ ] 평균 집중 유지 시간 계산 (성공/실패 분리)
- [ ] 포인트 누적 추세 계산 (7일/30일 기준)
- [ ] 단위 테스트: 각 통계 함수 정확성 검증

---

### Week 2B: 고급 통계 및 UI (Day 7-13)

#### 2B.1 고급 데이터 모델 (Day 7-8)
- [ ] `FocusDistraction` 엔티티/DAO/Repository 설계 (허용/차단 이벤트 전체) → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)
- [ ] `DetoxyRoutineLog` 엔티티/DAO 설계 (루틴 실행/실패 기록)
- [ ] UsageStats opt-in 사용자의 체류 시간 저장 구조 설계
- [ ] 통합 테스트: 신규 테이블 CRUD 동작 확인

#### 2B.2 고급 통계 계산 모듈 (Day 8-10)
- [ ] **Detoxy 위험 지수** 산식 정의 및 구현 (차단/허용 이벤트, 사용 시간 가중치) → [지표 확장](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 회복률 추세 계산 유틸 (7일, 30일 기준)
- [ ] 집중률 성장세 계산 및 위험 지수 연동
- [ ] 유혹 저항 시간·포기 지점 분석 함수
- [ ] 방해요인 Top 3 집계 유틸 (`FocusInterruption` 기반)
- [ ] 분산 회피율·허용 앱 체류 시간 계산 (`FocusDistraction` 기반)
- [ ] UsageStats 가중치 합산·보정 로직
- [ ] 위험 지수 기반 코치 추천 매핑 (고위험/주의/안정)
- [ ] 포인트/루틴 진행도 캐싱 전략 확정
- [ ] 단위 테스트: 고급 통계 함수 검증

#### 2B.3 UI 업데이트 (Day 10-12)
- [ ] 일간 카드: Detoxy 위험 지수, 회복률/집중률 변화 추가 → [UI 요구사항](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 주간 인사이트 그래프 2종 Compose 구현 (회복률/총 회복 시간)
- [ ] 방해요인 Top 3 카드 + Tooltip
- [ ] 분산 회피율 & 허용 앱 카드 시각화
- [ ] Detoxy 코치 추천 카드/다이얼로그 (위험 단계별 메시지)
- [ ] 빈 상태/데이터 부족 안내 문구 처리

#### 2B.4 통합 테스트 & 품질 (Day 12-13)
- [ ] QA 시나리오: 성공/포기/실패 세션 5개 생성 후 리포트 검증
- [ ] UsageStats opt-in/off 플로우 테스트 및 지표 표기 차이 검증
- [ ] Detoxy 위험 지수 단계별 분포 QA
- [ ] 디톡시 루틴 실행/실패 로그 QA
- [ ] 회귀 테스트: 기존 리포트 기능 정상 동작 확인 → [기존 리포트 구현](../working_history/2025-10-11_3.3.md)

---

## 4. 통합 QA 및 배포 준비 (Week 3)
- [ ] 신규 이벤트 로깅이 Analytics 콘솔/디버그 로그에서 확인되는지 검증 → [데이터 및 트래킹](./01_advanced_prd.md#5-데이터-및-트래킹)
- [ ] `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint` 실행 및 결과 기록 → [비기능 요구사항](./01_advanced_prd.md#6-비기능-요구사항)
- [ ] 성능 점검: 리포트 첫 로딩 시간 측정, 필요 시 캐싱 튜닝 → [비기능 요구사항](./01_advanced_prd.md#6-비기능-요구사항), [서비스 최적화 기록](../working_history/2025-10-12_4.3.md)
- [ ] 사용자 가이드/README/앱 내 도움말 텍스트 업데이트 → [완료 기준](./01_advanced_prd.md#8-완료-기준dod)
- [ ] 플레이스토어 릴리스 노트 초안 작성(디톡시 제어 설정, 위험 지수 리포트 강조) → [배경 및 목표](./01_advanced_prd.md#1-배경-및-목표)
- [ ] 내부 베타(5인) 배포 및 피드백 수집 계획 수립
- [ ] UsageStats 권한 안내/프라이버시 FAQ 업데이트 및 비허용 시 UI 경고 문구 검증 → [위험 요소 및 대응](./01_advanced_prd.md#7-위험-요소-및-대응)

---

## 5. 산출물 체크리스트
- [ ] `docs/01_advanced_prd.md` 최신화 여부 확인
- [ ] 디톡시 제어 설정 화면 UI 캡처 및 설명 문서 → [UI 요구사항](./01_advanced_prd.md#41-디톡시-제어-설정-화면)
- [ ] Room 마이그레이션 스크립트 및 테스트 보고 → [데이터 보강](./01_advanced_prd.md#42-리포트-고도화)
- [ ] 리포트 고도화 결과 스크린샷/그래프 → [UI 요구사항](./01_advanced_prd.md#42-리포트-고도화), [기존 리포트 구현](../working_history/2025-10-11_3.3.2.md)
- [ ] Detoxy 코치 추천 메시지/카피 가이드 문서 → [지표 확장](./01_advanced_prd.md#42-리포트-고도화)
- [ ] `working_history/YYYY-MM-DD_x.y.md` 기록 + 커밋 ID
- [ ] 고도화 기능 릴리스 노트 초안
