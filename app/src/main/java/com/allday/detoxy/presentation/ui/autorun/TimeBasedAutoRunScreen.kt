package com.allday.detoxy.presentation.ui.autorun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
 * 시간 기반 자동 실행 설정 화면
 *
 * 사용자가 특정 시간에 자동으로 타이머를 시작하도록 설정할 수 있는 화면입니다.
 * - 정확 알람 권한 경고 배너
 * - 템플릿 선택 버튼
 * - 시간대 리스트 (LazyColumn)
 * - 시간대 추가 버튼
 * - 배터리 영향 안내 카드
 *
 * @param onBack 뒤로가기 콜백
 * @param viewModel TimeBasedAutoRunViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBasedAutoRunScreen(
    onBack: () -> Unit = {},
    viewModel: TimeBasedAutoRunViewModel = hiltViewModel()
) {
    val autoRuns by viewModel.autoRuns.collectAsStateWithLifecycle()
    val canScheduleExactAlarms by viewModel.canScheduleExactAlarms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingAutoRun by remember { mutableStateOf<TimeBasedAutoRun?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시간 기반 자동 실행") },
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
            // 1. 정확 알람 권한 경고 배너
            if (!canScheduleExactAlarms) {
                ExactAlarmPermissionWarningBanner(
                    onSettingsClick = {
                        val intent = ExactAlarmPermissionUtil.createSettingsIntent(context)
                        context.startActivity(intent)
                    }
                )
            }

            // 2. 템플릿 선택 버튼 (Empty State일 때 눈에 띄게 표시)
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

            // 3. 시간대 리스트
            if (autoRuns.isNotEmpty()) {
                Text(
                    text = "등록된 시간대 (${autoRuns.size}/10)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                autoRuns.forEach { autoRun ->
                    TimeBasedAutoRunCard(
                        autoRun = autoRun,
                        onToggle = { id, enabled ->
                            viewModel.toggleAutoRun(id, enabled)
                        },
                        onEdit = { editingAutoRun = it },
                        onDelete = { id ->
                            viewModel.deleteAutoRun(id)
                        }
                    )
                }
            }

            // 4. 배터리 영향 안내 카드
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
}

