# 2025-10-19: Week 2B - 2B.3.1 ReportViewModel 리팩토링 & 데이터 연동

## 📋 작업 개요

**작업 범위**: [01_advanced_setting_report_todolist.md](../docs/01_advanced_setting_report_todolist.md) § 2B.3.1 ReportViewModel 리팩토링 & 데이터 연동 (Day 10)  
**작업 기간**: 2025-10-19  
**담당자**: AI Assistant  
**상태**: ✅ 완료

---

## 🎯 목표

신규 고급 통계 계산기를 ReportViewModel에 통합하고 데이터 흐름 구성:
1. **ReportUiState** 생성 및 신규 통계 필드 추가
2. **DetoxyAdvancedStatistics** 의존성 주입
3. **loadReportData()** 메서드 확장 (7일 데이터 로드)
4. **빈 상태 처리** 로직 구현
5. **하위 호환성** 유지 (기존 ReportScreen과 호환)

---

## ✅ 완료된 작업

### 1. ReportUiState 생성

#### 📁 생성된 파일
- **`presentation/viewmodel/ReportUiState.kt`** (60줄)

#### ✨ 주요 기능
**통합 UI 상태 관리**:
- 기본 데이터 (todaySessions, settings, isLoading, error)
- 신규 고급 통계 필드:
  - `riskIndex: DetoxyRiskIndex?`
  - `recoveryTrend: DetoxyRecoveryTrend?`
  - `topDistractions: List<DistractionItem>`
  - `resistanceAnalysis: ResistanceTimeAnalysis?`
  - `giveUpAnalysis: GiveUpPointAnalysis?`
  - `coachRecommendation: CoachRecommendation?`
- 빈 상태 플래그 (`hasData: Boolean`)
- 기본 통계 계산 메서드 (`getSuccessSessionCount`, `getTotalFocusMinutes`, etc.)

```kotlin
data class ReportUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val todaySessions: List<FocusSession> = emptyList(),
    val settings: UserSettings = UserSettings(1, 0, 0, null),
    
    // 신규 고급 통계 (Week 2B)
    val riskIndex: DetoxyRiskIndex? = null,
    val recoveryTrend: DetoxyRecoveryTrend? = null,
    val topDistractions: List<DistractionItem> = emptyList(),
    val resistanceAnalysis: ResistanceTimeAnalysis? = null,
    val giveUpAnalysis: GiveUpPointAnalysis? = null,
    val coachRecommendation: CoachRecommendation? = null,
    
    val hasData: Boolean = false
)
```

---

### 2. ReportViewModel 리팩토링

#### 📁 수정된 파일
- **`presentation/viewmodel/ReportViewModel.kt`** (170줄)

#### ✨ 주요 변경사항

**1) DetoxyAdvancedStatistics 의존성 주입**:
```kotlin
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val focusInterruptionDao: FocusInterruptionDao,
    private val advancedStatistics: DetoxyAdvancedStatistics  // 신규 추가
) : ViewModel()
```

**2) 통합 UiState 적용**:
- 기존: 개별 StateFlow (`todaySessions`, `settings`, `isLoading`)
- 변경: 단일 `ReportUiState` StateFlow

**3) loadAdvancedStatistics() 구현**:
```kotlin
private fun loadAdvancedStatistics() {
    // 최근 7일 세션 데이터 로드
    val recentSessions = repository.getSessionsInRange(sevenDaysAgo, now)
    
    // 최근 7일 차단 이벤트 로드
    val recentInterruptions = focusInterruptionDao.getInterruptionsInLastDays(7)
    
    // 종합 인사이트 계산
    val insights = advancedStatistics.calculateComprehensiveInsights(
        sessions = recentSessions,
        interruptions = recentInterruptions
    )
    
    // UI 상태 업데이트
    _uiState.update { 
        it.copy(
            hasData = true,
            riskIndex = insights.riskIndex,
            recoveryTrend = insights.recoveryTrend,
            topDistractions = insights.topDistractions,
            resistanceAnalysis = insights.resistanceAnalysis,
            giveUpAnalysis = insights.giveUpAnalysis,
            coachRecommendation = insights.coachRecommendation
        )
    }
}
```

