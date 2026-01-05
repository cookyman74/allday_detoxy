# 📱 흑백 모드 자동 전환 PRD (v1.3)

> ⚠️ **v1.3 업데이트**: 코드 리뷰 반영 - 실제 클래스명 수정, 아키텍처 경로 수정, 권한 UX 정책 명확화
> ⚠️ **v1.2 업데이트**: 코드 리뷰 반영 - 권한 UX 정책 명확화, 기본값 OFF로 변경, 적용 범위 명확화, DND 연동 방안 추가
> ⚠️ **v1.1 업데이트**: 기술 리뷰 반영 - API 메서드명 수정, AutomaticZenRule 기반 설계 전환, 권한 설명 수정

> 📍 **목적**: 집중 모드 활성화 시 화면을 흑백(그레이스케일)으로 전환하여 스마트폰 사용 욕구 감소
> ⚠️ **핵심**: Android 15+ `ZenDeviceEffects API` 활용, 하위 버전은 시스템 설정 안내

---

## 0️⃣ 기능 개요

### 문제 정의
- 스마트폰의 **화려한 컬러 UI**는 사용자의 주의를 끌어 집중력을 방해
- 기존 집중 모드는 앱 차단만 수행, **시각적 자극 감소** 기능 부재

### 해결책
```
스케줄 시작 (집중 모드 ON)
       ↓
화면 흑백 모드 자동 활성화
       ↓
스케줄 종료 (집중 모드 OFF)
       ↓
컬러 모드 자동 복귀
```

### 기대 효과
- 📉 스마트폰 사용 욕구 **30~50% 감소** (연구 기반)
- 🎯 집중 모드의 **효과 극대화**
- 🧠 사용자에게 **시각적 피드백** 제공 (집중 모드 활성 상태 인지)

---

## 1️⃣ Android 버전별 구현 전략

| Android 버전 | 구현 방식 | 사용자 경험 |
|-------------|----------|------------|
| **Android 15+** | `ZenDeviceEffects API` | ✅ 완전 자동화 |
| **Android 14 이하** | 시스템 설정 안내 | ⚠️ 반자동 (2탭 필요) |

---

## 2️⃣ Android 15+ 구현 (AutomaticZenRule + ZenDeviceEffects)

### 2-1. API 개요

Android 15(API 35)에서 Google이 공개한 새로운 API. **AutomaticZenRule**에 **ZenDeviceEffects**를 연결하여 그레이스케일 효과 적용.

| 기능 | API 메서드 | 설명 |
|------|---------|------|
| **그레이스케일** | `setShouldDisplayGrayscale(true)` | 화면 흑백 전환 |
| AOD 비활성화 | `setShouldSuppressAmbientDisplay(true)` | Always On Display 끄기 |
| 배경화면 디밍 | `setShouldDimWallpaper(true)` | 월페이퍼 어둡게 |

> ⚠️ **중요**: 기존 `setShouldMinimizeColorSaturation`이 아닌 **`setShouldDisplayGrayscale`** 사용

### 2-2. 핵심 아키텍처

```
┌───────────────────────────────────────────────┐
│          AutomaticZenRule                        │
│  - 앱이 소유하는 자동화 규칙                       │
│  - 스케줄과 1:1 또는 앱 단일 룰로 관리                │
├───────────────────────────────────────────────┤
│          ZenDeviceEffects                         │
│  - 그레이스케일, AOD 비활성화, 배경 디밍 등           │
│  - 룰 활성화 시 적용되는 디바이스 효과                │
├───────────────────────────────────────────────┤
│          ZenPolicy                                 │
│  - INTERRUPTION_FILTER_ALL: 알림 차단 없이 효과만      │
│  - INTERRUPTION_FILTER_PRIORITY: 우선 알림만 허용    │
└───────────────────────────────────────────────┘
```

### 2-3. 룰 관리 전략

