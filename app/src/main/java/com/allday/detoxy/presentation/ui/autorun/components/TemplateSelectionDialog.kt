package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import org.json.JSONArray
import java.util.*

/**
 * 템플릿 선택 다이얼로그
 *
 * 사전 정의된 템플릿(업무 집중, 공부 집중, 저녁 디톡시, 주말 집중)을 선택하여
 * 빠르게 시간 기반 자동 실행을 설정할 수 있습니다.
 *
 * @param currentCount 현재 등록된 시간대 개수
 * @param maxCount 최대 등록 가능 개수 (10개)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onTemplateSelected 템플릿 선택 완료 콜백 (생성된 TimeBasedAutoRun 리스트 전달)
 */
@Composable
fun TemplateSelectionDialog(
    currentCount: Int,
    maxCount: Int,
    onDismiss: () -> Unit,
    onTemplateSelected: (List<TimeBasedAutoRun>) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }

    val templates = remember {
        listOf(
            Template(
                name = "업무 집중",
                description = "평일 오전/오후 업무 시간에 집중",
                icon = "💼",
                autoRuns = listOf(
                    createAutoRun(hour = 9, minute = 0, label = "오전 업무", durationMinutes = 90, days = listOf("MON", "TUE", "WED", "THU", "FRI")),
                    createAutoRun(hour = 14, minute = 0, label = "오후 업무", durationMinutes = 120, days = listOf("MON", "TUE", "WED", "THU", "FRI"))
                )
            ),
            Template(
                name = "공부 집중",
                description = "저녁 시간 집중 학습",
                icon = "📚",
                autoRuns = listOf(
                    createAutoRun(hour = 19, minute = 0, label = "저녁 공부", durationMinutes = 60, days = listOf("MON", "TUE", "WED", "THU", "FRI")),
                    createAutoRun(hour = 21, minute = 0, label = "야간 공부", durationMinutes = 90, days = listOf("MON", "TUE", "WED", "THU", "FRI"))
                )
            ),
            Template(
                name = "저녁 디톡시",
                description = "저녁 식사 후 디지털 디톡스",
                icon = "🌙",
                autoRuns = listOf(
                    createAutoRun(hour = 20, minute = 0, label = "저녁 디톡시", durationMinutes = 60, presetType = "RELAXED", days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                )
            ),
            Template(
                name = "주말 집중",
                description = "주말 오전 집중 시간",
                icon = "☀️",
                autoRuns = listOf(
                    createAutoRun(hour = 10, minute = 0, label = "주말 오전", durationMinutes = 90, days = listOf("SAT", "SUN"))
                )
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "템플릿으로 시작하기",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 안내 문구
                Text(
                    text = "미리 준비된 템플릿으로 빠르게 시작하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 템플릿 리스트
                templates.forEach { template ->
                    val canAdd = currentCount + template.autoRuns.size <= maxCount
                    
                    TemplateCard(
                        template = template,
                        selected = selectedTemplate == template,
                        enabled = canAdd,
                        onClick = { if (canAdd) selectedTemplate = template }
                    )
                }

                // 제한 초과 경고
                if (templates.any { currentCount + it.autoRuns.size > maxCount }) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ 일부 템플릿은 최대 개수 제한으로 선택할 수 없습니다. (현재: $currentCount/$maxCount)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTemplate?.let { template ->
                        onTemplateSelected(template.autoRuns)
                    }
                },
                enabled = selectedTemplate != null
            ) {
                Text("적용하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 템플릿 카드
 */
@Composable
private fun TemplateCard(
    template: Template,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton
            ),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = template.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${template.autoRuns.size}개 시간대 추가",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (enabled) {
                RadioButton(
                    selected = selected,
                    onClick = null
                )
            }
        }
    }
}

/**
 * 템플릿 데이터 클래스
 */
private data class Template(
    val name: String,
    val description: String,
    val icon: String,
    val autoRuns: List<TimeBasedAutoRun>
)

/**
 * TimeBasedAutoRun 생성 Helper
 */
private fun createAutoRun(
    hour: Int,
    minute: Int,
    label: String,
    durationMinutes: Int,
    presetType: String = "STANDARD",
    days: List<String>
): TimeBasedAutoRun {
    return TimeBasedAutoRun(
        id = UUID.randomUUID().toString(),
        hour = hour,
        minute = minute,
        durationMinutes = durationMinutes,
        presetType = presetType,
        enabledDays = JSONArray(days).toString(),
        label = label,
        isEnabled = true,
        createdAt = System.currentTimeMillis()
    )
}

