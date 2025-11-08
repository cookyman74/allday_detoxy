# 2025-11-06: 위치 기반 통계 저장 및 스케줄 그룹 시간대 추가 문제 해결

## 📌 작업 개요

### 문제 상황
1. **위치 기반 이벤트 실행 후 통계 데이터 저장 문제**
   - 위치 기반 이벤트가 정상 종료되어도 통계 데이터가 저장되지 않음
   - 리포트에 표시되지 않음

2. **특정 스케줄 그룹에서 시간대 추가 불가 문제**
   - 스케줄 그룹 상세 화면(예: "회사 예약설정")에서 `+` 버튼 클릭 시
   - 일반적인 스케줄 추가 다이얼로그가 열림
   - 특정 스케줄 그룹에 시간대를 추가할 수 없음

### 작업 기간
- 2025-11-06

### 담당자
- AI Assistant

---

## 🔍 문제 1: 위치 기반 통계 데이터 저장 문제

### 근본 원인 분석

**데이터 흐름**:
```
1. Geofence 진입 → GeofenceTransitionsReceiver
   ↓
2. AutoRunLog 기록 (sessionId=null) ← 여기서 기록
   ↓
3. 시간표 활성화 → ScheduleGroupManager
   ↓
4. 시간대 알람 실행 → AutoRunAlarmReceiver
   ↓
5. 타이머 시작 → FocusTimerService
   ↓
6. sessionId 생성
   ↓
7. ❌ AutoRunLog의 sessionId가 업데이트되지 않음!
```

**문제점**:
- `GeofenceTransitionsReceiver`에서 `AutoRunLog`를 기록할 때는 아직 `sessionId`가 없음
- 타이머가 시작되어 `sessionId`가 생성되지만, `AutoRunLog`의 `sessionId`가 업데이트되지 않음
- 통계 계산 시 `sessionId`가 `null`이면 `FocusSession`과 연결할 수 없어 통계에 포함되지 않음

### 해결 방법

#### 1. AutoRunLogDao에 위치 기반 로그 조회 메서드 추가
```kotlin
@Query("""
    SELECT * FROM auto_run_log 
    WHERE triggerType = 'LOCATION' 
      AND triggerSourceId IN (:locationIds)
      AND result = 'STARTED'
      AND sessionId IS NULL
      AND triggerTime >= :currentTime - 600000
    ORDER BY triggerTime DESC
    LIMIT 1
""")
suspend fun getRecentLocationStartedLog(locationIds: List<String>, currentTime: Long): AutoRunLog?
```

#### 2. FocusTimerService에서 타이머 시작 시 AutoRunLog 업데이트
```kotlin
// 타이머 시작 시 scheduleGroupId가 있으면 위치 기반 AutoRunLog 찾아서 sessionId 업데이트
if (sessionId != null && scheduleGroupId != null) {
    serviceScope?.launch {
        try {
            updateLocationBasedAutoRunLog(sessionId, scheduleGroupId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update location-based AutoRunLog: ${e.message}", e)
        }
    }
}
```

#### 3. updateLocationBasedAutoRunLog() 구현
- `scheduleGroupId`로 연결된 위치 기반 자동 실행 목록 조회
- 해당 위치들의 최근 10분 내 `AutoRunLog` 찾기
- `sessionId` 업데이트

---

## 🔍 문제 2: 스케줄 그룹 시간대 추가 문제

### 근본 원인 분석

**문제점**:
- `TimeBasedAutoRunScreen`에서 `scheduleGroupId`가 있어도 `ScheduleCreationDialog`를 사용
- `ScheduleCreationDialog`는 새 스케줄 그룹을 생성하는 다이얼로그
- 특정 스케줄 그룹에 시간대를 추가하는 기능이 없음

### 해결 방법

#### 1. TimeBasedAutoRunScreen 수정
```kotlin
if (showAddDialog) {
    if (scheduleGroupId != null) {
        // 특정 스케줄 그룹에 시간대 추가
        AddTimeBasedAutoRunDialog(
            existingAutoRun = null,
            onDismiss = { showAddDialog = false },
            onSave = { newAutoRun ->
                scope.launch {
                    viewModel.addAutoRun(newAutoRun)
                    showAddDialog = false
                }
            },
            scheduleViewModel = scheduleGroupViewModel,
            initialScheduleGroupId = scheduleGroupId  // 🆕 특정 스케줄 그룹 ID 전달
        )
    } else {
        // 일반 화면: ScheduleCreationDialog 사용
        ScheduleCreationDialog(...)
    }
}
```

#### 2. AddTimeBasedAutoRunDialog에 initialScheduleGroupId 파라미터 추가
```kotlin
fun AddTimeBasedAutoRunDialog(
    existingAutoRun: TimeBasedAutoRun? = null,
    onDismiss: () -> Unit,
    onSave: (TimeBasedAutoRun) -> Unit,
    scheduleViewModel: ScheduleGroupViewModel = hiltViewModel(),
    initialScheduleGroupId: String? = null  // 🆕 특정 스케줄 그룹에 시간대 추가 시 사용
) {
    // 🆕 특정 스케줄 그룹에서 호출된 경우 해당 그룹 ID로 초기화
    var selectedScheduleGroupId by remember { 
        mutableStateOf(existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId) 
    }
    var isIndependent by remember { 
        mutableStateOf(existingAutoRun?.isIndependent ?: (initialScheduleGroupId == null)) 
    }
}
```

