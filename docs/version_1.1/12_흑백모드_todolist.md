# 흑백 모드 자동 전환 기능 작업 계획서 (v1.7)

> **TDD 방법론 기반**: Red → Green → Refactor 사이클 적용  
> **작업 원칙**: 테스트 먼저 작성 → 최소 코드 구현 → 리팩터링  
> **참고 문서**: [99_TDD_plan.md](../99_TDD_plan.md)  
> **버전**: v1.7 (코드 리뷰 v5 반영 - DI 패턴 최종 통일)

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 | 흑백 모드 자동 전환 - 집중 모드 활성화 시 화면 그레이스케일 전환 |
| 영향 범위 | `GrayscaleManager`, `FocusTimerService`, `FocusSettingsRepository`, `FocusSettingsViewModel`, `DetoxyControlSettingsScreen` |
| 위험 수준 | 🟠 High |
| 참고 PRD | [12_흑백모드_prd.md](./12_흑백모드_prd.md) (v1.3) |
| 작업 브랜치 | `FEAT/grayscale-mode` |

---

## ⚠️ v1.7 주요 수정사항 (코드 리뷰 v5)

| 항목 | 수정 내용 |
|------|----------|
| **DI 패턴 통일** | Phase 7의 정적 호출(`GrayscaleManager.xxx(this)`) → EntryPoint DI 주입으로 통일 |
| **메서드 시그니처 통일** | 모든 메서드에서 `context` 파라미터 제거 (클래스가 보유) |
| **@ApplicationContext 명시** | DI 클래스에 `@ApplicationContext` 사용 명시 (누수 위험 방지) |

### v1.6 수정사항 (이전)

| 항목 | 수정 내용 |
|------|----------|
| **Application Scope 수정** | `viewModelScope` → `applicationScope` (DetoxyApplication 기존 패턴 사용) |
| **집중 상태 확인 수정** | `focusStateRepository.isAnyFocusActive()` → `FocusTimerService.state` (StateFlow) |
| **GrayscaleSettingsActivity** | Phase 2에 스텁 생성 필요 메모 추가 (setConfigurationActivity 참조) |
| **테스트 케이스 보강** | Phase 4-6 테스트 상세화 (서비스/UI 테스트 분리) |

### v1.4 수정사항 (이전)

| 항목 | 수정 내용 |
|------|----------|
| **ViewModel 연동** | Phase 1에 `FocusSettingsViewModel`/`FocusSettingsUiState` 연동 추가 |
| **서비스 메서드** | `startTimerInternal()`/`stopTimerInternal()`로 수정 |
| **테스트 전략** | `androidTest` 또는 Robolectric 명시 |
| **메서드명 통일** | Phase 2에 `enableGrayscaleIfNeeded()` 정의 포함 |
| **권한 선언** | 이미 존재 (AndroidManifest.xml:34) - 작업 제거 |
| **상태 복원** | 집중 모드 진행 중 재적용 로직 추가 |
| **룰 정합성** | 시스템 룰 존재 여부 검증 로직 추가 |
| **working_history** | 각 Phase 사후 작업에 기록 단계 추가 |

---

## 📦 Phase 1: 저장소 + ViewModel 연동

> 📄 **목표**: 흑백 모드 설정 저장 + ViewModel StateFlow 연동
> 📄 **PRD 참조**: 섹션 4️⃣

### 1.1 사전 작업

- [x] **[CONTEXT]** PRD 섹션 4️⃣ 확인
- [x] **[ANALYSIS]** 현재 ViewModel 패턴 분석
  - 파일: `FocusSettingsViewModel.kt` (라인 56-82 loadSettings 패턴)
  - 파일: `FocusSettingsUiState` (라인 241-263)

- [ ] **[RED]** 실패 테스트 작성 (TDD 스킵 - 빌드 검증으로 대체)
  ```kotlin
  // test/
  @Test
  fun `grayscaleModeEnabled defaults to false`()
  
  @Test
  fun `grayscaleModeEnabled Flow emits on change`()
  
  @Test
  fun `FocusSettingsUiState contains grayscaleModeEnabled`()
  ```

### 1.2 본 작업

