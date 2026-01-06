package com.allday.detoxy.presentation.ui.timer

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.ui.component.GlassSurface

/**
 * 흑백 모드 안내 배너 (타이머 화면용)
 * 
 * Phase 6: 타이머 화면 배너
 * - Android 14 이하에서만 표시
 * - 흑백 모드가 활성화되어 있을 때만 표시
 * - "다시 보지 않기" 버튼으로 숨김 가능
 * 
 * @param onDismiss "다시 보지 않기" 콜백
 * @param onNavigateToSettings 시스템 설정 이동 콜백
 */
@Composable
fun GrayscaleTipBanner(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Android 15+에서는 표시하지 않음 (자동 흑백 모드 지원)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) return

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.4f,
        tint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💡", style = MaterialTheme.typography.titleMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "흑백 모드 활성화 방법",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "시스템 설정에서 직접 활성화해주세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 다시 보지 않기
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("다시 보지 않기")
                }
                
                // 설정으로 이동
                Button(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("설정 열기")
                }
            }
        }
    }
}
