# 스케줄 그룹 버튼 역할 변경 PRD

> **문서 버전**: v1.0  
> **작성일**: 2025-12-10  
> **상태**: 설계 검토 중

---

## 1. 개요

### 1.1 배경

현재 스케줄 그룹과 시간 스케줄의 제어 버튼들이 여러 depth에 분산되어 있어 사용자 경험이 불편합니다.

#### 현재 구조의 문제점

```
스케줄 그룹 리스트
  └── 그룹 카드 [토글: isActive] ← 위치기반에서 자동 변경되어 역할 모호
        └── 시간 클릭 → 시간 스케줄 상세 화면 (Depth +1)
              ├── 자동 실행 제어 카드 [마스터 스위치 + 일시중지]
              └── 개별 시간대 카드 [토글: isEnabled]
```

| 문제점 | 설명 |
|--------|------|
| **Depth 과다** | 자동 실행/일시중지 설정을 위해 여러 화면 진입 필요 |
| **토글 역할 혼란** | 위치기반 스케줄에서 그룹 토글이 자동으로 변경되어 사용자 제어와 충돌 |
| **제어 계층 복잡** | 마스터 스위치 ↔ 그룹 토글 ↔ 개별 시간대 토글 (3단계) |
| **일관성 부족** | 위치기반과 시간기반 스케줄의 제어 방식이 다름 |

### 1.2 목표

1. **접근성 향상**: 스케줄 그룹 리스트에서 바로 모든 제어 가능
2. **역할 명확화**: 활성/비활성과 일시중지의 역할을 명확히 분리
3. **일관성 확보**: 위치기반/시간기반 스케줄에 동일한 제어 방식 제공
4. **사용자 의도 존중**: 위치기반 스케줄에서도 사용자 수동 제어 우선

---

## 2. 기능 설계

### 2.1 UI 방안 비교 및 선택

#### 방안 비교

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **방안 1: 토글 + 아이콘 버튼** | `[🔘 활성] [⏸️]` | 간단한 UI | 일시중지 옵션 접근 불편 |
| **방안 2: 세그먼트 버튼** | `[ 활성 \| 일시중지 \| 비활성 ]` | 모든 상태 한눈에 | 공간 차지, 일시중지 옵션 없음 |
| **방안 3: 토글 + 드롭다운** ✅ | `[🔘 활성 ▾]` | 직관적, 일시중지 옵션 포함 | 롱프레스 학습 필요 |

#### 선택: 방안 3 (토글 + 드롭다운) 🎯

**선택 이유**:
1. **직관성**: 현재 상태가 버튼에 명확히 표시됨
2. **접근성**: 탭으로 빠른 토글, 롱프레스/▾로 상세 옵션 접근
3. **확장성**: 일시중지 기간 옵션을 자연스럽게 포함
4. **일관성**: 위치기반/시간기반 스케줄 모두 동일한 UI

### 2.2 새로운 UI 구조

#### 스케줄 그룹 카드 레이아웃

```
┌─────────────────────────────────────────────────────────────────┐
│  ⭐ 매일 향상                              [🔘 활성 ▾]          │
│  ─────────────────────────────────────────────────────────────  │
│  매일 같은 시간에 집중하는 루틴                                  │
│  ─────────────────────────────────────────────────────────────  │
│  📍 어디서나 적용 (위치 없음)                                    │
│  ─────────────────────────────────────────────────────────────  │
│  📅 시간                                                    >   │
│     • 오후 8:00 - 45분                                          │
│     • 오후 9:00 - 30분                                          │
│     • 오후 10:00 - 45분                                         │
│  ─────────────────────────────────────────────────────────────  │
│                                            ✏️ 수정   🗑️ 삭제    │
└─────────────────────────────────────────────────────────────────┘
```

#### 통합 제어 버튼 상세