- [x] **[TASK-001]** Repository 인터페이스 확장
  - 파일: `domain/repository/FocusSettingsRepository.kt`
  - 추가:
    ```kotlin
    val grayscaleModeEnabledFlow: Flow<Boolean>
    suspend fun saveGrayscaleModeEnabled(enabled: Boolean)
    ```

- [x] **[TASK-002]** Repository 구현
  - 파일: `data/repository/FocusSettingsRepositoryImpl.kt`
  - 키: `GRAYSCALE_MODE_ENABLED` (기본값: `false`)

- [x] **[TASK-003]** UiState 확장
  - 파일: `presentation/viewmodel/FocusSettingsViewModel.kt`
  - FocusSettingsUiState에 필드 추가:
    ```kotlin
    val grayscaleModeEnabled: Boolean = false
    val showGrayscalePermissionDialog: Boolean = false
    ```

- [x] **[TASK-004]** ViewModel loadSettings() 확장
  - 파일: `FocusSettingsViewModel.kt`
  - loadSettings()에 Flow 수집 추가:
    ```kotlin
    launch {
        settingsRepository.grayscaleModeEnabledFlow.collect { enabled ->
            _uiState.update { it.copy(grayscaleModeEnabled = enabled) }
        }
    }
    ```

- [x] **[TASK-005]** ViewModel 토글 메서드 추가
  - 파일: `FocusSettingsViewModel.kt`
  - 메서드: `toggleGrayscaleMode(enabled: Boolean)`, `onGrayscalePermissionResult(granted: Boolean)`
  - 핵심: **권한 체크 → 미승인 시 다이얼로그 표시**

- [x] **[GREEN]** 빌드 검증 완료 (`./gradlew compileDebugKotlin` 성공)
- [x] **[REFACTOR]** 코드 정리 완료

### 1.3 사후 작업

- [x] **[TEST]** 빌드 검증 실행 (BUILD SUCCESSFUL)
- [x] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase1_저장소ViewModel연동_2026-01-05.md`
- [ ] **[COMMIT]** `feat(settings): add grayscale mode setting to repository and ViewModel`

---

## 📦 Phase 2: GrayscaleManager 구현

> 📄 **목표**: Android 15+ AutomaticZenRule 관리
> 📄 **PRD 참조**: 섹션 2️⃣
> ⚠️ **경로**: `core/manager/GrayscaleManager.kt`

### 2.1 사전 작업

- [x] **[REVIEW]** Phase 1 결과서 검토
- [x] **[ANALYSIS]** 기존 `DndManager.kt` 패턴 참고

- [ ] **[RED]** 실패 테스트 작성 (TDD 스킵 - Android 15 에뮬레이터 필요)
  - ⚠️ **테스트 전략**: `androidTest` 또는 Robolectric (JVM에서 Android API 불가)
  ```kotlin
  // androidTest/
  @Test
  fun `enableGrayscale returns false when permission not granted`()
  
  @Test
  fun `enableGrayscaleIfNeeded skips when already active`()
  
  @Test
  fun `validateRuleExists returns false when rule deleted externally`()
  ```

### 2.2 본 작업

- [x] **[TASK-001]** GrayscaleResult sealed class
  - 파일: `core/manager/GrayscaleResult.kt`
  - ⚠️ **용도**: 에러 타입 분류 (Success, PermissionDenied, NotSupported 등)
  - 내부 메서드는 Boolean 반환, UI 레이어에서 Result 타입 활용

- [x] **[TASK-002]** GrayscaleManager 핵심 구현
  - 파일: `core/manager/GrayscaleManager.kt`
  - ⚠️ **패턴**: `@Singleton class` (DI 주입) - 기존 `DndManager` 패턴과 통일
  - **필수 메서드** (Phase 4에서 사용):
    ```kotlin
    // ⚠️ class로 정의 (object 아님) - DndManager 패턴 준수
    // ⚠️ @ApplicationContext로 Context 주입 (누수 방지)
    class GrayscaleManager @Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        private var isGrayscaleActive = false
        
        // 조건 확인 후 활성화 (중복 방지)
        // ⚠️ API 레벨 게이트 필수: Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        fun enableGrayscaleIfNeeded(): Boolean
        
        // 조건 확인 후 비활성화 (중복 방지)
        fun disableGrayscaleIfNeeded(): Boolean
        
        // 룰 존재 여부 검증 (시스템에서 삭제됐는지 확인)
        fun validateRuleExists(): Boolean
        
        // 앱 시작 시 복원
        fun restoreRuleId()
        
        // 상태 확인
        fun isActive(): Boolean = isGrayscaleActive
    }
    ```
  - ⚠️ **GrayscaleSettingsActivity 생성 완료** (스텁)
  - `setConfigurationActivity()`에서 참조되므로 빌드 오류 방지용

- [x] **[TASK-003]** AutomaticZenRule 파라미터 검증
  - ⚠️ `TYPE_SCHEDULE_TIME` + `Uri.EMPTY` 조합 (TYPE_SCHEDULE → TYPE_SCHEDULE_TIME 수정)

- [x] **[TASK-004]** 룰 정합성 검증 로직
  - 시스템에서 룰 삭제 시 `isGrayscaleActive` 동기화
  - ⚠️ **시그니처**: 파라미터 없음 (context는 클래스가 보유)
  ```kotlin
  // context는 생성자에서 주입받아 보유 중
  fun validateRuleExists(): Boolean {
      ruleId?.let { id ->
          val nm = context.getSystemService(NotificationManager::class.java)
          val rules = nm.automaticZenRules
          if (!rules.containsKey(id)) {
              // 시스템에서 삭제됨 → 상태 정리
              isGrayscaleActive = false
              clearRuleId()
              return false
          }
      }
      return isGrayscaleActive
  }
  ```

- [x] **[GREEN]** 빌드 검증 완료 (`./gradlew compileDebugKotlin` 성공)
- [x] **[REFACTOR]** Hilt 바인딩 설정 (@Singleton + @Inject 완료)

### 2.3 사후 작업

- [ ] **[TEST]** androidTest 실행 (Android 15 에뮬레이터) - Phase 7에서 통합 테스트
- [x] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase2_GrayscaleManager구현_2026-01-05.md`
- [ ] **[COMMIT]** `feat(manager): implement GrayscaleManager with ZenDeviceEffects API`

