# Allday Detoxy 4차 고도화 PRD
## 시간표 템플릿 및 AI 기반 스케줄 추천

- **버전**: v0.8
- **선행 작업**: 2.5차 고도화 (v0.7) 완료 필수
- **예상 기간**: 3주
- **담당자**: [TBD]
- **최종 수정일**: 2025-10-26

---

## 1. 배경 및 목표

### 1.1 배경

**2.5차 고도화 달성**:
- ✅ 위치 기반 컨텍스트 + 시간 기반 스케줄 복합 시나리오 구현
- ✅ ScheduleGroup을 통한 시간표 관리
- ✅ 위치 진입/이탈 시 자동 활성화/비활성화

**남은 과제**:
- 사용자가 시간표를 처음부터 만들어야 하는 부담
- 최적의 시간대 설정에 대한 가이드 부족
- 개인별 루틴 패턴 파악 및 제안 부재
- 시간표 설정 진입 장벽으로 인한 낮은 활용률

**사용자 피드백**:
> "시간표를 만들고 싶은데 어떻게 설정해야 할지 모르겠어요"
> "업무용 템플릿이 있으면 좋겠어요"
> "내 패턴에 맞는 시간표를 추천해줬으면 좋겠어요"

### 1.2 목표

**주요 목표**:
1. **시간표 템플릿 제공**: 업무, 공부, 운동 등 사전 정의된 템플릿으로 즉시 시작
2. **AI 기반 추천**: 사용자의 과거 패턴을 분석하여 최적의 시간표 추천
3. **설정 진입 장벽 완화**: 3번의 클릭으로 시간표 설정 완료

**부가 목표**:
- 시간표 공유 및 가져오기
- 커뮤니티 베스트 시간표
- 시즌별/상황별 템플릿 (시험 기간, 프로젝트 기간 등)

### 1.3 성공 지표

**기능 지표**:
- 템플릿 사용률 ≥ 70% (시간표 생성자 대비)
- AI 추천 수락률 ≥ 40%
- 시간표 설정 완료율 ≥ 80% (설정 시작자 대비)

**사용자 경험**:
- 시간표 설정 완료 시간 ≤ 2분 (템플릿 사용 시)
- 시간표 활용률 ≥ 60% (생성 후 7일 내 활성화)
- AI 추천 만족도 ≥ 75% (설문조사)

**비즈니스 지표**:
- 시간표 기능 사용자 증가율 ≥ 100% (v0.7 대비)
- 주간 활성 사용자(WAU) 증가율 ≥ 30%
- 사용자 리텐션 7일 ≥ 60%

---

## 2. 범위

### 2.1 포함

**시간표 템플릿**:
- 사전 정의된 템플릿 10개 (업무, 공부, 운동, 창작, 휴식 등)
- 템플릿 미리보기 및 커스터마이징
- 템플릿 즉시 적용 (1-Click)

**AI 기반 추천**:
- 과거 사용 패턴 분석 (최근 30일)
- 최적 시간대 추천 (요일별, 시간대별)
- 추천 이유 설명
- 추천 수락/거절/수정

**시간표 관리 고도화**:
- 시간표 복제 및 공유 (JSON Export/Import)
- 시간표 히스토리 및 성과 추적
- 시간표 A/B 테스트 (2개 시간표 비교)

**커뮤니티 기능 (선택)**:
- 커뮤니티 베스트 시간표
- 직업/상황별 추천 시간표
- 시간표 평점 및 리뷰

### 2.2 제외 (4차 고도화로 연기)

- **서버 동기화**: 시간표 클라우드 백업 및 기기 간 동기화
- **소셜 기능**: 친구와 시간표 공유, 그룹 챌린지
- **고급 AI**: 
  - 날씨, 캘린더 연동 추천
  - 실시간 컨텍스트 기반 동적 조정
  - 장기 학습 및 개인화 모델
- **웨어러블 연동**: 스마트워치에서 시간표 확인 및 제어

---

## 3. 주요 사용자 시나리오

### 시나리오 1: 템플릿 즉시 적용 (직장인 A)

```
1. 시간표 생성 화면 진입
   - "템플릿으로 시작하기" 버튼 클릭

2. 템플릿 선택
   - "직장인 업무 집중" 템플릿 선택
   - 미리보기: 오전 10시(45분), 오후 2시(30분), 오후 4시(45분)
   - "이 템플릿 사용" 버튼 클릭

3. 커스터마이징 (선택)
   - 오후 2시 시간대를 3시로 변경
   - "저장" 클릭

4. 위치 연결 (선택)
   - "회사" 위치와 연결
   - 완료!

소요 시간: 1분 30초
```

