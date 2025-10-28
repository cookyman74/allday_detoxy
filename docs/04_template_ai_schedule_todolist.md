# Allday Detoxy 4차 고도화 작업 계획
## 시간표 템플릿 및 AI 기반 스케줄 추천

- **기준 문서**: [4차 고도화 PRD](./03_template_ai_schedule_prd.md)
- **선행 작업**: 2.5차 고도화 (v0.7) 완료 필수
- **버전**: v0.7 → v0.8
- **예상 기간**: 3주 (Day 1-21)

---

## 0. 개요

### 0.1 배경 및 목표

**배경**:
- 2.5차(v0.7)에서 위치-시간표 연결 구현 완료
- 사용자 피드백: 시간표 생성 진입 장벽 높음
- 최적 시간대 설정에 대한 가이드 부족

**목표**:
- 10개 사전 정의 템플릿 제공으로 즉시 시작 가능
- AI 기반 패턴 분석으로 개인 맞춤 추천
- 시간표 설정 완료 시간 2분 이내 단축
- 템플릿 사용률 70%, AI 추천 수락률 40% 달성

### 0.2 핵심 기능

1. **시간표 템플릿** (10개 사전 정의)
2. **AI 패턴 분석 및 추천**
3. **시간표 Export/Import (JSON)**
4. **성과 추적 및 히스토리**
5. **A/B 테스트** (선택)
6. **커뮤니티 시간표** (선택)

### 0.3 성공 지표

- 템플릿 사용률 ≥ 70%
- AI 추천 수락률 ≥ 40%
- 시간표 설정 완료 시간 ≤ 2분
- 시간표 활용률 ≥ 60% (7일 내)
- WAU +30% (v0.7 대비)

---

## 1. 데이터 모델 설계 (Day 1-2)

### 1.1 Room 마이그레이션 v5→v6

#### 1.1.1 신규 엔티티 정의

- [ ] **ScheduleTemplate.kt** 엔티티 생성
```kotlin
@Entity(tableName = "schedule_template")
data class ScheduleTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val iconType: String,
    val colorHex: String,
    val timeSlotsJson: String,
    val isDefault: Boolean,
    val usageCount: Int = 0,
    val rating: Float = 0f,
    val authorId: String? = null,
    val createdAt: Long
)
```

- [ ] **AIRecommendation.kt** 엔티티 생성
```kotlin
@Entity(tableName = "ai_recommendation")
data class AIRecommendation(
    @PrimaryKey val id: String,
    val patternJson: String,
    val recommendedScheduleJson: String,
    val confidence: Float,
    val accepted: Boolean = false,
    val modificationsJson: String? = null,
    val createdAt: Long,
    val acceptedAt: Long? = null
)
```

- [ ] **ScheduleGroupHistory.kt** 엔티티 생성
```kotlin
@Entity(
    tableName = "schedule_group_history",
    foreignKeys = [ForeignKey(
        entity = ScheduleGroup::class,
        parentColumns = ["id"],
        childColumns = ["scheduleGroupId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScheduleGroupHistory(
    @PrimaryKey val id: String,
    val scheduleGroupId: String,
    val activeFrom: Long,
    val activeTo: Long? = null,
    val totalActiveDays: Int = 0,
    val totalSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val avgCompletionRate: Float = 0f,
    val successfulDays: Int = 0
)
```

- [ ] **ABTestConfig.kt** 엔티티 생성
```kotlin
@Entity(tableName = "ab_test_config")
data class ABTestConfig(
    @PrimaryKey val id: String,
    val scheduleGroupAId: String,
    val scheduleGroupBId: String,
    val testDurationDays: Int,
    val switchStrategy: String,
    val startDate: Long,
    val endDate: Long? = null,
    val isCompleted: Boolean = false,
    val winnerId: String? = null
)
```

#### 1.1.2 마이그레이션 스크립트 작성

- [ ] **Migration_5_6.kt** 작성 (4개 테이블 생성)
- [ ] **DetoxyDatabase.kt** 버전 6으로 업데이트
- [ ] **DatabaseModule.kt**에 MIGRATION_5_6 추가

#### 1.1.3 DAO 인터페이스 작성

- [ ] **ScheduleTemplateDao.kt** 생성 (12개 메서드)
```kotlin
@Dao
interface ScheduleTemplateDao {
    @Query("SELECT * FROM schedule_template WHERE isDefault = 1")
    fun getDefaultTemplates(): Flow<List<ScheduleTemplate>>
    
    @Query("SELECT * FROM schedule_template WHERE category = :category")
    fun getByCategory(category: String): Flow<List<ScheduleTemplate>>
    
    @Query("SELECT * FROM schedule_template ORDER BY usageCount DESC LIMIT :limit")
    fun getPopular(limit: Int = 10): Flow<List<ScheduleTemplate>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: ScheduleTemplate)
    
    @Update
    suspend fun update(template: ScheduleTemplate)
    
    @Delete
    suspend fun delete(template: ScheduleTemplate)
    
    @Query("UPDATE schedule_template SET usageCount = usageCount + 1 WHERE id = :templateId")
    suspend fun incrementUsage(templateId: String)
}
```