---

## 📦 Phase 3: 권한 관리 및 토글 롤백

> 📄 **목표**: 권한 요청 + 미승인 시 토글 OFF 롤백
> 📄 **PRD 참조**: 섹션 6-4
> ⚠️ **권한 선언**: AndroidManifest.xml:34에 **이미 존재** (작업 불필요)

### 3.1 사전 작업

- [ ] **[REVIEW]** Phase 2 결과서 검토
- [ ] **[ANALYSIS]** 기존 권한 다이얼로그 패턴

- [ ] **[RED]** 실패 테스트 작성
  ```kotlin
  @Test
  fun `toggleGrayscaleMode shows dialog when permission denied`()
  
  @Test
  fun `toggleGrayscaleMode rolls back to OFF when permission not granted after settings`()
  
  @Test
  fun `shouldApplyGrayscale returns false when permission denied`()
  ```

### 3.2 본 작업

- [ ] **[TASK-001]** shouldApplyGrayscale() 유틸리티
  - 파일: `core/manager/GrayscaleManager.kt`

- [ ] **[TASK-002]** 토글 롤백 로직 (ViewModel)
  - 파일: `FocusSettingsViewModel.kt`
  - ⚠️ **참고**: `FocusSettingsViewModel`은 `AndroidViewModel` 확장 (`getApplication<Application>()` 사용 가능)
  - `toggleGrayscaleMode()` 메서드:
    ```kotlin
    // FocusSettingsViewModel : AndroidViewModel(application)
    fun toggleGrayscaleMode(enabled: Boolean) {
        if (enabled) {
            // ⚠️ API 레벨 게이트 먼저 확인
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // Android 14 이하: 설정은 저장하되 경고 배너 표시
                _uiState.update { it.copy(grayscaleModeEnabled = enabled) }
                saveGrayscaleSetting(enabled)
                return
            }
            
            // Android 15+: 권한 확인
            val nm = getApplication<Application>()
                .getSystemService(NotificationManager::class.java)
            if (!nm.isNotificationPolicyAccessGranted) {
                // 다이얼로그 표시 → 설정 화면 이동
                _uiState.update { it.copy(showGrayscalePermissionDialog = true) }
                return  // 토글 ON 유보
            }
        }
        // 정상 토글
        _uiState.update { it.copy(grayscaleModeEnabled = enabled) }
        saveGrayscaleSetting(enabled)
    }
    
    fun onGrayscalePermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(grayscaleModeEnabled = true) }
            saveGrayscaleSetting(true)
        }
        // 미승인 시: 이미 false 상태이므로 추가 작업 불필요
        _uiState.update { it.copy(showGrayscalePermissionDialog = false) }
    }
    ```

