## [2025-11-14] bug list 
- [x] 위치기반 스케쥴을 등록후 해당 스케쥴에 시간을 추가 또는 편집하면 위치와 상관없이 시간 스케쥴로 가동된다. 
  - 관련 수정: [버그 1, 6, 7, 8, 9](../working_history/2025-11-14_fixed_bugs_01.md#1-위치기반-스케쥴-시간-추가편집-시-위치-정보-유지-문제)
  - 주요 수정 사항:
    - 버그 1: 위치 정보 유지 로직 추가
    - 버그 6: scheduleGroupId 미설정 문제 수정
    - 버그 7: 알람 등록 시 그룹 활성화 상태 확인
    - 버그 8: actualIsLocationBased 계산 로직 추가
    - 버그 9: **근본 원인** - 위치기반 스케쥴 그룹 초기 활성화 상태를 false로 설정
- [x] 위치기반 스케쥴을 등록하는 과정에서 '시간표 만들기'과정에서 '시간 방법 선택'항목을 템플릿이 default으로 설정되어야 한다. 
  - 관련 수정: [버그 2](../working_history/2025-11-14_fixed_bugs_01.md#2-위치기반-스케쥴-생성-시-시간-방법-선택-기본값-설정)
- [x] 권한 상태를 변경하고 다시 해당 앱 화면으로 돌아오면 여전히 권한설정이 변경되지 않은 것으로 보여진다. 하지만 앱을 재실행 (껐다 켜기)하면 정상적으로 보인다.
  - 관련 수정: [버그 3](../working_history/2025-11-14_fixed_bugs_01.md#3-권한-상태-변경-후-화면-복귀-시-권한-상태-업데이트-문제)

## [2025-11-15] bug list
- [x] 일반 시간 스케쥴(어디서나 적용)이 비활성화되어 작동하지 않음
  - 관련 수정: [버그 1](../working_history/2025-11-15_fixed_bugs_01.md#1-일반-시간-스케쥴-비활성화-문제-버그-9번-수정으로-인한-부작용)
  - 원인: 버그 9번 수정으로 모든 스케쥴 그룹을 `isActive = false`로 생성하여 일반 시간 스케쥴도 비활성화됨
  - 수정: `isLocationBased` 파라미터를 추가하여 위치기반 여부에 따라 초기 활성화 상태를 구분
    - 위치기반 스케쥴: `isActive = false` (위치 진입 시 활성화)
    - 일반 시간 스케쥴: `isActive = true` (즉시 활성화)

## [2025-11-18] bug list
- [x] 위치기반 스케쥴 등록 오류 
  - 관련 수정: [버그 1](../working_history/2025-11-18_fixed_bugs_01.md#1-위치기반-스케줄-등록-시-자동-활성화-설정-비활성화-문제)
  - 문제: 자동 활성화 설정 부분이 활성화되지 않음, 수정 기능을 통해서도 활성화 후 저장해도 해당 정보가 저장되지 않음
  - 원인: 
    - 자동 활성화 설정 스위치의 `enabled` 속성이 `isEnabled`에 의존하여 Geofence 실패 시 비활성화됨
    - Geofence 실패 시 `activateScheduleOnEnter`와 `deactivateScheduleOnExit`를 강제로 `false`로 설정하여 사용자 설정이 무시됨
  - 수정:
    - 스위치의 `enabled` 조건을 `isEnabled`에서 `linkedScheduleGroupId != null`로 변경
    - Geofence 실패 시에도 사용자 설정값 유지 (나중에 Geofence 등록되면 작동할 수 있도록)

## [2025-12-04] bug list
- [x] 스케쥴에 따라 실행된 집중모드가 실패로 잡힘, 실제로는 성공하였음. 
  - 관련 수정: [버그 수정](../working_history/2025-12-04_fix_session_success_failure_bug.md)
  - 주요 수정 사항:
    - 정상 완료 시 `stopTimerInternal` 호출 추가 (접근성 서비스 비활성화)
    - 세션 ID null 설정 타이밍 개선 (StateFlow 업데이트 대기 시간 증가)
    - `onTimerFinish`에서 세션 ID null 처리 fallback 로직 추가
- [x] 이에 따라 접근성 오류가 발생된 것으로 추정
  - 관련 수정: [버그 수정](../working_history/2025-12-04_fix_session_success_failure_bug.md)
  - 원인: 정상 완료 시 `stopTimerInternal`을 호출하지 않아 접근성 서비스가 비활성화되지 않음
  - 수정: 정상 완료 시에도 `stopTimerInternal` 호출하여 접근성 서비스 비활성화 
- [x] 알림 설정 권한 설정하기 오류 : 설정을 완료하고 다시 앱으로 돌아와도 업데이트가 화면이 갱신이 안되어 있다.
  - 관련 수정: [버그 수정](../working_history/2025-12-04_fix_exact_alarm_permission_refresh.md)
  - 주요 수정 사항:
    - `TimeBasedAutoRunScreen`에 `DisposableEffect`와 `LifecycleEventObserver` 추가하여 화면 재진입 시 권한 상태 갱신
    - `TimeBasedAutoRunViewModel`에 `refreshExactAlarmPermission()` 메서드 추가
    - `ON_RESUME` 이벤트에서 `alarmManager.canScheduleExactAlarms()` 호출하여 StateFlow 업데이트
  - 원인: 화면이 포그라운드로 돌아올 때 권한 상태를 다시 확인하는 로직이 없음
  - 수정: `PermissionCheckScreen`과 동일한 패턴 적용하여 생명주기 이벤트 관찰
- [ ] 스케쥴을 새로 생성할때 스케쥴 템플릿을 선택후 수정하기 위해 선택된 템플릿을 삭제하려면 삭제가 안된다. 