- [ ] **AIRecommendationDao.kt** 생성 (8개 메서드)
```kotlin
@Dao
interface AIRecommendationDao {
    @Query("SELECT * FROM ai_recommendation ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): AIRecommendation?
    
    @Query("SELECT * FROM ai_recommendation WHERE accepted = 1 ORDER BY acceptedAt DESC")
    fun getAccepted(): Flow<List<AIRecommendation>>
    
    @Insert
    suspend fun insert(recommendation: AIRecommendation)
    
    @Update
    suspend fun update(recommendation: AIRecommendation)
    
    @Query("UPDATE ai_recommendation SET accepted = 1, acceptedAt = :timestamp WHERE id = :id")
    suspend fun markAccepted(id: String, timestamp: Long = System.currentTimeMillis())
}
```

- [ ] **ScheduleGroupHistoryDao.kt** 생성 (10개 메서드)
```kotlin
@Dao
interface ScheduleGroupHistoryDao {
    @Query("SELECT * FROM schedule_group_history WHERE scheduleGroupId = :groupId")
    fun getHistory(groupId: String): Flow<List<ScheduleGroupHistory>>
    
    @Query("SELECT * FROM schedule_group_history WHERE scheduleGroupId = :groupId AND activeTo IS NULL")
    suspend fun getCurrentActive(groupId: String): ScheduleGroupHistory?
    
    @Insert
    suspend fun insert(history: ScheduleGroupHistory)
    
    @Update
    suspend fun update(history: ScheduleGroupHistory)
    
    @Query("UPDATE schedule_group_history SET activeTo = :timestamp WHERE scheduleGroupId = :groupId AND activeTo IS NULL")
    suspend fun endCurrentActive(groupId: String, timestamp: Long = System.currentTimeMillis())
}
```

- [ ] **ABTestConfigDao.kt** 생성 (8개 메서드)
```kotlin
@Dao
interface ABTestConfigDao {
    @Query("SELECT * FROM ab_test_config WHERE isCompleted = 0")
    fun getActiveTests(): Flow<List<ABTestConfig>>
    
    @Query("SELECT * FROM ab_test_config WHERE id = :testId")
    suspend fun getById(testId: String): ABTestConfig?
    
    @Insert
    suspend fun insert(config: ABTestConfig)
    
    @Update
    suspend fun update(config: ABTestConfig)
    
    @Query("UPDATE ab_test_config SET isCompleted = 1, winnerId = :winnerId WHERE id = :testId")
    suspend fun completeTest(testId: String, winnerId: String)
}
```

#### 1.1.4 Repository 구현

- [ ] **ScheduleTemplateRepository.kt** 생성 (~150줄)
- [ ] **AIRecommendationRepository.kt** 생성 (~100줄)
- [ ] **ScheduleGroupHistoryRepository.kt** 생성 (~150줄)
- [ ] **ABTestRepository.kt** 생성 (~120줄)
- [ ] Hilt 모듈 업데이트 (4개 Repository 제공)

#### 1.1.5 기본 템플릿 데이터 준비

- [ ] **DefaultTemplates.kt** 생성 - 10개 템플릿 정의
```kotlin
object DefaultTemplates {
    fun getAll(): List<ScheduleTemplate> = listOf(
        // 1. 직장인 업무 집중
        ScheduleTemplate(
            id = "template_work_focus",
            name = "직장인 업무 집중",
            category = "WORK",
            description = "업무 시간 중 3회 집중으로 생산성 극대화",
            iconType = "WORK",
            colorHex = "#4CAF50",
            timeSlotsJson = """[
                {"hour":10,"minute":0,"duration":45,"preset":"FULL_BLOCK","days":["MON","TUE","WED","THU","FRI"]},
                {"hour":14,"minute":0,"duration":30,"preset":"STANDARD","days":["MON","TUE","WED","THU","FRI"]},
                {"hour":16,"minute":0,"duration":45,"preset":"FULL_BLOCK","days":["MON","TUE","WED","THU","FRI"]}
            ]""",
            isDefault = true,
            createdAt = System.currentTimeMillis()
        ),
        // 2-10. 나머지 템플릿...
    )
}
```

- [ ] **DatabaseInitializer.kt** - 앱 최초 실행 시 템플릿 삽입 로직

#### 1.1.6 마이그레이션 테스트