| 전략 | 설명 | 장단점 |
|------|------|--------|
| **앱 단일 룰** | 앱 전체에서 1개의 룰만 사용 | 단순, 충돌 없음 |
| 스케줄별 룰 | 각 스케줄마다 별도 룰 생성 | 세밀하지만 복잡 |

> ✅ **권장**: **앱 단일 룰** 전략 채택 (단순성 우선)

### 2-4. 핵심 구현 코드

> ⚠️ **v1.3 아키텍처 결정**: Clean Architecture 준수를 위해 `core/manager/` 경로에 배치
> - `GrayscaleManager`는 Android 프레임워크(`NotificationManager`)에 의존
> - `domain` 레이어는 프레임워크 독립적이어야 함
> - 기존 `DndManager`와 동일한 레이어에 배치

```kotlin
// 파일 경로: core/manager/GrayscaleManager.kt
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM) // Android 15
object GrayscaleManager {
    
    private const val RULE_ID_KEY = "grayscale_zen_rule_id"
    private var ruleId: String? = null
    
    /**
     * 그레이스케일 룰 생성 또는 활성화
     */
    fun enableGrayscale(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        
        // 1) 권한 확인
        if (!nm.isNotificationPolicyAccessGranted) {
            return false
        }
        
        // 2) 사용자 관리 모드 확인
        if (nm.areAutomaticZenRulesUserManaged()) {
            // 시스템 UI로 위임 필요 - 룰 생성 불가
            return false
        }
        
        // 3) ZenDeviceEffects 생성 - 그레이스케일만 적용
        val effects = ZenDeviceEffects.Builder()
            .setShouldDisplayGrayscale(true)  // ✅ 올바른 메서드명
            .build()
        
        // 4) ZenPolicy - 알림 차단 없이 효과만 적용
        val policy = ZenPolicy.Builder()
            .allowAllSounds()  // 모든 알림음 허용
            .build()
        
        // 5) AutomaticZenRule 생성
        val rule = AutomaticZenRule.Builder("AllDay Detoxy 집중 모드", Uri.EMPTY)
            .setType(AutomaticZenRule.TYPE_SCHEDULE)  // 스케줄 타입
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)  // DND 없음
            .setZenPolicy(policy)
            .setDeviceEffects(effects)
            .setConfigurationActivity(  // 필수: 설정 화면 지정
                ComponentName(context, GrayscaleSettingsActivity::class.java)
            )
            .setEnabled(true)
            .build()
        
        // 6) 룰 추가 또는 업데이트
        ruleId = if (ruleId != null) {
            nm.updateAutomaticZenRule(ruleId!!, rule)
            ruleId
        } else {
            nm.addAutomaticZenRule(rule)
        }
        
        // 7) 룰 ID 저장 (앱 재시작 시 복원용)
        saveRuleId(context, ruleId)
        
        return true
    }
    
    /**
     * 그레이스케일 룰 비활성화
     */
    fun disableGrayscale(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        
        ruleId?.let { id ->
            try {
                // 룰 삭제 또는 비활성화
                nm.removeAutomaticZenRule(id)
                ruleId = null
                clearRuleId(context)
            } catch (e: Exception) {
                // 룰이 이미 삭제되었거나 권한 거부
            }
        }
    }
    
    /**
     * 앱 시작 시 룰 ID 복원
     */
    fun restoreRuleId(context: Context) {
        ruleId = getSavedRuleId(context)
    }
    
    private fun saveRuleId(context: Context, id: String?) { /* DataStore 저장 */ }
    private fun getSavedRuleId(context: Context): String? { /* DataStore 조회 */ }
    private fun clearRuleId(context: Context) { /* DataStore 삭제 */ }
}
```

### 2-5. 필요 권한

```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
```

> ⚠️ **중요**: 이 권한은 **일반 권한이 아님**!
> - **특수 접근 권한**으로, 사용자가 설정에서 직접 허용해야 함
> - `isNotificationPolicyAccessGranted()` 체크 필수
> - 허용 없이 호출 시 `SecurityException` 발생

