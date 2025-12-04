# iOS 플랫폼 개발 정리 (2025-10-13)

## 1. 배경 및 목표
- Android용 Allday Detoxy의 핵심 기능(타이머 기반 집중 모드, 앱 차단, 통계·보상)을 iOS에서도 제공하되, 강제적인 차단보다 **자발적 목표 달성과 통계 기반 동기 부여**를 중심으로 UX를 설계한다.
- iOS 특성상 Android에서 사용하던 `AccessibilityService`, 커스텀 전체 화면 오버레이, DND 강제 제어는 사용할 수 없으므로 **Screen Time 공식 프레임워크**로 기능을 재구성한다.
- 최종 목표는 사용자가 직접 설정한(또는 앱이 제안한) 집중 목표를 점진적으로 달성하게 돕고, 세션 통계를 통해 스마트폰 과사용에서 벗어날 동기를 강화하는 것이다.

## 2. 지원 가능한 iOS 시스템 기능
| 영역 | Android 구현 | iOS 대응 방안 | 비고 |
| --- | --- | --- | --- |
| 앱 차단 | 접근성 이벤트 감지 후 홈 이동 | `FamilyControls` + `ManagedSettings`를 통해 선택된 앱 토큰을 Shield 처리 | Screen Time 권한 필수 |
| 오버레이 | `SYSTEM_ALERT_WINDOW`로 LockOverlayService 표시 | 시스템 제공 **Shield UI** 구성 (`ShieldConfiguration`) | 완전 커스텀 뷰 불가 |
| 차단 유지 | AccessibilityService로 재진입 차단 | `shield.applications` 재적용 / DeviceActivity 스케줄 | 사용자가 Screen Time을 끄면 해제 |
| 방해 금지 모드 | `NotificationManager`로 DND 강제 | 퍼블릭 API 없음 → 사용자 안내/단축어 자동화 대체 | 기능 축소 |
| 데이터/보상 | Room, GamificationManager | SwiftData/CoreData 또는 공용 서비스, 동일 계산 로직 유지 | KMP/서버 공유 고려 |

## 3. Screen Time 및 Shield UI 개요
1. **권한 흐름**
   - 앱이 FamilyControls를 사용하려면 애플로부터 `Family Controls`, `Managed Settings`, `Device Activity` 엔타이틀먼트를 발급받아야 한다.
   - 최초 실행 시 사용자(보호자)가 Screen Time 접근을 승인해야 Shield 차단이 동작한다.
2. **차단 적용**
   - `FamilyActivityPicker`로 선택한 앱/카테고리 토큰을 저장하고, 집중 모드 시작 시 `ManagedSettingsStore().shield.applications`에 전달한다.
   - 대상 앱을 열면 시스템이 자동으로 Shield UI를 띄워 원래 화면을 가린다.
3. **Shield UI 구성**
   - `ShieldConfiguration`을 통해 제목, 부가 설명, 심볼, 버튼(최대 2개) 텍스트·스타일을 지정한다.
   - 버튼 동작은 `ShieldActionHandler`에서 `completionHandler(.close / .defer / .none)`으로 응답한다.
     - `.close`: 차단된 앱이 즉시 포그라운드에서 내려가며 홈으로 이동.
     - `.defer`: 일시 해제 로직을 구현해 제한 시간을 연장.
     - `.none`: 화면을 그대로 유지.
4. **해제 및 예외**
   - 앱 내부에서 Shield 토큰을 제거하거나 사용자가 Screen Time 설정에서 제한을 해제하면 즉시 접속 가능.
   - iOS 17~18 구간에서 Shield 재적용이 지연되는 버그 사례가 있어, 앱 활성화 시점에 재동기화 로직을 준비해야 한다.

## 4. 정책 및 심사 고려사항
- 2025년 6월 애플 발표(“Apple expands tools to help parents protect kids and teens online”) 이후 가족 보호/연령 관련 심사 강화:
  - **Declared Age Range API** 적용: 사용자 연령대 수집/선택 로직 명확화.
  - **PermissionKit** 승인: 자녀 기기 제어 시 추가 사용자 동의 필요.
