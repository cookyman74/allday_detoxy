# 2차 고도화 Firebase Analytics 로깅 스키마

**버전**: v0.6  
**작성일**: 2025-10-20  
**기준 문서**: [2차 고도화 PRD](./02_advanced_autosetting_prd.md)  
**참조**: [1차 고도화 Analytics](./01_advanced_analytics_schema.md)

---

## 1. 개요

### 1.1 목적
- 자동 실행 기능(시간/위치 기반) 사용 패턴 분석
- 커스텀 타이머 UI 인터랙션 데이터 수집
- 권한 승인율 및 전환율 추적
- 자동 실행 효과성 검증

### 1.2 로깅 원칙
- **명명 규칙**: `snake_case` (Firebase 권장)
- **개인정보 보호**:
  - ❌ 위치 좌표 수집 금지 (주소도 수집 안 함)
  - ❌ 위치 라벨만 "location_label_hash" (HMAC-SHA256 + 고정 솔트) 🆕
  - ❌ 프리셋 이름 수집 금지 (길이만 기록) 🆕
  - ✅ 패키지명, 카테고리, 통계 데이터만 수집
- **데이터 보존**: Firebase Analytics 14개월 (자동)
- **파라미터 제한**: Firebase 이벤트당 최대 25개 파라미터 제한 준수 🆕

### 1.3 이벤트 분류

```
auto_run_*              // 자동 실행 관련 (14개)
custom_timer_*          // 커스텀 타이머 관련 (5개)
permission_*            // 권한 관련 (2개)
```

### 1.4 총 이벤트 수
- **2차 고도화 신규**: 21개 이벤트
- **기존 이벤트**: 유지 (1차 고도화 19개)
- **총계**: 40개 이벤트

---

## 2. 자동 실행 설정 이벤트 (8개)

### 2.1 `auto_run_time_created`

**목적**: 시간 기반 자동 실행 생성 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `hour` | number | 시간 (0-23) | `9` |
| `minute` | number | 분 (0-59) | `0` |
| `duration_minutes` | number | 타이머 시간 | `45` |
| `preset_type` | string | 차단 프리셋 | `"STANDARD"` |
| `enabled_days_count` | number | 활성화 요일 수 | `5` |
| `has_label` | boolean | 라벨 설정 여부 | `true` |
| `is_from_template` | boolean | 템플릿으로 생성 여부 | `false` |
| `template_type` | string | 템플릿 종류 (있을 경우) | `"work_focus"` |

**로깅 코드**:
```kotlin
AnalyticsHelper.logTimeBasedAutoRunCreated(
    hour = 9,
    minute = 0,
    durationMinutes = 45,
    presetType = "STANDARD",
    enabledDaysCount = 5,
    hasLabel = true,
    isFromTemplate = false,
    templateType = null
)
```

---

### 2.2 `auto_run_time_edited`

**목적**: 시간 기반 자동 실행 편집 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `changed_fields` | string | 변경된 필드 (쉼표 구분) | `"hour,minute,duration"` |
| `new_duration_minutes` | number | 새 타이머 시간 | `60` |
| `new_enabled_days_count` | number | 새 활성화 요일 수 | `3` |

---

### 2.3 `auto_run_time_toggled`

**목적**: 시간 기반 자동 실행 활성화/비활성화 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `is_enabled` | boolean | 활성화 여부 | `true` |
| `total_enabled_count` | number | 활성화된 시간대 총 수 | `3` |

---

### 2.4 `auto_run_time_deleted`

**목적**: 시간 기반 자동 실행 삭제 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `usage_count` | number | 삭제 전 사용 횟수 | `12` |
| `days_active` | number | 활성화 기간 (일) | `7` |

---

### 2.5 `auto_run_location_created`

**목적**: 위치 기반 자동 실행 생성 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `location_label_hash` | string | 위치 라벨 해시 (HMAC-SHA256) 🆕 | `"a1b2c3..."` |
| `radius_meters` | number | 반경 (m) | `100` |
| `duration_minutes` | number | 타이머 시간 | `45` |
| `preset_type` | string | 차단 프리셋 | `"STANDARD"` |
| `trigger_type` | string | 트리거 타입 | `"ENTER"` |
| `dwell_time_minutes` | number | 체류 시간 (분) | `1` |
| `requires_confirmation` | boolean | 도착 후 확인 필요 | `false` |