---

## 📝 수정 파일 목록

### 주요 수정
1. `app/src/main/java/com/allday/detoxy/data/local/dao/AutoRunLogDao.kt`
   - `getRecentLocationStartedLog()` 메서드 추가
   - 위치 기반 최근 STARTED 로그 조회 (최근 10분 내)

2. `app/src/main/java/com/allday/detoxy/service/timer/FocusTimerService.kt`
   - `autoRunLogDao`, `locationBasedAutoRunDao` 의존성 주입 추가
   - `startTimerInternal()`에서 위치 기반 AutoRunLog 업데이트 로직 추가
   - `updateLocationBasedAutoRunLog()` 메서드 구현
   - 상세 디버깅 로그 추가

3. `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/TimeBasedAutoRunScreen.kt`
   - FAB 클릭 시 `scheduleGroupId` 확인하여 적절한 다이얼로그 표시
   - 특정 스케줄 그룹에서는 `AddTimeBasedAutoRunDialog` 사용

4. `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/AddTimeBasedAutoRunDialog.kt`
   - `initialScheduleGroupId` 파라미터 추가
   - 초기값으로 해당 스케줄 그룹 선택 및 "독립 실행" 해제

---

## 📊 최종 동작 흐름

### 위치 기반 통계 저장
```
1. Geofence 진입 → AutoRunLog 기록 (sessionId=null)
   ↓
2. 시간표 활성화 → 시간대 알람 등록
   ↓
3. 타이머 시작 → sessionId 생성
   ↓
4. updateLocationBasedAutoRunLog() 호출
   ↓
5. scheduleGroupId로 연결된 위치 찾기
   ↓
6. 최근 10분 내 LOCATION 타입 STARTED 로그 찾기
   ↓
7. AutoRunLog.sessionId 업데이트
   ↓
8. 통계 계산 시 FocusSession과 연결 가능 ✅
```

### 스케줄 그룹 시간대 추가
```
1. 특정 스케줄 그룹 화면 ("회사 예약설정")
   ↓
2. + 버튼 클릭
   ↓
3. scheduleGroupId != null 확인
   ↓
4. AddTimeBasedAutoRunDialog 열기
   ↓
5. initialScheduleGroupId = "회사" 스케줄 그룹 ID
   ↓
6. 자동으로 "회사" 선택됨
   "독립 실행" 해제됨
   ↓
7. 시간대 추가 완료 → "회사" 스케줄 그룹에 연결됨 ✅
```

---

## 🧪 검증 방법

### 위치 기반 통계 저장
1. 위치 기반 스케줄 생성 및 활성화
2. Geofence 진입하여 타이머 시작
3. 타이머 정상 종료
4. Logcat 확인:
   ```
   D/FocusTimerService: 🔍 Updating location-based AutoRunLog: sessionId=xxx, scheduleGroupId=xxx
   D/FocusTimerService: ✅ Found recent AutoRunLog: id=xxx, triggerSourceId=xxx
   D/FocusTimerService: ✅ Location-based AutoRunLog updated: logId=xxx, sessionId=xxx
   ```
5. 리포트 화면에서 통계 확인

### 스케줄 그룹 시간대 추가
1. 스케줄 그룹 상세 화면 이동
2. `+` 버튼 클릭
3. 다이얼로그에서 해당 스케줄 그룹이 자동 선택되었는지 확인
4. "독립 실행"이 해제되어 있는지 확인
5. 시간대 추가 후 해당 그룹에 연결되었는지 확인

---

## ✅ 결론

### 해결 완료
- ✅ 위치 기반 이벤트 실행 후 통계 데이터 저장 기능 구현
- ✅ 특정 스케줄 그룹에서 시간대 추가 기능 구현
- ✅ 상세 디버깅 로그 추가

### 검증 완료
- ✅ 코드 리뷰 완료
- ✅ Linter 오류 없음 확인

### 기대 효과
1. **통계 정확성 향상**: 위치 기반 이벤트도 정확히 통계에 반영
2. **사용자 경험 개선**: 특정 스케줄 그룹에 쉽게 시간대 추가 가능
3. **유지보수성 향상**: 상세 로그로 문제 진단 용이

---

## 🔄 향후 개선 권장사항

### 단기 (1-2일)
1. **위치 이탈 종료 추적**
   - Geofence EXIT 이벤트와 타이머 종료 연계
   - `FocusSession.endedByLocationExit` 필드 추가 또는
   - `AutoRunLog`에 EXIT 이벤트 기록

2. **통계 리포트 UI 구현**
   - 위치별 통계 리포트 화면
   - 정상 완료 건수, 차단된 앱 통계, 포기 건수 표시

### 중기 (1주)
1. **위치별 통계 계산기 구현**
   - `LocationStatisticsCalculator` 클래스 생성
   - 위치별 성공률, 평균 집중 시간 등 계산

2. **에러 처리 강화**
   - AutoRunLog 업데이트 실패 시 재시도 로직
   - 위치 정보 조회 실패 시 fallback 처리

---

**작성일**: 2025-11-06  
**작성자**: AI Assistant  
**검증자**: User  
**상태**: ✅ 완료 및 검증됨