### 시나리오 2: AI 추천 수락 (학생 B)

```
1. 시간표 생성 화면 진입
   - "AI 추천받기" 버튼 클릭

2. AI 분석 진행
   - "당신의 집중 패턴을 분석 중입니다..."
   - 최근 30일 데이터 분석 (5초)

3. 추천 시간표 제시
   - "당신에게 추천하는 시간표"
   - 오전 9시(90분), 오후 2시(90분), 저녁 7시(60분)
   - 추천 이유:
     - "평일 오전에 집중도가 높아요"
     - "오후 2-4시에 자주 집중하셨어요"
     - "저녁 시간에 짧은 집중이 효과적이에요"

4. 수락 및 저장
   - "이 시간표 사용" 클릭
   - 시간표 이름 입력: "AI 공부 시간표"
   - 완료!

소요 시간: 2분
```

### 시나리오 3: 커뮤니티 시간표 가져오기

```
1. 시간표 생성 화면 진입
   - "커뮤니티 시간표 보기" 클릭

2. 인기 시간표 탐색
   - 카테고리: 업무, 공부, 운동, 창작
   - "대학생 시험 기간 집중" (⭐ 4.8, 1,234명 사용)
   - 미리보기 및 상세 설명 확인

3. 가져오기
   - "이 시간표 가져오기" 클릭
   - 자동으로 내 시간표에 추가
   - 필요 시 커스터마이징

소요 시간: 2분
```

### 시나리오 4: 시간표 성과 비교

```
1. 시간표 관리 화면
   - "업무 시간표 A" vs "업무 시간표 B" 비교

2. 성과 지표 확인
   - 지난 2주 데이터 비교
   - 완주율: A (75%) vs B (65%)
   - 총 집중 시간: A (18시간) vs B (15시간)
   - 요일별 성과 그래프

3. 최적 시간표 선택
   - "A 시간표가 더 효과적입니다"
   - "A 계속 사용" 클릭
```

---

## 4. 기능 요구사항

### 4.1 시간표 템플릿

#### 4.1.1 사전 정의 템플릿 (10개)

**1. 직장인 업무 집중** 🏢
```
- 오전 10:00 (45분, 완전 차단)
- 오후 14:00 (30분, 표준)
- 오후 16:00 (45분, 완전 차단)
- 요일: 월~금
- 설명: "업무 시간 중 3회 집중으로 생산성 극대화"
```

**2. 대학생 공부 집중** 📚
```
- 오전 09:00 (90분, 완전 차단)
- 오후 14:00 (90분, 완전 차단)
- 저녁 19:00 (60분, 표준)
- 요일: 월~금
- 설명: "수업 전후 장시간 집중으로 학습 효율 향상"
```

**3. 시험 기간 특별 집중** 📝
```
- 오전 08:00 (120분, 완전 차단)
- 오전 11:00 (90분, 완전 차단)
- 오후 14:00 (120분, 완전 차단)
- 오후 17:00 (90분, 완전 차단)
- 저녁 20:00 (90분, 표준)
- 요일: 월~일
- 설명: "시험 기간 최대 강도 집중 루틴"
```

**4. 프리랜서 유연 집중** 💼
```
- 오전 10:00 (90분, 표준)
- 오후 15:00 (90분, 표준)
- 저녁 20:00 (60분, 완화)
- 요일: 월~토
- 설명: "유연한 근무 시간에 맞춘 3회 집중"
```

**5. 운동 전후 집중** 🏋️
```
- 오전 06:00 (30분, 완화) - 운동 전 계획
- 오전 08:00 (60분, 표준) - 운동 후 집중
- 오후 18:00 (30분, 완화) - 저녁 운동 전
- 저녁 20:00 (45분, 표준) - 운동 후 정리
- 요일: 월, 수, 금
- 설명: "운동과 집중을 결합한 건강한 루틴"
```

**6. 창작자 딥워크** 🎨
```
- 오전 09:00 (120분, 완전 차단)
- 오후 14:00 (120분, 완전 차단)
- 요일: 월~금
- 설명: "방해 없는 장시간 몰입으로 창의적 작업 수행"
```

**7. 저녁 부업 집중** 🌙
```
- 저녁 20:00 (90분, 표준)
- 저녁 22:00 (60분, 표준)
- 요일: 월~금
- 설명: "퇴근 후 부업/공부를 위한 저녁 집중"
```

**8. 주말 자기계발** 📖
```
- 오전 10:00 (90분, 표준)
- 오후 14:00 (90분, 표준)
- 저녁 19:00 (60분, 완화)
- 요일: 토, 일
- 설명: "주말을 활용한 자기계발 시간"
```

