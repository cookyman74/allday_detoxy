# 2025-10-19: Week 2A - 2A.3 기본 통계 계산 모듈

## 📋 작업 개요

**작업 범위**: [01_advanced_setting_report_todolist.md](../docs/01_advanced_setting_report_todolist.md) § 2A.3 기본 통계 계산 모듈 (Day 5-6)  
**작업 기간**: 2025-10-19  
**담당자**: AI Assistant  
**상태**: ✅ 완료

---

## 🎯 목표

리포트 고도화를 위한 기본 통계 계산 유틸리티 구현:
1. **총 집중 시간 계산** (성공 + 실패 세션 포함)
2. **집중률 계산** (성공 세션 / 전체 세션)
3. **평균 집중 유지 시간 계산** (성공/실패 분리)
4. **포인트 누적 추세 계산** (7일/30일 기준)

---

## ✅ 완료된 작업

### 1. FocusSessionDao 확장 - 날짜 범위 쿼리 추가

#### 📁 수정된 파일
- **`data/local/dao/FocusSessionDao.kt`** (100줄 ← 71줄)

#### ✨ 추가된 메서드

##### 1.1. getSessionsInLastDays()
```kotlin
@Query("""
    SELECT * FROM focus_sessions 
    WHERE DATE(startTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
    ORDER BY startTime DESC
""")
suspend fun getSessionsInLastDays(days: Int): List<FocusSession>
```
- **기능**: 최근 N일 동안의 세션 조회
- **파라미터**: `days` (예: 7, 30)
- **특징**: `'localtime'` 변환으로 로컬 타임존 기준 정확한 날짜 계산
- **사용 예**: 7일/30일 통계 계산, 포인트 추세 분석

##### 1.2. getSessionsInRange()
```kotlin
@Query("""
    SELECT * FROM focus_sessions 
    WHERE startTime >= :startTimestamp AND startTime <= :endTimestamp
    ORDER BY startTime DESC
""")
suspend fun getSessionsInRange(startTimestamp: Long, endTimestamp: Long): List<FocusSession>
```
- **기능**: 특정 날짜 범위의 세션 조회
- **파라미터**: Unix timestamp (milliseconds)
- **사용 예**: 커스텀 기간 통계, 월별 리포트

---

### 2. FocusStatisticsCalculator 생성 - 기본 통계 계산 유틸리티

#### 📁 생성된 파일
- **`domain/manager/FocusStatisticsCalculator.kt`** (163줄)

#### ✨ 주요 기능

##### 2.1. calculateTotalFocusTime()
```kotlin
fun calculateTotalFocusTime(sessions: List<FocusSession>): Int
```
- **기능**: 총 집중 시간 계산 (초 단위)
- **로직**:
  - 성공 세션: `durationMinutes * 60` (전체 시간)
  - 실패 세션: `interruptedSeconds` (실제 경과 시간)
- **반환**: 총 집중 시간 (초)
- **예시**:
  ```kotlin
  // 세션 1: 30분 성공 → 1800초
  // 세션 2: 20분 목표, 10분(600초)에서 포기 → 600초
  // 총 집중 시간: 2400초 (40분)
  ```

##### 2.2. calculateFocusRate()
```kotlin
fun calculateFocusRate(sessions: List<FocusSession>): Float
```
- **기능**: 집중률 계산 (%)
- **공식**: `(성공 세션 수 / 전체 세션 수) * 100`
- **반환**: 0~100 (세션이 없으면 0.0)
- **예시**:
  ```kotlin
  // 전체 10개 세션, 성공 7개
  // 집중률: 70.0%
  ```

##### 2.3. calculateAverageFocusDuration()
```kotlin
fun calculateAverageFocusDuration(sessions: List<FocusSession>): Pair<Int, Int>
```
- **기능**: 평균 집중 유지 시간 계산 (초 단위)
- **로직**:
  - 성공 세션: `평균 durationMinutes * 60`
  - 실패 세션: `평균 interruptedSeconds`
- **반환**: `Pair<성공 세션 평균, 실패 세션 평균>` (초 단위)
- **예시**:
  ```kotlin
  // 성공 세션: 30분, 25분, 20분 → 평균 1500초 (25분)
  // 실패 세션: 600초, 900초 → 평균 750초 (12.5분)
  // 결과: Pair(1500, 750)
  ```