```
┌─────────────────────────────────────┐
│  [▶️ 활성 ▾]                        │  ← 탭: 활성↔비활성 토글
│                                     │  ← ▾ 탭 또는 롱프레스: 드롭다운
└─────────────────────────────────────┘
         │
         ▼ (드롭다운 메뉴)
┌─────────────────────────────────────┐
│ ✓ 활성                              │
│ ─────────────────────────────────── │
│   일시중지                           │
│     • 1시간                         │
│     • 2시간                         │
│     • 오늘 하루                      │
│     • 내일까지                       │
│ ─────────────────────────────────── │
│   비활성                             │
└─────────────────────────────────────┘
```

#### 상태별 UI 표시

| 상태 | 버튼 표시 | 카드 배경색 |
|------|----------|------------|
| **활성** | `[▶️ 활성 ▾]` (Primary) | `primaryContainer` |
| **일시중지** | `[⏸️ 일시중지 ▾]` (Warning) | `tertiaryContainer` |
| **비활성** | `[⏹️ 비활성 ▾]` (Neutral) | `surfaceVariant` |

### 2.3 버튼 동작 정의

#### 통합 제어 버튼 (토글 + 드롭다운)

```kotlin
// 버튼 상태
enum class ScheduleGroupControlState {
    ACTIVE,           // 활성 (정상 실행)
    PAUSED,           // 일시중지 (알람 유지, 실행 건너뛰기)
    INACTIVE          // 비활성 (알람 취소, Geofence 해제)
}
```

**탭 동작**:
- 활성 → 비활성 (토글)
- 비활성 → 활성 (토글)
- 일시중지 → 활성 (일시중지 해제)

**롱프레스 또는 ▾ 탭 동작**:
- 드롭다운 메뉴 표시

### 2.4 상태 조합과 동작

#### 상태 전이 다이어그램

```
                    ┌──────────────┐
          ┌────────│    활성      │────────┐
          │        │  (ACTIVE)    │        │
          │        └──────┬───────┘        │
          │               │                │
     [비활성화]      [일시중지]       [시간 경과]
          │               │                │
          ▼               ▼                ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│    비활성    │   │   일시중지   │───│    활성      │
│  (INACTIVE)  │   │   (PAUSED)   │   │  (ACTIVE)    │
└──────────────┘   └──────────────┘   └──────────────┘
          │               │
          └───────────────┘
              [활성화]
```

#### 상태별 시스템 동작

| 상태 | 알람 등록 | Geofence | 시간 도래 시 | 위치 진입 시 |
|------|----------|----------|-------------|-------------|
| **활성** | ✅ 등록됨 | ✅ 등록됨 | 타이머 시작 | 그룹 활성화 |
| **일시중지** | ✅ 유지 | ✅ 유지 | 건너뛰기 (로그 기록) | 건너뛰기 |
| **비활성** | ❌ 취소 | ❌ 해제 | - | - |

---

### 🔧 2.5 위치기반 스케줄에서의 동작

위치기반 스케줄은 시간기반 스케줄과 다른 특수한 동작 방식을 가집니다.

#### 현재 문제점 분석

```
[문제 상황]
사용자가 스케줄 그룹 토글을 OFF로 설정
  → 하지만 위치에 진입하면 자동으로 활성화됨
  → 위치에서 벗어나면 자동으로 비활성화됨
  → 사용자의 수동 제어 의도가 무시됨
```

#### 위치기반 vs 시간기반 스케줄 비교

| 구분 | 시간기반 (어디서나 적용) | 위치기반 |
|------|------------------------|---------|
| **활성화 트리거** | 즉시 (사용자 수동) | 위치 진입 시 자동 |
| **비활성화 트리거** | 사용자 수동 | 위치 이탈 시 자동 |
| **토글 역할** | 직접 제어 | 현재 상태 표시 (자동 변경됨) |
| **Geofence** | 없음 | 등록됨 |

#### 새로운 동작 정의

##### 상태별 위치 진입/이탈 동작

