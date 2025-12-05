# Play Store 내부 테스트 준비 - ScreenSence

## 📋 작업 정보

**작업 일자**: 2025-10-26  
**작업 내용**: Google Play Console 내부 테스트를 위한 서명된 App Bundle 생성  
**앱 공식 명칭**: ScreenSence

---

## 🔐 서명 설정

### 1. Keystore 생성

**파일명**: `screensence-release-key.jks`  
**알고리즘**: RSA 2048bit  
**유효기간**: 10,000일  
**Alias**: screensence

**생성 명령어**:
```bash
keytool -genkey -v -keystore screensence-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias screensence \
  -storepass "screensense@hoya1024" -keypass "screensense@hoya1024" \
  -dname "CN=Jung Ho Jang, OU=Development, O=ScreenSence, L=Seoul, ST=Seoul, C=KR"
```

---

### 2. build.gradle.kts 서명 설정 추가

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../screensence-release-key.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "screensense@hoya1024"
        keyAlias = "screensence"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "screensense@hoya1024"
    }
}

buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

---

### 3. AndroidManifest.xml 수정

**추가된 권한**:
```xml
<!-- 광고 ID 권한 (Android 13+, Firebase Analytics 사용) -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

**이유**: Google Play Console 경고 해결  
- Android 13(API 33) 타겟팅 시 광고 ID 권한 선언 필요
- Firebase Analytics 사용으로 인해 필요

---

## 📦 서명된 Bundle 생성

### 빌드 명령어

```bash
./gradlew clean bundleRelease
```

### 빌드 결과

```
BUILD SUCCESSFUL in 37s
52 actionable tasks: 50 executed, 2 up-to-date
```

### 생성된 파일

- **경로**: `app/build/outputs/bundle/release/app-release.aab`
- **크기**: 8.5MB
- **서명 상태**: ✅ 완료
- **생성 시간**: 2025-10-26 21:05

---

## 🐛 해결된 문제

### 문제 1: "업로드된 모든 번들에 서명해야 합니다"

**원인**: Bundle이 서명되지 않음 (unsigned)

**해결**:
1. Keystore 생성
2. build.gradle.kts에 서명 설정 추가
3. 서명된 Bundle 재빌드

---

### 문제 2: Android 13(API 33) 광고 ID 권한 경고

**원인**: targetSdk 34 사용 시 AD_ID 권한 선언 필요

**해결**: AndroidManifest.xml에 AD_ID 권한 추가

---

## 📤 Google Play Console 업로드 정보

### 출시명
```
ScreenSence v1.0 - 내부 테스트 (2차 고도화)
```

### 출시 노트 (한국어)

```
ScreenSence v1.0 내부 테스트 버전입니다.

🎯 주요 기능

1. 시간 기반 자동 실행
   - 특정 시간에 타이머 자동 시작 (최대 10개)
   - 요일별 반복 설정
   - 5분 전 사전 알림 및 자동 시작

2. 위치 기반 자동 실행
   - 특정 장소 도착 시 타이머 자동 시작 (최대 5개)
   - 회사, 학교, 도서관 등 등록 가능
   - 배터리 효율적 (1일 1.5% 이하)

3. 커스텀 타이머
   - 도넛 그래프로 5~180분 자유롭게 조정
   - 나만의 프리셋 저장 (최대 10개)
   - 드래그/탭으로 직관적 설정

4. 자동 실행 제어
   - 전체 자동 실행 일시정지
   - 다음 예정 자동 실행 확인

⚠️ 테스트 항목

- 시간 기반 자동 실행 정확도 확인
- 위치 기반 자동 실행 동작 확인
- 커스텀 타이머 UI 사용성 확인
- 배터리 소모 모니터링
- 권한 요청 플로우 확인

📱 시스템 요구사항

- Android 8.0 (API 26) 이상
- 필수 권한: 접근성, 오버레이, DND
- 선택 권한: 정확한 알람, 위치 (백그라운드)

🐛 알려진 이슈

- 위치 이탈 시 안내 기능 미구현 (3차 고도화 예정)
- 일부 OEM 제조사 최적화 미완료

버그 발견 시 피드백 부탁드립니다!
```

### 앱 정보

- **패키지명**: com.allday.detoxy
- **공식 명칭**: ScreenSence
- **버전 코드**: 1
- **버전 이름**: 1.0.0
- **최소 SDK**: 26 (Android 8.0)
- **타겟 SDK**: 34 (Android 14)

---

## ⚠️ 주의사항

### Keystore 파일 보안

1. **백업 필수**: `screensence-release-key.jks` 파일을 안전한 곳에 백업
2. **Git 제외**: `.gitignore`에 `*.jks` 포함 (이미 설정됨)
3. **비밀번호 관리**: `screensense@hoya1024` 안전하게 보관

**⚠️ 중요**: Keystore를 분실하면 앱 업데이트 불가능!

---

## 📋 다음 단계

### 1. Google Play Console 업로드
- `app-release.aab` 파일 업로드
- 출시명 및 출시 노트 입력

### 2. 내부 테스터 추가
- **테스트 및 출시** → **내부 테스트**
- **테스터** 탭에서 이메일 목록 만들기
- 최소 1명, 권장 5명 추가

### 3. 내부 테스트 출시
- 검토 후 출시 버튼 클릭
- 테스터에게 링크 공유

### 4. 피드백 수집
- Crashlytics 모니터링
- Analytics 이벤트 확인
- 테스터 피드백 취합

---

## 📊 빌드 통계

- **빌드 시간**: 37초
- **실행된 태스크**: 52개
- **Bundle 크기**: 8.5MB
- **서명 상태**: ✅ 완료
- **컴파일 에러**: 0개
- **경고**: 4개 (기존 경고, 기능에 영향 없음)

---

**작업 완료일**: 2025-10-26  
**상태**: ✅ 서명된 Bundle 생성 완료, Play Store 업로드 준비 완료

---

> **Play Store 준비 완료!** 서명된 App Bundle이 성공적으로 생성되었습니다. Google Play Console에 업로드하고 내부 테스터를 추가하면 내부 테스트를 시작할 수 있습니다. 🚀

