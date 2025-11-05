# 2025-11-05: 스케줄 생성 위치 정보 저장 디버깅 로그 추가

## 📌 작업 개요

### 목표
- 위치 정보가 저장되지 않는 문제의 원인을 찾기 위한 상세 디버깅 로그 추가
- `ScheduleCreationDialog`와 `ScheduleTabScreen`의 전체 흐름 추적

### 배경
사용자가 스케줄 탭에서 시간표를 생성할 때 위치 정보가 저장되지 않는 문제가 지속됨.
이전 수정으로 코드 로직은 수정했으나, 실제 동작 확인이 필요함.

---

## 🔍 추가된 디버깅 로그

### 1. ScheduleCreationDialog.kt

#### 위치 선택 단계
```kotlin
ScheduleCreationStep.LOCATION_CHOICE -> {
    Log.d("ScheduleCreationDialog", "📍 Location choice: hasLocation=$hasLocation")
    if (hasLocation) {
        Log.d("ScheduleCreationDialog", "→ Moving to LOCATION_SEARCH step")
    } else {
        Log.d("ScheduleCreationDialog", "→ Moving to SCHEDULE_SETUP step (no location)")
    }
}
```

#### 위치 검색 단계
```kotlin
ScheduleCreationStep.LOCATION_SEARCH -> {
    Log.d("ScheduleCreationDialog", "📍 Location search step: selectedLocation=$selectedLocation")
    if (selectedLocation != null) {
        Log.d("ScheduleCreationDialog", "→ Moving to SCHEDULE_SETUP step with location: ${selectedLocation!!.name}")
    }
}
```

#### 위치 검색 다이얼로그 콜백
```kotlin
LocationSearchDialog(
    onDismiss = { 
        Log.d("ScheduleCreationDialog", "❌ Location search dismissed without selection")
    },
    onLocationSelected = { location ->
        Log.d("ScheduleCreationDialog", "📍 Location selected: ${location.name} (${location.address})")
    }
)
```

#### 시간표 생성 최종 단계
```kotlin
ScheduleCreationStep.SCHEDULE_SETUP -> {
    Log.d("ScheduleCreationDialog", "=== 🔍 Schedule Creation Debug ===")
    Log.d("ScheduleCreationDialog", "hasLocation: $hasLocation")
    Log.d("ScheduleCreationDialog", "selectedLocation: $selectedLocation")
    Log.d("ScheduleCreationDialog", "name: $name")
    Log.d("ScheduleCreationDialog", "mode: $mode")
    Log.d("ScheduleCreationDialog", "timeSlots: ${timeSlots.size}")
    
    val locationInfo = if (hasLocation && selectedLocation != null) {
        Log.d("ScheduleCreationDialog", "✅ LocationInfo created: ${it.name} (${it.address})")
        LocationInfo(...)
    } else {
        Log.d("ScheduleCreationDialog", "❌ LocationInfo is NULL (hasLocation=$hasLocation, selectedLocation=$selectedLocation)")
        null
    }
    
    Log.d("ScheduleCreationDialog", "📤 Calling onConfirm with locationInfo: $locationInfo")
}
```

### 2. ScheduleTabScreen.kt

#### 위치 기반 스케줄 생성
```kotlin
Log.d("ScheduleTabScreen", "🔵 Creating location-based schedule: $scheduleName")
Log.d("ScheduleTabScreen", "📍 Location: ${locationInfo.name} (${locationInfo.address})")
Log.d("ScheduleTabScreen", "✅ ScheduleGroup created: $scheduleGroupId")
Log.d("ScheduleTabScreen", "💾 Saving location: ${location.label} → $scheduleGroupId")
Log.d("ScheduleTabScreen", "✅ Location saved: ${location.label} (${location.address}) → ScheduleGroup: $scheduleGroupId")
```

---

## 📋 테스트 및 로그 확인 방법

