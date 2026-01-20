# 핫픽스: 스케쥴 상세화면 수정/삭제 버튼 기능 복구

**작업일자**: 2026-01-11  
**버전**: v1.1  
**작업자**: AI Assistant

---

## 문제 증상

스케쥴 그룹 상세 화면에서 상단의 수정(연필 아이콘) 및 삭제(휴지통 아이콘) 버튼을 클릭해도 아무 반응이 없음.

> 참고: 사용자 첨부 이미지 (스케쥴 상세 화면에서 수정/삭제 버튼 위치)

---

## 원인 분석

`ScheduleGroupDetailScreen.kt` 파일의 142~157번 라인에서 수정/삭제 버튼의 `onClick` 핸들러가 `/* TODO */` 코멘트로만 처리되어 있어 클릭해도 아무 동작을 하지 않음.

```kotlin
// 수정 버튼 (Line 142) - 수정 전
IconButton(onClick = { /* TODO: 수정 다이얼로그 */ }) { ... }

// 삭제 버튼 (Line 151) - 수정 전
IconButton(onClick = { /* TODO: 삭제 확인 */ }) { ... }
```

---

## 수정 내용

### 파일: `ScheduleGroupDetailScreen.kt`

| 항목 | 변경 내용 |
|------|----------|
| Import 추가 | `AddScheduleGroupDialog` 컴포넌트 import |
| 상태 변수 추가 | `showEditDialog`, `showDeleteDialog` 추가 |
| 버튼 핸들러 연결 | 수정 버튼 → `showEditDialog = true`, 삭제 버튼 → `showDeleteDialog = true` |
| 다이얼로그 UI 추가 | `AddScheduleGroupDialog` (편집 모드), `AlertDialog` (삭제 확인) |

### 코드 변경 요약

```diff
+ import com.allday.detoxy.presentation.ui.autorun.components.AddScheduleGroupDialog

+ var showEditDialog by remember { mutableStateOf(false) }
+ var showDeleteDialog by remember { mutableStateOf(false) }

- IconButton(onClick = { /* TODO: 수정 다이얼로그 */ }) { ... }
+ IconButton(onClick = { showEditDialog = true }) { ... }

- IconButton(onClick = { /* TODO: 삭제 확인 */ }) { ... }
+ IconButton(onClick = { showDeleteDialog = true }) { ... }

+ // 수정 다이얼로그
+ if (showEditDialog) {
+     AddScheduleGroupDialog(
+         onDismiss = { showEditDialog = false },
+         onConfirm = { name, description -> ... },
+         existingGroup = group
+     )
+ }

+ // 삭제 확인 다이얼로그
+ if (showDeleteDialog) {
+     AlertDialog( ... )
+ }
```

---

## 검증 결과

- ✅ **빌드 성공**: `./gradlew :app:compileDebugKotlin` - BUILD SUCCESSFUL
- ⏳ **수동 테스트 필요**: 에뮬레이터 또는 실제 기기에서 UI 동작 확인 필요

---

## 테스트 시나리오

### 수정 기능
1. 스케쥴 탭 → 스케쥴 그룹 선택 → 상세 화면 진입
2. 상단 연필 아이콘(수정 버튼) 클릭
3. **기대 결과**: 스케쥴 그룹 편집 다이얼로그 표시
4. 이름/설명 수정 후 "수정" 버튼 클릭
5. **기대 결과**: 변경 사항 반영, 헤더에 새 이름 표시

### 삭제 기능
1. 상단 휴지통 아이콘(삭제 버튼) 클릭
2. **기대 결과**: 삭제 확인 다이얼로그 표시
3. "삭제" 버튼 클릭
4. **기대 결과**: 목록으로 이동, 해당 그룹 삭제됨

---

## 추가 핫픽스: 위치 정보 수정 기능 복구

### 문제 증상

스케쥴 그룹 상세 화면에서 위치 정보(위치 섹션)를 클릭해도 수정 다이얼로그가 표시되지 않음.

### 원인 분석

위치 섹션이 클릭 불가능하게 되어 있었고, `LocationEditDialog` 및 `LocationBasedAutoRunViewModel` 연결이 누락됨.

### 수정 내용

| 항목 | 변경 내용 |
|------|----------|
| Import 추가 | `LocationEditDialog`, `LocationBasedAutoRun`, `LocationBasedAutoRunViewModel` |
| ViewModel 추가 | `locationViewModel` 파라미터 추가 |
| 상태 변수 추가 | `showLocationEditDialog`, `editingLocation` |
| 위치 섹션 UI | 클릭 가능하게 변경 + 화살표 아이콘 추가 |
| 다이얼로그 연결 | `LocationEditDialog` UI 추가 |

### 테스트 시나리오

**위치 수정 기능**:
1. 위치 기반 스케쥴 그룹 상세 화면 진입
2. 위치 정보 섹션(은평구 등) 클릭
3. **기대 결과**: 위치 정보 수정 다이얼로그 표시

**위치 기반 스케쥴 해제**:
1. 위치 정보 수정 다이얼로그에서 "위치 기반 자동 실행" 스위치 OFF
2. "저장" 버튼 클릭
3. **기대 결과**: 위치 기반 자동 실행이 비활성화됨

---

## 리뷰 피드백 반영 (2026-01-11)

### Medium 이슈 수정

| 피드백 | 수정 내용 |
|--------|----------|
| 삭제 확인에서 `onBack()` 즉시 호출 | 삭제를 코루틴으로 감싸고, 성공 시에만 `onBack()` 호출. 실패 시 스낵바로 에러 메시지 표시. |
| `rememberCoroutineScope` 사용 시 화면 제거 시 코루틴 취소 위험 | `withContext(NonCancellable)` 적용으로 화면 제거 시에도 위치 업데이트 완료 보장. |

### 코드 변경 요약

**삭제 로직 개선** (`ScheduleGroupDetailScreen.kt`):
```kotlin
scope.launch {
    try {
        viewModel.deleteScheduleGroupSuspend(groupId) // suspend 버전 사용
        showDeleteDialog = false
        onBack() // 성공 시에만 화면 닫기
    } catch (e: Exception) {
        snackbarHostState.showSnackbar("그룹 삭제 실패: ${e.message}")
    }
}
```

**위치 업데이트 NonCancellable 적용**:
```kotlin
scope.launch {
    withContext(NonCancellable) { // 화면 제거 시에도 완료 보장
        locationViewModel.updateLocation(updatedLocation)
        ...
    }
}
```

**ViewModel 추가 함수** (`ScheduleGroupViewModel.kt`):
- `deleteScheduleGroupSuspend()`: UI에서 삭제 완료를 기다릴 수 있는 suspend 함수

### Low 이슈 반영

- 스크린샷 절대 경로를 설명 텍스트로 대체