### 2-6. 권한 승인 플로우

```kotlin
fun checkAndRequestPermission(context: Context): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java)
    
    if (!nm.isNotificationPolicyAccessGranted) {
        // 설정 화면으로 이동
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        context.startActivity(intent)
        return false
    }
    
    return true
}

// 설정에서 돌아온 후 재확인 필수
fun onResumeAfterSettings(context: Context): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java)
    return nm.isNotificationPolicyAccessGranted
}
```

### 2-7. 사용자 관리 모드 대응

```kotlin
// areAutomaticZenRulesUserManaged() == true 인 경우
// 앱이 룰을 직접 생성할 수 없음 - 시스템 UI로 위임

fun handleUserManagedMode(context: Context) {
    // 사용자에게 안내 메시지 표시
    showUserManagedWarning(
        message = "현재 기기에서는 흑백 모드 자동 전환을 " +
                  "시스템 설정에서 직접 관리해야 합니다."
    )
}
```

---

## 3️⃣ Android 14 이하 구현 (Fallback)

### 3-1. 전략: 설정 페이지 사전 경고

> ⚠️ **설계 근거**: 스케줄은 대부분 자동 실행되므로, 시작 시점에 사용자가 스마트폰을 보고 있지 않을 가능성이 높음.
> 따라서 알림이 아닌 **설정 페이지/집중 모드 페이지에 사전 경고 메시지** 표시

### 3-2. 경고 메시지 UI (설정 페이지)

```
┌──────────────────────────────────┐
│  ⚙️ 설정                          │
├──────────────────────────────────┤
│  📱 집중 모드 설정                │
│  ─────────────────────────────── │
│  🌑 흑백 모드 사용          [ON] │
│                                  │
│  ┌────────────────────────────┐ │
│  │ ⚠️ 현재 기기에서는 흑백     │ │  ← Android 14 이하에만 표시
│  │ 모드 자동 전환이 지원되지  │ │
│  │ 않습니다.                  │ │
│  │                            │ │
│  │ 집중 모드 시작 전에 아래   │ │
│  │ 버튼을 눌러 직접 설정해     │ │
│  │ 주세요.                   │ │
│  │                            │ │
│  │   [흑백 모드 설정 열기]     │ │  ← 시스템 설정으로 이동
│  └────────────────────────────┘ │
│                                  │
└──────────────────────────────────┘
```

### 3-3. 경고 메시지 UI (집중 모드/타이머 페이지)

```
┌──────────────────────────────────┐
│  ⏱️ 집중 타이머                    │
├──────────────────────────────────┤
│                                  │
│  ┌────────────────────────────┐ │
│  │ 💡 흑백 모드를 사용하면     │ │  ← Android 14 이하에만 표시
│  │ 집중력이 향상됩니다!        │ │
│  │ [지금 설정하기]             │ │
│  └────────────────────────────┘ │
│                                  │
│        ┌────────────┐            │
│        │  25:00   │            │
│        └────────────┘            │
│          [시작]                  │
└──────────────────────────────────┘
```

### 3-4. 버전 확인 및 경고 표시 코드

```kotlin
@Composable
fun GrayscaleWarningBanner(
    onOpenSettings: () -> Unit
) {
    // Android 15 이상이면 표시하지 않음
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.warningContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚠️ 현재 기기에서는 흑백 모드 자동 전환이 지원되지 않습니다.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "집중 모드 시작 전에 직접 설정해 주세요.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onOpenSettings) {
                Text("흑백 모드 설정 열기")
            }
        }
    }
}
```

### 3-5. 시스템 설정 열기

```kotlin
fun openColorCorrectionSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_COLOR_CORRECTION_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 지원하지 않는 기기는 일반 접근성 설정으로
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
```

### 3-6. 표시 위치

