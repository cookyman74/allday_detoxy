package com.allday.detoxy.presentation.ui.autorun

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.domain.model.PauseDuration
import com.allday.detoxy.domain.model.ScheduleGroupControlState
import com.allday.detoxy.presentation.model.WeeklyHeatmapUiModel
import com.allday.detoxy.presentation.ui.component.SimpleGlassSurface
import com.allday.detoxy.presentation.ui.heatmap.WeeklyHeatmap
import com.allday.detoxy.presentation.util.WeeklyHeatmapCalculator
import com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 스케줄 그룹 상세 페이지
 *
 * 단일 스케줄 그룹의 상세 정보를 표시합니다.
 * - 히트맵 (항상 표시)
 * - 시간 목록 (숨기기 없이 바로 표시)
 * - 위치 정보
 * - 수정/삭제 버튼
 *
 * @param groupId 표시할 스케줄 그룹 ID
 * @param onBack 뒤로 가기 콜백
 * @param onNavigateToTimeBasedAutoRun 시간표 관리 화면 이동 콜백
 * @param viewModel ScheduleGroupViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGroupDetailScreen(
    groupId: String,
    onBack: () -> Unit = {},
    onNavigateToTimeBasedAutoRun: (scheduleGroupId: String, scheduleGroupName: String) -> Unit = { _, _ -> },
    viewModel: ScheduleGroupViewModel = hiltViewModel()
) {
    val scheduleGroups by viewModel.scheduleGroups.collectAsStateWithLifecycle()
    val linkedTimeBasedAutoRuns by viewModel.linkedTimeBasedAutoRuns.collectAsStateWithLifecycle()
    val linkedLocations by viewModel.linkedLocations.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    // 해당 그룹 찾기
    val group = scheduleGroups.find { it.id == groupId }
    val timeBasedAutoRuns = linkedTimeBasedAutoRuns[groupId] ?: emptyList()
    val locations = linkedLocations[groupId] ?: emptyList()
    
    // 히트맵 계산 (해당 그룹의 데이터로만)
    val heatmap = remember(timeBasedAutoRuns) {
        if (timeBasedAutoRuns.isNotEmpty()) {
            Log.d("ScheduleGroupDetailScreen", "Calculating heatmapWith ${timeBasedAutoRuns.size} items")
            WeeklyHeatmapCalculator.calculate(timeBasedAutoRuns)
        } else {
            Log.d("ScheduleGroupDetailScreen", "timeBasedAutoRuns is empty, returning EMPTY heatmap")
            WeeklyHeatmapUiModel.EMPTY
        }
    }
    
    // 데이터 로드
    LaunchedEffect(groupId) {
        Log.d("ScheduleGroupDetailScreen", "Loading data for group: $groupId")
        viewModel.loadLinkedTimeBasedAutoRuns(groupId)
        viewModel.loadLinkedLocations(groupId)
    }
    
    // 그룹이 없으면 빈 화면
    if (group == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // 제어 상태 계산
    val controlState = ScheduleGroupControlState.fromEntity(
        manualOverrideState = group.manualOverrideState,
        pauseUntil = group.pauseUntil
    )
    
    // 상태별 배경색
    val cardTint = when (controlState) {
        ScheduleGroupControlState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ScheduleGroupControlState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ScheduleGroupControlState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // 수정 버튼
                IconButton(onClick = { /* TODO: 수정 다이얼로그 */ }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "수정",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // 삭제 버튼
                IconButton(onClick = { /* TODO: 삭제 확인 */ }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 스크롤 가능한 콘텐츠
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 그룹 정보 카드
                SimpleGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = cardTint,
                    alpha = 0.4f
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 그룹 상태 및 토글
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.description ?: "스케줄 그룹",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Switch(
                                checked = controlState == ScheduleGroupControlState.ACTIVE,
                                onCheckedChange = { isOn ->
                                    val newState = if (isOn) {
                                        ScheduleGroupControlState.ACTIVE
                                    } else {
                                        ScheduleGroupControlState.INACTIVE
                                    }
                                    viewModel.changeControlState(groupId, newState)
                                }
                            )
                        }
                        
                        // 일시중지 상태 표시
                        if (controlState == ScheduleGroupControlState.PAUSED && group.pauseUntil != null) {
                            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                            val pauseUntilTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(group.pauseUntil),
                                ZoneId.systemDefault()
                            ).format(formatter)
                            
                            Text(
                                text = "⏸️ $pauseUntilTime 까지 일시중지",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                
                // 위치 정보 섹션
                if (locations.isNotEmpty()) {
                    SimpleGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        alpha = 0.3f
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "위치",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            locations.forEach { location ->
                                Column {
                                    Text(
                                        text = location.label ?: "위치",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = location.address ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 시간 섹션 (히트맵 + 시간 목록)
                SimpleGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    alpha = 0.3f
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "시간 (${timeBasedAutoRuns.size}개)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // 시간표 관리 버튼
                            TextButton(
                                onClick = { onNavigateToTimeBasedAutoRun(groupId, group.name) }
                            ) {
                                Text("관리")
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        // 히트맵 (항상 표시)
                        if (timeBasedAutoRuns.isNotEmpty()) {
                            WeeklyHeatmap(
                                heatmap = heatmap,
                                onRowClick = { row ->
                                    Log.d("ScheduleGroupDetailScreen", "Row clicked: ${row.dayOfWeek}")
                                }
                            )
                        } else {
                            // 빈 상태
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⏰",
                                    style = MaterialTheme.typography.displaySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "등록된 시간 스케줄이 없습니다",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onNavigateToTimeBasedAutoRun(groupId, group.name) }
                                ) {
                                    Text("시간 추가")
                                }
                            }
                        }
                        
                        // 시간 목록 (항상 표시)
                        if (timeBasedAutoRuns.isNotEmpty()) {
                            HorizontalDivider()
                            
                            timeBasedAutoRuns.forEach { autoRun ->
                                TimeScheduleItem(
                                    label = autoRun.label,
                                    hour = autoRun.hour,
                                    minute = autoRun.minute,
                                    durationMinutes = autoRun.durationMinutes,
                                    presetType = autoRun.presetType,
                                    isEnabled = autoRun.isEnabled
                                )
                            }
                        }
                    }
                }
                
                // 하단 여백
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // SnackbarHost
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

/**
 * 시간 스케줄 아이템
 */
@Composable
private fun TimeScheduleItem(
    label: String?,
    hour: Int,
    minute: Int,
    durationMinutes: Int,
    presetType: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val period = if (hour < 12) "오전" else "오후"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val timeText = String.format("%s %d:%02d", period, displayHour, minute)
    
    val presetName = when (presetType) {
        "FULL_BLOCK" -> "완전 차단"
        "STANDARD" -> "표준"
        "RELAXED" -> "여유"
        else -> presetType
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$timeText · ${durationMinutes}분",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            if (!label.isNullOrEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = presetName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isEnabled) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )
    }
}
