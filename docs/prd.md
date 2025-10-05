
## 0) 한 줄 정의 & 비전

- **한 줄 정의**: “디톡시 = 내가 정한 시간·장소·목표 동안 스마트폰을 ‘거의 못 쓰게’ 만들고, 성공할수록 보상과 자유 시간을 얻는 **습관 교정 코치 앱**.”
- **핵심 가치**: 강제 차단 대신 **자발적 참여 + 실질적 보상**으로 장기 사용을 유도.

## 1) 대상 사용자(페르소나)
- **청소년 자기관리형**: 시험/학원/자습 시간에 폰 유혹을 줄이고 싶은 학생.
- **성인 생산성형**: 업무/공부/운동 목표 달성 위해 집중 루틴이 필요한 사용자.
- **보호자/교사 협력형**: 자녀/학생의 폰 사용을 일정 시간 제한하고 리포트를 확인하려는 보호자/교사.

## 2) 문제 정의 & 해결 가설

- **문제**: 시간 제한 앱은 많지만, iOS/Android 정책 때문에 ‘완전 차단’이 어렵고, 사용자들은 쉽게 포기하거나 우회함.
- **가설**:
    1. **강한 UX 제약 + 보상**을 결합하면 체류·지속 사용률이 올라감.
    2. **전화만 허용 + 메시지 자동응답/SMS 중심**은 청소년/보호자에게 안심감을 줌.
    3. **위치/시간/목표 기반 자동화**는 사용자의 ‘설정 피로’를 줄여 준다.

## 3) 핵심 기능(요약)

### A. 포커스 타이머(잠금)
- 프리셋(5/10/15/25/45/60분) + 커스텀 시간
- 타이머 중 **앱 차단/알림 억제/오버레이** 적용 (OS별 방식 상이)
- 중도 포기 방지: 미션 입력/퍼즐/지연 해제(예: 15초 카운트다운) 등
### B. 연락·알림 정책
- **전화만 허용(화이트리스트 연락처 포함)**: 집중 세션 동안 전화/긴급 통화는 통과, 그 외 알림은 최대한 억제.
- **모든 알림 끄기(DND/집중 모드)**: 세션 시작 시 iOS/Android 각 OS의 DND/Focus 사용을 **유도**(앱이 직접 토글 불가; iOS는 단축어/사용자 설정이 필요).
- **자동응답의 범위(중요)**:
    - **iOS**: 앱이 임의로 SMS/iMessage 자동응답을 보낼 수 **없음**. 다만 사용자가 **‘운전 중 집중(Driving Focus)’의 OS 자동응답**을 직접 설정한 경우에 한해 **OS가** SMS/iMessage 자동응답을 보냄(우리 앱은 **설정 가이드**만 제공). 카카오톡/WhatsApp/텔레그램 등 **서드파티 메신저 자동응답은 불가**.
    - **Android**: 우리 앱이 **기본 SMS 앱으로 설정된 경우에 한해 SMS 자동응답 가능**. 서드파티 메신저 자동응답은 공식적으로 **불가/비권장**. 대신 **알림 차단 + ‘부재중 안내’ 로컬 팝업**으로 대체.
- **정리**: 자동응답은 **SMS 계열에 한정**(iOS는 Driving Focus 사용 시 OS가 처리, Android는 기본 SMS 앱일 때만 앱이 처리). **메신저 자동응답은 제공하지 않음**.
### C. 위치·상황 기반
- **iOS**: Shortcuts ‘개인 자동화’로 장소/시간/BT 연결 시 특정 Focus 켜기 (사용자 설정 유도)
- **Android**: 지오펜싱 + BT 연결 트리거로 포커스 자동 시작

### D. 보호자 플로우(옵션)
- **iOS(자녀 기기)**: FamilyControls + ManagedSettings + DeviceActivity로 앱/도메인 차단, 시간제한, Shield(차단화면)
- **Android(교육/기관 배포)**: Device Owner(키오스크) 기반 화이트리스트·설정 잠금(일반 배포는 접근성+오버레이)
    

