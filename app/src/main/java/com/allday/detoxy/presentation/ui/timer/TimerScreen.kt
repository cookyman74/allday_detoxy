package com.allday.detoxy.presentation.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.presentation.viewmodel.TimerViewModel
import com.allday.detoxy.presentation.ui.timer.components.*
import com.allday.detoxy.presentation.ui.component.GlassSurface
import com.allday.detoxy.presentation.ui.component.GlassDialog
import com.allday.detoxy.presentation.ui.component.liquidGlass
import com.allday.detoxy.presentation.ui.theme.GlassWhite

/**
 * 타이머 메인 화면 (도넛 그래프 통합)
 *
 * 도넛 그래프로 시간을 선택하고 프리셋 버튼을 제공합니다.
 *
 * ## 주요 기능
 * - 도넛 그래프 타이머 선택 (5-180분)
 * - 기본 프리셋 (25/45/60분)
 * - 커스텀 프리셋 저장 및 관리
 * - 원형 프로그레스 바 표시
 * - 다음 예약 정보 표시
 *
 * @param viewModel 타이머 ViewModel
 */
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val permissionError by viewModel.permissionError.collectAsState()
    val nextAutoRunInfo by viewModel.nextAutoRunInfo.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val showSuccessAnimation by viewModel.showSuccessAnimation.collectAsState() // 🆕 성공 애니메이션
    
    // 도넛 그래프 선택 시간
    var selectedMinutes by remember { mutableStateOf(25) }
    
    // 다이얼로그 상태
    var showSaveDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<com.allday.detoxy.data.local.entity.CustomTimerPreset?>(null) }
    
    // 🆕 성공 축하 다이얼로그
    if (showSuccessAnimation) {
        SuccessCelebrationDialog(
            onDismiss = { viewModel.onSuccessAnimationShown() }
        )
    }
    
    // 권한 에러 다이얼로그
    permissionError?.let { error ->
        PermissionErrorDialog(
            error = error,
            onDismiss = { viewModel.clearPermissionError() },
            onOpenSettings = {
                when (error) {
                    is TimerViewModel.PermissionError.AccessibilityServiceDisabled -> {
                        PermissionUtils.openAccessibilitySettings(context)
                    }
                    is TimerViewModel.PermissionError.OverlayPermissionDenied -> {
                        PermissionUtils.openOverlaySettings(context)
                    }
                }
                viewModel.clearPermissionError()
            }
        )
    }
    
    // 프리셋 저장 다이얼로그
    if (showSaveDialog) {
        SavePresetDialog(
            durationMinutes = selectedMinutes,
            onSave = { name, presetType ->
                viewModel.saveCustomPreset(name, selectedMinutes, presetType)
            },
            onDismiss = { showSaveDialog = false }
        )
    }
    
    // 프리셋 편집 다이얼로그
    if (showEditDialog && selectedPreset != null) {
        EditPresetDialog(
            presetName = selectedPreset!!.name,
            presetType = selectedPreset!!.presetType,
            onSave = { name, presetType ->
                viewModel.updateCustomPreset(
                    selectedPreset!!.copy(
                        name = name,
                        presetType = presetType
                    )
                )
            },
            onDismiss = { showEditDialog = false }
        )
    }
    
    // 프리셋 삭제 확인 다이얼로그
    if (showDeleteDialog && selectedPreset != null) {
        DeletePresetDialog(
            presetName = selectedPreset!!.name,
            onConfirm = {
                viewModel.deleteCustomPreset(selectedPreset!!.id)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    // 프리셋 관리 BottomSheet
    if (showPresetSheet && selectedPreset != null) {
        PresetManagementBottomSheet(
            preset = selectedPreset!!,
            onEdit = {
                showPresetSheet = false
                showEditDialog = true
            },
            onDelete = {
                showPresetSheet = false
                showDeleteDialog = true
            },
            onDismiss = { showPresetSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // 1. System Bar Padding
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 제목
        Text(
            text = "집중 타이머",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 타이머 상태에 따른 UI 표시
        when (timerState) {
            FocusState.IDLE -> {
                // Glass Panel for Timer Controls
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    alpha = 0.3f
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 도넛 그래프 (IDLE 상태에서만 표시)
                        DonutTimerPicker(
                            selectedMinutes = selectedMinutes,
                            onMinutesChange = { selectedMinutes = it },
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // 다음 예약 정보 표시
                        if (nextAutoRunInfo != null) {
                            Text(
                                text = nextAutoRunInfo!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // 프리셋으로 저장 버튼 (기본 프리셋이 아닐 때)
                        if (selectedMinutes !in listOf(25, 45, 60)) {
                            OutlinedButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("프리셋으로 저장")
                            }
                        }
                        
                        // 프리셋 버튼
                        PresetButtonRow(
                            customPresets = customPresets,
                            selectedMinutes = selectedMinutes,
                            onPresetClick = { minutes, presetId ->
                                selectedMinutes = minutes
                                viewModel.incrementPresetUsage(presetId)
                            },
                            onPresetLongClick = { preset ->
                                selectedPreset = preset
                                showPresetSheet = true
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 시작 버튼
                        Button(
                            onClick = { viewModel.startTimer(selectedMinutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "시작하기",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            FocusState.RUNNING -> {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    alpha = 0.3f
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 원형 프로그레스 바 & 타이머 표시
                        CircularTimerDisplay(
                            state = timerState,
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 포기 버튼
                        Button(
                            onClick = { viewModel.giveUpTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("포기하기")
                        }
                    }
                }
            }

            FocusState.FINISHED -> {
                // 완료 메시지
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎉 타이머 완료!",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "집중 시간을 성공적으로 완료했습니다.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("새 타이머 시작")
                    }
                }
            }

            FocusState.FAILED -> {
                // 포기 메시지
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "타이머 포기",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "다음엔 더 잘할 수 있어요!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("다시 시작")
                    }
                }
            }
        }
    }
}

/**
 * 원형 타이머 표시
 *
 * @param state 타이머 상태
 * @param remainingSeconds 남은 시간 (초)
 * @param totalSeconds 전체 시간 (초)
 */
@Composable
fun CircularTimerDisplay(
    state: FocusState,
    remainingSeconds: Int,
    totalSeconds: Int
) {
    // 포맷된 시간 계산
    val formattedTime = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // 진행률 계산
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds == 0) 0f
        else {
            val elapsed = totalSeconds - remainingSeconds
            elapsed.toFloat() / totalSeconds.toFloat()
        }
    }
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 배경 원
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 12.dp,
        )

        // 진행률 원
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = when (state) {
                FocusState.RUNNING -> MaterialTheme.colorScheme.primary
                FocusState.FINISHED -> MaterialTheme.colorScheme.tertiary
                FocusState.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            },
            strokeWidth = 12.dp,
        )

        // 시간 텍스트
        Text(
            text = if (state == FocusState.IDLE) "00:00" else formattedTime,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 성공 축하 다이얼로그
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun SuccessCelebrationDialog(
    onDismiss: () -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎉 집중 성공!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "목표를 달성했습니다!",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "집중 시간을 성공적으로 완료했어요.\n계속해서 좋은 습관을 만들어가세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("확인")
            }
        }
    }
}

/**
 * 권한 에러 다이얼로그
 *
 * @param error 권한 에러 타입
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onOpenSettings 설정 화면으로 이동 콜백
 */
@Composable
fun PermissionErrorDialog(
    error: TimerViewModel.PermissionError,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val (title, message) = when (error) {
        is TimerViewModel.PermissionError.AccessibilityServiceDisabled -> {
            "앱 차단 기능 권한 필요" to "집중 타이머를 사용하려면 앱 차단 기능(접근성 서비스)을 활성화해야 합니다.\n\n" +
                    "설정 화면에서 'ScreenSence'를 찾아 활성화해주세요."
        }
        is TimerViewModel.PermissionError.OverlayPermissionDenied -> {
            "잠금 화면 표시 권한 필요" to "집중 타이머를 사용하려면 잠금 화면 표시 권한(다른 앱 위에 표시)이 필요합니다.\n\n" +
                    "설정 화면에서 권한을 허용해주세요."
        }
    }

    GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onOpenSettings) {
                    Text("설정으로 이동")
                }
            }
        }
    }
}
