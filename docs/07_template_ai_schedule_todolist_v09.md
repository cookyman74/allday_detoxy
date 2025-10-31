# Allday Detoxy 4차 고도화 작업 계획 (v0.9 기반)
## 시간표 템플릿 및 AI 기반 스케줄 추천

> **Note**: 본 문서는 3차 고도화(v0.8) 완료 후 작성된 4차 고도화 작업 계획입니다.
> 기존 4차 고도화 작업계획은 `04_template_ai_schedule_todolist.md`를 참조하세요.

- **기준 문서**: [4차 고도화 PRD (v0.9 기반)](./07_template_ai_schedule_prd_v09.md)
- **선행 작업**: 3차 고도화 (v0.8) 완료 필수 ✅
- **버전**: v0.8 → v0.9
- **예상 기간**: 3주 (Day 1-21)
- **최종 수정일**: 2025-10-31

---

## 0. 개요

### 0.1 배경 및 목표

**배경**:
- **3차 고도화 (v0.8) 완료 (2025-10-31)** ✅:
  - 위치-시간표 복합 시나리오 구현
  - QuickCreateScheduleDialog 기본 생성 기능 구현
  - 사용자 흐름 개선 (위치 설정 중 바로 시간표 생성 가능)
- 사용자 피드백: 시간표 생성 진입 장벽 여전히 존재 (템플릿 부재)
- 최적 시간대 설정에 대한 가이드 부족

**목표**:
- 10개 사전 정의 템플릿 제공으로 즉시 시작 가능
- AI 기반 패턴 분석으로 개인 맞춤 추천
- 시간표 설정 완료 시간 2분 이내 단축
- 템플릿 사용률 70%, AI 추천 수락률 40% 달성

### 0.2 핵심 기능

1. **QuickCreateScheduleDialog 확장** (v0.8 기반) 🆕
2. **시간표 템플릿** (10개 사전 정의)
3. **AI 패턴 분석 및 추천**
4. **시간표 Export/Import (JSON)**
5. **성과 추적 및 히스토리**
6. **A/B 테스트** (선택)
7. **커뮤니티 시간표** (선택)

### 0.3 성공 지표

- 템플릿 사용률 ≥ 70%
- AI 추천 수락률 ≥ 40%
- 시간표 설정 완료 시간 ≤ 2분
- 시간표 활용률 ≥ 60% (7일 내)
- WAU +30% (v0.8 대비)

---

## 1. QuickCreateScheduleDialog 확장 (Day 1-2) 🆕

### 1.1 개요

**목적**: 3차 고도화에서 구현한 QuickCreateScheduleDialog를 확장하여 템플릿 및 AI 추천 기능 통합

**현재 상태 (v0.8)**:
```kotlin
QuickCreateScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
    locationLabel: String = ""
)
```

**목표 상태 (v0.9)**:
```kotlin
QuickCreateScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, templateId: String?, aiRecommended: Boolean) -> Unit,
    locationLabel: String = "",
    showTemplateOption: Boolean = true,  // 🆕
    showAIOption: Boolean = true  // 🆕
)
```

### 1.2 작업 내용

