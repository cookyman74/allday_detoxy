package com.allday.detoxy.presentation.ui.autorun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.presentation.ui.autorun.components.AddScheduleGroupDialog
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleGroupCard
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import kotlinx.coroutines.launch

/**
 * 스케줄 그룹 관리 화면
 *
 * 여러 시간 기반 자동 실행을 그룹으로 묶어 위치에 따라 자동으로 활성화/비활성화할 수 있는 화면입니다.
 *
 * ## 주요 기능
 * - ScheduleGroup 목록 표시
 * - ScheduleGroup 생성/수정/삭제
 * - ScheduleGroup 활성화/비활성화
 * - 연결된 시간표 및 위치 개수 표시
 * - 시간표 관리 버튼을 통해 TimeBasedAutoRunScreen으로 이동
 *
 * @param onBack 뒤로가기 콜백
 * @param onNavigateToTimeBasedAutoRun 시간 기반 자동 실행 화면으로 이동 콜백
 * @param viewModel ScheduleGroupViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGroupScreen(
    onBack: () -> Unit = {},
    onNavigateToTimeBasedAutoRun: () -> Unit = {},
    viewModel: ScheduleGroupViewModel = hiltViewModel()
) {
    val scheduleGroups by viewModel.scheduleGroups.collectAsStateWithLifecycle()
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val linkedTimeBasedAutoRuns by viewModel.linkedTimeBasedAutoRuns.collectAsStateWithLifecycle()
    val linkedLocationCounts by viewModel.linkedLocationCounts.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<ScheduleGroup?>(null) }
    var deletingGroupId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 화면 초기 로딩 시 모든 그룹의 연결된 설정 개수 로드
    LaunchedEffect(scheduleGroups) {
        if (scheduleGroups.isNotEmpty()) {
            viewModel.loadAllLinkedCounts()
        }
    }
    
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
    
    // 삭제 확인 다이얼로그
    if (deletingGroupId != null) {
        AlertDialog(
            onDismissRequest = { deletingGroupId = null },
            title = { Text("스케줄 그룹 삭제") },
            text = {
                Column {
                    Text("이 스케줄 그룹을 삭제하시겠습니까?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "연결된 시간표와 위치의 참조는 해제되지만, 설정 자체는 삭제되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingGroupId?.let { viewModel.deleteScheduleGroup(it) }
                        deletingGroupId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroupId = null }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 추가/편집 다이얼로그
    if (showAddDialog || editingGroup != null) {
        AddScheduleGroupDialog(
            onDismiss = {
                showAddDialog = false
                editingGroup = null
            },
            onConfirm = { name, description ->
                if (editingGroup != null) {
                    viewModel.updateScheduleGroup(
                        editingGroup!!.copy(
                            name = name,
                            description = description
                        )
                    )
                } else {
                    scope.launch {
                        viewModel.createScheduleGroup(name, description)
                    }
                }
            },
            existingGroup = editingGroup
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("스케줄 그룹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (scheduleGroups.size < 10) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "스케줄 그룹 추가"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && scheduleGroups.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (scheduleGroups.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "스케줄 그룹이 없습니다",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "여러 시간표를 그룹으로 묶어\n위치에 따라 자동으로 활성화할 수 있습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("첫 그룹 만들기")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 🆕 3차 고도화: 현재 활성화된 시간표 카드
                    if (activeGroup != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "⚡ 현재 활성화된 시간표",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = activeGroup!!.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    if (activeGroup!!.description != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = activeGroup!!.description!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.deactivateGroup(activeGroup!!.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    ) {
                                        Text("비활성화")
                                    }
                                }
                            }
                        }
                    }
                    
                    // 안내 카드
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "💡 스케줄 그룹 사용 방법",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "1. 스케줄 그룹을 만듭니다\n" +
                                            "2. 시간 기반 자동 실행 설정에서 그룹을 선택합니다\n" +
                                            "3. 위치 기반 자동 실행에서 그룹을 연결합니다\n" +
                                            "4. 해당 위치에 진입하면 그룹의 시간표가 자동으로 활성화됩니다",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // ScheduleGroup 목록
                    items(
                        items = scheduleGroups,
                        key = { it.id }
                    ) { group ->
                        // 🆕 3차 고도화 개선: ViewModel에서 데이터를 미리 준비해서 전달
                        val timeBasedAutoRunsList = linkedTimeBasedAutoRuns[group.id] ?: emptyList()
                        val locationCount = linkedLocationCounts[group.id] ?: 0
                        
                        ScheduleGroupCard(
                            group = group,
                            isActive = group.id == activeGroup?.id,
                            timeBasedAutoRuns = timeBasedAutoRunsList,
                            linkedLocationCount = locationCount,
                            onActivate = {
                                // 🆕 3차 고도화: ScheduleGroupManager 사용
                                if (group.isActive) {
                                    viewModel.deactivateGroup(group.id)
                                } else {
                                    viewModel.activateGroup(group.id)
                                }
                            },
                            onEdit = {
                                editingGroup = group
                            },
                            onDelete = {
                                deletingGroupId = group.id
                            },
                            onManageTimeSlots = {
                                // 시간 기반 자동 실행 화면으로 이동
                                onNavigateToTimeBasedAutoRun()
                            }
                        )
                    }
                    
                    // 하단 여백 (FAB 가리지 않도록)
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

