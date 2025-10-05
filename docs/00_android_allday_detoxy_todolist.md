# 안드로이드 Allday Detoxy 개발 할 일 목록

## 📋 프로젝트 개요
**프로젝트명**: Allday Detoxy - 스마트폰 습관 교정 코치 앱  
**기술 스택**: Kotlin 네이티브 + Jetpack Compose  
**개발환경**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) 참조  
**PRD 문서**: [prd.md](./prd.md) 참조  
**기술 설계**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) 참조

---

## 🎯 개발 단계별 우선순위

### Phase 1: MVP 핵심 기능 (2주)
- **목표**: 기본적인 앱 차단 및 타이머 기능 구현
- **완료 기준**: 사용자가 설정한 시간 동안 앱을 차단하고 타이머를 완료할 수 있음

### Phase 2: 통신 및 자동화 (1주)  
- **목표**: SMS 자동응답 및 DND 제어 기능 구현
- **완료 기준**: 집중 모드 중 SMS 자동응답 및 알림 차단 동작

### Phase 3: 데이터 및 게이미피케이션 (1주)
- **목표**: 데이터 저장, 포인트/스릭 시스템, 리포트 기능
- **완료 기준**: 사용자 데이터 저장 및 보상 시스템 동작

### Phase 4: 고도화 및 최적화 (2주)
- **목표**: 위치 기반 자동화, AI 코치, 성능 최적화
- **완료 기준**: 베타 테스트 준비 완료

---

## 📅 Phase 1: MVP 핵심 기능 (2주)

### 1.1 프로젝트 기본 구조 설정 (1일)

#### 1.1.1 폴더 구조 생성
- [ ] **프로젝트 폴더 구조 생성**
  ```
  app/src/main/java/com/allday/detoxy/
  ├── core/
  │   ├── di/           # 의존성 주입
  │   ├── network/      # 네트워크 관련
  │   └── utils/        # 유틸리티
  ├── data/
  │   ├── local/        # 로컬 데이터 (Room)
  │   ├── remote/       # 원격 데이터
  │   └── repository/   # Repository 구현
  ├── domain/
  │   ├── model/        # 도메인 모델
  │   ├── repository/   # Repository 인터페이스
  │   └── usecase/      # UseCase
  ├── presentation/
  │   ├── ui/           # Compose UI
  │   ├── viewmodel/    # ViewModel
  │   └── navigation/   # 네비게이션
  └── service/
      ├── accessibility/ # AccessibilityService
      ├── overlay/      # 오버레이 서비스
      └── sms/          # SMS 서비스
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 2.2 프로젝트 구조 설정

#### 1.1.2 Hilt 의존성 주입 설정
- [ ] **Application 클래스 생성**
  ```kotlin
  @HiltAndroidApp
  class DetoxyApplication : Application()
  ```
  - **참조**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) - 5.1 Hilt 설정

- [ ] **MainActivity Hilt 설정**
  ```kotlin
  @AndroidEntryPoint
  class MainActivity : ComponentActivity()
  ```
  - **참조**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) - 5.1 Hilt 설정

- [ ] **기본 DI 모듈 생성**
  - DatabaseModule
  - RepositoryModule  
  - ServiceModule
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.2 Repository 패턴 구현

### 1.2 AccessibilityService 구현 (2일)

#### 1.2.1 AccessibilityService 기본 구조
- [ ] **FocusAccessibilityService 클래스 생성**
  ```kotlin
  class FocusAccessibilityService : AccessibilityService() {
      override fun onAccessibilityEvent(event: AccessibilityEvent?)
      override fun onInterrupt()
      fun blockApp(packageName: String)
      fun isAppBlocked(packageName: String): Boolean
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 2.2 AccessibilityService 구현

#### 1.2.2 앱 모니터링 로직
- [ ] **앱 사용 감지 및 차단**
  - 현재 실행 중인 앱 패키지명 감지
  - 차단 목록과 비교하여 차단 여부 결정
  - 차단된 앱 실행 시 홈 화면으로 이동
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 A. 포커스 타이머(잠금)

#### 1.2.3 AccessibilityService 설정 파일
- [ ] **accessibility_service_config.xml 생성**
  ```xml
  <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
      android:accessibilityEventTypes="typeWindowStateChanged"
      android:accessibilityFeedbackType="feedbackGeneric"
      android:accessibilityFlags="flagDefault"
      android:canRetrieveWindowContent="true"
      android:description="@string/accessibility_service_description"
      android:notificationTimeout="100"
      android:packageNames="com.android.launcher" />
  ```
  - **참조**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) - 4.2 서비스 선언