### E. 리포트 & 보상
- 일/주/월 리포트: 성공/실패 시간, 자동응답 횟수, 차단 앱 Top
- **게이미피케이션**: 포인트, 스릭(streak), 뱃지, 아바타 성장, 데일리 퀘스트, 친구 랭킹, 협력 타이머
- **실물 리워드(선택)**: 포인트→쿠폰 교환(제휴 필요)
    

---

## 4) OS별 가능/제약 매트릭스(핵심)

|항목|iOS(일반 성인)|iOS(보호자-자녀)|Android(일반)|Android(Device Owner/MDM)|
|---|---|---|---|---|
|전화만 허용 근접|집중 모드/스크린타임 **가이드**로 일부 달성|보호자 정책으로 **강력** 달성|DND + 접근성/오버레이|화이트리스트 강제 가능|
|**SMS 자동응답**|**조건부 가능**: 사용자가 **Driving Focus 자동응답**을 직접 켤 때만 **OS가** 응답. 앱이 직접 전송 **불가**|동일|**가능**: 앱이 **기본 SMS 앱**일 때만 자동응답 구현|가능|
|**메신저 자동응답(카톡 등)**|**불가**|**불가**|**불가/비권장**(공식 API 부재)|**불가**|
|앱/웹 차단|자기관리 한도(약함)|**강력**(Shield)|접근성+오버레이로 ‘사실상 차단’|**강력**(정책 차단)|
|DND/Focus 토글|앱이 직접 토글 **불가**(단축어/사용자 자동화 유도)|정책으로 유도|앱에서 요청/유도|정책으로 강제|

> **자동응답 정리**: iOS는 **OS 내장 Driving Focus 자동응답만** 허용(사용자 설정 전제), Android는 **SMS 한정**(기본 SMS 앱일 때). 서드파티 메신저 자동응답은 일절 **미제공**.

---

## 5) UX 플로우(요약 다이어그램)
1. **온보딩**: 목표 설정 → 허용 연락처 선택 → iOS(집중 모드·단축어), Android(접근성·오버레이·DND) 권한 가이드
2. **포커스 시작**: 타이머 설정 → DND/집중 모드 유도(또는 자동화) → 차단 로직 ON
3. **진행 중**: 원형 타이머, 미션 해제 버튼(작게) → 실패 시 패널티 경고
4. **완료/실패**: 성공 보상/스릭 유지, 실패 패널티 + 리마인드
5. **리포트**: 일/주 추세, Top 방해요인, 코치 메시지
6. **보상 상점**: 스킨/스티커/아바타/쿠폰 교환
    

---

## 6) 게이미피케이션(상세)
- **포인트(FOC)**: 타이머 성공 시 획득. 긴 세션·연속 성공에 가중치.
- **스릭(Streak)**: 연속 일수 보너스(3/7/14/30일). 실패 시 초기화 또는 ‘스릭 보호권’ 사용.
- **퀘스트**: 데일리/위클리 목표(예: 25분×4, 60분×1). 랜덤 보상 상자.
- **아바타/세계관**: 집중 시간이 누적될수록 펫/정원/섬 성장. 마일스톤마다 시각적 변화.
- **협력/경쟁**: 동시 타이머, 친구 랭킹, 스터디 그룹 공동 보상.
- **실물 리워드**: 제휴 시 포인트→기프티콘/학습 서비스 할인.
- **반(反)치팅 설계**: 백그라운드 종료 감지, 화면 이탈 카운터, 빠른 해제 시 감점, 동일 일자 보상 상한.

---

## 7) 차별화 전략(요약)
1. **전화만 허용 + 메시지 자동응답(SMS 중심)**
2. **목표 기반/위치 기반/상황 기반 잠금** (단순 시간제에서 ‘똑똑한 잠금’으로)
3. **AI 코치**: 개인 패턴 분석/피드백(“8~10시는 성공률↑”)
4. **보호자/교사 연동**: 리포트 공유, 온건한 협업 정책(강압 이미지 탈피)
5. **실물 리워드 + 컬렉션**: 장기 동기부여

