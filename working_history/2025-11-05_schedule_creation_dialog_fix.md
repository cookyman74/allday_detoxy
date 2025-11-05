# 2025-11-05: 스케줄 탭에서 위치 정보 등록 과정 수정

## 📌 작업 개요

### 목표
- **스케줄 탭에서 첫 스케줄 등록 시 위치 정보 등록 과정이 생략되는 문제 해결**
- `QuickCreateScheduleDialog`를 `ScheduleCreationDialog`로 교체하여 완전한 생성 플로우 제공

### 배경
**사용자 피드백**:
- 스케줄 탭의 FAB 버튼으로 시간표를 생성할 때, 위치 정보 등록 단계가 생략됨
- `QuickCreateScheduleDialog`는 간단한 생성용이었고, 위치 정보 설정 단계가 없음
- `ScheduleCreationDialog`는 위치 설정 → 시간표 생성의 완전한 플로우를 제공

**근본 원인**:
```kotlin
// ❌ ScheduleTabScreen.kt (167번 라인)
if (showCreateDialog) {
    QuickCreateScheduleDialog(  // 위치 설정 단계 없음
        onDismiss = { showCreateDialog = false },
        ...
    )
}
```

**해결 방향**:
- `ScheduleTabScreen.kt`에서 `QuickCreateScheduleDialog` → `ScheduleCreationDialog`로 교체
- 위치 정보 있을 경우 `LocationBasedAutoRun` 엔티티 생성 및 저장
- `LocationBasedAutoRunViewModel` 추가하여 위치 정보 관리

---

## 🎯 주요 변경사항

### 1. ScheduleTabScreen.kt 수정

#### Import 변경
```kotlin
// Before
import com.allday.detoxy.presentation.ui.autorun.components.QuickCreateScheduleDialog
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel

// After
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleCreationDialog
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import com.allday.detoxy.presentation.viewmodel.LocationBasedAutoRunViewModel
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
```

#### ViewModel 파라미터 추가
```kotlin
@Composable
fun ScheduleTabScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ScheduleGroupViewModel = hiltViewModel(),
    locationViewModel: LocationBasedAutoRunViewModel = hiltViewModel()  // 🆕 추가
)
```

#### 다이얼로그 교체 및 로직 수정
**Before**:
```kotlin
if (showCreateDialog) {
    QuickCreateScheduleDialog(
        onDismiss = { showCreateDialog = false },
        onConfirm = { name, mode, data ->
            // 단순 시간표 생성만 수행
            // 위치 정보 처리 없음
        }
    )
}
```

**After**:
```kotlin
if (showCreateDialog) {
    ScheduleCreationDialog(
        onDismiss = { showCreateDialog = false },
        onConfirm = { scheduleName, mode, timeSlots, template, locationInfo ->
            scope.launch {
                if (locationInfo == null) {
                    // 어디서나 적용 (위치 없음)
                    when (mode) {
                        CreationMode.TEMPLATE -> {
                            viewModel.createFromTemplate(scheduleName, template!!)
                        }
                        CreationMode.CUSTOM -> {
                            viewModel.createScheduleGroupWithTimeSlots(
                                name = scheduleName,
                                description = null,
                                timeSlots = timeSlots
                            )
                        }
                    }
                } else {
                    // 위치 기반 스케줄
                    val scheduleGroupId = when (mode) {
                        CreationMode.TEMPLATE -> {
                            viewModel.createFromTemplate(scheduleName, template!!)
                        }
                        CreationMode.CUSTOM -> {
                            viewModel.createScheduleGroupWithTimeSlots(
                                name = scheduleName,
                                description = null,
                                timeSlots = timeSlots
                            )
                        }
                    }
                    
                    // 🆕 위치 정보를 LocationBasedAutoRun으로 저장
                    val location = LocationBasedAutoRun(
                        label = locationInfo.name,
                        address = locationInfo.address,
                        latitude = locationInfo.latitude,
                        longitude = locationInfo.longitude,
                        radiusMeters = locationInfo.radiusMeters,
                        durationMinutes = 90,
                        presetType = "STANDARD",
                        triggerType = "ENTER",
                        linkedScheduleGroupId = scheduleGroupId,
                        activateScheduleOnEnter = true,
                        deactivateScheduleOnExit = true,
                        isEnabled = true
                    )
                    
                    // 위치 정보 저장
                    locationViewModel.addLocation(location)
                    
                    // 🆕 위치 데이터 갱신
                    viewModel.loadLinkedLocations(scheduleGroupId)
                }
                
                showCreateDialog = false
            }
        },
        initialMode = CreationMode.CUSTOM
    )
}
```

