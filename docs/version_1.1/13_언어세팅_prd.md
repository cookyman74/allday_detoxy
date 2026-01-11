# 다국어 지원 기능 PRD (v1.2)

**문서 버전**: v1.2 (리뷰 2차 반영)  
**작성일**: 2026-01-07  
**상태**: 🟡 검토 대기

---

## 1️⃣ 개요

### 1.1 목적
현재 한국어 전용인 AllDay Detoxy 앱에 다국어 지원을 추가하여 글로벌 사용자에게 서비스를 제공합니다.

### 1.2 지원 언어
| 언어 | BCP-47 태그 | 리소스 폴더 | 우선순위 |
|------|-------------|-------------|----------|
| 한국어 | `ko` | `values/` (기본) | **기본값 + 폴백** |
| 영어 | `en` | `values-en/` | 필수 |
| 스페인어 | `es` | `values-es/` | 필수 |
| 일본어 | `ja` | `values-ja/` | 필수 |
| 중국어 (간체) | `zh-Hans` | `values-b+zh+Hans/` | 필수 |

> ⚠️ **BCP-47 준수**: 중국어는 간체(`zh-Hans`)와 번체(`zh-Hant`)를 구분합니다. 본 버전에서는 간체만 지원하며, `values-b+zh+Hans/` 폴더명을 사용합니다.

### 1.3 폴백 전략
시스템 언어가 지원되지 않는 경우 (예: 프랑스어):
- **한국어 (`ko`)** - 기본 리소스로 자동 폴백

> ℹ️ **설계 근거**: 기본 리소스(`values/`)가 한국어이므로, Android 리소스 시스템의 자연스러운 폴백을 따릅니다. 영어를 1차 폴백으로 하려면 기본 리소스를 영어로 재구성하는 추가 작업이 필요하므로, 현재는 한국어 폴백을 유지합니다.

### 1.4 핵심 기능
1. **자동 언어 감지**: 시스템 언어 설정에 따라 앱 언어 자동 설정
2. **수동 언어 변경**: 설정에서 사용자가 직접 언어 선택 가능
3. **즉시 적용**: 언어 변경 시 앱 재시작 없이 바로 반영
4. **시스템 통합**: Android 13+ "앱별 언어 설정" 지원

---

## 2️⃣ 기술 아키텍처

### 2.1 Android 13+ 공식 API 사용

> ⚠️ **핵심 변경**: `LocaleManager.setLocale()` 직접 구현 대신 **`AppCompatDelegate.setApplicationLocales()`** 사용

```kotlin
// AndroidX AppCompat 기반 언어 설정 (권장)
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleManager {
    
    /**
     * 앱 언어 설정 (Android 13+ 공식 API 호환)
     * @param languageTag BCP-47 태그 (예: "ko", "en", "zh-Hans") 또는 null (시스템 기본)
     */
    fun setAppLocale(languageTag: String?) {
        val localeList = if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()  // 시스템 기본값
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    /**
     * 현재 앱 언어 가져오기
     */
    fun getAppLocale(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales[0]?.toLanguageTag()
    }
}
```

### 2.2 locale_config.xml 설정 (Android 13+)

```xml
<!-- res/xml/locale_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="ko"/>           <!-- 한국어 -->
    <locale android:name="en"/>           <!-- 영어 -->
    <locale android:name="es"/>           <!-- 스페인어 -->
    <locale android:name="ja"/>           <!-- 일본어 -->
    <locale android:name="zh-Hans"/>      <!-- 중국어 간체 -->
</locale-config>
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:localeConfig="@xml/locale_config"
    ...>
```

### 2.3 리소스 폴더 구조
```
res/
├── values/                    # 기본 (한국어)
│   └── strings.xml
├── values-en/                 # 영어
│   └── strings.xml
├── values-es/                 # 스페인어
│   └── strings.xml
├── values-ja/                 # 일본어
│   └── strings.xml
└── values-b+zh+Hans/          # 중국어 간체 (BCP-47)
    └── strings.xml
```

### 2.4 시스템 기본 언어 감지