- [ ] **[TASK-003]** UiState에 다이얼로그 상태 추가
  - 추가 필드: `showGrayscalePermissionDialog: Boolean = false`

- [ ] **[GREEN]** 테스트 통과 확인

### 3.3 사후 작업

- [ ] **[VERIFY]** 권한 플로우 수동 검증
- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase3_권한관리_{YYYY-MM-DD}.md`
- [ ] **[COMMIT]** `feat(settings): add grayscale permission check and toggle rollback`

---

## 📦 Phase 4: FocusTimerService 연동

> 📄 **목표**: 타이머 시작/종료 시 흑백 모드 연동
> 📄 **PRD 참조**: 섹션 5️⃣, 6-A
> ⚠️ **실제 메서드**: `startTimerInternal()` (라인 276), `stopTimerInternal()` (라인 439)

### 4.1 사전 작업

- [ ] **[REVIEW]** Phase 3 결과서 검토
- [ ] **[ANALYSIS]** FocusTimerService 구조 분석
  - 파일: `service/timer/FocusTimerService.kt`
  - 시작: `startTimerInternal()` (라인 276)
  - 종료: `stopTimerInternal()` (라인 439)

- [ ] **[RED]** 실패 테스트 작성
  - ⚠️ **테스트 전략**: Robolectric 또는 Mock 기반 (Service 단위 테스트)
  ```kotlin
  @Test
  fun `startTimerInternal calls enableGrayscaleIfNeeded when setting enabled`()
  
  @Test
  fun `startTimerInternal skips grayscale when setting disabled`()
  
  @Test
  fun `stopTimerInternal calls disableGrayscaleIfNeeded`()
  
  @Test
  fun `stopTimerInternal handles grayscale already inactive`()
  ```

### 4.2 본 작업

- [ ] **[TASK-001]** GrayscaleManager DI 추가
  - 파일: `FocusTimerService.kt`
  - 주입: `@Inject lateinit var grayscaleManager: GrayscaleManager`

- [ ] **[TASK-002]** startTimerInternal() 연동
  - 파일: `FocusTimerService.kt`
  - 위치: `startTimerInternal()` 메서드 내 DND 활성화 코드 다음
  - 추가:
    ```kotlin
    // 흑백 모드 활성화 (설정 ON + Android 15+ + 권한 있음만)
    // ⚠️ API 레벨 게이트는 GrayscaleManager 내부에서 처리
    grayscaleManager.enableGrayscaleIfNeeded()
    ```

- [ ] **[TASK-003]** stopTimerInternal() 연동
  - 파일: `FocusTimerService.kt`
  - 위치: `stopTimerInternal()` 메서드 내 DND 비활성화 코드 다음
  - 추가:
    ```kotlin
    // 흑백 모드 비활성화
    grayscaleManager.disableGrayscaleIfNeeded()
    ```

- [ ] **[GREEN]** 테스트 통과 확인

### 4.3 사후 작업

- [ ] **[VERIFY]** 실제 기기에서 흑백 전환 확인
- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase4_서비스연동_{YYYY-MM-DD}.md`
- [ ] **[COMMIT]** `feat(timer): integrate grayscale mode with FocusTimerService`

---

## 📦 Phase 5: 설정 페이지 UI

