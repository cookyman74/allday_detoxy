# 차단 앱 카테고리 매핑 초안

## 개요
- **작성 일자**: 2025-10-12
- **기준**: 한국 사용자 기준 주요 앱 (2024-2025 기준)
- **출처**: Play Store 다운로드 수, 사용 빈도 조사

---

## 카테고리 정의

### 1. SNS (소셜 네트워크 서비스)
**목적**: 소셜 미디어 플랫폼 차단 (피드 스크롤, 포스팅, 메시징 포함)

| 앱명 | 패키지명 | 우선순위 | 비고 |
|------|----------|----------|------|
| Instagram | `com.instagram.android` | ⭐⭐⭐ | 피드 + 스토리 + 릴스 |
| Facebook | `com.facebook.katana` | ⭐⭐⭐ | 피드 + 워치 |
| X (Twitter) | `com.twitter.android` | ⭐⭐⭐ | |
| Threads | `com.instagram.barcelona` | ⭐⭐ | Meta의 새 SNS |
| LinkedIn | `com.linkedin.android` | ⭐ | 업무용 SNS |
| Snapchat | `com.snapchat.android` | ⭐⭐ | |
| Pinterest | `com.pinterest` | ⭐ | |
| Reddit | `com.reddit.frontpage` | ⭐ | |
| Tumblr | `com.tumblr` | ⭐ | |

**총 9개 앱**

---

### 2. 메신저 & 커뮤니케이션
**목적**: 메시징 앱 (긴급 연락 고려하여 기본 허용)

| 앱명 | 패키지명 | 우선순위 | 비고 |
|------|----------|----------|------|
| KakaoTalk | `com.kakao.talk` | ⭐⭐⭐ | 한국 국민 메신저 |
| WhatsApp | `com.whatsapp` | ⭐⭐⭐ | 글로벌 1위 |
| Telegram | `org.telegram.messenger` | ⭐⭐⭐ | |
| LINE | `jp.naver.line.android` | ⭐⭐ | 일본, 대만, 태국 |
| Discord | `com.discord` | ⭐⭐ | 게이머/커뮤니티 |
| Slack | `com.Slack` | ⭐ | 업무용 |
| Microsoft Teams | `com.microsoft.teams` | ⭐ | 업무용 |
| Facebook Messenger | `com.facebook.orca` | ⭐⭐ | |
| Signal | `org.thoughtcrime.securesms` | ⭐ | 보안 메신저 |
| WeChat | `com.tencent.mm` | ⭐ | 중국 |
| Viber | `com.viber.voip` | ⭐ | |

**총 11개 앱**

**기본값**: ⚪ OFF (허용) - 사용자가 명시적으로 차단 선택 가능

---

### 3. Web 서핑
**목적**: 웹 브라우저 차단

| 앱명 | 패키지명 | 우선순위 | 비고 |
|------|----------|----------|------|
| Chrome | `com.android.chrome` | ⭐⭐⭐ | |
| Samsung Internet | `com.sec.android.app.sbrowser` | ⭐⭐⭐ | 삼성 기본 브라우저 |
| Firefox | `org.mozilla.firefox` | ⭐⭐ | |
| Microsoft Edge | `com.microsoft.emmx` | ⭐⭐ | |
| Opera | `com.opera.browser` | ⭐ | |
| Brave | `com.brave.browser` | ⭐ | |
| Naver Whale | `com.naver.whale` | ⭐⭐ | 한국 |
| DuckDuckGo | `com.duckduckgo.mobile.android` | ⭐ | |

**총 8개 앱**

---

### 4. 영상 & 쇼츠
**목적**: 동영상 플랫폼 및 쇼츠 콘텐츠 차단