**9. 단기 집중 (포모도로)** ⏰
```
- 오전 09:00 (25분, 표준)
- 오전 10:00 (25분, 표준)
- 오전 11:00 (25분, 표준)
- 오후 14:00 (25분, 표준)
- 오후 15:00 (25분, 표준)
- 오후 16:00 (25분, 표준)
- 요일: 월~금
- 설명: "포모도로 기법으로 짧고 강한 집중 반복"
```

**10. 균형 잡힌 일상** ⚖️
```
- 오전 09:00 (60분, 표준)
- 오후 14:00 (45분, 표준)
- 저녁 19:00 (30분, 완화)
- 요일: 월~금
- 설명: "업무와 휴식의 균형을 위한 적당한 집중"
```

#### 4.1.2 템플릿 선택 UI

**TemplateSelectionScreen**:
- 카테고리 필터: 업무, 공부, 운동, 창작, 기타
- 템플릿 카드:
  - 아이콘 + 이름
  - 시간대 요약 (예: "3회 집중, 총 2시간")
  - 설명 (1-2줄)
  - 사용자 수 (예: "1,234명 사용 중")
- 미리보기 버튼
- "이 템플릿 사용" 버튼

**템플릿 미리보기 다이얼로그**:
- 전체 시간대 리스트
- 타임라인 시각화
- 예상 효과 설명
- "사용" / "커스터마이징" / "취소" 버튼

#### 4.1.3 템플릿 커스터마이징

- 시간대 추가/삭제
- 시간 조정
- 요일 변경
- 차단 강도 조정
- "사용자 정의 템플릿으로 저장" 옵션

#### 4.1.4 템플릿 데이터 모델

```kotlin
data class ScheduleTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val description: String,
    val iconType: String,
    val colorHex: String,
    val timeSlots: List<TemplateTimeSlot>,
    val isDefault: Boolean,      // 기본 제공 템플릿
    val usageCount: Int = 0,     // 사용 횟수 (커뮤니티)
    val rating: Float = 0f,      // 평점 (커뮤니티)
    val authorId: String? = null // 작성자 ID (커뮤니티)
)

data class TemplateTimeSlot(
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
    val presetType: String,
    val enabledDays: List<DayOfWeek>,
    val label: String? = null
)

enum class TemplateCategory {
    WORK,        // 업무
    STUDY,       // 공부
    EXERCISE,    // 운동
    CREATIVE,    // 창작
    BALANCED,    // 균형
    INTENSIVE,   // 집중
    FLEXIBLE,    // 유연
    CUSTOM       // 사용자 정의
}
```

---

### 4.2 AI 기반 스케줄 추천

#### 4.2.1 데이터 분석 로직

**입력 데이터**:
- 최근 30일 FocusSession 기록
- 요일별 집중 패턴
- 시간대별 집중 패턴
- 완주율이 높은 시간대
- 평균 집중 시간

**분석 지표**:
```kotlin
data class UserFocusPattern(
    // 시간대별 집중도
    val hourlyFocusScore: Map<Int, Float>,      // 0-23시, 0.0-1.0
    
    // 요일별 집중도
    val dailyFocusScore: Map<DayOfWeek, Float>, // 0.0-1.0
    
    // 선호 집중 시간
    val preferredDurations: List<Int>,           // [25, 45, 60, 90, 120]분
    
    // 완주율이 높은 시간대 (Top 5)
    val bestTimeSlots: List<Pair<Int, Float>>,  // (hour, completionRate)
    
    // 평균 일일 집중 시간
    val avgDailyFocusMinutes: Int,
    
    // 집중 빈도 (일주일 평균)
    val avgWeeklySessionCount: Int,
    
    // 최적 시간대 간격
    val optimalInterval: Int                     // 시간 (hours)
)
```

