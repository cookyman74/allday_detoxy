package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allday.detoxy.domain.validation.ScheduleInfoValidation

/**
 * 목표 입력 섹션
 * 
 * 스케줄 생성/편집 다이얼로그에서 집중 목표를 입력받습니다.
 * 
 * @param title 현재 목표 텍스트
 * @param onTitleChange 목표 변경 콜백
 * @param showTodoSection 할일 섹션 표시 여부
 * @param onToggleTodoSection 할일 섹션 토글 콜백
 * @param modifier Modifier
 */
@Composable
fun GoalInputSection(
    title: String,
    onTitleChange: (String) -> Unit,
    showTodoSection: Boolean,
    onToggleTodoSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxLength = ScheduleInfoValidation.MAX_TITLE_LENGTH
    val isNearLimit = title.length > maxLength - 5
    val isOverLimit = title.length > maxLength
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 섹션 헤더
        Text(
            text = "🎯 집중 목표",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 목표 입력 필드
        OutlinedTextField(
            value = title,
            onValueChange = { newValue ->
                if (newValue.length <= maxLength) {
                    onTitleChange(newValue)
                }
            },
            placeholder = { 
                Text(
                    text = "예: 보고서 초안 완성하기",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            isError = isOverLimit,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOverLimit) "글자 수 제한을 초과했습니다" else "선택 사항입니다",
                        color = if (isOverLimit) MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${title.length}/$maxLength",
                        color = when {
                            isOverLimit -> MaterialTheme.colorScheme.error
                            isNearLimit -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )
        
        // 세부 항목 추가 버튼 (할일 섹션이 숨겨진 경우에만)
        if (!showTodoSection) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onToggleTodoSection,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("세부 할일 추가")
            }
        }
    }
}
