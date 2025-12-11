# 리퀴드 글래스 (Liquid Glass) 디자인 적용 작업 계획서

> **관련 PRD**: [05_리퀴드글래스_prd.md](./05_리퀴드글래스_prd.md)  
> **작성일**: 2025-12-11  
> **예상 총 소요**: 5일  
> **상태**: 작업 대기

---

## 📋 작업 규칙

### 사전작업 규칙
- 이전 단계의 작업결과서(`working_history/`)를 반드시 확인
- 관련 코드의 현재 상태를 grep/read_file로 확인
- 의존성 있는 파일들의 변경사항 체크

### 작업 규칙
- PRD의 설계 내용을 기반으로 구현
- 한 번에 하나의 컴포넌트/화면씩 수정
- 수정 후 즉시 Preview 및 에뮬레이터 확인
- 디자인 변경은 주관적이므로 지속적으로 전후 비교 스크린샷 기록

### 작업후처리 규칙
- 작업 완료 후 `working_history/version_2.0/{단계번호}_{작업타이틀}_{날짜}.md` 파일 작성
- [working_history_template.md](../working_history/version_2.0/working_history_template.md) 형식 준수
- 다음 작업을 위한 주의사항/교훈 반드시 기록

---

## 🔧 단계별 작업 계획

---

### 📌 단계 1: 프로젝트 설정 및 라이브러리 추가

**예상 소요**: 0.5일

#### 사전작업
- [x] `libs.versions.toml` 파일 확인
- [x] Haze 라이브러리 최신 버전 확인

#### 작업
- [x] **1.1** Haze 라이브러리 의존성 추가
  - `libs.versions.toml`: `haze = { module = "dev.chrisbanes.haze:haze-jetpack-compose", version = "..." }`
  - `app/build.gradle.kts`: `implementation(libs.haze)`
- [x] **1.2** Mesh Gradient 리소스 추가
  - `res/drawable`에 그라데이션 이미지 자산 추가 (또는 코드 기반 Brush 정의)
- [x] **1.3** Color Palette 확장
  - `ui/theme/Color.kt`: Glass 효과용 투명도 색상 정의

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/01_Glass라이브러리설정_2025-12-11.md`
- [ ] 기록할 내용:
  - 추가된 라이브러리 버전
  - 정의된 컬러/리소스 목록

---

### 📌 단계 2: 기반 컴포넌트 중 (Glass Primitives) 구현

**예상 소요**: 1일

#### 사전작업
- [ ] `Haze` 라이브러리 샘플 코드 분석
- [ ] 현재 `Modifier` 확장 함수 구조 확인

#### 작업
- [x] **2.1** `Modifier.liquidGlass()` 확장 함수 구현
  - 공통 Blur, Alpha, Border, Shadow, Clip 속성을 캡슐화
- [x] **2.2** `GlassSurface` Composable 구현
  - Material3 Surface를 대체하거나 래핑하여 Glass 스타일 적용
- [x] **2.3** `GlassScaffold` 구현
  - 배경(Mesh Gradient)과 컨텐츠(Glass)가 조화되도록 레이아웃 구성

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/02_Glass기반컴포넌트구현_2025-12-11.md`
- [x] 기록할 내용:
  - 구현된 Modifier/Composable 사용법
  - Preview 스크린샷

---

### 📌 단계 3: 메인 네비게이션 (Bottom Bar) 개편

**예상 소요**: 1일

#### 사전작업
- [ ] 현재 `ScreenSenseBottomNavigation` 구현 확인
- [ ] `Scaffold`의 bottomBar 처리 로직 확인

#### 작업
- [x] **3.1** 네비게이션 구조 변경
  - 기존: Scaffold bottomBar (고정형)
  - 변경: Box 내부의 Floating Bottom Bar (플로팅형)
- [x] **3.2** `GlassBottomNavigation` 구현
  - 둥근 모서리, 강한 Blur, 투명 배경 적용
  - 아이템 선택 효과 (Selection Indicator) 디자인 변경
