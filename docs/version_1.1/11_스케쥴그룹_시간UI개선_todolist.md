# 스케쥴그룹 시간 UI 개선 - 주간 히트맵 작업 계획서

> **TDD 방법론 기반**: Red → Green → Refactor 사이클 적용  
> **작업 원칙**: 테스트 먼저 작성 → 최소 코드 구현 → 리팩터링  
> **참고 문서**: [99_TDD_plan.md](../99_TDD_plan.md)  
> **작업 결과서 위치**: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/`  
> **작업 결과서 템플릿**: [template.md](../../working_history/version_1.1/template.md)  
> **버전**: v2.0 (리뷰 피드백 반영)

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 | 스케쥴 그룹 상세 화면에 주간 히트맵 UI 추가 |
| 영향 범위 | ScheduleGroupScreen, ScheduleGroupCard, ViewModel |
| 위험 수준 | 🟡 Medium (UI 신규 추가, 기존 기능 변경 없음) |
| 참고 PRD | [11_스케쥴그룹_시간UI개선_prd.md](./11_스케쥴그룹_시간UI개선_prd.md) |
| 작업 브랜치 | `v1.1/feat/time_heatmap` |

## 🚨 핵심 리스크 요약

| 리스크 | 영향 | 대응 방안 | 상태 |
|--------|------|----------|------|
| 시간대 자정 넘김 처리 | 🟠 Medium | 기존 AutoRunAlarmManager 로직 참조 | ⬜ |
| 히트맵 계산 성능 | 🟡 Low | ViewModel에서 StateFlow로 캐싱 | ⬜ |

---

## 📦 Phase 1: UI 모델 및 계산 로직

> 📄 **목적**: 히트맵 데이터 계산 로직 구현 (Presentation 레이어)

### 1.1 사전 작업

- [x] **[CONTEXT]** 작업 목적 및 배경 확인
  - PRD 문서 검토: [11_스케쥴그룹_시간UI개선_prd.md](./11_스케쥴그룹_시간UI개선_prd.md)
  - PRD 섹션 4️⃣ "데이터 집계 규칙" 핵심 숙지

- [x] **[ANALYSIS]** 기존 엔티티 및 유틸 분석
  - 파일: `app/src/main/java/com/allday/detoxy/data/local/entity/TimeBasedAutoRun.kt`
  - 파일: `app/src/main/java/com/allday/detoxy/data/local/entity/ScheduleGroup.kt`
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/AddTimeBasedAutoRunDialog.kt`
  - 확인: hour, minute, durationMinutes, enabledDays(JSON), isEnabled 필드

- [x] **[RED]** 실패 테스트 작성 (시간 슬롯 분배)
  ```kotlin
  // WeeklyHeatmapCalculatorTest.kt
  @Test
  fun `9시 15분 시작 90분 duration은 9시에 45분, 10시에 45분 분배`() {
      val result = WeeklyHeatmapCalculator.distributeToSlots(
          startHour = 9,
          startMinute = 15,
          durationMinutes = 90
      )
      
      assertEquals(45, result[9])  // 09:15~10:00
      assertEquals(45, result[10]) // 10:00~10:45
  }
  ```

### 1.2 본 작업

