# Allday Detoxy - 스마트폰 습관 교정 코치 앱

## 📱 프로젝트 소개
Allday Detoxy는 사용자의 스마트폰 과다 사용을 방지하고 집중 시간을 늘리는 Android 네이티브 앱입니다.

**MVP 목표**: 앱 차단 + 보상 시스템을 통해 사용자의 집중 시간 증가 검증

## 🛠 기술 스택

### 언어 & 프레임워크
- **Kotlin**: Android 네이티브 개발
- **Jetpack Compose**: 선언형 UI

### 아키텍처 & 라이브러리
- **Clean Architecture**: domain, data, presentation 계층 분리
- **Hilt**: 의존성 주입 (Dependency Injection)
- **Room**: 로컬 데이터베이스 (SQLite)
- **Kotlin Coroutines**: 비동기 처리
- **StateFlow/Flow**: 반응형 데이터 스트림

### Android 시스템 기능
- **AccessibilityService**: 앱 차단 감지 및 제어
- **Overlay Service**: 전체 화면 잠금 UI
- **NotificationManager**: 방해금지 모드(DND) 제어
- **Foreground Service**: 백그라운드 차단 서비스

## 🏗 프로젝트 아키텍처

```
app/
├── core/          # 공통 모듈
│   ├── di/        # Hilt 의존성 주입 모듈
│   └── utils/     # 유틸리티 함수
├── data/          # 데이터 계층
│   ├── local/     # Room 데이터베이스
│   └── repository/# Repository 구현
├── domain/        # 비즈니스 로직 계층
│   ├── model/     # 도메인 모델
│   └── usecase/   # UseCase
├── presentation/  # UI 계층
│   ├── ui/        # Compose 화면
│   └── viewmodel/ # ViewModel
└── service/       # Android 서비스
    ├── accessibility/ # 앱 차단 서비스
    └── overlay/   # 오버레이 잠금 서비스
```

## 📋 주요 기능 (MVP)

1. **타이머 기반 집중 모드**
   - 프리셋 타이머 (25분, 45분, 60분)
   - 타이머 실행 중 특정 앱 차단

2. **앱 차단 기능**
   - Instagram, TikTok, YouTube, Facebook 차단
   - AccessibilityService를 통한 실시간 감지

3. **전체 화면 잠금**
   - 오버레이를 통한 잠금 화면 표시
   - 남은 시간 표시 및 포기 옵션

4. **방해금지 모드(DND)**
   - 타이머 실행 중 알림 차단

5. **보상 시스템**
   - 포인트 지급 (1분 = 1포인트)
   - 연속 성공 스트릭(streak) 추적

6. **일일 리포트**
   - 오늘 성공한 세션 수
   - 총 집중 시간
   - 현재 스트릭 및 총 포인트

## 🚀 빌드 및 실행

### 요구사항
- Android Studio Hedgehog | 2023.1.1 이상
- JDK 17 이상
- Android SDK API 34
- Android SDK Build-Tools 34.0.0
- Gradle 8.2 이상

### 설치 방법

1. **저장소 클론**
   ```bash
   git clone https://github.com/[username]/allday_detoxy.git
   cd allday_detoxy
   ```

2. **Android Studio에서 프로젝트 열기**
   - File → Open → 프로젝트 폴더 선택

3. **Gradle 동기화**
   - Android Studio에서 자동으로 Gradle 동기화 실행
   - 또는 File → Sync Project with Gradle Files

4. **에뮬레이터 또는 실제 기기 연결**
   - API 26 이상 필요
   - 권장: API 34 (Android 14) 에뮬레이터

5. **빌드 및 실행**
   - Run → Run 'app' (Shift + F10)

### 필수 권한 설정

앱 실행 후 다음 권한을 수동으로 설정해야 합니다:

1. **접근성 서비스 권한**
   - 설정 → 접근성 → Allday Detoxy 활성화

2. **다른 앱 위에 표시 권한**
   - 설정 → 앱 → 특수 앱 액세스 → 다른 앱 위에 표시 → Allday Detoxy 허용

3. **방해금지 모드 액세스 권한**
   - 설정 → 알림 → 방해 금지 모드 액세스 → Allday Detoxy 허용

## 📊 MVP 성공 지표

- **D1 유지율**: 60% 이상
- **타이머 완주율**: 50% 이상
- **주간 활성 사용자**: 40% 이상 (주 3회 이상 사용)
- **평균 세션 시간**: 20분 이상

## 📝 참조 문서

- [PRD 문서](./docs/prd.md) - 제품 요구사항 정의서
- [MVP 개발 계획](./docs/00_mvp_allday_detoxy_todolist.md) - 4주 개발 일정
- [기술 설계 문서](./docs/00_android_allday_detoxy_plan.md) - 상세 기술 설계
- [개발환경 설정](./docs/00_kotlin_environment_todolist.md) - macOS 환경 구성

## 📄 라이선스

MIT License (예정)

## 👨‍💻 개발자

Jung Ho Jang - [@junghojang](https://github.com/junghojang)
