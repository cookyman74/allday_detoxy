# 1차 고도화 UI 와이어프레임 스펙 (텍스트 기반)

## 개요
- **기준 문서**: [01_advanced_prd.md](./01_advanced_prd.md)
- **작성 일자**: 2025-10-12
- **설계 원칙**: Material3 디자인, Jetpack Compose, 기존 MVP UI와 일관성 유지

---

## 1. 집중모드 설정 화면 (FocusModeSettingsScreen)

### 1.1 화면 진입
- **엔트리 포인트**: 
  - 타이머 화면 상단 우측 설정 아이콘 (⚙️)
  - 또는 하단 네비게이션 "설정" 탭 → "집중모드 설정"
- **네비게이션**: `TimerScreen` → `FocusModeSettingsScreen`

### 1.2 레이아웃 구조

```
┌─────────────────────────────────────┐
│  ← 집중모드 설정              [저장] │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  ╔═══════════════════════════════╗ │  ← 섹션 1: 프리셋 선택
│  ║  차단 강도 프리셋              ║ │
│  ╠═══════════════════════════════╣ │
│  ║  [전체 차단] [집중] [완화]    ║ │
│  ║  ━━━━━━━                      ║ │
│  ║  선택: 전체 차단              ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 섹션 2: 카테고리별 토글
│  ║  차단 앱 카테고리              ║ │
│  ╠═══════════════════════════════╣ │
│  ║  📱 SNS             [🔴 ON]  ║ │  ← SNS 카테고리
│  ║    Instagram, Facebook...     ║ │
│  ║                               ║ │
│  ║  💬 메신저          [⚪ OFF] ║ │  ← 메신저 (기본 OFF)
│  ║    💡 긴급 연락 유지 권장     ║ │
│  ║    KakaoTalk, WhatsApp...     ║ │
│  ║                               ║ │
│  ║  🌐 Web 서핑       [🔴 ON]  ║ │  ← Web
│  ║    Chrome, Samsung Internet   ║ │
│  ║                               ║ │
│  ║  🎬 영상 & 쇼츠    [🔴 ON]  ║ │  ← 영상
│  ║    YouTube, TikTok...         ║ │
│  ║                               ║ │
│  ║  📦 기타 앱        [🔴 ON]  ║ │  ← 기타
│  ║    기본 차단 앱 이외 모든 앱 ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 섹션 3: 프리뷰
│  ║  현재 설정 요약                ║ │
│  ╠═══════════════════════════════╣ │
│  ║  "SNS, Web, 영상, 기타 앱을   ║ │
│  ║   차단합니다."                ║ │
│  ║  "메신저는 사용 가능합니다."  ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 섹션 4: 권한 상태
│  ║  권한 상태                     ║ │
│  ╠═══════════════════════════════╣ │
│  ║  ✅ 앱 차단 기능 활성화됨     ║ │  ← 접근성 권한
│  ║  ✅ 잠금 화면 표시 허용됨     ║ │  ← 오버레이 권한
│  ║  ⚪ 알림 차단 (선택)          ║ │  ← DND 권한
│  ║     [설정하기]                ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
└─────────────────────────────────────┘
```

### 1.3 컴포넌트 상세

#### 프리셋 버튼 (SegmentedButton)
```kotlin
// 3개 선택형 버튼
SegmentedButton {
    items = ["전체 차단", "집중", "완화"]
    selectedIndex = 0
    onSelectionChange = { preset -> 
        // 해당 프리셋에 맞게 카테고리 자동 설정
    }
}
```

**프리셋 정의**:
| 프리셋 | SNS | 메신저 | Web | 영상 | 기타 |
|--------|-----|--------|-----|------|------|
| 전체 차단 | ON | OFF | ON | ON | ON |
| 집중 | ON | OFF | OFF | ON | OFF |
| 완화 | OFF | OFF | OFF | ON | OFF |

#### 카테고리 토글 카드
```kotlin
@Composable
fun CategoryToggleCard(
    icon: String,
    title: String,
    description: String,
    isEnabled: Boolean,
    showWarning: Boolean = false,
    warningText: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Card {
        Row {
            Icon(icon) // 📱 💬 🌐 🎬 📦
            Column {
                Text(title, style = titleMedium)
                Text(description, style = bodySmall)
                if (showWarning) {
                    Row {
                        Icon(💡)
                        Text(warningText)
                    }
                }
            }
            Switch(checked = isEnabled, onCheckedChange = onToggle)
        }
    }
}
```

#### 프리뷰 문구
- 동적 생성: 선택된 카테고리에 따라 문구 자동 업데이트
- 예시:
  - "모든 앱을 차단합니다." (전체 차단)
  - "SNS와 영상 앱만 차단합니다." (집중)
  - "영상 앱만 차단합니다." (완화)

#### 권한 상태 카드
```kotlin
@Composable
fun PermissionStatusCard(
    title: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onSettingsClick: () -> Unit
) {
    Card {
        Row {
            Icon(if (isGranted) ✅ else ⚪)
            Text(title)
            if (!isGranted) {
                Button("설정하기", onClick = onSettingsClick)
            }
        }
    }
}
```