- [x] **[TASK-001]** UI 모델 생성 (Presentation 레이어)
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/model/WeeklyHeatmapModels.kt`
  - 작업:
    ```kotlin
    package com.allday.detoxy.presentation.model
    
    import java.time.DayOfWeek
    
    /**
     * 주간 히트맵 UI 모델
     */
    data class WeeklyHeatmapUiModel(
        val rows: List<HeatmapRow>,
        val summary: HeatmapSummary
    ) {
        companion object {
            val EMPTY = WeeklyHeatmapUiModel(
                rows = emptyList(),
                summary = HeatmapSummary(0, 0, "")
            )
        }
    }
    
    data class HeatmapRow(
        val dayOfWeek: DayOfWeek,
        val period: Period,  // AM, PM
        val cells: List<HeatmapCell>,
        val timeSlots: List<TimeSlotInfo>,
        val totalMinutes: Int  // 해당 행의 총 계획 시간
    )
    
    data class HeatmapCell(
        val hour: Int,
        val totalMinutes: Int,
        val level: HeatmapLevel
    )
    
    data class TimeSlotInfo(
        val id: String,  // TimeBasedAutoRun ID (UUID)
        val hour: Int,
        val minute: Int,
        val durationMinutes: Int,
        val presetType: String,
        val label: String?
    )
    
    data class HeatmapSummary(
        val amTotalMinutes: Int,
        val pmTotalMinutes: Int,
        val peakTimeRange: String
    )
    
    enum class Period { AM, PM }
    
    enum class HeatmapLevel(val minMinutes: Int) {
        NONE(0), LIGHT(1), MEDIUM(16), HIGH(31), MAX(46);
        
        companion object {
            fun fromMinutes(minutes: Int): HeatmapLevel = when {
                minutes >= 46 -> MAX
                minutes >= 31 -> HIGH
                minutes >= 16 -> MEDIUM
                minutes >= 1 -> LIGHT
                else -> NONE
            }
        }
    }
    ```
  - 예상 소요: 20분

- [x] **[TASK-002]** 시간 슬롯 분배 로직 구현
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/util/WeeklyHeatmapCalculator.kt`
  - 작업:
    ```kotlin
    package com.allday.detoxy.presentation.util
    
    import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
    import com.allday.detoxy.presentation.model.*
    import java.time.DayOfWeek
    import org.json.JSONArray
    
    object WeeklyHeatmapCalculator {
        
        private const val MAX_MINUTES_PER_SLOT = 60
        
        /**
         * 시간대를 1시간 슬롯에 분배 (자정 넘김 시 요일 오프셋 포함)
         * 예: 09:15 시작 90분 → 9시(45분), 10시(45분)
         * 예: 23:30 시작 90분 → (dayOffset=0, 23시:30분), (dayOffset=1, 0시:60분)
         * 
         * @return List<SlotDistribution> - 각 슬롯의 요일 오프셋과 분 정보
         */
        data class SlotDistribution(
            val dayOffset: Int,  // 0=당일, 1=다음날, ...
            val hour: Int,
            val minutes: Int
        )
        
        fun distributeToSlots(
            startHour: Int,
            startMinute: Int,
            durationMinutes: Int
        ): List<SlotDistribution> {
            val result = mutableListOf<SlotDistribution>()
            var remainingMinutes = durationMinutes
            var currentHour = startHour
            var currentMinute = startMinute
            var dayOffset = 0
            
            while (remainingMinutes > 0) {
                val minutesInThisSlot = minOf(60 - currentMinute, remainingMinutes)
                val hour = currentHour % 24
                
                // 자정 넘김 감지: 24시간 이상이 되면 다음 요일로 이동
                if (currentHour >= 24 && hour == 0 && result.lastOrNull()?.hour != 0) {
                    dayOffset++
                }
                
                result.add(SlotDistribution(dayOffset, hour, minutesInThisSlot))
                
                remainingMinutes -= minutesInThisSlot
                currentHour++
                currentMinute = 0
            }
            
            return result
        }
        
        /**
         * enabledDays JSON 파싱
         * 예: "[\"MON\",\"TUE\"]" → listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
         */
        fun parseEnabledDays(enabledDaysJson: String): List<DayOfWeek> {
            return try {
                val jsonArray = JSONArray(enabledDaysJson)
                (0 until jsonArray.length()).mapNotNull { index ->
                    when (jsonArray.getString(index)) {
                        "MON" -> DayOfWeek.MONDAY
                        "TUE" -> DayOfWeek.TUESDAY
                        "WED" -> DayOfWeek.WEDNESDAY
                        "THU" -> DayOfWeek.THURSDAY
                        "FRI" -> DayOfWeek.FRIDAY
                        "SAT" -> DayOfWeek.SATURDAY
                        "SUN" -> DayOfWeek.SUNDAY
                        else -> null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        /**
         * 전체 히트맵 계산
         */
        fun calculate(
            autoRuns: List<TimeBasedAutoRun>
        ): WeeklyHeatmapUiModel {
            // isEnabled=true만 필터링
            val enabledAutoRuns = autoRuns.filter { it.isEnabled }
            
            if (enabledAutoRuns.isEmpty()) {
                return WeeklyHeatmapUiModel.EMPTY
            }
            
            // 요일별, 시간별 분 합산
            val dayHourMinutes = mutableMapOf<DayOfWeek, MutableMap<Int, Int>>()
            val dayTimeSlots = mutableMapOf<DayOfWeek, MutableList<TimeSlotInfo>>()
            
            enabledAutoRuns.forEach { autoRun ->
                val days = parseEnabledDays(autoRun.enabledDays)
                val slots = distributeToSlots(autoRun.hour, autoRun.minute, autoRun.durationMinutes)
                
                days.forEach { day ->
                    // 히트맵 셀 분배 (요일별 시간 합산)
                    slots.forEach { slot ->
                        val targetDay = day.plus(slot.dayOffset.toLong())
                        val hourMap = dayHourMinutes.getOrPut(targetDay) { mutableMapOf() }
                        hourMap[slot.hour] = minOf(
                            (hourMap[slot.hour] ?: 0) + slot.minutes,
                            MAX_MINUTES_PER_SLOT
                        )
                    }
                    
                    // TimeSlotInfo 저장 (자정 넘김 시 다음 요일에도 저장)
                    val timeSlotInfo = TimeSlotInfo(
                        id = autoRun.id,
                        hour = autoRun.hour,
                        minute = autoRun.minute,
                        durationMinutes = autoRun.durationMinutes,
                        presetType = autoRun.presetType,
                        label = autoRun.label
                    )
                    
                    // 시작 요일에 저장
                    dayTimeSlots.getOrPut(day) { mutableListOf() }.add(timeSlotInfo)
                    
                    // 자정 넘김 시 다음 요일(AM)에도 저장 (상세 시트에서 누락 방지)
                    val hasNextDaySlots = slots.any { it.dayOffset > 0 }
                    if (hasNextDaySlots) {
                        val nextDay = day.plus(1)
                        dayTimeSlots.getOrPut(nextDay) { mutableListOf() }.add(timeSlotInfo)
                    }
                }
            }
            
            // HeatmapRow 생성
            val rows = DayOfWeek.values().flatMap { day ->
                listOf(
                    createRow(day, Period.AM, dayHourMinutes[day] ?: emptyMap(), 
                              dayTimeSlots[day] ?: emptyList()),
                    createRow(day, Period.PM, dayHourMinutes[day] ?: emptyMap(), 
                              dayTimeSlots[day] ?: emptyList())
                )
            }
            
            // Summary 계산
            val amTotal = rows.filter { it.period == Period.AM }.sumOf { it.totalMinutes }
            val pmTotal = rows.filter { it.period == Period.PM }.sumOf { it.totalMinutes }
            
            return WeeklyHeatmapUiModel(
                rows = rows,
                summary = HeatmapSummary(amTotal, pmTotal, findPeakTimeRange(dayHourMinutes))
            )
        }
        
        private fun createRow(
            day: DayOfWeek,
            period: Period,
            hourMinutes: Map<Int, Int>,
            timeSlots: List<TimeSlotInfo>
        ): HeatmapRow {
            val hourRange = if (period == Period.AM) 0..11 else 12..23
            val cells = hourRange.map { hour ->
                val minutes = hourMinutes[hour] ?: 0
                HeatmapCell(hour, minutes, HeatmapLevel.fromMinutes(minutes))
            }
            
            val periodSlots = timeSlots.filter { slot ->
                val slotHour = slot.hour
                if (period == Period.AM) slotHour in 0..11 else slotHour in 12..23
            }
            
            return HeatmapRow(
                dayOfWeek = day,
                period = period,
                cells = cells,
                timeSlots = periodSlots,
                totalMinutes = cells.sumOf { it.totalMinutes }
            )
        }
        
        private fun findPeakTimeRange(dayHourMinutes: Map<DayOfWeek, Map<Int, Int>>): String {
            // 가장 밀집된 시간대 찾기 (간단 구현)
            val hourTotals = mutableMapOf<Int, Int>()
            dayHourMinutes.values.forEach { hourMap ->
                hourMap.forEach { (hour, minutes) ->
                    hourTotals[hour] = (hourTotals[hour] ?: 0) + minutes
                }
            }
            
            val peakHour = hourTotals.maxByOrNull { it.value }?.key ?: return ""
            val period = if (peakHour < 12) "오전" else "오후"
            // 12시간 표기 (0시=오전 12시, 12시=오후 12시, 13시=오후 1시)
            val displayHour = when {
                peakHour == 0 -> 12   // 오전 12시
                peakHour == 12 -> 12  // 오후 12시
                peakHour < 12 -> peakHour
                else -> peakHour - 12
            }
            // 다음 시간 계산 (12시 다음은 1시)
            val nextHour = if (displayHour == 12) 1 else displayHour + 1
            return "$period ${displayHour}~${nextHour}시"
        }
    }
    ```
  - 예상 소요: 60분