**4) 빈 상태 처리**:
```kotlin
if (hasData) {
    // 통계 계산 및 업데이트
} else {
    // 빈 상태 설정
    _uiState.update { 
        it.copy(
            hasData = false,
            riskIndex = null,
            recoveryTrend = null,
            // ... 모든 필드 null 처리
        )
    }
}
```

**5) 하위 호환성 유지** (Task 2B.3.2에서 제거 예정):
```kotlin
// 기존 API 유지
val todaySessions: StateFlow<List<FocusSession>>
val settings: StateFlow<UserSettings>
val isLoading: StateFlow<Boolean>

fun getSuccessSessionCount(): Int
fun getTotalFocusMinutes(): Int
fun getSuccessRate(): Float
```

---

### 3. FocusRepository 확장

#### 📁 수정된 파일
- **`domain/repository/FocusRepository.kt`**
- **`data/repository/FocusRepositoryImpl.kt`**

#### ✨ 추가된 메서드
```kotlin
/**
 * 기간별 세션 조회
 *
 * @param startTime 시작 시간 (밀리초)
 * @param endTime 종료 시간 (밀리초)
 * @return 기간 내 세션 리스트
 */
suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<FocusSession>
```

**구현**:
```kotlin
override suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<FocusSession> {
    return sessionDao.getSessionsInRange(startTime, endTime)
}
```

---

### 4. FocusInterruptionDao 확장

#### 📁 수정된 파일
- **`data/local/dao/FocusInterruptionDao.kt`**

#### ✨ 추가된 메서드
```kotlin
/**
 * 최근 N일 동안의 차단 이벤트 조회
 *
 * @param days 조회할 일수 (예: 7, 30)
 * @return 최근 N일의 차단 이벤트 리스트
 */
@Query("""
    SELECT * FROM focus_interruptions 
    WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
    ORDER BY timestamp DESC
""")
suspend fun getInterruptionsInLastDays(days: Int): List<FocusInterruption>
```

---

## 🔍 빌드 검증

### 컴파일 테스트
```bash
./gradlew compileDebugKotlin --quiet
```
**결과**: ✅ BUILD SUCCESSFUL

### Lint 검증
**결과**: ✅ 0 errors (presentation/viewmodel 패키지)

---

## 📊 변경 통계

### 생성된 파일 (1개)
| 파일 | 줄 수 | 설명 |
|------|-------|------|
| `ReportUiState.kt` | 60 | 통합 UI 상태 관리 |

### 수정된 파일 (4개)
| 파일 | 변경 내용 |
|------|-----------|
| `ReportViewModel.kt` | DetoxyAdvancedStatistics 의존성 주입, 고급 통계 로딩, 하위 호환성 유지 |
| `FocusRepository.kt` | getSessionsInRange() 메서드 추가 |
| `FocusRepositoryImpl.kt` | getSessionsInRange() 메서드 구현 |
| `FocusInterruptionDao.kt` | getInterruptionsInLastDays() 메서드 추가 |

### 총 코드 라인 수
- **신규 작성**: ~60줄 (1개 파일)
- **수정**: ~130줄 (4개 파일)
- **합계**: ~190줄

---

## 🎯 완료된 체크리스트

✅ ReportViewModel에 `DetoxyAdvancedStatistics` 의존성 주입 (Hilt)  
✅ 기존 `ReportUiState`에 신규 통계 필드 추가 (6개 필드)  
✅ `loadReportData()` 메서드 확장: 7일 세션 데이터 로드 및 고급 통계 계산  
✅ 빈 상태 처리 로직 구현: 세션 데이터 없을 때 기본값 표시  
✅ 데이터 로딩 상태 관리 (`Loading`, `Success`, `Error`)  
✅ 하위 호환성 유지: 기존 ReportScreen API 유지