**분석 알고리즘**:
```kotlin
class ScheduleRecommender {
    fun analyzePattern(sessions: List<FocusSession>): UserFocusPattern {
        // 1. 시간대별 집중도 계산
        val hourlyScore = sessions
            .groupBy { it.startTime.hour }
            .mapValues { (_, sessions) ->
                sessions.count { it.completionRate >= 0.8f } / sessions.size.toFloat()
            }
        
        // 2. 요일별 집중도 계산
        val dailyScore = sessions
            .groupBy { it.startTime.dayOfWeek }
            .mapValues { (_, sessions) ->
                sessions.averageOf { it.completionRate }
            }
        
        // 3. 선호 집중 시간 분석
        val preferredDurations = sessions
            .groupBy { it.durationMinutes }
            .mapValues { (_, sessions) -> sessions.averageOf { it.completionRate } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // 4. 최고 성과 시간대 (완주율 기준)
        val bestTimeSlots = hourlyScore
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
        
        // 5. 평균 지표 계산
        val avgDailyMinutes = sessions
            .groupBy { it.startTime.toLocalDate() }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }
            .values
            .average()
            .toInt()
        
        val avgWeeklyCount = sessions.size * 7 / 30
        
        // 6. 최적 간격 계산 (연속 세션 간 평균 시간 차이)
        val intervals = sessions
            .sortedBy { it.startTime }
            .zipWithNext { a, b -> 
                Duration.between(a.endTime, b.startTime).toHours()
            }
            .filter { it in 1..8 }  // 1-8시간 간격만 고려
            .average()
            .toInt()
        
        return UserFocusPattern(
            hourlyFocusScore = hourlyScore,
            dailyFocusScore = dailyScore,
            preferredDurations = preferredDurations,
            bestTimeSlots = bestTimeSlots,
            avgDailyFocusMinutes = avgDailyMinutes,
            avgWeeklySessionCount = avgWeeklyCount,
            optimalInterval = intervals
        )
    }
    
    fun recommendSchedule(pattern: UserFocusPattern): RecommendedSchedule {
        // 추천 로직 (다음 섹션)
    }
}
```

#### 4.2.2 추천 로직

**추천 전략**:
```kotlin
data class RecommendedSchedule(
    val name: String,
    val timeSlots: List<RecommendedTimeSlot>,
    val reasons: List<String>,
    val confidence: Float,           // 0.0-1.0
    val expectedImpact: String       // "일일 집중 시간 +30분 예상"
)

data class RecommendedTimeSlot(
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
    val presetType: String,
    val enabledDays: List<DayOfWeek>,
    val reason: String               // "평일 오전에 집중도가 높아요"
)

fun recommendSchedule(pattern: UserFocusPattern): RecommendedSchedule {
    val timeSlots = mutableListOf<RecommendedTimeSlot>()
    val reasons = mutableListOf<String>()
    
    // 1. 최고 성과 시간대 선택 (Top 3)
    val topHours = pattern.bestTimeSlots.take(3).map { it.first }
    
    // 2. 시간대 간격 조정 (최소 2시간 간격 유지)
    val adjustedHours = adjustSpacing(topHours, pattern.optimalInterval)
    
    // 3. 각 시간대에 대한 추천 생성
    adjustedHours.forEach { hour ->
        val score = pattern.hourlyFocusScore[hour] ?: 0.5f
        
        // 3-1. 집중 시간 결정 (완주율 기반)
        val duration = when {
            score >= 0.8f -> pattern.preferredDurations.firstOrNull { it >= 60 } ?: 60
            score >= 0.6f -> pattern.preferredDurations.firstOrNull { it in 30..60 } ?: 45
            else -> pattern.preferredDurations.firstOrNull { it <= 30 } ?: 25
        }
        
        // 3-2. 차단 강도 결정
        val presetType = when {
            score >= 0.8f -> "FULL_BLOCK"
            score >= 0.6f -> "STANDARD"
            else -> "RELAXED"
        }
        
        // 3-3. 요일 결정 (요일별 집중도 기반)
        val enabledDays = pattern.dailyFocusScore
            .filter { it.value >= 0.6f }
            .keys
            .toList()
        
        // 3-4. 이유 생성
        val reason = generateReason(hour, score, pattern)
        
        timeSlots.add(
            RecommendedTimeSlot(
                hour = hour,
                minute = 0,
                durationMinutes = duration,
                presetType = presetType,
                enabledDays = enabledDays,
                reason = reason
            )
        )
    }
    
    // 4. 전체 추천 이유 생성
    reasons.add("최근 30일 데이터를 분석했어요")
    if (pattern.avgWeeklySessionCount >= 10) {
        reasons.add("꾸준한 집중 습관이 있으시네요! 👍")
    }
    reasons.add("평일 ${topHours[0]}시에 집중도가 가장 높아요")
    reasons.add("${pattern.preferredDurations[0]}분 집중을 선호하시네요")
    
    // 5. 신뢰도 계산 (데이터 충분성)
    val confidence = calculateConfidence(pattern)
    
    // 6. 예상 효과 계산
    val expectedMinutes = timeSlots.sumOf { it.durationMinutes }
    val currentMinutes = pattern.avgDailyFocusMinutes
    val impact = if (expectedMinutes > currentMinutes) {
        "일일 집중 시간 +${expectedMinutes - currentMinutes}분 예상"
    } else {
        "현재 패턴 최적화"
    }
    
    return RecommendedSchedule(
        name = "AI 추천 시간표",
        timeSlots = timeSlots,
        reasons = reasons,
        confidence = confidence,
        expectedImpact = impact
    )
}

private fun generateReason(hour: Int, score: Float, pattern: UserFocusPattern): String {
    return when {
        hour < 9 -> "이른 아침에 집중하시는 편이에요"
        hour < 12 -> "오전 시간에 생산성이 높아요"
        hour < 14 -> "점심 직후 집중이 효과적이에요"
        hour < 18 -> "오후 시간대 집중도가 좋아요"
        else -> "저녁 시간에 집중을 선호하시네요"
    } + if (score >= 0.8f) " (완주율 ${(score * 100).toInt()}%)" else ""
}

private fun calculateConfidence(pattern: UserFocusPattern): Float {
    // 최소 7일, 10회 이상 세션 필요
    val sessionCount = pattern.avgWeeklySessionCount * 4  // 월간 추정
    return when {
        sessionCount >= 40 -> 0.9f  // 매우 높음
        sessionCount >= 20 -> 0.7f  // 높음
        sessionCount >= 10 -> 0.5f  // 보통
        else -> 0.3f                // 낮음
    }
}
```