- [ ] **MigrationTest_5_6.kt** 작성
- [ ] 4개 테이블 생성 검증
- [ ] 외래 키 제약 조건 검증
- [ ] 기본 템플릿 삽입 검증

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_1.md`

---

## 2. 시간표 템플릿 기능 (Day 3-7)

### 2.1 템플릿 선택 UI

#### 2.1.1 TemplateSelectionScreen

- [ ] **TemplateSelectionScreen.kt** 생성 (~300줄)
```kotlin
@Composable
fun TemplateSelectionScreen(
    onBack: () -> Unit,
    onTemplateSelected: (ScheduleTemplate) -> Unit,
    onCreateFromScratch: () -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시간표 템플릿 선택") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(...) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // 카테고리 필터
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategoryChange = viewModel::selectCategory
            )
            
            // 템플릿 리스트
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onSelect = { onTemplateSelected(template) },
                        onPreview = { viewModel.showPreview(template) }
                    )
                }
                
                item {
                    CreateFromScratchCard(onClick = onCreateFromScratch)
                }
            }
        }
    }
}
```

#### 2.1.2 TemplateCard 컴포넌트

- [ ] **TemplateCard.kt** 생성 (~150줄)
```kotlin
@Composable
fun TemplateCard(
    template: ScheduleTemplate,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPreview
    ) {
        Column(Modifier.padding(16.dp)) {
            // 헤더: 아이콘 + 이름
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getIconForType(template.iconType),
                    contentDescription = null,
                    tint = Color(parseColor(template.colorHex)),
                    modifier = Modifier.size(40.dp)
                )
                
                Column(Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 시간대 요약
            val timeSlots = parseTimeSlots(template.timeSlotsJson)
            Text(
                text = "${timeSlots.size}회 집중, 총 ${timeSlots.sumOf { it.duration }}분",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // 사용자 수 (커뮤니티)
            if (template.usageCount > 0) {
                Text(
                    text = "${template.usageCount}명 사용 중",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 액션 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPreview, Modifier.weight(1f)) {
                    Text("미리보기")
                }
                Button(onClick = onSelect, Modifier.weight(1f)) {
                    Text("이 템플릿 사용")
                }
            }
        }
    }
}
```

#### 2.1.3 TemplatePreviewDialog

- [ ] **TemplatePreviewDialog.kt** 생성 (~200줄)
```kotlin
@Composable
fun TemplatePreviewDialog(
    template: ScheduleTemplate,
    onDismiss: () -> Unit,
    onUse: () -> Unit,
    onCustomize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 시간대 리스트
                val timeSlots = parseTimeSlots(template.timeSlotsJson)
                timeSlots.forEach { slot ->
                    TimeSlotPreviewCard(slot)
                    Spacer(Modifier.height(8.dp))
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 타임라인 시각화
                TimelineVisualization(timeSlots)
                
                Spacer(Modifier.height(16.dp))
                
                // 예상 효과
                ExpectedImpactCard(
                    totalMinutes = timeSlots.sumOf { it.duration },
                    sessionCount = timeSlots.size
                )
            }
        },
        confirmButton = {
            Button(onClick = onUse) {
                Text("바로 사용")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCustomize) {
                    Text("커스터마이징")
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}
```

### 2.2 템플릿 적용 로직

#### 2.2.1 TemplateViewModel

- [ ] **TemplateViewModel.kt** 생성 (~250줄)
```kotlin
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: ScheduleTemplateRepository,
    private val scheduleGroupRepository: ScheduleGroupRepository,
    private val timeBasedAutoRunRepository: TimeBasedAutoRunRepository
) : ViewModel() {
    
    private val _selectedCategory = MutableStateFlow<TemplateCategory?>(null)
    val selectedCategory: StateFlow<TemplateCategory?> = _selectedCategory.asStateFlow()
    
    val templates: StateFlow<List<ScheduleTemplate>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                templateRepository.getDefaultTemplates()
            } else {
                templateRepository.getByCategory(category.name)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun selectCategory(category: TemplateCategory?) {
        _selectedCategory.value = category
    }
    
    /**
     * 템플릿을 ScheduleGroup + TimeBasedAutoRun으로 변환하여 저장
     */
    suspend fun applyTemplate(
        template: ScheduleTemplate,
        customName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. ScheduleGroup 생성
            val scheduleGroup = ScheduleGroup(
                id = UUID.randomUUID().toString(),
                name = customName ?: template.name,
                description = template.description,
                isActive = false,
                linkedLocationId = null,
                iconType = template.iconType,
                colorHex = template.colorHex,
                createdAt = System.currentTimeMillis()
            )
            scheduleGroupRepository.insert(scheduleGroup)
            
            // 2. TimeBasedAutoRun 생성
            val timeSlots = Json.decodeFromString<List<TemplateTimeSlot>>(template.timeSlotsJson)
            timeSlots.forEach { slot ->
                val autoRun = TimeBasedAutoRun(
                    id = UUID.randomUUID().toString(),
                    hour = slot.hour,
                    minute = slot.minute,
                    durationMinutes = slot.durationMinutes,
                    presetType = slot.presetType,
                    scheduleGroupId = scheduleGroup.id,
                    isIndependent = false,
                    isEnabled = false,
                    enabledDays = slot.enabledDays
                )
                timeBasedAutoRunRepository.insert(autoRun)
            }
            
            // 3. 템플릿 사용 횟수 증가
            templateRepository.incrementUsage(template.id)
            
            scheduleGroup.id  // 반환
        }
    }
}
```

#### 2.2.2 템플릿 커스터마이징 화면

- [ ] **TemplateCustomizationScreen.kt** 생성 (~300줄)
- 기존 `TimeBasedAutoRunScreen` UI 재사용
- 템플릿 시간대를 편집 가능한 상태로 로드
- "저장" 시 `applyTemplate()` 호출

### 2.3 템플릿 관리

#### 2.3.1 사용자 정의 템플릿 저장

- [ ] **TemplateViewModel**에 `saveAsTemplate()` 메서드 추가
```kotlin
suspend fun saveAsTemplate(
    scheduleGroup: ScheduleGroup,
    timeSlots: List<TimeBasedAutoRun>
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val templateTimeSlots = timeSlots.map { slot ->
            TemplateTimeSlot(
                hour = slot.hour,
                minute = slot.minute,
                durationMinutes = slot.durationMinutes,
                presetType = slot.presetType,
                enabledDays = slot.enabledDays,
                label = slot.label
            )
        }
        
        val template = ScheduleTemplate(
            id = UUID.randomUUID().toString(),
            name = scheduleGroup.name,
            category = "CUSTOM",
            description = scheduleGroup.description ?: "",
            iconType = scheduleGroup.iconType,
            colorHex = scheduleGroup.colorHex,
            timeSlotsJson = Json.encodeToString(templateTimeSlots),
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
        
        templateRepository.insert(template)
    }
}
```

#### 2.3.2 내 템플릿 관리 화면

- [ ] **MyTemplatesScreen.kt** 생성 (~200줄)
- 사용자 정의 템플릿 리스트
- 편집/삭제 기능
- 공유 기능 (3.5 Export)

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_2.md`

