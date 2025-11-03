package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun

/**
 * 위치 정보 수정 다이얼로그
 *
 * 기존 LocationBasedAutoRun의 정보를 수정합니다.
 * - 라벨 (이름)
 * - 반경
 * - 활성화 여부
 *
 * @param location 수정할 위치 정보
 * @param onDismiss 취소 콜백
 * @param onSave 저장 콜백
 */
@Composable
fun LocationEditDialog(
    location: LocationBasedAutoRun,
    onDismiss: () -> Unit,
    onSave: (LocationBasedAutoRun) -> Unit
) {
    var label by remember { mutableStateOf(location.label) }
    var radiusMeters by remember { mutableIntStateOf(location.radiusMeters) }
    var isEnabled by remember { mutableStateOf(location.isEnabled) }
    var activateOnEnter by remember { mutableStateOf(location.activateScheduleOnEnter) }
    var deactivateOnExit by remember { mutableStateOf(location.deactivateScheduleOnExit) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "위치 정보 수정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 라벨 (이름)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("위치 이름 *") },
                    placeholder = { Text("예: 학교, 독서실, 카페") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                HorizontalDivider()
                
                // 주소 (읽기 전용)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "주소",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = location.address ?: "주소 없음",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 반경
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "반경: ${radiusMeters}m",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = radiusMeters.toFloat(),
                        onValueChange = { radiusMeters = it.toInt() },
                        valueRange = 50f..500f,
                        steps = (500 - 50) / 50 - 1,  // 50m 단위
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "이 반경 내에 진입하면 자동으로 시간표가 활성화됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 활성화 설정
                Text(
                    text = "자동 활성화 설정",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 위치 기반 자동 실행 활성화
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "위치 기반 자동 실행",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "이 위치에서 Geofence를 사용합니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
                
                // 진입 시 활성화
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "진입 시 시간표 활성화",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "위치에 도착하면 연결된 시간표를 켭니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = activateOnEnter,
                        onCheckedChange = { activateOnEnter = it },
                        enabled = isEnabled
                    )
                }
                
                // 이탈 시 비활성화
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "이탈 시 시간표 비활성화",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "위치를 벗어나면 연결된 시간표를 끕니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = deactivateOnExit,
                        onCheckedChange = { deactivateOnExit = it },
                        enabled = isEnabled
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedLocation = location.copy(
                        label = label.trim(),
                        radiusMeters = radiusMeters,
                        isEnabled = isEnabled,
                        activateScheduleOnEnter = activateOnEnter,
                        deactivateScheduleOnExit = deactivateOnExit
                    )
                    onSave(updatedLocation)
                },
                enabled = label.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