#### 4.2.3 추천 UI

**AI 추천 화면**:
```
1. 분석 진행 화면
   - 로딩 애니메이션
   - "당신의 집중 패턴을 분석 중입니다..."
   - 진행률 표시

2. 추천 결과 화면
   - 헤더: "당신에게 추천하는 시간표"
   - 신뢰도 배지 (높음/보통/낮음)
   - 시간대 리스트 (카드)
     - 시간 + 기간
     - 추천 이유 (각 시간대별)
   - 전체 추천 이유 (3-5개)
   - 예상 효과
   - "이 시간표 사용" 버튼
   - "다시 추천받기" 버튼
   - "직접 만들기" 버튼

3. 데이터 부족 화면
   - "아직 데이터가 부족해요"
   - 필요 조건: 최소 7일, 10회 이상 집중 세션
   - 현재 상태 표시 (프로그레스 바)
   - "템플릿으로 시작하기" 버튼
```

#### 4.2.4 추천 개선 학습

**사용자 피드백 수집**:
```kotlin
data class RecommendationFeedback(
    val recommendationId: String,
    val accepted: Boolean,           // 수락 여부
    val modifications: List<String>, // 수정 사항 (시간, 기간 등)
    val actualUsage: Int,            // 실제 사용 횟수 (7일 내)
    val satisfaction: Float?         // 만족도 (1-5, 선택)
)

// 피드백 기반 추천 개선
fun improveRecommendation(
    pattern: UserFocusPattern,
    feedback: RecommendationFeedback
): RecommendedSchedule {
    // 거절된 시간대 가중치 감소
    // 수정된 시간대로 선호도 업데이트
    // 실제 사용률 반영
}
```

---

### 4.3 시간표 관리 고도화

#### 4.3.1 시간표 Export/Import (JSON)

**Export 형식**:
```json
{
  "version": "1.0",
  "scheduleGroup": {
    "name": "업무 시간표",
    "description": "직장인을 위한 3회 집중",
    "iconType": "WORK",
    "colorHex": "#4CAF50",
    "timeSlots": [
      {
        "hour": 10,
        "minute": 0,
        "durationMinutes": 45,
        "presetType": "FULL_BLOCK",
        "enabledDays": ["MON", "TUE", "WED", "THU", "FRI"],
        "label": "오전 집중"
      }
    ]
  },
  "metadata": {
    "createdAt": 1698000000000,
    "author": "anonymous",
    "usageCount": 0,
    "source": "template" // template, ai, manual, community
  }
}
```

**Import 로직**:
- JSON 파일 읽기 (파일 선택 또는 QR 코드 스캔)
- 스키마 검증
- 시간대 충돌 체크
- "가져오기" 또는 "미리보기 후 가져오기"

**공유 기능**:
- "공유하기" 버튼 → JSON 파일 생성
- 공유 방법:
  - 파일로 저장
  - QR 코드 생성
  - 클립보드 복사
  - 앱 간 공유 (Intent)

#### 4.3.2 시간표 히스토리 및 성과 추적

