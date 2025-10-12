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

## 2. 이벤트 분류

### 2.1 집중모드 설정 관련
```
focus_settings_*
```

### 2.2 리포트 관련
```
report_*
```

### 2.3 세션 관련
```
session_*
```

### 2.4 권한 관련
```
permission_*
```

---

## 3. 이벤트 상세 정의

### 3.1 집중모드 설정 이벤트

#### `focus_settings_open`
**목적**: 설정 화면 진입 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `source` | string | 진입 경로 | `"timer_screen"`, `"settings_menu"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("focus_settings_open") {
    param("source", "timer_screen")
}
```

---

#### `focus_settings_saved`
**목적**: 설정 저장 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `preset` | string | 선택한 프리셋 | `"full_block"`, `"focus"`, `"relaxed"`, `"custom"` |
| `sns_enabled` | boolean | SNS 차단 여부 | `true` |
| `messenger_enabled` | boolean | 메신저 차단 여부 | `false` |
| `web_enabled` | boolean | Web 차단 여부 | `true` |
| `video_enabled` | boolean | 영상 차단 여부 | `true` |
| `other_enabled` | boolean | 기타 앱 차단 여부 | `true` |
| `total_enabled_count` | number | 활성화된 카테고리 수 | `4` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("focus_settings_saved") {
    param("preset", "full_block")
    param("sns_enabled", true)
    param("messenger_enabled", false)
    param("web_enabled", true)
    param("video_enabled", true)
    param("other_enabled", true)
    param("total_enabled_count", 4)
}
```

---

#### `focus_settings_category_toggle`
**목적**: 개별 카테고리 토글 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `category` | string | 카테고리명 | `"sns"`, `"messenger"`, `"web"`, `"video"`, `"other"` |
| `enabled` | boolean | 활성화 여부 | `true` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("focus_settings_category_toggle") {
    param("category", "messenger")
    param("enabled", true)
}
```

---

#### `focus_settings_preset_selected`
**목적**: 프리셋 선택 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `preset` | string | 선택한 프리셋 | `"full_block"`, `"focus"`, `"relaxed"` |
| `previous_preset` | string | 이전 프리셋 | `"focus"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("focus_settings_preset_selected") {
    param("preset", "full_block")
    param("previous_preset", "focus")
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
**목적**: Detoxy 위험 지수 계산 추적

**파라미터**:
| 파라미터명 | 타입 | 설명 | 예시 값 |
|-----------|------|------|---------|
| `risk_index` | number | 위험 지수 (0-100) | `32` |
| `risk_level` | string | 위험 단계 | `"low"`, `"medium"`, `"high"` |

**로깅 코드**:
```kotlin
FirebaseAnalytics.getInstance(context).logEvent("report_risk_index_calculated") {
    param("risk_index", 32)
    param("risk_level", "low")
}
```

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
| `permission_type` | string | 권한 타입 | `"accessibility"`, `"overlay"`, `"dnd"` |

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

### 5.2 설정 Funnel
```
1. focus_settings_open
2. focus_settings_preset_selected
3. focus_settings_category_toggle (여러 번 가능)
4. focus_settings_saved
```

### 5.3 세션 Funnel
```
1. session_started
2. session_interrupted (0회 이상)
3. session_completed OR session_give_up
4. report_view_daily
```

---

## 6. 핵심 지표 (KPI)

### 6.1 사용자 참여도
- **DAU (Daily Active Users)**: `app_open` 이벤트 발생 사용자 수
- **세션 시작률**: `session_started` / DAU
- **설정 변경률**: `focus_settings_saved` / DAU

### 6.2 기능 채택률
- **카테고리 활용률**: 
  - SNS 차단 사용자 비율: `sns_enabled=true` / 전체 사용자
  - 메신저 차단 사용자 비율: `messenger_enabled=true` / 전체 사용자
- **프리셋 분포**:
  - 전체 차단: X%
  - 집중: Y%
  - 완화: Z%
  - 커스텀: W%

### 6.3 성과 지표
- **집중 성공률**: `session_completed` / `session_started`
- **평균 집중 시간**: `total_focus_minutes` 평균
- **방해요인 차단 효과**: `session_interrupted` 발생률

### 6.4 리포트 지표
- **리포트 조회율**: `report_view_daily` / DAU
- **인사이트 참여율**: `report_insight_expand` / `report_view_daily`
- **위험 지수 분포**: low / medium / high 비율

---

## 7. 구현 가이드

### 7.1 Analytics Helper 클래스
```kotlin
object AnalyticsHelper {
    
    private lateinit var analytics: FirebaseAnalytics
    
    fun initialize(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }
    
    // 집중모드 설정 이벤트
    fun logFocusSettingsOpen(source: String) {
        analytics.logEvent("focus_settings_open") {
            param("source", source)
        }
    }
    
    fun logFocusSettingsSaved(
        preset: String,
        snsEnabled: Boolean,
        messengerEnabled: Boolean,
        webEnabled: Boolean,
        videoEnabled: Boolean,
        otherEnabled: Boolean
    ) {
        val enabledCount = listOf(
            snsEnabled, messengerEnabled, webEnabled, videoEnabled, otherEnabled
        ).count { it }
        
        analytics.logEvent("focus_settings_saved") {
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

