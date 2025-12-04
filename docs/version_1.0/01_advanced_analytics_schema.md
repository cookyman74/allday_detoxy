# Firebase Analytics 로깅 스키마

## 개요
- **작성 일자**: 2025-10-12
- **플랫폼**: Firebase Analytics (google-analytics)
- **목적**: 1차 고도화 기능 사용 패턴 분석 및 제품 개선

---

## 1. 로깅 원칙

### 1.1 명명 규칙
- **이벤트명**: `snake_case` (Firebase 권장)
- **파라미터명**: `snake_case`
- **값**: 문자열, 숫자, 불린

### 1.2 개인정보 보호
- ❌ 사용자 식별 정보 수집 금지 (이메일, 전화번호, 이름)
- ✅ 익명화된 사용자 ID만 사용 (Firebase User ID)
- ✅ 패키지명은 수집하되, 앱 내 컨텐츠는 수집하지 않음

### 1.3 데이터 보존
- Firebase Analytics: 14개월 (자동)
- 장기 분석 필요 시 BigQuery 연동 (2차 고도화)

---

## 2. 이벤트 분류 (PRD §4.1, §4.2 기준)

### 2.1 디톡시 제어 설정 관련
```
detoxy_settings_*  (구 focus_settings_*)
```
**변경 사유**: PRD §4.1에서 "디톡시 제어 설정"으로 정의, 중독 회복 관점 강조

### 2.2 리포트 및 회복률 관련
```
report_*
```

### 2.3 세션 및 루틴 관련
```
session_*
detoxy_routine_*
```

### 2.4 권한 관련
```
permission_*
usage_stats_*  (신규)
```

### 2.5 총 이벤트 수
- **1차 고도화**: 19개 이벤트 (기존 15개 + 신규 4개)
- **마이그레이션**: `focus_settings_*` → `detoxy_settings_*` (4개 이벤트)

---

## 3. 이벤트 상세 정의

### 3.1 디톡시 제어 설정 이벤트

#### `detoxy_settings_open` (구 `focus_settings_open`)
**목적**: 디톡시 제어 설정 화면 진입 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `source` | string | 진입 경로 | `"timer_screen"`, `"settings_menu"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("detoxy_settings_open") {
    param("source", "timer_screen")
}
```

**마이그레이션 노트**: Week 1 구현 시 즉시 새 이벤트명 사용

---

#### `detoxy_settings_saved` (구 `focus_settings_saved`)
**목적**: 디톡시 제어 설정 저장 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `preset` | string | 선택한 프리셋 | `"complete_block"`, `"standard"`, `"relaxed"`, `"custom"` |
| `sns_enabled` | boolean | SNS 차단 여부 | `true` |
| `messenger_enabled` | boolean | 메신저 차단 여부 | `false` |
| `web_enabled` | boolean | Web 차단 여부 | `true` |
| `video_enabled` | boolean | 영상 차단 여부 | `true` |
| `other_enabled` | boolean | 기타 앱 차단 여부 | `true` |
| `total_enabled_count` | number | 활성화된 카테고리 수 | `4` |
| `risk_index` | number | 현재 위험 지수 (0-100) | `32` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("detoxy_settings_saved") {
    param("preset", "complete_block")  // 프리셋명 변경
    param("sns_enabled", true)
    param("messenger_enabled", false)
    param("web_enabled", true)
    param("video_enabled", true)
    param("other_enabled", true)
    param("total_enabled_count", 4)
    param("risk_index", 32)  // 신규 파라미터
}
```

**프리셋 값 변경**:
- `"full_block"` → `"complete_block"` (완전 차단)
- `"focus"` → `"standard"` (표준 디톡시)
- `"relaxed"` → `"relaxed"` (유지)

---

#### `detoxy_settings_category_toggle` (구 `focus_settings_category_toggle`)
**목적**: 개별 카테고리 토글 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `category` | string | 카테고리명 | `"sns"`, `"messenger"`, `"web"`, `"video"`, `"other"` |
| `enabled` | boolean | 활성화 여부 | `true` |
| `dialog_shown` | boolean | 메신저 안내 다이얼로그 표시 여부 | `true` (messenger 카테고리만) |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("detoxy_settings_category_toggle") {
    param("category", "messenger")
    param("enabled", true)
    param("dialog_shown", true)
}
```