| 화면 | 표시 조건 | 메시지 타입 |
|------|----------|------------|
| **설정 페이지** | Android 14 이하 + 흑백모드 ON | 경고 배너 |
| **집중 타이머 페이지** | Android 14 이하 + 흑백모드 ON + 최초 1회 | 추천 배너 |

> 💡 **UX 원칙**: 집중 타이머 페이지의 배너는 "다시 보지 않기" 옵션 제공

---

## 4️⃣ 앱 설정 (글로벌 설정)

### 4-1. 설정 페이지 UI

```
┌──────────────────────────────────┐
│  ⚙️ 설정                          │
├──────────────────────────────────┤
│                                  │
│  📱 집중 모드 설정                │
│  ─────────────────────────────── │
│  🌑 흑백 모드 사용         [OFF] │  ← 글로벌 설정 (기본값: OFF)
│     집중 모드 시작 시 화면을      │
│     흑백으로 전환합니다           │
│                                  │
│  [⚠️ 경고 배너 - Android 14 이하시] │  ← 3-2 참조
│                                  │
└──────────────────────────────────┘
```

### 4-2. 설정 데이터 모델

```kotlin
// AppSettings (DataStore)
data class AppSettings(
    // ... 기존 필드
    val grayscaleModeEnabled: Boolean = false  // 기본값: 미사용(OFF) - v1.2 변경
)
```

### 4-3. 동작 규칙

| 글로벌 설정 | 동작 |
|------------|------|
| **ON** | 모든 집중 모드 시작 시 흑백 모드 활성화 |
| **OFF (기본값)** | 흑백 모드 전환하지 않음 |

> ⚠️ **v1.2 변경**: 기본값을 **OFF**로 변경
> - **이유**: DND 권한이 현재 앱에서 "선택 권한"으로 분류되어 있음
> - 기본값 ON 시 "권한 없이 동작 안 함" 혼란 발생
> - 사용자가 명시적으로 활성화해야 권한 요청 플로우 진행
>
> ⚠️ **설계 결정**: 스케줄별 개별 설정 대신 **글로벌 설정**으로 단순화
> - Room 마이그레이션/DAO/UI 변경 비용 최소화
> - 사용자 혼란 최소화

---

## 5️⃣ 스케줄 연동 설계

### 5-1. 적용 범위

> ✅ **결정**: 흑백 모드는 **모든 집중 모드 트리거에 동일하게** 적용

| 트리거 유형 | 흑백 모드 적용 | 비고 |
|------------|--------------|------|
| 수동 타이머 시작 | ✅ 적용 | `FocusTimerService.onFocusStart()` |
| 시간 기반 자동 실행 | ✅ 적용 | `AutoStartTimerWorker` |
| 위치 기반 자동 실행 | ✅ 적용 | `GeofenceReceiver` |
| 스누즈 | ✅ 적용 | 이미 진행 중이면 상태 유지 |

> ⚠️ **주의**: 스케줄별 개별 흑백 모드 설정은 **지원하지 않음** (글로벌 설정만)

### 5-2. 흐름도

