# Hotfix v1.1.2: 위치기반 스케줄 DB-Geofence 불일치 개선

> **작업일자**: 2026-01-19  
> **버전**: v1.1.2  
> **상태**: ✅ 완료

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| 문제 | UI "위치 모니터링 5/5" 표시되나 실제 Geofence 미등록 |
| 원인 | DB `isEnabled` 와 실제 Geofence 등록 상태의 불일치 |
| 해결 | DB-Geofence 일관성 보장 로직 추가 |
| 영향 파일 | 6개 파일 수정 |

---

## 🔧 변경 사항

### Phase 1: 권한 해제 시 DB 비활성화 처리

| 파일 | 변경 내용 |
|------|----------|
| `LocationBasedAutoRunDao.kt` | `disableAll()` 메서드 추가 |
| `LocationBasedAutoRunRepository.kt` | `disableAll()` 래퍼 함수 추가 |
| `LocationBasedAutoRunViewModel.kt` | `checkPermissions()`에서 Geofence 제거 성공 시에만 DB 비활성화 |

#### 핵심 변경 (ViewModel)
```kotlin
// 권한 해제 감지 시
if (previousHasFullPermission && !currentHasFullPermission) {
    val removeResult = geofenceManager.removeAllGeofences()
    if (removeResult.isSuccess) {
        repository.disableAll()  // 🆕 DB도 일괄 비활성화
    }
}
```

---

### Phase 2: rescheduleAll 개선

| 파일 | 변경 내용 |
|------|----------|
| `AutoRunGeofenceManager.kt` | `RescheduleResult` 클래스 추가, `rescheduleAll()` 개선 |
| `LocationBasedAutoRunDao.kt` | `disableByIds()` 메서드 추가 |
| `BootCompletedReceiver.kt` | rescheduleAll 결과 처리 로직 추가 |
| `DetoxyApplication.kt` | rescheduleAll 결과 처리 로직 추가 |

#### RescheduleResult 구조
```kotlin
data class RescheduleResult(
    val successIds: List<String>,          // 등록 성공
    val failedPermanentIds: List<String>,  // 영구 실패 (권한, Play Services)
    val failedTempIds: List<String>,       // 일시 실패 (위치 서비스 OFF)
    val skippedIds: List<String>           // 제한 초과 스킵
)
```

#### 핵심 개선 사항
1. **잔존 Geofence 방지**: 모든 enabled ID의 Geofence를 먼저 제거 후 재등록
2. **실패 사유별 분기**: 영구적 실패만 DB 비활성화, 일시적 실패는 유지
3. **DB 상태 동기화**: 영구 실패 + 스킵 항목만 `isEnabled=false` 처리

---

## 📊 Diff 요약

| 파일 | 추가 | 삭제 | 수정 |
|------|------|------|------|
| `LocationBasedAutoRunDao.kt` | +21 | 0 | 0 |
| `LocationBasedAutoRunRepository.kt` | +11 | 0 | 0 |
| `LocationBasedAutoRunViewModel.kt` | +13 | -3 | 체크로직 |
| `AutoRunGeofenceManager.kt` | +47 | -20 | rescheduleAll |
| `BootCompletedReceiver.kt` | +15 | -2 | 결과처리 |
| `DetoxyApplication.kt` | +15 | -2 | 결과처리 |

---

## ✅ 빌드 검증

```
BUILD SUCCESSFUL in 1m 46s
43 actionable tasks: 14 executed, 29 up-to-date
```

---

## 🧪 테스트 시나리오 (수동 검증 필요)

| 시나리오 | 예상 결과 |
|----------|----------|
| 권한 해제 후 앱 복귀 | UI "0/5" 표시, DB 일괄 비활성화 |
| 앱 재시작 시 6개 이상 활성화 | 초과분 DB 비활성화 + Geofence 제거 |
| Play Services 없는 환경에서 앱 시작 | 영구 실패로 분류, DB 비활성화 |
| 위치 서비스 OFF 상태에서 앱 시작 | 일시 실패로 분류, DB 유지 |

---

## 📝 관련 문서

- 작업 계획서: `docs/version_1.1/hotfix_v1.1.2_위치기반스케쥴_DB_Geofence_불일치_개선_todolist.md`
- 이전 핫픽스: `위치기반_개수제한_핫픽스_2026-01-13.md`

---

**작성자**: Antigravity AI  
**작업일**: 2026-01-19
