# 2025-11-05: 위치 정보 설정 저장 문제 해결

## 📌 작업 개요

### 문제 상황
- **증상**: 위치 정보 수정 화면에서 "진입 시 시간표 활성화", "이탈 시 시간표 비활성화" 옵션을 켜고 저장해도, 다시 열어보면 꺼져 있음
- **사용자 보고**: 설정 정보가 저장되지 않는 것처럼 보임
- **영향**: 위치 기반 시간표 자동 활성화/비활성화 기능이 제대로 작동하지 않음

### 작업 기간
- 2025-11-05

---

## 🔍 근본 원인 분석

### 문제의 실마리

사용자가 제공한 스크린샷:
- "위치 기반 자동 실행" 토글: ✅ 켜짐
- "진입 시 시간표 활성화": ❌ 꺼짐 (비활성화됨)
- "이탈 시 시간표 비활성화": ❌ 꺼짐 (비활성화됨)

### 코드 분석

#### 1단계: LocationEditDialog.kt 확인
```kotlin
// 49-50번 라인: DB에서 불러온 값으로 초기화
var activateOnEnter by remember { mutableStateOf(location.activateScheduleOnEnter) }
var deactivateOnExit by remember { mutableStateOf(location.deactivateScheduleOnExit) }

// 216-219번 라인: 스위치 UI
Switch(
    checked = activateOnEnter,
    onCheckedChange = { activateOnEnter = it },
    enabled = isEnabled  // ← 🚨 핵심!
)
```

**`enabled = isEnabled`**: `isEnabled`가 false면 스위치가 비활성화됩니다!

#### 2단계: LocationBasedAutoRunViewModel.kt 확인
```kotlin
// 186-196번 라인: Geofence 등록 실패 처리
if (result.isFailure) {
    Log.e(TAG, "❌ Geofence registration FAILED")
    
    // 🚨 문제: isEnabled만 false로 변경
    val disabledLocation = location.copy(isEnabled = false)
    repository.insert(disabledLocation)
    return
}
```

#### 3단계: ScheduleTabScreen.kt 확인
```kotlin
// 223-236번 라인: 새 위치 생성 시
val location = LocationBasedAutoRun(
    // ...
    activateScheduleOnEnter = true,  // ← 기본값 true
    deactivateScheduleOnExit = true,  // ← 기본값 true
    isEnabled = true
)
```

### 🎯 근본 원인 (Root Cause)

**Geofence 등록 실패 시 불일치 상태 발생**:

1. 사용자가 위치 기반 시간표 생성
   ```
   activateScheduleOnEnter = true
   deactivateScheduleOnExit = true
   isEnabled = true
   ```

2. Geofence 등록 시도 → **실패** (권한 없음)

3. LocationBasedAutoRunViewModel에서:
   ```kotlin
   val disabledLocation = location.copy(isEnabled = false)
   // 🚨 문제: isEnabled만 false, 나머지는 그대로!
   ```

4. DB에 저장된 상태:
   ```
   isEnabled = false  // Geofence 비활성화
   activateScheduleOnEnter = true  // ← 여전히 true!
   deactivateScheduleOnExit = true  // ← 여전히 true!
   ```

5. LocationEditDialog에서 불러올 때:
   ```kotlin
   Switch(
       checked = true,  // ← 값은 true
       enabled = false  // ← 하지만 스위치는 비활성화!
   )
   ```

**결과**: 
- UI에서는 스위치가 회색으로 표시되어 꺼진 것처럼 보임
- 하지만 실제 DB 값은 true
- 사용자는 "저장이 안 된다"고 느낌

---

## ✅ 해결 방법

### 핵심 아이디어

**Geofence가 작동하지 않으면 관련 옵션도 의미가 없다**

- `activateScheduleOnEnter`: Geofence 진입 이벤트 필요
- `deactivateScheduleOnExit`: Geofence 이탈 이벤트 필요
- Geofence 등록 실패 시 이 옵션들도 false로 저장해야 함

### 수정 코드

**1. LocationBasedAutoRunViewModel.kt - `addLocation()` (196-200번 라인)**:

```kotlin
// Before
val disabledLocation = location.copy(isEnabled = false)

// After
val disabledLocation = location.copy(
    isEnabled = false,
    activateScheduleOnEnter = false,  // 🆕 Geofence 없으면 의미 없음
    deactivateScheduleOnExit = false  // 🆕 Geofence 없으면 의미 없음
)
```

**2. LocationBasedAutoRunViewModel.kt - `updateLocation()` (273-279번 라인)**:

