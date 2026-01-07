package com.allday.detoxy.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.core.utils.BatteryOptimizationUtils

/**
 * 배터리 최적화 설정 가이드 다이얼로그
 * 
 * 제조사별 배터리 최적화 해제 방법을 안내하고
 * 관련 설정 화면으로 직접 이동할 수 있게 합니다.
 * 
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onSettingsClick 설정 화면 이동 버튼 클릭 콜백 (null이면 내부에서 처리)
 */
@Composable
fun BatteryOptimizationGuideDialog(
    onDismiss: () -> Unit,
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val manufacturer = BatteryOptimizationUtils.getManufacturer()
    val guideTitle = BatteryOptimizationUtils.getGuideTitle()
    val guideText = BatteryOptimizationUtils.getBatteryOptimizationGuideText()
    val isAggressive = BatteryOptimizationUtils.isAggressiveBatteryOptimizationManufacturer()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = "🔋 $guideTitle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isAggressive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ 이 설정을 하지 않으면 앱이 정상 작동하지 않을 수 있습니다",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = guideText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // 표준 배터리 최적화 예외 요청 버튼 (실패 시 설정 화면으로 fallback)
                Button(
                    onClick = {
                        val success = BatteryOptimizationUtils.requestBatteryOptimizationExemption(context)
                        if (!success) {
                            // Fallback: 배터리 최적화 설정 화면으로 이동
                            BatteryOptimizationUtils.openBatteryOptimizationSettings(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("배터리 최적화 해제 요청")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 제조사별 자동 시작 설정 버튼
                if (isAggressive) {
                    OutlinedButton(
                        onClick = {
                            if (onSettingsClick != null) {
                                onSettingsClick()
                            } else {
                                BatteryOptimizationUtils.openAutoStartSettings(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("자동 시작 설정 열기")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 닫기 버튼
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("나중에 하기")
                }
            }
        },
        dismissButton = null
    )
}

/**
 * 간소화된 배터리 최적화 안내 카드
 * 
 * 설정 화면이나 권한 체크 화면에서 인라인으로 표시할 수 있습니다.
 * 
 * @param onSettingsClick 설정 화면 이동 버튼 클릭 시 콜백
 */
@Composable
fun BatteryOptimizationCard(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status = BatteryOptimizationUtils.getBatteryOptimizationStatus(context)
    val isAggressive = BatteryOptimizationUtils.isAggressiveBatteryOptimizationManufacturer()
    val isWhitelisted = status == BatteryOptimizationUtils.BatteryOptimizationStatus.WHITELISTED
    
    // 적극적 최적화 제조사가 아니면 표시 안함
    if (!isAggressive) {
        return
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWhitelisted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isWhitelisted) "✅" else "🔋",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isWhitelisted) 
                            "배터리 최적화 설정 완료" 
                        else 
                            "배터리 최적화 설정 필요",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isWhitelisted)
                            "배터리 최적화에서 제외되었습니다"
                        else
                            "앱 차단 기능이 정상 작동하려면 배터리 최적화에서 제외해야 합니다",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 설정이 필요한 경우에만 버튼 표시
            if (!isWhitelisted) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onSettingsClick,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("설정하기", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