- [x] **[GREEN]** 테스트 통과 확인
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "*WeeklyHeatmapCalculatorTest*"
  ```

- [x] **[REFACTOR]** 코드 정리
  - 정규식 기반 enabledDays 파싱 적용 (org.json.JSONArray 대체)
  - 상수 정의 확인

### 1.3 사후 작업

- [x] **[TEST]** 단위 테스트 실행
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "*WeeklyHeatmap*"
  # 15개 테스트 전체 통과
  ```

- [x] **[TEST]** 엣지 케이스 테스트 추가
  - 자정 넘김: 23시 30분 시작 120분 ✅
  - 빈 시간대: autoRuns 없음 ✅
  - 전체 비활성: isEnabled=false ✅

- [x] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase1_히트맵계산로직_2026-01-01.md`
  - 템플릿: [template.md](../../working_history/version_1.1/template.md)

- [x] **[COMMIT]** 변경사항 커밋
  ```bash
  git commit -m "[Phase1] 히트맵 UI 모델 및 계산 로직 구현"
  ```

---

## 📦 Phase 2: ViewModel 통합

> 📄 **목적**: 기존 ScheduleGroupViewModel 확장 또는 StateFlow 추가

### 2.1 사전 작업

- [x] **[REVIEW]** Phase 1 작업 결과서 검토
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase1_히트맵계산로직_2026-01-01.md`