> ⚠️ **주의**: `Locale.getDefault()`는 앱에서 변경한 값을 반환하므로, **원본 시스템 언어**를 가져오려면 다른 방식 사용

```kotlin
/**
 * 시스템 기본 언어 가져오기
 * 
 * ⚠️ 범위 제한: 사용자가 설정한 다중 언어 목록(우선순위 리스트)이 아닌,
 * 시스템의 **1순위 단일 로케일**만 반환합니다.
 * 
 * 다중 로케일 폴백이 필요하면 ConfigurationCompat.getLocales()를 사용하세요.
 */
fun getSystemPrimaryLocale(): Locale {
    return Resources.getSystem().configuration.locales[0]
}
```

> ℹ️ **"시스템 기본값" 동작 정의**: 본 앱에서 "시스템 기본값"은 사용자의 **1순위 시스템 로케일**을 의미합니다. 다중 언어 목록의 순차 폴백은 Android 리소스 시스템이 자동으로 처리합니다.

---

## 3️⃣ UI 설계

### 3.1 언어 설정 화면 위치
```
설정 탭 > 앱 설정
  └── 언어 설정
```

### 3.2 Android 13+ 통합 전략

| 방식 | 설명 | 권장 |
|------|------|------|
| **앱 내부 스위치** | 앱 설정에서 언어 목록 표시 + 선택 | ✅ 권장 |
| 시스템 설정 딥링크 | "시스템 설정에서 변경" 버튼 제공 | 보조 옵션 |

**앱 내 언어 선택 UI**:
```
○ 시스템 기본값 (자동)
○ 한국어
○ English
○ Español
○ 日本語
○ 中文 (简体)
```

> 각 언어는 **해당 언어로 표시**합니다.

### 3.3 언어 변경 시 동작
1. 사용자가 언어 선택
2. `AppCompatDelegate.setApplicationLocales()` 호출
3. **자동으로 Activity 재생성 및 리컴포지션** (AppCompat 처리)
4. 알림 채널 재생성 (별도 처리 필요)

---

## 4️⃣ 알림 및 서비스 현지화

### 4.1 NotificationChannel 현지화 전략

> ⚠️ **문제**: NotificationChannel의 이름/설명은 생성 시 고정되어 언어 변경 후에도 갱신되지 않음

> ⚠️ **제약**: 채널 삭제 후 재생성 시 **사용자 맞춤 설정(알림 소리/진동/중요도/차단 상태)이 초기화됨**

**권장 해결책**: 같은 ID로 `createNotificationChannel()` 재호출 (이름/설명만 갱신)

```kotlin
object NotificationHelper {
    
    /**
     * 알림 채널 이름/설명 갱신
     * 
     * ⚠️ 같은 ID로 createNotificationChannel을 재호출하면
     * 이름과 설명만 갱신되고, 사용자 설정(소리/진동/중요도 등)은 유지됩니다.
     */
    fun updateNotificationChannelLocale(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            
            // 같은 ID로 재호출 → 이름/설명만 갱신, 사용자 설정 유지
            val channel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                context.getString(R.string.notification_channel_focus),
                NotificationManager.IMPORTANCE_LOW  // 기존 사용자 설정이 있으면 무시됨
            ).apply {
                description = context.getString(R.string.notification_channel_focus_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
```

> ℹ️ **동작 원리**: `createNotificationChannel()`을 기존 ID로 호출하면:
> - ✅ 이름(name)과 설명(description)은 **새 값으로 갱신**
> - ✅ 중요도, 소리, 진동 등 **사용자 맞춤 설정은 유지**
> - ❌ 코드에서 지정한 중요도 등은 **무시됨** (사용자 설정 우선)

### 4.2 포그라운드 서비스 알림 갱신
```kotlin
// FocusTimerService 내
fun updateNotificationLocale() {
    val notification = buildFocusNotification()  // 현지화된 문자열 사용
    val nm = getSystemService(NotificationManager::class.java)
    nm.notify(FOCUS_NOTIFICATION_ID, notification)
}
```

---

## 5️⃣ 날짜/시간 형식화

### 5.1 사용자 12/24시간 설정 반영