```
┌─────────────────────────────────────────────────┐
│                스케줄 시작 트리거                │
│    (시간 기반 / 위치 기반 / 수동)                │
└─────────────────────────────────────────────────┘
                        │
                        ▼
             ┌─────────────────────────┐
             │ 글로벌 설정             │
             │ grayscaleModeEnabled?   │
             └─────────────────────────┘
                   │         │
                  YES        NO
              (기본값)        │
                   │         │
                   ▼         └──────────────────┐
        ┌─────────────────────┐                 │
        │ Android 15+?        │                 │
        └─────────────────────┘                 │
              │         │                       │
             YES        NO                      │
              │         │                       │
              ▼         ▼                       │
    ┌────────────┐ ┌────────────────┐          │
    │ ZenDevice  │ │ (추가 동작 없음)│          │
    │ Effects    │ │                │          │
    │ API 호출   │ │ ※ 설정 페이지에서│          │
    │            │ │   사전 경고 완료│          │
    │  → 흑백    │ │   (3-2 참조)   │          │
    │    자동    │ │                │          │
    │    전환    │ │ → 수동 설정    │          │
    │            │ │   필요         │          │
    └────────────┘ └────────────────┘          │
              │         │                       │
              └────┬────┘                       │
                   ▼                            │
         ┌─────────────────┐                    │
         │  집중 모드 진행  │◄───────────────────┘
         └─────────────────┘
                   │
                   ▼
         ┌─────────────────┐
         │  스케줄 종료     │
         └─────────────────┘
                   │
                   ▼
    ┌─────────────────────────────┐
    │ Android 15+?                │
    └─────────────────────────────┘
              │         │
             YES        NO
              │         │
              ▼         ▼
    ┌────────────┐ ┌────────────┐
    │ 흑백 모드  │ │ (자동 해제 │
    │ 자동 해제  │ │  불가)     │
    └────────────┘ └────────────┘
```

---

## 5️⃣ 사용자 온보딩

### 5-1. 최초 설정 시 안내

Android 15 미만 사용자에게 최초 1회 안내:

```
┌─────────────────────────────────────┐
│  🌑 흑백 모드 설정 안내              │
├─────────────────────────────────────┤
│                                      │
│  현재 기기에서는 흑백 모드를          │
│  자동으로 전환할 수 없습니다.         │
│                                      │
│  집중 모드 시작 시 알림을 통해        │
│  빠르게 설정할 수 있도록              │
│  안내해 드릴게요.                     │
│                                      │
│            [확인]                    │
└─────────────────────────────────────┘
```

### 5-2. 버전별 기능 표시

```kotlin
fun getGrayscaleFeatureDescription(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        "스케줄 시작 시 자동으로 흑백 모드가 활성화됩니다."
    } else {
        "스케줄 시작 시 흑백 모드 설정 알림이 표시됩니다."
    }
}
```

---

## 6️⃣ 에러 처리

### 6-1. 권한 거부 대응

```kotlin
sealed class GrayscaleResult {
    object Success : GrayscaleResult()
    object PermissionDenied : GrayscaleResult()
    object UserManagedMode : GrayscaleResult()
    object RuleCreationFailed : GrayscaleResult()
    object NotSupported : GrayscaleResult()  // Android 14 이하
}

fun enableGrayscaleWithResult(context: Context): GrayscaleResult {
    // Android 15 미만
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return GrayscaleResult.NotSupported
    }
    
    val nm = context.getSystemService(NotificationManager::class.java)
    
    // 권한 확인
    if (!nm.isNotificationPolicyAccessGranted) {
        return GrayscaleResult.PermissionDenied
    }
    
    // 사용자 관리 모드 확인
    if (nm.areAutomaticZenRulesUserManaged()) {
        return GrayscaleResult.UserManagedMode
    }
    
    // 룰 생성 시도
    return try {
        if (GrayscaleRuleManager.enableGrayscale(context)) {
            GrayscaleResult.Success
        } else {
            GrayscaleResult.RuleCreationFailed
        }
    } catch (e: Exception) {
        GrayscaleResult.RuleCreationFailed
    }
}
```

### 6-2. 결과별 UI 처리

```kotlin
fun handleGrayscaleResult(result: GrayscaleResult, context: Context) {
    when (result) {
        is GrayscaleResult.Success -> {
            // 성공 - 추가 동작 없음
        }
        is GrayscaleResult.PermissionDenied -> {
            // 권한 설정 화면으로 안내
            showPermissionDialog(context)
        }
        is GrayscaleResult.UserManagedMode -> {
            // 시스템 UI로 위임 안내
            showUserManagedWarning(context)
        }
        is GrayscaleResult.RuleCreationFailed -> {
            // 수동 설정 안내 (경고 배너 표시)
            showManualSettingGuide(context)
        }
        is GrayscaleResult.NotSupported -> {
            // Android 14 이하 - 설정 페이지 경고 배너 표시
            // (이미 3-2에서 처리됨)
        }
    }
}
```