##### 2.4. calculateDailyPointsTrend()
```kotlin
fun calculateDailyPointsTrend(sessions: List<FocusSession>): Map<String, Int>
```
- **기능**: 포인트 누적 추세 계산 (일별)
- **로직**:
  - 성공 세션만 포인트 획득 (1분 = 1포인트)
  - 일별로 그룹화하여 합산
  - 날짜 순으로 정렬 (YYYY-MM-DD)
- **반환**: `Map<날짜, 포인트>`
- **예시**:
  ```kotlin
  // 2025-10-19: 30분 + 25분 성공 → 55포인트
  // 2025-10-20: 20분 성공, 15분 실패 → 20포인트
  // 결과: {"2025-10-19": 55, "2025-10-20": 20}
  ```

##### 2.5. calculateSummary()
```kotlin
fun calculateSummary(sessions: List<FocusSession>): FocusStatisticsSummary
```
- **기능**: 통합 통계 계산
- **반환**: `FocusStatisticsSummary` 데이터 클래스
  ```kotlin
  data class FocusStatisticsSummary(
      val totalFocusTimeSeconds: Int,       // 총 집중 시간 (초)
      val focusRatePercent: Float,          // 집중률 (%)
      val avgSuccessDurationSeconds: Int,   // 평균 성공 세션 시간 (초)
      val avgFailedDurationSeconds: Int,    // 평균 실패 세션 시간 (초)
      val totalSessionsCount: Int,          // 전체 세션 수
      val successSessionsCount: Int,        // 성공 세션 수
      val failedSessionsCount: Int,         // 실패 세션 수
      val dailyPointsTrend: Map<String, Int> // 일별 포인트 추세
  )
  ```
- **사용 예**: 리포트 화면에서 모든 통계를 한 번에 조회

---

## 📊 사용 시나리오

### 시나리오 1: 7일 통계 조회
```kotlin
// ViewModel 또는 Repository에서
val sessions = sessionDao.getSessionsInLastDays(7)
val summary = statisticsCalculator.calculateSummary(sessions)

// UI에 표시
println("최근 7일 집중 시간: ${summary.totalFocusTimeSeconds / 60}분")
println("집중률: ${summary.focusRatePercent}%")
println("성공 세션 평균: ${summary.avgSuccessDurationSeconds / 60}분")
```

### 시나리오 2: 30일 포인트 추세
```kotlin
val sessions = sessionDao.getSessionsInLastDays(30)
val pointsTrend = statisticsCalculator.calculateDailyPointsTrend(sessions)

// 그래프에 표시
pointsTrend.forEach { (date, points) ->
    println("$date: $points 포인트")
}
```

### 시나리오 3: 커스텀 기간 통계
```kotlin
val startTimestamp = /* 시작 날짜 Unix timestamp */
val endTimestamp = /* 종료 날짜 Unix timestamp */
val sessions = sessionDao.getSessionsInRange(startTimestamp, endTimestamp)

val focusRate = statisticsCalculator.calculateFocusRate(sessions)
println("해당 기간 집중률: $focusRate%")
```

---

## 🔍 빌드 검증

### 컴파일 테스트
```bash
./gradlew compileDebugKotlin --quiet
```
**결과**: ✅ BUILD SUCCESSFUL

### Lint 검증
**결과**: ✅ 0 errors (신규 파일 2개 검증)

---

## 📊 변경 통계

### 생성된 파일 (1개)
| 파일 | 줄 수 | 설명 |
|------|-------|------|
| `domain/manager/FocusStatisticsCalculator.kt` | 163 | 기본 통계 계산 유틸리티 |

### 수정된 파일 (1개)
| 파일 | 변경 내용 | 설명 |
|------|-----------|------|
| `data/local/dao/FocusSessionDao.kt` | +29줄 (71 → 100) | 날짜 범위 쿼리 2개 추가 |

### 총 코드 라인 수
- **신규 작성**: ~163줄 (FocusStatisticsCalculator)
- **수정**: ~29줄 (FocusSessionDao)
- **합계**: ~192줄

---

## 🎯 완료된 체크리스트

✅ 총 집중 시간 계산 유틸 (성공+실패 세션 포함)  
✅ 집중률 계산 함수 (성공 세션 / 전체 세션)  
✅ 평균 집중 유지 시간 계산 (성공/실패 분리)  
✅ 포인트 누적 추세 계산 (7일/30일 기준)  
✅ FocusSessionDao에 날짜 범위 쿼리 추가  
✅ 빌드 및 Lint 검증 완료

---

## 🔧 기술적 하이라이트