#### 1.2.1 QuickCreateScheduleDialog 확장

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/QuickCreateScheduleDialog.kt`

- [ ] 생성 옵션 추가: 빈 시간표 / 템플릿 / AI 추천
```kotlin
enum class ScheduleCreationMode {
    BLANK,      // 빈 시간표 (v0.8 기존)
    TEMPLATE,   // 템플릿 사용 (v0.9)
    AI_RECOMMENDED  // AI 추천 (v0.9)
}
```

- [ ] UI 확장
```kotlin
@Composable
fun QuickCreateScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mode: ScheduleCreationMode, templateId: String?) -> Unit,
    locationLabel: String = "",
    showTemplateOption: Boolean = true,
    showAIOption: Boolean = true
) {
    var name by remember { ... }
    var selectedMode by remember { mutableStateOf(ScheduleCreationMode.BLANK) }
    var showTemplateSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        title = { Text("시간표 만들기") },
        text = {
            Column {
                // 기존 안내 텍스트
                
                // 시간표 이름 입력
                OutlinedTextField(...)
                
                // 🆕 생성 모드 선택
                Text("시작 방법 선택")
                
                RadioButtonOption(
                    text = "빈 시간표로 시작",
                    selected = selectedMode == ScheduleCreationMode.BLANK,
                    onClick = { selectedMode = ScheduleCreationMode.BLANK }
                )
                
                if (showTemplateOption) {
                    RadioButtonOption(
                        text = "템플릿으로 시작",
                        selected = selectedMode == ScheduleCreationMode.TEMPLATE,
                        onClick = { 
                            selectedMode = ScheduleCreationMode.TEMPLATE
                            showTemplateSelector = true
                        }
                    )
                }
                
                if (showAIOption) {
                    RadioButtonOption(
                        text = "AI 추천받기",
                        selected = selectedMode == ScheduleCreationMode.AI_RECOMMENDED,
                        onClick = { selectedMode = ScheduleCreationMode.AI_RECOMMENDED }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(name, selectedMode, selectedTemplateId)
            }) {
                Text("만들기")
            }
        }
    )
    
    // 🆕 템플릿 선택 BottomSheet
    if (showTemplateSelector) {
        TemplateSelectionBottomSheet(
            onDismiss = { showTemplateSelector = false },
            onTemplateSelected = { template ->
                selectedTemplateId = template.id
                showTemplateSelector = false
            }
        )
    }
}
```

#### 1.2.2 TemplateSelectionBottomSheet 생성

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/TemplateSelectionBottomSheet.kt` (신규)

- [ ] BottomSheet UI 구현 (~200줄)
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionBottomSheet(
    onDismiss: () -> Unit,
    onTemplateSelected: (ScheduleTemplate) -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.defaultTemplates.collectAsStateWithLifecycle()
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "템플릿 선택",
                style = MaterialTheme.typography.titleLarge
            )
            
            LazyColumn {
                items(templates) { template ->
                    TemplateOptionCard(
                        template = template,
                        onClick = { onTemplateSelected(template) }
                    )
                }
            }
        }
    }
}
```

#### 1.2.3 AddLocationAutoRunDialog 통합

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/AddLocationAutoRunDialog.kt`

- [ ] QuickCreateScheduleDialog 호출 부분 수정
```kotlin
if (showQuickCreateDialog) {
    QuickCreateScheduleDialog(
        onDismiss = { showQuickCreateDialog = false },
        onConfirm = { scheduleName, mode, templateId ->
            scope.launch {
                val newGroupId = when (mode) {
                    ScheduleCreationMode.BLANK -> {
                        scheduleViewModel.createScheduleGroup(scheduleName, null)
                    }
                    ScheduleCreationMode.TEMPLATE -> {
                        templateId?.let { 
                            templateViewModel.applyTemplate(it, scheduleName)
                        } ?: scheduleViewModel.createScheduleGroup(scheduleName, null)
                    }
                    ScheduleCreationMode.AI_RECOMMENDED -> {
                        aiViewModel.analyzeAndCreate(scheduleName)
                    }
                }
                
                selectedScheduleGroupId = newGroupId
                enableScheduleLink = true
                activateOnEnter = true
                deactivateOnExit = true
                
                showQuickCreateDialog = false
            }
        },
        locationLabel = label,
        showTemplateOption = true,
        showAIOption = hasEnoughData  // AI 추천은 데이터 충분 시만
    )
}
```

### 1.3 빌드 검증

- [ ] `./gradlew compileDebugKotlin`
- [ ] Linter 검증
- [ ] UI 테스트

**작업 기록**: `working_history/2025-11-XX_4th_advanced_1.md`

---

## 2. 데이터 모델 설계 (Day 3-4)

(기존 04_template_ai_schedule_todolist.md의 §1 내용과 동일)

### 2.1 Room 마이그레이션 v6→v7

#### 2.1.1 신규 엔티티 정의