> ⚠️ **수정**: `DateTimeFormatter.ofLocalizedTime()`은 로케일만 반영하고 사용자의 12/24시간 설정은 무시함

```kotlin
import android.text.format.DateFormat

/**
 * 시간 형식화 (사용자 12/24시간 설정 반영)
 */
fun formatTime(context: Context, time: LocalTime): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, time.hour)
        set(Calendar.MINUTE, time.minute)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

/**
 * 날짜 형식화 (로케일 기반)
 */
fun formatDate(context: Context, date: LocalDate): String {
    val calendar = Calendar.getInstance().apply {
        set(date.year, date.monthValue - 1, date.dayOfMonth)
    }
    return DateFormat.getDateFormat(context).format(calendar.time)
}
```

### 5.2 언어별 표시 예시 (사용자 설정 반영)
| 언어 | 날짜 | 시간 (24시간) | 시간 (12시간) |
|------|------|---------------|---------------|
| 한국어 | 2026. 1. 6. | 20:30 | 오후 8:30 |
| 영어 | 1/6/2026 | 20:30 | 8:30 PM |
| 스페인어 | 6/1/2026 | 20:30 | 8:30 p. m. |
| 일본어 | 2026/01/06 | 20:30 | 午後8:30 |
| 중국어 | 2026/1/6 | 20:30 | 下午8:30 |

---

## 6️⃣ 번역 대상 문자열

### 6.1 번역 범위
| 범주 | 예시 | 수량 (예상) |
|------|------|------------|
| 화면 제목 | "집중 타이머", "설정" | ~20 |
| 버튼/라벨 | "시작하기", "취소", "저장" | ~50 |
| 설명 텍스트 | 권한 안내, 기능 설명 | ~30 |
| 알림 메시지 | 타이머 종료, 진행 중 알림 | ~15 |
| 에러 메시지 | 권한 거부, 네트워크 오류 | ~10 |
| 기타 | 단위 (분, 시간) | ~10 |

**총 예상**: 약 **130개** 문자열

### 6.2 번역 제외 항목
- 앱 이름 ("AllDay Detoxy") - 브랜드명 유지
- 로고 및 아이콘
- 외부 링크 URL

### 6.3 복수형/문법 처리
```xml
<plurals name="minutes_remaining">
    <item quantity="one">%d minute remaining</item>
    <item quantity="other">%d minutes remaining</item>
</plurals>
```

---

## 7️⃣ 구현 단계

### Phase 1: 기반 구조 (1일)
- [ ] `locale_config.xml` 생성
- [ ] `AndroidManifest.xml`에 `android:localeConfig` 추가
- [ ] `AppLocaleManager` 유틸리티 생성 (AppCompatDelegate 기반)
- [ ] 알림 채널 재생성 로직 추가

### Phase 2: 문자열 추출 (1일)
- [ ] 하드코딩된 한국어 문자열 → `strings.xml` 이동
- [ ] Compose 코드에서 `stringResource()` 사용
- [ ] 비Compose 코드에서 `context.getString()` 사용

### Phase 3: 번역 파일 생성 (2일)
- [ ] `values-en/strings.xml` (영어)
- [ ] `values-es/strings.xml` (스페인어)
- [ ] `values-ja/strings.xml` (일본어)
- [ ] `values-b+zh+Hans/strings.xml` (중국어 간체)

### Phase 4: 설정 UI (0.5일)
- [ ] 언어 설정 화면 Composable 생성
- [ ] `AppCompatDelegate.setApplicationLocales()` 연동
- [ ] 알림 채널 재생성 트리거

### Phase 5: 테스트 및 검증 (1일)
- [ ] 각 언어별 UI 레이아웃 확인 (텍스트 길이 차이)
- [ ] 12/24시간 시스템 설정 반영 확인
- [ ] 알림 채널 이름 현지화 확인
- [ ] Android 13+ 시스템 "앱별 언어" 연동 확인

**총 예상 기간**: 5.5일

---

## 8️⃣ 고려사항

### 8.1 텍스트 길이 차이
- 버튼/라벨에 충분한 여백 확보
- 긴 텍스트는 `ellipsis` 또는 `wrap` 처리

