package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.R
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * 시간 기반 자동 실행 카드
 *
 * 등록된 시간대 정보를 표시하고, 활성화/비활성화, 편집, 삭제 기능을 제공합니다.
 *
 * @param autoRun 표시할 TimeBasedAutoRun 데이터
 * @param scheduleGroup 연결된 ScheduleGroup (3차 고도화)
 * @param onToggle 활성화/비활성화 토글 콜백 (ID, isEnabled)
 * @param onEdit 편집 버튼 클릭 콜백
 * @param onDelete 삭제 버튼 클릭 콜백 (ID)
 */
@Composable
fun TimeBasedAutoRunCard(
    autoRun: TimeBasedAutoRun,
    scheduleGroup: ScheduleGroup? = null,  // 🆕 3차 고도화
    onToggle: (String, Boolean) -> Unit,
    onEdit: (TimeBasedAutoRun) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (autoRun.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더: 시간 + 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 시간 표시
                Text(
                    text = formatTime(autoRun.hour, autoRun.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (autoRun.isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // 활성화 토글
                Switch(
                    checked = autoRun.isEnabled,
                    onCheckedChange = { enabled ->
                        onToggle(autoRun.id, enabled)
                    }
                )
            }

            // 라벨 (선택 사항)
            if (!autoRun.label.isNullOrEmpty()) {
                Text(
                    text = autoRun.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // 타이머 시간 + 차단 프리셋
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${autoRun.durationMinutes}분 • ${getPresetDisplayName(autoRun.presetType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 요일 표시
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatEnabledDays(autoRun.enabledDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 🆕 3차 고도화: 그룹 배지
            if (scheduleGroup != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = scheduleGroup.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!scheduleGroup.isActive) {
                        Badge {
                            Text(stringResource(R.string.autorun_card_inactive_badge), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 🆕 3차 고도화: 독립 실행 안내
            if (!autoRun.isIndependent) {
                Text(
                    text = stringResource(R.string.autorun_card_independent_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }

            // 액션 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 편집 버튼
                IconButton(onClick = { onEdit(autoRun) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "편집",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 삭제 버튼
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.autorun_delete_dialog_title)) },
            text = { Text(stringResource(R.string.autorun_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(autoRun.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

/**
 * 시간을 12시간 형식으로 포맷 (예: "오전 10:00", "오후 2:30")
 * 
 * v_multi: Locale.KOREAN -> Locale.getDefault()
 */
private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val sdf = SimpleDateFormat("a h:mm", Locale.getDefault())
    return sdf.format(calendar.time)
}

/**
 * 차단 프리셋 표시 이름 변환
 */
@Composable
private fun getPresetDisplayName(presetType: String): String {
    return when (presetType) {
        "FULL_BLOCK" -> stringResource(R.string.preset_full_block_display)
        "STANDARD" -> stringResource(R.string.preset_standard_display)
        "RELAXED" -> stringResource(R.string.preset_relaxed_display)
        else -> presetType
    }
}

/**
 * 활성화된 요일 포맷 (예: "월, 화, 수, 목, 금")
 */
@Composable
private fun formatEnabledDays(enabledDaysJson: String): String {
    // 1. JSON Parsing (Non-Composable logic inside try-catch)
    val days = try {
        val jsonArray = JSONArray(enabledDaysJson)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }

    // 2. String Composition (Composable logic outside try-catch)
    val dayNames = days.map { getDayDisplayName(it) }

    return if (days.isEmpty()) {
        stringResource(R.string.day_none_selected)
    } else if (days.size == 7) {
        stringResource(R.string.day_daily)
    } else {
        dayNames.joinToString(", ")
    }
}

/**
 * 요일 코드를 한글 표시 이름으로 변환
 */
@Composable
private fun getDayDisplayName(dayCode: String): String {
    return when (dayCode.uppercase()) {
        "MON" -> stringResource(R.string.day_mon)
        "TUE" -> stringResource(R.string.day_tue)
        "WED" -> stringResource(R.string.day_wed)
        "THU" -> stringResource(R.string.day_thu)
        "FRI" -> stringResource(R.string.day_fri)
        "SAT" -> stringResource(R.string.day_sat)
        "SUN" -> stringResource(R.string.day_sun)
        else -> dayCode
    }
}