> 📄 **목표**: 흑백 모드 토글 + Android 14 이하 경고 배너
> 📄 **PRD 참조**: 섹션 3️⃣, 4️⃣
> ⚠️ **파일**: `DetoxyControlSettingsScreen.kt`

### 5.1 사전 작업

- [ ] **[REVIEW]** Phase 4 결과서 검토
- [ ] **[ANALYSIS]** DetoxyControlSettingsScreen 구조 분석

- [ ] **[RED]** UI 테스트 작성 (Compose UI Test)
  ```kotlin
  @Test
  fun `GrayscaleSettingItem shows toggle OFF by default`()
  
  @Test
  fun `GrayscaleSettingItem toggle triggers permission dialog when not granted`()
  
  @Test
  fun `GrayscaleWarningBanner visible on Android 14-`()
  
  @Test
  fun `GrayscaleWarningBanner hidden on Android 15+`()
  
  @Test
  fun `GrayscaleWarningBanner button opens system settings`()
  ```

### 5.2 본 작업

- [ ] **[TASK-001]** 흑백 모드 설정 항목 Composable
  - 파일: `presentation/ui/settings/focus/GrayscaleSettingItem.kt`

- [ ] **[TASK-002]** Android 14 이하 경고 배너
  - 파일: `presentation/ui/settings/focus/GrayscaleWarningBanner.kt`

- [ ] **[TASK-003]** DetoxyControlSettingsScreen 통합
  - ViewModel의 `grayscaleModeEnabled` 상태 바인딩
  - `toggleGrayscaleMode()` 호출 연결
  - 권한 다이얼로그 표시 로직

- [ ] **[GREEN]** 테스트 통과 확인

### 5.3 사후 작업

- [ ] **[VERIFY]** UI 수동 검증
- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase5_설정UI_{YYYY-MM-DD}.md`
- [ ] **[COMMIT]** `feat(ui): add grayscale mode toggle and warning banner to settings`

---

## 📦 Phase 6: 타이머 화면 배너

> 📄 **목표**: 집중 타이머 페이지에 흑백 모드 안내 배너
> 📄 **PRD 참조**: 섹션 3-3
> ⚠️ **"다시 보지 않기" 저장**: `FocusSettingsRepositoryImpl` DataStore에 통합
> - 키 정의: `KEY_GRAYSCALE_TIP_DISMISSED = booleanPreferencesKey("grayscale_tip_dismissed")`
> - `FocusSettingsRepository` 인터페이스에 메서드 추가

### 6.1 사전 작업

- [ ] **[REVIEW]** Phase 5 결과서 검토
- [ ] **[ANALYSIS]** 타이머 화면 구조 분석

### 6.2 본 작업

- [ ] **[TASK-001]** GrayscaleTipBanner Composable
  - 파일: `presentation/ui/timer/GrayscaleTipBanner.kt`

- [ ] **[TASK-002]** "다시 보지 않기" 상태 저장
  - 파일: `FocusSettingsRepositoryImpl.kt`
  - 키: `KEY_GRAYSCALE_TIP_DISMISSED`
  - 인터페이스 확장:
    ```kotlin
    // FocusSettingsRepository.kt
    val grayscaleTipDismissedFlow: Flow<Boolean>
    suspend fun saveGrayscaleTipDismissed(dismissed: Boolean)
    ```

- [ ] **[TASK-003]** 타이머 화면 통합
  - Android 14 이하 + 흑백 모드 ON + 최초 1회

- [ ] **[GREEN]** 테스트 통과 확인

### 6.3 사후 작업

- [ ] **[VERIFY]** 배너 동작 확인
- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase6_타이머배너_{YYYY-MM-DD}.md`
- [ ] **[COMMIT]** `feat(ui): add grayscale tip banner to timer screen`

---

## 📦 Phase 7: 상태 복원 및 최종 통합

> 📄 **목표**: 앱 재시작 시 상태 복원 + 최종 검증
> 📄 **PRD 참조**: 섹션 6-B, 8-2

### 7.1 본 작업

- [ ] **[TASK-001]** GrayscaleSettingsActivity 생성
  - 파일: `presentation/ui/settings/GrayscaleSettingsActivity.kt`
  - AndroidManifest: `exported=true`