### 8.2 폰트 지원
- 중국어/일본어: 시스템 기본 CJK 폰트 사용

### 8.3 RTL (Right-to-Left) 지원
> ⚠️ **현재 해당 없음**: 지원 언어 중 RTL 언어(아랍어, 히브리어 등)가 없음
> 
> 향후 RTL 언어 추가 시 별도 검토 필요

### 8.4 앱스토어 메타데이터
- 앱 설명, 스크린샷도 각 언어별로 준비 필요
- Google Play Console 다국어 설정

---

## 9️⃣ 파일 구조 예상

```
app/src/main/
├── java/com/allday/detoxy/
│   ├── core/locale/
│   │   └── AppLocaleManager.kt           # 언어 설정 (AppCompat 기반)
│   ├── core/notification/
│   │   └── NotificationHelper.kt         # 채널 재생성 로직
│   └── presentation/ui/settings/
│       └── LanguageSettingsScreen.kt     # 언어 설정 UI
└── res/
    ├── xml/locale_config.xml             # Android 13+ 언어 설정
    ├── values/strings.xml                # 한국어 (기본)
    ├── values-en/strings.xml             # 영어
    ├── values-es/strings.xml             # 스페인어
    ├── values-ja/strings.xml             # 일본어
    └── values-b+zh+Hans/strings.xml      # 중국어 간체
```

---

## 🔟 성공 지표

| 지표 | 목표 |
|------|------|
| 지원 언어 수 | 5개 (한/영/스/일/중) |
| 번역 완성도 | 100% (모든 사용자 노출 문자열) |
| 언어 전환 속도 | 1초 이내 즉시 반영 |
| 레이아웃 깨짐 | 0건 |
| 알림 현지화 | 채널 이름 + 알림 내용 |

---

## 📚 참고 자료

- [Android per-app language preferences (공식)](https://developer.android.com/guide/topics/resources/app-languages)
- [AppCompatDelegate.setApplicationLocales](https://developer.android.com/reference/androidx/appcompat/app/AppCompatDelegate#setApplicationLocales(androidx.core.os.LocaleListCompat))
- [BCP-47 Language Tags](https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry)
- [Jetpack Compose String Resources](https://developer.android.com/jetpack/compose/resources)

---

## ✅ 리뷰 반영 내역

### v1.1 반영사항
| 구분 | 지적 사항 | 반영 내용 |
|------|----------|----------|
| High | LocaleManager.setLocale 직접 구현 | `AppCompatDelegate.setApplicationLocales()` + `locale_config.xml` 사용 |
| High | BCP-47 미준수/중국어 구분 없음 | `zh-Hans`, `values-b+zh+Hans/` 폴더 사용 |
| Medium | Locale.getDefault() 문제 | `Resources.getSystem().configuration.locales[0]` 사용 |
| Medium | NotificationChannel 현지화 누락 | 채널 현지화 전략 추가 |
| Medium | 12/24시간 설정 무시 | `DateFormat.getTimeFormat(context)` 사용 |
| Low | RTL 검증 항목 불필요 | "현재 해당 없음" 명시 |
| Q&A | 폴백 순서 미정의 | 폴백 전략 명시 |
| Q&A | Android 13+ 통합 방식 | 앱 내부 스위치 권장, 시스템 딥링크 보조 |

### v1.2 반영사항
| 구분 | 지적 사항 | 반영 내용 |
|------|----------|----------|
| High | 폴백 순서가 리소스 기본값과 불일치 | 한국어 단일 폴백으로 변경 (기본 리소스=한국어) |
| Medium | NotificationChannel 삭제 시 사용자 설정 초기화 | 같은 ID로 재호출하여 이름만 갱신, 설정 보존 |
| Medium | "시스템 기본값" 다중 로케일 동작 모호 | 1순위 단일 로케일 사용 명시 |
| Q&A | 영어 1차 폴백 확정 여부 | 한국어 폴백 유지 (추가 작업 불필요) |
| Q&A | NotificationChannel 우선순위 | 사용자 설정 보존 우선 |

---

**작성자**: AI Assistant  
**검토자**: (검토 완료)  
**승인자**: (승인 필요)