---

#### `detoxy_settings_preset_selected` (구 `focus_settings_preset_selected`)
**목적**: 프리셋 선택 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `preset` | string | 선택한 프리셋 | `"complete_block"`, `"standard"`, `"relaxed"` |
| `previous_preset` | string | 이전 프리셋 | `"standard"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("detoxy_settings_preset_selected") {
    param("preset", "complete_block")
    param("previous_preset", "standard")
}
```

---

### 3.2 리포트 이벤트

#### `report_view_daily`
**목적**: 일간 리포트 조회 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `has_data` | boolean | 데이터 존재 여부 | `true` |
| `session_count` | number | 오늘 세션 수 | `3` |
| `success_count` | number | 성공 세션 수 | `2` |
| `total_focus_minutes` | number | 총 집중 시간 (분) | `75` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_view_daily") {
    param("has_data", true)
    param("session_count", 3)
    param("success_count", 2)
    param("total_focus_minutes", 75)
}
```

---

#### `report_view_weekly`
**목적**: 주간 리포트 조회 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `has_data` | boolean | 데이터 존재 여부 | `true` |
| `week_session_count` | number | 주간 세션 수 | `15` |
| `week_success_count` | number | 주간 성공 수 | `10` |
| `week_focus_minutes` | number | 주간 총 집중 시간 | `375` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_view_weekly") {
    param("has_data", true)
    param("week_session_count", 15)
    param("week_success_count", 10)
    param("week_focus_minutes", 375)
}
```

---

#### `report_insight_expand`
**목적**: 인사이트 카드 확장 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `insight_type` | string | 인사이트 타입 | `"risk_index"`, `"top_distractions"`, `"coach_recommendation"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_insight_expand") {
    param("insight_type", "top_distractions")
}
```

---

#### `report_risk_index_calculated`
**목적**: 디톡시 위험 지수 계산 추적 (중독 진단)

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `risk_index` | number | 위험 지수 (0-100) | `32` |
| `risk_level` | string | 위험 단계 | `"recovery"`, `"warning"`, `"high_risk"` |
| `trend` | string | 추세 | `"improving"`, `"stable"`, `"worsening"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_risk_index_calculated") {
    param("risk_index", 32)
    param("risk_level", "recovery")  // 회복 단계
    param("trend", "improving")
}
```

**위험 단계 매핑** (PRD §4.2 기준):
- `"recovery"`: 0-30 (회복 중)
- `"warning"`: 31-60 (주의)
- `"high_risk"`: 61-100 (고위험, 중독 재발 가능성)

---

#### `report_recovery_rate_calculated` (신규)
**목적**: 디톡시 회복률 계산 추적 (PRD §4.2)

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `recovery_rate` | number | 회복률 (0-100%) | `72` |
| `previous_rate` | number | 이전 회복률 | `65` |
| `change` | number | 변화량 (%p) | `+7` |
| `period` | string | 계산 기간 | `"weekly"`, `"monthly"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_recovery_rate_calculated") {
    param("recovery_rate", 72)
    param("previous_rate", 65)
    param("change", 7)
    param("period", "weekly")
}
```

**계산 로직**:
- 회복률 = (성공 세션 / 전체 세션) × 100
- 주간 추세: 지난주 대비 변화율
- PRD §4.2.2에 정의된 핵심 지표

---

#### `report_coach_recommendation_shown`
**목적**: 코치 추천 메시지 노출 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `risk_level` | string | 위험 단계 | `"low"`, `"medium"`, `"high"` |
| `recommendation_type` | string | 추천 타입 | `"maintain"`, `"increase_duration"`, `"enable_category"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_coach_recommendation_shown") {
    param("risk_level", "medium")
    param("recommendation_type", "increase_duration")
}
```

---

### 3.3 세션 이벤트 (기존 + 확장)