| 사용자 설정 상태 | 위치 진입 시 | 위치 이탈 시 |
|-----------------|-------------|-------------|
| **활성** | 시간표 자동 활성화 → 알람 등록 | 시간표 자동 비활성화 → 알람 취소 |
| **일시중지** | 시간표 활성화되지만 **알람 트리거 안됨** | 시간표 비활성화 |
| **비활성** | **무시** (Geofence 해제됨, 위치 감지 안됨) | - |

##### 상태별 Geofence 동작

```
┌─────────────────────────────────────────────────────────────────┐
│                        활성 상태                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Geofence: ✅ 등록됨                                     │    │
│  │  위치 진입 → 시간표 활성화 → 알람 정상 등록               │    │
│  │  위치 이탈 → 시간표 비활성화 → 알람 취소                  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       일시중지 상태                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Geofence: ✅ 등록됨 (위치 감지는 유지)                   │    │
│  │  위치 진입 → 시간표 활성화 → 알람 등록 BUT 트리거 안됨    │    │
│  │  위치 이탈 → 시간표 비활성화                              │    │
│  │  ⏰ 일시중지 만료 → 자동으로 활성 상태로 전환             │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        비활성 상태                               │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Geofence: ❌ 해제됨 (위치 감지 자체가 안됨)              │    │
│  │  위치 진입 → 무반응 (Geofence가 없으므로)                 │    │
│  │  위치 이탈 → 무반응                                       │    │
│  │  ✋ 사용자가 명시적으로 다시 활성화해야 함                 │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

##### 일시중지 상태에서의 특수 처리

```kotlin
// 일시중지 중 위치 진입 시
if (scheduleGroup.manualOverrideState == "PAUSED") {
    // 시간표는 활성화하되, 알람 트리거는 건너뛰기
    scheduleManager.activateGroup(groupId, skipAlarmTrigger = true)
    
    // 로그 기록
    Log.i(TAG, "⏸️ 일시중지 상태, 시간표 활성화하지만 알람 건너뛰기")
    logAutoRunSkipped(locationId, "SCHEDULE_GROUP_PAUSED")
}
```

##### 비활성 상태에서 다시 활성화할 때

```kotlin
// 사용자가 비활성 → 활성으로 변경할 때
fun changeControlState(groupId: String, newState: ScheduleGroupControlState) {
    when (newState) {
        ScheduleGroupControlState.ACTIVE -> {
            // 1. manualOverride 해제
            repository.updateManualOverride(groupId, null, null)
            
            // 2. 그룹 활성화
            scheduleGroupManager.activateGroup(groupId)
            
            // 3. 위치기반인 경우 Geofence 재등록
            val linkedLocations = repository.getLinkedLocations(groupId)
            linkedLocations.forEach { location ->
                geofenceManager.addGeofence(location)
            }
        }
        // ...
    }
}
```

---

### 2.6 사용자 의도 우선 정책 (manualOverride)

위치기반 스케줄에서 사용자의 수동 제어 의도를 존중하기 위해 `manualOverride` 플래그를 도입합니다.

#### 핵심 원칙

```
사용자가 명시적으로 "비활성" 또는 "일시중지"를 선택한 경우,
위치 진입에 의한 자동 활성화를 차단합니다.
```

#### 데이터 모델

```kotlin
data class ScheduleGroup(
    // ... 기존 필드
    
    /**
     * 사용자 수동 제어 상태
     * 
     * null: 자동 모드 (위치에 따라 자동 활성화/비활성화) - 기본값
     * "INACTIVE": 사용자가 명시적으로 비활성화 → Geofence 해제, 위치 감지 안됨
     * "PAUSED": 사용자가 명시적으로 일시중지 → Geofence 유지, 알람만 건너뛰기
     */
    val manualOverrideState: String? = null,
    
    /**
     * 일시중지 해제 시각
     * 
     * null: 일시중지 아님 또는 무기한
     * timestamp: 해당 시각에 자동으로 일시중지 해제 → 활성 상태로 전환
     */
    val pauseUntil: Long? = null
)
```

#### 위치 진입 시 동작 (GeofenceTransitionsReceiver 수정)

```kotlin
// GeofenceTransitionsReceiver.kt 수정
private fun handleGeofenceTrigger(...) {
    // 1. 위치 정보 조회
    val location = locationDao.getByIdOnce(locationId)
    
    // 2. 연결된 스케줄 그룹 조회
    val scheduleGroup = scheduleGroupDao.getById(location.linkedScheduleGroupId)
    
    // 3. 사용자 수동 제어 상태 확인 ⭐ 핵심 로직
    when (scheduleGroup.manualOverrideState) {
        "INACTIVE" -> {
            // 🚫 비활성 상태: 이 시점에 도달할 수 없음 (Geofence가 해제되어 있으므로)
            // 안전장치로 유지
            Log.w(TAG, "⏹️ 비활성 상태인데 Geofence 트리거됨 (비정상): ${location.label}")
            return
        }
        "PAUSED" -> {
            // ⏸️ 일시중지: 만료 확인
            if (scheduleGroup.pauseUntil != null && 
                System.currentTimeMillis() < scheduleGroup.pauseUntil) {
                Log.i(TAG, "⏸️ 일시중지 상태, 알람 트리거 건너뛰기: ${location.label}")
                
                // 시간표는 활성화하되 알람 트리거는 건너뛰기
                scheduleManager.activateGroup(groupId, skipAlarmTrigger = true)
                logAutoRunSkipped(location.id, "SCHEDULE_GROUP_PAUSED")
                return
            }
            // 일시중지 만료됨 → 정상 진행
            Log.i(TAG, "⏸️ 일시중지 만료됨, 정상 활성화: ${location.label}")
        }
        null -> {
            // ✅ 자동 모드: 정상 활성화
            Log.d(TAG, "✅ 자동 모드, 정상 활성화: ${location.label}")
        }
    }
    
    // 4. 정상 활성화 진행
    scheduleManager.activateGroupByLocation(...)
}
```

#### 상태 전환 시나리오

```
[시나리오 1: 활성 → 비활성]
사용자가 토글 탭 (또는 드롭다운에서 비활성 선택)
  ↓