- [ ] **ScheduleTemplate.kt** 엔티티 생성
- [ ] **AIRecommendation.kt** 엔티티 생성
- [ ] **ScheduleGroupHistory.kt** 엔티티 생성
- [ ] **ABTestConfig.kt** 엔티티 생성

#### 2.1.2 마이그레이션 스크립트 작성

- [ ] **Migration_6_7.kt** 작성 (4개 테이블 생성)
- [ ] **DetoxyDatabase.kt** 버전 7로 업데이트
- [ ] **DatabaseModule.kt**에 MIGRATION_6_7 추가

#### 2.1.3 DAO 인터페이스 작성

- [ ] **ScheduleTemplateDao.kt** 생성
- [ ] **AIRecommendationDao.kt** 생성
- [ ] **ScheduleGroupHistoryDao.kt** 생성
- [ ] **ABTestConfigDao.kt** 생성

#### 2.1.4 Repository 구현

- [ ] **ScheduleTemplateRepository.kt** 생성
- [ ] **AIRecommendationRepository.kt** 생성
- [ ] **ScheduleGroupHistoryRepository.kt** 생성
- [ ] **ABTestRepository.kt** 생성

#### 2.1.5 기본 템플릿 데이터 준비

- [ ] **DefaultTemplates.kt** 생성 - 10개 템플릿 정의
- [ ] **DatabaseInitializer.kt** - 앱 최초 실행 시 템플릿 삽입

**작업 기록**: `working_history/2025-11-XX_4th_advanced_2.md`

---

## 3. 시간표 템플릿 기능 (Day 5-9)

### 3.1 템플릿 선택 UI

#### 3.1.1 TemplateViewModel

- [ ] **TemplateViewModel.kt** 생성 (~250줄)
- [ ] `applyTemplate()` 메서드 구현 (ScheduleGroup + TimeBasedAutoRun 생성)

#### 3.1.2 TemplateSelectionScreen (독립 화면)

- [ ] **TemplateSelectionScreen.kt** 생성 (~300줄)
- [ ] **TemplateCard.kt** 컴포넌트 생성
- [ ] **TemplatePreviewDialog.kt** 생성

#### 3.1.3 템플릿 관리

- [ ] **TemplateViewModel**에 `saveAsTemplate()` 추가
- [ ] **MyTemplatesScreen.kt** 생성 (~200줄)

**작업 기록**: `working_history/2025-11-XX_4th_advanced_3.md`

---

## 4. AI 기반 스케줄 추천 (Day 10-16)

### 4.1 패턴 분석 로직

#### 4.1.1 ScheduleRecommender 클래스

- [ ] **ScheduleRecommender.kt** 생성 (~500줄)
- [ ] `analyzePattern()` 메서드 구현
- [ ] `recommendSchedule()` 메서드 구현

#### 4.1.2 데이터 모델

- [ ] **UserFocusPattern.kt** 생성
- [ ] **RecommendedSchedule.kt** 생성
- [ ] **RecommendedTimeSlot.kt** 생성

### 4.2 AI 추천 UI

#### 4.2.1 AIRecommendationScreen

- [ ] **AIRecommendationScreen.kt** 생성 (~400줄)
- [ ] **RecommendationResultScreen.kt** 생성 (~300줄)
- [ ] **InsufficientDataScreen.kt** 생성 (~150줄)

#### 4.2.2 AIRecommendationViewModel

- [ ] **AIRecommendationViewModel.kt** 생성 (~300줄)
- [ ] `analyze()` 메서드 구현
- [ ] `acceptRecommendation()` 메서드 구현

### 4.3 QuickCreateScheduleDialog에서 AI 추천 호출

- [ ] AI 추천 옵션 선택 시 즉시 분석 시작
- [ ] 분석 완료 후 자동으로 시간표 생성
- [ ] 데이터 부족 시 템플릿 제안

**작업 기록**: `working_history/2025-11-XX_4th_advanced_4.md`

---

## 5. 시간표 Export/Import (Day 17-18)

### 5.1 Export 기능

- [ ] **ScheduleExporter.kt** 생성 (~150줄)
- [ ] **ShareScheduleDialog.kt** 생성 (~150줄)

