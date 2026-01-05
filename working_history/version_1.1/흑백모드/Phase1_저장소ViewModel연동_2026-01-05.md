# Grayscale Mode Phase 1 - 저장소 + ViewModel 연동

**작업일**: 2026-01-05  
**버전**: v1.1  
**Phase**: Phase 1 (저장소 + ViewModel 연동)

---

## 📋 작업 요약

흑백 모드 설정을 저장하고 ViewModel StateFlow와 연동하는 기능을 구현했습니다.

---

## ✅ 완료된 작업

### TASK-001: Repository 인터페이스 확장
- **파일**: `domain/repository/FocusSettingsRepository.kt`
- **추가 내용**:
  - `grayscaleModeEnabledFlow: Flow<Boolean>` - 흑백 모드 설정 Flow
  - `saveGrayscaleModeEnabled(enabled: Boolean)` - 흑백 모드 설정 저장

### TASK-002: Repository 구현
- **파일**: `data/repository/FocusSettingsRepositoryImpl.kt`
- **추가 내용**:
  - `KEY_GRAYSCALE_MODE_ENABLED = booleanPreferencesKey("grayscale_mode_enabled")`
  - DataStore 기반 Flow 및 저장 메서드 구현
  - 기본값: `false`

### TASK-003: UiState 확장
- **파일**: `presentation/viewmodel/FocusSettingsViewModel.kt`
- **FocusSettingsUiState에 추가**:
  - `grayscaleModeEnabled: Boolean = false`
  - `showGrayscalePermissionDialog: Boolean = false`

### TASK-004: ViewModel loadSettings() 확장
- **파일**: `FocusSettingsViewModel.kt`
- **추가**: `grayscaleModeEnabledFlow` 수집 및 UiState 업데이트

### TASK-005: ViewModel 토글 메서드 추가
- **파일**: `FocusSettingsViewModel.kt`
- **메서드**:
  - `toggleGrayscaleMode(enabled: Boolean)` - API 레벨 게이트 + 권한 체크
  - `onGrayscalePermissionResult(granted: Boolean)` - 권한 요청 결과 처리
  - `saveGrayscaleSetting(enabled: Boolean)` - 설정 저장

---

## 🔍 주요 구현 내용

### 권한 체크 로직 (toggleGrayscaleMode) - 리뷰 피드백 반영

```kotlin
fun toggleGrayscaleMode(enabled: Boolean) {
    // ⚠️ 토글 OFF 시: 다이얼로그가 열려있다면 닫기 (리뷰 피드백 반영)
    if (!enabled) {
        _uiState.update { 
            it.copy(
                grayscaleModeEnabled = false, 
                showGrayscalePermissionDialog = false  // 다이얼로그도 닫기
            ) 
        }
        saveGrayscaleSetting(false)
        return
    }
    
    // 토글 ON 시: API 레벨 게이트 확인
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // Android 14 이하: 설정 저장 (자동 전환 불가)
        _uiState.update { it.copy(grayscaleModeEnabled = true) }
        saveGrayscaleSetting(true)
        return
    }
    
    // Android 15+: 권한 확인
    val nm = getApplication<Application>()
        .getSystemService(NotificationManager::class.java)
    if (!nm.isNotificationPolicyAccessGranted) {
        // ⚠️ 권한 미승인: grayscaleModeEnabled를 명시적으로 false 설정
        // (저장값이 true인 상태에서 권한이 철회된 경우에도 false로 강제 동기화)
        _uiState.update { 
            it.copy(
                grayscaleModeEnabled = false,  // 명시적 false (리뷰 피드백 반영)
                showGrayscalePermissionDialog = true
            ) 
        }
        // 저장값도 false로 동기화 (권한 없으면 기능 사용 불가)
        saveGrayscaleSetting(false)
        return
    }
    
    // 권한 있음: 정상 토글 ON
    _uiState.update { it.copy(grayscaleModeEnabled = true) }
    saveGrayscaleSetting(true)
}
```

---

## 🔧 리뷰 피드백 반영 Round 1 (2026-01-05)

### [중간] 다이얼로그 OFF 토글 시 닫기
- **문제**: 권한 다이얼로그가 열린 상태에서 토글 OFF 시 `showGrayscalePermissionDialog`가 false로 내려가지 않음
- **해결**: `enabled == false` 분기에서 다이얼로그도 함께 닫도록 수정

### [중간] 단위 테스트 추가
- **파일**: `FocusSettingsUiStateTest.kt` (7개 테스트)

---

## 🔧 리뷰 피드백 반영 Round 2 (2026-01-05)

### [중간] 권한 미승인 시 grayscaleModeEnabled 명시적 false 설정
- **문제**: 저장값이 이미 true인 상태(권한이 나중에 회수된 케이스)에서 UI 상태가 true로 유지될 수 있음
- **해결**: 
  - 권한 미승인 시 `grayscaleModeEnabled = false` 명시적 설정
  - 저장값도 `saveGrayscaleSetting(false)`로 동기화
- **정책 결정**: 권한 철회 시 저장값도 false로 강제 동기화

### [중간] 행동 테스트 공백
- **상태**: UiState 테스트로 상태 전환 시나리오 검증 중
- **추가 필요**: ViewModel 행동 테스트는 Phase 2 이후 mock 기반으로 추가 예정

### [낮음] 결과서 경로 규칙
- **현재 경로**: `working_history/version_1.1/흑백모드/{Phase}_{작업타이틀}_{날짜}.md`
- **사유**: 사용자 요청에 따라 기능별 하위 디렉토리 구조 사용

---

## 🧪 검증

### 빌드 검증
```bash
./gradlew compileDebugKotlin
# BUILD SUCCESSFUL in 10s
```

### 단위 테스트
```bash
./gradlew testDebugUnitTest --tests "com.allday.detoxy.presentation.viewmodel.FocusSettingsUiStateTest"
# BUILD SUCCESSFUL in 10s (7 tests passed)
```

---

## 📁 수정된 파일

| 파일 | 변경 내용 |
|------|----------|
| `FocusSettingsRepository.kt` | `grayscaleModeEnabledFlow`, `saveGrayscaleModeEnabled()` 추가 |
| `FocusSettingsRepositoryImpl.kt` | DataStore 키, Flow, 저장 메서드 구현 |
| `FocusSettingsViewModel.kt` | `toggleGrayscaleMode()`, `onGrayscalePermissionResult()`, Flow 수집 추가 |
| `FocusSettingsUiState` | `grayscaleModeEnabled`, `showGrayscalePermissionDialog` 필드 추가 |
| `FocusSettingsUiStateTest.kt` | 흑백 모드 UiState 단위 테스트 7개 추가 (신규) |

---

## 📝 다음 단계

- **Phase 2**: GrayscaleManager 구현 (Android 15+ AutomaticZenRule 관리)