| 앱명 | 패키지명 | 우선순위 | 비고 |
|------|----------|----------|------|
| YouTube | `com.google.android.youtube` | ⭐⭐⭐ | 롱폼 + 쇼츠 |
| TikTok | `com.zhiliaoapp.musically` | ⭐⭐⭐ | 쇼츠 1위 |
| Instagram (Reels 포함) | `com.instagram.android` | ⭐⭐⭐ | SNS 카테고리와 중복 |
| Facebook (Watch 포함) | `com.facebook.katana` | ⭐⭐ | SNS 카테고리와 중복 |
| Netflix | `com.netflix.mediaclient` | ⭐⭐⭐ | 스트리밍 |
| Disney+ | `com.disney.disneyplus` | ⭐⭐ | 스트리밍 |
| Wavve | `com.pooq.wavve` | ⭐⭐ | 한국 OTT |
| Tving | `com.pooq.skb` | ⭐⭐ | 한국 OTT |
| Coupang Play | `com.coupang.mobile.play` | ⭐ | 한국 OTT |
| Twitch | `tv.twitch.android.app` | ⭐⭐ | 게임 스트리밍 |
| AfreecaTV | `com.rsupport.android.afreeca` | ⭐ | 한국 스트리밍 |
| V LIVE | `com.naver.vapp` | ⭐ | K-POP 팬 |

**총 12개 앱**

**중복 처리**:
- Instagram, Facebook은 SNS 또는 영상 중 하나만 선택 시에도 차단
- 사용자가 둘 다 선택하면 중복 제거

---

### 5. 게임 (기타 카테고리 일부)
**목적**: 모바일 게임 차단 (추후 확장 가능)

| 앱명 | 패키지명 | 우선순위 | 비고 |
|------|----------|----------|------|
| PUBG Mobile | `com.tencent.ig` | ⭐⭐⭐ | |
| 리그 오브 레전드: 와일드 리프트 | `com.riotgames.league.wildrift` | ⭐⭐⭐ | |
| 배틀그라운드 모바일 | `com.pubg.krmobile` | ⭐⭐⭐ | 한국 버전 |
| 로블록스 | `com.roblox.client` | ⭐⭐ | |
| Minecraft | `com.mojang.minecraftpe` | ⭐⭐ | |
| Clash of Clans | `com.supercell.clashofclans` | ⭐⭐ | |
| Clash Royale | `com.supercell.clashroyale` | ⭐⭐ | |
| Candy Crush Saga | `com.king.candycrushsaga` | ⭐ | |
| Among Us | `com.innersloth.spacemafia` | ⭐ | |

**총 9개 앱**

**MVP 제외**: 게임 카테고리는 1차 고도화에서 "기타" 카테고리에 포함

---

### 6. 커머스 (기타 카테고리 일부)
**목적**: 쇼핑 앱 차단 (충동 구매 방지)

| 앱명 | 패키지명 | 우선순위 | 비고 |
|------|----------|----------|------|
| Coupang | `com.coupang.mobile` | ⭐⭐⭐ | 한국 1위 |
| 11번가 | `com.elevenst` | ⭐⭐ | |
| G마켓 | `com.gmarket.mobile` | ⭐⭐ | |
| SSG.COM | `kr.co.ssg.android` | ⭐ | |
| 네이버 쇼핑 | `com.nhn.android.search` | ⭐⭐ | |
| Amazon | `com.amazon.mShop.android.shopping` | ⭐⭐ | 글로벌 |
| AliExpress | `com.alibaba.aliexpresshd` | ⭐ | |

**총 7개 앱**

**MVP 제외**: 커머스 카테고리는 1차 고도화에서 "기타" 카테고리에 포함

---

### 7. 기타 앱 (Catch-All)
**목적**: 명시적으로 분류되지 않은 모든 앱 차단

**동작 방식**:
- 사용자가 "기타 앱" 토글을 ON으로 설정 시
- 상기 카테고리(SNS, 메신저, Web, 영상)에 속하지 않은 모든 앱 차단
- **제외 대상**:
  - 시스템 앱 (Settings, Phone, Contacts, Launcher 등)
  - 자사 앱 (Allday Detoxy)
  - 화이트리스트 (2차 고도화 예정)

**구현 방식**:
```kotlin
// 접근성 서비스에서 패키지명 확인
fun isBlockedApp(packageName: String): Boolean {
    // 1. 명시적 카테고리에 속한 앱인가?
    if (packageName in categoryApps) return categoryEnabled[category] == true
    
    // 2. 기타 앱 차단이 활성화되어 있는가?
    if (otherAppsBlockEnabled) {
        // 3. 시스템 앱 또는 자사 앱인가?
        if (packageName in systemApps || packageName == "com.allday.detoxy") {
            return false
        }
        // 4. 모두 아니면 차단
        return true
    }
    
    return false
}
```