#### 1.2.4 권한 요청 및 설정 가이드
- [ ] **접근성 서비스 권한 요청 UI**
  - 권한 설정 화면으로 이동하는 버튼
  - 설정 방법 안내 다이얼로그
  - 권한 상태 확인 로직
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🚨 주의사항 및 제약사항

### 1.3 오버레이 잠금 화면 구현 (2일)

#### 1.3.1 LockOverlayService 구현
- [ ] **포그라운드 서비스 생성**
  ```kotlin
  class LockOverlayService : Service() {
      private var overlayView: View? = null
      override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
      private fun showOverlay()
      private fun hideOverlay()
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 2.4 오버레이 잠금 화면

#### 1.3.2 오버레이 UI 디자인
- [ ] **잠금 화면 UI 구현**
  - 전체 화면 반투명 오버레이
  - 원형 타이머 표시
  - 남은 시간 표시
  - 미션 해제 버튼 (작게)
  - **참조**: [prd.md](./prd.md) - 5) UX 플로우 3. 진행 중

#### 1.3.3 오버레이 권한 관리
- [ ] **SYSTEM_ALERT_WINDOW 권한 처리**
  - 권한 요청 다이얼로그
  - 설정 화면으로 이동
  - 권한 상태 확인
  - **참조**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) - 4.1 필수 권한 추가

### 1.4 기본 타이머 기능 구현 (2일)

#### 1.4.1 타이머 상태 관리
- [ ] **FocusTimer 클래스 구현**
  ```kotlin
  enum class FocusState { idle, running, paused, finished, failed }
  class FocusTimer {
      fun start()
      fun pause()
      fun resume()
      fun giveUp()
  }
  ```
  - **참조**: [prd.md](./prd.md) - 18) 샘플 코드 스니펫 18.1 Flutter 타이머 로직

#### 1.4.2 타이머 UI 구현
- [ ] **TimerScreen Compose UI**
  - 프리셋 버튼 (5/10/15/25/45/60분)
  - 커스텀 시간 입력
  - 원형 프로그레스 바
  - 시작/일시정지/포기 버튼
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 4.2 타이머 화면

#### 1.4.3 타이머와 서비스 연동
- [ ] **타이머 시작 시 서비스 활성화**
  - AccessibilityService 활성화
  - LockOverlayService 시작
  - DND 모드 활성화
  - **참조**: [prd.md](./prd.md) - 5) UX 플로우 2. 포커스 시작

### 1.5 DND (방해금지 모드) 제어 (1일)

#### 1.5.1 DND 권한 및 제어
- [ ] **DND 모드 제어 구현**
  ```kotlin
  class DndManager {
      fun enableDnd()
      fun disableDnd()
      fun isDndEnabled(): Boolean
  }
  ```
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 B. 연락·알림 정책

#### 1.5.2 알림 정책 설정
- [ ] **알림 차단 로직**
  - 전화만 허용 (화이트리스트 연락처)
  - SMS 알림 차단
  - 앱 알림 차단
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 B. 연락·알림 정책

---

## 📅 Phase 2: 통신 및 자동화 (1주)

### 2.1 SMS 자동응답 시스템 (3일)

#### 2.1.1 기본 SMS 앱 등록
- [ ] **SMS 앱 등록 기능**
  - 기본 SMS 앱으로 설정하는 UI
  - 설정 방법 안내
  - 권한 요청 처리
  - **참조**: [prd.md](./prd.md) - 4) OS별 가능/제약 매트릭스 - SMS 자동응답

#### 2.1.2 SmsReceiver 구현
- [ ] **SMS 수신 처리**
  ```kotlin
  class SmsReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent)
  }
  ```
  - **참조**: [prd.md](./prd.md) - 18) 샘플 코드 스니펫 18.3 Android — SMS 자동응답

#### 2.1.3 자동응답 로직
- [ ] **자동응답 전송 기능**
  - 집중 모드 중일 때만 자동응답
  - 화이트리스트 연락처 제외
  - 사용자 설정 메시지 전송
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 B. 연락·알림 정책

#### 2.1.4 자동응답 설정 UI
- [ ] **자동응답 메시지 설정**
  - 기본 메시지 템플릿
  - 커스텀 메시지 입력
  - 미리보기 기능
  - **참조**: [prd.md](./prd.md) - 17) 화면 목록 - 설정 화면

### 2.2 연락처 관리 시스템 (2일)

#### 2.2.1 화이트리스트 연락처 관리
- [ ] **연락처 선택 UI**
  - 연락처 목록 표시
  - 다중 선택 기능
  - 검색 기능
  - **참조**: [prd.md](./prd.md) - 5) UX 플로우 1. 온보딩

#### 2.2.2 연락처 데이터 저장
- [ ] **연락처 데이터 모델**
  ```kotlin
  @Entity(tableName = "whitelist_contacts")
  data class WhitelistContact(
      @PrimaryKey val phoneNumber: String,
      val name: String,
      val isEmergency: Boolean = false
  )
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.1 Room 데이터베이스 설정

