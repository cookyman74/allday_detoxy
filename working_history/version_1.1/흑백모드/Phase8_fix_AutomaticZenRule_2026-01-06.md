# Grayscale Mode Phase 8 - AutomaticZenRule 버그 수정

**작업일**: 2026-01-06  
**버전**: v1.1  
**Phase**: Phase 8 (버그 수정)

---

## 📋 문제 요약

Phase 1-7 구현 완료 후 실기기 테스트에서 **흑백 모드가 활성화되지 않는 문제** 발견.

| 증상 | 상세 |
|------|------|
| 토글 ON | ✅ 정상 저장됨 |
| 타이머 시작 | ✅ 로그에 "Grayscale mode enabled" 출력 |
| 실제 화면 | ❌ **흑백으로 변하지 않음** |
| 타이머 배너 | ❌ 표시되지 않음 (Android 15에서는 의도된 동작) |

---

## 🔍 디버깅 과정

### 1단계: 로그 확인

```bash
adb logcat -d | grep -iE "(Grayscale|enableGrayscale)" | tail -40
```

**첫 번째 오류 발견**:
```
01-06 10:43:53.329 E GrayscaleManager: ❌ Failed to enable grayscale: 
    ZenPolicy is only applicable to INTERRUPTION_FILTER_PRIORITY filters
    java.lang.IllegalArgumentException
```

### 2단계: 첫 번째 수정 (ZenPolicy 제거)

**원인**: `ZenPolicy`는 `INTERRUPTION_FILTER_PRIORITY`에서만 사용 가능
- 우리는 `INTERRUPTION_FILTER_ALL`을 사용 (알림 차단 없음)
- 두 가지를 함께 사용하면 `IllegalArgumentException` 발생

**수정**:
```kotlin
// 수정 전
val policy = ZenPolicy.Builder()
    .allowAllSounds()
    .build()

val rule = AutomaticZenRule.Builder(RULE_NAME, Uri.EMPTY)
    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    .setZenPolicy(policy)  // ❌ FILTER_ALL과 함께 사용 불가
    ...

// 수정 후
val rule = AutomaticZenRule.Builder(RULE_NAME, Uri.EMPTY)
    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    // .setZenPolicy(policy)  // 제거
    .setDeviceEffects(effects)  // DeviceEffects만 사용
    ...
```

---

### 3단계: 재테스트 및 두 번째 오류 발견

수정 후에도 흑백 모드 미작동. 로그 확인:

```
01-06 10:50:19.112 D GrayscaleManager: Rule exists, isEnabled=false
```

**`isEnabled=false`** - 룰이 비활성화 상태!

### 4단계: 시스템 덤프 분석

```bash
adb shell dumpsys notification policy | grep -iA 5 "zen"
```

**핵심 발견**:
```
ZenRule[id=2008e6df12e04ab095761e033fa7a304,
  state=STATE_FALSE,    ← ⚠️ FALSE!
  enabled=TRUE,
  zenMode=ZEN_MODE_OFF, ← ⚠️ OFF!
  conditionId=,         ← ⚠️ 빈 문자열!
  condition=null,       ← ⚠️ NULL!
  deviceEffects=[grayscale]
]
```

**문제**: 
- `conditionId`가 비어있음 (`Uri.EMPTY` 사용했기 때문)
- `condition=null`이므로 시스템이 룰을 활성화할 수 없음

### 5단계: 두 번째 수정 (setAutomaticZenRuleState 추가)

API 문서 조사 결과:
> `addAutomaticZenRule`만으로는 룰이 활성화되지 않음
> `setAutomaticZenRuleState()`를 호출하여 조건 충족을 시스템에 알려야 함

**수정**:
```kotlin
// 룰 생성 후 명시적 활성화
val condition = android.service.notification.Condition(
    Uri.parse("condition://com.allday.detoxy/grayscale"),
    "Grayscale Active",
    Condition.STATE_TRUE
)
nm.setAutomaticZenRuleState(resultRuleId, condition)
```

---

### 6단계: 재테스트 및 세 번째 오류 발견

수정 후에도 여전히 미작동. 시스템 덤프 재확인:

```
ZenRule[...
  state=STATE_FALSE,    ← 여전히 FALSE!
  conditionId=,         ← 여전히 빈 문자열!
  condition=null,       ← 여전히 NULL!
]
```

**원인 분석**:
- `setAutomaticZenRuleState()`의 Uri와 룰의 `conditionId`가 **일치하지 않음**
- 룰 생성 시 `Uri.EMPTY`를 사용했으나, 상태 설정 시 `condition://com.allday.detoxy/grayscale` 사용

### 7단계: 최종 수정 (conditionId 일치화)

**핵심 수정**:
1. `AutomaticZenRule.Builder`의 두 번째 인자(conditionId)를 `Uri.EMPTY` → `condition://com.allday.detoxy/grayscale`
2. `TYPE_SCHEDULE_TIME` → `TYPE_OTHER` (앱에서 직접 제어하는 룰)

```kotlin
// 최종 수정
val conditionId = Uri.parse("condition://com.allday.detoxy/grayscale")

val rule = AutomaticZenRule.Builder(RULE_NAME, conditionId)  // conditionId 명시
    .setType(AutomaticZenRule.TYPE_OTHER)  // TYPE_OTHER 사용
    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    .setDeviceEffects(effects)
    .setConfigurationActivity(...)
    .setEnabled(true)
    .build()

// 룰 생성/업데이트 후 명시적 활성화
nm.addAutomaticZenRule(rule)

val condition = Condition(
    conditionId,  // 동일한 Uri 사용
    "Grayscale Active",
    Condition.STATE_TRUE
)
nm.setAutomaticZenRuleState(resultRuleId, condition)
```

