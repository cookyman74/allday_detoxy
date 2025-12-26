package com.allday.detoxy.presentation.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * 🆕 v9: 세션 종료 목표 달성 다이얼로그
 * 
 * 할일 없이 목표만 있는 경우 표시되는 예/아니오 다이얼로그입니다.
 * 30초 타이머가 표시되며, 시간 초과 시 미완료로 기록됩니다.
 * 
 * @param goal 목표 텍스트
 * @param onComplete 완료 선택 시 호출되는 콜백 (true: 완료, false: 미완료)
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun SessionEndGoalDialog(
    goal: String,
    onComplete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // 30초 카운트다운
    var remainingSeconds by remember { mutableStateOf(30) }
    
    // 이중 저장 방지 플래그
    var isCompleted by remember { mutableStateOf(false) }
    
    // 안전한 완료 처리 함수
    fun safeComplete(completed: Boolean) {
        if (!isCompleted) {
            isCompleted = true
            onComplete(completed)
        }
    }
    
    // 30초 타이머
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isCompleted) {
            delay(1000)
            remainingSeconds--
        }
        // 타임아웃 시 미완료로 저장
        if (!isCompleted) {
            safeComplete(false)
        }
    }
    
    Dialog(onDismissRequest = { if (!isCompleted) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 헤더
                Text(
                    text = "🎉",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "집중 세션 완료!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 목표 표시
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎯 목표",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "✅ 목표를 달성하셨나요?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 예/아니오 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { safeComplete(true) },
                        enabled = !isCompleted,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("예 ✅")
                    }
                    
                    OutlinedButton(
                        onClick = { safeComplete(false) },
                        enabled = !isCompleted,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("아니오")
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 타이머 표시
                Text(
                    text = "⏱️ ${remainingSeconds}초 후 \"미완료\"로 기록됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { remainingSeconds / 30f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "(목표는 필수 항목이므로 미응답 시 미완료 처리)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