---

## MVP 1차 고도화 범위

### 포함 카테고리 (5개)
1. ✅ **SNS** (9개 앱)
2. ✅ **메신저 & 커뮤니케이션** (11개 앱, 기본 OFF)
3. ✅ **Web 서핑** (8개 앱)
4. ✅ **영상 & 쇼츠** (12개 앱, 중복 제거)
5. ✅ **기타 앱** (Catch-All)

### 제외 (2차 고도화 예정)
- ⏸️ 게임 (별도 카테고리)
- ⏸️ 커머스 (별도 카테고리)
- ⏸️ 업무/생산성 화이트리스트

---

## 데이터 구조 설계

### Enum 정의
```kotlin
enum class AppCategory {
    SNS,           // 소셜 네트워크
    MESSENGER,     // 메신저 & 커뮤니케이션
    WEB,           // 웹 브라우저
    VIDEO_SHORTS,  // 영상 & 쇼츠
    OTHER;         // 기타 앱 (Catch-All)
    
    fun getDisplayName(): String = when (this) {
        SNS -> "SNS"
        MESSENGER -> "메신저"
        WEB -> "Web 서핑"
        VIDEO_SHORTS -> "영상 & 쇼츠"
        OTHER -> "기타 앱"
    }
    
    fun getIcon(): String = when (this) {
        SNS -> "📱"
        MESSENGER -> "💬"
        WEB -> "🌐"
        VIDEO_SHORTS -> "🎬"
        OTHER -> "📦"
    }
}
```

### 매핑 데이터
```kotlin
object AppCategoryMapper {
    // 카테고리별 패키지 리스트
    private val categoryMap = mapOf(
        AppCategory.SNS to setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.instagram.barcelona",
            "com.linkedin.android",
            "com.snapchat.android",
            "com.pinterest",
            "com.reddit.frontpage",
            "com.tumblr"
        ),
        AppCategory.MESSENGER to setOf(
            "com.kakao.talk",
            "com.whatsapp",
            "org.telegram.messenger",
            "jp.naver.line.android",
            "com.discord",
            "com.Slack",
            "com.microsoft.teams",
            "com.facebook.orca",
            "org.thoughtcrime.securesms",
            "com.tencent.mm",
            "com.viber.voip"
        ),
        AppCategory.WEB to setOf(
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.naver.whale",
            "com.duckduckgo.mobile.android"
        ),
        AppCategory.VIDEO_SHORTS to setOf(
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.instagram.android",  // 중복 (Reels)
            "com.facebook.katana",     // 중복 (Watch)
            "com.netflix.mediaclient",
            "com.disney.disneyplus",
            "com.pooq.wavve",
            "com.pooq.skb",
            "com.coupang.mobile.play",
            "tv.twitch.android.app",
            "com.rsupport.android.afreeca",
            "com.naver.vapp"
        )
    )
    
    // 시스템 앱 (차단 제외)
    private val systemApps = setOf(
        "com.android.settings",
        "com.android.phone",
        "com.android.contacts",
        "com.android.launcher",
        "com.android.vending",  // Play Store
        "com.google.android.gms",
        "com.allday.detoxy"  // 자사 앱
    )
    
    /**
     * 패키지명으로 카테고리 찾기
     */
    fun getCategoryByPackage(packageName: String): AppCategory? {
        return categoryMap.entries.find { packageName in it.value }?.key
    }
    
    /**
     * 카테고리별 패키지 리스트
     */
    fun getPackagesByCategory(category: AppCategory): Set<String> {
        return categoryMap[category] ?: emptySet()
    }
    
    /**
     * 차단 여부 확인 (카테고리 설정 기반)
     */
    fun isBlocked(
        packageName: String,
        enabledCategories: Set<AppCategory>,
        otherAppsEnabled: Boolean
    ): Boolean {
        // 시스템 앱은 항상 허용
        if (packageName in systemApps) return false
        
        // 명시적 카테고리 확인
        val category = getCategoryByPackage(packageName)
        if (category != null) {
            return category in enabledCategories
        }
        
        // 기타 앱 차단 여부
        return otherAppsEnabled
    }
}
```

---

## 프리셋 정의