manualOverrideState = "INACTIVE"
  ↓
Geofence 해제
  ↓
위치에 있어도 스케줄 비활성화
  ↓
✅ 사용자 의도 존중

[시나리오 2: 활성 → 일시중지 (1시간)]
사용자가 드롭다운에서 "1시간" 선택
  ↓
manualOverrideState = "PAUSED"
pauseUntil = 현재시각 + 1시간
  ↓
Geofence 유지 (위치 감지는 계속)
  ↓
위치에 있으면 시간표 활성화되지만 알람 건너뛰기
  ↓
1시간 후 → pauseUntil 만료 → 자동으로 활성 상태
  ↓
✅ 사용자 의도 존중 + 자동 복귀
```

---

## 3. 데이터 모델 변경

### 3.1 ScheduleGroup 엔티티 수정

```kotlin
@Entity(
    tableName = "schedule_group",
    indices = [Index(value = ["isActive"])]
)
data class ScheduleGroup(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    
    // 기존 필드
    val iconType: String = "WORK",
    val colorHex: String = "#4CAF50",
    val lastActivatedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    
    // ==================== v8 추가: 통합 제어 ====================
    
    /**
     * 사용자 수동 제어 상태 (v8+)
     * 
     * null: 자동 모드 (기본값, 기존 동작 유지)
     * "INACTIVE": 사용자가 명시적으로 비활성화
     * "PAUSED": 사용자가 명시적으로 일시중지
     */
    val manualOverrideState: String? = null,
    
    /**
     * 일시중지 해제 시각 (v8+)
     * 
     * null: 일시중지 아님 또는 무기한
     * timestamp: 해당 시각에 자동으로 해제
     */
    val pauseUntil: Long? = null
)
```

### 3.2 Room Migration

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. manualOverrideState 컬럼 추가
        database.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN manualOverrideState TEXT DEFAULT NULL"
        )
        
        // 2. pauseUntil 컬럼 추가
        database.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN pauseUntil INTEGER DEFAULT NULL"
        )
    }
}
```

