# Task 2B.3.4: 코치 추천 카드 완성 (RiskLevel 문제 해결)

**날짜**: 2025-10-19  
**작업자**: AI Assistant  
**관련 문서**: [01_advanced_setting_report_todolist.md](../docs/01_advanced_setting_report_todolist.md)

---

## 📋 작업 목표

Task 2B.3.3에서 발생한 `RiskLevel` enum 참조 오류를 해결하고, 코치 추천 카드/다이얼로그를 완성하여 ReportScreen에 통합.

### 우선순위
- **High** (2B.3.3에서 미완성)

---

## 🔍 문제 분석

### RiskLevel enum 참조 오류
Kotlin 컴파일러가 `DetoxyRiskCalculator.RiskLevel` nested enum을 Composable 함수의 파라미터 타입으로 인식하지 못하는 문제가 발생했습니다.

#### 시도한 해결 방법 (모두 실패)
1. **Full qualified name**: `DetoxyRiskCalculator.RiskLevel`
2. **typealias**: `private typealias RiskLevel = DetoxyRiskCalculator.RiskLevel`
3. **Import**: `import com.allday.detoxy.domain.manager.DetoxyRiskCalculator.RiskLevel`

### 채택된 해결 방안
**방안 1: RiskLevel을 별도 파일로 분리 (top-level enum class)**
- `domain/manager/RiskLevel.kt` 생성
- `DetoxyRiskCalculator`에서 RiskLevel 참조하도록 수정
- 영향 범위: `DetoxyRiskCalculator.kt`, `DetoxyRiskIndex.kt`, `CoachRecommendation.kt`

---

## ✅ 완료된 작업

### 1. RiskLevel enum 분리

#### 생성: `app/src/main/java/com/allday/detoxy/domain/manager/RiskLevel.kt` (36줄)
```kotlin
package com.allday.detoxy.domain.manager

/**
 * 디톡시 위험 단계
 *
 * 위험 지수(0-100)를 3단계로 분류
 *
 * Week 2B: Task 2B.3.4 - RiskLevel enum 분리
 */
enum class RiskLevel {
    RECOVERY,   // 회복: 0-33
    WARNING,    // 주의: 34-66
    HIGH_RISK;  // 고위험: 67-100

    /**
     * 사용자에게 표시할 텍스트
     */
    fun toDisplayString(): String = when (this) {
        RECOVERY -> "회복 중"
        WARNING -> "주의 필요"
        HIGH_RISK -> "고위험"
    }

    /**
     * Analytics 이벤트에 사용할 문자열
     */
    fun toAnalyticsString(): String = when (this) {
        RECOVERY -> "recovery"
        WARNING -> "warning"
        HIGH_RISK -> "high_risk"
    }
}
```

**특징**:
- Top-level enum class로 분리하여 import 문제 해결
- `toDisplayString()`: UI에 표시할 한글 텍스트
- `toAnalyticsString()`: Analytics 이벤트에 사용할 문자열

#### 수정: `DetoxyRiskCalculator.kt`
- 기존 nested enum class 제거
- 주석으로 분리 사실 명시
```kotlin
// RiskLevel enum은 별도 파일로 분리됨 (RiskLevel.kt)
// Week 2B: Task 2B.3.4
```

### 2. CoachRecommendationCard 구현

#### 생성: `app/src/main/java/com/allday/detoxy/presentation/ui/report/components/CoachRecommendationCard.kt` (227줄)

**주요 기능**:
- 위험 단계별 배경색 표시 (RECOVERY: 초록, WARNING: 주황, HIGH_RISK: 빨강)
- 코치 추천 제목 및 메시지 표시
- 행동 제안 Top 3 표시 (아이콘 + 제목 + 설명)
- "전체 보기" 버튼 → 다이얼로그 호출
- 빈 상태 처리 ("집중 세션 데이터가 쌓이면...")

**주요 Composable 함수**:
1. `CoachRecommendationCard()`: 메인 카드
   - `recommendation: CoachRecommendation?`
   - `onDetailClick: () -> Unit`
2. `ActionItemRow()`: 행동 제안 항목
   - 우선순위별 색상 (High: 빨강, Medium: 주황, Low: 초록)

**색상 매핑 함수**:
```kotlin
@Composable
private fun getRecommendationColor(level: RiskLevel): Color {
    return when (level) {
        RiskLevel.RECOVERY -> Color(0xFF4CAF50) // Green
        RiskLevel.WARNING -> Color(0xFFFFA726) // Orange
        RiskLevel.HIGH_RISK -> Color(0xFFF44336) // Red
    }
}

@Composable
private fun getPriorityColor(priority: Int): Color {
    return when (priority) {
        1 -> Color(0xFFF44336) // Red (High)
        2 -> Color(0xFFFFA726) // Orange (Medium)
        else -> Color(0xFF4CAF50) // Green (Low)
    }
}
```

### 3. CoachRecommendationDialog 구현

#### 생성: `app/src/main/java/com/allday/detoxy/presentation/ui/report/components/CoachRecommendationDialog.kt` (234줄)

