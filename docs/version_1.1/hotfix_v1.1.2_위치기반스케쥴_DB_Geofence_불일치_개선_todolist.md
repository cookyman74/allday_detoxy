# 위치기반 스케줄 DB-Geofence 상태 불일치 개선 핫픽스

> **작업 원칙**: DB 상태와 실제 Geofence 등록 상태의 일관성 보장  
> **버전**: Hotfix v1.1.2 (Rev.2 - 리뷰 피드백 반영)  
> **작성일**: 2026-01-18  
> **수정일**: 2026-01-19 (리뷰 피드백 반영)

---

## 📋 문제 현황

### 현상
- UI에 "위치 모니터링: 5/5"로 표시되지만, 실제 Geofence는 등록되지 않은 경우 발생
- 사용자가 위치 기반 스케줄을 정상적으로 등록했다고 착각하게 됨

### 근본 원인 분석 (4가지)

| # | 원인 | 관련 파일 | 라인 |
|---|------|-----------|------|
| 1 | **UI 카운트가 DB `isEnabled` 기준** | `ScheduleTabScreen.kt` | L88, L322 |
|   | UI "위치 모니터링: X/5"는 DB의 `isEnabled=true` 개수를 표시하며, 실제 Geofence 등록 성공 여부를 반영하지 않음 | `LocationBasedAutoRunViewModel.kt` | L140 |
| 2 | **rescheduleAll 실패 시 DB 미갱신** | `AutoRunGeofenceManager.kt` | L267-316 |
|   | 앱 시작/부팅 시 재등록(`rescheduleAll`)에서 실패 항목은 로그만 남기고 DB 상태(`isEnabled`)를 변경하지 않음 | | |
| 3 | **권한 해제 시 DB 미갱신** | `LocationBasedAutoRunViewModel.kt` | L189-211 |
|   | 권한 해제 감지 시 `removeAllGeofences()`로 Geofence는 전부 제거하지만 DB의 `isEnabled`는 그대로 유지됨 | | |
| 4 | **rescheduleAll MAX 초과분 제외만** | `AutoRunGeofenceManager.kt` | L273-279 |
|   | 최대 5개 초과분은 등록 제외만 하고 DB 상태는 그대로 유지되어 불일치 발생 | | |

---

## � 리뷰 피드백 검증 결과 (2026-01-19)

| # | 리뷰 피드백 | 심각도 | 검증 결과 | 관련 코드 |
|---|------------|--------|----------|----------|
| 1 | **MAX 초과(스킵) ID의 Geofence 잔존** | ⛔ High | ✅ **확인됨** | `AutoRunGeofenceManager.kt:L283-287` |
|   | 현재 `locationsToRegister`(최대 5개)만 `removeGeofences` 호출. 스킵된 ID는 제거 대상이 아니므로 역불일치 발생 가능 | | | |
| 2 | **일시적 조건에서도 영구 비활성화 회귀 위험** | ⛔ High | ✅ **확인됨** | `AutoRunGeofenceManager.kt:L149-175` |
|   | 위치 서비스 OFF, Play Services 업데이트 중 등 일시적 조건에서도 `isEnabled=false` 처리 시 사용자 설정이 의도치 않게 꺼짐 | | | |
| 3 | **TASK-006 DI 불일치 (Repository vs DAO)** | 🟡 Medium | ✅ **확인됨** | `BootCompletedReceiver.kt:L52-59`, `DetoxyApplication.kt:L59-69` |
|   | 부팅/앱 시작 위치에서는 DAO만 주입됨. Repository 기반 코드 적용 시 DI 변경 필요 | | | |
| 4 | **권한 해제 시 removeAllGeofences() 실패 무시** | 🟡 Medium | ✅ **확인됨** | `LocationBasedAutoRunViewModel.kt:L200-203` |
|   | `removeAllGeofences()` 결과를 체크하지 않고 DB 비활성화 시 역불일치 발생 가능 | | | |
| 5 | **Phase 3 enum 필드 Room Migration/TypeConverter 누락** | 🔵 Low | ✅ **확인됨** | `LocationBasedAutoRun.kt` |
|   | enum 필드 추가 시 TypeConverter 및 Migration 필요. 현재 엔티티에는 enum이 없음 (모두 String 또는 기본 타입) | | | |

---

## 🎯 수정 방향 (리뷰 피드백 반영)

### 핵심 설계 변경: `isEnabled` (사용자 의도) vs `geofenceStatus` (실제 상태) 분리

> **권장 접근법**: 사용자가 켜둔 설정(`isEnabled`)을 임의로 끄지 않으면서, 실제 Geofence 등록 상태(`geofenceStatus`)를 별도로 추적