---

## 3. AI 기반 스케줄 추천 (Day 8-14)

### 3.1 패턴 분석 로직

#### 3.1.1 ScheduleRecommender 클래스

- [ ] **ScheduleRecommender.kt** 생성 (~500줄)
```kotlin
@Singleton
class ScheduleRecommender @Inject constructor(
    private val focusSessionDao: FocusSessionDao,
    private val aiRecommendationRepository: AIRecommendationRepository
) {
    
    /**
     * 최근 30일 세션 분석
     */
    suspend fun analyzePattern(): Result<UserFocusPattern> = withContext(Dispatchers.IO) {
        runCatching {
            val thirtyDaysAgo = System.currentTimeMillis() - 30.days.inWholeMilliseconds
            val sessions = focusSessionDao.getSessionsSince(thirtyDaysAgo)
            
            if (sessions.size < MIN_SESSIONS_REQUIRED) {
                throw InsufficientDataException("최소 ${MIN_SESSIONS_REQUIRED}회 세션 필요")
            }
            
            // 1. 시간대별 집중도
            val hourlyScore = calculateHourlyScore(sessions)
            
            // 2. 요일별 집중도
            val dailyScore = calculateDailyScore(sessions)
            
            // 3. 선호 집중 시간
            val preferredDurations = calculatePreferredDurations(sessions)
            
            // 4. 최고 성과 시간대
            val bestTimeSlots = hourlyScore
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            
            // 5. 평균 지표
            val avgDailyMinutes = calculateAvgDailyMinutes(sessions)
            val avgWeeklyCount = sessions.size * 7 / 30
            
            // 6. 최적 간격
            val optimalInterval = calculateOptimalInterval(sessions)
            
            UserFocusPattern(
                hourlyFocusScore = hourlyScore,
                dailyFocusScore = dailyScore,
                preferredDurations = preferredDurations,
                bestTimeSlots = bestTimeSlots,
                avgDailyFocusMinutes = avgDailyMinutes,
                avgWeeklySessionCount = avgWeeklyCount,
                optimalInterval = optimalInterval
            )
        }
    }
    
    /**
     * 시간대별 집중도 계산 (완주율 기반)
     */
    private fun calculateHourlyScore(sessions: List<FocusSession>): Map<Int, Float> {
        return sessions
            .groupBy { LocalDateTime.ofInstant(Instant.ofEpochMilli(it.startTime), ZoneId.systemDefault()).hour }
            .mapValues { (_, sessions) ->
                val completedSessions = sessions.count { it.completionRate >= 0.8f }
                completedSessions.toFloat() / sessions.size
            }
    }
    
    /**
     * 추천 시간표 생성
     */
    suspend fun recommendSchedule(pattern: UserFocusPattern): Result<RecommendedSchedule> = 
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 최고 성과 시간대 선택 (Top 3)
                val topHours = pattern.bestTimeSlots.take(3).map { it.first }
                
                // 2. 시간대 간격 조정
                val adjustedHours = adjustSpacing(topHours, pattern.optimalInterval)
                
                // 3. 각 시간대 추천 생성
                val timeSlots = adjustedHours.map { hour ->
                    generateRecommendedSlot(hour, pattern)
                }
                
                // 4. 추천 이유 생성
                val reasons = generateReasons(pattern, adjustedHours)
                
                // 5. 신뢰도 계산
                val confidence = calculateConfidence(pattern)
                
                // 6. 예상 효과
                val expectedMinutes = timeSlots.sumOf { it.durationMinutes }
                val impact = "일일 집중 시간 +${expectedMinutes - pattern.avgDailyFocusMinutes}분 예상"
                
                RecommendedSchedule(
                    name = "AI 추천 시간표",
                    timeSlots = timeSlots,
                    reasons = reasons,
                    confidence = confidence,
                    expectedImpact = impact
                )
            }
        }
    
    companion object {
        private const val MIN_SESSIONS_REQUIRED = 10
        private const val MIN_DAYS_REQUIRED = 7
    }
}
```

