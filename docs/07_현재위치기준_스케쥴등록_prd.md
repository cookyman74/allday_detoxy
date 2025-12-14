# 07. 현재 위치 기준 스케줄 등록 기능 추가

> **작성일**: 2025-12-14  
> **상태**: Draft  
> **작성자**: Antigravity

---

## 1. 개요 (Overview)

### 1.1 배경
기존 위치 기반 스케줄 등록 방식은 사용자가 주소를 직접 검색(텍스트 입력)하고 목록에서 선택하는 방식이었습니다. 이는 정확한 주소를 모를 때 불편하며, 현재 내가 있는 장소를 즉시 등록하고 싶을 때 번거로운 과정을 거쳐야 합니다.

### 1.2 목표
- **현재 위치 등록 편의성 강화**: 주소 검색 다이얼로그에 "현재 위치로 설정" 기능을 추가하여, 별도의 검색어 입력 없이 원클릭으로 현재 좌표를 스케줄 위치로 등록할 수 있도록 합니다.
- **정확도 향상**: GPS 좌표를 직접 사용하여 검색어 매칭 오류를 최소화합니다.

---

## 2. 요구사항 (Requirements)

### 2.1 기능 요구사항
1.  **현재 위치 찾기 버튼 제공**:
    - 위치 검색 다이얼로그(LocationSearchDialog / LocationSearchStep) 내에 "📍 현재 위치로 찾기" 버튼을 제공합니다.
2.  **권한 처리**:
    - 버튼 클릭 시 `ACCESS_FINE_LOCATION` 권한을 확인하고, 없으면 요청합니다.
3.  **좌표 획득 및 주소 변환**:
    - `FusedLocationProviderClient`를 사용하여 현재의 정확한 위도/경도를 획득합니다.
    - 획득한 좌표를 `Geocoder`를 통해 주소로 변환(Reverse Geocoding)하여 사용자에게 표시합니다.
4.  **자동 선택**:
    - 주소 변환이 완료되면 자동으로 해당 위치가 선택된 상태로 상세 설정 화면으로 전환됩니다.

### 2.2 UI/UX 요구사항 (상세)

#### A. 진입점 (Entry Point)
-   **위치**: `LocationSearchDialog` 및 `LocationSearchStep`의 검색 입력 필드 우측 또는 하단.
-   **컴포넌트**: `IconButton`(아이콘: `Icons.Default.MyLocation`) 또는 "현재 위치로 찾기" 텍스트 버튼.
-   **배치 전략**:
    -   **Option 1 (권장)**: 검색바 `trailingIcon`에 검색 아이콘 대신 '현재 위치' 아이콘 배치, 검색 아이콘은 입력 시 표시.
    -   **Option 2**: 검색바 바로 아래에 `TextButton` 형태로 "📍 현재 위치로 설정" 배치.

#### B. 로딩 및 피드백 (Feedback)
-   **로딩 상태**: 좌표를 찾는 동안 게이지 형태(CircularProgressIndicator)가 아닌, "위치를 찾는 중..." 텍스트와 함께 은은한 Pulse 애니메이션 또는 스켈레톤 UI 적용.
-   **성공 피드백**: 주소 변환 성공 시, 검색 결과 리스트 최상단에 "📍 [현재 위치] 주소..." 형태로 강조(Highlight)하여 표시.
-   **정확도 표시**: 획득한 위치의 정확도(예: ±15m)를 Chip 형태로 주소 옆에 표시.

#### C. 지도 확인 (Optional)
-   등록된 좌표를 외부 지도 앱으로 확인하는 "지도에서 보기" 링크를 제공합니다.

#### D. 에러 처리 및 예외 상황 UI
-   **GPS 미수신**: Toast 메시지("위치를 찾을 수 없습니다") 대신, 검색창 하단에 "위치 서비스를 켜주세요"라는 힌트 텍스트와 [설정] 링크 노출.
-   **권한 거부**: "위치 권한이 필요합니다" 다이얼로그 후 설정 이동 버튼 제공.
-   **네트워크/타임아웃**: "위치 정보를 가져오는 데 실패했습니다" 스낵바와 함께 [재시도] 액션 제공.

---

## 3. UI/UX 상세 설계 (Where & How)

### 3.1 변경 위치 (Where)
코드 중복을 방지하기 위해 **공통 컴포넌트(`LocationSearchContent`)**를 분리하여 다음 두 파일에 적용합니다.

