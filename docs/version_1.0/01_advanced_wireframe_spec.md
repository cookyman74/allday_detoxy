# 1차 고도화 UI 와이어프레임 스펙 (텍스트 기반)

## 개요
- **기준 문서**: [01_advanced_prd.md](./01_advanced_prd.md)
- **작성 일자**: 2025-10-12
- **설계 원칙**: Material3 디자인, Jetpack Compose, 기존 MVP UI와 일관성 유지

---

## 1. 디톡시 제어 설정 화면 (DetoxyControlSettingsScreen)

### 1.1 화면 진입
- **엔트리 포인트**: 
  - 타이머 화면 상단 우측 설정 아이콘 (⚙️)
  - 또는 하단 네비게이션 "설정" 탭 → "디톡시 제어 설정"
- **네비게이션**: `TimerScreen` → `DetoxyControlSettingsScreen`
- **PRD 참조**: [4.1 디톡시 제어 설정 화면](./01_advanced_prd.md#41-디톡시-제어-설정-화면)

### 1.2 레이아웃 구조

```
┌─────────────────────────────────────┐
│  ← 디톡시 제어 설정          [저장] │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  ╔═══════════════════════════════╗ │  ← 섹션 1: 프리셋 선택
│  ║  디톡시 강도 프리셋            ║ │
│  ╠═══════════════════════════════╣ │
│  ║  [완전 차단] [표준] [완화]    ║ │
│  ║  ━━━━━━━                      ║ │
│  ║  선택: 완전 차단              ║ │
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
    items = ["완전 차단", "표준 디톡시", "완화"]
    selectedIndex = 0
    onSelectionChange = { preset -> 
        // 해당 프리셋에 맞게 카테고리 자동 설정
    }
}
```

**프리셋 정의** (PRD §4.1 기준):
| 프리셋 | SNS | 메신저 | Web | 영상 | 기타 | 설명 |
|--------|-----|--------|-----|------|------|------|
| 완전 차단 | ON | OFF | ON | ON | ON | 중독 진단: 고위험 사용자 권장 |
| 표준 디톡시 | ON | OFF | OFF | ON | OFF | 회복 단계: 일반 사용자 권장 |
| 완화 | OFF | OFF | OFF | ON | OFF | 유지 단계: 저위험 사용자 권장 |

**메신저 카테고리 특이사항**:
- 기본값 OFF (긴급 연락 유지)
- 사용자가 명시적으로 차단 선택 가능
- 첫 활성화 시 안내 다이얼로그 표시

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
- **디톡시 회복 관점 메시지**:
  - "완전 차단: SNS, Web, 영상, 기타 앱 차단 중. 디톡시 회복에 집중하세요." (완전 차단)
  - "표준 디톡시: SNS와 영상 앱 차단 중. 균형 잡힌 디지털 습관을 만들어가세요." (표준)
  - "완화 모드: 영상 앱만 차단 중. 지금까지 잘 유지하고 있어요!" (완화)
  - "메신저는 긴급 연락을 위해 사용 가능합니다."

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
│  ║  📊 디톡시 위험 지수          ║ │
│  ╠═══════════════════════════════╣ │
│  ║     ┌──────────────┐          ║ │
│  ║     │   [  32  ]   │  안정    ║ │  ← 원형 게이지
│  ║     │  ━━━━━━━━   │          ║ │
│  ║     │   /100       │          ║ │
│  ║     └──────────────┘          ║ │
│  ║  디지털 중독 위험도가 낮아요! 💪 ║ │
│  ║  회복이 잘 진행되고 있습니다.  ║ │
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
│  ╔═══════════════════════════════╗ │  ← 신규: 디톡시 코치 추천
│  ║  💡 디톡시 코치의 조언         ║ │
│  ╠═══════════════════════════════╣ │
│  ║  "Instagram 차단 시도가 늘고  ║ │
│  ║   있어요. 디톡시 타이머를      ║ │
│  ║   10분 더 늘려 회복 속도를     ║ │
│  ║   높여보는 건 어떨까요?"       ║ │
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

**위험 지수 단계** (중독 진단 관점):
| 점수 | 단계 | 색상 | 메시지 |
|------|------|------|--------|
| 0-30 | 회복 | 🟢 | "디지털 중독 위험도가 낮아요! 회복이 잘 진행되고 있습니다. 💪" |
| 31-60 | 주의 | 🟡 | "중독 위험도가 상승 중입니다. 디톡시 강도를 '완전 차단'으로 높여보세요." |
| 61-100 | 고위험 | 🔴 | "중독 고위험! 디톡시 타이머가 크게 줄었어요. 긴급 개입이 필요합니다." |

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

**추천 메시지 예시** (위험 지수 기반, 중독/회복 관점):
- 회복 (0-30): "디지털 웰빙 회복이 순조로워요! 지금처럼만 유지하세요. 🎉"
- 주의 (31-60): "[앱명] 차단 시도가 증가 중입니다. 디톡시 타이머를 10분 더 늘려 회복 속도를 높여보세요."
- 고위험 (61-100): "중독 재발 위험! 이번 주 디톡시 시간이 50% 감소했어요. 긴급히 '완전 차단' 모드로 전환하세요."

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
        title = { Text("💬 메신저 차단 안내") },
        text = {
            Column {
                Text("메신저 앱은 긴급 연락이 필요할 수 있어 기본적으로 차단하지 않습니다.")
                Spacer(height = 8.dp)
                Text("디지털 중독 회복을 위해 완전한 디톡시가 필요하다면 메신저도 차단할 수 있습니다.")
                Spacer(height = 8.dp)
                Text("⚠️ 긴급 연락이 불가능해질 수 있으니 신중히 선택하세요.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("메신저도 차단")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("허용 유지 (권장)")
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
1. ✅ DetoxyControlSettingsScreen 레이아웃
2. ✅ 프리셋 선택 UI (완전 차단/표준 디톡시/완화)
3. ✅ 카테고리 토글 카드
4. ✅ 프리뷰 문구 (회복 관점 메시지)
5. ✅ 권한 상태 카드
6. ✅ 메신저 차단 안내 다이얼로그

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