#### 3.1.2 데이터 모델

- [ ] **UserFocusPattern.kt** 생성
```kotlin
data class UserFocusPattern(
    val hourlyFocusScore: Map<Int, Float>,
    val dailyFocusScore: Map<DayOfWeek, Float>,
    val preferredDurations: List<Int>,
    val bestTimeSlots: List<Pair<Int, Float>>,
    val avgDailyFocusMinutes: Int,
    val avgWeeklySessionCount: Int,
    val optimalInterval: Int
)

data class RecommendedSchedule(
    val name: String,
    val timeSlots: List<RecommendedTimeSlot>,
    val reasons: List<String>,
    val confidence: Float,
    val expectedImpact: String
)

data class RecommendedTimeSlot(
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
    val presetType: String,
    val enabledDays: List<DayOfWeek>,
    val reason: String
)
```

### 3.2 AI 추천 UI

#### 3.2.1 AIRecommendationScreen

- [ ] **AIRecommendationScreen.kt** 생성 (~400줄)
```kotlin
@Composable
fun AIRecommendationScreen(
    onBack: () -> Unit,
    onRecommendationAccepted: (RecommendedSchedule) -> Unit,
    viewModel: AIRecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 시간표 추천") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(...) } }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AIRecommendationUiState.Analyzing -> {
                AnalyzingScreen(progress = state.progress)
            }
            is AIRecommendationUiState.Success -> {
                RecommendationResultScreen(
                    recommendation = state.recommendation,
                    pattern = state.pattern,
                    onAccept = { onRecommendationAccepted(state.recommendation) },
                    onRetry = viewModel::analyze
                )
            }
            is AIRecommendationUiState.InsufficientData -> {
                InsufficientDataScreen(
                    requiredSessions = state.requiredSessions,
                    currentSessions = state.currentSessions,
                    onUseTemplate = { /* ... */ }
                )
            }
            is AIRecommendationUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = viewModel::analyze
                )
            }
        }
    }
}
```

#### 3.2.2 RecommendationResultScreen

- [ ] **RecommendationResultScreen.kt** 생성 (~300줄)
```kotlin
@Composable
fun RecommendationResultScreen(
    recommendation: RecommendedSchedule,
    pattern: UserFocusPattern,
    onAccept: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 헤더
        Text(
            text = "당신에게 추천하는 시간표",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // 신뢰도 배지
        ConfidenceBadge(confidence = recommendation.confidence)
        
        // 추천 시간대 리스트
        recommendation.timeSlots.forEach { slot ->
            RecommendedTimeSlotCard(slot)
        }
        
        HorizontalDivider()
        
        // 추천 이유
        ReasonCard(
            title = "이렇게 추천한 이유",
            reasons = recommendation.reasons
        )
        
        // 예상 효과
        ExpectedImpactCard(impact = recommendation.expectedImpact)
        
        // 액션 버튼
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRetry, Modifier.weight(1f)) {
                Text("다시 추천받기")
            }
            Button(onClick = onAccept, Modifier.weight(1f)) {
                Text("이 시간표 사용")
            }
        }
    }
}
```

### 3.3 AIRecommendationViewModel

