package com.allday.detoxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.presentation.ui.permission.AccessibilityPermissionDialog
import com.allday.detoxy.presentation.ui.permission.PermissionGuideCard
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 메인 액티비티
 *
 * @AndroidEntryPoint 어노테이션으로 Hilt가 의존성 주입 가능
 * Jetpack Compose를 사용하여 UI 렌더링
 * 접근성 권한 체크 및 안내 기능 포함
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DetoxyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    // 접근성 권한 상태 확인
    LaunchedEffect(Unit) {
        // TODO: ViewModel로 이동 예정 (Week 3)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Greeting(
                name = "Allday Detoxy",
                modifier = Modifier.padding(top = 16.dp)
            )

            // 권한 안내 카드 (MVP 간소화)
            PermissionGuideCard(
                isGranted = isAccessibilityEnabled,
                onOpenSettings = {
                    // TODO: Context 전달 방법 개선 필요 (Week 3)
                    showPermissionDialog = true
                }
            )
        }

        // 권한 안내 다이얼로그
        if (showPermissionDialog) {
            AccessibilityPermissionDialog(
                onDismiss = { showPermissionDialog = false },
                onOpenSettings = {
                    // TODO: MainActivity context 사용 개선 필요
                    showPermissionDialog = false
                    // PermissionUtils.openAccessibilitySettings(this)
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DetoxyTheme {
        Greeting("Allday Detoxy")
    }
}