---

## 4. UI 컴포넌트 설계

### 4.1 ScheduleControlButton 컴포넌트

```kotlin
/**
 * 스케줄 그룹 통합 제어 버튼
 * 
 * 탭: 활성/비활성 토글
 * 롱프레스 또는 ▾ 탭: 드롭다운 메뉴 표시
 */
@Composable
fun ScheduleControlButton(
    state: ScheduleGroupControlState,
    pauseUntil: Long?,
    onStateChange: (ScheduleGroupControlState) -> Unit,
    onPause: (PauseDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    val backgroundColor = when (state) {
        ScheduleGroupControlState.ACTIVE -> MaterialTheme.colorScheme.primary
        ScheduleGroupControlState.PAUSED -> MaterialTheme.colorScheme.tertiary
        ScheduleGroupControlState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (state) {
        ScheduleGroupControlState.ACTIVE -> MaterialTheme.colorScheme.onPrimary
        ScheduleGroupControlState.PAUSED -> MaterialTheme.colorScheme.onTertiary
        ScheduleGroupControlState.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val buttonText = when (state) {
        ScheduleGroupControlState.ACTIVE -> "활성"
        ScheduleGroupControlState.PAUSED -> {
            if (pauseUntil != null) {
                "일시중지 (${formatRemainingTime(pauseUntil)})"
            } else {
                "일시중지"
            }
        }
        ScheduleGroupControlState.INACTIVE -> "비활성"
    }
    
    val icon = when (state) {
        ScheduleGroupControlState.ACTIVE -> Icons.Default.PlayArrow
        ScheduleGroupControlState.PAUSED -> Icons.Default.Pause
        ScheduleGroupControlState.INACTIVE -> Icons.Default.Stop
    }
    
    Box(modifier = modifier) {
        // 메인 버튼
        Surface(
            onClick = {
                // 탭: 활성 ↔ 비활성 토글
                when (state) {
                    ScheduleGroupControlState.ACTIVE -> onStateChange(ScheduleGroupControlState.INACTIVE)
                    ScheduleGroupControlState.INACTIVE -> onStateChange(ScheduleGroupControlState.ACTIVE)
                    ScheduleGroupControlState.PAUSED -> onStateChange(ScheduleGroupControlState.ACTIVE)
                }
            },
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .combinedClickable(
                    onClick = { /* 위에서 처리 */ },
                    onLongClick = { showDropdown = true }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = buttonText,
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "더보기",
                    tint = contentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { showDropdown = true }
                )
            }
        }
        
        // 드롭다운 메뉴
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            // 활성
            DropdownMenuItem(
                text = { Text("활성") },
                leadingIcon = {
                    if (state == ScheduleGroupControlState.ACTIVE) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
                onClick = {
                    onStateChange(ScheduleGroupControlState.ACTIVE)
                    showDropdown = false
                }
            )
            
            HorizontalDivider()
            
            // 일시중지 서브메뉴
            Text(
                text = "일시중지",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            PauseDuration.values().forEach { duration ->
                DropdownMenuItem(
                    text = { Text(duration.displayName) },
                    onClick = {
                        onPause(duration)
                        showDropdown = false
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            
            HorizontalDivider()
            
            // 비활성
            DropdownMenuItem(
                text = { Text("비활성") },
                leadingIcon = {
                    if (state == ScheduleGroupControlState.INACTIVE) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
                onClick = {
                    onStateChange(ScheduleGroupControlState.INACTIVE)
                    showDropdown = false
                }
            )
        }
    }
}

/**
 * 일시중지 기간 옵션
 */
enum class PauseDuration(val displayName: String, val durationMillis: Long) {
    ONE_HOUR("1시간", 60 * 60 * 1000L),
    TWO_HOURS("2시간", 2 * 60 * 60 * 1000L),
    TODAY("오늘 하루", -1L),  // 자정까지 계산 필요
    TOMORROW("내일까지", -2L)  // 내일 자정까지 계산 필요
}
```