1.  **`AddLocationAutoRunDialog.kt`**
    -   Target: `LocationSearchStep` Composable (Line ~315) -> `LocationSearchContent`로 대체

2.  **`LocationEditDialog.kt`**
    -   Target: `LocationSearchDialog` Composable (Line ~320) -> `LocationSearchContent`로 대체

### 3.2 UI 변경안 (How)

**[AS-IS] 기존 화면**
```
+--------------------------------+
|          위치  추가             |
+--------------------------------+
| [ 🔍 주소 또는 장소 검색      ] |  <-- 검색창만 존재
|                                |
| (검색 결과 리스트 영역)          |
|                                |
+--------------------------------+
```

**[TO-BE] 변경된 화면**
```
+--------------------------------+
|          위치  추가             |
+--------------------------------+
| [ 🔍 주소 또는 장소 검색    (◎)] | <-- (◎) 현재 위치 아이콘 추가 (Trailing Icon)
+--------------------------------+
|                                |
|  [ 📍 현재 위치로 설정하기 ]      | <-- 강조된 보조 버튼 (OutlinedButton)
|                                |
+--------------------------------+
| ▼ 검색 결과                     |
| ------------------------------ |
| 📍 [현재위치] 서울시 강남구...     | <-- (위치 찾기 성공 시 최상단 노출)
|    오차범위: ±15m               |
| ------------------------------ |
| 📍 스타벅스 강남점               |
+--------------------------------+
```

### 3.3 인터랙션 흐름
1.  **버튼 클릭**: "현재 위치로 설정하기" 또는 (◎) 아이콘 클릭
2.  **권한 확인**: 
    -   (없음) -> 시스템 권한 팝업 -> (승인) -> 3번 이동
    -   (있음) -> 3번 이동
3.  **로딩**: 검색 결과 영역에 "위치 찾는 중..." 표시 (스피너)
4.  **결과 표시**: 
    -   성공: 검색창에 주소 자동 입력 + 리스트 최상단에 결과 카드 표시 (자동 선택되지 않음, 사용자가 카드 클릭하여 확인 후 이동)
    -   실패: "위치를 찾을 수 없습니다" 스낵바 표시

---

## 4. 기술적 고려사항 (Technical Considerations)

### 4.1 데이터 구조 개선
-   **LocationInfo 확장**: 정확도 표시를 위해 `accuracy` 필드를 추가해야 합니다.
    ```kotlin
    data class LocationInfo(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float? = null // 🆕 추가
    )
    ```

### 4.2 아키텍처 개선
-   **LocationUtils 분리**: `GeocoderUtils`는 주소 변환에 집중하고, 위치 획득 로직은 `LocationUtils`로 분리합니다.
-   **Hilt 일관성**: `FusedLocationProviderClient`는 `GeofenceModule`에서 제공하므로 ViewModel에서 `@Inject`로 주입받아 사용합니다.

### 4.3 위치 서비스 (Location Services)
-   `Play Services Location` 라이브러리의 `FusedLocationProviderClient.getCurrentLocation()`을 권장합니다.
-   `Priority.PRIORITY_HIGH_ACCURACY`를 사용하여 정확한 위치를 얻도록 합니다.
-   **Timeout**: 배터리 소모 방지를 위해 위치 요청에 5초 Timeout을 설정합니다.

### 4.4 에러 처리 전략
-   **GPS 꺼짐**: `LocationSettingsRequest`로 켜기 팝업 유도 또는 설정 창 이동.
-   **네트워크 오류**: 역지오코딩 실패 시 "알 수 없는 위치 (위도, 경도)"로 폴백 처리하여 좌표 등록은 가능하게 합니다.

---

## 4. 데이터 흐름 (Data Flow)

1.  사용자가 "현재 위치로 설정" 클릭
2.  앱이 권한 확인 (권한 승인 시 진행)
3.  `FusedLocationProviderClient`로 좌표(Example: 37.123, 127.456) 획득
4.  `GeocoderUtils` 호출 -> 역지오코딩 수행
5.  결과(LocationInfo) 반환
6.  UI 상태 업데이트 (SelectedLocation = 현재위치)
7.  상세 설정 단계(Step 2)로 이동

---

## 5. 성공 지표 (Success Metrics)
- "현재 위치" 기능을 통한 스케줄 등록 비율 증가
- 위치 등록 소요 시간 단축