---

## 2. 리포트 고도화 화면 (ReportScreen - Enhanced)

### 2.1 기존 구조 확장
- **기존**: 일간 통계 카드 (성공 횟수, 총 집중 시간, 스트릭, 포인트)
- **추가**: 고급 지표 카드, 주간 인사이트, 방해요인 분석

### 2.2 레이아웃 구조

```
┌─────────────────────────────────────┐
│  리포트              [일간] [주간]  │  ← TopAppBar + 탭
├─────────────────────────────────────┤
│                                     │
│  ╔═══════════════════════════════╗ │  ← 일간 요약 카드 (기존)
│  ║  오늘의 집중                   ║ │
│  ╠═══════════════════════════════╣ │
│  ║  ✅ 성공: 3회                 ║ │
│  ║  ⏱️ 총 집중: 75분             ║ │
│  ║  🔥 스트릭: 5일               ║ │
│  ║  ⭐ 포인트: 120 (+25)         ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 신규: Detoxy 위험 지수
│  ║  📊 Detoxy 위험 지수          ║ │
│  ╠═══════════════════════════════╣ │
│  ║     ┌──────────────┐          ║ │
│  ║     │   [  32  ]   │  안정    ║ │  ← 원형 게이지
│  ║     │  ━━━━━━━━   │          ║ │
│  ║     │   /100       │          ║ │
│  ║     └──────────────┘          ║ │
│  ║  집중 습관이 안정적입니다! 💪 ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 신규: 회복률 추세
│  ║  📈 회복률 추세 (7일)         ║ │
│  ╠═══════════════════════════════╣ │
│  ║    65% → 72% (+7%p) ⬆️       ║ │
│  ║  ▅▃▄▆▇██  (미니 그래프)      ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 신규: 방해요인 Top 3
│  ║  🚫 방해요인 Top 3            ║ │
│  ╠═══════════════════════════════╣ │
│  ║  1. Instagram    8회 차단    ℹ️ │
│  ║  2. YouTube      5회 차단     ║ │
│  ║  3. Chrome       3회 차단     ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 신규: Detoxy 코치 추천
│  ║  💡 코치의 한마디              ║ │
│  ╠═══════════════════════════════╣ │
│  ║  "Instagram 사용이 늘고       ║ │
│  ║   있어요. 집중 시간을          ║ │
│  ║   10분 더 늘려보는 건 어때요?" ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 주간 인사이트 (주간 탭)
│  ║  📊 주간 회복률 그래프         ║ │
│  ╠═══════════════════════════════╣ │
│  ║  100%┤                        ║ │
│  ║   80%┤    ●───●               ║ │
│  ║   60%┤  ●       ●─●           ║ │
│  ║   40%┤●               ●       ║ │
│  ║      └────────────────        ║ │
│  ║      월 화 수 목 금 토 일     ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
│  ╔═══════════════════════════════╗ │  ← 주간 총 회복 시간
│  ║  📊 주간 총 회복 시간          ║ │
│  ╠═══════════════════════════════╣ │
│  ║  300분┤                       ║ │
│  ║  200분┤  ▇                    ║ │
│  ║  100분┤▅ █ ▇ ▅ ▆ ▄ ▇         ║ │
│  ║      └────────────────        ║ │
│  ║      월 화 수 목 금 토 일     ║ │
│  ╚═══════════════════════════════╝ │
│                                     │
└─────────────────────────────────────┘
```

### 2.3 컴포넌트 상세

#### Detoxy 위험 지수 카드
```kotlin
@Composable
fun DetoxyRiskIndexCard(riskIndex: Int) {
    Card {
        Column {
            Text("📊 Detoxy 위험 지수")
            
            // 원형 게이지 (Canvas 또는 CircularProgressIndicator)
            CircularProgressIndicator(
                progress = riskIndex / 100f,
                color = getRiskColor(riskIndex) // 0-30: 녹색, 31-60: 노란색, 61+: 빨간색
            )
            
            Text(
                text = getRiskLevel(riskIndex), // "안정" / "주의" / "고위험"
                style = titleLarge
            )
            
            Text(
                text = getRiskMessage(riskIndex),
                style = bodySmall
            )
        }
    }
}
```

**위험 지수 단계**:
| 점수 | 단계 | 색상 | 메시지 |
|------|------|------|--------|
| 0-30 | 안정 | 🟢 | "집중 습관이 안정적입니다! 💪" |
| 31-60 | 주의 | 🟡 | "집중력이 조금 흔들리고 있어요. 차단 설정을 강화해보세요." |
| 61-100 | 고위험 | 🔴 | "위험! 집중 시간이 크게 줄었어요. 긴급 개입이 필요합니다." |

#### 방해요인 Top 3 카드
```kotlin
@Composable
fun DistractionTopCard(distractions: List<DistractionItem>) {
    Card {
        Column {
            Text("🚫 방해요인 Top 3")
            
            distractions.take(3).forEachIndexed { index, item ->
                Row {
                    Text("${index + 1}.")
                    Icon(getAppIcon(item.packageName))
                    Text(item.appName)
                    Text("${item.blockCount}회 차단")
                    
                    // Tooltip 아이콘
                    IconButton(onClick = { showTooltip(item) }) {
                        Icon(Icons.Default.Info)
                    }
                }
            }
        }
    }
}
```