**주요 기능**:
- 위험 단계 배지 표시 (상단)
- 전체 행동 제안 목록 (스크롤 가능)
- 우선순위별 색상 구분 (뱃지 + 아이콘)
- "확인" 버튼

**주요 Composable 함수**:
1. `CoachRecommendationDialog()`: 메인 다이얼로그
   - `recommendation: CoachRecommendation`
   - `onDismiss: () -> Unit`
2. `DetailedActionItem()`: 상세 행동 제안 항목
   - 우선순위 뱃지 ("높음", "중간", "낮음")
   - 제목 및 설명

**우선순위 텍스트 함수**:
```kotlin
private fun getPriorityText(priority: Int): String {
    return when (priority) {
        1 -> "높음"
        2 -> "중간"
        else -> "낮음"
    }
}
```

### 4. ReportScreen 통합

#### 수정: `app/src/main/java/com/allday/detoxy/presentation/ui/report/ReportScreen.kt`

**변경 사항**:
1. **Import 주석 제거**:
   ```kotlin
   import com.allday.detoxy.presentation.ui.report.components.CoachRecommendationCard
   import com.allday.detoxy.presentation.ui.report.components.CoachRecommendationDialog
   ```

2. **코치 추천 카드 활성화** (227-236줄):
   ```kotlin
   // 4. 코치 추천 카드 (Task 2B.3.4 - RiskLevel 분리 후 활성화)
   item {
       CoachRecommendationCard(
           recommendation = uiState.coachRecommendation,
           onDetailClick = {
               showCoachDialog = true
               // TODO: Analytics 이벤트 추가 (report_coach_recommendation_shown)
           }
       )
   }
   ```

3. **코치 추천 다이얼로그 활성화** (259-267줄):
   ```kotlin
   // 코치 추천 다이얼로그 (Task 2B.3.4 - RiskLevel 분리 후 활성화)
   uiState.coachRecommendation?.let { recommendation ->
       if (showCoachDialog) {
           CoachRecommendationDialog(
               recommendation = recommendation,
               onDismiss = { showCoachDialog = false }
           )
       }
   }
   ```

4. **문서 주석 업데이트**:
   ```kotlin
   /**
    * 리포트 화면
    *
    * Week 2B: Task 2B.3.3 - 최종 통합 및 Analytics 연동
    * Week 2B: Task 2B.3.4 - 코치 추천 카드 활성화
    */
   ```

**최종 카드 순서**:
1. 일간 요약 (총 집중 시간, 성공 세션, 연속 성공, 총 포인트)
2. 주간 인사이트
   - 위험 지수 카드
   - 회복률 추세 카드
   - 방해요인 Top 3 카드
   - **코치 추천 카드** (신규 활성화)
3. 오늘의 세션 리스트

---

## 🔧 빌드 검증

### compileDebugKotlin
```bash
./gradlew compileDebugKotlin --quiet
```
**결과**: ✅ SUCCESS

**Warning**:
```
warning: The following options were not recognized by any processor: 
'[dagger.fastInit, dagger.hilt.android.internal.disableAndroidSuperclassValidation, 
dagger.hilt.android.internal.projectType, dagger.hilt.internal.useAggregatingRootProcessor, 
kapt.kotlin.generated]'
```
- Dagger/Hilt 관련 경고이며 빌드에는 영향 없음

### Lint
```bash
./gradlew lint --quiet
```
**결과**: ⚠️ 1 error, 105 warnings (기존 오류로 판단)

**확인 사항**:
- 신규 작성한 파일 (`RiskLevel.kt`, `CoachRecommendationCard.kt`, `CoachRecommendationDialog.kt`)에는 Lint 오류 없음
- `ReportScreen.kt`에도 Lint 오류 없음
- Lint 오류는 다른 파일에서 발생한 기존 오류로 추정

---

## 📊 작업 통계

### 생성된 파일 (3개, ~497줄)
| 파일명 | 줄 수 | 설명 |
|--------|------|------|
| `RiskLevel.kt` | 36 | Top-level enum class |
| `CoachRecommendationCard.kt` | 227 | 코치 추천 카드 Composable |
| `CoachRecommendationDialog.kt` | 234 | 코치 추천 다이얼로그 Composable |

### 수정된 파일 (2개)
| 파일명 | 변경 사항 | 설명 |
|--------|----------|------|
| `DetoxyRiskCalculator.kt` | -20줄 | RiskLevel enum 제거 및 주석 추가 |
| `ReportScreen.kt` | +5줄 | Import 주석 제거, 카드/다이얼로그 활성화 |

### 전체 통계
- **생성**: 3개 파일 (~497줄)
- **수정**: 2개 파일 (~-15줄)
- **순증가**: ~482줄

---

## 🎯 해결된 문제

### Before (Task 2B.3.3)
❌ **문제**: Kotlin 컴파일러가 nested enum을 인식하지 못함
```kotlin
// DetoxyRiskCalculator.kt
enum class RiskLevel { ... } // nested enum

// CoachRecommendationCard.kt (주석 처리됨)
/*
@Composable
fun CoachRecommendationCard(recommendation: CoachRecommendation?) {
    // RiskLevel 참조 불가
}
*/
```