**ScheduleGroupHistory**:
```kotlin
data class ScheduleGroupHistory(
    val id: String,
    val scheduleGroupId: String,
    val activeFrom: Long,            // 활성화 시작
    val activeTo: Long?,             // 활성화 종료 (null: 현재 활성)
    val totalActiveDays: Int,        // 총 활성 일수
    val totalSessions: Int,          // 총 세션 수
    val totalFocusMinutes: Int,      // 총 집중 시간
    val avgCompletionRate: Float,    // 평균 완주율
    val successfulDays: Int          // 성공한 날 수 (완주율 >= 80%)
)

// 성과 통계
data class SchedulePerformance(
    val scheduleGroupId: String,
    val period: Period,              // WEEK, MONTH, ALL
    val completionRate: Float,
    val totalFocusMinutes: Int,
    val sessionsCount: Int,
    val bestDay: DayOfWeek,
    val worstDay: DayOfWeek,
    val trend: Trend               // IMPROVING, STABLE, DECLINING
)
```

**성과 추적 UI**:
- 시간표별 통계 카드
- 그래프:
  - 일별 완주율 (라인 그래프)
  - 요일별 성과 (막대 그래프)
  - 총 집중 시간 누적 (영역 그래프)
- 인사이트:
  - "이번 주 완주율이 10% 향상되었어요"
  - "월요일 성과가 좋아요"
  - "주말에도 시도해보시겠어요?"

#### 4.3.3 시간표 A/B 테스트

**목적**: 2개 시간표를 일정 기간 번갈아 사용하며 효과 비교

**사용 사례**:
- "아침형" vs "저녁형" 시간표
- "짧고 자주" vs "길고 드물게"
- "강한 차단" vs "완화된 차단"

**ABTestConfig**:
```kotlin
data class ABTestConfig(
    val id: String,
    val scheduleGroupA: String,
    val scheduleGroupB: String,
    val testDuration: Int,          // 일 수 (기본 14일)
    val switchStrategy: SwitchStrategy,
    val startDate: LocalDate,
    val endDate: LocalDate?
)

enum class SwitchStrategy {
    DAILY,       // 매일 번갈아
    WEEKLY,      // 주별 번갈아
    BI_WEEKLY    // 2주씩
}

data class ABTestResult(
    val winner: String,              // scheduleGroupId
    val scheduleAScore: Float,
    val scheduleBScore: Float,
    val metrics: Map<String, Float>, // completionRate, focusTime, etc.
    val recommendation: String
)
```

**A/B 테스트 UI**:
- 테스트 설정 화면
  - 두 시간표 선택
  - 테스트 기간 선택
  - 전환 전략 선택
- 진행 중 화면
  - 현재 사용 중인 시간표 표시
  - 남은 기간
  - 중간 성과 미리보기
- 결과 화면
  - 승자 선언 🏆
  - 상세 비교 (완주율, 집중 시간, 만족도)
  - "승자 계속 사용" / "패자도 유지" 버튼

---

### 4.4 커뮤니티 기능 (선택)

#### 4.4.1 커뮤니티 시간표 공유

**요구사항**:
- 서버 없이 동작 (Firebase Firestore 또는 유사)
- 익명 공유 지원
- 신고 및 필터링 시스템

**CommunitySchedule**:
```kotlin
data class CommunitySchedule(
    val id: String,
    val scheduleData: String,        // JSON
    val name: String,
    val category: TemplateCategory,
    val description: String,
    val authorId: String,            // 익명 ID
    val createdAt: Long,
    val usageCount: Int,
    val rating: Float,
    val reviewCount: Int,
    val tags: List<String>           // ["직장인", "오전", "집중"]
)

data class CommunityReview(
    val id: String,
    val scheduleId: String,
    val authorId: String,
    val rating: Float,               // 1-5
    val comment: String?,
    val helpful: Int,                // 도움됨 수
    val createdAt: Long
)
```

#### 4.4.2 커뮤니티 시간표 탐색

**탐색 화면**:
- 탭: 인기순, 최신순, 평점순
- 필터: 카테고리, 시간대 수, 총 집중 시간
- 검색: 이름, 태그
- 시간표 카드:
  - 이름 + 설명
  - 평점 ⭐ + 사용자 수
  - 미리보기 (간단한 타임라인)
  - "가져오기" 버튼

**시간표 상세 화면**:
- 전체 시간대 리스트
- 타임라인 시각화
- 통계:
  - 총 사용자 수
  - 평균 평점
  - 리뷰 수
- 리뷰 리스트 (최신 5개)
- "이 시간표 가져오기" 버튼
- "공유하기" 버튼

#### 4.4.3 베스트 시간표 큐레이션