### 1. 전체 차단 (Default)
```kotlin
val PRESET_FULL_BLOCK = setOf(
    AppCategory.SNS,          // ON
    AppCategory.WEB,          // ON
    AppCategory.VIDEO_SHORTS, // ON
    AppCategory.OTHER         // ON
)
// MESSENGER: OFF (긴급 연락 고려)
```

### 2. 집중 (Focus)
```kotlin
val PRESET_FOCUS = setOf(
    AppCategory.SNS,          // ON
    AppCategory.VIDEO_SHORTS  // ON
)
// WEB: OFF (검색 허용)
// MESSENGER: OFF
// OTHER: OFF
```

### 3. 완화 (Relaxed)
```kotlin
val PRESET_RELAXED = setOf(
    AppCategory.VIDEO_SHORTS  // ON
)
// SNS: OFF
// WEB: OFF
// MESSENGER: OFF
// OTHER: OFF
```

---

## 중복 처리 전략

### 문제
- Instagram, Facebook이 SNS와 VIDEO_SHORTS에 모두 포함

### 해결책
```kotlin
fun getBlockedPackages(enabledCategories: Set<AppCategory>): Set<String> {
    val packages = mutableSetOf<String>()
    
    enabledCategories.forEach { category ->
        packages.addAll(getPackagesByCategory(category))
    }
    
    // 중복 자동 제거 (Set 사용)
    return packages
}
```

**결과**:
- SNS만 선택: Instagram, Facebook 차단
- VIDEO_SHORTS만 선택: Instagram, Facebook 차단
- 둘 다 선택: Instagram, Facebook 차단 (중복 제거됨)

---

## 향후 확장 계획

### 2차 고도화
1. **게임 카테고리 분리**
   - 현재 "기타"에 포함 → 독립 카테고리로
   - 추가 게임 타이틀 확대 (100+ 앱)

2. **커머스 카테고리 분리**
   - 쇼핑 앱 독립 관리
   - 충동 구매 시간대 분석

3. **업무/생산성 화이트리스트**
   - 사용자 커스텀 허용 앱 추가
   - 패키지 검색 UI 제공

4. **사용자 피드백 기반 확장**
   - 차단 실패 앱 수집
   - 커뮤니티 제안 앱 추가

---

## 테스트 계획

### 단계 1: 패키지명 검증
```bash
# 실제 기기에 설치된 앱 확인
adb shell pm list packages | grep instagram
adb shell pm list packages | grep kakao
```

### 단계 2: 카테고리 매핑 테스트
```kotlin
@Test
fun `Instagram은 SNS와 VIDEO_SHORTS 모두에 매핑`() {
    val snsPackages = AppCategoryMapper.getPackagesByCategory(AppCategory.SNS)
    val videoPackages = AppCategoryMapper.getPackagesByCategory(AppCategory.VIDEO_SHORTS)
    
    assertTrue("com.instagram.android" in snsPackages)
    assertTrue("com.instagram.android" in videoPackages)
}

@Test
fun `중복 제거 확인`() {
    val enabled = setOf(AppCategory.SNS, AppCategory.VIDEO_SHORTS)
    val packages = getBlockedPackages(enabled)
    
    // Instagram이 한 번만 포함되어야 함
    assertEquals(
        packages.filter { it == "com.instagram.android" }.size,
        1
    )
}
```

### 단계 3: 실제 차단 테스트
- Week 1 구현 시 각 카테고리별 대표 앱 3개씩 테스트
- 차단 실패 시 로그 분석 및 패키지명 재확인

---

## 주의사항

### 1. 패키지명 변경
- 앱 업데이트 시 패키지명이 변경될 수 있음
- 정기적인 검증 필요 (분기별)

### 2. 지역별 차이
- 한국 앱 (Kakao, Naver) 우선 포함
- 글로벌 버전 출시 시 지역별 매핑 필요

### 3. 개인정보 보호
- 설치된 앱 목록 수집 시 사용자 동의 필요
- 로깅 시 패키지명만 기록 (앱 사용 시간 등은 UsageStats 권한 필요)

---

**최종 업데이트**: 2025-10-12
**총 앱 수**: 47개 (중복 제거 전)
**카테고리**: 5개 (SNS, 메신저, Web, 영상, 기타)

