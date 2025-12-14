# 07. 현재 위치 기반 스케줄 등록 작업 계획서

> **관련 문서**: [07_현재위치기준_스케쥴등록_prd.md](./07_현재위치기준_스케쥴등록_prd.md)  
> **작성일**: 2025-12-14  
> **관련 문서**: [07_현재위치기준_스케쥴등록_prd.md](./07_현재위치기준_스케쥴등록_prd.md)  
> **작성일**: 2025-12-14  
> **예상 총 소요**: 1.0일  
> **상태**: 작업 대기

---

## 📋 작업 규칙

### 사전작업 규칙
- `GeocoderUtils.kt`의 현재 구현 상태 확인 (이미 `getAddressFromCoordinates`가 존재하는지 확인)
- `AndroidManifest.xml`의 위치 권한 선언 확인

### 작업 규칙
- UI와 로직을 분리하여 구현 (Utils -> UI)
- 권한 처리는 Accompanist Permissions 또는 Compose 표준 방식을 사용
- 에러 케이스(GPS 꺼짐, 네트워크 오류)에 대한 방어 로직 포함

### 작업후처리 규칙
- 작업 완료 후 `working_history/version_2.0/{단계번호}_{작업타이틀}_{날짜}.md` 파일 작성
- [working_history_template.md](../working_history/version_2.0/working_history_template.md) 형식 준수

---

## 🔧 단계별 작업 계획

### 📌 단계 1: 유틸리티 및 데이터 구조 개선

**예상 소요**: 0.3일

#### 사전작업
#### 사전작업
- [ ] 이전 작업 내역 확인 (`working_history/` 확인)
- [ ] `GeocoderUtils.kt` 현재 구현 상태 분석 (비동기 처리 등)
- [ ] `GeofenceModule.kt` Hilt 주입 설정 확인

#### 작업
- [x] **1.1** `LocationInfo` 데이터 클래스 확장
  - `accuracy: Float? = null` 필드 추가
  - 관련 `searchLocation` 및 `getAddressFromCoordinates` 반환 값 수정

- [x] **1.2** `LocationUtils.kt` (신규 파일) 생성
  - `FusedLocationProviderClient`를 사용하는 `getCurrentLocation` 함수 구현
  - `suspend` 함수로 구현 (CoroutineScope 내 호출)
  - **Timeout 처리**: `withTimeout(5000)` 등으로 5초 제한 설정
  - **Retry 로직**: 실패 시 `getLastLocation` 폴백 고려
  - **정확도 반환**: `LocationResult` 또는 `LocationInfo` 반환

#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/01_LocationUtils구현_2025-12-14.md`

### 📌 단계 2: UI 구현 및 리팩토링

**예상 소요**: 0.5일

#### 사전작업
#### 사전작업
- [ ] 단계 1의 작업 결과 확인 (`working_history/version_2.0/01_LocationUtils구현_...md`)
- [ ] `AddLocationAutoRunDialog.kt` 및 `LocationEditDialog.kt` UI 구조 및 중복 코드 분석
- [ ] `AndroidManifest.xml` 권한 설정 재확인

#### 작업
- [x] **2.0** 공통 컴포넌트 `LocationSearchContent` 분리
  - `AddLocationAutoRunDialog`와 `LocationEditDialog`의 중복된 검색 로직 추출
  - 검색창, 리스트, 로딩, 에러 UI 포함

- [x] **2.1** `LocationSearchContent` UI 개편
  - **검색바 영역 수정**: `OutlinedTextField`의 `trailingIcon`에 '현재 위치' 아이콘(`Icons.Default.MyLocation`) 추가 (입력 값이 없을 때만 표시)
  - **보조 버튼 추가**: 검색바 하단에 `TextButton`으로 "📍 현재 위치로 설정" 명시적 버튼 추가 (접근성 고려)
  - **권한 런처 연결**: 버튼 클릭 시 `rememberLauncherForActivityResult` 실행 (권한 체크 -> 요청 -> 조회)

- [x] **2.2** 권한 획득 후 로직 연결
  - 권한 승인 시 `GeocoderUtils.getCurrentLocation` 호출
  - **로딩 UI**: 검색 결과 영역에 "위치 찾는 중..." 텍스트와 `LinearProgressIndicator` 표시
  - 결과 수신 시 `onLocationSelected` 호출 및 주소 필드 자동 채움
  
- [x] **2.3** 정확도 및 지도 확인 UI 추가
  - `LocationSettingsStep` (상세 설정) 화면에서 주소 카드 내에:
    - 정확도 Chip (`AssistChip` 등 활용): "오차 ±15m" (LocationInfo.accuracy 활용)
    - 지도 아이콘 버튼: 클릭 시 구글지도/네이버지도 Intent 실행 (`geo:lat,lng`)

- [x] **2.4** 기존 다이얼로그에 공통 컴포넌트 적용
  - `AddLocationAutoRunDialog.kt` -> `LocationSearchContent` 사용
  - `LocationEditDialog.kt` -> `LocationSearchContent` 사용


#### 작업후처리
- [x] 작업결과서 작성: `working_history/version_2.0/02_LocationUI구현_2025-12-14.md` (Step 2.0~2.2, 2.4)
- [x] 작업결과서 작성: `working_history/version_2.0/03_Location기능테스트_2025-12-14.md` (Step 2.3 포함)

### 📌 단계 3: 테스트 및 디버깅

**예상 소요**: 0.2일

#### 사전작업
- [ ] 단계 2의 작업 결과 확인 (`working_history/version_2.0/02_LocationUI구현_...md`)
- [ ] 테스트 시나리오(GPS Off, 권한 거부 등) 준비 상태 점검

#### 작업
- [ ] **3.1** 권한 시나리오 테스트
  - 권한 없음 -> 팝업 -> 승인 -> 위치 조회 성공
  - 권한 없음 -> 팝업 -> 거부 -> 안내 다이얼로그 표시
- [ ] **3.2** 예외 상황 테스트
  - **GPS 꺼짐**: 설정 이동 안내 확인
  - **네트워크 오류**: 좌표 기반("알 수 없는 위치") 등록 확인
  - **낮은 정확도**: 정확도 Chip 경고 색상/텍스트 확인
- [ ] **3.3** 실제 위치 확인
  - 에뮬레이터/기기 위치 변경 후 "현재 위치" 클릭 시 올바른 주소 표시 확인
  - "지도에서 확인" 버튼 클릭 시 외부 지도 앱 연동 확인

#### 작업후처리
- [ ] 작업결과서 작성: `working_history/version_2.0/03_Location기능테스트_2025-12-14.md`

---

## 📊 진행 상황 추적

| 단계 | 작업명 | 상태 | 시작일 | 완료일 | 작업결과서 |
|---|---|---|---|---|---|
| 1 | 유틸리티 및 권한 로직 보강 | ✅ 완료 | 2025-12-14 | 2025-12-14 | 생성완료 |
| 2 | UI 구현 (현재 위치 버튼 추가) | ✅ 완료 | 2025-12-14 | 2025-12-14 | 생성완료 |
| 3 | 테스트 및 디버깅 | ⬜ 대기 | - | - | - |
| 3 | 테스트 및 디버깅 | ⬜ 대기 | - | - | - |

**상태 범례**: ⬜ 대기 | 🔄 진행중 | ✅ 완료 | ⏸️ 보류