---

## ✅ 최종 해결

### 변경된 파일

| 파일 | 변경 내용 |
|------|----------|
| `GrayscaleManager.kt` | conditionId 설정, TYPE_OTHER 사용, setAutomaticZenRuleState 추가 |

### 핵심 학습 포인트

1. **ZenPolicy vs ZenDeviceEffects**
   - `ZenPolicy`는 `INTERRUPTION_FILTER_PRIORITY`에서만 사용 가능
   - `ZenDeviceEffects`는 모든 필터와 함께 사용 가능

2. **AutomaticZenRule 활성화 조건**
   - `addAutomaticZenRule()`만으로는 불충분
   - `setAutomaticZenRuleState()`를 호출해야 실제 활성화
   - **conditionId가 일치해야 함**

3. **TYPE_SCHEDULE_TIME vs TYPE_OTHER**
   - `TYPE_SCHEDULE_TIME`: 시스템이 스케줄 기반으로 제어
   - `TYPE_OTHER`: 앱에서 직접 `setAutomaticZenRuleState()`로 제어

4. **시스템 덤프 활용**
   ```bash
   adb shell dumpsys notification policy | grep -iA 20 "ZenRule"
   ```
   - `state`, `conditionId`, `condition` 필드를 확인하여 문제 진단

---

## 🧪 검증

### 테스트 환경
- 기기: USB 연결 실기기
- Android 버전: API 35 (Android 15)

### 테스트 결과
| 항목 | 결과 |
|------|------|
| 흑백 모드 토글 ON | ✅ 정상 저장 |
| 타이머 시작 시 흑백 전환 | ✅ **작동함** |
| 타이머 종료 시 컬러 복원 | ✅ 작동함 |
| 타이머 배너 (Android 15) | ✅ 숨김 (의도된 동작) |

---

## 📊 버그 수정 타임라인

| 시간 | 작업 | 결과 |
|------|------|------|
| 10:34 | 첫 테스트 - 흑백 미작동 확인 | ❌ |
| 10:38 | 로그 분석 - ZenPolicy 오류 발견 | 🔍 |
| 10:44 | ZenPolicy 제거 후 재테스트 | ❌ 여전히 미작동 |
| 10:52 | 로그 분석 - isEnabled=false 발견 | 🔍 |
| 10:55 | setAutomaticZenRuleState 추가 | ❌ 여전히 미작동 |
| 10:59 | 시스템 덤프 분석 - conditionId 불일치 발견 | 🔍 |
| 11:02 | conditionId 일치화 + TYPE_OTHER 적용 | ✅ **성공** |

---

## 📝 향후 참고사항

1. **Android 15 AutomaticZenRule 사용 시**:
   - conditionId를 명시적으로 설정
   - setAutomaticZenRuleState()로 활성화
   - 시스템 덤프로 실제 상태 확인

2. **디버깅 명령어**:
   ```bash
   # 룰 상태 확인
   adb shell dumpsys notification policy | grep -iA 20 "ZenRule"
   
   # 앱 로그 확인
   adb logcat -d | grep -iE "GrayscaleManager"
   ```

---

## 🔧 추가 수정: 연속 스케줄 흑백 모드 문제 (2026-01-06 17:20)

### 증상
- 스케줄 1 시작 → 흑백 모드 ON ✅
- 스케줄 1 종료 → 흑백 모드 OFF ✅
- 스케줄 2 시작 (폰 꺼진 상태) → **흑백 모드가 켜지지 않음** ❌

### 원인 분석
`disableGrayscaleInternal()`에서 룰을 `setEnabled(false)`로만 비활성화하고, **`setAutomaticZenRuleState()`로 조건 상태를 리셋하지 않았음**.

시스템은 `Condition.STATE_TRUE`가 설정된 상태로 남아있어서, 두 번째 스케줄에서 `setAutomaticZenRuleState(STATE_TRUE)`를 호출해도 시스템이 "이미 활성화됨"으로 판단.

### 해결책
`disableGrayscaleInternal()`에 `setAutomaticZenRuleState(STATE_FALSE)` 호출 추가:

```kotlin
ruleId?.let { id ->
    // ⚠️ 핵심 수정: 먼저 setAutomaticZenRuleState로 조건을 비활성화
    val conditionId = Uri.parse("condition://com.allday.detoxy/grayscale")
    val condition = android.service.notification.Condition(
        conditionId,
        "Grayscale Inactive",
        android.service.notification.Condition.STATE_FALSE
    )
    nm.setAutomaticZenRuleState(id, condition)
    Log.d(TAG, "Rule state set to inactive: $id")
    
    // 룰 비활성화 (삭제하지 않고 비활성화)
    val existingRules = nm.automaticZenRules
    existingRules[id]?.let { existingRule ->
        val disabledRule = AutomaticZenRule.Builder(existingRule)
            .setEnabled(false)
            .build()
        nm.updateAutomaticZenRule(id, disabledRule)
    }
}
```

### 핵심 포인트
- `enable` 시: `setAutomaticZenRuleState(STATE_TRUE)` 호출
- `disable` 시: `setAutomaticZenRuleState(STATE_FALSE)` 호출
- **조건 상태의 대칭적 관리**가 필수
