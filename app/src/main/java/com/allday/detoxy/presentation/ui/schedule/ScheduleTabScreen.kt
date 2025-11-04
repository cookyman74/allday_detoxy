package com.allday.detoxy.presentation.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.presentation.ui.autorun.components.QuickCreateScheduleDialog
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import com.allday.detoxy.domain.model.CreationMode
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import kotlinx.coroutines.launch

/**
 * 스케줄 탭 화면 (v0.10 UI/UX 개선)
 *
 * 하단 네비게이션의 "스케줄" 탭에서 표시되는 메인 화면입니다.
 * 모든 스케줄 그룹을 한눈에 보여주고, 활성 스케줄 및 다음 예약 정보를 제공합니다.
 *
 * ## 주요 기능
 * - 활성 스케줄 요약 카드 표시
 * - 다음 예약 요약 카드 표시
 * - 모든 스케줄 그룹 리스트 표시
 * - FAB를 통한 빠른 스케줄 생성
 * - 스케줄 카드 클릭 시 상세 화면으로 이동
 *
 * @param onNavigateToDetail 스케줄 상세 화면으로 이동하는 콜백 (groupId 전달)
 * @param viewModel ScheduleGroupViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTabScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ScheduleGroupViewModel = hiltViewModel()
) {
    val scheduleGroups by viewModel.scheduleGroups.collectAsState()
    val activeGroup by viewModel.activeGroup.collectAsState()
    val linkedTimeBasedAutoRuns by viewModel.linkedTimeBasedAutoRuns.collectAsState()
    val linkedLocationCounts by viewModel.linkedLocationCounts.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 화면 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadAllLinkedCounts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("스케줄") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "시간표 추가")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 활성 스케줄 카드
            if (activeGroup != null) {
                item {
                    ActiveScheduleSummaryCard(
                        activeSchedule = activeGroup!!
                    )
                }
            }
            
            // 다음 예약 카드
            item {
                NextScheduleSummaryCard(
                    scheduleGroups = scheduleGroups,
                    activeGroupId = activeGroup?.id
                )
            }
            
            // 섹션 헤더
            item {
                Text(
                    text = "모든 시간표 (${scheduleGroups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            // 빈 상태
            if (scheduleGroups.isEmpty()) {
                item {
                    EmptyScheduleCard(
                        onCreateClick = { showCreateDialog = true }
                    )
                }
            } else {
                // 시간표 목록
                items(scheduleGroups) { group ->
                    val timeSlots = linkedTimeBasedAutoRuns[group.id] ?: emptyList()
                    val locationCount = linkedLocationCounts[group.id] ?: 0
                    
                    ScheduleSummaryCard(
                        group = group,
                        isActive = group.id == activeGroup?.id,
                        timeSlotCount = timeSlots.size,
                        linkedLocationCount = locationCount,
                        onClick = { onNavigateToDetail(group.id) }
                    )
                }
            }
        }
    }
    
    // 시간표 생성 다이얼로그 (기존 재사용)
    if (showCreateDialog) {
        QuickCreateScheduleDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, mode, data ->
                scope.launch {
                    val newGroupId = when (mode) {
                        CreationMode.TEMPLATE -> {
                            @Suppress("UNCHECKED_CAST")
                            val template = data as ScheduleTemplate
                            viewModel.createFromTemplate(
                                name = name,
                                template = template
                            )
                        }
                        CreationMode.CUSTOM -> {
                            @Suppress("UNCHECKED_CAST")
                            val timeSlots = data as List<TimeSlot>
                            viewModel.createScheduleGroupWithTimeSlots(
                                name = name,
                                description = null,
                                timeSlots = timeSlots
                            )
                        }
                    }
                    
                    showCreateDialog = false
                    // 생성 후 상세 화면으로 이동 (선택적)
                    // onNavigateToDetail(newGroupId)
                }
            }
        )
    }
}

/**
 * 활성 스케줄 요약 카드
 *
 * 현재 활성화된 스케줄 그룹을 표시합니다.
 */
@Composable
fun ActiveScheduleSummaryCard(
    activeSchedule: ScheduleGroup
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 활성 인디케이터
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "활성화 중",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = activeSchedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Icon(
                imageVector = getIconForType(activeSchedule.iconType),
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(activeSchedule.colorHex)),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 다음 예약 요약 카드
 *
 * 오늘 예정된 다음 시간대를 표시합니다.
 */
@Composable
fun NextScheduleSummaryCard(
    scheduleGroups: List<ScheduleGroup>,
    activeGroupId: String?,
    viewModel: ScheduleGroupViewModel = hiltViewModel()
) {
    val nextSchedule by viewModel.getNextScheduleToday().collectAsState(initial = null)
    
    if (nextSchedule != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "다음 예약",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${nextSchedule!!.hour}:${String.format("%02d", nextSchedule!!.minute)} - ${nextSchedule!!.durationMinutes}분",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 스케줄 요약 카드
 *
 * 각 스케줄 그룹의 간단한 정보를 표시합니다.
 */
@Composable
fun ScheduleSummaryCard(
    group: ScheduleGroup,
    isActive: Boolean,
    timeSlotCount: Int,
    linkedLocationCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘
            Icon(
                imageVector = getIconForType(group.iconType),
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(group.colorHex)),
                modifier = Modifier.size(32.dp)
            )
            
            // 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 시간대 개수
                    Text(
                        text = "⏰ ${timeSlotCount}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 연결된 위치
                    if (linkedLocationCount > 0) {
                        Text(
                            text = "📍 위치 ${linkedLocationCount}개",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "어디서나 적용",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            // 활성 배지
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "활성화",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 빈 상태 카드
 *
 * 스케줄이 없을 때 표시되는 안내 카드입니다.
 */
@Composable
fun EmptyScheduleCard(
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Text(
                text = "아직 시간표가 없습니다",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "자동 실행 스케줄을 추가하여\n매일 반복되는 집중 루틴을 만들어보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Button(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("첫 시간표 만들기")
            }
        }
    }
}

/**
 * 아이콘 타입에 따른 Material Icon 반환
 */
@Composable
fun getIconForType(iconType: String): ImageVector {
    return when (iconType) {
        "HOME" -> Icons.Default.Home
        "WORK" -> Icons.Default.Star
        "STUDY" -> Icons.Default.Place
        "CALENDAR" -> Icons.Default.DateRange
        "PLACE" -> Icons.Default.Place
        else -> Icons.Default.DateRange
    }
}

