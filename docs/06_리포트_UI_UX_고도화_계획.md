# 리포트 UI/UX 고도화 및 디자인 통일화 작업 계획 (v2)

## 1. 개요

### 1.1 현황 분석
현재 `ReportScreen`은 **부분적 Glassmorphism**이 적용된 상태입니다:
- ✅ **Glass 적용됨**: `GlassScaffold`, 상단 통계 카드(`GlassStatCard`), 성공률 카드, 세션 카드
- ❌ **미적용 (Material Card)**: `DetoxyRiskCard`, `RecoveryTrendCard`, `DistractionTopCard`, `CoachRecommendationCard`, `DistractionAvoidanceCard`, `AllowedAppDwellCard`, `EmptyStateCard`

이로 인해 화면 전체에서 **시각적 이질감**이 발생하고 있습니다.

### 1.2 목표
**"Liquid Glass Design System"**으로 전체 리포트 화면을 통일하여:
1. 앱 전체의 **일관된 사용자 경험(UX)** 제공
2. **2024-2025 UI 트렌드**(Glassmorphism 2.0, Aurora UI, Ambient Glow)에 부합하는 세련된 인터페이스 구현
3. **데이터 가독성**과 **시각적 아름다움**의 균형 달성

---

## 2. 디자인 시스템 (Liquid Glass 2.0)

### 2.1 핵심 원칙

| 원칙 | 설명 | 구현 방법 |
|------|------|----------|
| **Glass Material** | 반투명 + 블러로 깊이감 | `GlassSurface(alpha=0.3~0.4)`, `blurRadius=20dp` |
| **Ambient Glow** | 중요 데이터를 빛으로 강조 | 네온 컬러 Border Gradient, Glow Shadow |
| **Floating Layers** | 콘텐츠 계층 분리 | 서로 다른 alpha 값으로 레이어 구분 |
| **Soft Gradients** | 그라데이션으로 부드러운 전환 | `Brush.linearGradient()`, `animatedColor` |
| **Micro-interactions** | 상호작용 시 섬세한 피드백 | `animateFloatAsState`, `HapticFeedback` |

### 2.2 컬러 시스템 (Vivid Neon Palette)

#### 데이터 시각화 컬러
```kotlin
object ReportColors {
    // Risk Level Colors (Neon Gradient)
    val RiskLow = Color(0xFF00E676)        // Neon Green
    val RiskMedium = Color(0xFFFFAB00)     // Amber Glow
    val RiskHigh = Color(0xFFFF5252)       // Coral Red
    
    // Chart Colors (Aurora)
    val ChartPrimary = Color(0xFF7C4DFF)   // Deep Purple
    val ChartSecondary = Color(0xFF18FFFF) // Cyan Glow
    val ChartTertiary = Color(0xFFFF4081)  // Pink Accent
    
    // Glass Tints (Overlay)
    val GlassTintLight = Color(0x33FFFFFF) // White 20%
    val GlassTintDark = Color(0x33000000)  // Black 20%
    val GlassBorder = Color(0x66FFFFFF)    // White 40% (Border Glow)
}
```

#### 상태별 배경 그라데이션
```kotlin
// 위험 단계별 Ambient Glow
fun riskGradient(level: RiskLevel) = when (level) {
    RECOVERY -> Brush.linearGradient(
        colors = listOf(Color(0x1A00E676), Color(0x0D00E676))
    )
    WARNING -> Brush.linearGradient(
        colors = listOf(Color(0x1AFFAB00), Color(0x0DFFAB00))
    )
    HIGH_RISK -> Brush.linearGradient(
        colors = listOf(Color(0x1AFF5252), Color(0x0DFF5252))
    )
}
```

### 2.3 타이포그래피 계층

| 레벨 | 용도 | 스타일 | 색상 |
|------|------|--------|------|
| **Display** | 큰 숫자(점수, %) | `displayMedium` + Bold | `onSurface` 100% |
| **Title** | 섹션 제목 | `titleMedium` + Bold | `onSurface` 100% |
| **Body** | 설명 텍스트 | `bodyMedium` | `onSurfaceVariant` 80% |
| **Label** | 보조 정보 | `labelSmall` | `onSurfaceVariant` 60% |

---

## 3. 가독성 및 시인성 전략 (Visibility Strategy)