### 4.2 수정된 ScheduleGroupCard

```kotlin
@Composable
fun ScheduleGroupCard(
    group: ScheduleGroup,
    controlState: ScheduleGroupControlState,  // 🆕 통합 상태
    timeBasedAutoRuns: List<TimeBasedAutoRun>,
    linkedLocations: List<LocationBasedAutoRun> = emptyList(),
    onStateChange: (ScheduleGroupControlState) -> Unit,  // 🆕 상태 변경
    onPause: (PauseDuration) -> Unit,  // 🆕 일시중지
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLocationClick: () -> Unit = {},
    onTimeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardBackgroundColor = when (controlState) {
        ScheduleGroupControlState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
        ScheduleGroupControlState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
        ScheduleGroupControlState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더: 아이콘 + 이름 + 통합 제어 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getIconForType(group.iconType),
                        contentDescription = null,
                        tint = Color(android.graphics.Color.parseColor(group.colorHex)),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 🆕 통합 제어 버튼 (기존 Switch 대체)
                ScheduleControlButton(
                    state = controlState,
                    pauseUntil = group.pauseUntil,
                    onStateChange = onStateChange,
                    onPause = onPause
                )
            }
            
            // ... 나머지 기존 UI 유지
        }
    }
}
```

---

## 5. ViewModel 및 Repository 변경

### 5.1 ScheduleGroupViewModel 수정

```kotlin
@HiltViewModel
class ScheduleGroupViewModel @Inject constructor(
    private val repository: ScheduleGroupRepository,
    private val scheduleGroupManager: ScheduleGroupManager,
    private val alarmManager: AutoRunAlarmManager,
    private val geofenceManager: AutoRunGeofenceManager
) : ViewModel() {
    
    /**
     * 스케줄 그룹 제어 상태 변경
     * 
     * @param groupId 스케줄 그룹 ID
     * @param newState 새로운 상태
     */
    fun changeControlState(groupId: String, newState: ScheduleGroupControlState) {
        viewModelScope.launch {
            try {
                when (newState) {
                    ScheduleGroupControlState.ACTIVE -> {
                        // 활성화: manualOverride 해제 + 그룹 활성화
                        repository.updateManualOverride(groupId, null, null)
                        scheduleGroupManager.activateGroup(groupId)
                    }
                    ScheduleGroupControlState.INACTIVE -> {
                        // 비활성화: manualOverride 설정 + 그룹 비활성화
                        repository.updateManualOverride(groupId, "INACTIVE", null)
                        scheduleGroupManager.deactivateGroup(groupId)
                    }
                    ScheduleGroupControlState.PAUSED -> {
                        // 일시중지는 pauseScheduleGroup으로 처리
                    }
                }
            } catch (e: Exception) {
                _errorState.value = "상태 변경 실패: ${e.message}"
            }
        }
    }
    
    /**
     * 스케줄 그룹 일시중지
     * 
     * @param groupId 스케줄 그룹 ID
     * @param duration 일시중지 기간
     */
    fun pauseScheduleGroup(groupId: String, duration: PauseDuration) {
        viewModelScope.launch {
            try {
                val pauseUntil = when (duration) {
                    PauseDuration.ONE_HOUR -> System.currentTimeMillis() + duration.durationMillis
                    PauseDuration.TWO_HOURS -> System.currentTimeMillis() + duration.durationMillis
                    PauseDuration.TODAY -> calculateMidnight()
                    PauseDuration.TOMORROW -> calculateTomorrowMidnight()
                }
                
                // 일시중지 상태 저장 (알람은 유지)
                repository.updateManualOverride(groupId, "PAUSED", pauseUntil)
                
                // UI 업데이트를 위해 Flow가 자동 갱신됨
            } catch (e: Exception) {
                _errorState.value = "일시중지 실패: ${e.message}"
            }
        }
    }
    
    /**
     * 제어 상태 계산 (DB 상태 → UI 상태 변환)
     */
    fun getControlState(group: ScheduleGroup): ScheduleGroupControlState {
        return when (group.manualOverrideState) {
            "INACTIVE" -> ScheduleGroupControlState.INACTIVE
            "PAUSED" -> {
                // 일시중지 만료 확인
                if (group.pauseUntil != null && System.currentTimeMillis() >= group.pauseUntil) {
                    ScheduleGroupControlState.ACTIVE  // 만료됨
                } else {
                    ScheduleGroupControlState.PAUSED
                }
            }
            else -> {
                if (group.isActive) ScheduleGroupControlState.ACTIVE
                else ScheduleGroupControlState.INACTIVE
            }
        }
    }
    
    private fun calculateMidnight(): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    private fun calculateTomorrowMidnight(): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
```