#### GeofenceStatus 정의
```kotlin
enum class GeofenceStatus {
    REGISTERED,           // Geofence 정상 등록
    FAILED_PERMISSION,    // 영구적 실패: 권한 거부
    FAILED_PLAY_SERVICES, // 영구적 실패: Play Services 없음
    FAILED_TEMP,          // 일시적 실패: 위치 서비스 OFF, 타임아웃 등
    SKIPPED_LIMIT         // 제한 초과로 스킵됨
}
```

#### UI 카운트 기준 변경
- **현재**: `isEnabled = true` 개수
- **변경 후**: `isEnabled = true AND geofenceStatus = REGISTERED` 개수

#### 핫픽스 최소화 대안 (권한 해제 등 "영구 조건"만 처리)
- 영구 조건(권한 해제, Play Services 없음)만 `isEnabled=false`
- 일시적 실패는 `isEnabled` 유지 + 에러 배지 표시

---

## 📌 수정된 작업 계획

### Phase 1: 권한 해제 시 DB 비활성화 처리 (안전하게 수정)

#### 1.1 사전 작업
- [x] **[CONTEXT]** 권한 해제 감지 로직 확인
  - 파일: `LocationBasedAutoRunViewModel.kt`
  - 위치: `checkPermissions()` (L189-211)

- [x] **[ANALYSIS]** Repository의 일괄 비활성화 메서드 존재 여부 확인
  - 파일: `LocationBasedAutoRunRepository.kt`
  - ✅ 확인 완료: `disableAll()` 메서드 없음 → 추가 완료

#### 1.2 본 작업

- [x] **[TASK-001]** DAO에 `disableAll()` 메서드 추가
  - 파일: `LocationBasedAutoRunDao.kt`
  - 변경:
    ```kotlin
    @Query("UPDATE location_based_auto_run SET isEnabled = 0")
    suspend fun disableAll()
    ```

- [x] **[TASK-002]** Repository에 `disableAll()` suspend 함수 추가
  - 파일: `LocationBasedAutoRunRepository.kt`
  - 변경:
    ```kotlin
    suspend fun disableAll() {
        dao.disableAll()
    }
    ```

- [x] **[TASK-003]** checkPermissions()에서 권한 해제 시 DB 일괄 비활성화 (결과 체크 포함)
  - 파일: `LocationBasedAutoRunViewModel.kt` (L189)
  - ⚠️ **리뷰 피드백 반영**: `removeAllGeofences()` 성공 여부 체크 후 DB 비활성화
  - 변경:
    ```kotlin
    if (previousHasFullPermission && !currentHasFullPermission) {
        viewModelScope.launch {
            Log.w(TAG, "⚠️ Location permission revoked")
            
            // 🆕 Geofence 제거 성공 여부 확인
            val removeResult = geofenceManager.removeAllGeofences()
            
            if (removeResult.isSuccess) {
                // Geofence 제거 성공 시에만 DB 비활성화
                repository.disableAll()
                Log.i(TAG, "✅ All geofences removed and DB disabled")
            } else {
                // Geofence 제거 실패 시 DB는 그대로 유지 (재시도 유도)
                Log.w(TAG, "⚠️ Failed to remove geofences, DB not modified")
            }
            
            _errorState.value = LocationError.PermissionError(
                "위치 권한이 해제되어 위치 기반 자동 실행이 비활성화되었습니다."
            )
        }
    }
    ```

#### 1.3 사후 작업
- [x] **[VERIFY]** 빌드 성공 확인
- [ ] **[DOC]** 작업 결과서 작성

---

### Phase 2: rescheduleAll 개선 (리뷰 피드백 전면 반영)

#### 2.1 사전 작업
- [x] **[CONTEXT]** rescheduleAll 호출 위치 확인
  - ✅ `BootCompletedReceiver.kt:L111` - DAO만 주입됨
  - ✅ `DetoxyApplication.kt:L147` - DAO만 주입됨

#### 2.2 본 작업

> ⚠️ **핵심 변경 1**: 스킵된 ID의 Geofence도 제거해야 함 (잔존 방지)
> ⚠️ **핵심 변경 2**: 실패 사유별 분기 처리 (영구적 조건만 비활성화)

- [x] **[TASK-004]** AutoRunGeofenceManager에 RescheduleResult 데이터 클래스 추가
  - 파일: `AutoRunGeofenceManager.kt`
  - 변경:
    ```kotlin
    /**
     * rescheduleAll 결과
     * 
     * @property successIds 등록 성공한 ID 목록
     * @property failedPermanentIds 영구적 실패 ID 목록 (권한, Play Services)
     * @property failedTempIds 일시적 실패 ID 목록 (위치 서비스 OFF 등)
     * @property skippedIds 제한 초과로 스킵된 ID 목록
     */
    data class RescheduleResult(
        val successIds: List<String>,
        val failedPermanentIds: List<String>,
        val failedTempIds: List<String>,
        val skippedIds: List<String>
    )
    ```

