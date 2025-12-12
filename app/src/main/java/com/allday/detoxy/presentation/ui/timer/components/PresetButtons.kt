package com.allday.detoxy.presentation.ui.timer.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import com.allday.detoxy.data.local.entity.CustomTimerPreset
import com.allday.detoxy.presentation.ui.component.GlassSurface
import com.allday.detoxy.presentation.ui.theme.LocalHazeState

/**
 * 프리셋 버튼 Row
 *
 * 기본 프리셋(25/45/60분) + 커스텀 프리셋을 스크롤 가능한 Row로 표시합니다.
 *
 * ## 주요 기능
 * - 기본 프리셋 (25분, 45분, 60분)
 * - 커스텀 프리셋 (최대 10개)
 * - 길게 누르면 편집/삭제 옵션
 * - 선택된 프리셋 강조 표시
 *
 * @param customPresets 커스텀 프리셋 리스트
 * @param selectedMinutes 현재 선택된 시간 (분)
 * @param onPresetClick 프리셋 클릭 콜백 (시간(분), 프리셋ID)
 * @param onPresetLongClick 프리셋 길게 누르기 콜백 (프리셋)
 * @param modifier Modifier
 */
@Composable
fun PresetButtonRow(
    customPresets: List<CustomTimerPreset>,
    selectedMinutes: Int,
    onPresetClick: (durationMinutes: Int, presetId: String?) -> Unit,
    onPresetLongClick: (CustomTimerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "빠른 선택",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 기본 프리셋 (25분, 45분, 60분)
            val defaultPresets = listOf(25, 45, 60)
            defaultPresets.forEach { minutes ->
                DefaultPresetButton(
                    minutes = minutes,
                    isSelected = selectedMinutes == minutes,
                    onClick = { onPresetClick(minutes, null) }
                )
            }
            
            // 커스텀 프리셋
            customPresets.forEach { preset ->
                CustomPresetButton(
                    preset = preset,
                    isSelected = selectedMinutes == preset.durationMinutes,
                    onClick = { onPresetClick(preset.durationMinutes, preset.id) },
                    onLongClick = { onPresetLongClick(preset) }
                )
            }
        }
    }
}

/**
 * 기본 프리셋 버튼
 *
 * @param minutes 시간 (분)
 * @param isSelected 선택 여부
 * @param onClick 클릭 콜백
 */
@Composable
private fun DefaultPresetButton(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$minutes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "분",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * 커스텀 프리셋 버튼
 *
 * @param preset 커스텀 프리셋
 * @param isSelected 선택 여부
 * @param onClick 클릭 콜백
 * @param onLongClick 길게 누르기 콜백
 */
@Composable
private fun CustomPresetButton(
    preset: CustomTimerPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(100.dp)
            .height(64.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onTertiary
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "${preset.durationMinutes}분",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 프리셋 관리 옵션 BottomSheet
 *
 * 길게 눌렀을 때 표시되는 편집/삭제 옵션입니다.
 *
 * @param preset 프리셋
 * @param onEdit 편집 콜백
 * @param onDelete 삭제 콜백
 * @param onDismiss BottomSheet 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetManagementBottomSheet(
    preset: CustomTimerPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null // DragHandle will be inside GlassSurface
    ) {
        // Disable Haze in BottomSheets (new window) to prevent cross-window RenderNode issues
        CompositionLocalProvider(LocalHazeState provides null) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                alpha = 0.5f // Matching GlassDialog alpha
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Drag Handle
                    BottomSheetDefaults.DragHandle(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
    
                    // 프리셋 정보
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "${preset.durationMinutes}분",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    HorizontalDivider()
                    
                    // 편집 버튼
                    TextButton(
                        onClick = {
                            onEdit()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("편집하기")
                    }
                    
                    // 삭제 버튼
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("삭제하기")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