#### `session_started` (기존)
**파라미터 확장**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `duration_minutes` | number | 목표 시간 | `25` |
| `sns_enabled` | boolean | SNS 차단 여부 | `true` |
| `messenger_enabled` | boolean | 메신저 차단 여부 | `false` |
| `web_enabled` | boolean | Web 차단 여부 | `true` |
| `video_enabled` | boolean | 영상 차단 여부 | `true` |
| `other_enabled` | boolean | 기타 차단 여부 | `true` |

---

#### `session_interrupted` (신규)
**목적**: 세션 중 차단 이벤트 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `session_id` | string | 세션 ID | `"uuid-..."` |
| `category` | string | 차단된 앱 카테고리 | `"sns"`, `"web"`, `"video"` |
| `elapsed_seconds` | number | 경과 시간 (초) | `600` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("session_interrupted") {
    param("session_id", sessionId)
    param("category", "sns")
    param("elapsed_seconds", 600)
}
```

---

#### `session_give_up` (확장)
**파라미터 확장**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `duration_minutes` | number | 목표 시간 | `25` |
| `elapsed_seconds` | number | 경과 시간 (초) | `780` |
| `interruption_count` | number | 차단 이벤트 수 | `3` |
| `primary_distraction` | string | 주요 방해요인 | `"sns"` |
| `reason` | string | 포기 사유 | `"user_cancel"`, `"permission_revoked"` |

---

#### `detoxy_routine_completed` (신규)
**목적**: 디톡시 루틴 완료 추적 (PRD §5.3)

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `routine_type` | string | 루틴 타입 | `"morning"`, `"lunch"`, `"evening"` |
| `scheduled_time` | string | 예정 시간 | `"09:00"` |
| `actual_time` | string | 실제 실행 시간 | `"09:05"` |
| `duration_minutes` | number | 디톡시 시간 | `25` |
| `success` | boolean | 성공 여부 | `true` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("detoxy_routine_completed") {
    param("routine_type", "morning")
    param("scheduled_time", "09:00")
    param("actual_time", "09:05")
    param("duration_minutes", 25)
    param("success", true)
}
```

**루틴 실패 케이스**:
```kotlin
// 사용자가 루틴을 건너뛴 경우
FirebaseAnalytics.getInstance(context).logEvent("detoxy_routine_completed") {
    param("routine_type", "lunch")
    param("scheduled_time", "12:00")
    param("actual_time", "null")  // 실행하지 않음
    param("duration_minutes", 0)
    param("success", false)
}
```

---

### 3.4 권한 이벤트

#### `permission_request_shown`
**목적**: 권한 요청 다이얼로그 노출 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `permission_type` | string | 권한 타입 | `"accessibility"`, `"overlay"`, `"dnd"` |
| `source` | string | 요청 출처 | `"onboarding"`, `"timer_start"`, `"settings"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("permission_request_shown") {
    param("permission_type", "accessibility")
    param("source", "onboarding")
}
```

---

#### `permission_granted`
**목적**: 권한 허용 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `permission_type` | string | 권한 타입 | `"accessibility"`, `"overlay"`, `"dnd"`, `"usage_stats"` |

---

#### `usage_stats_opt_in` (신규)
**목적**: UsageStats 권한 동의 추적 (PRD §5.2)

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `source` | string | 동의 요청 출처 | `"onboarding"`, `"report_screen"`, `"settings"` |
| `granted` | boolean | 권한 허용 여부 | `true` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("usage_stats_opt_in") {
    param("source", "report_screen")
    param("granted", true)
}
```

**사용 시나리오**:
- 리포트 화면에서 "더 정확한 통계를 위해 UsageStats 권한 필요" 안내
- 사용자가 "허용" 선택 시 `granted = true`
- 사용자가 "거부" 선택 시 `granted = false`

---

#### `usage_stats_opt_out` (신규)
**목적**: UsageStats 권한 거부/철회 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `reason` | string | 거부 사유 | `"privacy_concern"`, `"not_needed"`, `"user_revoked"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("usage_stats_opt_out") {
    param("reason", "privacy_concern")
}
```

**프라이버시 준수**:
- UsageStats는 선택 권한 (필수 아님)
- 사용자가 언제든 철회 가능
- 거부 시에도 기본 리포트 제공 (FocusSession 기반)

---

#### `permission_denied`
**목적**: 권한 거부 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `permission_type` | string | 권한 타입 | `"accessibility"`, `"overlay"`, `"dnd"` |

---

## 4. 사용자 속성 (User Properties)

### 4.1 정의
```kotlin
// 총 세션 수
FirebaseAnalytics.getInstance(context).setUserProperty("total_sessions", "50")