---

## 8) 경쟁사 요약(한국 중심)
- **터닝(iOS)**: 앱 제한/딥 포커스/미션 문구. 감성적 장치 강점, 강제력 제한.
- **스라밸(Android)**: 사용 시간 조회/앱·폰 잠금/숏폼 잠금. 국내 사용자 맞춤.
- **넌얼마나쓰니(Android)**: 앱별·그룹 잠금, 통계. 자동응답 없음.
- **알고루틴(Android)**: 차단+보상(시간→화폐) 컨셉.
- **Striving(iOS)**: 집중 타이머 + 일부 보상.
- (글로벌) **AppBlock, StayFree, Digital Detox, Freedom** 등: 제어·통계 강점, 완전 제어는 제한.
    

> **디톡시 포지셔닝**: ‘**연락은 놓치지 않고** 메시지는 비우는’ **전화-중심 안전 모드 + 보상 강화** + **위치/목표 기반 자동화**.

---

## 9) 기술 아키텍처(제안)
- **클라이언트**: Flutter(권장) — 한 번 개발로 iOS/Android 동시 빌드, 차트·UI 용이
- **iOS 네이티브 브리지**: FamilyControls/ManagedSettings/DeviceActivity, Shortcuts 안내 딥링크
- **Android 네이티브 브리지**: AccessibilityService, ForegroundService, Overlay, UsageStats, SMS(기본 앱)
- **로컬 저장소**: `isar` 또는 `sqflite` (오프라인 퍼스트)
- **클라우드(선택)**: Firebase/Supabase(익명 통계/백업), Remote Config, A/B 테스트
- **알림**: flutter_local_notifications (현지화), FCM/APNs(선택)
- **분석/로그**: Amplitude/Mixpanel/GA4 + 자체 이벤트 스키마

### 모듈 구조(예시)
- `core/` (타이머, 포인트 엔진, 권한 헬퍼)
- `platform/ios_bridge`, `platform/android_bridge`
- `features/focus`, `features/report`, `features/reward`, `features/guardian`
- `data/local`, `data/remote`
    

---

## 10) 데이터 모델(초안)

- **UserSettings**: `base_allowed_min`, `whitelist_contacts[]`, `block_categories[]`, `bonus_per_60min`, `penalty_step` …
    
- **FocusSession**: `id`, `start_at`, `end_at`, `duration`, `success(bool)`, `break_reason`, `mode(standard/guardian)`, `location_id?`
    
- **DailyTotals**: `date`, `focus_success_min`, `failed_count`, `allowed_min`, `sms_auto_replies`
    
- **Rewards**: `points`, `badges[]`, `streak_days`, `last_success_date`
    
- **GuardianLink**(옵션): `child_device_id`, `policies`, `reports[]`
    

---

## 11) 알고리즘(점진적 허용시간/보상)

- **기본 허용**: `allowed_today = base_allowed`
    
- **보너스**: `bonus_today = floor(focus_success_today / 60) * bonus_per_hour`
    
- **패널티**: 실패 2회 → `penalty += 5분`(상한 있음)
    
- **내일 허용**: `allowed_tomorrow = clamp(base_allowed + rolling_bonus - penalties, 60, 240)`
    
- **포인트**: `points += base * (1 + streak_factor + long_session_factor)`
    

---

## 12) API(백엔드 연동, 선택)

- `POST /sessions` (세션 기록 업로드)
    
- `GET /reports/daily|weekly` (리포트 페치)
    
- `POST /rewards/redeem` (포인트 사용)
    
- `POST /guardian/link` / `PATCH /guardian/policy`
    
- 인증: 익명 UID → 필요 시 계정(애플/구글)
    

---

## 13) 권한/스토어 심사 체크리스트

