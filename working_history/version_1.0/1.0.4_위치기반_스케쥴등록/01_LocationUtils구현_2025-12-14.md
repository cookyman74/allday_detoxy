# 01. LocationUtils 구현 및 데이터 구조 개선

**작성일**: 2025-12-14  
**작성자**: Antigravity

---

## 1. 작업 개요
위치 기반 스케줄 등록 기능의 핵심인 '현재 위치 조회'를 위한 기반 유틸리티 코드를 작성하고 데이터 구조를 개선했습니다.

## 2. 변경 내역

### 2.1 데이터 구조 확장 (`GeocoderUtils.kt`)
- `LocationInfo` 데이터 클래스에 `accuracy` 필드를 추가했습니다.
- 기본값을 `null`로 설정하여 기존 코드와의 호환성을 유지했습니다.

```kotlin
data class LocationInfo(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null // 🆕 추가
)
```

### 2.2 위치 유틸리티 구현 (`LocationUtils.kt`)
- `FusedLocationProviderClient`를 사용하여 현재 위치를 조회하는 `getCurrentLocation` 함수를 구현했습니다.
- **주요 기능**:
    - `Priority.PRIORITY_HIGH_ACCURACY`: 높은 정확도 요청
    - `withTimeout(5000L)`: 5초 타임아웃 처리
    - `CancellationTokenSource`: 코루틴 취소 시 위치 요청 취소
    - **Fallback**: 타임아웃 또는 실패 시 `getLastLocation`(마지막 알려진 위치) 시도

## 3. 검증 결과
- `LocationUtils.getCurrentLocation` 로직 검토: 정상
- 타임아웃 및 에러 처리 흐름: 정상 (TimeoutException 발생 시 catch 블록에서 getLastLocation 수행)
- 컴파일 에러 없음 확인.

## 4. 향후 계획
- UI 구현 단계에서 `LocationUtils`를 사용하여 실제 위치 데이터를 가져오고 UI에 표시할 예정입니다.
