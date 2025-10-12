# Firebase Crashlytics 설정 가이드

## 📋 개요
이 문서는 Allday Detoxy 앱에 Firebase Crashlytics를 설정하는 방법을 설명합니다.

## ✅ 이미 완료된 작업 (코드 레벨)

### 1. Gradle 설정 완료
- `gradle/libs.versions.toml`: Firebase BOM, Crashlytics 버전 추가
- `build.gradle.kts` (프로젝트): Google Services, Crashlytics 플러그인 추가
- `app/build.gradle.kts`: Firebase 의존성 추가

### 2. Application 클래스 준비
- `DetoxyApplication.kt`: Crashlytics 자동 초기화 주석 추가

## 🔧 수동 설정 필요 작업

### 1. Firebase 프로젝트 생성

1. **Firebase Console 접속**
   ```
   https://console.firebase.google.com/
   ```

2. **새 프로젝트 만들기**
   - 프로젝트 이름: `Allday Detoxy` (또는 원하는 이름)
   - Google Analytics 사용 여부 선택 (권장: 사용)
   - 프로젝트 생성 완료

### 2. Android 앱 추가

1. **Android 앱 추가 버튼 클릭**
   - Android 패키지 이름: `com.allday.detoxy`
   - 앱 닉네임(선택사항): `Allday Detoxy`
   - 디버그 서명 인증서 SHA-1 (선택사항): 나중에 추가 가능

2. **google-services.json 다운로드**
   - Firebase Console에서 제공하는 `google-services.json` 파일 다운로드
   - 파일을 다음 경로에 복사:
     ```
     /Users/junghojang/Developments/myProject/allday_detoxy/app/google-services.json
     ```

3. **주의사항**
   - `google-services.json`은 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다
   - 팀원과 공유 시 별도의 보안 채널 사용 필요
   - 각 환경(dev, staging, prod)별로 별도의 Firebase 프로젝트 사용 권장

### 3. Crashlytics 활성화

1. **Firebase Console에서 Crashlytics 메뉴 선택**
2. **SDK 설정** 단계 완료 (이미 코드에 추가됨)
3. **첫 빌드 및 실행**으로 연결 확인

## 🧪 테스트

### 1. 빌드 확인
```bash
cd /Users/junghojang/Developments/myProject/allday_detoxy
./gradlew clean assembleDebug
```

**예상 출력**:
- Google Services 플러그인 작동 확인
- `BUILD SUCCESSFUL`

### 2. 앱 실행 및 Crashlytics 연결 확인
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep -i firebase
```

**예상 로그**:
```
FirebaseApp: Firebase is initialized
Crashlytics: Crashlytics report upload has started
```

### 3. 테스트 크래시 발생 (선택사항)

앱 내에서 의도적으로 크래시를 발생시켜 Crashlytics가 제대로 작동하는지 확인:

```kotlin
// 디버그 메뉴 또는 특정 화면에서 테스트
Button(onClick = {
    throw RuntimeException("Test Crash for Firebase Crashlytics")
}) {
    Text("테스트 크래시 발생")
}
```

### 4. Firebase Console에서 확인
1. 앱 실행 후 크래시 발생
2. Firebase Console → Crashlytics 메뉴
3. 1-2분 후 크래시 리포트 확인

## 📊 Crashlytics 활용

### 커스텀 로그 추가
```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics

// 사용자 ID 설정 (익명화 권장)
FirebaseCrashlytics.getInstance().setUserId("user_${userId}")

// 커스텀 키 설정
FirebaseCrashlytics.getInstance().setCustomKey("timer_duration", durationMinutes)
FirebaseCrashlytics.getInstance().setCustomKey("blocked_apps_count", blockedAppsCount)

// 커스텀 로그 추가
FirebaseCrashlytics.getInstance().log("User started focus session")

// 비-치명적 예외 기록
try {
    // 위험한 작업
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
}
```

### 권장 커스텀 키
- `focus_state`: 타이머 상태 (IDLE, RUNNING, FINISHED, FAILED)
- `accessibility_enabled`: 접근성 서비스 활성화 여부
- `overlay_enabled`: 오버레이 권한 여부
- `dnd_enabled`: DND 권한 여부
- `session_count`: 총 세션 수
- `streak`: 현재 스트릭

## 🔒 보안 및 프라이버시

### 1. 개인정보 처리
- 사용자 ID는 익명화된 UUID 사용 권장
- 민감한 정보(전화번호, 이메일 등)를 로그에 포함하지 말 것

### 2. 데이터 보존
- Firebase Crashlytics는 90일간 크래시 데이터 보관
- 필요 시 데이터 내보내기 가능

### 3. 사용자 동의
- 프라이버시 정책에 크래시 리포팅 명시
- 앱 설정에서 크래시 리포팅 비활성화 옵션 제공 (선택사항)

```kotlin
// 사용자가 거부한 경우
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
```

## 📝 트러블슈팅

### 문제 1: "google-services.json not found"
**해결**: `app/google-services.json` 파일 경로 확인

### 문제 2: "Default FirebaseApp is not initialized"
**해결**: 
1. `google-services.json` 파일 존재 확인
2. 패키지 이름이 `com.allday.detoxy`와 일치하는지 확인
3. Gradle 동기화 재실행

### 문제 3: Crashlytics 리포트가 보이지 않음
**해결**:
1. 앱 실행 후 1-2분 대기
2. Release 빌드에서는 즉시 업로드되지만 Debug 빌드는 지연될 수 있음
3. Firebase Console에서 "Test mode" 확인

## 🚀 배포 시 고려사항

### Release 빌드 설정
```kotlin
// app/build.gradle.kts
buildTypes {
    release {
        // ProGuard 매핑 파일 업로드 (난독화된 스택 추적 복원)
        isMinifyEnabled = true
        
        // Crashlytics 매핑 파일 자동 업로드
        firebaseCrashlytics {
            mappingFileUploadEnabled = true
        }
    }
}
```

### NDK 크래시 리포팅 (필요 시)
```kotlin
// Native 크래시 추적이 필요한 경우
firebaseCrashlytics {
    nativeSymbolUploadEnabled = true
}
```

## 📚 참고 자료
- [Firebase Crashlytics 공식 문서](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [Firebase Console](https://console.firebase.google.com/)
- [Android Crashlytics SDK 참조](https://firebase.google.com/docs/reference/android/com/google/firebase/crashlytics/FirebaseCrashlytics)

## ✅ 완료 체크리스트

MVP 4.4 베타 준비:
- [x] Gradle 의존성 추가
- [x] Application 클래스 주석 추가
- [ ] Firebase 프로젝트 생성 (수동)
- [ ] google-services.json 파일 추가 (수동)
- [ ] 빌드 및 Crashlytics 연결 확인 (수동)
- [ ] Firebase Console에서 첫 리포트 확인 (수동)

**다음 단계**: 위의 수동 작업을 완료한 후 실제 빌드 및 테스트를 진행하세요.

