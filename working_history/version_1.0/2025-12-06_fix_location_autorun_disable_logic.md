# 작업 기록: 위치 기반 자동 실행 비활성화 로직 수정

**작업 일시**: 2025-12-06
**작업 범위**: `ScheduleGroupManager`, `GeofenceTransitionsReceiver`

## 문제 상황
- 사용자가 스케줄 그룹을 수동으로 비활성화(OFF)했음에도 불구하고, 해당 위치에 진입하면 자동으로 활성화되는 문제 발생.
- 이는 `ScheduleGroupManager.deactivateGroup`이 시간 기반 알람만 취소하고, 위치 기반 Geofence는 해제하지 않았기 때문.
- 결과적으로 Geofence가 살아있어 `GeofenceTransitionsReceiver`가 트리거되고, 이는 다시 `activateGroup`을 호출하여 스케줄을 강제로 활성화함.

## 수정 내용

### 1. `ScheduleGroupManager`에 Geofence 관리 통합
- **파일**: `app/src/main/java/com/allday/detoxy/core/manager/ScheduleGroupManager.kt`
- **변경**:
    - `AutoRunGeofenceManager`를 주입받도록 수정.
    - `activateGroup` 메서드에 `updateGeofences` 파라미터 추가 (기본값 `true`).
        - `true`일 경우: 연결된 위치들의 Geofence를 등록 (`addGeofence`).
        - `false`일 경우: Geofence 등록 건너뜀 (이미 Geofence 진입으로 활성화된 경우 재등록 방지).
    - `deactivateGroup` 메서드 수정:
        - 연결된 위치들의 Geofence를 해제 (`removeGeofence`)하도록 로직 추가.

### 2. `GeofenceTransitionsReceiver` 무한 루프 방지
- **파일**: `app/src/main/java/com/allday/detoxy/core/manager/ScheduleGroupManager.kt` (receiver 로직은 manager의 activateGroupByLocation 내 호출부 수정으로 대응)
- **변경**:
    - `activateGroupByLocation` 내부에서 `activateGroup` 호출 시 `updateGeofences = false`로 설정.
    - 이를 통해 "Geofence 진입 -> Receiver -> activateGroup -> Geofence 재등록 -> 다시 진입 트리거"의 무한 루프를 방지.

## 검증 결과
- **빌드 검증**: `./gradlew compileDebugKotlin` 성공.
- **예상 동작**:
    1. 사용자가 스케줄 그룹 OFF → `deactivateGroup` 호출 -> Geofence 해제됨 -> 위치 진입해도 Receiver 동작 안 함 (비활성화 유지 성공).
    2. 사용자가 스케줄 그룹 ON → `activateGroup` 호출 -> Geofence 등록됨 -> 위치 진입 시 Receiver 동작 -> 스케줄 실행.
    3. 위치 진입으로 자동 실행 시 -> Receiver -> `activateGroup(false)` -> 스케줄 실행되지만 Geofence 재등록은 안 함 (루프 방지).

## 참고
- UI 상의 버튼 일관성 관련해서는, 리스트 화면(`ScheduleTabScreen`)의 아이콘은 상태 표시용이고, 상세/관리 화면(`ScheduleGroupScreen`)의 스위치는 제어용임이 코드상 확인됨. 기능적 오류(자동 활성화)를 우선 수정함.

