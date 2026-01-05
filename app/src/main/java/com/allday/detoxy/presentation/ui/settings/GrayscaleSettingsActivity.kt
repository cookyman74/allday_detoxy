package com.allday.detoxy.presentation.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

/**
 * 흑백 모드 설정 Activity (AutomaticZenRule 설정 화면)
 * 
 * AutomaticZenRule.setConfigurationActivity()에서 참조됩니다.
 * 시스템 설정에서 "Detoxy 집중 모드" 룰을 탭하면 이 화면이 열립니다.
 * 
 * ⚠️ Phase 7에서 완성 예정 (현재는 스텁)
 * 
 * @see GrayscaleManager
 */
@AndroidEntryPoint
class GrayscaleSettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                GrayscaleSettingsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun GrayscaleSettingsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("흑백 모드 설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "흑백 모드 설정",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "이 화면은 디톡시 앱 내 설정에서 관리됩니다.\n" +
                       "디톡시 앱을 열어 설정을 변경하세요.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onBackClick) {
                Text("닫기")
            }
        }
    }
}