### 5.2 Import 기능

- [ ] **ScheduleImporter.kt** 생성 (~200줄)
- [ ] **ImportScheduleScreen.kt** 생성 (~250줄)

**작업 기록**: `working_history/2025-11-XX_4th_advanced_5.md`

---

## 6. 성과 추적 및 A/B 테스트 (Day 19-20)

### 6.1 히스토리 추적

- [ ] **ScheduleGroupManager** 수정 - 히스토리 자동 기록
- [ ] **SchedulePerformanceScreen.kt** 생성 (~400줄)

### 6.2 A/B 테스트 (선택)

- [ ] **ABTestManager.kt** 생성 (~200줄)
- [ ] **ABTestSetupScreen.kt** 생성 (~250줄)
- [ ] **ABTestProgressScreen.kt** 생성 (~200줄)
- [ ] **ABTestResultScreen.kt** 생성 (~300줄)

**작업 기록**: `working_history/2025-11-XX_4th_advanced_6.md`

---

## 7. 통합 테스트 및 QA (Day 21)

### 7.1 QuickCreateScheduleDialog 통합 테스트

- [ ] 빈 시간표 생성 시나리오
- [ ] 템플릿 선택 시나리오
- [ ] AI 추천 시나리오
- [ ] 데이터 부족 시 fallback 시나리오

### 7.2 End-to-End 시나리오

1. **위치 + 템플릿 시간표 생성**:
   - [위치 기반] → "회사" 추가
   - SCHEDULE 단계 → "새 시간표 만들기"
   - "템플릿으로 시작" 선택
   - "직장인 업무 집중" 템플릿 선택
   - 자동으로 10시, 14시, 16시 시간대 생성
   - 완료 ✅

2. **위치 + AI 추천 시간표 생성**:
   - [위치 기반] → "도서관" 추가
   - SCHEDULE 단계 → "새 시간표 만들기"
   - "AI 추천받기" 선택
   - 분석 진행 → 추천 시간표 제시
   - 수락 → 자동 생성
   - 완료 ✅

3. **템플릿 커스터마이징**:
   - 템플릿 선택 후 시간대 수정
   - 요일 변경
   - 차단 강도 조정
   - 완료 ✅

### 7.3 빌드 검증

- [ ] `./gradlew clean assembleDebug`
- [ ] `./gradlew test`
- [ ] `./gradlew connectedAndroidTest`
- [ ] `./gradlew lint`
- [ ] APK 크기 확인

**작업 기록**: `working_history/2025-11-XX_4th_advanced_7.md`

---

## 8. 문서화 및 배포 준비

### 8.1 문서 업데이트

- [ ] `RELEASE_NOTES_v0.9.md` 작성
- [ ] `README.md` 업데이트
- [ ] `docs/08_migration_guide_v8_to_v9.md` 작성

### 8.2 최종 체크리스트

- [ ] PRD 요구사항 100% 구현
- [ ] QuickCreateScheduleDialog 확장 완료
- [ ] 템플릿 10개 정의 및 적용 가능
- [ ] AI 추천 정상 작동
- [ ] Export/Import 정상 작동
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] QA 시나리오 모두 통과
- [ ] 문서 최신화
- [ ] 내부 베타 테스트

---

## 9. 참조 문서

- [4차 고도화 PRD (v0.9 기반)](./07_template_ai_schedule_prd_v09.md)
- [4차 고도화 PRD (원본)](./04_template_ai_schedule_prd.md)
- [4차 고도화 작업계획 (원본)](./04_template_ai_schedule_todolist.md)
- [3차 고도화 작업 기록](../working_history/2025-10-31_3rd_advanced_3.4.md)
- [3차 고도화 PRD](./03_complex_time&location_todolist.md)

---

> **v0.9 작업 철학**: "3차 고도화에서 구축한 QuickCreateScheduleDialog를 기반으로, 최소한의 변경으로 템플릿과 AI 추천을 통합합니다. 기존 사용자 경험을 해치지 않으면서 새로운 옵션을 제공하는 점진적 개선을 추구합니다."

