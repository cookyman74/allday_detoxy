package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 빠른 시간표 생성 다이얼로그 (3차 고도화 개선)
 *
 * 위치 기반 자동 실행 설정 중 간단하게 시간표를 생성할 수 있는 Dialog입니다.
 * AddScheduleGroupDialog의 간소화된 버전으로, 이름만 입력받습니다.
 *
 * ## 특징
 * - 최소한의 입력만 요구 (이름만)
 * - 빠른 생성 후 자동 선택
 * - 고급 설정은 [스케줄 그룹] 화면에서 수정 가능
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 확인 콜백 (시간표 이름 전달)
 * @param locationLabel 위치 라벨 (기본값 제안용)
 */
@Composable
fun QuickCreateScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
    locationLabel: String = ""
) {
    var name by remember { 
        mutableStateOf(
            if (locationLabel.isNotBlank()) {
                "$locationLabel 시간표"
            } else {
                ""
            }
        ) 
    }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "시간표 만들기",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 안내 텍스트
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "💡 시간표를 만든 후, 시간 기반 자동 실행 설정에서 시간대(예: 오전 10시, 오후 2시)를 추가할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                // 시간표 이름
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("시간표 이름 *") },
                    placeholder = { Text("예: 업무 시간표, 공부 루틴") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                
                // 사용 예시
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📋 사용 예시",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "• 회사 → 업무 시간표 (10시, 14시, 16시)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "• 도서관 → 공부 시간표 (9시, 14시)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "• 헬스장 → 운동 시간표 (19시)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "시간표 이름을 입력해주세요"
                    } else {
                        onConfirm(name.trim())
                    }
                }
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