### 3.1 레이어별 투명도 규칙

| 레이어 유형 | Alpha | Blur | 용도 |
|------------|-------|------|------|
| **Background Scaffold** | - | - | 전체 배경 (Mesh Gradient) |
| **Primary Card** | 0.35 | 20dp | 주요 정보 카드 |
| **Secondary Card** | 0.25 | 16dp | 부가 정보 카드 |
| **Nested Element** | 0.15 | 12dp | 카드 내부 섹션 |
| **Modal Dialog** | 0.92 | 24dp | 팝업 (높은 불투명도) |
| **Scrim** | 0.7 | - | 팝업 배경 딤 처리 |

### 3.2 팝업/다이얼로그 시인성 보장

```kotlin
// GlassDialog - 높은 불투명도로 배경 차단
@Composable
fun GlassDialog(
    content: @Composable () -> Unit
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Strong Scrim (배경 확실히 어둡게)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(enabled = true) { /* dismiss */ }
        )
        
        // High Opacity Glass Surface
        GlassSurface(
            alpha = 0.92f,  // 거의 불투명 (가독성 최우선)
            blurRadius = 24.dp,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
```

### 3.3 다크/라이트 모드 대응

```kotlin
// Dynamic Glass Tint (모드별 자동 조정)
val glassTint = if (isSystemInDarkTheme()) {
    GlassTintDark.copy(alpha = 0.35f)
} else {
    GlassTintLight.copy(alpha = 0.4f)
}
```

---

## 4. 컴포넌트별 리디자인 명세

### 4.1 DetoxyRiskCard (위험 지수 카드) ⭐ Priority 1

**현재**: Material `Card` + `LinearProgressIndicator`  
**목표**: Glass Gauge with Neon Glow

```
┌─────────────────────────────────────────────────┐
│ ⚠️ 디톡시 위험 지수                              │
│                                                 │
│         ╭──────────────────────╮                │
│        │  ███████░░░░░░░░░░░  │ ← Arc Gauge    │
│        │       42 / 100        │                │
│         ╰──────────────────────╯                │
│                                                 │
│    ┌──────────────┐                            │
│    │  주의 필요   │ ← Risk Level Badge (Glow) │
│    └──────────────┘                            │
│                                                 │
│  실패율          35%    ░░░░░░░░░░░░░          │
│  포기 시점       28점   ░░░░░░░░░░░░░          │
│  차단 빈도       45점   ░░░░░░░░░░░░░          │
└─────────────────────────────────────────────────┘
```

**구현 포인트**:
- [ ] `Card` → `GlassSurface(alpha=0.35f)` 교체
- [ ] `LinearProgressIndicator` → Custom Arc Gauge (Canvas)
- [ ] Risk Level Badge에 `Glow` 효과 (Shadow + Border Gradient)
- [ ] 세부 지표를 Mini Glass Bar로 표현

### 4.2 RecoveryTrendCard (회복률 추세 카드) ⭐ Priority 1

**현재**: Material `Card` + Basic Line Chart  
**목표**: Glass Chart with Aurora Gradient Line

```
┌─────────────────────────────────────────────────┐
│ ⭐ 회복률 추세                                   │
│                                                 │
│    78%  ↗️ +12%                                 │
│    "회복률이 개선되고 있어요!"                   │
│                                                 │
│    ╭─────────────────────────────────╮          │
│    │        ╭────╮                   │          │
│    │   ╭───╯    ╰──────╮            │ ← Glow  │
│    │  ╯                 ╰───╮        │          │
│    │                         ╰──     │          │
│    ╰─────────────────────────────────╯          │
│      월   화   수   목   금   토   일           │
└─────────────────────────────────────────────────┘
```

**구현 포인트**:
- [ ] `Card` → `GlassSurface(alpha=0.35f)` 교체
- [ ] 차트 라인에 Gradient 적용 (`Brush.linearGradient`)
- [ ] 라인 주변 Glow 효과 (`drawPath` with `BlendMode.Screen`)
- [ ] 그리드 라인 제거 또는 극히 미세하게 (alpha 0.1)
- [ ] 데이터 포인트에 Pulse 애니메이션 (옵션)

### 4.3 DistractionTopCard (방해요인 Top 3) ⭐ Priority 2