### 1. 로컬 타임존 지원
```kotlin
@Query("""
    SELECT * FROM focus_sessions 
    WHERE DATE(startTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
    ORDER BY startTime DESC
""")
```
- `'localtime'` 변환으로 KST 등 로컬 타임존 기준 정확한 날짜 계산
- Task 2A.1 리뷰 피드백 반영 (UTC 타임존 이슈 해결)

### 2. 성공/실패 세션 구분 계산
```kotlin
fun calculateTotalFocusTime(sessions: List<FocusSession>): Int {
    return sessions.sumOf { session ->
        if (session.success) {
            session.durationMinutes * 60  // 성공: 전체 시간
        } else {
            session.interruptedSeconds    // 실패: 실제 경과 시간
        }
    }
}
```
- 성공 세션: 목표 시간 전체 (durationMinutes)
- 실패 세션: 실제 경과 시간 (interruptedSeconds, Task 2A.1에서 추가)
- 정확한 총 집중 시간 계산

### 3. 일별 그룹화 및 정렬
```kotlin
sessions
    .groupBy { session ->
        val date = java.time.Instant.ofEpochMilli(session.startTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
        date
    }
    .mapValues { (_, sessionsInDay) ->
        sessionsInDay.sumOf { it.durationMinutes }
    }
    .toSortedMap()  // 날짜 순 정렬
```
- `java.time.Instant`로 Unix timestamp → LocalDate 변환
- `systemDefault()` ZoneId로 로컬 타임존 적용
- `toSortedMap()`으로 자동 정렬

### 4. @Singleton 주입
```kotlin
@Singleton
class FocusStatisticsCalculator @Inject constructor() {
    // ...
}
```
- Hilt를 통한 의존성 주입
- 앱 전체에서 단일 인스턴스 사용
- 메모리 효율성 확보

---

## 🧪 향후 작업 (Week 2B)

### Task 2B.2: 고급 통계 계산 모듈 (Day 8-10)
- [ ] **디톡시 위험 지수** 산식 정의 및 구현
- [ ] **디톡시 회복률 추세** 계산 유틸
- [ ] 집중률 성장세 계산
- [ ] 유혹 저항 시간·포기 지점 분석 함수

### Task 2B.3: ReportViewModel 리팩토링 (Day 10-11)
- [ ] `FocusStatisticsCalculator` 주입 및 연동
- [ ] 7일/30일 통계 StateFlow 추가
- [ ] 일간/주간 인사이트 UI 상태 업데이트

---

## 📌 참조 문서

- **PRD**: [01_advanced_prd.md](../docs/01_advanced_prd.md) § 4.2 리포트 고도화
- **Analytics 스키마**: [01_advanced_analytics_schema.md](../docs/01_advanced_analytics_schema.md)
- **QA 시나리오**: [01_advanced_qa_devices.md](../docs/01_advanced_qa_devices.md)
- **이전 작업**: [2025-10-19_1st_advanced_2A.2.md](./2025-10-19_1st_advanced_2A.2.md)

---

## 📝 주의사항

### 단위 테스트
- 현재 단위 테스트는 구현하지 않음 (선택적 요구사항)
- 향후 Week 3 QA 단계에서 통합 테스트로 검증 예정
- 필요 시 JUnit + Mockito로 테스트 추가 가능

### 날짜 범위 쿼리 성능
- `getSessionsInLastDays()`는 인덱스가 없는 `startTime` 컬럼 사용
- 세션 수가 많아지면 성능 저하 가능
- 향후 필요 시 `startTime` 컬럼에 인덱스 추가 고려

### 포인트 계산 일관성
- 현재는 성공 세션만 포인트 획득 (1분 = 1포인트)
- `GamificationManager.calculatePoints()`와 동일한 로직 사용
- 향후 포인트 정책 변경 시 두 곳 모두 수정 필요

---

## 📌 커밋 정보

**브랜치**: `feat/v0.5`  
**커밋 ID**: `692a37d` (2025-10-19)

**커밋 메시지**: `feat(statistics): Task 2A.3 기본 통계 계산 모듈 구현`

**커밋 내용**:
- 생성: 2개 파일 (FocusStatisticsCalculator, 작업 문서)
- 수정: 2개 파일 (FocusSessionDao, 체크리스트)
- 합계: 536 insertions, 5 deletions

**작업 완료일**: 2025-10-19  
**총 소요 시간**: ~1시간

---

**✅ Task 2A.3 기본 통계 계산 모듈 완료!**

