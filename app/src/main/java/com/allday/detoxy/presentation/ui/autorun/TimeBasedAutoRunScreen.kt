package com.allday.detoxy.presentation.ui.autorun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.core.utils.ExactAlarmPermissionUtil
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.presentation.ui.autorun.components.*
import com.allday.detoxy.presentation.viewmodel.TimeBasedAutoRunViewModel

/**
 * 예약설정 화면 (시간 기반 자동 실행)
 *
 * 사용자가 특정 시간에 자동으로 타이머를 시작하도록 설정할 수 있는 화면입니다.
 * - 정확 알람 권한 경고 배너
 * - 템플릿 선택 버튼
 * - 예약 목록 (LazyColumn)
 * - 예약 추가 버튼
 * - 배터리 영향 안내 카드
 *
 * @param onBack 뒤로가기 콜백
 * @param onNavigateToLocationBased 위치 기반 자동 실행으로 이동 콜백
 * @param viewModel TimeBasedAutoRunViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBasedAutoRunScreen(
    onBack: () -> Unit = {},
    onNavigateToLocationBased: () -> Unit = {},
    viewModel: TimeBasedAutoRunViewModel = hiltViewModel()
) {
    val autoRuns by viewModel.autoRuns.collectAsStateWithLifecycle()
    val canScheduleExactAlarms by viewModel.canScheduleExactAlarms.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    
    // 🆕 3차 고도화: 시간표 그룹 맵
    val scheduleGroupMap by viewModel.scheduleGroupMap.collectAsStateWithLifecycle()
    
    // 글로벌 옵션 상태
    val excludeWeekends by viewModel.excludeWeekends.collectAsStateWithLifecycle()
    val autoStartDelayMinutes by viewModel.autoStartDelayMinutes.collectAsStateWithLifecycle()
    val preNotificationMinutes by viewModel.preNotificationMinutes.collectAsStateWithLifecycle()
    
    // 자동 실행 제어 상태
    val masterEnabled by viewModel.masterEnabled.collectAsStateWithLifecycle()
    val pauseUntil by viewModel.pauseUntil.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingAutoRun by remember { mutableStateOf<TimeBasedAutoRun?>(null) }
    var deletingAutoRunId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    // 에러 표시
    LaunchedEffect(errorState) {
        errorState?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("예약설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (autoRuns.size < 10) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "시간대 추가"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 자동 실행 제어 카드 (MVP)
            AutoRunControlCard(
                masterEnabled = masterEnabled,
                pauseUntil = pauseUntil,
                onMasterEnabledChange = { viewModel.setMasterEnabled(it) },
                onPauseForHours = { viewModel.pauseForHours(it) },
                onPauseUntilMidnight = { viewModel.pauseUntilMidnight() },
                onResume = { viewModel.resumeAutoRun() }
            )
            
            // 2. 정확 알람 권한 경고 배너
            if (!canScheduleExactAlarms) {
                ExactAlarmPermissionWarningBanner(
                    onSettingsClick = {
                        val intent = ExactAlarmPermissionUtil.createSettingsIntentSafe(context)
                        context.startActivity(intent)
                    }
                )
            }

            // 3. 템플릿 선택 버튼 (Empty State일 때 눈에 띄게 표시)
            if (autoRuns.isEmpty()) {
                EmptyStateWithTemplate(
                    onTemplateClick = { showTemplateDialog = true }
                )
            } else {
                TemplateSelectionButton(
                    enabled = autoRuns.size < 10,
                    onClick = { showTemplateDialog = true }
                )
            }

            // 4. 시간대 리스트
            if (autoRuns.isNotEmpty()) {
                Text(
                    text = "등록된 시간대 (${autoRuns.size}/10)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                autoRuns.forEach { autoRun ->
                    // 🆕 3차 고도화: 연결된 시간표 그룹 조회
                    val scheduleGroup = autoRun.scheduleGroupId?.let { scheduleGroupMap[it] }
                    
                    TimeBasedAutoRunCard(
                        autoRun = autoRun,
                        scheduleGroup = scheduleGroup,  // 🆕 3차 고도화
                        onToggle = { id, enabled ->
                            viewModel.toggleAutoRun(id, enabled)
                        },
                        onEdit = { editingAutoRun = it },
                        onDelete = { id ->
                            deletingAutoRunId = id
                        }
                    )
                }
            }

            // 5. 글로벌 옵션 섹션
            GlobalOptionsSection(
                excludeWeekends = excludeWeekends,
                autoStartDelayMinutes = autoStartDelayMinutes,
                preNotificationMinutes = preNotificationMinutes,
                onExcludeWeekendsChange = { viewModel.setExcludeWeekends(it) },
                onAutoStartDelayChange = { viewModel.setAutoStartDelayMinutes(it) },
                onPreNotificationChange = { viewModel.setPreNotificationMinutes(it) }
            )

            // 6. 위치 기반 자동 실행 안내 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📍 위치 기반 자동 실행도 사용해보세요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "특정 장소(회사, 학교 등)에 도착하면 자동으로 집중 모드를 시작할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Button(
                        onClick = onNavigateToLocationBased,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("위치 기반 설정 보기")
                    }
                }
            }

            // 6. 배터리 영향 안내 카드
            BatteryImpactInfoCard(
                impactLevel = "최소",
                estimatedPercentage = "< 1%/일",
                description = "시간 기반 자동 실행은 배터리에 거의 영향을 주지 않습니다."
            )
        }
    }

    // 시간대 추가 다이얼로그
    if (showAddDialog) {
        AddTimeBasedAutoRunDialog(
            onDismiss = { showAddDialog = false },
            onSave = { autoRun ->
                viewModel.addAutoRun(autoRun)
                showAddDialog = false
            }
        )
    }

    // 시간대 편집 다이얼로그
    editingAutoRun?.let { autoRun ->
        AddTimeBasedAutoRunDialog(
            existingAutoRun = autoRun,
            onDismiss = { editingAutoRun = null },
            onSave = { updatedAutoRun ->
                viewModel.updateAutoRun(updatedAutoRun)
                editingAutoRun = null
            }
        )
    }

    // 템플릿 선택 다이얼로그
    if (showTemplateDialog) {
        TemplateSelectionDialog(
            currentCount = autoRuns.size,
            maxCount = 10,
            onDismiss = { showTemplateDialog = false },
            onTemplateSelected = { templates ->
                templates.forEach { template ->
                    viewModel.addAutoRun(template)
                }
                showTemplateDialog = false
            }
        )
    }

    // 삭제 확인 다이얼로그
    deletingAutoRunId?.let { id ->
        val autoRun = autoRuns.find { it.id == id }
        AlertDialog(
            onDismissRequest = { deletingAutoRunId = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            title = { Text("삭제 확인") },
            text = {
                Text(
                    if (autoRun != null) {
                        val timeString = String.format("%02d:%02d", autoRun.hour, autoRun.minute)
                        "\"$timeString\" 자동 실행을 삭제하시겠습니까?\n알람도 함께 취소됩니다."
                    } else {
                        "이 자동 실행을 삭제하시겠습니까?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAutoRun(id)
                        deletingAutoRunId = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAutoRunId = null }) {
                    Text("취소")
                }
            }
        )
    }
}