**주간 베스트**:
- 매주 월요일 업데이트
- 가장 많이 사용된 시간표 Top 10
- 카테고리별 Best 1

**직업/상황별 추천**:
- 직업: 개발자, 디자이너, 학생, 프리랜서, 주부 등
- 상황: 시험 기간, 프로젝트 마감, 다이어트, 자격증 준비 등

---

## 5. 데이터 모델

### 5.1 신규 엔티티

```kotlin
// 템플릿
@Entity(tableName = "schedule_template")
data class ScheduleTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,            // WORK, STUDY, etc.
    val description: String,
    val iconType: String,
    val colorHex: String,
    val timeSlotsJson: String,       // JSON 직렬화
    val isDefault: Boolean,
    val usageCount: Int = 0,
    val rating: Float = 0f,
    val authorId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// AI 추천 기록
@Entity(tableName = "ai_recommendation")
data class AIRecommendation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val patternJson: String,         // UserFocusPattern JSON
    val recommendedScheduleJson: String,
    val confidence: Float,
    val accepted: Boolean = false,
    val modificationsJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null
)

// 시간표 히스토리
@Entity(tableName = "schedule_group_history")
data class ScheduleGroupHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scheduleGroupId: String,
    val activeFrom: Long,
    val activeTo: Long? = null,
    val totalActiveDays: Int = 0,
    val totalSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val avgCompletionRate: Float = 0f,
    val successfulDays: Int = 0
)

// A/B 테스트
@Entity(tableName = "ab_test_config")
data class ABTestConfig(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scheduleGroupAId: String,
    val scheduleGroupBId: String,
    val testDurationDays: Int,
    val switchStrategy: String,
    val startDate: Long,
    val endDate: Long? = null,
    val isCompleted: Boolean = false,
    val winnerId: String? = null
)
```

### 5.2 DB 마이그레이션 (v5 → v6)

```sql
-- schedule_template 테이블
CREATE TABLE IF NOT EXISTS schedule_template (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    iconType TEXT NOT NULL,
    colorHex TEXT NOT NULL,
    timeSlotsJson TEXT NOT NULL,
    isDefault INTEGER NOT NULL DEFAULT 0,
    usageCount INTEGER NOT NULL DEFAULT 0,
    rating REAL NOT NULL DEFAULT 0,
    authorId TEXT,
    createdAt INTEGER NOT NULL
);

CREATE INDEX idx_template_category ON schedule_template(category);
CREATE INDEX idx_template_usage ON schedule_template(usageCount DESC);
CREATE INDEX idx_template_rating ON schedule_template(rating DESC);

-- ai_recommendation 테이블
CREATE TABLE IF NOT EXISTS ai_recommendation (
    id TEXT PRIMARY KEY NOT NULL,
    patternJson TEXT NOT NULL,
    recommendedScheduleJson TEXT NOT NULL,
    confidence REAL NOT NULL,
    accepted INTEGER NOT NULL DEFAULT 0,
    modificationsJson TEXT,
    createdAt INTEGER NOT NULL,
    acceptedAt INTEGER
);

CREATE INDEX idx_recommendation_accepted ON ai_recommendation(accepted);
CREATE INDEX idx_recommendation_created ON ai_recommendation(createdAt DESC);

-- schedule_group_history 테이블
CREATE TABLE IF NOT EXISTS schedule_group_history (
    id TEXT PRIMARY KEY NOT NULL,
    scheduleGroupId TEXT NOT NULL,
    activeFrom INTEGER NOT NULL,
    activeTo INTEGER,
    totalActiveDays INTEGER NOT NULL DEFAULT 0,
    totalSessions INTEGER NOT NULL DEFAULT 0,
    totalFocusMinutes INTEGER NOT NULL DEFAULT 0,
    avgCompletionRate REAL NOT NULL DEFAULT 0,
    successfulDays INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (scheduleGroupId) REFERENCES schedule_group(id) ON DELETE CASCADE
);

CREATE INDEX idx_history_schedule ON schedule_group_history(scheduleGroupId);

-- ab_test_config 테이블
CREATE TABLE IF NOT EXISTS ab_test_config (
    id TEXT PRIMARY KEY NOT NULL,
    scheduleGroupAId TEXT NOT NULL,
    scheduleGroupBId TEXT NOT NULL,
    testDurationDays INTEGER NOT NULL,
    switchStrategy TEXT NOT NULL,
    startDate INTEGER NOT NULL,
    endDate INTEGER,
    isCompleted INTEGER NOT NULL DEFAULT 0,
    winnerId TEXT,
    FOREIGN KEY (scheduleGroupAId) REFERENCES schedule_group(id) ON DELETE CASCADE,
    FOREIGN KEY (scheduleGroupBId) REFERENCES schedule_group(id) ON DELETE CASCADE
);

-- 기본 템플릿 삽입
INSERT INTO schedule_template (id, name, category, description, iconType, colorHex, timeSlotsJson, isDefault, createdAt)
VALUES 
    ('template_work_focus', '직장인 업무 집중', 'WORK', '업무 시간 중 3회 집중으로 생산성 극대화', 'WORK', '#4CAF50', '[...]', 1, 1698000000000),
    ('template_student_study', '대학생 공부 집중', 'STUDY', '수업 전후 장시간 집중으로 학습 효율 향상', 'STUDY', '#2196F3', '[...]', 1, 1698000000000),
    -- ... 나머지 8개 템플릿
;
```