### 6-3. 권한 재확인 플로우

```kotlin
// 설정 화면에서 돌아온 후 반드시 재확인
class GrayscaleSettingsActivity : AppCompatActivity() {
    
    override fun onResume() {
        super.onResume()
        
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.isNotificationPolicyAccessGranted) {
            // 권한 허용됨 - 기능 활성화
            updateGrayscaleEnabled(true)
        } else {
            // 여전히 거부됨 - 안내 메시지 유지
            showPermissionStillDenied()
        }
    }
}
```

### 6-4. 권한 미승인 시 동작 정책

> ⚠️ **v1.3 UX 정책 명확화**: 권한 미승인 시 **토글 OFF로 롤백**

| 상황 | 동작 |
|------|------|
| 토글 ON 시도 + 권한 없음 | ① 권한 요청 다이얼로그 → ② 설정 화면 이동 → ③ **미승인 시 OFF로 롤백** |
| 권한 있음 + 토글 ON | 정상 활성화 |
| 권한 미승인 + 집중 모드 시작 | 흑백 모드 적용 안 함 (조용히 무시) |
| 권한 승인 후 | 다음 집중 모드부터 정상 적용 |

> 💡 **UX 원칙**: 사용자가 토글을 ON으로 설정했는데 동작하지 않는 혼란 방지

```kotlin
// 권한 미승인 시 흑백 모드 적용 로직
fun shouldApplyGrayscale(context: Context): Boolean {
    // 1) 글로벌 설정 확인
    if (!appSettings.grayscaleModeEnabled) return false
    
    // 2) Android 15+ 확인
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
    
    // 3) 권한 확인 - 없으면 적용 안 함 (조용히 실패)
    val nm = context.getSystemService(NotificationManager::class.java)
    if (!nm.isNotificationPolicyAccessGranted) return false
    
    return true
}
```

---

## 6-A. 기존 DND 로직과의 연동

> ⚠️ **중요**: 현재 앱은 `FocusTimerService`에서 DND만 토글하고 있음 (흑백 로직 없음)

### 6-A-1. 현재 앱 구조

```
FocusTimerService
    ├── onFocusStart()
    │       └── DndManager.enableDnd()  // 기존 DND 토글
    │
    └── onFocusEnd()
            └── DndManager.disableDnd()  // 기존 DND 토글
```

### 6-A-2. 흑백 모드 연동 방안

```kotlin
// FocusTimerService 수정 (예시)
class FocusTimerService : Service() {
    
    @Inject lateinit var grayscaleRuleManager: GrayscaleRuleManager
    @Inject lateinit var appSettings: AppSettingsRepository
    
    private fun onFocusStart() {
        // 기존 DND 로직
        dndManager.enableDnd()
        
        // 신규 흑백 모드 로직 추가
        if (shouldApplyGrayscale()) {
            grayscaleRuleManager.enableGrayscale(this)
        }
    }
    
    private fun onFocusEnd() {
        // 기존 DND 로직
        dndManager.disableDnd()
        
        // 신규 흑백 모드 로직 추가
        if (shouldApplyGrayscale()) {
            grayscaleRuleManager.disableGrayscale(this)
        }
    }
}
```

### 6-A-3. DND와의 정책 충돌 방지

> ⚠️ **리스크**: DND와 ZenDeviceEffects가 같은 권한(`ACCESS_NOTIFICATION_POLICY`)을 공유

| 항목 | DND (기존) | 흑백 모드 (신규) | 충돌 여부 |
|------|-----------|----------------|----------|
| 권한 | ACCESS_NOTIFICATION_POLICY | ACCESS_NOTIFICATION_POLICY | ⚠️ 동일 권한 |
| 메커니즘 | `setInterruptionFilter()` | `AutomaticZenRule` | ✅ 분리됨 |
| 사용자 DND 설정 | 덮어쓸 수 있음 | 별도 룰로 관리 | ✅ 안전 |