### 5.2 ScheduleGroupRepository 인터페이스 추가

```kotlin
interface ScheduleGroupRepository {
    // ... 기존 메서드
    
    /**
     * 수동 제어 상태 업데이트
     * 
     * @param groupId 스케줄 그룹 ID
     * @param overrideState 수동 제어 상태 (null, "INACTIVE", "PAUSED")
     * @param pauseUntil 일시중지 해제 시각 (null이면 무기한 또는 해당 없음)
     */
    suspend fun updateManualOverride(
        groupId: String, 
        overrideState: String?, 
        pauseUntil: Long?
    )
}
```

---

## 6. 시간 알람 동작 수정

### 6.1 AutoRunAlarmReceiver 수정

```kotlin
private suspend fun handleTimeBasedAutoRun(...) {
    // 1. 자동 실행 가능 여부 확인 (마스터 스위치 + 일시중지)
    if (!entryPoint.userSettingsRepository().isAutoRunEnabled()) {
        Log.w(TAG, "⚠️ AutoRun is disabled or paused, skipping")
        return
    }
    
    // 2. 스케줄 그룹 제어 상태 확인 (🆕 추가)
    val autoRun = entryPoint.timeBasedAutoRunDao().getById(autoRunId).firstOrNull()
    
    if (autoRun?.scheduleGroupId != null) {
        val scheduleGroup = entryPoint.scheduleGroupDao().getByIdOnce(autoRun.scheduleGroupId)
        
        if (scheduleGroup != null) {
            when (scheduleGroup.manualOverrideState) {
                "INACTIVE" -> {
                    Log.w(TAG, "⏹️ 스케줄 그룹이 비활성화됨, 건너뛰기: ${scheduleGroup.name}")
                    logAutoRunSkipped(autoRunId, "SCHEDULE_GROUP_INACTIVE", entryPoint)
                    return
                }
                "PAUSED" -> {
                    if (scheduleGroup.pauseUntil == null || 
                        System.currentTimeMillis() < scheduleGroup.pauseUntil) {
                        Log.w(TAG, "⏸️ 스케줄 그룹이 일시중지됨, 건너뛰기: ${scheduleGroup.name}")
                        logAutoRunSkipped(autoRunId, "SCHEDULE_GROUP_PAUSED", entryPoint)
                        return
                    }
                    // 일시중지 만료됨 → 정상 진행
                }
            }
        }
    }
    
    // 3. 정상 실행 진행
    // ... 기존 로직
}
```

---

## 7. 마이그레이션 전략

