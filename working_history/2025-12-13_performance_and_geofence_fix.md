# 작업 기록: 성능 최적화 및 위치기반 스케줄 수정

**작업 일시**: 2025-12-13
**작업 범위**: `GlassModifier`, `GlassSurface`, `TimerStyle*`, `ScheduleGroupManager`

---

## 1. Liquid Glass 성능 최적화

### 문제 상황
- Liquid Glass UI 적용 후 스크롤 시 끊김 현상 발생
- 특히 ReportScreen에서 심각한 프레임 드랍
- 원인: `GlassSurface`가 Haze 라이브러리의 실시간 블러를 사용하여 GPU 과부하

### 수정 내용

#### 1.1 경량 Glass 컴포넌트 추가
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/component/GlassModifier.kt`
- **변경**: `simpleGlass()` modifier 추가
  - 실시간 블러 없이 단순 반투명 배경 + 테두리만 적용
  - `backgroundColor` 파라미터 지원 (커스텀 배경색)

- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/component/GlassSurface.kt`
- **변경**: `SimpleGlassSurface` Composable 추가
  - LazyColumn/LazyRow 내 리스트 아이템에 사용 권장

#### 1.2 리스트 아이템 최적화 적용
- **파일**: `ReportScreen.kt`, `ScheduleTabScreen.kt`, `ScheduleGroupScreen.kt`, `ScheduleGroupCard.kt`
- **변경**: 리스트 내 `GlassSurface` → `SimpleGlassSurface` 교체

### 검증 결과
- **빌드 검증**: `./gradlew compileDebugKotlin` 성공
- **효과**: 스크롤 시 프레임 드랍 방지, 시각적 품질 유사하게 유지

---

## 2. Visual Timer 게이지 표시 수정

### 문제 상황
- RUNNING 상태에서 게이지가 `progress(비율)` 기준으로 계산됨
- 예: 15분 타이머에서 12분 10초 남음 → 292도(48.7분 위치)까지 표시
- 60분 기준 눈금과 불일치하여 사용자 혼란

### 수정 내용

#### 2.1 TimerStyleGlassSector (60분 기준)
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/components/TimerStyleGlassSector.kt`
- **변경**: 
  - Before: `sweepAngle = 360f * progress`
  - After: `sweepAngle = remainingMinutesFloat * 6f` (1분 = 6도)

#### 2.2 TimerStyleLiquidRing (180분 기준)
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/components/TimerStyleLiquidRing.kt`
- **변경**: 
  - Before: `sweepAngle = 360f * progress`
  - After: `sweepAngle = (remainingMinutesFloat / maxMinutes) * 360f`

#### 2.3 TimerStyleMinimalFlux (60분 기준)
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/components/TimerStyleMinimalFlux.kt`
- **변경**: 
  - Before: `sweepAngle = 360f * progress`
  - After: `sweepAngle = remainingMinutesFloat * 6f` (1분 = 6도)

### 검증 결과
- **빌드 검증**: `./gradlew compileDebugKotlin` 성공
- **효과**: 게이지가 60분/180분 기준 눈금과 정확히 일치

---

## 3. 위치기반 스케줄 Geofence 유지 수정

### 문제 상황
- 시간 기반 스케줄은 정상 작동하지만, 위치 기반 스케줄이 작동하지 않음
- 원인: `deactivateGroup()`에서 Geofence 해제
- Geofence 없음 → 위치 진입 감지 불가 → 스케줄 그룹 자동 활성화 불가

### 수정 내용

- **파일**: `app/src/main/java/com/allday/detoxy/core/manager/ScheduleGroupManager.kt`
- **변경**:
  - `deactivateGroup()` 내 Geofence 해제 로직 제거
  - 스케줄 그룹 비활성화 시 알람만 취소, Geofence는 유지
  - Geofence가 항상 등록되어 있어야 위치 진입 시 자동 활성화 가능

### 동작 비교

| 상황 | Before | After |
|-----|--------|-------|
| 그룹 비활성화 | 알람 취소 + Geofence 해제 | 알람만 취소 |
| 집에서 나감 | Geofence 해제됨 | Geofence 유지 |
| 집으로 돌아옴 | 위치 감지 안됨 ❌ | 위치 감지 → 자동 활성화 ✅ |

### 검증 결과
- **빌드 검증**: `./gradlew compileDebugKotlin` 성공
- **예상 동작**:
  1. 위치 이탈 시: 알람만 취소, Geofence는 유지
  2. 위치 재진입 시: Geofence 트리거 → 스케줄 그룹 자동 활성화 → 알람 등록

---

## 커밋 이력

| 커밋 ID | 설명 |
|---------|------|
| `4726699` | perf: Liquid Glass 성능 최적화 - 리스트 아이템 블러 제거 |
| `e2a06dd` | fix(timer): Visual Timer 게이지 표시 수정 |
| `010ab67` | fix(timer): LiquidRing, MinimalFlux 타이머 게이지도 동일하게 수정 |
| `222d6d8` | fix(geofence): 스케줄 그룹 비활성화 시 Geofence 유지 |

---

## 참고

### 이전 작업과의 관계
- `2025-12-06_fix_location_autorun_disable_logic.md`에서 수정한 내용의 반대 케이스 수정
- 이전: 수동 비활성화 시에도 Geofence가 살아있어 자동 활성화되는 문제
- 이번: Geofence가 해제되어 위치 진입 시 자동 활성화가 안되는 문제

### Geofence 동작 정리
- **Geofence 등록**: `LocationBasedAutoRun.isEnabled = true`이면 항상 등록
- **Geofence 해제**: 위치 삭제 또는 `isEnabled = false`일 때만 해제
- **스케줄 그룹 활성화/비활성화**: 알람만 등록/취소 (Geofence 영향 없음)