// 현재 스트릭
FirebaseAnalytics.getInstance(context).setUserProperty("current_streak", "7")

// 선호 프리셋
FirebaseAnalytics.getInstance(context).setUserProperty("preferred_preset", "full_block")

// 평균 집중 시간
FirebaseAnalytics.getInstance(context).setUserProperty("avg_focus_minutes", "35")
```

---

## 5. Funnel 분석

### 5.1 온보딩 Funnel
```
1. app_open (첫 실행)
2. onboarding_start
3. permission_request_shown (accessibility)
4. permission_granted (accessibility)
5. permission_request_shown (overlay)
6. permission_granted (overlay)
7. onboarding_complete
```

### 5.2 설정 Funnel (업데이트됨)
```
1. detoxy_settings_open  (구 focus_settings_open)
2. detoxy_settings_preset_selected
3. detoxy_settings_category_toggle (여러 번 가능)
4. detoxy_settings_saved
```

### 5.3 세션 및 루틴 Funnel (확장됨)
```
1. session_started
2. session_interrupted (0회 이상)
3. session_completed OR session_give_up
4. report_view_daily
5. report_risk_index_calculated
6. report_recovery_rate_calculated (신규)
7. detoxy_routine_completed (루틴 사용자만)
```

---

## 6. 핵심 지표 (KPI)

### 6.1 사용자 참여도
- **DAU (Daily Active Users)**: `app_open` 이벤트 발생 사용자 수
- **세션 시작률**: `session_started` / DAU
- **설정 변경률**: `detoxy_settings_saved` / DAU (업데이트)
- **루틴 준수율**: `detoxy_routine_completed(success=true)` / 전체 루틴 (신규)

### 6.2 기능 채택률
- **카테고리 활용률**: 
  - SNS 차단 사용자 비율: `sns_enabled=true` / 전체 사용자
  - 메신저 차단 사용자 비율: `messenger_enabled=true` / 전체 사용자
- **프리셋 분포** (업데이트):
  - 완전 차단: X% (`preset="complete_block"`)
  - 표준 디톡시: Y% (`preset="standard"`)
  - 완화: Z% (`preset="relaxed"`)
  - 커스텀: W%
- **UsageStats 동의율**: `usage_stats_opt_in(granted=true)` / 전체 사용자 (신규)

### 6.3 성과 지표 (디톡시/회복 관점)
- **디톡시 성공률**: `session_completed` / `session_started`
- **회복률 추세**: `report_recovery_rate_calculated` 주간 변화량 (신규)
- **평균 디톡시 시간**: `total_focus_minutes` 평균
- **방해요인 차단 효과**: `session_interrupted` 발생률
- **중독 위험 감소율**: 주간 `risk_index` 변화량 (신규)

### 6.4 리포트 지표
- **리포트 조회율**: `report_view_daily` / DAU
- **인사이트 참여율**: `report_insight_expand` / `report_view_daily`
- **위험 지수 분포**: recovery / warning / high_risk 비율 (업데이트)

---

## 7. 구현 가이드

### 7.1 Analytics Helper 클래스 (업데이트)
```kotlin
/**
 * Firebase Analytics 로깅 헬퍼
 * 
 * 이벤트명 변경 (1차 고도화):
 * - focus_settings_* → detoxy_settings_*
 * - 신규 이벤트: report_recovery_rate_calculated, detoxy_routine_completed, usage_stats_opt_in/out
 */
object AnalyticsHelper {
    
    private lateinit var analytics: FirebaseAnalytics
    
    fun initialize(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }
    
    // 디톡시 제어 설정 이벤트
    fun logDetoxySettingsOpen(source: String) {
        analytics.logEvent("detoxy_settings_open") {
            param("source", source)
        }
    }
    
