package com.allday.detoxy.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.R
import com.allday.detoxy.core.locale.AppLocaleManager
import com.allday.detoxy.presentation.ui.component.GlassSurface
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme

@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit
) {
    // 현재 설정된 언어 태그를 가져옴
    // Activity가 재생성되므로 remember로 상태를 유지할 필요는 없으나,
    // 초기 진입 시 값을 읽어오기 위해 사용
    val currentTag = remember { AppLocaleManager.getAppLocale() }
    val currentLanguage = remember(currentTag) {
        AppLocaleManager.findSupportedLanguage(currentTag)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 상단 헤더
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            alpha = 0.4f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.language_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 언어 목록
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                alpha = 0.35f
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    AppLocaleManager.getAllSupportedLanguages().forEach { language ->
                        LanguageOptionItem(
                            language = language,
                            isSelected = language == currentLanguage,
                            onClick = {
                                if (language != currentLanguage) {
                                    AppLocaleManager.setAppLocale(language.languageTag)
                                    // 주의: 언어 설정 시 Activity가 재생성되므로 여기서 별도의 네비게이션 동작을 할 필요 없음
                                    // (자동으로 화면이 갱신되거나 홈으로 갈 수 있음 - 동작 확인 필요)
                                }
                            }
                        )
                        if (language != AppLocaleManager.getAllSupportedLanguages().last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageOptionItem(
    language: AppLocaleManager.SupportedLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (language.languageTag.isNotEmpty()) {
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null, // 장식용 아이콘
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