**현재**: Material `Card` + 숫자 뱃지 + `LinearProgressIndicator`  
**목표**: Floating Glass Panels

```
┌─────────────────────────────────────────────────┐
│ ⚡ 방해요인 Top 3                                │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 1️⃣  SNS (소셜 미디어)           48%    │   │
│  │     ████████████████░░░░░░░░░░░░░░░   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 2️⃣  메신저                      32%    │   │
│  │     ██████████░░░░░░░░░░░░░░░░░░░░░░   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 3️⃣  웹 브라우저                 20%    │   │
│  │     ██████░░░░░░░░░░░░░░░░░░░░░░░░░░   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ─────────────────────────────────────────     │
│  총 차단 횟수                         127회    │
└─────────────────────────────────────────────────┘
```

**구현 포인트**:
- [ ] 외부 `Card` → `GlassSurface(alpha=0.35f)`
- [ ] 각 항목을 개별 `GlassSurface(alpha=0.15f)` 패널로 분리
- [ ] Rank Badge에 Neon Color + 그림자 적용
- [ ] 프로그레스 바를 Rounded Gradient Bar로 교체

### 4.4 CoachRecommendationCard (코치 추천 카드) ⭐ Priority 2

**현재**: Material `Card` + `Surface` (배경색 적용)  
**목표**: Spotlight Effect + Glass Action Items

```
┌─────────────────────────────────────────────────┐
│ 🧑‍🏫 디톡시 코치 추천                            │
│                                                 │
│  ╭───────────────────────────────────────────╮  │
│  │  💡 "조금 더 노력하면 회복 단계에 진입해요!" │  │ ← Spotlight
│  │      현재 집중 패턴을 분석했어요.          │  │    Glow BG
│  ╰───────────────────────────────────────────╯  │
│                                                 │
│  추천 행동                                      │
│  ┌────────────────────────────────────────┐    │
│  │ ✅ 점심 후 15분 집중 세션 추가          │    │
│  │ ✅ SNS 알림 끄기                        │    │
│  │ ✅ 취침 1시간 전 디톡시 타임 설정       │    │
│  └────────────────────────────────────────┘    │
│                                        [전체 보기]
└─────────────────────────────────────────────────┘
```

**구현 포인트**:
- [ ] 외부 `Card` → `GlassSurface(alpha=0.35f)`
- [ ] 코치 메시지 영역에 Ambient Glow 배경 (Risk Level별 컬러)
- [ ] 행동 제안 리스트를 Nested Glass Panel로 그룹화
- [ ] 체크 아이콘에 완료 시 Pulse 애니메이션

### 4.5 CoachRecommendationDialog (코치 상세 다이얼로그) ⭐ Priority 1

**현재**: 기본 `Dialog` (시인성 문제 우려)  
**목표**: `GlassDialog` (높은 불투명도 + Strong Scrim)

**구현 포인트**:
- [ ] 배경 Scrim: `Color.Black.copy(alpha=0.7f)`
- [ ] Dialog Surface: `GlassSurface(alpha=0.92f)` (거의 불투명)
- [ ] 내부 콘텐츠는 기존 스타일 유지 (가독성 최우선)
- [ ] 닫기 버튼에 Glass Style 적용

### 4.6 DistractionAvoidanceCard (포기 지점 분석) ⭐ Priority 3

**구현 포인트**:
- [ ] `Card` → `GlassSurface(alpha=0.35f)`
- [ ] 분석 텍스트 가독성 강화 (높은 대비)
- [ ] 인사이트 아이콘에 Glow 효과

### 4.7 AllowedAppDwellCard (허용 앱 체류 시간) ⭐ Priority 3

**구현 포인트**:
- [ ] `Card` → `GlassSurface(alpha=0.35f)`
- [ ] 프로그레스 바 → Rounded Gradient Bar
- [ ] 앱 아이콘 영역에 Glass Circle 배경

### 4.8 EmptyStateCard (빈 상태 카드) ⭐ Priority 3

**구현 포인트**:
- [ ] `Card` → `GlassSurface(alpha=0.3f)`
- [ ] 아이콘 배경에 Pulse Glow 애니메이션
- [ ] CTA 버튼에 Gradient Border 적용

---