---

## 🔧 기술적 하이라이트

### 1. 통합 UiState 패턴
```kotlin
private val _uiState = MutableStateFlow(ReportUiState())
val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
```
- 단일 진실 공급원 (Single Source of Truth)
- 상태 업데이트 추적 용이
- Compose UI와 자연스럽게 연동

### 2. 고급 통계 계산 플로우
```
1. Repository에서 최근 7일 세션 조회
2. DAO에서 최근 7일 차단 이벤트 조회
3. DetoxyAdvancedStatistics.calculateComprehensiveInsights() 호출
4. UI 상태 업데이트 (riskIndex, recoveryTrend, etc.)
```

### 3. 빈 상태 처리
```kotlin
val hasData = recentSessions.isNotEmpty()

if (hasData) {
    // 통계 계산 및 표시
} else {
    // "아직 집중 세션이 없어요" 상태
}
```

### 4. 하위 호환성 전략
```kotlin
// 기존 API → 새로운 UiState 매핑
val todaySessions: StateFlow<List<FocusSession>>
    get() = uiState.map { it.todaySessions }.stateIn(...)
```
- Task 2B.3.2에서 ReportScreen UI 업데이트 시 제거 예정
- 단계적 마이그레이션 가능

---

## 🧪 향후 작업 (Week 2B)

### Task 2B.3.2: 일간/주간 카드 UI 구현 (Day 11)
- [ ] **위험 지수 카드** Composable 구현
- [ ] **회복률 추세 카드** Composable 구현
- [ ] **주간 인사이트 그래프** 2종 (Canvas)
- [ ] **방해요인 Top 3 카드** 구현
- [ ] ReportScreen에 신규 카드 통합
- [ ] 기존 하위 호환성 API 제거

### Task 2B.3.3: 고급 카드 & 최종 통합 (Day 12)
- [ ] 분산 회피율 카드
- [ ] 허용 앱 체류 시간 카드
- [ ] 디톡시 코치 추천 카드/다이얼로그
- [ ] Analytics 이벤트 연동

---

## 📌 참조 문서

- **PRD**: [01_advanced_prd.md](../docs/01_advanced_prd.md) § 4.2 리포트 고도화
- **이전 작업**: [2025-10-19_1st_advanced_2B.2.md](./2025-10-19_1st_advanced_2B.2.md) (고급 통계 계산 모듈)
- **Todolist**: [01_advanced_setting_report_todolist.md](../docs/01_advanced_setting_report_todolist.md) § 2B.3.1

---

## 📌 최종 커밋 정보 (Task 2B.3.1)

| 항목 | 내용 |
|------|------|
| **브랜치** | `feat/v0.5` |
| **커밋 ID** | `87c3296` |
| **커밋 메시지** | `feat(report): Task 2B.3.1 ReportViewModel 리팩토링 & 데이터 연동` |
| **변경 통계** | 7 files changed, 565 insertions(+), 62 deletions(-) |
| **작업 완료일** | 2025-10-19 |
| **총 소요 시간** | ~2시간 |

**커밋 내용**:
- 생성: 1개 파일 (`ReportUiState.kt`, `2025-10-19_1st_advanced_2B.3.1.md`)
- 수정: 5개 파일 (ReportViewModel, FocusRepository 관련 4개, todolist)
- 합계: 565줄 추가, 62줄 삭제

---

## 🔧 리뷰 피드백 반영 (2025-10-19 추가)

### 🚨 발견된 이슈 (2건)

#### 1. Flow 수집 코루틴 중복 생성 (Critical)
**문제점**: `refresh()` 호출 시마다 `loadReportData()` 내에서 `repository.getTodaySessions().collect { … }`와 `repository.getSettings().collect { … }`가 `viewModelScope.launch`로 실행되어, 기존 코루틴을 취소하지 않은 채 동일한 Flow를 여러 번 수집하게 되어 중복 업데이트 및 메모리 누수 위험이 있었습니다.