- [ ] **AIRecommendationViewModel.kt** 생성 (~300줄)
```kotlin
@HiltViewModel
class AIRecommendationViewModel @Inject constructor(
    private val recommender: ScheduleRecommender,
    private val repository: AIRecommendationRepository,
    private val scheduleGroupRepository: ScheduleGroupRepository,
    private val timeBasedAutoRunRepository: TimeBasedAutoRunRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AIRecommendationUiState>(AIRecommendationUiState.Idle)
    val uiState: StateFlow<AIRecommendationUiState> = _uiState.asStateFlow()
    
    fun analyze() {
        viewModelScope.launch {
            _uiState.value = AIRecommendationUiState.Analyzing(0.0f)
            
            // 1. 패턴 분석
            delay(500)
            _uiState.value = AIRecommendationUiState.Analyzing(0.3f)
            
            val patternResult = recommender.analyzePattern()
            if (patternResult.isFailure) {
                val error = patternResult.exceptionOrNull()
                if (error is InsufficientDataException) {
                    _uiState.value = AIRecommendationUiState.InsufficientData(
                        requiredSessions = 10,
                        currentSessions = 5  // TODO: 실제 값
                    )
                } else {
                    _uiState.value = AIRecommendationUiState.Error(error?.message ?: "분석 실패")
                }
                return@launch
            }
            
            val pattern = patternResult.getOrThrow()
            
            // 2. 추천 생성
            delay(500)
            _uiState.value = AIRecommendationUiState.Analyzing(0.7f)
            
            val recommendationResult = recommender.recommendSchedule(pattern)
            if (recommendationResult.isFailure) {
                _uiState.value = AIRecommendationUiState.Error("추천 생성 실패")
                return@launch
            }
            
            val recommendation = recommendationResult.getOrThrow()
            
            // 3. 저장
            val aiRecommendation = AIRecommendation(
                id = UUID.randomUUID().toString(),
                patternJson = Json.encodeToString(pattern),
                recommendedScheduleJson = Json.encodeToString(recommendation),
                confidence = recommendation.confidence,
                createdAt = System.currentTimeMillis()
            )
            repository.insert(aiRecommendation)
            
            delay(300)
            _uiState.value = AIRecommendationUiState.Success(recommendation, pattern)
        }
    }
    
    suspend fun acceptRecommendation(recommendation: RecommendedSchedule): Result<String> {
        // ScheduleGroup + TimeBasedAutoRun 생성 (템플릿 적용과 동일)
        return runCatching {
            val scheduleGroup = ScheduleGroup(/* ... */)
            scheduleGroupRepository.insert(scheduleGroup)
            
            recommendation.timeSlots.forEach { slot ->
                val autoRun = TimeBasedAutoRun(/* ... */)
                timeBasedAutoRunRepository.insert(autoRun)
            }
            
            // AIRecommendation 수락 표시
            val latest = repository.getLatest()
            if (latest != null) {
                repository.markAccepted(latest.id)
            }
            
            scheduleGroup.id
        }
    }
}
```

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_3.md`

---

## 4. 시간표 Export/Import (Day 15-16)

### 4.1 Export 기능

#### 4.1.1 JSON 직렬화

- [ ] **ScheduleExporter.kt** 생성 (~150줄)
```kotlin
@Singleton
class ScheduleExporter @Inject constructor() {
    
    fun exportToJson(
        scheduleGroup: ScheduleGroup,
        timeSlots: List<TimeBasedAutoRun>
    ): String {
        val exportData = ScheduleExportData(
            version = "1.0",
            scheduleGroup = scheduleGroup,
            timeSlots = timeSlots.map { it.toExportFormat() },
            metadata = ExportMetadata(
                createdAt = System.currentTimeMillis(),
                author = "anonymous",
                source = "manual"
            )
        )
        
        return Json.encodeToString(exportData)
    }
    
    fun exportToFile(
        context: Context,
        scheduleGroup: ScheduleGroup,
        timeSlots: List<TimeBasedAutoRun>
    ): File {
        val json = exportToJson(scheduleGroup, timeSlots)
        val fileName = "${scheduleGroup.name}_${System.currentTimeMillis()}.json"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(json)
        return file
    }
    
    fun generateQRCode(json: String): Bitmap {
        // ZXing 라이브러리 사용
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(json, BarcodeFormat.QR_CODE, 512, 512)
        // Bitmap 생성...
    }
}

