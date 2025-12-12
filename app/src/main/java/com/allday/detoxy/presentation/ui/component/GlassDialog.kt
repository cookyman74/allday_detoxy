package com.allday.detoxy.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.CompositionLocalProvider
import com.allday.detoxy.presentation.ui.theme.LocalHazeState

/**
 * Glassmorphism 스타일이 적용된 다이얼로그
 *
 * 기본 [AlertDialog]를 대체하며, [GlassSurface]를 컨테이너로 사용하여
 * 앱의 전반적인 Liquid Glass 디자인 컨셉을 유지합니다.
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 시 호출되는 콜백
 * @param content 다이얼로그 내용
 * @param properties 다이얼로그 속성 (기본값 사용)
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        // Disable Haze in Dialogs to prevent cross-window RenderNode issues
        CompositionLocalProvider(LocalHazeState provides null) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                alpha = 0.5f
            ) {
                content()
            }
        }
    }
}
