# 스케쥴그룹 시간 UI 개선 - 주간 히트맵 작업 계획서

> **TDD 방법론 기반**: Red → Green → Refactor 사이클 적용  
> **작업 원칙**: 테스트 먼저 작성 → 최소 코드 구현 → 리팩터링  
> **참고 문서**: [99_TDD_plan.md](../99_TDD_plan.md)  
> **작업 결과서 위치**: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/`  
> **작업 결과서 템플릿**: [template.md](../../working_history/version_1.1/template.md)  
> **버전**: v1.0

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 | 스케쥴 그룹 상세 화면에 주간 히트맵 UI 추가 |
| 영향 범위 | ScheduleGroupDetailScreen, ViewModel, Domain Layer |
| 위험 수준 | 🟡 Medium (UI 신규 추가, 기존 기능 변경 없음) |
| 참고 PRD | [11_스케쥴그룹_시간UI개선_prd.md](./11_스케쥴그룹_시간UI개선_prd.md) |
| 작업 브랜치 | `v1.1/feat/time_heatmap` |

---

## 🚨 핵심 리스크 요약

| 리스크 | 영향 | 대응 방안 | 상태 |
|--------|------|----------|------|
| 시간대 자정 넘김 처리 | 🟠 Medium | 분 단위 분배 로직 철저 테스트 | ⬜ |
| 히트맵 계산 성능 | 🟡 Low | ViewModel에서 StateFlow로 캐싱 | ⬜ |
| 접근성 (스크린 리더) | 🟡 Low | Row 기반 semantics 제공 | ⬜ |

---

## 📦 Phase 1: 도메인 모델 및 계산 로직

> 📄 **목적**: 히트맵 데이터 계산 로직 구현 (비즈니스 로직 레이어)

### 1.1 사전 작업

- [ ] **[CONTEXT]** 작업 목적 및 배경 확인
  - PRD 문서 검토: [11_스케쥴그룹_시간UI개선_prd.md](./11_스케쥴그룹_시간UI개선_prd.md)
  - PRD 섹션 4️⃣ "데이터 집계 규칙" 핵심 숙지

- [ ] **[ANALYSIS]** 기존 도메인 모델 분석
  - 파일: `domain/model/TimeBasedAutoRun.kt`
  - 파일: `domain/model/ScheduleGroup.kt`
  - 확인: startTime, duration, enabledDays, isEnabled 필드

- [ ] **[RED]** 실패 테스트 작성 (시간 슬롯 분배)
  ```kotlin
  // WeeklyHeatmapCalculatorTest.kt
  @Test
  fun `9시 15분 시작 90분 duration은 9시에 45분, 10시에 45분 분배`() {
      val autoRun = createAutoRun(startTime = "09:15", durationMinutes = 90)
      val result = WeeklyHeatmapCalculator.distributeToSlots(autoRun)
      
      assertEquals(45, result[9])  // 09:15~10:00
      assertEquals(45, result[10]) // 10:00~10:45
  }
  ```

### 1.2 본 작업

- [ ] **[TASK-001]** 도메인 모델 생성
  - 파일: `domain/model/WeeklyHeatmapModels.kt`
  - 작업:
    ```kotlin
    data class WeeklyHeatmapUiModel(
        val rows: List<HeatmapRow>,
        val summary: HeatmapSummary
    )
    
    data class HeatmapRow(
        val dayOfWeek: DayOfWeek,
        val period: Period,  // AM, PM
        val cells: List<HeatmapCell>,
        val timeSlots: List<TimeSlotInfo>
    )
    
    data class HeatmapCell(
        val hour: Int,
        val totalMinutes: Int,
        val level: HeatmapLevel  // NONE, LIGHT, MEDIUM, HIGH, MAX
    )
    
    data class HeatmapSummary(
        val amTotalMinutes: Int,
        val pmTotalMinutes: Int,
        val peakTimeRange: String
    )
    
    enum class HeatmapLevel(val minMinutes: Int) {
        NONE(0), LIGHT(1), MEDIUM(16), HIGH(31), MAX(46)
    }
    ```
  - 예상 소요: 20분

- [ ] **[TASK-002]** 시간 슬롯 분배 로직 구현
  - 파일: `domain/util/WeeklyHeatmapCalculator.kt`
  - 작업:
    ```kotlin
    object WeeklyHeatmapCalculator {
        /**
         * 시간대를 1시간 슬롯에 분배
         * 예: 09:15 시작 90분 → 9시(45분), 10시(45분)
         */
        fun distributeToSlots(
            startHour: Int,
            startMinute: Int,
            durationMinutes: Int
        ): Map<Int, Int>
        
        /**
         * 자정 넘김 처리
         * 예: 23:30 시작 60분 → 23시(30분, 당일), 0시(30분, 다음일)
         */
        fun handleMidnightCrossing(...)
    }
    ```
  - 예상 소요: 40분

- [ ] **[TASK-003]** 히트맵 전체 계산 로직 구현
  - 파일: `domain/util/WeeklyHeatmapCalculator.kt`
  - 작업:
    ```kotlin
    fun calculate(
        scheduleGroup: ScheduleGroup,
        timeBasedAutoRuns: List<TimeBasedAutoRun>
    ): WeeklyHeatmapUiModel {
        // 1. isEnabled=true 필터링
        // 2. enabledDays 기준 요일 필터링
        // 3. 각 시간대 슬롯 분배
        // 4. 겹치는 시간대 합산 (최대 60분 cap)
        // 5. HeatmapLevel 매핑
    }
    ```
  - 예상 소요: 60분

- [ ] **[GREEN]** 테스트 통과 확인
  ```bash
  ./gradlew test --tests "*WeeklyHeatmapCalculatorTest*"
  ```

- [ ] **[REFACTOR]** 코드 정리
  - 중복 시간 계산 로직 추출
  - 상수 정의 (MAX_MINUTES_PER_SLOT = 60)

### 1.3 사후 작업

- [ ] **[TEST]** 단위 테스트 실행
  ```bash
  ./gradlew test --tests "*WeeklyHeatmap*"
  ```

- [ ] **[TEST]** 엣지 케이스 테스트 추가
  - 자정 넘김: 23:30 시작 120분
  - 빈 시간대: autoRuns 없음
  - 전체 비활성: isEnabled=false

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase1_히트맵계산로직_YYYY-MM-DD.md`
  - 템플릿: [template.md](../../working_history/version_1.1/template.md)

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git add .
  git commit -m "[Phase1] 히트맵 도메인 모델 및 계산 로직 구현"
  ```

---

## 📦 Phase 2: ViewModel 통합

> 📄 **목적**: ViewModel에서 히트맵 데이터 제공 (StateFlow)

### 2.1 사전 작업

- [ ] **[REVIEW]** Phase 1 작업 결과서 검토
  - 파일: `docs/working_history/version_1.1/Phase1_히트맵계산로직_2026-01-XX.md`

- [ ] **[ANALYSIS]** 기존 ViewModel 분석
  - 파일: `presentation/viewmodel/ScheduleGroupViewModel.kt`
  - 확인: scheduleGroups, timeBasedAutoRuns 데이터 흐름

- [ ] **[RED]** 실패 테스트 작성
  ```kotlin
  @Test
  fun `선택된 스케쥴그룹의 히트맵 데이터가 StateFlow로 제공된다`() {
      viewModel.selectScheduleGroup(testGroup.id)
      
      val heatmap = viewModel.weeklyHeatmap.first()
      assertNotNull(heatmap)
      assertEquals(14, heatmap.rows.size) // 7요일 × 2
  }
  ```

### 2.2 본 작업

- [ ] **[TASK-001]** ScheduleGroupDetailViewModel 생성 또는 확장
  - 파일: `presentation/viewmodel/ScheduleGroupDetailViewModel.kt`
  - 작업:
    ```kotlin
    @HiltViewModel
    class ScheduleGroupDetailViewModel @Inject constructor(
        private val scheduleGroupRepository: ScheduleGroupRepository,
        private val timeBasedAutoRunRepository: TimeBasedAutoRunRepository
    ) : ViewModel() {
        
        private val _selectedGroupId = MutableStateFlow<Long?>(null)
        
        val weeklyHeatmap: StateFlow<WeeklyHeatmapUiModel> = 
            combine(
                _selectedGroupId.filterNotNull(),
                timeBasedAutoRunRepository.getAllFlow()
            ) { groupId, autoRuns ->
                val group = scheduleGroupRepository.getById(groupId)
                val linkedAutoRuns = autoRuns.filter { it.scheduleGroupId == groupId }
                WeeklyHeatmapCalculator.calculate(group, linkedAutoRuns)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = WeeklyHeatmapUiModel.EMPTY
            )
        
        fun selectScheduleGroup(groupId: Long) {
            _selectedGroupId.value = groupId
        }
    }
    ```
  - 예상 소요: 40분

- [ ] **[GREEN]** 테스트 통과 확인

- [ ] **[REFACTOR]** 코드 정리

### 2.3 사후 작업

- [ ] **[TEST]** ViewModel 테스트 실행
  ```bash
  ./gradlew test --tests "*ScheduleGroupDetailViewModelTest*"
  ```

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase2_ViewModel통합_YYYY-MM-DD.md`

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git add .
  git commit -m "[Phase2] ViewModel 히트맵 StateFlow 통합"
  ```

---

## 📦 Phase 3: UI 컴포넌트 구현

> 📄 **목적**: 히트맵 Composable UI 구현 (Box/Row 기반)

### 3.1 사전 작업

- [ ] **[REVIEW]** Phase 2 작업 결과서 검토

- [ ] **[ANALYSIS]** 기존 UI 패턴 분석
  - 파일: `presentation/ui/schedule/*.kt`
  - 확인: 기존 스케쥴 그룹 상세 화면 구조

- [ ] **[RED]** UI 테스트 작성 (Compose Test)
  ```kotlin
  @Test
  fun `히트맵 행 탭 시 상세 정보 표시`() {
      composeTestRule.setContent {
          WeeklyHeatmapRow(
              row = testRow,
              onRowClick = { /* verify */ }
          )
      }
      
      composeTestRule
          .onNodeWithContentDescription("월요일 오전")
          .performClick()
      
      // 상세 정보 표시 확인
  }
  ```

### 3.2 본 작업

- [ ] **[TASK-001]** 히트맵 셀 Composable
  - 파일: `presentation/ui/heatmap/HeatmapCell.kt`
  - 작업:
    ```kotlin
    @Composable
    fun HeatmapCell(
        cell: HeatmapCell,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .size(24.dp)
                .background(
                    color = cell.level.toColor(),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
    
    private fun HeatmapLevel.toColor(): Color = when (this) {
        HeatmapLevel.NONE -> Color(0xFFE0E0E0)
        HeatmapLevel.LIGHT -> Color(0xFF90CAF9)
        HeatmapLevel.MEDIUM -> Color(0xFF42A5F5)
        HeatmapLevel.HIGH -> Color(0xFF1976D2)
        HeatmapLevel.MAX -> Color(0xFF0D47A1)
    }
    ```
  - 예상 소요: 20분

- [ ] **[TASK-002]** 히트맵 행 Composable (탭 가능)
  - 파일: `presentation/ui/heatmap/HeatmapRow.kt`
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
                    contentDescription = "${row.dayOfWeek.displayName} ${row.period.displayName}, " +
                        "총 ${row.totalMinutes}분 계획됨"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 라벨: "MON AM"
            Text(
                text = "${row.dayOfWeek.shortName} ${row.period.name}",
                modifier = Modifier.width(64.dp)
            )
            
            // 12개 셀
            row.cells.forEach { cell ->
                HeatmapCell(cell = cell)
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
    ```
  - 예상 소요: 30분

- [ ] **[TASK-003]** 히트맵 전체 Composable
  - 파일: `presentation/ui/heatmap/WeeklyHeatmap.kt`
  - 작업:
    ```kotlin
    @Composable
    fun WeeklyHeatmap(
        heatmap: WeeklyHeatmapUiModel,
        onRowClick: (HeatmapRow) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            // 요약 정보
            HeatmapSummary(summary = heatmap.summary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 히트맵 그리드
            LazyColumn {
                items(heatmap.rows) { row ->
                    HeatmapRow(row = row, onRowClick = onRowClick)
                    if (row.period == Period.PM) {
                        Divider()  // 요일 구분선
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 범례
            HeatmapLegend()
        }
    }
    ```
  - 예상 소요: 40분

- [ ] **[TASK-004]** 범례 Composable
  - 파일: `presentation/ui/heatmap/HeatmapLegend.kt`
  - 예상 소요: 15분

- [ ] **[TASK-005]** 행 탭 시 상세 정보 BottomSheet
  - 파일: `presentation/ui/heatmap/HeatmapDetailSheet.kt`
  - 예상 소요: 30분

- [ ] **[GREEN]** UI 테스트 통과 확인

- [ ] **[REFACTOR]** 코드 정리

### 3.3 사후 작업

- [ ] **[TEST]** Compose UI 테스트 실행
  ```bash
  ./gradlew connectedAndroidTest --tests "*HeatmapTest*"
  ```

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

## 📦 Phase 4: 화면 통합 및 빈 상태 처리

> 📄 **목적**: 스케쥴 그룹 상세 화면에 히트맵 통합, 빈 상태 CTA 추가

### 4.1 사전 작업

- [ ] **[REVIEW]** Phase 3 작업 결과서 검토

- [ ] **[ANALYSIS]** 기존 상세 화면 분석
  - 파일: `presentation/ui/schedule/ScheduleGroupDetailScreen.kt`
  - 확인: 현재 시간 리스트 표시 위치

### 4.2 본 작업

- [ ] **[TASK-001]** 상세 화면에 히트맵 통합
  - 파일: `presentation/ui/schedule/ScheduleGroupDetailScreen.kt`
  - 작업: 시간 섹션에 WeeklyHeatmap 추가

- [ ] **[TASK-002]** 빈 상태 UI 구현
  - 파일: `presentation/ui/heatmap/HeatmapEmptyState.kt`
  - 작업:
    ```kotlin
    @Composable
    fun HeatmapEmptyState(
        onAddScheduleClick: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 빈 그리드 표시
            EmptyHeatmapGrid()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("시간 스케쥴을 추가해보세요")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = onAddScheduleClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("스케쥴 추가")
            }
        }
    }
    ```

- [ ] **[TASK-003]** 기존 시간 리스트와 공존 처리
  - 히트맵 아래에 기존 시간 리스트 유지 (편집용)
  - [수정] [삭제] 버튼 기존 위치 유지

- [ ] **[GREEN]** 테스트 통과 확인

### 4.3 사후 작업

- [ ] **[VERIFY]** 전체 플로우 검증
  - 스케쥴 그룹 리스트 → 카드 탭 → 상세 화면 → 히트맵 표시
  - 빈 상태 → CTA 버튼 → 시간 추가 다이얼로그

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/스케쥴그룹_시간UI개선_히트맵/Phase4_화면통합_YYYY-MM-DD.md`

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git add .
  git commit -m "[Phase4] 상세 화면 히트맵 통합 및 빈 상태 처리"
  ```

---

## 📦 Phase 5: 접근성 및 최종 검증

> 📄 **목적**: 접근성 준수 확인, 최종 QA

### 5.1 사전 작업

- [ ] **[REVIEW]** Phase 4 작업 결과서 검토

### 5.2 본 작업

- [ ] **[TASK-001]** 스크린 리더 테스트
  - TalkBack 활성화
  - 모든 히트맵 행에 contentDescription 읍션 확인

- [ ] **[TASK-002]** 터치 타겟 검증
  - 행 높이 48dp 이상 확인
  - 탭 오류율 테스트

- [ ] **[TASK-003]** 색상 콘트라스트 검증
  - WCAG 2.1 AA 기준 (4.5:1) 확인

### 5.3 사후 작업

- [ ] **[VERIFY]** QA 체크리스트
  - [ ] 히트맵 표시 정상
  - [ ] 행 탭 → 상세 정보 표시
  - [ ] 빈 상태 → CTA 동작
  - [ ] 스크린 리더 읍션 정상
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
| Phase 1 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 2 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 3 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 4 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 5 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

---

## 📅 예상 일정

| Phase | 예상 소요 | 시작일 | 완료일 | 비고 |
|-------|----------|--------|--------|------|
| Phase 1 | 0.5일 | - | - | 도메인 로직 |
| Phase 2 | 0.5일 | - | - | ViewModel |
| Phase 3 | 1일 | - | - | UI 컴포넌트 |
| Phase 4 | 0.5일 | - | - | 화면 통합 |
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
**상태**: ⬜ 작성 중