**참고**:
- `location_label_hash`는 "회사", "도서관" 등 라벨만 해시 처리
- **해시 알고리즘** 🆕: HMAC-SHA256 + 앱 고유 솔트 사용
  - 단순 SHA-256은 짧은 문자열 역추적 가능 (Rainbow Table)
  - HMAC을 사용하여 솔트 없이는 역추적 불가능
  - 솔트는 앱 빌드 시 난수 생성 후 BuildConfig에 저장
- 실제 주소/좌표는 절대 수집하지 않음

---

### 2.6 `auto_run_location_edited`

**목적**: 위치 기반 자동 실행 편집 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `changed_fields` | string | 변경된 필드 | `"radius,duration"` |
| `new_radius_meters` | number | 새 반경 | `200` |
| `new_dwell_time_minutes` | number | 새 체류 시간 | `3` |

---

### 2.7 `auto_run_location_toggled`

**목적**: 위치 기반 자동 실행 활성화/비활성화 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `is_enabled` | boolean | 활성화 여부 | `true` |
| `total_enabled_count` | number | 활성화된 위치 총 수 | `2` |

---

### 2.8 `auto_run_location_deleted`

**목적**: 위치 기반 자동 실행 삭제 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `usage_count` | number | 삭제 전 사용 횟수 | `15` |
| `success_rate` | number | 성공률 (0-100) | `85` |
| `days_active` | number | 활성화 기간 (일) | `14` |

---

## 3. 자동 실행 트리거 및 결과 이벤트 (6개)

### 3.1 `auto_run_triggered`

**목적**: 자동 실행 트리거 발생 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `trigger_type` | string | 트리거 타입 | `"TIME"`, `"LOCATION"` |
| `source_id_hash` | string | 소스 ID 해시 | `"xyz123..."` |
| `duration_minutes` | number | 타이머 시간 | `45` |
| `preset_type` | string | 차단 프리셋 | `"STANDARD"` |

---

### 3.2 `auto_run_notification_shown`

**목적**: 자동 실행 알림 표시 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `trigger_type` | string | 트리거 타입 | `"TIME"` |
| `is_pre_notification` | boolean | 사전 알림 여부 | `true` |
| `minutes_before` | number | 사전 알림 시간 (분) | `5` |

---

### 3.3 `auto_run_notification_action`

**목적**: 자동 실행 알림 액션 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `action` | string | 액션 타입 | `"START"`, `"SNOOZE"`, `"SKIP"` |
| `trigger_type` | string | 트리거 타입 | `"TIME"` |
| `response_time_seconds` | number | 응답 시간 (초) | `15` |

---

### 3.4 `auto_run_started`

**목적**: 자동 실행으로 타이머 시작 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `trigger_type` | string | 트리거 타입 | `"LOCATION"` |
| `duration_minutes` | number | 타이머 시간 | `45` |
| `is_auto_start` | boolean | 자동 시작 여부 | `true` |
| `delay_seconds` | number | 지연 시간 (초) | `300` |

---

### 3.5 `auto_run_skipped`

**목적**: 자동 실행 건너뜀 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `trigger_type` | string | 트리거 타입 | `"TIME"` |
| `reason` | string | 건너뜀 이유 | `"USER_SKIP"`, `"TIMER_RUNNING"`, `"PAUSED"` |

---

### 3.6 `auto_run_failed`

**목적**: 자동 실행 실패 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `trigger_type` | string | 트리거 타입 | `"LOCATION"` |
| `failure_reason` | string | 실패 사유 | `"PERMISSION_DENIED"`, `"LOCATION_DISABLED"`, `"GEOFENCE_ERROR"` |

---

## 4. 커스텀 타이머 이벤트 (5개)

### 4.1 `custom_timer_adjusted`

**목적**: 도넛 그래프로 시간 조정 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `duration_minutes` | number | 조정된 시간 | `37` |
| `method` | string | 조정 방법 | `"DRAG"`, `"TAP"` |
| `is_custom_time` | boolean | 프리셋 외 시간 여부 | `true` |

---

### 4.2 `custom_preset_created`

**목적**: 커스텀 프리셋 생성 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `name_length` | number | 프리셋 이름 길이 (개인정보 보호) 🆕 | `4` |
| `duration_minutes` | number | 시간 | `37` |
| `has_preset_type` | boolean | 차단 프리셋 연결 여부 | `true` |
| `total_custom_count` | number | 총 커스텀 프리셋 수 | `5` |

**개인정보 보호** 🆕:
- 프리셋 이름(name)은 수집하지 않음 (사용자 개인 정보 포함 가능)
- 대신 `name_length`로 이름 길이만 기록
- 통계 분석: "평균 프리셋 이름 길이 5.2자" 등으로 활용

---

### 4.3 `custom_preset_used`

