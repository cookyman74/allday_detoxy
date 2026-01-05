# 흑백 모드 자동 전환 기능 작업 계획서 (v1.4)

> **TDD 방법론 기반**: Red → Green → Refactor 사이클 적용  
> **작업 원칙**: 테스트 먼저 작성 → 최소 코드 구현 → 리팩터링  
> **참고 문서**: [99_TDD_plan.md](../99_TDD_plan.md)  
> **버전**: v1.4 (코드 리뷰 v2 반영 - ViewModel 연동, 실제 서비스 메서드 수정)

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

## ⚠️ v1.4 주요 수정사항 (코드 리뷰 v2)

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

- [ ] **[CONTEXT]** PRD 섹션 4️⃣ 확인
- [ ] **[ANALYSIS]** 현재 ViewModel 패턴 분석
  - 파일: `FocusSettingsViewModel.kt` (라인 56-82 loadSettings 패턴)
  - 파일: `FocusSettingsUiState` (라인 241-263)

- [ ] **[RED]** 실패 테스트 작성
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

- [ ] **[TASK-001]** Repository 인터페이스 확장
  - 파일: `domain/repository/FocusSettingsRepository.kt`
  - 추가:
    ```kotlin
    val grayscaleModeEnabledFlow: Flow<Boolean>
    suspend fun saveGrayscaleModeEnabled(enabled: Boolean)
    ```

- [ ] **[TASK-002]** Repository 구현
  - 파일: `data/repository/FocusSettingsRepositoryImpl.kt`
  - 키: `GRAYSCALE_MODE_ENABLED` (기본값: `false`)

- [ ] **[TASK-003]** UiState 확장
  - 파일: `presentation/viewmodel/FocusSettingsViewModel.kt`
  - FocusSettingsUiState에 필드 추가:
    ```kotlin
    val grayscaleModeEnabled: Boolean = false
    ```

- [ ] **[TASK-004]** ViewModel loadSettings() 확장
  - 파일: `FocusSettingsViewModel.kt`
  - loadSettings()에 Flow 수집 추가:
    ```kotlin
    launch {
        settingsRepository.grayscaleModeEnabledFlow.collect { enabled ->
            _uiState.update { it.copy(grayscaleModeEnabled = enabled) }
        }
    }
    ```

- [ ] **[TASK-005]** ViewModel 토글 메서드 추가
  - 파일: `FocusSettingsViewModel.kt`
  - 메서드: `toggleGrayscaleMode(enabled: Boolean)`
  - 핵심: **권한 체크 → 미승인 시 롤백**

- [ ] **[GREEN]** 테스트 통과 확인
- [ ] **[REFACTOR]** 코드 정리

### 1.3 사후 작업

- [ ] **[TEST]** 단위 테스트 실행
- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `docs/working_history/v1.1/Phase1_흑백모드_저장소_{작업일자}.md`
- [ ] **[COMMIT]** `[Phase1] 흑백 모드 저장소 + ViewModel 연동`

---

## 📦 Phase 2: GrayscaleManager 구현

> 📄 **목표**: Android 15+ AutomaticZenRule 관리
> 📄 **PRD 참조**: 섹션 2️⃣
> ⚠️ **경로**: `core/manager/GrayscaleManager.kt`

### 2.1 사전 작업

- [ ] **[REVIEW]** Phase 1 결과서 검토
- [ ] **[ANALYSIS]** 기존 `DndManager.kt` 패턴 참고

- [ ] **[RED]** 실패 테스트 작성
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

- [ ] **[TASK-001]** GrayscaleResult sealed class
  - 파일: `core/manager/GrayscaleResult.kt`

- [ ] **[TASK-002]** GrayscaleManager 핵심 구현
  - 파일: `core/manager/GrayscaleManager.kt`
  - **필수 메서드** (Phase 4에서 사용):
    ```kotlin
    object GrayscaleManager {
        private var isGrayscaleActive = false
        
        // 조건 확인 후 활성화 (중복 방지)
        fun enableGrayscaleIfNeeded(context: Context): Boolean
        
        // 조건 확인 후 비활성화 (중복 방지)
        fun disableGrayscaleIfNeeded(context: Context): Boolean
        
        // 룰 존재 여부 검증 (시스템에서 삭제됐는지 확인)
        fun validateRuleExists(context: Context): Boolean
        
        // 앱 시작 시 복원
        fun restoreRuleId(context: Context)
        
        // 상태 확인
        fun isActive(): Boolean = isGrayscaleActive
    }
    ```

- [ ] **[TASK-003]** AutomaticZenRule 파라미터 검증
  - ⚠️ `TYPE_SCHEDULE` + `Uri.EMPTY` 조합 실제 기기 테스트

- [ ] **[TASK-004]** 룰 정합성 검증 로직
  - 시스템에서 룰 삭제 시 `isGrayscaleActive` 동기화
  ```kotlin
  fun validateRuleExists(context: Context): Boolean {
      ruleId?.let { id ->
          val nm = context.getSystemService(NotificationManager::class.java)
          val rules = nm.automaticZenRules
          if (!rules.containsKey(id)) {
              // 시스템에서 삭제됨 → 상태 정리
              isGrayscaleActive = false
              clearRuleId(context)
              return false
          }
      }
      return isGrayscaleActive
  }
  ```