### 7.1 기존 데이터 처리

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 새 컬럼 추가
        database.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN manualOverrideState TEXT DEFAULT NULL"
        )
        database.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN pauseUntil INTEGER DEFAULT NULL"
        )
        
        // 2. 기존 isActive=false인 그룹은 manualOverrideState='INACTIVE'로 설정
        database.execSQL("""
            UPDATE schedule_group 
            SET manualOverrideState = 'INACTIVE' 
            WHERE isActive = 0
        """)
    }
}
```

### 7.2 기존 마스터 스위치와의 관계

| 기존 설정 | 새로운 동작 |
|----------|------------|
| 마스터 스위치 OFF | 모든 스케줄 그룹 실행 안됨 (최상위 제어) |
| 마스터 스위치 ON + 그룹 비활성 | 해당 그룹만 실행 안됨 |
| 마스터 스위치 ON + 그룹 일시중지 | 해당 그룹만 일시적으로 건너뛰기 |

**마스터 스위치 유지**: 기존 `AutoRunControlCard`의 마스터 스위치는 유지하되, 시간 스케줄 상세 화면에서 스케줄 그룹 리스트 화면으로 이동

---

## 8. 테스트 시나리오

### 8.1 기능 테스트

| # | 시나리오 | 예상 결과 |
|---|----------|----------|
| 1 | 활성 상태에서 버튼 탭 | 비활성으로 전환, 알람 취소 |
| 2 | 비활성 상태에서 버튼 탭 | 활성으로 전환, 알람 등록 |
| 3 | 일시중지 상태에서 버튼 탭 | 활성으로 전환, 일시중지 해제 |
| 4 | ▾ 탭하여 드롭다운 → 1시간 일시중지 | 일시중지 상태, 1시간 후 자동 해제 |
| 5 | 일시중지 중 시간 도래 | 타이머 시작 안됨, 로그 기록 |
| 6 | 일시중지 만료 후 시간 도래 | 정상적으로 타이머 시작 |

### 8.2 위치기반 통합 테스트

| # | 시나리오 | 예상 결과 |
|---|----------|----------|
| 1 | 비활성 그룹 + 위치 진입 | 활성화 안됨 (Geofence 해제됨) |
| 2 | 일시중지 그룹 + 위치 진입 | 활성화되지만 알람 트리거 안됨 |
| 3 | 활성 그룹 + 위치 이탈 | 자동 비활성화 (기존 동작 유지) |
| 4 | 사용자 비활성화 후 → 다시 활성화 | 정상 활성화, Geofence 재등록 |

---

## 9. 일정 계획

| 단계 | 작업 | 예상 소요 |
|------|------|----------|
| 1 | DB 마이그레이션 (v7→v8) | 0.5일 |
| 2 | ScheduleControlButton 컴포넌트 구현 | 1일 |
| 3 | ScheduleGroupCard 수정 | 0.5일 |
| 4 | ViewModel/Repository 수정 | 1일 |
| 5 | GeofenceTransitionsReceiver 수정 | 0.5일 |
| 6 | AutoRunAlarmReceiver 수정 | 0.5일 |
| 7 | 테스트 및 버그 수정 | 1일 |
| **합계** | | **5일** |

---

## 10. 참고 사항

### 10.1 관련 파일

- `ScheduleGroup.kt` - 엔티티 수정
- `ScheduleGroupCard.kt` - UI 수정
- `ScheduleGroupViewModel.kt` - 로직 추가
- `GeofenceTransitionsReceiver.kt` - 위치 진입 처리 수정
- `AutoRunAlarmReceiver.kt` - 알람 처리 수정
- `ScheduleGroupManager.kt` - 활성화/비활성화 로직

### 10.2 의존성

- Room Migration 필요 (v7 → v8)
- 기존 마스터 스위치 동작 유지
- 위치기반 Geofence 등록/해제 로직과 연동