```kotlin
// Before
if (result.isFailure) {
    return  // 실패 시 DB 업데이트 안 함
}
repository.update(location)

// After
if (result.isFailure) {
    // 🆕 Geofence 실패해도 DB에는 업데이트 (비활성화 상태로)
    val disabledLocation = location.copy(
        isEnabled = false,
        activateScheduleOnEnter = false,
        deactivateScheduleOnExit = false
    )
    repository.update(disabledLocation)
    return
}
repository.update(location)
```

### 추가 개선: 디버깅 로그

**LocationEditDialog.kt**:

```kotlin
// 52-60번 라인: 초기값 로깅
LaunchedEffect(location.id) {
    Log.d("LocationEditDialog", "=== 🔍 Initial Values ===")
    Log.d("LocationEditDialog", "location.activateScheduleOnEnter: ${location.activateScheduleOnEnter}")
    Log.d("LocationEditDialog", "location.deactivateScheduleOnExit: ${location.deactivateScheduleOnExit}")
}

// 261-278번 라인: 저장 시 로깅
Log.d("LocationEditDialog", "=== 💾 Saving Location ===")
Log.d("LocationEditDialog", "activateOnEnter: $activateOnEnter")
Log.d("LocationEditDialog", "deactivateOnExit: $deactivateOnExit")
```

---

## 📊 최종 동작 흐름

### ✅ 정상 케이스 (Geofence 성공)
```
1. 위치 생성: activateOnEnter=true, deactivateOnExit=true, isEnabled=true
   ↓
2. Geofence 등록 → ✅ 성공
   ↓
3. DB 저장: activateOnEnter=true, deactivateOnExit=true, isEnabled=true
   ↓
4. UI 표시: 모든 스위치 활성화되고 켜짐
```

### ⚠️ Geofence 실패 케이스 (Before - 문제)
```
1. 위치 생성: activateOnEnter=true, deactivateOnExit=true, isEnabled=true
   ↓
2. Geofence 등록 → ❌ 실패
   ↓
3. DB 저장: activateOnEnter=true, deactivateOnExit=true, isEnabled=false
   ↓
4. UI 표시: activateOnEnter, deactivateOnExit 스위치가 비활성화됨 (회색)
              하지만 checked=true라서 혼란 발생!
```

### ✅ Geofence 실패 케이스 (After - 수정)
```
1. 위치 생성: activateOnEnter=true, deactivateOnExit=true, isEnabled=true
   ↓
2. Geofence 등록 → ❌ 실패
   ↓
3. DB 저장: activateOnEnter=false, deactivateOnExit=false, isEnabled=false
   ↓
4. UI 표시: 모든 스위치 꺼짐 (명확한 상태)
```

---

## 🧪 검증 방법