- [x] **[TASK-005]** rescheduleAll() 로직 개선 (스킵 ID Geofence 제거 + 실패 사유 분기)
  - 파일: `AutoRunGeofenceManager.kt`
  - ⚠️ **리뷰 피드백 반영**: 모든 enabled ID의 Geofence를 먼저 제거 후 상위 5개만 재등록
  - 변경:
    ```kotlin
    suspend fun rescheduleAll(enabledLocations: List<LocationBasedAutoRun>): RescheduleResult {
        val successIds = mutableListOf<String>()
        val failedPermanentIds = mutableListOf<String>()
        val failedTempIds = mutableListOf<String>()
        
        // 🆕 리뷰 피드백: 모든 enabled ID의 Geofence를 먼저 제거 (잔존 방지)
        val allEnabledIds = enabledLocations.map { it.id }
        try {
            geofencingClient.removeGeofences(allEnabledIds).await()
            Log.d(TAG, "✅ All ${allEnabledIds.size} existing geofences removed")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to remove some geofences: ${e.message}")
        }
        
        // 상위 5개만 등록, 나머지는 스킵
        val locationsToRegister = enabledLocations.take(MAX_GEOFENCES)
        val skippedIds = enabledLocations.drop(MAX_GEOFENCES).map { it.id }
        
        // 사전 조건 체크 (영구적 vs 일시적 분류용)
        val isPermanentFailure = !isPlayServicesAvailable() || !hasLocationPermission() || !hasBackgroundLocationPermission()
        
        locationsToRegister.forEach { location ->
            val result = addGeofence(location, skipCountCheck = true)
            if (result.isSuccess) {
                successIds.add(location.id)
            } else {
                // 실패 사유 분류
                if (isPermanentFailure) {
                    failedPermanentIds.add(location.id)
                } else {
                    failedTempIds.add(location.id)  // 위치 서비스 OFF 등 일시적
                }
            }
        }
        
        return RescheduleResult(
            successIds = successIds,
            failedPermanentIds = failedPermanentIds,
            failedTempIds = failedTempIds,
            skippedIds = skippedIds
        )
    }
    ```

- [x] **[TASK-006]** DAO에 `disableByIds()` 메서드 추가 (DI 변경 없이 적용 가능)
  - 파일: `LocationBasedAutoRunDao.kt`
  - ⚠️ **리뷰 피드백 반영**: Repository가 아닌 DAO 레벨에서 처리
  - 변경:
    ```kotlin
    @Query("UPDATE location_based_auto_run SET isEnabled = 0 WHERE id IN (:ids)")
    suspend fun disableByIds(ids: List<String>)
    ```

- [x] **[TASK-007]** Boot/App 시작 측에서 rescheduleAll 결과 처리
  - 파일 1: `BootCompletedReceiver.kt` (L111 부근)
  - 파일 2: `DetoxyApplication.kt` (L147 부근)
  - ⚠️ **리뷰 피드백 반영**: 영구적 실패 + 스킵만 DB 비활성화, 일시적 실패는 유지
  - 변경:
    ```kotlin
    val result = geofenceManager.rescheduleAll(enabledLocationBasedAutoRuns)
    
    // 영구적 실패 + 스킵만 DB 비활성화 (일시적 실패는 유지)
    val idsToDisable = result.failedPermanentIds + result.skippedIds
    if (idsToDisable.isNotEmpty()) {
        locationBasedAutoRunDao.disableByIds(idsToDisable)
        Log.w(TAG, "⚠️ ${idsToDisable.size} locations disabled (permanent failure or limit)")
    }
    
    // 일시적 실패는 로그만 (다음 reschedule에서 재시도)
    if (result.failedTempIds.isNotEmpty()) {
        Log.w(TAG, "⚠️ ${result.failedTempIds.size} locations failed temporarily, will retry later")
    }
    ```

#### 2.3 사후 작업
- [x] **[VERIFY]** 빌드 성공 확인
- [ ] **[DOC]** 작업 결과서 작성

---

### Phase 3 (선택): UI 등록 상태 배지 추가 + geofenceStatus 필드

> ⚠️ **주의**: Room Migration + TypeConverter 필요 (Low 위험)

#### 3.1 사전 작업
- [ ] **[CONTEXT]** 현재 DB 버전 확인
- [ ] **[ANALYSIS]** TypeConverter 사용 방식 확인

#### 3.2 본 작업 (선택)

