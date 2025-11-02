package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.data.template.DefaultTemplates
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.presentation.util.formatDays
import java.time.DayOfWeek

/**
 * 템플릿 선택 BottomSheet (Phase 2.2)
 *
 * 사용자가 미리 정의된 6개 템플릿 중 하나를 선택할 수 있는 BottomSheet입니다.
 *
 * ## 특징
 * - Material3 ModalBottomSheet 사용
 * - 6개 템플릿 목록 표시 (시간 기반 3개, 위치 기반 3개)
 * - 템플릿 카드에 아이콘, 색상, 정보 표시
 * - 위치 필요 여부 표시 (아이콘)
 *
 * @param onDismiss BottomSheet 닫기 콜백
 * @param onTemplateSelected 템플릿 선택 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionBottomSheet(
    onDismiss: () -> Unit,
    onTemplateSelected: (ScheduleTemplate) -> Unit
) {
    val templates = remember { DefaultTemplates.ALL_TEMPLATES }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 제목
            Text(
                text = "템플릿 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 설명
            Text(
                text = "기본 시간대가 포함된 템플릿을 선택하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider()
            
            // 템플릿 목록
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onTemplateSelected(template) }
                    )
                }
            }
        }
    }
}

/**
 * 템플릿 선택 Dialog (중첩 AlertDialog용)
 *
 * AlertDialog 내부에서 호출 가능한 템플릿 선택 다이얼로그입니다.
 * ModalBottomSheet 대신 AlertDialog를 사용하여 레이어링 문제를 해결합니다.
 *
 * @param onDismiss Dialog 닫기 콜백
 * @param onTemplateSelected 템플릿 선택 콜백
 */
@Composable
fun TemplateSelectionDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (ScheduleTemplate) -> Unit
) {
    val templates = remember { DefaultTemplates.ALL_TEMPLATES }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "템플릿 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 설명
                Text(
                    text = "기본 시간대가 포함된 템플릿을 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // 템플릿 목록
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)  // 최대 높이 제한
                ) {
                    items(templates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onTemplateSelected(template) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 템플릿 카드 컴포넌트
 *
 * 개별 템플릿을 표시하는 카드입니다.
 *
 * @param template 템플릿 정보
 * @param onClick 클릭 콜백
 */
@Composable
private fun TemplateCard(
    template: ScheduleTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘
            Icon(
                imageVector = getIconForType(template.iconType),
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(template.colorHex)),
                modifier = Modifier.size(48.dp)
            )
            
            // 정보
            Column(modifier = Modifier.weight(1f)) {
                // 이름 + 위치 아이콘
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (template.hasLocation) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "위치 필요",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // 설명
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(4.dp))
                
                // 시간대 개수 + 요일
                Text(
                    text = "${template.timeSlots.size}개 시간대 · ${template.defaultDays.formatDays()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 화살표
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "선택",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 아이콘 타입에 따른 ImageVector 반환
 *
 * @param iconType 아이콘 타입 ("HOME", "WORK", "STUDY", "CALENDAR")
 * @return 해당하는 Material Icon
 */
@Composable
private fun getIconForType(iconType: String): ImageVector {
    return when (iconType) {
        "HOME" -> Icons.Default.Home
        "WORK" -> Icons.Default.Place  // Work 아이콘 대체
        "STUDY" -> Icons.Default.Star  // School 아이콘 대체  
        "CALENDAR" -> Icons.Default.DateRange
        else -> Icons.Default.Settings  // Schedule 아이콘 대체
    }
}

/**
 * 선택된 템플릿 미리보기 카드
 *
 * QuickCreateScheduleDialog에서 선택된 템플릿을 표시합니다.
 *
 * @param template 선택된 템플릿
 * @param onClear 선택 해제 콜백
 */
@Composable
fun SelectedTemplateCard(
    template: ScheduleTemplate,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 템플릿 정보
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getIconForType(template.iconType),
                        contentDescription = null,
                        tint = Color(android.graphics.Color.parseColor(template.colorHex)),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "선택된 템플릿: ${template.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    if (template.hasLocation) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "위치 필요",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${template.timeSlots.size}개 시간대 · ${template.defaultDays.formatDays()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // 선택 해제 버튼
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "템플릿 선택 해제",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