#### 2.2.3 긴급 연락처 처리
- [ ] **긴급 통화 허용**
  - 긴급 연락처는 항상 허용
  - 전화 수신 시 오버레이 일시 해제
  - 통화 종료 후 다시 잠금
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 B. 연락·알림 정책

---

## 📅 Phase 3: 데이터 및 게이미피케이션 (1주)

### 3.1 Room 데이터베이스 구현 (2일)

#### 3.1.1 데이터 모델 정의
- [ ] **핵심 엔티티 생성**
  ```kotlin
  @Entity(tableName = "focus_sessions")
  data class FocusSession(
      @PrimaryKey val id: String,
      val startTime: Long,
      val endTime: Long?,
      val duration: Int,
      val success: Boolean,
      val breakReason: String?
  )
  
  @Entity(tableName = "user_settings")
  data class UserSettings(
      @PrimaryKey val id: Int = 1,
      val baseAllowedMin: Int,
      val whitelistContacts: String,
      val blockCategories: String,
      val bonusPer60Min: Int
  )
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.1 Room 데이터베이스 설정

#### 3.1.2 DAO 인터페이스 구현
- [ ] **데이터 접근 객체 생성**
  - FocusSessionDao
  - UserSettingsDao
  - WhitelistContactDao
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.1 Room 데이터베이스 설정

#### 3.1.3 데이터베이스 설정
- [ ] **Room 데이터베이스 구성**
  ```kotlin
  @Database(
      entities = [FocusSession::class, UserSettings::class, WhitelistContact::class],
      version = 1
  )
  abstract class DetoxyDatabase : RoomDatabase()
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.1 Room 데이터베이스 설정

### 3.2 Repository 패턴 구현 (1일)