@Serializable
data class ScheduleExportData(
    val version: String,
    val scheduleGroup: ScheduleGroup,
    val timeSlots: List<TimeSlotExport>,
    val metadata: ExportMetadata
)
```

#### 4.1.2 공유 UI

- [ ] **ShareScheduleDialog.kt** 생성 (~150줄)
```kotlin
@Composable
fun ShareScheduleDialog(
    scheduleGroup: ScheduleGroup,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    var shareMethod by remember { mutableStateOf<ShareMethod?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간표 공유") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShareMethodButton(
                    icon = Icons.Default.SaveAlt,
                    text = "파일로 저장",
                    onClick = { shareMethod = ShareMethod.FILE }
                )
                ShareMethodButton(
                    icon = Icons.Default.QrCode,
                    text = "QR 코드 생성",
                    onClick = { shareMethod = ShareMethod.QR_CODE }
                )
                ShareMethodButton(
                    icon = Icons.Default.ContentCopy,
                    text = "클립보드 복사",
                    onClick = { shareMethod = ShareMethod.CLIPBOARD }
                )
                ShareMethodButton(
                    icon = Icons.Default.Share,
                    text = "앱으로 공유",
                    onClick = { shareMethod = ShareMethod.SHARE_INTENT }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
    
    // 실제 공유 처리
    LaunchedEffect(shareMethod) {
        shareMethod?.let { method ->
            viewModel.share(scheduleGroup, method)
        }
    }
}
```

### 4.2 Import 기능

#### 4.2.1 JSON 파싱 및 검증

- [ ] **ScheduleImporter.kt** 생성 (~200줄)
```kotlin
@Singleton
class ScheduleImporter @Inject constructor(
    private val scheduleGroupRepository: ScheduleGroupRepository,
    private val timeBasedAutoRunRepository: TimeBasedAutoRunRepository
) {
    
    suspend fun importFromJson(json: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. JSON 파싱
            val exportData = Json.decodeFromString<ScheduleExportData>(json)
            
            // 2. 스키마 검증
            validateSchema(exportData)
            
            // 3. ScheduleGroup 생성 (새 ID)
            val newGroup = exportData.scheduleGroup.copy(
                id = UUID.randomUUID().toString(),
                isActive = false,
                linkedLocationId = null,
                createdAt = System.currentTimeMillis()
            )
            scheduleGroupRepository.insert(newGroup)
            
            // 4. TimeBasedAutoRun 생성
            exportData.timeSlots.forEach { slot ->
                val autoRun = TimeBasedAutoRun(
                    id = UUID.randomUUID().toString(),
                    hour = slot.hour,
                    minute = slot.minute,
                    durationMinutes = slot.durationMinutes,
                    presetType = slot.presetType,
                    scheduleGroupId = newGroup.id,
                    isIndependent = false,
                    isEnabled = false,
                    enabledDays = slot.enabledDays
                )
                timeBasedAutoRunRepository.insert(autoRun)
            }
            
            newGroup.id
        }
    }
    
    suspend fun importFromFile(file: File): Result<String> {
        return importFromJson(file.readText())
    }
    
    suspend fun importFromQRCode(bitmap: Bitmap): Result<String> {
        // ZXing으로 QR 코드 디코딩
        val json = decodeQRCode(bitmap)
        return importFromJson(json)
    }
    
    private fun validateSchema(data: ScheduleExportData) {
        require(data.version == "1.0") { "지원하지 않는 버전입니다" }
        require(data.timeSlots.isNotEmpty()) { "시간대가 없습니다" }
    }
}
```

#### 4.2.2 Import UI

- [ ] **ImportScheduleScreen.kt** 생성 (~250줄)
```kotlin
@Composable
fun ImportScheduleScreen(
    onBack: () -> Unit,
    onImportSuccess: (String) -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 파일 선택 런처
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFromFile(it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시간표 가져오기") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(...) } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 가져오기 방법 선택
            ImportMethodCard(
                icon = Icons.Default.FileUpload,
                title = "파일에서 가져오기",
                description = "JSON 파일 선택",
                onClick = { fileLauncher.launch("application/json") }
            )
            
            ImportMethodCard(
                icon = Icons.Default.QrCodeScanner,
                title = "QR 코드 스캔",
                description = "카메라로 QR 코드 스캔",
                onClick = { /* QR 스캔 화면 */ }
            )
            
            ImportMethodCard(
                icon = Icons.Default.ContentPaste,
                title = "클립보드에서 가져오기",
                description = "복사한 JSON 데이터 사용",
                onClick = { viewModel.importFromClipboard() }
            )
            
            // 상태 표시
            when (val state = uiState) {
                is ImportUiState.Success -> {
                    SuccessCard(
                        message = "시간표를 가져왔습니다",
                        onConfirm = { onImportSuccess(state.scheduleGroupId) }
                    )
                }
                is ImportUiState.Error -> {
                    ErrorCard(message = state.message)
                }
                else -> {}
            }
        }
    }
}
```

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_4.md`

---

## 5. 성과 추적 및 A/B 테스트 (Day 17-18)

### 5.1 히스토리 추적

#### 5.1.1 히스토리 자동 기록

- [ ] **ScheduleGroupManager** 수정 - activateGroup/deactivateGroup 시 히스토리 자동 생성
```kotlin
suspend fun activateGroup(groupId: String): Result<Unit> {
    // ... 기존 코드
    
    // 히스토리 시작 기록
    historyRepository.endCurrentActive(groupId)  // 이전 활성 종료
    historyRepository.insert(
        ScheduleGroupHistory(
            id = UUID.randomUUID().toString(),
            scheduleGroupId = groupId,
            activeFrom = System.currentTimeMillis()
        )
    )
}
```

#### 5.1.2 성과 통계 UI

- [ ] **SchedulePerformanceScreen.kt** 생성 (~400줄)
```kotlin
@Composable
fun SchedulePerformanceScreen(
    scheduleGroupId: String,
    onBack: () -> Unit,
    viewModel: PerformanceViewModel = hiltViewModel()
) {
    val performance by viewModel.getPerformance(scheduleGroupId).collectAsState()
    
    // 차트 라이브러리 (MPAndroidChart) 사용
    // - 일별 완주율 라인 그래프
    // - 요일별 성과 막대 그래프
    // - 총 집중 시간 누적 영역 그래프
}
```

### 5.2 A/B 테스트 (선택)

#### 5.2.1 ABTestManager

