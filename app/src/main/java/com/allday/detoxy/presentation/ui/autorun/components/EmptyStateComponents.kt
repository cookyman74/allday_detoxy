package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 비어있는 상태 (Empty State) UI with 템플릿 버튼
 *
 * 등록된 시간대가 없을 때 표시되며, 템플릿으로 빠르게 시작할 수 있도록 유도합니다.
 *
 * @param onTemplateClick 템플릿으로 시작하기 버튼 클릭 콜백
 */
@Composable
fun EmptyStateWithTemplate(
    onTemplateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⏰",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "등록된 시간대가 없습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "특정 시간에 자동으로 집중 모드가 시작되도록 설정해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onTemplateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("템플릿으로 시작하기")
            }
        }
    }
}

/**
 * 템플릿 선택 버튼 (등록된 시간대가 있을 때)
 *
 * @param enabled 버튼 활성화 여부 (최대 10개 제한)
 * @param onClick 버튼 클릭 콜백
 */
@Composable
fun TemplateSelectionButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (enabled) "⏰ 템플릿으로 추가하기" else "최대 10개까지 등록 가능합니다",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