---

## 🔍 기술적 세부사항

### ScheduleCreationDialog의 단계별 플로우

`ScheduleCreationDialog`는 3단계로 구성됩니다:

1. **LOCATION_CHOICE**: 위치 설정 여부 선택
   - "어디서나 적용" (위치 없음)
   - "특정 위치에서만" (위치 있음)

2. **LOCATION_SEARCH**: 위치 검색 (위치 있음 선택 시)
   - 주소 또는 장소 검색
   - 반경 설정 (50m ~ 500m)

3. **SCHEDULE_SETUP**: 시간표 설정
   - 시간표 이름 입력
   - 템플릿 vs 커스텀 선택
   - 시간대 추가

### QuickCreateScheduleDialog vs ScheduleCreationDialog

| 구분 | QuickCreateScheduleDialog | ScheduleCreationDialog |
|------|---------------------------|------------------------|
| 위치 설정 | ❌ 없음 | ✅ 있음 (3단계 플로우) |
| 용도 | 빠른 생성 (내부 다이얼로그용) | 전체 생성 플로우 (메인 진입점) |
| 콜백 시그니처 | `(name, mode, data)` | `(name, mode, timeSlots, template, locationInfo)` |
| 위치 정보 반환 | ❌ 불가능 | ✅ `LocationInfo` 객체 |

---

## 📋 검증 체크리스트

### 기능 테스트
- [x] 스케줄 탭 FAB 클릭 시 `ScheduleCreationDialog` 표시 확인
- [ ] "어디서나 적용" 선택 시 위치 없이 시간표 생성 확인
- [ ] "특정 위치에서만" 선택 시 위치 검색 단계 표시 확인
- [ ] 위치 검색 후 시간표 생성 확인
- [ ] 생성된 위치 기반 시간표가 `LocationBasedAutoRun`에 저장되는지 확인
- [ ] 생성된 시간표 카드에 위치 정보 표시 확인

### UI/UX 검증
- [ ] 다이얼로그 단계별 전환이 자연스러운지 확인
- [ ] "이전" 버튼으로 단계 되돌리기 동작 확인
- [ ] 위치 검색 결과 표시 확인
- [ ] 반경 슬라이더 동작 확인
- [ ] 시간대 추가/수정/삭제 동작 확인

### 코드 품질
- [x] Linter 오류 없음 확인
- [ ] 컴파일 성공 확인
- [ ] Repository guidelines 준수 확인

---

## 🔄 관련 파일

### 수정된 파일
- `app/src/main/java/com/allday/detoxy/presentation/ui/schedule/ScheduleTabScreen.kt`

### 참조 파일 (수정 없음)
- `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleCreationDialog.kt`
- `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/QuickCreateScheduleDialog.kt`
- `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
- `app/src/main/java/com/allday/detoxy/presentation/viewmodel/LocationBasedAutoRunViewModel.kt`

---

## 📝 다음 단계

1. **빌드 및 실행 테스트**
   ```bash
   ./gradlew assembleDebug
   ```

2. **기능 테스트**
   - 스케줄 탭에서 시간표 생성
   - 위치 설정 여부 선택
   - 위치 검색 및 반경 설정
   - 시간대 추가
   - 생성된 시간표 확인

3. **문서 업데이트**
   - `docs/03.5_ui_ux_advanced_todolist.md` 업데이트
   - 위치 기반 시간표 생성 플로우 문서화

---

## 🐛 알려진 이슈

없음

---

## 💡 개선 제안

1. **일관성 있는 다이얼로그 사용**
   - `TimeBasedAutoRunScreen`도 동일하게 `ScheduleCreationDialog` 사용
   - `AddLocationAutoRunDialog`의 빠른 생성 기능에서만 `QuickCreateScheduleDialog` 사용

2. **사용자 경험 개선**
   - 위치 설정 시 현재 위치 사용 옵션 추가
   - 최근 검색한 위치 목록 표시
   - 위치 즐겨찾기 기능

---

## 📚 참고 문서
- `docs/03.5_예약설정 프로세스.md` - 시간표 생성 프로세스 정의
- `docs/03.5_ui_ux_advanced_todolist.md` - UI/UX 고도화 체크리스트

