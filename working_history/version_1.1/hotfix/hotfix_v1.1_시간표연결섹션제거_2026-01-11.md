# Hotfix: v1.1 - 시간표 연결 섹션 제거

## 📋 작업 개요

| 항목 | 내용 |
|-----|-----|
| **작업일** | 2026-01-11 |
| **브랜치** | hotfix/v1.1 |
| **버전** | v1.1.x |
| **기반 커밋** | `aaee2f3` (hotfix(v1.1) : 위치 정보 수정 기능 복구) |

## 🎯 문제 정의

### 현상
시간대(TimeBasedAutoRun) 편집 화면에서 **"시간표 연결"** 섹션이 불필요하게 노출되어 사용자에게 혼란을 야기함.

### 문제점
1. **맥락 혼동**: 시간대 편집 화면에서 "시간표 연결"은 의미 없음 (위치 기반 기능에만 해당)
2. **불필요한 복잡도**: 사용자가 시간대 추가/수정 시 필요한 것은 시작 시간, 요일, 반복 설정 뿐
3. **비활성화 상태 노출**: 비활성화된 옵션이 표시되어 혼란 가중

## ✅ 해결 방법

**"시간표 연결" 섹션 완전 제거** 및 **관련 불필요 코드 정리**

## 📁 수정 파일

### [MODIFY] AddTimeBasedAutoRunDialog.kt
- **경로**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/AddTimeBasedAutoRunDialog.kt`

### 변경 내용

#### 1차 작업: UI 섹션 제거
- "시간표 연결" 제목 텍스트 제거
- `ScheduleLinkSection` 컴포저블 호출 제거
- `ScheduleLinkSection()` 함수 전체 제거 (127줄)

#### 2차 작업: 리뷰 반영 (리팩토링)

| 리뷰 의견 | 조치 |
|---------|-----|
| 불필요 상태 변수 잔존 | `actualIsLocationBased`, `selectedScheduleGroupId`, `isIndependent` 상태 변수 제거. 저장 로직에서 직접 계산으로 변경 |
| 기본 요일 계산 최신성 | `enabledDays` remember 키에 `isDaily` 추가. scheduleGroups 비동기 로딩 완료 후 재계산되도록 수정 |
| 작업 결과서 표준 누락 | 빌드 명령어 및 커밋 ID 추가 |

## 📊 코드 변경 요약

```diff
// 상태 변수 제거
- val actualIsLocationBased = remember(...) { ... }
- var selectedScheduleGroupId by remember { ... }
- var isIndependent by remember { ... }

// enabledDays remember 키 수정
- val enabledDays = remember(existingAutoRun) {
+ val targetGroupId = existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId
+ val targetGroup = scheduleGroups.find { it.id == targetGroupId }
+ val isDaily = targetGroup?.name?.contains("매일") == true || 
+               targetGroup?.name?.contains("Daily", ignoreCase = true) == true
+ val enabledDays = remember(existingAutoRun, isDaily) {

// 저장 로직 단순화
- val finalScheduleGroupId = if (actualIsLocationBased) { ... } else { selectedScheduleGroupId }
+ val finalScheduleGroupId = existingAutoRun?.scheduleGroupId ?: initialScheduleGroupId
```

## ✅ 검증 결과

### 빌드 명령어
```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

### 빌드 결과
```
BUILD SUCCESSFUL in 6s
18 actionable tasks: 3 executed, 15 up-to-date
```

### 수동 검증 항목
- [ ] 시간대 추가 다이얼로그에서 "시간표 연결" 섹션이 사라졌는지 확인
- [ ] 시간대 편집 다이얼로그에서 "시간표 연결" 섹션이 사라졌는지 확인
- [ ] "매일" 그룹에서 시간대 추가 시 월~일 기본 선택 확인
- [ ] 기존 시간대 데이터가 정상 표시되는지 확인

## 📝 참고 사항

- `isLocationBased` 파라미터는 API 호환성 유지를 위해 시그니처에 유지 (`@Suppress("UNUSED_PARAMETER")`)
- 위치 기반 스케줄(LocationBasedAutoRun)의 시간표 연결은 `AddLocationAutoRunDialog`에서 별도 관리

---
**작성일**: 2026-01-11