- Screen Time 데이터 이용 목적과 저장 범위를 투명하게 고지하고, 개인정보 처리 방침에 명시해야 한다.
- Screen Time 버그(제한 값 초기화 등)에 대비해 앱 내 가이드를 제공하고, 리뷰 대응 문서에 한계점을 정리해 제출한다.

## 5. 기능 설계 방향
1. **목표 기반 UX**
   - 사용자 정의 목표 + 앱 추천 목표(프리셋)를 선택하게 하고, 난이도 조절 시나리오로 점진적 성공 경험을 제공.
2. **차단 경험**
   - Shield UI에는 “현재 집중 모드 진행 중” 메시지, 남은 시간, 오늘 목표 달성률 등을 표시(프레임워크 허용 범위 내).
   - 닫기 버튼은 `.close`로 처리해 앱을 홈으로 돌려보내되, 차단 상태는 유지해 재진입 시 다시 Shield가 뜨도록 한다.
3. **통계/보상**
   - Android와 동일한 포인트·스트릭·세션 기록 규칙 적용.
   - 일별/주별 리포트, 누적 집중 시간, 재도전 횟수 등을 시각화하여 자발적 재참여를 유도.
4. **우회 대비 UX**
   - Screen Time이 꺼졌을 때의 안내(재설정 튜토리얼)와 알림/단축어 자동화 예시를 제공.
   - 집중 세션 종료 시 즉시 차단을 해제해 사용자 자율성을 강조.

## 6. 로드맵 제안
- **Phase 0 (2025.10~11)**  
  - iOS Screen Time 프로토타입 제작, 엔타이틀먼트 신청.  
  - Android 도메인 로직을 공용 모듈(KMP 또는 서비스)로 추출하는 설계 착수.
- **Phase 1 (2025.12~2026.01)**  
  - iOS 타이머·세션 로깅·Shield UX 구현, Android가 공용 코어를 사용하도록 리팩터링.  
  - 크로스 플랫폼 분석 이벤트 표준화.
- **Phase 2 (2026.02~03)**  
  - 보상/리포트/목표 추천 기능을 양 플랫폼에 정렬.  
  - Declared Age Range, PermissionKit 흐름을 온보딩에 통합.
- **Phase 3 (2026.04)**  
  - 통합 베타/리그레션 테스트, Screen Time 회귀 체크 자동화, 지역화된 온보딩/리뷰 자료 완비 후 동시 출시.

## 7. QA 및 운영 포인트
- Shield 적용/해제, 재부팅 후 유지, Screen Time 암호 변경 등 시나리오를 주기적으로 수동 테스트한다.
- 제한 해제 시 사용자에게 빠르게 재설정 안내를 제공하는 인앱 알림/가이드를 마련한다.
- 양 플랫폼에서 동일한 KPI(타이머 완주율, 스트릭, 주간 집중 시간)를 추적해 A/B 실험과 개선안을 공유한다.
- `working_history/`에 iOS 관련 작업 로그, 실행 명령, 테스트 결과, 심사 이슈를 지속적으로 기록한다.

## 8. 다음 행동 항목
1. **iOS 스파이크 브랜치 개설**: FamilyControls/ManagedSettings/DeviceActivity 데모 앱 작성 및 동작 검증.
2. **공유 도메인 결정**: Kotlin Multiplatform vs. 백엔드 서비스 중 채택안과 영향 분석 문서화.
3. **심사 준비 패키지 정리**: 동작 영상, 연령·데이터 사용 근거, Screen Time 한계 설명, 사용자 가이드 초안 작성.
4. **QA 체크리스트 업데이트**: Shield 동작, Screen Time 우회, 통계 일관성 등 iOS 전용 항목 추가.

> 이 문서는 iOS 플랫폼 대응을 위한 현재까지의 논의 내용을 집약한 것이며, 향후 애플 정책 변경이나 기술 검증 결과에 따라 업데이트해야 한다.