- [ ] **[TASK-008]** GeofenceStatus enum 및 TypeConverter 추가
  - 파일: `Converters.kt` (또는 새 파일)
  - 변경:
    ```kotlin
    enum class GeofenceStatus {
        REGISTERED,
        FAILED_PERMISSION,
        FAILED_PLAY_SERVICES,
        FAILED_TEMP,
        SKIPPED_LIMIT
    }
    
    class GeofenceStatusConverter {
        @TypeConverter
        fun fromGeofenceStatus(status: GeofenceStatus?): String? {
            return status?.name
        }
        
        @TypeConverter
        fun toGeofenceStatus(value: String?): GeofenceStatus? {
            return value?.let { GeofenceStatus.valueOf(it) }
        }
    }
    ```

- [ ] **[TASK-009]** LocationBasedAutoRun 엔티티에 필드 추가 + Migration 작성
  - 파일: `LocationBasedAutoRun.kt`
  - 변경:
    ```kotlin
    val geofenceStatus: GeofenceStatus = GeofenceStatus.REGISTERED,
    val geofenceStatusReason: String? = null,
    val geofenceStatusUpdatedAt: Long = System.currentTimeMillis()
    ```
  - Migration:
    ```kotlin
    val MIGRATION_X_Y = object : Migration(X, Y) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN geofenceStatus TEXT NOT NULL DEFAULT 'REGISTERED'")
            database.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN geofenceStatusReason TEXT")
            database.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN geofenceStatusUpdatedAt INTEGER NOT NULL DEFAULT 0")
        }
    }
    ```

- [ ] **[TASK-010]** UI에 실패/제외 상태 배지 표시
  - 파일: `ScheduleTabScreen.kt`
  - 변경: 카운트를 `geofenceStatus = REGISTERED` 기준으로 표시

#### 3.3 사후 작업
- [ ] **[VERIFY]** 빌드 성공 확인
- [ ] **[TEST]** UI 변경 확인
- [ ] **[DOC]** 작업 결과서 작성

---

## 🚨 위험 요소 및 대응 (리뷰 피드백 반영)

| 위험 | 영향 | 대응 방안 | 상태 |
|------|------|----------|------|
| MAX 초과 ID Geofence 잔존 | ⛔ High | 모든 enabled ID의 Geofence를 먼저 제거 후 재등록 | � 계획 반영 |
| 일시적 조건에서 영구 비활성화 | ⛔ High | 실패 사유별 분기 (영구적 조건만 비활성화) | 📝 계획 반영 |
| BootReceiver/App DI 불일치 | 🟡 Medium | DAO 레벨에서 `disableByIds()` 추가 | 📝 계획 반영 |
| removeAllGeofences() 실패 시 역불일치 | 🟡 Medium | 결과 체크 후 DB 비활성화 진행 | 📝 계획 반영 |
| Phase 3 Room Migration/TypeConverter | � Low | String 기반 TypeConverter + Migration 작성 | 📝 계획 반영 |

---

## ✅ 최종 체크리스트

### Phase 1 검증
- [ ] 권한 설정에서 위치 권한 해제 후 앱 복귀
- [ ] `removeAllGeofences()` 성공 시에만 DB 비활성화 확인
- [ ] UI "위치 모니터링: 0/5" 확인
- [ ] 에러 메시지 표시 확인

### Phase 2 검증
- [ ] 앱 재시작 시 모든 enabled ID의 Geofence 제거 후 재등록 확인
- [ ] Play Services 사용 불가 환경에서 영구 실패로 분류되어 DB 비활성화 확인
- [ ] 위치 서비스 OFF 환경에서 일시적 실패로 분류되어 DB 유지 확인
- [ ] 6개 이상 활성화 상태에서 초과분 비활성화 + Geofence 제거 확인

### Phase 3 검증 (선택)
- [ ] DB Migration 정상 적용 확인
- [ ] TypeConverter 동작 확인
- [ ] 등록 실패 위치에 상태 배지 표시 확인

---

## 📊 진행 상태

| Phase | 작업 내용 | 상태 |
|-------|----------|------|
| Phase 1 | 권한 해제 시 DB 비활성화 (결과 체크 포함) | ✅ 완료 |
| Phase 2 | rescheduleAll 개선 (스킵 ID 제거 + 실패 사유 분기) | ✅ 완료 |
| Phase 3 | geofenceStatus 필드 + UI 배지 (선택) | ⬜ 검토 필요 |

---

## 📝 관련 문서

- 이전 핫픽스: `위치기반_개수제한_핫픽스_2026-01-13.md`
- 작업 결과서: `working_history/version_1.1/99_hotfix/hotfix_v1.1.2_DB_Geofence_불일치_개선_2026-01-19.md`
- 관련 대화: Refactor Geofence Management (2e40ec6b-3822-472d-a6fb-dab0557ba0a9)

---

**작성자**: Antigravity AI  
**작성일**: 2026-01-18  
**수정일**: 2026-01-19 (작업 완료)  
**상태**: ✅ Phase 1, 2 완료 (Phase 3 선택)
