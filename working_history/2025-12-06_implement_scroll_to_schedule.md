# 작업 기록: 스케줄 상세 이동 시 스크롤 위치 자동 이동 기능 구현

**작업 일시**: 2025-12-06
**작업 범위**: `ScheduleGroupScreen`, `MainActivity`

## 문제 상황
- 사용자가 스케줄 목록 탭에서 특정 스케줄을 클릭하여 관리 화면(`ScheduleGroupScreen`)으로 이동할 때, 항상 화면 최상단(첫 번째 스케줄)부터 표시됨.
- 스케줄이 많을 경우 사용자가 자신이 클릭한 스케줄을 다시 찾아야 하는 불편함과 혼란을 초래함.
- **추가 수정**: `LazyColumn` 상단에 '안내 카드'가 있어, 단순히 스케줄 인덱스로 스크롤하면 한 칸씩 밀리는 문제 발생 (예: '집 평일' 클릭 시 '회사 평일' 위치로 스크롤됨).

## 수정 내용

### 1. `ScheduleGroupScreen`에 초기 스크롤 기능 추가
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/ScheduleGroupScreen.kt`
- **변경**:
    - `initialScrollToGroupId: String?` 파라미터 추가.
    - `rememberLazyListState()`를 사용하여 `LazyColumn`의 스크롤 상태(`listState`) 관리.
    - `LaunchedEffect`를 통해 `scheduleGroups` 데이터가 로드된 후 `initialScrollToGroupId`와 일치하는 인덱스를 찾아 `listState.animateScrollToItem(index)` 호출.
    - **보정 적용**: `LazyColumn`의 0번째 아이템이 "안내 카드"이므로, 실제 스케줄 인덱스에 **+1**을 더하여 정확한 위치로 스크롤되도록 수정.

### 2. `MainActivity`에서 클릭한 그룹 ID 전달
- **파일**: `app/src/main/java/com/allday/detoxy/MainActivity.kt`
- **변경**:
    - `AutoRunScreenType.SCHEDULE_GROUP` 케이스에서 `ScheduleGroupScreen` 호출 시 `initialScrollToGroupId` 파라미터에 `selectedScheduleGroupId`를 전달하도록 수정.

## 검증 결과
- **빌드 검증**: `./gradlew compileDebugKotlin` 성공.
- **예상 동작**:
    1. 사용자가 스케줄 탭에서 'A 스케줄' 클릭.
    2. `selectedScheduleGroupId`에 'A 스케줄' ID 저장.
    3. `ScheduleGroupScreen`으로 화면 전환.
    4. `ScheduleGroupScreen` 진입 시 'A 스케줄'이 화면에 보이도록 리스트가 자동으로 스크롤됨 (오차 없이 정확한 위치).

## 참고
- `LazyColumn`에 헤더나 고정 아이템이 있는 경우, `animateScrollToItem` 호출 시 인덱스 보정이 필수적임.
