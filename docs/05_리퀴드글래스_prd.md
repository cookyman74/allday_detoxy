# 05. 리퀴드 글래스 (Glassmorphism) 디자인 적용 PRD

## 1. 개요 (Overview)
본 문서는 'AllDay Detoxy' 앱의 UI/UX를 현대적이고 생동감 있는 **리퀴드 글래스(Liquid Glass/Glassmorphism)** 스타일로 개편하기 위한 제품 요구사항 정의서(PRD)이다. 
iOS의 시스템 UI에서 볼 수 있는 흐린 배경 효과(Blur), 반투명 레이어, 그리고 선명한 그라데이션을 안드로이드 환경(Jetpack Compose)에 최적화하여 적용함으로써 프리미엄 사용자 경험을 제공한다.

## 2. 디자인 목표 (Design Goals)
1.  **시각적 깊이감(Visual Depth)**: 배경과 컨텐츠 레이어 사이의 계층을 흐림 효과와 투명도를 통해 명확히 구분한다.
2.  **생동감(Vibrancy)**: 배경의 컬러가 전면 레이어에 은은하게 비치도록 하여 앱 전체에 활기를 부여한다.
3.  **일관성(Consistency)**: iOS와 안드로이드 간의 디자인 격차를 줄이고, 브랜드 아이덴티티를 강화하는 고품질 UI를 구축한다.

## 3. 핵심 디자인 원칙 (Core Design Principles)

### 3.1. Glassmorphism 재질 (Material)
*   **Translucency (반투명)**: 컨텐츠 영역 배경에 불투명도(Opacity) 40%~70% 정도의 White/Black 레이어를 사용한다.
*   **Background Blur (배경 흐림)**: 배경 너머의 요소가 흐릿하게 보이도록 강한 블러(Radius 20dp 이상)를 적용한다.
*   **Border (테두리)**: 유리 판의 가장자리를 강조하기 위해 1dp 두께의 미세한 그라데이션 테두리(흰색 -> 투명)를 추가한다.
*   **Shadow (그림자)**: 요소가 떠 있는 느낌을 주기 위해 부드럽고 넓게 퍼지는 그림자를 사용한다.

### 3.2. 컬러 팔레트 (Color Palette) Update
기존의 단색 배경을 탈피하고, Glass 효과를 극대화할 수 있는 그라데이션 메쉬(Mesh Gradient) 배경을 도입한다.
*   **Primary Mesh**: 브랜드 컬러를 기반으로 한 유동적인 그라데이션 배경.
*   **Glass Surface**: 
    *   Light Mode: `Color.White.copy(alpha = 0.6f)`
    *   Dark Mode: `Color.Black.copy(alpha = 0.5f)`

## 4. 적용 대상 컴포넌트 (Target Components)

### 4.1. 메인 컨테이너 및 배경
*   **Background**: 앱 전체 배경에 고정된 단색 대신, 스크롤이나 페이지 전환에 따라 미세하게 움직이거나 고정된 그라데이션 이미지를 배치한다.

### 4.2. 네비게이션 바 (Bottom Navigation Bar)
*   기존의 불투명한 바를 **플로팅(Floating)** 형태의 유리 질감 바로 변경한다.
*   화면 하단에서 띄우고, 둥근 모서리(Corner Radius)를 적용한다.
*   스크롤 시 뒤따라오는 컨텐츠가 네비게이션 바 뒤로 흐릿하게 비쳐 보여야 한다.

### 4.3. 카드 및 리스트 아이템 (Cards & List Items)
*   `Timer` 카드, `Todo` 리스트 아이템 등에 Glass Modifier를 적용한다.
*   중요도에 따라 투명도와 블러 강도를 조절하여 시각적 위계를 설정한다.

### 4.4. 다이얼로그 및 오버레이 (Dialogs & Overlays)
*   팝업 발생 시 배경을 단순히 어둡게(Dim) 처리하는 것을 넘어, 뒷 배경을 강하게 블러 처리(Backdrop Blur)하여 집중도를 높인다.

## 5. 기술적 구현 방안 (Technical Implementation)

### 5.1. 라이브러리 검토: Haze (추천)
안드로이드 API 호환성(API 26+)과 고성능 블러 구현을 위해 **Chris Banes의 `Haze`** 라이브러리 사용을 권장한다.
*   `Modifier.blur`는 Android 12 이전 버전에서 동작하지 않는 제약이 있음.
*   `Haze`는 Compose Multiplatform을 지원하며, 렌더 효과(RenderEffect)를 효율적으로 처리함.

### 5.2. 커스텀 Modifier 정의
재사용성을 위해 공통 Glass Modifier를 작성한다.

```kotlin
// 예시 코드 (수도 코드)
fun Modifier.liquidGlass(
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.5f,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier
```

### 5.3. 성능 최적화
*   실시간 블러는 GPU 비용이 높으므로, 리스트 스크롤 시 렉이 발생하지 않도록 최적화가 필요하다.
*   정적인 배경에는 미리 블러 처리된 이미지를 사용하고, 동적인 상호작용(스크롤 등)이 필요한 영역에만 실시간 블러를 적용하는 하이브리드 방식을 고려한다.

## 6. 작업 단계 (Implementation Steps)

### Phase 1: 기반 마련
1.  `libs.versions.toml`에 Haze 라이브러리 의존성 추가.
2.  `GlassScaffold` 등 기본 레이아웃 컴포넌트 생성.
3.  앱 전반에 사용할 `MeshGradient` 배경 리소스 추가.

### Phase 2: 주요 컴포넌트 변환
1.  **BottomNavigation**: 기존 컴포넌트를 `GlassBottomNavigation`으로 교체.
2.  **Home/Timer Screen**: 메인 대시보드 카드들에 Glass 효과 적용.
3.  **Dialogs**: 커스텀 다이얼로그에 Backdrop Blur 적용.

### Phase 3: 디테일 및 최적화
1.  테두리(Border) 디테일 및 그림자 조정.
2.  저사양 기기에서의 성능 테스트 및 Fallback(단순 투명도 처리) 구현.

## 7. 성공 기준 (Success Metrics)
*   사용자 심미성 만족도 증가.
*   스크롤 및 애니메이션 프레임 드랍(Jank) 없이 60fps 유지.