    fun logDetoxySettingsSaved(
        preset: String,  // "complete_block", "standard", "relaxed", "custom"
        snsEnabled: Boolean,
        messengerEnabled: Boolean,
        webEnabled: Boolean,
        videoEnabled: Boolean,
        otherEnabled: Boolean,
        riskIndex: Int  // 신규 파라미터
    ) {
        val enabledCount = listOf(
            snsEnabled, messengerEnabled, webEnabled, videoEnabled, otherEnabled
        ).count { it }
        
        analytics.logEvent("detoxy_settings_saved") {
            param("preset", preset)
            param("sns_enabled", snsEnabled)
            param("messenger_enabled", messengerEnabled)
            param("web_enabled", webEnabled)
            param("video_enabled", videoEnabled)
            param("other_enabled", otherEnabled)
            param("total_enabled_count", enabledCount)
        }
    }
    
    // 세션 이벤트
    fun logSessionInterrupted(
        sessionId: String,
        category: String,
        elapsedSeconds: Int
    ) {
        analytics.logEvent("session_interrupted") {
            param("session_id", sessionId)
            param("category", category)
            param("elapsed_seconds", elapsedSeconds)
        }
    }
    
    // 리포트 이벤트
    fun logReportViewDaily(
        hasData: Boolean,
        sessionCount: Int,
        successCount: Int,
        totalFocusMinutes: Int
    ) {
        analytics.logEvent("report_view_daily") {
            param("has_data", hasData)
            param("session_count", sessionCount)
            param("success_count", successCount)
            param("total_focus_minutes", totalFocusMinutes)
        }
    }
    
    fun logRiskIndexCalculated(riskIndex: Int, riskLevel: String) {
        analytics.logEvent("report_risk_index_calculated") {
            param("risk_index", riskIndex)
            param("risk_level", riskLevel)
        }
    }
}
```

---

### 7.2 ViewModel 통합
```kotlin
class FocusModeSettingsViewModel @Inject constructor(
    private val repository: FocusSettingsRepository
) : ViewModel() {
    
    fun saveSettings(settings: FocusSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            
            // Analytics 로깅
            AnalyticsHelper.logFocusSettingsSaved(
                preset = settings.preset,
                snsEnabled = settings.snsEnabled,
                messengerEnabled = settings.messengerEnabled,
                webEnabled = settings.webEnabled,
                videoEnabled = settings.videoEnabled,
                otherEnabled = settings.otherEnabled
            )
        }
    }
}
```

---

## 8. 프라이버시 및 규정 준수

### 8.1 GDPR / 개인정보보호법
- ✅ 익명화된 데이터만 수집
- ✅ 사용자 동의 획득 (프라이버시 정책)
- ✅ 데이터 삭제 요청 대응 가능

### 8.2 사용자 동의 구현
```kotlin
class AnalyticsConsentManager(private val context: Context) {
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
        
        // SharedPreferences 저장
        PreferenceManager(context).setAnalyticsConsent(enabled)
    }
    
    fun isAnalyticsEnabled(): Boolean {
        return PreferenceManager(context).getAnalyticsConsent()
    }
}
```

---

## 9. 테스트 및 디버깅

### 9.1 디버그 모드
```bash
# Android
adb shell setprop debug.firebase.analytics.app com.allday.detoxy

# 로그 확인
adb logcat | grep -i "FirebaseAnalytics"
```

### 9.2 DebugView
Firebase Console → Analytics → DebugView 에서 실시간 이벤트 확인

---

## 10. 구현 일정

### Week 1 (집중모드 설정)
- [x] `focus_settings_*` 이벤트 구현
- [x] AnalyticsHelper 클래스 작성

### Week 2B (리포트 고도화)
- [x] `report_*` 이벤트 구현
- [x] `session_interrupted` 이벤트 구현

### Week 3 (QA)
- [x] 전체 이벤트 로깅 검증
- [x] Firebase Console 데이터 확인

---

**최종 업데이트**: 2025-10-12
**총 이벤트 수**: 15개
**핵심 지표**: 10개