## 5. 공통 컴포넌트 제작

### 5.1 필요한 신규 컴포넌트

| 컴포넌트 | 용도 | 우선순위 |
|----------|------|----------|
| `GlassCard` | 기존 Card 대체용 래퍼 | P0 |
| `GlassDialog` | 다이얼로그 래퍼 | P0 |
| `GlowText` | 강조 텍스트 (그림자 + 발광) | P1 |
| `GradientProgressBar` | 그라데이션 프로그레스 | P1 |
| `ArcGauge` | 원형 게이지 (Canvas) | P1 |
| `GlowBadge` | 발광 효과 뱃지 | P2 |
| `AnimatedLineChart` | 애니메이션 라인 차트 | P2 |

### 5.2 GlassCard 구현 예시

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.35f,
    glowColor: Color? = null,  // Risk Level별 Glow
    content: @Composable ColumnScope.() -> Unit
) {
    val borderGradient = glowColor?.let {
        Brush.linearGradient(
            colors = listOf(it.copy(alpha = 0.5f), it.copy(alpha = 0.1f))
        )
    }
    
    GlassSurface(
        modifier = modifier
            .then(
                if (borderGradient != null) {
                    Modifier.border(1.dp, borderGradient, RoundedCornerShape(24.dp))
                } else Modifier
            ),
        alpha = alpha
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}
```

---

## 6. 작업 단계 (Phased Approach)

### Phase 1: 기반 구축 (1~2일)
- [ ] `GlassCard` 공통 컴포넌트 제작
- [ ] `GlassDialog` 공통 컴포넌트 제작  
- [ ] `GradientProgressBar` 제작
- [ ] 컬러 팔레트 (`ReportColors`) 정의

### Phase 2: 핵심 카드 글래스화 (2~3일)
- [ ] `DetoxyRiskCard` 리팩토링
- [ ] `RecoveryTrendCard` 리팩토링
- [ ] `CoachRecommendationDialog` → `GlassDialog` 적용

### Phase 3: 보조 카드 글래스화 (1~2일)
- [ ] `DistractionTopCard` 리팩토링
- [ ] `CoachRecommendationCard` 리팩토링

### Phase 4: 나머지 카드 및 폴리싱 (1~2일)
- [ ] `DistractionAvoidanceCard`, `AllowedAppDwellCard` 리팩토링
- [ ] `EmptyStateCard` 리팩토링
- [ ] 전체 여백/폰트/애니메이션 점검

### Phase 5: QA 및 최적화 (1일)
- [ ] 다크 모드 / 라이트 모드 테스트
- [ ] 저사양 기기 블러 Fallback 테스트
- [ ] 스크롤 성능 최적화

---

## 7. 기대 효과

| 항목 | Before | After |
|------|--------|-------|
| **디자인 일관성** | 50% (Glass + Material 혼재) | 100% (Liquid Glass 통일) |
| **트렌드 적합성** | 2022 (Basic Material 3) | 2024 (Glassmorphism 2.0) |
| **브랜드 인상** | 일반 유틸리티 앱 | 프리미엄 웰니스 앱 |
| **데이터 가독성** | 보통 (밋밋한 표현) | 높음 (Glow로 강조) |

---

## 8. 참고 디자인 레퍼런스

1. **iOS 18 Control Center** - Glassmorphism 2.0의 표준
2. **Samsung One UI 6** - 반투명 패널 + Soft Glow
3. **Spotify Wrapped 2024** - Vivid Data Visualization
4. **Apple Health** - Ambient Color Coding for Risk Levels

---

## 9. 주의사항 및 Fallback

### 9.1 블러 미지원 기기 대응
```kotlin
// 블러가 지원되지 않는 기기에서는 높은 alpha로 대체
val effectiveAlpha = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
    0.85f  // 블러 없이 높은 불투명도로 대체
} else {
    0.35f  // 블러 적용 시 낮은 불투명도
}
```

### 9.2 성능 고려사항
- `graphicsLayer()` 적용 시 하드웨어 가속 활성화
- 대량 리스트에서 `GlassSurface` 중첩 피하기
- 애니메이션은 `remember`로 상태 캐싱

---

**문서 버전**: v2.0  
**최종 수정**: 2025-12-11  
**작성자**: AI Assistant (Claude)