**해결 방법**:
```kotlin
// Before: refresh() 호출 시마다 Flow 수집 재시작
fun refresh() {
    loadReportData()  // 매번 collect 재실행
}

// After: init에서만 Flow 수집, refresh는 고급 통계만 재계산
init {
    loadReportData()  // Flow 수집은 한 번만
}

fun refresh() {
    loadAdvancedStatistics()  // 고급 통계만 재계산
}
```

**영향 범위**:
- `ReportViewModel.kt` - `refresh()` 메서드 수정
- Flow 수집은 `init`에서 한 번만 실행되도록 보장
- `refresh()`는 고급 통계만 재계산하여 중복 수집 방지

---

#### 2. Clean Architecture 위반 - ViewModel이 직접 DAO에 의존 (Critical)
**문제점**: ReportViewModel이 `FocusInterruptionDao`를 직접 주입받아 `getInterruptionsInLastDays()`를 호출하여, presentation 레이어가 data 레이어 구현에 종속되고 Clean Architecture 경계가 무너졌습니다.

**해결 방법**:
```kotlin
// Before: ViewModel이 DAO 직접 사용
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val focusInterruptionDao: FocusInterruptionDao,  // ❌ data 레이어 의존
    private val advancedStatistics: DetoxyAdvancedStatistics
)

// After: Repository를 통해 추상화
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: FocusRepository,  // ✅ domain 레이어만 의존
    private val advancedStatistics: DetoxyAdvancedStatistics
)
```

**구현 단계**:

1. **FocusRepository에 메서드 추가**:
```kotlin
// domain/repository/FocusRepository.kt
suspend fun getInterruptionsInLastDays(days: Int): List<FocusInterruption>
```

2. **FocusRepositoryImpl에 구현 추가**:
```kotlin
// data/repository/FocusRepositoryImpl.kt
override suspend fun getInterruptionsInLastDays(days: Int): List<FocusInterruption> {
    return interruptionDao.getInterruptionsInLastDays(days)
}
```

3. **ReportViewModel에서 사용**:
```kotlin
// Before
val recentInterruptions = focusInterruptionDao.getInterruptionsInLastDays(7)

// After
val recentInterruptions = repository.getInterruptionsInLastDays(7)
```

**영향 범위**:
- `FocusRepository.kt` - 인터페이스에 메서드 추가
- `FocusRepositoryImpl.kt` - 구현 추가
- `ReportViewModel.kt` - DAO 의존성 제거, Repository 사용

---

### ✅ 수정 검증

#### 빌드 테스트
```bash
./gradlew compileDebugKotlin --quiet
```
**결과**: ✅ BUILD SUCCESSFUL

#### Lint 검증
**결과**: ✅ 0 errors (ReportViewModel)

---

### 📊 리뷰 반영 변경 통계

| 파일 | 변경 내용 |
|------|-----------|
| `FocusRepository.kt` | `getInterruptionsInLastDays()` 메서드 추가 |
| `FocusRepositoryImpl.kt` | `getInterruptionsInLastDays()` 구현 추가 |
| `ReportViewModel.kt` | DAO 의존성 제거, Flow 수집 로직 개선, refresh() 최적화 |

### 🎯 개선 효과

1. **메모리 누수 방지**: Flow 수집이 init에서 한 번만 실행되어 중복 수집 방지
2. **Clean Architecture 준수**: Presentation → Domain → Data 레이어 경계 유지
3. **성능 최적화**: `refresh()` 호출 시 고급 통계만 재계산하여 불필요한 Flow 재구독 방지
4. **테스트 용이성**: Repository 추상화로 단위 테스트 작성 용이

---

**✅ Task 2B.3.1 ReportViewModel 리팩토링 & 데이터 연동 완료!**  
**✅ 리뷰 피드백 반영 완료 (2025-10-19)**