**대응 방안**:
- 흑백 모드는 `INTERRUPTION_FILTER_ALL` 사용 → DND 상태 변경 없음
- `AutomaticZenRule`로 별도 관리 → 기존 DND 로직과 독립

---

## 6-B. 상태 복원 및 중복 실행 대응

### 6-B-1. 상태 복원 리스크

| 시나리오 | 리스크 | 대응 |
|---------|-------|------|
| 앱 프로세스 강제 종료 | 흑백 모드 상태 유실 | DataStore에 상태 저장 |
| 기기 재부팅 | 룰 ID 유실 | 앱 시작 시 복원 로직 |
| 집중 모드 중 앱 업데이트 | 상태 불일치 | 서비스 재시작 시 상태 확인 |

### 6-B-2. 상태 관리 코드

```kotlin
// 앱 시작 시 상태 복원
class DetoxyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 1) 저장된 룰 ID 복원
        GrayscaleManager.restoreRuleId(this)
        
        // 2) 현재 집중 모드 진행 중인지 확인
        val isFocusActive = focusStateRepository.isAnyFocusActive()
        val isGrayscaleEnabled = appSettings.grayscaleModeEnabled
        
        // 3) 상태 불일치 시 복원
        if (isFocusActive && isGrayscaleEnabled && shouldApplyGrayscale()) {
            GrayscaleManager.enableGrayscale(this)
        }
    }
}
```

### 6-B-3. 중복 실행 방지

> ⚠️ **v1.3 단순화**: 현재 앱은 **단일 타이머** 구조 (`AutoStartTimerWorker`도 기존 세션 재사용)
> - 복잡한 Ref-Count 대신 **단순 플래그** 방식으로 충분
> - `FocusTimerService.isRunning` 상태와 동기화

```kotlin
// 단순화된 흑백 모드 상태 관리 (core/manager/GrayscaleManager.kt)
object GrayscaleManager {
    private var isGrayscaleActive = false
    
    fun enableGrayscaleIfNeeded(context: Context): Boolean {
        if (isGrayscaleActive) return false  // 이미 활성화됨
        
        if (!shouldApplyGrayscale(context)) return false
        
        val success = enableGrayscale(context)
        if (success) isGrayscaleActive = true
        return success
    }
    
    fun disableGrayscaleIfNeeded(context: Context): Boolean {
        if (!isGrayscaleActive) return false  // 이미 비활성화됨
        
        disableGrayscale(context)
        isGrayscaleActive = false
        return true
    }
}
```

---

## 7️⃣ 제한 사항 및 고려 사항

### 7-1. OEM/제조사별 차이

| 제조사 | 특이사항 | 대응 방안 |
|--------|---------|----------|
| Samsung | One UI 자체 집중 모드와 충돌 가능성 | 테스트 필수 |
| Xiaomi | MIUI 권한 관리로 인한 추가 허용 필요 | 온보딩 안내 |
| Huawei | EMUI/HarmonyOS 호환성 이슈 | 별도 테스트 |
| Pixel | 기준 동작, 가장 안정적 | - |

> ⚠️ **ZenDeviceEffects는 시스템 레벨 동작**이므로 OEM 구현 차이에 따라 
> 체감이나 동작 시점이 달라질 수 있음

### 7-2. 사용자 수동 조작 시

- 사용자가 스케줄 진행 중 수동으로 흑백 모드 해제 시
- → 스케줄 종료까지 재활성화하지 않음 (사용자 의도 존중)

### 7-3. 다중 스케줄 동시 실행 시 (Ref-Count 설계)