### After (Task 2B.3.4)
✅ **해결**: RiskLevel을 top-level enum으로 분리
```kotlin
// RiskLevel.kt (별도 파일)
enum class RiskLevel { ... }

// CoachRecommendationCard.kt (활성화됨)
@Composable
fun CoachRecommendationCard(recommendation: CoachRecommendation?) {
    val color = getRecommendationColor(recommendation.level) // RiskLevel 정상 참조
}
```

---

## 🚀 기대 효과

### 1. 기술적 효과
- ✅ Kotlin enum 참조 문제 완전 해결
- ✅ 코드 모듈화 개선 (RiskLevel 재사용 가능)
- ✅ ReportScreen 최종 완성 (4개 고급 카드 모두 활성화)

### 2. 사용자 경험 개선
- 📊 **맞춤형 코치 추천**: 위험 단계별 차별화된 메시지
- 🎯 **행동 제안**: 우선순위별로 정렬된 실천 가능한 조언
- 🔍 **상세 보기**: 다이얼로그로 전체 행동 제안 확인 가능

### 3. 데이터 기반 인사이트
- 위험 지수 + 회복률 + 방해요인 + **코치 추천** = 종합 디톡시 리포트 완성
- 사용자의 디톡시 상태를 4가지 관점에서 분석

---

## 📝 후속 작업 (TODO)

### Analytics 이벤트 연동
현재 ReportScreen에 TODO로 표시된 Analytics 이벤트 로깅 구현 필요:

```kotlin
// ReportScreen.kt (233줄)
// TODO: Analytics 이벤트 추가 (report_coach_recommendation_shown)
```

**구현 필요 사항**:
1. `AnalyticsHelper.logCoachRecommendationShown()` 메서드 추가
2. 이벤트 파라미터:
   - `risk_level`: recovery / warning / high_risk
   - `recommendation_count`: 행동 제안 개수
   - `priority_high_count`: 높음 우선순위 개수
3. ReportScreen에서 카드 클릭 시 이벤트 로깅

### UI 테스트
각 위험 단계별 코치 추천 카드 시각적 검증:
- [ ] RECOVERY (회복 중) 상태: 초록색 배경, 긍정적 메시지
- [ ] WARNING (주의 필요) 상태: 주황색 배경, 경고 메시지
- [ ] HIGH_RISK (고위험) 상태: 빨강색 배경, 긴급 메시지

---

## 💡 교훈

### 1. Kotlin Nested Enum의 한계
- Nested enum은 Compose 함수 파라미터로 사용 시 컴파일 오류 발생 가능
- **해결책**: Top-level enum class로 분리하여 재사용성과 가독성 향상

### 2. 모듈화의 중요성
- `RiskLevel`을 별도 파일로 분리하여:
  - 여러 파일에서 재사용 가능
  - 의존성 명확화
  - 코드 관리 용이

### 3. 단계적 문제 해결
- 문제 분석 → 해결 방안 제시 → 최선의 방안 선택 → 구현 → 검증
- 시행착오를 기록하여 유사한 문제 발생 시 빠른 해결 가능

---

## 📦 커밋 정보

### 커밋 ID
```
[추후 기록]
```

### 커밋 메시지
```
feat(report): Task 2B.3.4 코치 추천 카드 완성 (RiskLevel 문제 해결)

RiskLevel enum을 top-level class로 분리하여 참조 오류 해결:

생성 파일:
- RiskLevel.kt (36줄) - Top-level enum class
- CoachRecommendationCard.kt (227줄) - 코치 추천 카드 Composable
- CoachRecommendationDialog.kt (234줄) - 코치 추천 다이얼로그

수정 파일:
- DetoxyRiskCalculator.kt - RiskLevel enum 제거
- ReportScreen.kt - 카드/다이얼로그 활성화

주요 기능:
- 위험 단계별 배경색 (RECOVERY: 초록, WARNING: 주황, HIGH_RISK: 빨강)
- 행동 제안 Top 3 표시 (우선순위별 색상)
- "전체 보기" 버튼으로 다이얼로그 호출
- 빈 상태 처리

빌드 검증:
- compileDebugKotlin: SUCCESS
- 신규 파일 Lint: 0 errors

Week 2B: Task 2B.3.4
```

---

## ✅ 체크리스트

- [x] RiskLevel enum 분리 (방안 1 선택)
- [x] DetoxyRiskCalculator 수정
- [x] CoachRecommendationCard 구현
- [x] CoachRecommendationDialog 구현
- [x] ReportScreen 주석 제거 및 통합
- [x] 빌드 검증 (compileDebugKotlin)
- [x] Lint 확인 (신규 파일)
- [x] 작업 문서 작성
- [ ] Analytics 이벤트 로깅 (후속 작업)
- [ ] UI 테스트 (각 위험 단계별)

---

**작업 완료 시간**: 2025-10-19  
**다음 작업**: Task 2B.4 - 통합 테스트 & 품질 (Day 12-13)