**목적**: 커스텀 프리셋 사용 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `preset_id_hash` | string | 프리셋 ID 해시 | `"abc123..."` |
| `duration_minutes` | number | 시간 | `37` |
| `usage_count` | number | 누적 사용 횟수 | `12` |

---

### 4.4 `custom_preset_edited`

**목적**: 커스텀 프리셋 편집 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `changed_fields` | string | 변경된 필드 | `"name,duration"` |
| `new_duration_minutes` | number | 새 시간 | `40` |
| `new_name_length` | number | 새 이름 길이 (name 변경 시) 🆕 | `6` |

---

### 4.5 `custom_preset_deleted`

**목적**: 커스텀 프리셋 삭제 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `usage_count` | number | 삭제 전 사용 횟수 | `3` |
| `days_active` | number | 생성 후 경과 일수 | `7` |

---

## 5. 권한 및 대시보드 이벤트 (2개)

### 5.1 `permission_exact_alarm_requested`

**목적**: 정확 알람 권한 요청 추적 (Android 12+)

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `source` | string | 요청 경로 | `"time_based_settings"`, `"permission_status_screen"` |
| `is_granted` | boolean | 승인 여부 | `true` |
| `fallback_to_workmanager` | boolean | WorkManager fallback 여부 | `false` |
| `went_to_settings` | boolean | 설정 화면 이동 여부 🆕 | `true` |

**이벤트 분석 개선** 🆕:
- `went_to_settings=true` & `is_granted=false`: 설정 이동했으나 허용 안 함 (유도 실패)
- `went_to_settings=false` & `is_granted=false`: 다이얼로그에서 거부
- 이를 통해 권한 요청 UX 개선 포인트 발견 가능

---

### 5.2 `permission_background_location_requested`

**목적**: 백그라운드 위치 권한 요청 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `source` | string | 요청 경로 | `"location_based_settings"` |
| `is_granted` | boolean | 승인 여부 | `true` |
| `permission_level` | string | 권한 레벨 | `"ALWAYS"`, `"WHILE_IN_USE"`, `"DENIED"` |

---

## 6. 주요 KPI 및 지표

### 6.1 자동 실행 전환율

**정의**: 자동 실행 설정 사용자 비율

**계산식**:
```
자동 실행 설정자 / DAU * 100
```

**목표**: ≥ 40% (PRD §8.1)

**관련 이벤트**:
- `auto_run_time_created`
- `auto_run_location_created`

---

### 6.2 자동 실행 성공률

**정의**: 자동 실행 트리거 성공 비율

**계산식**:
```
auto_run_started / (auto_run_started + auto_run_skipped + auto_run_failed) * 100
```

**목표**: ≥ 60% (PRD §8.2)

**관련 이벤트**:
- `auto_run_started`
- `auto_run_skipped`
- `auto_run_failed`

---

### 6.3 커스텀 타이머 사용률

**정의**: 타이머 화면 진입자 중 커스텀 시간 설정 비율

**계산식**:
```
custom_timer_adjusted (is_custom_time=true) / timer_screen_viewed * 100
```

**목표**: ≥ 50% (PRD §8.1)

**관련 이벤트**:
- `custom_timer_adjusted`
- `custom_preset_created`

---

### 6.4 위치 권한 승인율

**정의**: 백그라운드 위치 권한 승인 비율

**계산식**:
```
permission_background_location_requested (is_granted=true) / 
permission_background_location_requested (전체) * 100
```

**목표**: ≥ 50% (PRD §8.1)

**관련 이벤트**:
- `permission_background_location_requested`

---

## 7. AnalyticsHelper 클래스 확장

### 7.1 메서드 목록

**파일**: `app/src/main/java/com/allday/detoxy/core/utils/AnalyticsHelper.kt`