- [ ] **[GREEN]** 테스트 통과 확인
- [ ] **[REFACTOR]** Hilt 바인딩 설정

### 2.3 사후 작업

- [ ] **[TEST]** androidTest 실행 (Android 15 에뮬레이터)
- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `docs/working_history/v1.1/Phase2_GrayscaleManager_{작업일자}.md`
- [ ] **[COMMIT]** `[Phase2] GrayscaleManager 구현`

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
  - `toggleGrayscaleMode()` 메서드:
    ```kotlin
    fun toggleGrayscaleMode(enabled: Boolean) {
        if (enabled) {
            // 권한 확인
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
- [ ] **[COMMIT]** `[Phase3] 권한 관리 및 토글 롤백`

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
  ```kotlin
  @Test
  fun `startTimerInternal calls enableGrayscaleIfNeeded`()
  
  @Test
  fun `stopTimerInternal calls disableGrayscaleIfNeeded`()
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
    grayscaleManager.enableGrayscaleIfNeeded(this)
    ```

- [ ] **[TASK-003]** stopTimerInternal() 연동
  - 파일: `FocusTimerService.kt`
  - 위치: `stopTimerInternal()` 메서드 내 DND 비활성화 코드 다음
  - 추가:
    ```kotlin
    // 흑백 모드 비활성화
    grayscaleManager.disableGrayscaleIfNeeded(this)
    ```

- [ ] **[GREEN]** 테스트 통과 확인

### 4.3 사후 작업

- [ ] **[VERIFY]** 실제 기기에서 흑백 전환 확인
- [ ] **[DOC]** 작업 결과서 작성
- [ ] **[COMMIT]** `[Phase4] FocusTimerService 흑백 모드 연동`

---

## 📦 Phase 5: 설정 페이지 UI

> 📄 **목표**: 흑백 모드 토글 + Android 14 이하 경고 배너
> 📄 **PRD 참조**: 섹션 3️⃣, 4️⃣
> ⚠️ **파일**: `DetoxyControlSettingsScreen.kt`

### 5.1 사전 작업

- [ ] **[REVIEW]** Phase 4 결과서 검토
- [ ] **[ANALYSIS]** DetoxyControlSettingsScreen 구조 분석

- [ ] **[RED]** UI 테스트 작성
  ```kotlin
  @Test
  fun `GrayscaleSettingItem shows toggle OFF by default`()
  
  @Test
  fun `GrayscaleWarningBanner hidden on Android 15+`()
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
- [ ] **[COMMIT]** `[Phase5] 설정 페이지 UI`

---

## 📦 Phase 6: 타이머 화면 배너

> 📄 **목표**: 집중 타이머 페이지에 흑백 모드 안내 배너
> 📄 **PRD 참조**: 섹션 3-3
> ⚠️ **"다시 보지 않기" 저장**: PreferencesDataStore 별도 키 사용 (UX 설정 분리)

### 6.1 사전 작업

- [ ] **[REVIEW]** Phase 5 결과서 검토
- [ ] **[ANALYSIS]** 타이머 화면 구조 분석

### 6.2 본 작업

- [ ] **[TASK-001]** GrayscaleTipBanner Composable
  - 파일: `presentation/ui/timer/GrayscaleTipBanner.kt`

- [ ] **[TASK-002]** "다시 보지 않기" 상태 저장
  - ⚠️ **별도 키**: `UiPreferences.GRAYSCALE_TIP_DISMISSED`
  - FocusSettingsRepository와 분리 (UX vs 기능 설정)

- [ ] **[TASK-003]** 타이머 화면 통합
  - Android 14 이하 + 흑백 모드 ON + 최초 1회

- [ ] **[GREEN]** 테스트 통과 확인

### 6.3 사후 작업

- [ ] **[VERIFY]** 배너 동작 확인
- [ ] **[DOC]** 작업 결과서 작성
- [ ] **[COMMIT]** `[Phase6] 타이머 화면 안내 배너`

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
  - **PRD 6-B-2 전체 로직**:
    ```kotlin
    // 1) 룰 ID 복원
    GrayscaleManager.restoreRuleId(this)
    
    // 2) 룰 정합성 검증 (시스템에서 삭제됐는지)
    GrayscaleManager.validateRuleExists(this)
    
    // 3) 집중 모드 진행 중 + 설정 ON → 재적용
    viewModelScope.launch {
        val isFocusActive = focusStateRepository.isAnyFocusActive()
        val isGrayscaleEnabled = settingsRepository.grayscaleModeEnabledFlow.first()
        
        if (isFocusActive && isGrayscaleEnabled) {
            GrayscaleManager.enableGrayscaleIfNeeded(this@DetoxyApplication)
        }
    }
    ```

### 7.2 사후 작업

- [ ] **[VERIFY]** E2E 시나리오 검증
  - Android 15: 자동 흑백 전환
  - Android 14: 경고 배너 표시
  - 권한 미승인: 토글 OFF 롤백
  - 앱 재시작: 상태 복원
  - 시스템에서 룰 삭제: 상태 동기화
- [ ] **[DOC]** 최종 작업 결과서 작성
- [ ] **[COMMIT]** `[Phase7] 상태 복원 및 최종 통합`

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
**버전**: v1.4 (코드 리뷰 v2 반영)  
**상태**: ⬜ 작성 완료