```kotlin
// 흑백 모드 활성화 스케줄 카운트 관리
object GrayscaleRefCounter {
    private var activeScheduleCount = 0
    
    fun onScheduleStart() {
        activeScheduleCount++
        if (activeScheduleCount == 1) {
            // 첫 번째 스케줄 시작 - 흑백 모드 ON
            GrayscaleRuleManager.enableGrayscale(context)
        }
    }
    
    fun onScheduleEnd() {
        activeScheduleCount = maxOf(0, activeScheduleCount - 1)
        if (activeScheduleCount == 0) {
            // 모든 스케줄 종료 - 흑백 모드 OFF
            GrayscaleRuleManager.disableGrayscale(context)
        }
    }
}
```

### 7-4. 기본값 ON에 따른 온보딩 고려

> ⚠️ **주의**: 기본값 ON은 온보딩 시 부정적 경험(권한 요청/설정 안내) 빈도를 높일 수 있음

- **Android 15+**: 특수 접근 권한 승인 필요 → 초기 안내 필수
- **Android 14 이하**: "기본 ON인데 자동 불가" 상태 → 혼란 방지 안내 필수

**권장 대응**:
1. 최초 앱 실행 시 흑백 모드 기능 설명 + 권한 요청 동의 플로우
2. Android 14 이하에서는 기본값 ON이어도 "수동 설정 필요" 안내 명확히

---

## 8️⃣ 기술 부채/유지보수 고려

### 8-1. AutomaticZenRule 관리 복잡도

- 룰 생성/업데이트/삭제/복원 로직 필요
- 앱 재시작 시 룰 ID 복원 로직 필수
- 사용자가 시스템 설정에서 룰 삭제 시 대응 필요

### 8-2. configurationActivity 필수 구현

```kotlin
// ConditionProviderService 없이 운영 시 configurationActivity 지정 필수
class GrayscaleSettingsActivity : AppCompatActivity() {
    // 사용자가 시스템 설정에서 룰 클릭 시 열리는 화면
    // AndroidManifest.xml에 exported=true 필요
}
```

---

## 9️⃣ 결정 사항 요약

| 항목 | 결정 |
|------|------|
| Android 15+ API | `AutomaticZenRule` + `ZenDeviceEffects` |
| API 메서드 | `setShouldDisplayGrayscale(true)` |
| 룰 관리 전략 | **앱 단일 룰** (단순성 우선) |
| Android 14 이하 | 설정 페이지 경고 배너 (사전 안내) |
| **기본값** | **흑백 모드 비활성화** (`grayscaleModeEnabled=false`) ⚠️ v1.2 변경 |
| **적용 단위** | **글로벌 설정** (스케줄별 개별 설정 미지원) |
| **적용 범위** | 수동 타이머, 시간/위치 기반 자동실행 **모두 동일 적용** |
| 알림 차단 | `INTERRUPTION_FILTER_ALL` (알림 유지, 효과만 적용) |
| 필수 권한 | `ACCESS_NOTIFICATION_POLICY` (**특수 접근 권한**) |
| **권한 정책** | 권한 미승인 시 기능 OFF (조용히 무시) |
| 다중 스케줄 | Ref-Count 방식으로 관리 |
| DND 연동 | `FocusTimerService`에서 DND 토글과 병행 호출 |

---

## 🔟 향후 확장 고려

- [ ] 흑백 외 다른 색상 필터 지원 (세피아, 따뜻한 색조 등)
- [ ] AOD 비활성화 옵션 추가
- [ ] 배경화면 디밍 옵션 추가
- [ ] 다크 모드 자동 전환 연동
- [ ] 스케줄별 개별 설정 옵션 (고급 설정)

---

# 🧠 이 기능의 철학 요약

- ❌ 강제로 사용을 막지 않는다
- ⭕ **사용 욕구를 자연스럽게 줄인다**
- ❌ 복잡한 설정을 요구하지 않는다
- ⭕ **자동화로 마찰을 최소화한다**
- ❌ 모든 사용자에게 강요하지 않는다
- ⭕ **선택적으로 제공한다**

---
