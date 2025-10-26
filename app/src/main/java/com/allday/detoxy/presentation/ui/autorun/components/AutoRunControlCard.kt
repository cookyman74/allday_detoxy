package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 자동 실행 제어 카드 (MVP)
 *
 * 마스터 스위치와 일시중지 기능을 제공합니다.
 *
 * @param masterEnabled 마스터 스위치 상태
 * @param pauseUntil 일시중지 해제 시각 (null이면 일시중지되지 않음)
 * @param onMasterEnabledChange 마스터 스위치 변경 콜백
 * @param onPauseForHours N시간 동안 일시중지 콜백
 * @param onPauseUntilMidnight 오늘 하루 중지 콜백
 * @param onResume 일시중지 해제 콜백
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoRunControlCard(
    masterEnabled: Boolean,
    pauseUntil: Long?,
    onMasterEnabledChange: (Boolean) -> Unit,
    onPauseForHours: (Int) -> Unit,
    onPauseUntilMidnight: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPauseMenu by remember { mutableStateOf(false) }
    
    // 일시중지 상태 확인
    val isPaused = pauseUntil != null && System.currentTimeMillis() < pauseUntil
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (masterEnabled && !isPaused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 마스터 스위치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "자동 실행",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (masterEnabled) "활성화됨" else "비활성화됨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = onMasterEnabledChange
                )
            }
            
            // 일시중지 상태 표시
            if (isPaused && pauseUntil != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "일시중지 중",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "해제: ${formatPauseUntil(pauseUntil)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    FilledTonalButton(
                        onClick = onResume,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("해제")
                    }
                }
            }
            
            // 일시중지 버튼 (마스터 스위치가 켜져 있고 일시중지되지 않았을 때만 표시)
            if (masterEnabled && !isPaused) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Box {
                    OutlinedButton(
                        onClick = { showPauseMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("일시중지")
                    }
                    
                    DropdownMenu(
                        expanded = showPauseMenu,
                        onDismissRequest = { showPauseMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("1시간 동안 중지") },
                            onClick = {
                                onPauseForHours(1)
                                showPauseMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("3시간 동안 중지") },
                            onClick = {
                                onPauseForHours(3)
                                showPauseMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("6시간 동안 중지") },
                            onClick = {
                                onPauseForHours(6)
                                showPauseMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("오늘 하루 중지") },
                            onClick = {
                                onPauseUntilMidnight()
                                showPauseMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 일시중지 해제 시각을 포맷팅
 */
private fun formatPauseUntil(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now
    
    if (diff <= 0) {
        return "곧 해제됨"
    }
    
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
    
    return when {
        hours > 0 -> "${hours}시간 ${minutes}분 후"
        minutes > 0 -> "${minutes}분 후"
        else -> "1분 미만"
    }
}