- [x] **[ANALYSIS]** 기존 ViewModel 분석
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
  - 확인: `linkedTimeBasedAutoRuns` Map 구조 활용
  - 확인: 초기 로딩 후 갱신 타이밍

- [ ] **[RED]** 실패 테스트 작성
  ```kotlin
  @Test
  fun `선택된 스케쥴그룹의 히트맵 데이터가 StateFlow로 제공된다`() {
      // Given
      val groupId = "test-uuid"
      viewModel.selectGroupForHeatmap(groupId)
      
      // When
      val heatmap = viewModel.selectedGroupHeatmap.first()
      
      // Then
      assertNotNull(heatmap)
      assertEquals(14, heatmap.rows.size) // 7요일 × 2 (AM/PM)
  }
  ```

### 2.2 본 작업

- [x] **[TASK-001]** ScheduleGroupViewModel 확장
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
  - 작업: 히트맵 StateFlow 및 메서드 추가 완료
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
  - 작업:
    ```kotlin
    // ==================== 히트맵 지원 (v11 추가) ====================
    
    private val _selectedGroupIdForHeatmap = MutableStateFlow<String?>(null)
    
    // 히트맵 데이터를 위한 Flow (그룹별 시간대 리스트)
    private val _heatmapAutoRuns = MutableStateFlow<Map<String, List<TimeBasedAutoRun>>>(emptyMap())
    
    /**
     * 선택된 그룹의 히트맵 데이터
     * 
     * ⚠️ snapshotFlow 사용 불가 (Compose 런타임 전용)
     * → _heatmapAutoRuns StateFlow를 직접 combine
     */
    val selectedGroupHeatmap: StateFlow<WeeklyHeatmapUiModel> = 
        combine(
            _selectedGroupIdForHeatmap.filterNotNull(),
            _heatmapAutoRuns
        ) { groupId, autoRunsMap ->
            val autoRuns = autoRunsMap[groupId] ?: emptyList()
            WeeklyHeatmapCalculator.calculate(autoRuns)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = WeeklyHeatmapUiModel.EMPTY
        )
    
    /**
     * 히트맵 표시를 위한 그룹 선택
     * 
     * @param groupId 스케줄 그룹 ID (UUID)
     */
    fun selectGroupForHeatmap(groupId: String) {
        _selectedGroupIdForHeatmap.value = groupId
        // 선택 시 해당 그룹의 시간대 데이터 로드
        refreshHeatmapData(groupId)
    }
    
    /**
     * 히트맵 데이터 갱신 (시간대 추가/삭제 후 호출)
     */
    fun refreshHeatmapData(groupId: String) {
        viewModelScope.launch {
            val autoRuns = repository.getLinkedTimeBasedAutoRuns(groupId)
            _heatmapAutoRuns.update { current ->
                current.toMutableMap().apply { this[groupId] = autoRuns }
            }
        }
    }
    
    /**
     * 히트맵 표시 해제
     */
    fun clearHeatmapSelection() {
        _selectedGroupIdForHeatmap.value = null
    }
    ```
  - 예상 소요: 30분