- [ ] **[TASK-002]** DetoxyApplication 상태 복원 (완전 로직)
  - 파일: `DetoxyApplication.kt`
  - ⚠️ **코드 수정**: `viewModelScope` 대신 기존 `applicationScope` 패턴 사용
  - ⚠️ **상태 확인**: `FocusTimerService.state.value == FocusState.RUNNING` 사용
  - **PRD 6-B-2 전체 로직**:
    ```kotlin
    // 기존 applicationScope 활용 (라인 63)
    // private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // ⚠️ 모든 GrayscaleManager 호출은 EntryPoint를 통한 DI 주입으로 통일
    applicationScope.launch {
        // EntryPoint를 통해 의존성 가져오기
        val entryPoint = EntryPointAccessors.fromApplication(
            this@DetoxyApplication,
            DetoxyApplicationEntryPoint::class.java
        )
        val grayscaleManager = entryPoint.grayscaleManager()
        
        // 1) 룰 ID 복원
        grayscaleManager.restoreRuleId()
        
        // 2) 룰 정합성 검증 (시스템에서 삭제됐는지)
        grayscaleManager.validateRuleExists()
        
        // 3) 집중 모드 진행 중 + 설정 ON → 재적용
        val isFocusActive = FocusTimerService.state.value == FocusState.RUNNING
        val isGrayscaleEnabled = entryPoint.focusSettingsRepository()
            .grayscaleModeEnabledFlow.first()
        
        if (isFocusActive && isGrayscaleEnabled) {
            grayscaleManager.enableGrayscaleIfNeeded()
        }
    }
    ```
  - **EntryPoint 확장 필요**: `DetoxyApplicationEntryPoint`에 추가
    ```kotlin
    fun focusSettingsRepository(): FocusSettingsRepository
    fun grayscaleManager(): GrayscaleManager
    ```

### 7.2 사후 작업

- [ ] **[VERIFY]** E2E 시나리오 검증
  - Android 15: 자동 흑백 전환
  - Android 14: 경고 배너 표시
  - 권한 미승인: 토글 OFF 롤백
  - 앱 재시작: 상태 복원
  - 시스템에서 룰 삭제: 상태 동기화
- [ ] **[DOC]** 최종 작업 결과서 작성
  - 파일: `working_history/version_1.1/흑백모드/Phase7_상태복원최종_{YYYY-MM-DD}.md`
- [ ] **[COMMIT]** `feat(app): add grayscale state restoration on app start`

---

## ✅ 최종 체크리스트

- [ ] 모든 Phase 완료
- [ ] ViewModel↔Repository↔UI 연동 확인
- [ ] Android 15 기기 흑백 전환 확인
- [ ] Android 14 기기 경고 배너 확인
- [ ] 권한 미승인 시 토글 OFF 롤백 확인
- [ ] 앱 재시작 후 상태 복원 확인
- [ ] 시스템에서 룰 삭제 시 상태 동기화 확인
- [ ] 린터 경고 0개

---

## 📊 Phase 완료 조건

| Phase | 테스트 | 린터 | 결과서 | 커밋 | 상태 |
|-------|--------|------|--------|------|------|
| 1 (저장소+ViewModel) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 2 (GrayscaleManager) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 3 (권한 관리) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 4 (서비스 연동) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 5 (설정 UI) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 6 (타이머 배너) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 7 (상태 복원) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

---

## 📅 예상 일정

| Phase | 예상 | 비고 |
|-------|------|------|
| Phase 1 | 0.5일 | 저장소+ViewModel |
| Phase 2 | 1일 | 핵심 API + androidTest |
| Phase 3 | 0.5일 | 권한 롤백 |
| Phase 4 | 0.5일 | 서비스 연동 |
| Phase 5 | 0.5일 | 설정 UI |
| Phase 6 | 0.5일 | 타이머 배너 |
| Phase 7 | 0.5일 | 상태 복원 |
| **Total** | **4.5일** | |

---

**작성일**: 2026-01-05  
**버전**: v1.7 (코드 리뷰 v5 반영 - DI 패턴 최종 통일)  
**상태**: ⬜ 작성 완료