```kotlin
object AnalyticsHelper {
    
    // 기존 메서드 (1차 고도화)
    // ... (19개 메서드)
    
    // ─── 2차 고도화 신규 메서드 (21개) ───
    
    // 자동 실행 설정 (8개)
    fun logTimeBasedAutoRunCreated(
        hour: Int,
        minute: Int,
        durationMinutes: Int,
        presetType: String,
        enabledDaysCount: Int,
        hasLabel: Boolean,
        isFromTemplate: Boolean,
        templateType: String?
    )
    
    fun logTimeBasedAutoRunEdited(
        changedFields: String,
        newDurationMinutes: Int,
        newEnabledDaysCount: Int
    )
    
    fun logTimeBasedAutoRunToggled(
        isEnabled: Boolean,
        totalEnabledCount: Int
    )
    
    fun logTimeBasedAutoRunDeleted(
        usageCount: Int,
        daysActive: Int
    )
    
    fun logLocationBasedAutoRunCreated(
        labelHash: String,
        radiusMeters: Int,
        durationMinutes: Int,
        presetType: String,
        triggerType: String,
        dwellTimeMinutes: Int,
        requiresConfirmation: Boolean
    )
    
    fun logLocationBasedAutoRunEdited(
        changedFields: String,
        newRadiusMeters: Int,
        newDwellTimeMinutes: Int
    )
    
    fun logLocationBasedAutoRunToggled(
        isEnabled: Boolean,
        totalEnabledCount: Int
    )
    
    fun logLocationBasedAutoRunDeleted(
        usageCount: Int,
        successRate: Int,
        daysActive: Int
    )
    
    // 자동 실행 트리거 (6개)
    fun logAutoRunTriggered(
        triggerType: String,
        sourceIdHash: String,
        durationMinutes: Int,
        presetType: String
    )
    
    fun logAutoRunNotificationShown(
        triggerType: String,
        isPreNotification: Boolean,
        minutesBefore: Int?
    )
    
    fun logAutoRunNotificationAction(
        action: String,
        triggerType: String,
        responseTimeSeconds: Int
    )
    
    fun logAutoRunStarted(
        triggerType: String,
        durationMinutes: Int,
        isAutoStart: Boolean,
        delaySeconds: Int
    )
    
    fun logAutoRunSkipped(
        triggerType: String,
        reason: String
    )
    
    fun logAutoRunFailed(
        triggerType: String,
        failureReason: String
    )
    
    // 커스텀 타이머 (5개)
    fun logCustomTimerAdjusted(
        durationMinutes: Int,
        method: String,
        isCustomTime: Boolean
    )
    
    fun logCustomPresetCreated(
        name: String,
        durationMinutes: Int,
        hasPresetType: Boolean,
        totalCustomCount: Int
    )
    
    fun logCustomPresetUsed(
        presetIdHash: String,
        durationMinutes: Int,
        usageCount: Int
    )
    
    fun logCustomPresetEdited(
        changedFields: String,
        newDurationMinutes: Int
    )
    
    fun logCustomPresetDeleted(
        usageCount: Int,
        daysActive: Int
    )
    
    // 권한 (2개)
    fun logPermissionExactAlarmRequested(
        source: String,
        isGranted: Boolean,
        fallbackToWorkmanager: Boolean
    )
    
    fun logPermissionBackgroundLocationRequested(
        source: String,
        isGranted: Boolean,
        permissionLevel: String
    )
}
```

### 7.2 구현 예시

```kotlin
fun logTimeBasedAutoRunCreated(
    hour: Int,
    minute: Int,
    durationMinutes: Int,
    presetType: String,
    enabledDaysCount: Int,
    hasLabel: Boolean,
    isFromTemplate: Boolean,
    templateType: String?
) {
    FirebaseAnalytics.getInstance(context).logEvent("auto_run_time_created") {
        param("hour", hour.toLong())
        param("minute", minute.toLong())
        param("duration_minutes", durationMinutes.toLong())
        param("preset_type", presetType)
        param("enabled_days_count", enabledDaysCount.toLong())
        param("has_label", if (hasLabel) 1L else 0L)
        param("is_from_template", if (isFromTemplate) 1L else 0L)
        templateType?.let { param("template_type", it) }
    }
}

// 위치 라벨 해시 함수 예시 🆕
private fun hashLocationLabel(label: String): String {
    val salt = BuildConfig.ANALYTICS_SALT // 앱 빌드 시 생성된 고유 솔트
    val mac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(salt.toByteArray(), "HmacSHA256")
    mac.init(secretKey)
    val hash = mac.doFinal(label.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}
```

**솔트 생성 및 관리** 🆕:
```gradle
// build.gradle.kts
android {
    defaultConfig {
        // 빌드 시 난수 생성 (한 번만)
        buildConfigField("String", "ANALYTICS_SALT", "\"${generateRandomSalt()}\"")
    }
}

fun generateRandomSalt(): String {
    return UUID.randomUUID().toString()
}
```

---

## 8. 개인정보 보호 준수 사항

### 8.1 Google Play 정책

**수집 데이터 선언** (Play Console → 데이터 보안):
- ✅ 앱 사용 데이터 (타이머 사용, 프리셋 선택)
- ✅ 위치 데이터 타입: "대략적 위치" (라벨 해시만)
- ❌ 정확한 위치 좌표 수집 안 함
- ❌ 주소 수집 안 함

### 8.2 GDPR 준수