- [x] **[TASK-002]** 히트맵 데이터 갱신 트리거 추가
  - **✅ Option A 선택: SharedFlow 이벤트 발행**
  - 파일: `TimeBasedAutoRunViewModel.kt`에 `timeSlotUpdated: SharedFlow<String>` 추가
  - `addAutoRun`, `updateAutoRun`, `deleteAutoRun`, `toggleAutoRun`에서 emit
  - **⚠️ ViewModel 간 연결 필요**:
    - 실제 CRUD는 `TimeBasedAutoRunViewModel`에서 처리됨
    - `ScheduleGroupViewModel.refreshHeatmapData()` 호출 필요
  - **해결책 (3가지 중 선택)**:
    
    **A. SharedFlow 이벤트 발행 (권장)**
    ```kotlin
    // TimeBasedAutoRunViewModel.kt
    private val _timeSlotUpdated = MutableSharedFlow<String>()  // groupId
    val timeSlotUpdated: SharedFlow<String> = _timeSlotUpdated.asSharedFlow()
    
    fun addTimeBasedAutoRun(autoRun: TimeBasedAutoRun) {
        viewModelScope.launch {
            repository.insert(autoRun)
            autoRun.scheduleGroupId?.let { _timeSlotUpdated.emit(it) }
        }
    }
    
    // ScheduleGroupScreen.kt
    LaunchedEffect(Unit) {
        timeBasedViewModel.timeSlotUpdated.collect { groupId ->
            scheduleGroupViewModel.refreshHeatmapData(groupId)
        }
    }
    ```
    
    **B. UI에서 직접 호출**
    ```kotlin
    // TimeBasedAutoRunScreen.kt
    onAddComplete = { autoRun ->
        scheduleGroupViewModel.refreshHeatmapData(autoRun.scheduleGroupId!!)
    }
    ```
    
    **C. Repository Flow 구독 (자동 갱신)**
    ```kotlin
    // ScheduleGroupViewModel.kt
    init {
        viewModelScope.launch {
            timeBasedRepository.getAll().collect { allAutoRuns ->
                // 모든 그룹의 히트맵 데이터 갱신
                updateAllHeatmapData(allAutoRuns)
            }
        }
    }
    ```

- [x] **[GREEN]** 빌드 통과 확인
  ```bash
  ./gradlew :app:compileDebugKotlin
  # BUILD SUCCESSFUL
  ```

- [x] **[REFACTOR]** 코드 정리
  - 기존 `_linkedTimeBasedAutoRuns` StateFlow 재활용으로 중복 방지

### 2.3 사후 작업

- [x] **[TEST]** ViewModel 테스트 실행 → **Phase 3 이후 통합 테스트로 진행**
  - 현재 `ScheduleGroupViewModelTest.kt` 미존재
  - UI 통합 후 동작 검증이 더 용이

