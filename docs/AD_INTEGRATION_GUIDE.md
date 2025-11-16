# Google AdMob 광고 통합 가이드

**작성일**: 2025년 11월 14일  
**앱 이름**: ScreenSence  
**목적**: Google AdMob을 통한 광고 수익화

---

## 📋 목차

1. [사전 준비](#1-사전-준비)
2. [AdMob 계정 설정](#2-admob-계정-설정)
3. [프로젝트 설정](#3-프로젝트-설정)
4. [광고 단위 생성](#4-광고-단위-생성)
5. [코드 구현](#5-코드-구현)
6. [개인정보처리방침 업데이트](#6-개인정보처리방침-업데이트)
7. [구글 플레이 콘솔 업데이트](#7-구글-플레이-콘솔-업데이트)
8. [테스트](#8-테스트)
9. [배포](#9-배포)

---

## 1. 사전 준비

### 1.1 필요한 것

- Google 계정
- AdMob 계정 (생성 필요)
- Firebase 프로젝트 (이미 있음)
- 앱이 Google Play Console에 등록되어 있어야 함

### 1.2 광고 유형 선택

**추천 광고 유형**:
1. **배너 광고** (Banner Ad)
   - 화면 하단 또는 상단에 고정
   - 지속적으로 표시
   - 수익: 낮음, 사용자 경험: 양호

2. **전면 광고** (Interstitial Ad)
   - 전체 화면 광고
   - 특정 액션 후 표시 (예: 타이머 완료 후)
   - 수익: 높음, 사용자 경험: 주의 필요

3. **보상형 광고** (Rewarded Ad)
   - 사용자가 선택적으로 시청
   - 보상 제공 (예: 포인트 추가)
   - 수익: 중간, 사용자 경험: 우수

**ScreenSence 추천 조합**:
- 리포트 화면: 배너 광고 (하단)
- 타이머 완료 후: 전면 광고 (선택적)
- 포인트 보너스: 보상형 광고 (선택적)

---

## 2. AdMob 계정 설정

### 2.1 AdMob 계정 생성

1. [AdMob 웹사이트](https://admob.google.com/) 접속
2. Google 계정으로 로그인
3. "앱 추가" 클릭
4. 앱 정보 입력:
   - 앱 이름: ScreenSence
   - 플랫폼: Android
   - 패키지명: `com.allday.detoxy`

### 2.2 Firebase 프로젝트 연결

1. AdMob 대시보드 → 앱 설정
2. "Firebase 프로젝트 연결" 클릭
3. 기존 Firebase 프로젝트 선택 (Allday Detoxy)
4. 연결 완료

---

## 3. 프로젝트 설정

### 3.1 Gradle 의존성 추가

**`gradle/libs.versions.toml`** 파일 수정:

```toml
[versions]
# 기존 버전들...
googleMobileAds = "23.0.0"  # 최신 버전 확인 필요

[libraries]
# 기존 라이브러리들...
google-mobile-ads = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "googleMobileAds" }
```

**`app/build.gradle.kts`** 파일 수정:

```kotlin
dependencies {
    // 기존 의존성들...
    
    // Google Mobile Ads
    implementation(libs.google.mobile.ads)
}
```

### 3.2 AndroidManifest.xml 확인

**이미 설정됨**:
```xml
<!-- 광고 ID 권한 (Android 13+, Firebase Analytics 사용) -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

**추가 필요** (선택):
```xml
<!-- 인터넷 권한 (광고 로드용, 이미 있을 수 있음) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3.3 AdMob App ID 추가

**`app/src/main/AndroidManifest.xml`** 파일 수정:

```xml
<application
    android:name=".DetoxyApplication"
    ...>
    
    <!-- AdMob App ID 추가 -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
    
    <!-- 기존 메타데이터들... -->
</application>
```

**참고**: `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`는 AdMob에서 발급받은 App ID로 교체해야 합니다.

---

## 4. 광고 단위 생성

### 4.1 AdMob 대시보드에서 광고 단위 생성

1. AdMob 대시보드 → 앱 선택 → 광고 단위
2. "광고 단위 만들기" 클릭
3. 광고 유형 선택 및 설정:

**배너 광고**:
- 이름: `Banner_ReportScreen`
- 광고 형식: 배너
- 광고 단위 ID: `ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`

**전면 광고**:
- 이름: `Interstitial_TimerComplete`
- 광고 형식: 전면
- 광고 단위 ID: `ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`

**보상형 광고**:
- 이름: `Rewarded_PointsBonus`
- 광고 형식: 보상형
- 광고 단위 ID: `ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`

### 4.2 테스트 광고 단위 ID

**개발 중에는 테스트 광고 ID 사용**:

```kotlin
// 테스트 광고 단위 ID
val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
```

---

## 5. 코드 구현

### 5.1 배너 광고 구현

**Compose에서 배너 광고 표시**:

```kotlin
// ReportScreen.kt 예시
@Composable
fun ReportScreen() {
    Column {
        // 리포트 내용...
        
        // 배너 광고 (화면 하단)
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    adUnitId = if (BuildConfig.DEBUG) {
                        "ca-app-pub-3940256099942544/6300978111" // 테스트 ID
                    } else {
                        "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 실제 ID
                    }
                    setAdSize(AdSize.BANNER)
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}
```

### 5.2 전면 광고 구현

**타이머 완료 후 전면 광고 표시**:

```kotlin
// TimerViewModel.kt 또는 적절한 위치
class TimerViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {
    
    private var interstitialAd: InterstitialAd? = null
    
    fun loadInterstitialAd() {
        InterstitialAd.load(
            context,
            if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544/1033173712" // 테스트 ID
            } else {
                "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 실제 ID
            },
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }
    
    fun showInterstitialAd() {
        interstitialAd?.let { ad ->
            if (ad.fullScreenContentCallback == null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // 광고 닫힘 후 처리
                        loadInterstitialAd() // 다음 광고 미리 로드
                    }
                }
            }
            ad.show(context as Activity)
        }
    }
    
    fun onTimerCompleted() {
        // 타이머 완료 처리...
        
        // 전면 광고 표시 (선택적)
        if (shouldShowAd()) {
            showInterstitialAd()
        }
    }
    
    private fun shouldShowAd(): Boolean {
        // 광고 표시 빈도 제어 (예: 3번 중 1번)
        return Random.nextInt(3) == 0
    }
}
```

### 5.3 보상형 광고 구현

**포인트 보너스용 보상형 광고**:

```kotlin
// RewardViewModel.kt 또는 적절한 위치
class RewardViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {
    
    private var rewardedAd: RewardedAd? = null
    
    fun loadRewardedAd() {
        RewardedAd.load(
            context,
            if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544/5224354917" // 테스트 ID
            } else {
                "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 실제 ID
            },
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }
    
    fun showRewardedAd(
        onRewardEarned: (Int) -> Unit // 포인트 지급 콜백
    ) {
        rewardedAd?.let { ad ->
            if (ad.fullScreenContentCallback == null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        loadRewardedAd() // 다음 광고 미리 로드
                    }
                }
            }
            
            ad.show(
                context as Activity,
                OnUserEarnedRewardListener { rewardItem ->
                    // 보상 지급 (예: 포인트 100점)
                    val points = rewardItem.amount.toInt() * 100
                    onRewardEarned(points)
                }
            )
        } ?: run {
            // 광고가 로드되지 않음
            Toast.makeText(context, "광고를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 5.4 광고 표시 전략

**사용자 경험을 고려한 광고 표시**:

1. **배너 광고**:
   - 리포트 화면 하단에만 표시
   - 타이머 실행 중에는 표시하지 않음
   - 설정 화면에는 표시하지 않음

2. **전면 광고**:
   - 타이머 완료 후에만 표시
   - 빈도 제한: 3번 중 1번 또는 사용자 설정
   - 타이머 포기 시에는 표시하지 않음

3. **보상형 광고**:
   - 사용자가 명시적으로 선택할 때만 표시
   - "포인트 보너스 받기" 버튼 제공
   - 리포트 화면 또는 보상 화면에 배치

---

## 6. 개인정보처리방침 업데이트

### 6.1 개인정보처리방침에 추가할 내용

**`docs/PRIVACY_POLICY.md`** 파일 수정:

```markdown
### 4.3 Google AdMob (향후 추가 예정)
- **광고 표시**: Google AdMob을 통한 광고 표시
  - 광고 ID는 개인화된 광고 제공을 위해 사용됩니다
  - 사용자는 Android 설정에서 광고 ID를 재설정하거나 삭제할 수 있습니다
  - [Google AdMob 개인정보처리방침](https://support.google.com/admob/answer/6128543)
```

**또는 기존 섹션 수정**:

```markdown
### 4.1 Google Firebase
- **Firebase Analytics**: 익명화된 사용 통계 수집
  - 데이터는 Google의 개인정보처리방침에 따라 처리됩니다
  - [Google 개인정보처리방침](https://policies.google.com/privacy)
  
- **Firebase Crashlytics**: 앱 오류 및 크래시 리포트 수집
  - 오류 해결을 위한 기술적 정보만 수집됩니다
  - 개인정보는 포함되지 않습니다

- **Google AdMob** (향후 추가 예정): 광고 표시
  - 광고 ID를 사용하여 개인화된 광고를 제공합니다
  - 사용자는 Android 설정에서 광고 ID를 재설정할 수 있습니다
```

---

## 7. 구글 플레이 콘솔 업데이트

### 7.1 광고 ID 사용 목적 업데이트

**구글 플레이 콘솔 → 데이터 보안 → 광고 ID**:

1. "앱에서 광고 ID를 사용하나요?" → **예** (이미 선택됨)

2. "앱에 광고 ID가 필요한 이유는 무엇인가요?" → **다음 항목 추가**:
   - ✅ **애널리틱스** (기존 선택 유지)
   - ✅ **광고 또는 마케팅** (새로 추가)
     - 설명: "Google AdMob을 통한 광고 표시 및 개인화된 광고 제공"

---

## 8. 테스트

### 8.1 테스트 광고 ID 사용

**개발 중에는 반드시 테스트 광고 ID 사용**:

```kotlin
object AdConfig {
    val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111" // 테스트
    } else {
        "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 실제
    }
    
    val INTERSTITIAL_AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712" // 테스트
    } else {
        "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 실제
    }
    
    val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/5224354917" // 테스트
    } else {
        "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 실제
    }
}
```

### 8.2 테스트 체크리스트

- [ ] 배너 광고가 정상적으로 로드되는지 확인
- [ ] 전면 광고가 정상적으로 표시되는지 확인
- [ ] 보상형 광고 보상이 정상적으로 지급되는지 확인
- [ ] 광고가 사용자 경험을 방해하지 않는지 확인
- [ ] 광고 로드 실패 시 앱이 정상 작동하는지 확인
- [ ] 릴리스 빌드에서 실제 광고 ID로 테스트

---

## 9. 배포

### 9.1 배포 전 확인사항

- [ ] 실제 광고 단위 ID로 교체
- [ ] 테스트 광고 ID 제거
- [ ] 개인정보처리방침 업데이트
- [ ] 구글 플레이 콘솔 광고 ID 사용 목적 업데이트
- [ ] 광고 표시 빈도 및 위치 최적화
- [ ] 사용자 피드백 수집

### 9.2 광고 수익 최적화 팁

1. **광고 배치**:
   - 사용자가 자연스럽게 볼 수 있는 위치
   - 핵심 기능을 방해하지 않는 위치

2. **광고 빈도**:
   - 너무 자주 표시하지 않기
   - 사용자 경험 우선

3. **보상형 광고 활용**:
   - 사용자에게 가치 제공
   - 자발적 시청 유도

---

## 10. 참고 자료

- [Google AdMob 공식 문서](https://developers.google.com/admob/android/quick-start)
- [AdMob 가이드](https://developers.google.com/admob/android)
- [광고 정책](https://support.google.com/admob/answer/6128543)
- [테스트 광고 단위 ID](https://developers.google.com/admob/android/test-ads)

---

## 11. 주의사항

### 11.1 정책 준수

- **광고 정책**: Google AdMob 정책 준수 필수
- **콘텐츠 정책**: 부적절한 콘텐츠와 함께 광고 표시 금지
- **클릭 사기**: 광고 클릭 조작 금지

### 11.2 사용자 경험

- **과도한 광고**: 사용자 경험을 해치지 않도록 주의
- **타이밍**: 집중 모드 중에는 광고 표시하지 않기
- **선택권**: 사용자가 광고를 스킵할 수 있는 옵션 제공

### 11.3 수익 최적화

- **A/B 테스트**: 광고 배치 및 빈도 테스트
- **분석**: AdMob 대시보드에서 수익 분석
- **사용자 피드백**: 광고 관련 피드백 수집 및 개선

---

**문서 최종 업데이트**: 2025년 11월 14일  
**작성자**: AI Assistant  
**버전**: 1.0

