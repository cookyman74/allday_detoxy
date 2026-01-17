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
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.PauseDuration
import com.allday.detoxy.domain.model.ScheduleGroupControlState
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import android.util.Log
import com.allday.detoxy.presentation.ui.autorun.components.AddScheduleGroupDialog
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleCreationDialog
import com.allday.detoxy.presentation.ui.autorun.components.ScheduleGroupCard
import com.allday.detoxy.presentation.ui.autorun.components.LocationEditDialog
import com.allday.detoxy.presentation.ui.component.SimpleGlassSurface
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import com.allday.detoxy.presentation.viewmodel.LocationBasedAutoRunViewModel
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.presentation.model.WeeklyHeatmapUiModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.allday.detoxy.R

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
    onNavigateToTimeBasedAutoRun: (scheduleGroupId: String, scheduleGroupName: String) -> Unit = { _, _ -> },
    initialScrollToGroupId: String? = null,
    onNavigateToDetail: (groupId: String) -> Unit = {},  // v1.1: 상세 페이지 이동
    viewModel: ScheduleGroupViewModel = hiltViewModel(),
    locationViewModel: LocationBasedAutoRunViewModel = hiltViewModel()
) {
    val scheduleGroups by viewModel.scheduleGroups.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val linkedTimeBasedAutoRuns by viewModel.linkedTimeBasedAutoRuns.collectAsStateWithLifecycle()
    // val linkedLocations by viewModel.linkedLocations.collectAsStateWithLifecycle()  // 목록에서 안씀
    // val linkedLocationCounts by viewModel.linkedLocationCounts.collectAsStateWithLifecycle()
    // val groupHeatmaps by viewModel.groupHeatmaps.collectAsStateWithLifecycle()  // 목록에서 안씀
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState() // 🆕 스크롤 상태 관리
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // 🆕 UiText 처리를 위해 Context 획득
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<ScheduleGroup?>(null) }
    var deletingGroupId by remember { mutableStateOf<String?>(null) }
    
    // 🆕 위치 정보 수정 다이얼로그 상태
    var showLocationEditDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<LocationBasedAutoRun?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 화면 초기 로딩 시 모든 그룹의 연결된 설정 개수 로드
    LaunchedEffect(scheduleGroups) {
        if (scheduleGroups.isNotEmpty()) {
            viewModel.loadAllLinkedCounts()
            
            // 🆕 초기 진입 시 특정 그룹으로 스크롤 이동
            if (initialScrollToGroupId != null) {
                val index = scheduleGroups.indexOfFirst { it.id == initialScrollToGroupId }
                if (index >= 0) {
                    // 🆕 인덱스 보정: LazyColumn에 "안내 카드"가 0번째 항목으로 추가되어 있으므로 index + 1 필요
                    val adjustedIndex = index + 1
                    Log.d("ScheduleGroupScreen", "📜 Scrolling to group: $initialScrollToGroupId (original: $index, adjusted: $adjustedIndex)")
                    listState.animateScrollToItem(adjustedIndex)
                }
            }
        }
    }
    
    // 에러 표시
    LaunchedEffect(errorState) {
        errorState?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.asString(context), // UiText -> String 변환
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    // 삭제 확인 다이얼로그
    if (deletingGroupId != null) {
        AlertDialog(
            onDismissRequest = { deletingGroupId = null },
            title = { Text(stringResource(R.string.dialog_delete_group_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_delete_group_message))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dialog_delete_group_warning),
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
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroupId = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
    
    // ... (중략) ...

    // 🆕 위치 정보 수정 다이얼로그
    if (showLocationEditDialog && editingLocation != null) {
        LocationEditDialog(
            location = editingLocation!!,
            onDismiss = {
                showLocationEditDialog = false
                editingLocation = null
            },
            onSave = { updatedLocation ->
                Log.d("ScheduleGroupScreen", "📍 onSave called for location: ${updatedLocation.label}")
                
                // 🆕 즉시 다이얼로그 닫기 (비동기 작업 전)
                showLocationEditDialog = false
                editingLocation = null
                
                // 비동기 작업 시작
                scope.launch {
                    try {
                        // 1. 🆕 LocationBasedAutoRun 업데이트 (완료될 때까지 대기)
                        locationViewModel.updateLocation(updatedLocation)
                        Log.d("ScheduleGroupScreen", "✅ Location updated successfully")
                        
                        // 2. 🆕 연결된 스케줄 그룹의 위치 정보 갱신 (업데이트 완료 후!)
                        updatedLocation.linkedScheduleGroupId?.let { groupId ->
                            viewModel.loadLinkedLocations(groupId)
                            Log.d("ScheduleGroupScreen", "✅ Linked locations reloaded")
                        }
                        
                        // 3. 성공 메시지 표시
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.msg_location_updated), // Composable scope 밖이므로 context 사용
                            duration = SnackbarDuration.Short
                        )
                    } catch (e: Exception) {
                        // 오류 발생 시 오류 메시지 표시
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.msg_location_update_failed, e.message ?: ""),
                            duration = SnackbarDuration.Long
                        )
                        Log.e("ScheduleGroupScreen", "❌ Failed to update location", e)
                    }
                }
            }
        )
    }

    // MainActivity의 GlassScaffold 배경 위에 그려짐 (배경 중복 방지)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 헤더 (뒤로 가기 버튼 포함)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_desc),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.title_schedule_group),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                    )
                }

        Box(
                modifier = Modifier.fillMaxSize()
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
                        text = stringResource(R.string.group_empty_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.group_empty_desc),
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
                        Text(stringResource(R.string.btn_create_first_group))
                    }
                }
            } else {
                LazyColumn(
                    state = listState, // 🆕 스크롤 상태 연결
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 🆕 3.5차 고도화: 다중 활성화 지원 - 각 카드에 활성화 상태 표시
                    // (이전의 "현재 활성화된 시간표" 카드는 단일 활성화 가정으로 제거)
                    
                    // 안내 카드 (Glass 스타일)
                    item {
                        SimpleGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            alpha = 0.4f,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.guide_card_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.guide_card_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                        // val linkedLocationsList = linkedLocations[group.id] ?: emptyList()  // 목록에서 위치 정보 제거
                        // val locationCount = linkedLocationCounts[group.id] ?: 0
                        // val heatmap = groupHeatmaps[group.id] ?: WeeklyHeatmapUiModel.EMPTY  // 목록에서 히트맵 제거
                        
                        // v8: controlState 계산
                        val controlState = ScheduleGroupControlState.fromEntity(
                            manualOverrideState = group.manualOverrideState,
                            pauseUntil = group.pauseUntil
                        )
                        
                        // v1.1 Refactor: 복잡한 카드 대신 심플한 리스트 아이템 사용
                        SimpleScheduleGroupItem(
                            group = group,
                            controlState = controlState,
                            timeCount = timeBasedAutoRunsList.size,
                            onStateChange = { newState ->
                                viewModel.changeControlState(group.id, newState)
                            },
                            onClick = {
                                onNavigateToDetail(group.id)
                            }
                        )
                    }
                    
                    // 하단 여백 (FAB 가리지 않도록)
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        } // 내부 Box 끝
    } // Column 끝

        
        // FAB (Box 내 BoxScope)
        if (scheduleGroups.size < 10) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.fab_add_group_desc)
                )
            }
        }
        
        // SnackbarHost (Box 내 BoxScope)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    } // 외부 Box 끝
}

/**
 * 목록용 심플 스케줄 그룹 아이템
 */
@Composable
private fun SimpleScheduleGroupItem(
    group: ScheduleGroup,
    controlState: ScheduleGroupControlState,
    timeCount: Int,
    onStateChange: (ScheduleGroupControlState) -> Unit,
    onClick: () -> Unit
) {
    val cardTint = when (controlState) {
        ScheduleGroupControlState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ScheduleGroupControlState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ScheduleGroupControlState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    SimpleGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = cardTint,
        alpha = 0.3f
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 아이콘
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.label_time_schedule_count, timeCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (controlState == ScheduleGroupControlState.PAUSED && group.pauseUntil != null) {
                        Text(
                            text = stringResource(R.string.label_group_paused),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Switch(
                checked = controlState == ScheduleGroupControlState.ACTIVE,
                onCheckedChange = { isOn ->
                    val newState = if (isOn) {
                        ScheduleGroupControlState.ACTIVE
                    } else {
                        ScheduleGroupControlState.INACTIVE
                    }
                    onStateChange(newState)
                }
            )
        }
    }
}