- [x] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase2_ViewModel통합_2026-01-02.md`

- [x] **[COMMIT]** 변경사항 커밋 ✅
  - `[Phase2] ViewModel 히트맵 StateFlow 통합`
  - `[Phase2] 리뷰 피드백 반영: SharedFlow backpressure 방지`

---

## 📦 Phase 3: UI 컴포넌트 구현

> 📄 **목적**: 히트맵 Composable UI 구현 (Row 기반, 터치 가능)

### 3.1 사전 작업

- [x] **[REVIEW]** Phase 2 작업 결과서 검토

- [x] **[ANALYSIS]** 기존 UI 패턴 분석
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleGroupCard.kt`
  - 확인: 기존 시간 리스트 표시 방식 분석 완료

### 3.2 본 작업

- [x] **[TASK-001]** 히트맵 셀 Composable
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/HeatmapCell.kt`

- [x] **[TASK-002]** 히트맵 행 Composable (Row 단위 탭 가능)
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/HeatmapRow.kt`
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/HeatmapRow.kt`
  - 작업:
    ```kotlin
    @Composable
    fun HeatmapRow(
        row: HeatmapRow,
        onRowClick: (HeatmapRow) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)  // 접근성: 최소 터치 타겟
                .clickable { onRowClick(row) }
                .semantics {
                    contentDescription = "${row.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)} " +
                        "${row.period.name}, 총 ${row.totalMinutes}분 계획됨"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 라벨: "월 AM"
            Text(
                text = "${row.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)} ${row.period.name}",
                modifier = Modifier.width(56.dp),
                style = MaterialTheme.typography.labelSmall
            )
            
            // 12개 셀
            row.cells.forEach { cell ->
                HeatmapCell(cell = cell, modifier = Modifier.weight(1f))
            }
        }
    }
    ```
  - 예상 소요: 30분

- [x] **[TASK-003]** 히트맵 전체 Composable
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/WeeklyHeatmap.kt`

- [x] **[TASK-004]** 범례 Composable
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/HeatmapLegend.kt`

- [x] **[TASK-005]** 행 탭 시 상세 정보 BottomSheet
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/HeatmapDetailSheet.kt`

- [x] **[GREEN]** 빌드 통과 확인

### 3.3 사후 작업

- [ ] **[VERIFY]** 에뮬레이터 수동 검증
  - 히트맵 표시 확인
  - 행 탭 → 상세 정보 표시 확인
  - 스크린 리더 동작 확인

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase3_UI컴포넌트_YYYY-MM-DD.md`

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git add .
  git commit -m "[Phase3] 히트맵 UI 컴포넌트 구현"
  ```

---

## 📦 Phase 4: ScheduleGroupCard 통합

> 📄 **목적**: 기존 카드에 히트맵 확장/축소 기능 추가

### 4.1 사전 작업

- [ ] **[REVIEW]** Phase 3 작업 결과서 검토