### 1. 앱 재빌드 및 설치
```bash
cd /Users/junghojang/Developments/myProject/allday_detoxy
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Logcat 실시간 모니터링
터미널에서 다음 명령 실행:
```bash
adb logcat | grep -E "ScheduleCreationDialog|ScheduleTabScreen"
```

또는 Android Studio의 Logcat 창에서:
- 필터: `ScheduleCreationDialog|ScheduleTabScreen`

### 3. 테스트 시나리오

#### 시나리오 A: 위치 기반 시간표 생성
1. 스케줄 탭 이동
2. + FAB 버튼 클릭
3. "특정 위치에서만" 선택 → **"다음"** 버튼 클릭
4. 위치 검색 (예: "회사")
5. 위치 선택 및 반경 설정 → **"다음"** 버튼 클릭
6. 시간표 이름 입력 (예: "회사 스케줄")
7. "시간대 직접 추가" 선택
8. 시간대 추가 (예: 14:00, 240분)
9. **"만들기"** 버튼 클릭

#### 시나리오 B: 위치 없는 시간표 생성
1. 스케줄 탭 이동
2. + FAB 버튼 클릭
3. "어디서나 적용" 선택 → **"다음"** 버튼 클릭
4. 시간표 이름 입력 및 시간대 추가
5. **"만들기"** 버튼 클릭

---

## 🔍 예상 로그 패턴

### 정상 동작 시 (위치 기반 시간표)
```
D/ScheduleCreationDialog: 📍 Location choice: hasLocation=true
D/ScheduleCreationDialog: → Moving to LOCATION_SEARCH step
D/ScheduleCreationDialog: 📍 Location selected: 회사 (서울시 강남구...)
D/ScheduleCreationDialog: 📍 Location search step: selectedLocation=LocationInfo(name=회사, ...)
D/ScheduleCreationDialog: → Moving to SCHEDULE_SETUP step with location: 회사
D/ScheduleCreationDialog: === 🔍 Schedule Creation Debug ===
D/ScheduleCreationDialog: hasLocation: true
D/ScheduleCreationDialog: selectedLocation: LocationInfo(name=회사, ...)
D/ScheduleCreationDialog: name: 회사 스케줄
D/ScheduleCreationDialog: mode: CUSTOM
D/ScheduleCreationDialog: timeSlots: 1
D/ScheduleCreationDialog: ✅ LocationInfo created: 회사 (서울시 강남구...)
D/ScheduleCreationDialog: 📤 Calling onConfirm with locationInfo: LocationInfo(name=회사, ...)
D/ScheduleTabScreen: 🔵 Creating location-based schedule: 회사 스케줄
D/ScheduleTabScreen: 📍 Location: 회사 (서울시 강남구...)
D/ScheduleTabScreen: ✅ ScheduleGroup created: [UUID]
D/ScheduleTabScreen: 💾 Saving location: 회사 → [UUID]
D/ScheduleTabScreen: ✅ Location saved: 회사 (서울시 강남구...) → ScheduleGroup: [UUID]
```

### 비정상 동작 시 (위치 정보 누락)
```
D/ScheduleCreationDialog: 📍 Location choice: hasLocation=true
D/ScheduleCreationDialog: → Moving to LOCATION_SEARCH step
D/ScheduleCreationDialog: ❌ Location search dismissed without selection  ← 🚨 문제!
D/ScheduleCreationDialog: 📍 Location search step: selectedLocation=null  ← 🚨 문제!
```

또는

```
D/ScheduleCreationDialog: === 🔍 Schedule Creation Debug ===
D/ScheduleCreationDialog: hasLocation: true
D/ScheduleCreationDialog: selectedLocation: null  ← 🚨 문제!
D/ScheduleCreationDialog: ❌ LocationInfo is NULL (hasLocation=true, selectedLocation=null)
```

---

## 🎯 로그 분석 체크리스트

### 1단계: 위치 선택 여부 확인
- [ ] `hasLocation=true`가 출력되는가?
  - ❌ → 사용자가 "어디서나 적용"을 선택한 것 (정상)
  - ✅ → 다음 단계 확인

### 2단계: 위치 검색 확인
- [ ] `"📍 Location selected:"` 로그가 나타나는가?
  - ❌ → 위치 검색 다이얼로그에서 선택하지 않음
  - ✅ → 다음 단계 확인

### 3단계: 위치 정보 유지 확인
- [ ] `selectedLocation: LocationInfo(...)`가 출력되는가?
  - ❌ → **상태 유지 문제** (Compose 상태 재설정 가능성)
  - ✅ → 다음 단계 확인

### 4단계: LocationInfo 생성 확인
- [ ] `"✅ LocationInfo created:"` 로그가 나타나는가?
  - ❌ → `hasLocation && selectedLocation != null` 조건 실패
  - ✅ → 다음 단계 확인

### 5단계: 저장 프로세스 확인
- [ ] `"🔵 Creating location-based schedule:"` 로그가 나타나는가?
  - ❌ → `locationInfo`가 null로 전달됨
  - ✅ → 다음 단계 확인

### 6단계: DB 저장 확인
- [ ] `"✅ Location saved:"` 로그가 나타나는가?
  - ❌ → `addLocation()` 실패 (DB 오류)
  - ✅ → 저장 성공, UI 갱신 문제

---

## 🔧 수정된 파일

- `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleCreationDialog.kt`
- `app/src/main/java/com/allday/detoxy/presentation/ui/schedule/ScheduleTabScreen.kt`

---

## 📝 다음 단계

1. **로그 수집**
   - 앱 재빌드 및 설치
   - Logcat 모니터링하며 테스트 수행
   - 로그 전체 복사

2. **문제 분석**
   - 위 체크리스트에 따라 어느 단계에서 실패하는지 확인
   - 로그 패턴 비교

3. **원인 파악 및 수정**
   - 상태 유지 문제 → Compose 상태 관리 수정
   - DB 저장 실패 → Repository/ViewModel 확인
   - UI 갱신 문제 → StateFlow 갱신 로직 확인

---

## 💡 예상 문제 시나리오

### 시나리오 1: Compose 상태 재설정
**증상**: `selectedLocation`이 검색 시에는 있었으나 나중에 null
**원인**: 다이얼로그 재composition 시 상태 초기화
**해결**: `rememberSaveable` 사용 또는 상태 끌어올리기

### 시나리오 2: 위치 검색 다이얼로그 버그
**증상**: 위치 선택 후 `onLocationSelected` 콜백이 호출되지 않음
**원인**: `LocationSearchDialog` 내부 버그
**해결**: `LocationSearchDialog.kt` 확인 및 수정

### 시나리오 3: DB 저장 실패
**증상**: `addLocation()` 호출 후 예외 발생
**원인**: DB 제약 조건 위반 또는 트랜잭션 실패
**해결**: `LocationBasedAutoRunRepository` 확인

---

## 🎯 요약

이제 **완전한 로그 추적**이 가능합니다:
1. 위치 선택 단계별 상태
2. 위치 검색 성공/실패
3. LocationInfo 생성 여부
4. ScheduleGroup 생성 및 연결
5. DB 저장 결과

다음 테스트 시 **Logcat 로그를 전체 복사**하여 제공해주시면 정확한 원인을 찾을 수 있습니다.