#### Detoxy 코치 추천 카드
```kotlin
@Composable
fun CoachRecommendationCard(recommendation: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row {
            Icon("💡")
            Column {
                Text("코치의 한마디", style = titleMedium)
                Text(recommendation, style = bodyMedium)
            }
        }
    }
}
```

**추천 메시지 예시** (위험 지수 기반):
- 안정: "잘하고 있어요! 이 습관을 유지하세요."
- 주의: "[앱명] 사용이 늘고 있어요. 집중 시간을 10분 더 늘려보는 건 어때요?"
- 고위험: "위험! 이번 주 집중 시간이 50% 감소했어요. 차단 설정을 '전체 차단'으로 변경해보세요."

#### 주간 그래프 (Compose Canvas 기반)
```kotlin
@Composable
fun WeeklyChart(
    title: String,
    data: List<Float>, // 7일치 데이터
    unit: String = "%"
) {
    Card {
        Column {
            Text(title)
            
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                // 선 그래프 또는 막대 그래프 그리기
                drawLineChart(data)
            }
            
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEach {
                    Text(it, style = labelSmall)
                }
            }
        }
    }
}
```

---

## 3. 빈 상태 처리

### 3.1 데이터 없음 상태
```kotlin
@Composable
fun EmptyStateCard(message: String) {
    Card {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                style = bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "집중 세션을 시작하면 데이터가 표시됩니다.",
                style = bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 3.2 주요 빈 상태 메시지
- 리포트: "아직 집중 기록이 없어요"
- 방해요인: "차단 이벤트가 없어요"
- 주간 그래프: "최소 3일치 데이터가 필요해요"

---

## 4. 다이얼로그

### 4.1 메신저 카테고리 안내 다이얼로그
```kotlin
@Composable
fun MessengerCategoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💬 메신저 카테고리") },
        text = {
            Column {
                Text("메신저 앱은 긴급 연락이 필요할 수 있어 기본적으로 차단하지 않습니다.")
                Spacer(height = 8.dp)
                Text("완전한 집중이 필요하다면 메신저도 차단할 수 있습니다.")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("메신저도 차단")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("허용 유지")
            }
        }
    )
}
```

---

## 5. 색상 및 스타일 가이드

### 5.1 색상 정의
```kotlin
// 위험 지수 색상
val RiskLow = Color(0xFF4CAF50)      // 녹색
val RiskMedium = Color(0xFFFF9800)   // 주황색
val RiskHigh = Color(0xFFF44336)     // 빨간색

// 카테고리 아이콘 색상
val CategorySNS = Color(0xFFE91E63)      // 핑크
val CategoryMessenger = Color(0xFF2196F3) // 파란색
val CategoryWeb = Color(0xFF9C27B0)      // 보라색
val CategoryVideo = Color(0xFFFF5722)    // 주황-빨강
val CategoryOther = Color(0xFF607D8B)    // 회색
```

### 5.2 타이포그래피
```kotlin
titleLarge   → 카드 제목 (24sp, Bold)
titleMedium  → 섹션 제목 (20sp, SemiBold)
bodyLarge    → 주요 텍스트 (16sp, Regular)
bodyMedium   → 설명 텍스트 (14sp, Regular)
bodySmall    → 보조 텍스트 (12sp, Regular)
labelSmall   → 라벨/캡션 (11sp, Medium)
```

---

## 6. 접근성 (Accessibility)

### 6.1 대비율
- 텍스트-배경 대비율 4.5:1 이상 (WCAG AA 준수)
- 아이콘-배경 대비율 3:1 이상

### 6.2 ContentDescription
- 모든 아이콘에 명확한 설명 추가
- 예: "SNS 카테고리 토글", "위험 지수 정보"

### 6.3 터치 영역
- 최소 48dp × 48dp 터치 영역 보장
- 토글 스위치, 버튼 모두 충분한 크기

---

## 7. 구현 우선순위

### Phase 1 (Week 1)
1. ✅ FocusModeSettingsScreen 레이아웃
2. ✅ 프리셋 선택 UI
3. ✅ 카테고리 토글 카드
4. ✅ 프리뷰 문구
5. ✅ 권한 상태 카드

### Phase 2 (Week 2A)
1. ✅ Room 마이그레이션
2. ✅ 기본 통계 계산
3. ✅ 빈 상태 처리

### Phase 3 (Week 2B)
1. ✅ Detoxy 위험 지수 UI
2. ✅ 방해요인 Top 3 카드
3. ✅ Detoxy 코치 추천
4. ✅ 주간 그래프 2종
5. ✅ 다이얼로그

---

## 8. 참고 자료
- [Material3 Design](https://m3.material.io/)
- [Jetpack Compose Samples](https://github.com/android/compose-samples)
- [PRD 문서](./01_advanced_prd.md)
- [기존 MVP UI](../working_history/2025-10-11_3.3.md)

---

**최종 업데이트**: 2025-10-12

