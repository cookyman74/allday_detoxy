# Grayscale Mode Phase 2 - GrayscaleManager 구현

**작업일**: 2026-01-05  
**버전**: v1.1  
**Phase**: Phase 2 (GrayscaleManager 구현)

---

## 📋 작업 요약

Android 15+ AutomaticZenRule + ZenDeviceEffects 기반 GrayscaleManager를 구현했습니다.

---

## ✅ 완료된 작업

### TASK-001: GrayscaleResult sealed class
- **파일**: `core/manager/GrayscaleResult.kt`
- **용도**: 에러 타입 분류 (Success, PermissionDenied, NotSupported 등)
- **타입**:
  - `Success` - 성공
  - `PermissionDenied` - 권한 거부
  - `NotSupported` - Android 15 미만
  - `UserManaged` - 시스템 사용자 관리 모드
  - `AlreadyInState` - 이미 활성/비활성 상태
  - `RuleDeleted` - 룰이 시스템에서 삭제됨
  - `Error(errorMessage)` - 알 수 없는 오류

### TASK-002: GrayscaleManager 핵심 구현
- **파일**: `core/manager/GrayscaleManager.kt`
- **패턴**: `@Singleton class` (DI 주입) - DndManager 패턴 준수
- **필수 메서드**:
  - `enableGrayscaleIfNeeded(): Boolean` - 조건 확인 후 활성화
  - `disableGrayscaleIfNeeded(): Boolean` - 조건 확인 후 비활성화
  - `validateRuleExists(): Boolean` - 룰 정합성 검증
  - `restoreRuleId()` - 앱 시작 시 복원
  - `isActive(): Boolean` - 상태 확인
  - `isSupported(): Boolean` - 지원 여부 확인
  - `hasPermission(): Boolean` - 권한 확인

### TASK-003: GrayscaleSettingsActivity 스텁
- **파일**: `presentation/ui/settings/GrayscaleSettingsActivity.kt`
- **용도**: AutomaticZenRule.setConfigurationActivity()에서 참조
- **AndroidManifest**: `exported="true"` 등록 완료

---

## 🔍 주요 구현 내용

### AutomaticZenRule + ZenDeviceEffects (최신 구현)

```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun enableGrayscaleInternal(): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java)
    
    // 권한/사용자 관리 모드 확인 (생략)...
    
    return try {
        // ZenDeviceEffects - 그레이스케일만 적용
        val effects = ZenDeviceEffects.Builder()
            .setShouldDisplayGrayscale(true)
            .build()
        
        // ZenPolicy - 알림 차단 없이 효과만 적용
        val policy = ZenPolicy.Builder()
            .allowAllSounds()
            .build()
        
        // ⚠️ 클래스 참조 사용 (문자열 대신)
        val rule = AutomaticZenRule.Builder(RULE_NAME, Uri.EMPTY)
            .setType(AutomaticZenRule.TYPE_SCHEDULE_TIME)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            .setZenPolicy(policy)
            .setDeviceEffects(effects)
            .setConfigurationActivity(
                ComponentName(context, GrayscaleSettingsActivity::class.java)
            )
            .setEnabled(true)
            .build()
        
        // ⚠️ null 체크 추가
        val resultRuleId: String? = if (ruleId != null) {
            try {
                nm.updateAutomaticZenRule(ruleId!!, rule)
                ruleId
            } catch (e: Exception) {
                // 기존 룰 삭제됨 → 새로 생성
                nm.addAutomaticZenRule(rule)
            }
        } else {
            nm.addAutomaticZenRule(rule)
        }
        
        // ⚠️ 룰 생성 실패 시 false 반환
        if (resultRuleId == null) {
            isGrayscaleActive = false
            return false
        }
        
        ruleId = resultRuleId
        saveRuleIdAsync(ruleId)  // ⚠️ 비동기 저장
        
        isGrayscaleActive = true
        true
    } catch (e: Exception) {
        isGrayscaleActive = false  // ⚠️ 실패 시 명시적 false
        false
    }
}
```

### DI 패턴 (Hilt)

```kotlin
@Singleton
class GrayscaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 비동기 처리용 CoroutineScope (ANR 방지)
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // ...
}
```

---

## 🧪 검증

### 빌드 검증
```bash
./gradlew compileDebugKotlin
# BUILD SUCCESSFUL in 3s
```

---

## 📁 수정된 파일

| 파일 | 변경 내용 |
|------|----------|
| `GrayscaleResult.kt` | sealed class 생성 (신규) |
| `GrayscaleManager.kt` | AutomaticZenRule 기반 구현 (신규) |
| `GrayscaleSettingsActivity.kt` | Activity 스텁 생성 (신규) |
| `AndroidManifest.xml` | GrayscaleSettingsActivity 등록 |

---

## ⚠️ 빌드 중 수정 사항

1. **TYPE_SCHEDULE → TYPE_SCHEDULE_TIME**: Android 15 API에서 올바른 상수명
2. **getMessage() → getDisplayMessage()**: Error data class의 message 프로퍼티와 JVM 시그니처 충돌 해결

---

## � 리뷰 피드백 반영 (2026-01-05)

### [높음] addAutomaticZenRule() null 반환 처리
- **문제**: 룰 생성 실패 시에도 `isGrayscaleActive = true`로 설정되어 상태 불일치 발생
- **해결**: 
  - `resultRuleId` 변수로 반환값을 받고 null 체크 추가
  - null이면 `isGrayscaleActive = false` 설정 후 `return false`
  - 예외 catch 블록에서도 `isGrayscaleActive = false` 명시적 설정

### [중간] runBlocking ANR 위험
- **문제**: `restoreRuleId()`/`saveRuleId()` 내 `runBlocking` 사용으로 메인 스레드 블로킹
- **해결**:
  - `managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` 추가
  - `saveRuleId()` → `saveRuleIdAsync()` 비동기 처리
  - `clearRuleId()` → `clearRuleIdAsync()` 비동기 처리
  - `restoreRuleId()` 내부를 `managerScope.launch {}` 블록으로 변경

### [중간] TYPE_SCHEDULE_TIME + Uri.EMPTY 실패 처리
- **문제**: 룰 생성 거부 시 활성화 성공처럼 보임
- **해결**: null 체크와 결합하여 명확한 에러 로그 및 false 반환

### [낮음] ComponentName 문자열 참조
- **문제**: 리팩터링/ProGuard 변경 시 문자열 경로 깨질 위험
- **해결**: `ComponentName(context, GrayscaleSettingsActivity::class.java)` 클래스 참조 사용

---

## �📝 다음 단계

- **Phase 3**: 권한 관리 및 토글 롤백
- **Phase 4**: FocusTimerService 연동