- **데이터 최소화**: 필요한 최소한의 데이터만 수집
- **익명화**: 모든 ID, 라벨 해시 처리
- **사용자 동의**: 첫 실행 시 Analytics 동의 다이얼로그 (선택)
- **삭제 권리**: Firebase User ID 기반 데이터 삭제 지원

### 8.3 한국 개인정보보호법 준수

- **위치정보법 준수**:
  - 위치 좌표 수집 안 함 (해시만)
  - 위치 기반 서비스 약관 제공
  - 사용자 명시적 동의 필수

---

## 9. 테스트 및 검증

### 9.1 로깅 테스트 체크리스트

- [ ] 모든 이벤트 파라미터 정확성 확인
- [ ] Firebase Console에서 이벤트 수신 확인 (24시간 이내)
- [ ] 개인정보 누락 검증 (위치 좌표, 주소 등)
- [ ] 해시 함수 동일성 확인 (같은 라벨 → 같은 해시)
- [ ] 권한 승인/거부 시나리오 테스트

### 9.2 BigQuery 연동 (선택)

**쿼리 예시** (자동 실행 성공률):
```sql
SELECT
  COUNT(CASE WHEN event_name = 'auto_run_started' THEN 1 END) AS started,
  COUNT(CASE WHEN event_name = 'auto_run_skipped' THEN 1 END) AS skipped,
  COUNT(CASE WHEN event_name = 'auto_run_failed' THEN 1 END) AS failed,
  ROUND(
    COUNT(CASE WHEN event_name = 'auto_run_started' THEN 1 END) * 100.0 /
    (COUNT(CASE WHEN event_name IN ('auto_run_started', 'auto_run_skipped', 'auto_run_failed') THEN 1 END)),
    2
  ) AS success_rate
FROM
  `project.analytics_dataset.events_*`
WHERE
  _TABLE_SUFFIX BETWEEN '20251020' AND '20251027'
  AND event_name IN ('auto_run_started', 'auto_run_skipped', 'auto_run_failed')
```

---

## 10. 마이그레이션 체크리스트

### 10.1 구현 체크리스트

- [ ] AnalyticsHelper에 21개 메서드 추가
- [ ] 각 화면/ViewModel에 로깅 코드 삽입
- [ ] 단위 테스트 작성 (로깅 호출 검증)
- [ ] Firebase Console에서 이벤트 수신 확인

### 10.2 검증 체크리스트

- [ ] 자동 실행 설정 → 생성/편집/삭제 이벤트 로깅
- [ ] 자동 실행 트리거 → 알림/액션/결과 이벤트 로깅
- [ ] 커스텀 타이머 → 조정/프리셋 이벤트 로깅
- [ ] 권한 요청 → 승인/거부 이벤트 로깅
- [ ] 개인정보 누락 최종 확인

---

## 11. 파라미터 관리 전략 🆕

### 11.1 Firebase 제한 사항
- **이벤트당 최대 파라미터**: 25개
- **파라미터명 최대 길이**: 40자
- **파라미터 값 최대 길이**: 100자

### 11.2 현재 사용량 모니터링
- 현재 최대 파라미터 수: 8개 (location_based_auto_run_created)
- 여유 공간: 17개

### 11.3 확장 시 전략
**우선순위 기반 파라미터 축소**:
1. **필수** (항상 유지): trigger_type, duration_minutes, result
2. **중요** (가능한 유지): preset_type, enabled_days_count, is_granted
3. **부가** (필요 시 제거): is_from_template, template_type

**대안**:
- 여러 boolean을 bitmask로 통합
- 드물게 사용되는 파라미터는 별도 이벤트로 분리

### 11.4 모니터링 및 리뷰
- 분기별 파라미터 사용량 리뷰
- BigQuery 쿼리 빈도 분석 → 사용되지 않는 파라미터 제거
- 문서화: "파라미터 추가 시 §11 검토 필수" 명시

---

## 부록: 참조 링크

- [2차 고도화 PRD §5.2](./02_advanced_autosetting_prd.md#52-이벤트-로깅)
- [2차 고도화 작업 계획 §6.2](./02_advanced_autosetting_todolist.md#62-analytics-이벤트-로깅-day-26)
- [1차 고도화 Analytics 스키마](./01_advanced_analytics_schema.md)
- [Firebase Analytics 문서](https://firebase.google.com/docs/analytics)
- [Google Play 데이터 보안 가이드](https://support.google.com/googleplay/android-developer/answer/10787469)

---

**문서 버전**: v1.0  
**최종 수정**: 2025-10-20  
**작성자**: AI Assistant