- [x] **3.3** 메인 화면 레이아웃 조정
  - 네비게이션 바가 컨텐츠 위에 떠 있도록 여백(Padding) 조정

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/03_Glass네비게이션구현_2025-12-11.md`
- [x] 기록할 내용:
  - 변경 전후 비교 스크린샷
  - 플로팅 바 구현 시 유의점 (Window Insets 등)

---

### 📌 단계 4: 주요 화면 및 카드 적용 (Timer/DashBoard)

**예상 소요**: 1.5일

#### 사전작업
- [ ] `TimerScreen`, `ScheduleGroupScreen` 구조 확인
- [ ] 카드 컴포넌트들의 데이터 의존성 확인

#### 작업
- [x] **4.1** `TimerScreen` 배경 교체
  - 단색 배경 → Mesh Gradient
- [x] **4.2** 대시보드 카드 (`TimerCard` 등) Glass 적용
  - `liquidGlass` modifier 적용
  - 텍스트 가독성을 위한 명도 조절
- [x] **4.3** `ScheduleGroupScreen` 리스트 아이템 Glass 적용
  - 리스트 스크롤 시 성능 확인 (렉이 걸리지 않는지)

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/04_주요화면Glass적용_2025-12-11.md`
- [x] 기록할 내용:
  - 주요 화면 스크린샷
  - 리스트 렌더링 성능 이슈 유무

---

### 📌 단계 5: 다이얼로그 및 디테일 폴리싱

**예상 소요**: 0.5일

#### 사전작업
- [ ] 현재 사용 중인 Dialog 컴포넌트 확인

#### 작업
- [ ] **5.1** `GlassDialog` 구현
  - 배경 Dim 처리 외에 Backdrop Blur 추가
  - 다이얼로그 컨테이너 자체도 Glass 스타일 적용
- [ ] **5.2** 전체적인 Border 및 Shadow 튜닝
  - 깊이감이 느껴지도록 미세 조정 (1px Border 등)
- [ ] **5.3** 애니메이션 점검
  - 화면 전환, 다이얼로그 등장 시 부자연스러운 부분 수정

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/05_Glass마무리및다이얼로그_2025-12-11.md`
- [ ] 기록할 내용:
  - 최종 UI 스크린샷 갤러리

---

### 📌 단계 6: 성능 최적화 및 테스트

**예상 소요**: 0.5일

#### 사전작업
- [ ] 저사양 에뮬레이터 또는 디바이스 준비

#### 작업
- [ ] **6.1** GPU Overdraw 및 프레임 레이트 확인
  - 리스트 스크롤 시 끊김 확인
- [ ] **6.2** Fallback 처리
  - 블러를 지원하지 않거나 성능이 낮은 기기에서 단순 투명도(Alpha)로 대체되는지 확인
- [ ] **6.3** 다크 모드/라이트 모드 점검
  - 두 모드에서 모두 텍스트 가독성이 확보되는지 확인

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/06_Glass성능최적화_2025-12-11.md`
- [ ] 기록할 내용:
  - 성능 측정 결과
  - 최종 릴리즈 노트용 요약

---

## 📊 진행 상황 추적

| 단계 | 작업명 | 상태 | 시작일 | 완료일 | 작업결과서 |
|------|--------|------|--------|--------|-----------|
| 1 | 라이브러리 및 리소스 설정 | ✅ 완료 | 2025-12-11 | 2025-12-11 | [01_Glass라이브러리설정_2025-12-11.md](../working_history/version_2.0/01_Glass라이브러리설정_2025-12-11.md) |
| 2 | 기반 컴포넌트(Primitive) 구현 | ✅ 완료 | 2025-12-11 | 2025-12-11 | [02_Glass기반컴포넌트구현_2025-12-11.md](../working_history/version_2.0/02_Glass기반컴포넌트구현_2025-12-11.md) |
| 3 | 메인 네비게이션 개편 | ✅ 완료 | 2025-12-11 | 2025-12-11 | [03_Glass네비게이션구현_2025-12-11.md](../working_history/version_2.0/03_Glass네비게이션구현_2025-12-11.md) |
| 4 | 주요 화면 및 카드 적용 | ✅ 완료 | 2025-12-11 | 2025-12-11 | [04_주요화면Glass적용_2025-12-11.md](../working_history/version_2.0/04_주요화면Glass적용_2025-12-11.md) |
| 5 | 다이얼로그 및 폴리싱 | ⬜ 대기 | - | - | - |
| 6 | 성능 최적화 및 테스트 | ⬜ 대기 | - | - | - |

**상태 범례**: ⬜ 대기 | 🔄 진행중 | ✅ 완료 | ⏸️ 보류

---

## ⚠️ 주의사항

### 디자인/성능 밸런스
- 실시간 블러는 비용이 비싼 연산임. 리스트의 모든 아이템에 과도한 블러를 적용하면 스크롤 성능이 저하될 수 있음.
- 필요한 경우 "정적 블러(Static Blur)"와 "동적 블러(Dynamic/Realtime Blur)"를 구분해서 적용.

### 가독성 유지
- 화려한 배경으로 인해 텍스트 가독성이 떨어지지 않도록, 텍스트 레이어 아래에 적절한 Dim 레이어 확보 필수.