#### 3.2.1 Repository 인터페이스
- [ ] **도메인 Repository 정의**
  ```kotlin
  interface FocusRepository {
      suspend fun startFocusSession(duration: Int): Result<FocusSession>
      suspend fun endFocusSession(sessionId: String, success: Boolean): Result<Unit>
      suspend fun getUserSettings(): UserSettings
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.2 Repository 패턴 구현

#### 3.2.2 Repository 구현체
- [ ] **Repository 구현**
  - FocusRepositoryImpl
  - UserSettingsRepositoryImpl
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 3.2 Repository 패턴 구현

### 3.3 게이미피케이션 시스템 (2일)

#### 3.3.1 포인트 시스템
- [ ] **GamificationManager 구현**
  ```kotlin
  class GamificationManager {
      fun calculatePoints(session: FocusSession): Int
      fun updateStreak(success: Boolean): Int
      fun checkBadges(session: FocusSession): List<Badge>
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 5.1 포인트 및 스릭 시스템

#### 3.3.2 스릭(연속 성공) 시스템
- [ ] **연속 성공 추적**
  - 일일 성공 여부 기록
  - 연속 일수 계산
  - 스릭 보너스 포인트
  - **참조**: [prd.md](./prd.md) - 6) 게이미피케이션(상세)

#### 3.3.3 뱃지 시스템
- [ ] **뱃지 획득 로직**
  - 첫 성공 뱃지
  - 연속 성공 뱃지
  - 시간별 뱃지 (1시간, 2시간 등)
  - **참조**: [prd.md](./prd.md) - 6) 게이미피케이션(상세)

#### 3.3.4 퀘스트 시스템
- [ ] **데일리/위클리 퀘스트**
  - 데일리 목표 설정
  - 퀘스트 완료 추적
  - 보상 지급
  - **참조**: [prd.md](./prd.md) - 6) 게이미피케이션(상세)

### 3.4 리포트 시스템 (1일)

#### 3.4.1 일일 리포트
- [ ] **일일 통계 UI**
  - 성공/실패 시간
  - 자동응답 횟수
  - 차단된 앱 Top 5
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 E. 리포트 & 보상

#### 3.4.2 주간 리포트
- [ ] **주간 통계 UI**
  - 주간 성공률
  - 평균 집중 시간
  - 스릭 현황
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 E. 리포트 & 보상

#### 3.4.3 차트 구현
- [ ] **MPAndroidChart 활용**
  - 시간별 집중 패턴
  - 주간/월간 트렌드
  - 앱 사용량 차트
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🔧 개발 도구 및 라이브러리

---

## 📅 Phase 4: 고도화 및 최적화 (2주)

### 4.1 위치 기반 자동화 (3일)

#### 4.1.1 지오펜싱 구현
- [ ] **위치 기반 자동 시작**
  - 특정 장소 도착 시 자동 포커스 시작
  - 지오펜싱 API 활용
  - 사용자 설정 UI
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 C. 위치·상황 기반

#### 4.1.2 블루투스 연결 감지
- [ ] **BT 기반 자동화**
  - 특정 기기 연결 시 자동 시작
  - 헤드폰/이어폰 연결 감지
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 C. 위치·상황 기반

### 4.2 AI 코치 기능 (2일)

#### 4.2.1 패턴 분석
- [ ] **사용 패턴 분석**
  - 성공률이 높은 시간대 분석
  - 실패 원인 분석
  - 개인화된 피드백 제공
  - **참조**: [prd.md](./prd.md) - 7) 차별화 전략 3. AI 코치

#### 4.2.2 코치 메시지
- [ ] **개인화된 조언**
  - 성공 패턴 기반 조언
  - 실패 시 격려 메시지
  - 목표 달성 가이드
  - **참조**: [prd.md](./prd.md) - 7) 차별화 전략 3. AI 코치

### 4.3 보상 상점 시스템 (2일)