- **iOS**:
    
    - FamilyControls/ManagedSettings/DeviceActivity 엔타이틀먼트 설정(보호자-자녀 전용 기능임을 명시)
        
    - **자동응답 관련 고지**: “앱이 SMS/iMessage 자동응답을 직접 전송하지 않으며, 사용자가 켠 **Driving Focus 자동응답**에 한해 iOS가 처리” 문구를 노출
        
    - Focus/DND를 앱이 직접 토글하지 않음을 명확히(단축어/사용자 자동화 안내)
        
- **Android**:
    
    - 접근성 서비스 목적 명확히, 오버레이 허가 안내, 배터리 최적화 예외 안내
        
    - **기본 SMS 앱 등록 시** 개인정보/통신 고지 + 자동응답 기능 설명(사용자 동의)
        
- **공통**:
    
    - 긴급 전화 예외, 개인정보 최소 수집, 투명한 동의·철회 UX
        

---

## 14) 측정 지표(KPI) & 이벤트 스키마

- **핵심 KPI**: D1/D7 유지율, 주당 성공 분, 실패율, 스릭 평균, 일일 활성률, NPS
    
- **보조 KPI**: 자동응답 건수, 탈출 버튼 사용률, 권한 설정 완료율, 퀘스트 완료율
    
- **핵심 이벤트**: `focus_start`, `focus_finish{success}`, `give_up`, `dnd_enabled`, `sms_auto_reply_sent`, `quest_completed`, `reward_redeemed`
    

---

## 15) 로드맵(6주 출시 플랜)

- **W1–2**: 타이머, Android(접근성+오버레이+DND), 세션 저장, 기본 리포트
    
- **W3–4**: iOS(스크린타임 가이드+Driving Focus 자동응답 안내), 스릭/뱃지/포인트, 퀘스트
    
- **W5**: 주간 리포트/차트, 온보딩 튜토리얼, 알림, 간단 A/B
    
- **W6**: QA, 정책 점검, 베타 배포(TestFlight/Closed Track)
    
- **Post**: 보호자 모드(iOS 자녀 기기), 위치/목표 기반 자동화, 실물 리워드 제휴
    

---

## 16) 리스크 & 대응

- **iOS 강제성 한계** → ‘자발적 제어 + 보상’ 중심, 보호자 모드 옵션 제공
    
- **메신저 자동응답 불가** → **명시적 비기능(Non-goal)**로 문서화, 대체 UX(**알림 차단 + 로컬 부재중 안내**) 제공
    
- **SMS 자동응답 한계** → iOS는 **Driving Focus 설정 의존**(앱 직접 불가), Android는 **기본 SMS 앱 전제**를 온보딩에서 명확 고지
    
- **우회/탈출** → 미션 해제, 지연해제, 재시작 감지, 감점
    
- **배터리/백그라운드 제약** → 포그라운드 서비스/권한 안내, 저전력 모드 탐지
    
- **심사 거절 리스크** → 권한 목적 명확화, 안내 문구/UX 정교화, 자동응답 범위 고지
    

---

## 17) 화면 목록(요약)

- 온보딩(목표/연락처/권한), 메인(타이머), 잠금 화면(오버레이/Shield), 성공/실패 피드백, 리포트(일/주), 보상 상점, 설정(화이트리스트/차단 목록), 보호자 설정(옵션)
    

---

## 18) 샘플 코드 스니펫(간단)

### 18.1 Flutter 타이머 로직(주석: <추가>/<수정>/<삭제>)

