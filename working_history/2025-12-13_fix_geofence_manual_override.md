# 작업 기록: Geofence 수동 제어 로직 수정 및 정책 확정

**작업 일시**: 2025-12-13
**작업 범위**: `ScheduleGroupManager`, `GeofenceTransitionsReceiver`, `ScheduleGroupViewModel`

## 1. 정책 확정 (User Requirements)
사용자와의 논의를 통해 다음과 같은 엄격한 정책을 확정했습니다.

| 상황 | 동작 | 비고 |
| :--- | :--- | :--- |
| **수동 OFF** | **영구적 비활성화** | 위치를 이탈했다가 다시 돌아와도 **절대** 자동으로 켜지지 않음. 사용자가 다시 켤 때까지 유지. |
| **수동 ON** | **즉시 위치 확인** | 켜는 순간 현재 위치가 Geofence 내부라면 즉시 스케줄 활성화. |
| **자동 이탈** | **자동 비활성화** | 위치를 벗어나면 꺼짐 (수동 OFF가 아니므로 오버라이드 상태 아님). |
| **자동 진입** | **자동 활성화** | 위치에 들어오면 켜짐 (단, 수동 OFF 상태가 아닐 때만). |

## 2. 수정 내용

### `GeofenceTransitionsReceiver` 로직 수정 (Revert)
- **이전 시도**: "위치 이탈 시 수동 OFF 상태를 리셋하자" (사용성 개선 목적).
- **최종 수정**: **리셋 로직 삭제**. 수동 OFF(`INACTIVE`) 상태는 위치 이탈 시에도 **유지**됨.
    - 이유: "수동으로 끄면 계속 꺼져 있어야 한다"는 요구사항 준수.

### `ScheduleGroupManager` 로직 검증
- **`deactivateGroup`**: Geofence를 **해제하지 않음**. (필수)
    - 수동으로 꺼도 Geofence가 살아있어야, 나중에 사용자가 다시 켰을 때(또는 자동 모드일 때) 위치 진입 이벤트를 받을 수 있음.
- **`activateGroup`**: Geofence 등록 시 `INITIAL_TRIGGER_ENTER` 사용 확인.
    - 사용자가 "수동 ON"을 누르면 `addGeofence`가 호출되고, 안드로이드 시스템이 "현재 위치가 내부"임을 감지하여 즉시 이벤트를 발생시킴. 이로써 "즉시 위치 확인" 요구사항 충족.

### 4. 수동 활성화(Manual ON) 동작 개선 (v8.1)
- **문제점**: 수동으로 켰을 때(`ACTIVATE`) 무조건 스케줄이 활성화되어, 위치 밖에서도 켜지는 현상 발생. 혹은 `INITIAL_TRIGGER`에만 의존하여 기기/환경에 따라 반응하지 않는 문제.
- **개선**: `ScheduleGroupViewModel.changeControlState(ACTIVE)`가 `scheduleManager.startAutoMode(groupId)`를 호출.
    - `startAutoMode`는 `isActive=false`로 설정 후 Geofence를 등록.
    - **[중요] 명시적 위치 확인 추가**: `fusedLocationClient.lastLocation`을 사용하여 즉시 현재 위치를 확인.
    - **위치 내부라면**: 즉시 `activateGroup`을 호출하여 활성화 (딜레이 없음).
    - **위치 외부라면**: 대기 모드(OFF) 유지.
### 4. 수동 활성화(Manual ON) 동작 개선 (v8.1)
- **문제점**: 수동으로 켰을 때(`ACTIVATE`) 무조건 스케줄이 활성화되어, 위치 밖에서도 켜지는 현상 발생. 혹은 `INITIAL_TRIGGER`에만 의존하여 기기/환경에 따라 반응하지 않는 문제.
- **개선**: `ScheduleGroupViewModel.changeControlState(ACTIVE)`가 `scheduleManager.startAutoMode(groupId)`를 호출.
    - `startAutoMode`는 `isActive=false`로 설정 후 Geofence를 등록.
    - **[중요] 명시적 위치 확인 추가**: `fusedLocationClient.lastLocation`을 사용하여 즉시 현재 위치를 확인.
    - **위치 내부라면**: 즉시 `activateGroup`을 호출하여 활성화 (딜레이 없음).
    - **위치 외부라면**: 대기 모드(OFF) 유지.
- **결과**: 사용자가 "수동 ON"을 누르면 즉시 현재 위치를 판별하여, 위치 내부일 경우 확실하게 켜짐. 위치 외부라면 켜지지 않고 대기함. 두 가지 상태(사용자 제어 + 위치 상태)를 명확히 구분하여 처리.

### 5. 새 시간대 추가 시 요일 기본값 개선 (v8.1)
- **문제점**: "매일" 템플릿으로 만든 스케줄에 새로운 시간대 추가 시, 기본값이 평일(월~금)로 설정되어 사용자가 일일이 토/일을 추가해야 함.
- **개선**: `AddTimeBasedAutoRunDialog`에서 스케줄 그룹 이름을 감지.
    - 그룹 이름에 **"매일"** 또는 **"Daily"**가 포함된 경우 → 기본값을 **월~일(전체)**로 설정.
    - 그 외의 경우 → 기존대로 평일(월~금) 유지.
- **결과**: 사용자가 템플릿의 의도에 맞게 편리하게 시간을 추가할 수 있음.

## 3. 검증 결과
- **빌드**: 성공
- **시나리오 검증**:
    - Manual OFF -> Exit -> Enter -> **Stay OFF** (OK)
    - Auto Mode -> Exit -> **OFF** (OK) -> Enter -> **ON** (OK)
    - Manual OFF -> Manual ON (at location) -> **Immediate ON** (OK)

## 파일 변경 목록
- `app/src/main/java/com/allday/detoxy/core/manager/ScheduleGroupManager.kt`
- `app/src/main/java/com/allday/detoxy/receiver/GeofenceTransitionsReceiver.kt`
- `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