- [ ] **ABTestManager.kt** 생성 (~200줄)
```kotlin
@Singleton
class ABTestManager @Inject constructor(
    private val repository: ABTestRepository,
    private val scheduleManager: ScheduleGroupManager
) {
    
    suspend fun startTest(
        scheduleGroupA: String,
        scheduleGroupB: String,
        durationDays: Int,
        switchStrategy: SwitchStrategy
    ): Result<String> {
        // 테스트 설정 생성 및 첫 시간표 활성화
    }
    
    suspend fun checkAndSwitch() {
        // 매일 체크하여 전환 전략에 따라 시간표 전환
    }
    
    suspend fun completeTest(testId: String): Result<ABTestResult> {
        // 두 시간표 성과 비교 및 승자 결정
    }
}
```

#### 5.2.2 A/B 테스트 UI

- [ ] **ABTestSetupScreen.kt** 생성 (~250줄)
- [ ] **ABTestProgressScreen.kt** 생성 (~200줄)
- [ ] **ABTestResultScreen.kt** 생성 (~300줄)

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_5.md`

---

## 6. 커뮤니티 기능 (Day 19-20, 선택)

### 6.1 Firebase Firestore 연동

- [ ] build.gradle에 Firebase Firestore 의존성 추가
- [ ] **CommunityScheduleRepository.kt** 생성 (~300줄)

### 6.2 커뮤니티 탐색 UI

- [ ] **CommunityScheduleScreen.kt** 생성 (~400줄)
- [ ] **CommunityScheduleDetailScreen.kt** 생성 (~300줄)

### 6.3 리뷰 및 평점 시스템

- [ ] **CommunityReviewDialog.kt** 생성 (~150줄)

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_6.md` (선택)

---

## 7. 테스트 및 QA (Day 21)

### 7.1 단위 테스트

- [ ] **ScheduleRecommenderTest.kt** - AI 분석 로직
- [ ] **ScheduleExporterTest.kt** - JSON 직렬화
- [ ] **ScheduleImporterTest.kt** - JSON 파싱 및 검증

### 7.2 통합 테스트

- [ ] **TemplateE2ETest.kt** - 템플릿 선택부터 적용까지
- [ ] **AIRecommendationE2ETest.kt** - 분석부터 수락까지

### 7.3 QA 시나리오 (20개)

#### 템플릿 (7개)
1. 템플릿 선택 및 즉시 적용
2. 템플릿 미리보기 및 커스터마이징
3. 여러 카테고리 템플릿 탐색
4. 사용자 정의 템플릿 저장
5. 템플릿 공유 (파일)
6. 템플릿 가져오기 (파일)
7. 템플릿 가져오기 (QR 코드)

#### AI 추천 (6개)
1. 충분한 데이터로 추천 받기
2. 불충분한 데이터 처리
3. 추천 수락 및 시간표 생성
4. 추천 거절 후 다시 추천
5. 추천 시간표 커스터마이징
6. 추천 이유 및 신뢰도 확인

#### 성과 추적 (4개)
1. 시간표 히스토리 확인
2. 일별 완주율 그래프
3. 요일별 성과 비교
4. 총 집중 시간 누적 확인

#### A/B 테스트 (3개, 선택)
1. 테스트 설정 및 시작
2. 진행 중 상태 확인
3. 결과 확인 및 승자 선택

### 7.4 빌드 검증

- [ ] `./gradlew clean assembleDebug`
- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] APK 크기 확인 (< 18MB 목표)

**작업 기록**: `working_history/2025-11-XX_3rd_advanced_7.md`

---

## 8. 문서화 및 배포 준비

### 8.1 문서 업데이트

- [ ] `RELEASE_NOTES_v0.8.md` 작성
- [ ] `README.md` 업데이트 (템플릿, AI 추천 가이드)
- [ ] `docs/03_migration_guide_v7_to_v8.md` 작성

### 8.2 최종 체크리스트

- [ ] PRD 요구사항 100% 구현
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] QA 시나리오 20개 모두 통과
- [ ] 성공 지표 달성 가능성 확인
- [ ] 문서 최신화
- [ ] 내부 베타 테스트 (10명 이상)

---

## 9. 산출물 체크리스트

- [ ] `docs/03_template_ai_schedule_prd.md`
- [ ] `docs/03_template_ai_schedule_todolist.md`
- [ ] `docs/RELEASE_NOTES_v0.8.md`
- [ ] `docs/03_migration_guide_v7_to_v8.md`
- [ ] `working_history/2025-11-XX_3rd_advanced_*.md` (7개 예상)
- [ ] 신규 파일 20개 이상
- [ ] 수정 파일 10개 이상
- [ ] 테스트 파일 5개 이상

---

## 10. 참조 문서

- [4차 고도화 PRD](./03_template_ai_schedule_prd.md)
- [2.5차 고도화 PRD](./02_advanced_autosetting_prd.md)
- [2.5차 고도화 작업계획](./02.5_complex_time&location_todolist.md)
- [2차 고도화 PRD](./02_advanced_autosetting_prd.md)
- [2차 고도화 작업계획](./02_advanced_autosetting_todolist.md)

---

> **4차 고도화 철학**: "지능형 자동화". 템플릿으로 즉시 시작하고, AI가 패턴을 학습하여 최적화합니다. "생각 없이 시작, 지능적으로 최적화"를 실현합니다.