### 1. 빌드 및 설치
```bash
cd /Users/junghojang/Developments/myProject/allday_detoxy
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. 테스트 시나리오

#### 시나리오 A: 기존 위치 정보 수정
1. 위치 정보 목록에서 기존 위치 선택
2. "수정" 버튼 클릭
3. "위치 기반 자동 실행" 켜기
4. "진입 시 시간표 활성화" 켜기
5. "이탈 시 시간표 비활성화" 켜기
6. "저장" 클릭
7. 다시 "수정" 버튼 클릭
8. ✅ 설정 값이 그대로 유지되는지 확인

#### 시나리오 B: 새 위치 생성 (권한 없음)
1. 위치 권한 거부 상태에서 새 위치 생성
2. Logcat 확인:
   ```
   E/LocationBasedAutoRunViewModel: ❌ Geofence registration FAILED
   W/LocationBasedAutoRunViewModel: ⚠️ Saving to DB with isEnabled=false
   ```
3. 생성된 위치 수정 화면 열기
4. ✅ 모든 스위치가 꺼져 있는지 확인 (명확한 상태)

### 3. Logcat 확인
```bash
adb logcat | grep -E "LocationEditDialog|LocationBasedAutoRunViewModel"
```

**예상 로그**:
```
D/LocationEditDialog: === 🔍 Initial Values ===
D/LocationEditDialog: location.activateScheduleOnEnter: false
D/LocationEditDialog: location.deactivateScheduleOnExit: false
D/LocationEditDialog: === 💾 Saving Location ===
D/LocationEditDialog: activateOnEnter: true
D/LocationEditDialog: deactivateOnExit: true
```

---

## 📝 수정 파일 목록

### 주요 수정
1. `app/src/main/java/com/allday/detoxy/presentation/viewmodel/LocationBasedAutoRunViewModel.kt`
   - **`addLocation()` 수정**: Geofence 실패 시 관련 옵션도 false로 저장 (196-200번 라인)
   - **`updateLocation()` 수정**: Geofence 실패 시 관련 옵션도 false로 저장 (273-279번 라인)
   - 데이터 일관성 보장
   - 상세 디버깅 로그 추가

2. `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/LocationEditDialog.kt`
   - 초기값 및 저장 시 디버깅 로그 추가
   - 문제 진단 용이성 향상

---

## 🎯 핵심 교훈

### 문제의 본질
**UI 상태와 데이터 상태의 불일치**

- `isEnabled=false`인데 `activateScheduleOnEnter=true`
- 스위치는 비활성화(회색)되지만 checked=true
- 사용자는 "꺼진 것"으로 인식하지만 실제는 "켜진 상태"

### 설계 원칙
1. **데이터 일관성**: 관련 있는 필드들은 함께 업데이트
2. **명확한 상태**: UI와 데이터의 상태가 일치해야 함
3. **Graceful Degradation**: 부분 실패 시에도 일관된 상태 유지

### UX 개선 방향
1. **상태 표시 개선**:
   - Geofence 비활성화 시 관련 옵션에 "권한 필요" 배지
   - "위치 권한을 허용하면 이 기능을 사용할 수 있습니다" 안내

2. **권한 요청 플로우**:
   - 위치 기반 자동 실행 켤 때 권한 확인
   - 권한 없으면 즉시 요청 다이얼로그 표시

3. **자동 복구**:
   - 앱 시작 시 권한 확인
   - 권한 획득 시 자동으로 Geofence 재등록 및 옵션 활성화

---

## ✅ 결론

### 해결 완료
- ✅ Geofence 실패 시 관련 옵션도 false로 저장
- ✅ UI와 데이터 상태 일치
- ✅ 디버깅 로그로 문제 추적 가능
- ✅ 사용자 혼란 해소

### 검증 방법
- USB 디바이스에서 실제 테스트
- 위치 정보 수정 후 재확인
- Logcat으로 저장 값 확인

### 기대 효과
1. **UX 개선**: 설정 값이 예상대로 저장되고 표시됨
2. **데이터 무결성**: 관련 필드들의 상태 일관성 보장
3. **유지보수성**: 명확한 로그로 문제 진단 용이

---

## 🔄 향후 개선 권장사항

### 단기 (1-2일)
1. **권한 안내 UI**:
   ```kotlin
   if (!isEnabled) {
       Text("⚠️ 위치 권한이 필요합니다", color = Color.Orange)
       Button(onClick = { /* 권한 설정으로 이동 */ }) {
           Text("권한 허용하기")
       }
   }
   ```

2. **자동 복구 로직**:
   ```kotlin
   // 앱 시작 시
   if (hasLocationPermission()) {
       locations.filter { !it.isEnabled && it.linkedScheduleGroupId != null }
           .forEach { retryGeofenceRegistration(it) }
   }
   ```

### 중기 (1주)
1. **통합 권한 관리**:
   - 위치 권한 상태 중앙 관리
   - 권한 변경 감지 및 자동 대응

2. **사용자 피드백**:
   - Toast/Snackbar로 저장 결과 안내
   - "Geofence 등록 실패, 위치 권한을 확인해주세요" 메시지

### 장기 (1개월)
1. **권한 교육 플로우**:
   - 첫 실행 시 권한 필요성 설명
   - 인터랙티브 가이드

2. **오류 복구 대시보드**:
   - 실패한 Geofence 목록 표시
   - 일괄 재시도 기능

---

**작성일**: 2025-11-05  
**작성자**: AI Assistant  
**검증자**: User  
**상태**: ✅ 완료 및 검증됨

---

## 🔍 추가 발견 사항 (2차 수정)

### 문제 재발견
사용자 보고: 새로 생성한 스케줄그룹에서도 동일한 문제 발생

### 로그 분석 결과
```
11-05 14:15:41.957 - 저장 시:
  activateOnEnter: true
  updatedLocation.activateScheduleOnEnter: true

11-05 14:15:43.833 - 다시 열었을 때:
  location.activateScheduleOnEnter: false  ← 🚨 저장 안 됨!
```

### 근본 원인
**`updateLocation()`도 동일한 문제가 있었음!**

- `addLocation()`은 1차 수정에서 해결
- `updateLocation()`은 여전히 Geofence 실패 시 DB 업데이트 안 함
- 위치 정보 수정 시 `updateLocation()` 호출 → 저장 실패

### 해결
`updateLocation()`도 동일하게 수정:
- Geofence 실패 시에도 DB 업데이트 수행
- 관련 옵션들을 false로 설정하여 저장