```dart
// <추가> 간단한 포커스 타이머 상태 클래스 (Flutter)
import 'dart:async';

enum FocusState { idle, running, paused, finished, failed }

class FocusTimer {
  // <추가> 총 타이머 길이(초). 예: 25분 = 1500
  final int totalSeconds;

  // <추가> 내부 상태
  int _elapsed = 0;
  Timer? _ticker;
  FocusState state = FocusState.idle;

  void Function(int remaining, FocusState state)? onTick;

  FocusTimer({required this.totalSeconds});

  void start() {
    if (state == FocusState.running) return;
    state = FocusState.running;
    _ticker = Timer.periodic(const Duration(seconds: 1), (t) {
      _elapsed += 1;
      final remain = (totalSeconds - _elapsed).clamp(0, totalSeconds);
      onTick?.call(remain, state);
      if (_elapsed >= totalSeconds) {
        t.cancel();
        state = FocusState.finished;
        onTick?.call(0, state);
      }
    });
  }

  void pause() {
    if (state != FocusState.running) return;
    _ticker?.cancel();
    state = FocusState.paused;
    onTick?.call(totalSeconds - _elapsed, state);
  }

  void resume() {
    if (state != FocusState.paused) return;
    start();
  }

  void giveUp() {
    _ticker?.cancel();
    state = FocusState.failed;
    onTick?.call(totalSeconds - _elapsed, state);
  }

  void dispose() => _ticker?.cancel();
}
```

### 18.2 iOS(스위프트) — FamilyControls 권한 요청(개념 예시)

```swift
// <추가> 개념 예시: 실제 구현 시 최신 API 시그니처 확인 필요
import FamilyControls

func requestFamilyControlsAuthorization() async throws {
    // <추가> 보호자 승인 플로우 필요(가족 공유/자녀 기기 전제)
    try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
}
```

### 18.3 Android — SMS 자동응답(개념 흐름)

```kotlin
// <추가> 기본 SMS 앱으로 등록 후 BroadcastReceiver에서 수신 처리
class SmsReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    // <추가> SMS 수신 파싱 → 자동응답 전송 (사용자 설정 문구)
  }
}
```

---

## 19) 오픈 이슈 / 결정 필요 사항

- iOS 보호자 모드: 1차 출시 대상 포함 여부, 가족 공유 전제 UX 설계
    
- 실물 리워드 제휴: 초기 파트너(편의점/카페/학습 앱) 후보
    
- 위치/목표 기반 자동화: 1차/2차 릴리스 분리 범위
    
- 아바타/세계관 리소스: 일러스트/사운드 가이드라인
    

---

## 20) 즉시 실행 체크리스트(다음 액션)

1. 와이어프레임(10화면) 산출 → 온보딩/타이머/리포트/상점
    
2. Android 초MVP: 타이머+접근성+오버레이+DND+SMS 자동응답
    
3. iOS: 온보딩 가이드(Driving Focus/Shortcuts/스크린타임) + 기본 리포트
    
4. 보상 엔진(포인트/스릭/퀘스트) 베타 구현
    
5. 이벤트 스키마 적용 & 대시보드(Amplitude)
    
6. 베타 사용자 30명 폐쇄 테스트 & 피드백 반영
    

---

> 본 문서는 실제 구현 가능성과 스토어 정책을 모두 고려한 **현실적 PRD 초안**입니다. 세부 API/권한/심사 체크는 개발 시작 시점에 최신 문서로 재검증해 주세요.

---

## 부록 A) 자동응답 기능 진실표(Truth Table)

|시나리오|iOS(일반)|iOS(보호자)|Android(일반)|Android(Device Owner)|
|---|---|---|---|---|
|SMS/iMessage 자동응답|**가능(조건부)** — 사용자가 **Driving Focus 자동응답**을 켠 경우에 **OS가** 응답. 앱 직접 전송 불가|동일|**가능** — 앱이 **기본 SMS 앱**일 때만|가능|
|카카오톡/WhatsApp/텔레그램 자동응답|**불가**|**불가**|**불가/비권장**|**불가**|

## 변경 이력(Changelog)

- **v1.0.1**: 자동응답 범위/책임 주체(iOS=OS, Android=기본 SMS 앱) 명확화. 메신저 자동응답 **미제공**으로 문서 전면 정정. 권한/심사 체크리스트와 리스크 항목 보강.