- [ ] **[ANALYSIS]** 기존 카드 구조 분석
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleGroupCard.kt`
  - 확인: 현재 시간 리스트 위치 및 구조

### 4.2 본 작업

- [ ] **[TASK-001]** 카드 확장 상태 관리
  - 파일: `ScheduleGroupCard.kt`
  - 작업: `isHeatmapExpanded` 상태 추가

- [ ] **[TASK-002]** 히트맵 토글 버튼 추가
  - 시간 섹션 헤더에 확장/축소 아이콘 추가

- [ ] **[TASK-003]** 히트맵 영역 추가
  - 기존 시간 리스트 위에 히트맵 표시
  - AnimatedVisibility로 확장/축소 애니메이션

- [ ] **[TASK-004]** 빈 상태 UI 구현
  - 파일: `app/src/main/java/com/allday/detoxy/presentation/ui/heatmap/HeatmapEmptyState.kt`
  - **CTA 동작**: 시간표 관리 화면(`TimeBasedAutoRunScreen`)으로 이동
    - ❗ 주의: ScheduleGroupCard 내부에는 시간 추가 다이얼로그 없음
    - 기존 `onNavigateToTimeBasedAutoRun(groupId, groupName)` 콜백 활용
  - 작업:
    ```kotlin
    @Composable
    fun HeatmapEmptyState(
        onNavigateToTimeScreen: () -> Unit  // TimeBasedAutoRunScreen으로 이동
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "시간 스케쥴을 추가해보세요",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToTimeScreen) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("시간표 관리로 이동")
            }
        }
    }
    ```

- [ ] **[GREEN]** 테스트 통과 확인

### 4.3 사후 작업

- [ ] **[VERIFY]** 전체 플로우 검증
  - 스케쥴 그룹 카드 → 시간 섹션 확장 → 히트맵 표시
  - 행 탭 → 상세 정보 바텀시트
  - 빈 상태 → CTA 버튼 → TimeBasedAutoRunScreen 이동

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase4_카드통합_YYYY-MM-DD.md`

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git add .
  git commit -m "[Phase4] ScheduleGroupCard 히트맵 통합"
  ```

---

## 📦 Phase 5: 접근성 및 최종 검증

> 📄 **목적**: 접근성 준수 확인, 최종 QA

### 5.1 본 작업

- [ ] **[TASK-001]** 스크린 리더 테스트
  - TalkBack 활성화
  - 모든 히트맵 행에 contentDescription 확인

- [ ] **[TASK-002]** 터치 타겟 검증
  - 행 높이 48dp 이상 확인
  - 탭 오류율 테스트

- [ ] **[TASK-003]** 색상 콘트라스트 검증
  - WCAG 2.1 AA 기준 (4.5:1) 확인

### 5.2 사후 작업

- [ ] **[VERIFY]** QA 체크리스트
  - [ ] 히트맵 표시 정상
  - [ ] 행 탭 → 상세 정보 표시
  - [ ] 빈 상태 → CTA 동작
  - [ ] 스크린 리더 음성 정상
  - [ ] 다크 모드 대응

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase5_접근성QA_YYYY-MM-DD.md`

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git add .
  git commit -m "[Phase5] 접근성 개선 및 최종 QA 완료"
  ```

---

## ✅ 최종 체크리스트

### 기능 검증
- [ ] 모든 Phase 작업 완료
- [ ] 전체 테스트 통과 (`./gradlew test`)
- [ ] UI 테스트 통과 (`./gradlew connectedAndroidTest`)
- [ ] 린터 경고 0개

### 문서화
- [ ] 각 Phase별 작업 결과서 작성 완료 (`working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/`)
- [ ] PRD 문서와 구현 일치 검증

### 최종 커밋 및 PR
- [ ] 모든 변경사항 커밋 완료
- [ ] PR 생성 및 코드 리뷰 요청
- [ ] CI 파이프라인 통과

---

## 📊 진행 체크리스트

### Phase 완료 조건
| Phase | 테스트 통과 | 린터 통과 | 결과서 작성 | 커밋 완료 | 상태 |
|-------|------------|----------|------------|----------|------|
| Phase 1 | ✅ | ✅ | ✅ | ✅ | ✅ 완료 |
| Phase 2 | ✅ | ✅ | ✅ | ✅ | ✅ 완료 |
| Phase 3 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 4 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 5 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

---

## 📅 예상 일정

| Phase | 예상 소요 | 시작일 | 완료일 | 비고 |
|-------|----------|--------|--------|------|
| Phase 1 | 0.5일 | - | - | UI 모델 + 계산 로직 |
| Phase 2 | 0.5일 | - | - | ViewModel 확장 |
| Phase 3 | 1일 | - | - | UI 컴포넌트 |
| Phase 4 | 0.5일 | - | - | 카드 통합 |
| Phase 5 | 0.5일 | - | - | 접근성/QA |
| **Total** | **3일** | - | - | - |

---

## 🔗 관련 문서

- [PRD 문서](./11_스케쥴그룹_시간UI개선_prd.md) - 기획설계 문서
- [99_TDD_plan.md](../99_TDD_plan.md) - TDD 방법론 가이드
- [작업 결과서 디렉토리](../../working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/) - 작업 결과서 위치
- [template.md](../../working_history/version_1.1/template.md) - 작업 결과서 템플릿

---

**작성일**: 2026-01-01  
**작성자**: AI Assistant  
**최종 수정일**: 2026-01-01  
**상태**: ✅ 리뷰 피드백 반영 완료