#### 4.3.1 보상 상점 UI
- [ ] **상점 화면 구현**
  ```kotlin
  @Composable
  fun RewardsScreen() {
      // 포인트 잔액, 상품 목록, 구매 기능
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 5.2 보상 상점

#### 4.3.2 상품 및 구매 시스템
- [ ] **상품 관리**
  - 아바타/스킨 상품
  - 스티커/테마 상품
  - 실물 리워드 (제휴 필요)
  - **참조**: [prd.md](./prd.md) - 6) 게이미피케이션(상세)

### 4.4 성능 최적화 (2일)

#### 4.4.1 배터리 최적화
- [ ] **배터리 사용량 최적화**
  - 포그라운드 서비스 최적화
  - 백그라운드 작업 최소화
  - 배터리 최적화 예외 설정 안내
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🚨 주의사항 및 제약사항

#### 4.4.2 메모리 최적화
- [ ] **메모리 누수 방지**
  - 오버레이 뷰 메모리 관리
  - 서비스 생명주기 관리
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🚨 주의사항 및 제약사항

### 4.5 테스트 및 QA (1일)

#### 4.5.1 단위 테스트
- [ ] **핵심 로직 테스트**
  ```kotlin
  @Test
  fun `포커스 세션 시작 시 올바른 데이터 저장`() {
      // 테스트 로직
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 6.1 단위 테스트

#### 4.5.2 UI 테스트
- [ ] **UI 동작 테스트**
  ```kotlin
  @Test
  fun `타이머 시작 버튼 클릭 시 타이머 시작`() {
      // UI 테스트 로직
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 6.2 UI 테스트

---

## 🎯 성공 지표 및 검증

### 기술적 지표
- [ ] **앱 차단 성공률 95% 이상**
  - AccessibilityService 동작 확인
  - 차단된 앱 실행 시 홈 화면 이동 확인
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🎯 성공 지표

- [ ] **SMS 자동응답 전송률 100%**
  - 집중 모드 중 SMS 수신 시 자동응답 확인
  - 화이트리스트 연락처 제외 확인
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🎯 성공 지표

- [ ] **배터리 사용량 최적화 (일일 5% 이하)**
  - 배터리 사용량 모니터링
  - 포그라운드 서비스 최적화
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🎯 성공 지표

### 사용자 지표
- [ ] **D1 유지율 80% 이상**
  - 첫 사용 후 다음날 재사용률 측정
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🎯 성공 지표

- [ ] **주간 성공 세션 3회 이상**
  - 주간 완료된 포커스 세션 수 측정
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 🎯 성공 지표

---

## 📝 다음 액션 아이템

### 즉시 실행 (개발환경 구축 완료 후)
1. **프로젝트 기본 구조 설정**
   - [ ] 폴더 구조 생성
   - [ ] Hilt 의존성 주입 설정
   - [ ] 기본 DI 모듈 생성

2. **AccessibilityService 구현**
   - [ ] FocusAccessibilityService 클래스 생성
   - [ ] 앱 모니터링 로직 구현
   - [ ] 권한 요청 UI 구현

3. **오버레이 잠금 화면 구현**
   - [ ] LockOverlayService 구현
   - [ ] 잠금 화면 UI 디자인
   - [ ] 오버레이 권한 관리

### 1주차 목표
- [ ] 기본 타이머 기능 완성
- [ ] AccessibilityService 동작 확인
- [ ] 오버레이 잠금 화면 동작 확인
- [ ] DND 제어 기능 구현

### 2주차 목표
- [ ] SMS 자동응답 시스템 구현
- [ ] 연락처 관리 시스템 구현
- [ ] 기본 UI 완성

### 3주차 목표
- [ ] Room 데이터베이스 구현
- [ ] 게이미피케이션 시스템 구현
- [ ] 리포트 시스템 구현

### 4주차 목표
- [ ] 위치 기반 자동화 구현
- [ ] AI 코치 기능 구현
- [ ] 성능 최적화 완료

---

## 🔗 참조 문서

- **PRD 문서**: [prd.md](./prd.md) - 전체 요구사항 및 기능 명세
- **개발환경 구축**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) - 개발환경 설정 가이드
- **기술 설계**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 기술 스택 및 아키텍처 설계

---

> **참고**: 이 할 일 목록은 PRD의 요구사항을 바탕으로 Kotlin 네이티브의 장점을 최대한 활용한 현실적인 개발 계획입니다. 각 단계별로 철저한 테스트와 사용자 피드백을 반영하여 안정적인 앱을 구축할 예정입니다.