---

## 6. 기술 스택

- **AI/ML**: On-device 분석 (Kotlin 로직, TensorFlow Lite 미사용)
- **데이터 시각화**: MPAndroidChart 또는 Compose Canvas
- **JSON 처리**: Kotlinx Serialization
- **QR 코드**: ZXing 라이브러리
- **커뮤니티 (선택)**: Firebase Firestore (Serverless)

---

## 7. 성공 지표 (상세)

### 7.1 기능 지표
- 템플릿 사용률 ≥ 70%
- AI 추천 수락률 ≥ 40%
- AI 추천 신뢰도 평균 ≥ 0.7
- 시간표 공유율 ≥ 10%
- A/B 테스트 사용률 ≥ 5%

### 7.2 사용자 경험
- 시간표 설정 완료율 ≥ 80%
- 시간표 활용률 ≥ 60% (7일 내)
- AI 추천 만족도 ≥ 75%
- 템플릿 만족도 ≥ 80%

### 7.3 비즈니스 지표
- 시간표 기능 사용자 +100% (v0.7 대비)
- WAU +30%
- 7일 리텐션 ≥ 60%
- 평균 일일 집중 시간 +20%

---

## 8. 위험 요소 및 대응

### 8.1 AI 추천 정확도 부족
**위험**: 데이터 부족 또는 알고리즘 한계로 부정확한 추천
**대응**:
- 최소 데이터 요구사항 명확히 (7일, 10회)
- 신뢰도 표시 및 "다시 추천받기" 제공
- 템플릿 대안 항상 제공

### 8.2 템플릿 부적합
**위험**: 제공된 템플릿이 사용자 니즈에 맞지 않음
**대응**:
- 다양한 카테고리 및 시나리오 커버 (10개 이상)
- 커스터마이징 기능 강화
- 사용자 피드백 수집 및 템플릿 개선

### 8.3 커뮤니티 스팸/저품질 콘텐츠
**위험**: 커뮤니티 시간표에 스팸 또는 저품질 콘텐츠
**대응**:
- 신고 시스템
- 평점 및 리뷰 필터링
- 관리자 큐레이션

### 8.4 개인정보 보호
**위험**: AI 분석 시 민감한 패턴 노출
**대응**:
- 모든 분석 On-device
- 서버 전송 없음 (커뮤니티 제외)
- 익명 공유만 지원

---

## 9. 일정 및 마일스톤

### Week 1 (Day 1-7): 데이터 모델 및 템플릿
- DB 마이그레이션 v5→v6
- 템플릿 데이터 모델 및 DAO
- 10개 기본 템플릿 정의
- 템플릿 선택 UI

### Week 2 (Day 8-14): AI 추천 로직
- FocusSession 분석 로직
- 패턴 분석 알고리즘
- 추천 생성 로직
- AI 추천 UI

### Week 3 (Day 15-21): 고급 기능 및 QA
- 시간표 Export/Import
- 히스토리 및 성과 추적
- A/B 테스트 (선택)
- 커뮤니티 기능 (선택)
- 통합 테스트 및 QA
- 문서화 및 배포 준비

---

## 10. 참조 문서

- [2.5차 고도화 PRD](./02_advanced_autosetting_prd.md)
- [2.5차 고도화 작업계획](./02.5_complex_time&location_todolist.md)
- [2차 고도화 PRD](./02_advanced_autosetting_prd.md)
- [2차 고도화 작업계획](./02_advanced_autosetting_todolist.md)

---

> **4차 고도화 철학**: "지능형 자동화". AI가 사용자의 패턴을 학습하여 최적의 시간표를 추천하고, 템플릿을 통해 즉시 시작할 수 있는 경험을 제공합니다. "생각 없이 시작, 지능적으로 최적화"를 목표로 합니다.

