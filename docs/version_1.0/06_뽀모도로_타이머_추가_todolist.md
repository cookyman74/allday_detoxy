# 뽀모도로 타이머 멀티 스타일 추가 작업 계획서

> **관련 문서**: [06_뽀모도로_타이머_추가.md](./06_뽀모도로_타이머_추가.md)  
> **작성일**: 2025-12-12  
> **예상 총 소요**: 3일  
> **상태**: 작업 대기

---

## 📋 작업 규칙

### 사전작업 규칙
- 이전 단계의 작업결과서(`working_history/`)를 반드시 확인
- 관련 코드의 현재 상태를 grep/read_file로 확인 (`TimerScreen.kt`, `TimerViewModel.kt`)
- 의존성 있는 파일들의 변경사항 체크

### 작업 규칙
- 문서의 설계 내용을 기반으로 구현
- 한 번에 하나의 스타일/기능씩 수정
- 수정 후 즉시 컴파일 확인 (`./gradlew compileDebugKotlin`)
- 복잡한 Canvas 드로잉 로직은 미리 계산식 검증

### 작업후처리 규칙
- 작업 완료 후 `working_history/version_2.0/{단계번호}_{작업타이틀}_{날짜}.md` 파일 작성
- [working_history_template.md](../working_history/version_2.0/working_history_template.md) 형식 준수

---

## 🔧 단계별 작업 계획

---

### 📌 단계 1: Pager 도입 및 기존 UI 리팩토링

**예상 소요**: 0.5일

#### 사전작업
- [ ] `TimerScreen.kt` 코드 구조 분석
- [ ] `HorizontalPager` 라이브러리 의존성 확인 (Compose Foundation)
- [ ] 현재 `TimerCircle` 컴포넌트 독립성 확인

#### 작업
- [x] **1.1** `TimerStyleLiquidRing.kt` 컴포넌트 분리
  - 기존 `TimerCircle` 코드를 별도 파일/컴포저블로 분리 (Type A)
  - `progress`, `remainingTimeText` 등을 파라미터로 받도록 수정
  
- [x] **1.2** `TimerScreen`에 `HorizontalPager` 적용
  - `pagerState = rememberPagerState(pageCount = { 3 })`
  - 기존 타이머 영역을 Pager로 감싸기
  - 임시로 3개 페이지 모두 Type A를 띄워서 스와이프 동작 확인

- [x] **1.3** Page Indicator 추가
  - 하단에 심플한 Dot Indicator 추가
  - 현재 페이지에 따라 활성/비활성 스타일 적용 (Liquid Glass 스타일)

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/01_TimerPager도입_2025-12-12.md`

---

### 📌 단계 2: Type B (Minimal Flux) 구현

**예상 소요**: 1일

#### 사전작업
- [ ] `TimerStyleLiquidRing` 분리 상태 확인
- [ ] Canvas Gradient/Blur 효과 구현 방법 조사

#### 작업
- [x] **2.1** `TimerStyleMinimalFlux.kt` 생성
  - 배경에 부드러운 호흡(Breathing) 애니메이션 적용 (`infiniteTransition`)
  - 중앙 Orb(구체) 그리기 (RadialGradient 활용)
  
- [x] **2.2** 엠비언트 효과 구현
  - 시간이 흐름(progress)에 따라 Orb의 색상 변화 (Blue -> Purple -> Red)
  - 숫자는 매우 작게 표시하거나, 탭 시에만 표시되도록 토글 처리

- [x] **2.3** Pager 연동
  - 2번째 페이지(Index 1)에 `TimerStyleMinimalFlux` 배치

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/02_TimerStyleMinimalFlux구현_2025-12-12.md`

---

### 📌 단계 3: Type C (Glass Sector) 구현

**예상 소요**: 1일

#### 사전작업
- [ ] Canvas `drawArc` API 확인
- [ ] 타임타이머(Visual Timer) 시각 효과 레퍼런스 참고

#### 작업
- [x] **3.1** `TimerStyleGlassSector.kt` 생성
  - 배경 디스크(시계 눈금) 그리기
  - 남은 시간을 나타내는 부채꼴(Sector) 그리기 (`drawArc`)
  
- [x] **3.2** Glassmorphism 적용
  - 부채꼴 영역에 반투명 색상 및 외곽선(Stroke) 적용하여 유리 조각처럼 보이게 처리
  - 눈금과 숫자의 가독성 확보

- [x] **3.3** Pager 연동
  - 3번째 페이지(Index 2)에 `TimerStyleGlassSector` 배치

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/03_TimerStyleGlassSector구현_2025-12-13.md`

---

### 📌 단계 4: 상태 저장 및 마무리 (Persistence & Polish)

**예상 소요**: 0.5일

#### 사전작업
- [ ] `UserPreferences` 또는 `DataStore` 구조 확인

#### 작업
- [x] **4.1** 마지막 선택 스타일 저장 로직 추가
  - `TimerViewModel`에 `selectedTimerStyleIndex` 상태 추가
  - Pager 스크롤 시 `LaunchedEffect`로 인덱스 변경 감지 및 저장
  - 앱 시작 시 저장된 인덱스로 `pagerState` 초기화

- [x] **4.2** 전체 UI 폴리싱
  - 스와이프 트랜지션 부드럽게 조정
  - 각 스타일별 텍스트 색상 및 크기 최적화 (가독성 검수)

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/04_TimerUI마무리_2025-12-12.md`

---

## 📊 진행 상황 추적

| 단계 | 작업명 | 상태 | 시작일 | 완료일 | 작업결과서 |
|---|---|---|---|---|---|
| 1 | Pager 도입 및 UI 리팩토링 | ✅ 완료 | 2025-12-12 | 2025-12-12 | [01_TimerPager도입_2025-12-12.md](../working_history/version_2.0/01_TimerPager도입_2025-12-12.md) |
| 2 | Type B (Minimal Flux) 구현 | ✅ 완료 | 2025-12-12 | 2025-12-12 | [02_TimerStyleMinimalFlux구현_2025-12-12.md](../working_history/version_2.0/02_TimerStyleMinimalFlux구현_2025-12-12.md) |
| 3 | Type C (Glass Sector) 구현 | ✅ 완료 | 2025-12-12 | 2025-12-12 | [03_TimerStyleGlassSector구현_2025-12-12.md](../working_history/version_2.0/03_TimerStyleGlassSector구현_2025-12-12.md) |
| 4 | 상태 저장 및 마무리 | ✅ 완료 | 2025-12-12 | 2025-12-12 | [04_TimerUI마무리_2025-12-12.md](../working_history/version_2.0/04_TimerUI마무리_2025-12-12.md) |

**상태 범례**: ⬜ 대기 | 🔄 진행중 | ✅ 완료 | ⏸️ 보류

---

**v2.0 뽀모도로 멀티 스타일 타이머 구현 완료** ✅
