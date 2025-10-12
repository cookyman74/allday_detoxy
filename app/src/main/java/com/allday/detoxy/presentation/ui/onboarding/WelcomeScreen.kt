package com.allday.detoxy.presentation.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 온보딩 환영 화면
 *
 * 앱 최초 실행 시 사용자에게 앱의 목적과 주요 기능을 소개합니다.
 * MVP 범위: 간소화된 3단계 온보딩
 * - Welcome (환영)
 * - Permission (권한 안내)
 * - Complete (완료)
 *
 * @param onNextClick 다음 단계로 이동하는 콜백
 */
@Composable
fun WelcomeScreen(
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 앱 아이콘/로고
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 앱 이름
        Text(
            text = "Allday Detoxy",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 서브 타이틀
        Text(
            text = "스마트폰 습관 교정 코치",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 주요 기능 안내
        FeatureCard(
            emoji = "⏰",
            title = "타이머 기반 집중 모드",
            description = "25분, 45분, 60분 프리셋으로 집중 시간을 설정하세요"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            emoji = "🚫",
            title = "앱 차단 기능",
            description = "집중 모드 중 Instagram, TikTok, YouTube 등을 자동 차단"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            emoji = "🏆",
            title = "보상 시스템",
            description = "성공할 때마다 포인트를 받고 연속 성공 기록을 쌓아가세요"
        )

        Spacer(modifier = Modifier.weight(1f))

        // 시작 버튼
        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "시작하기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 기능 소개 카드
 *
 * @param emoji 기능을 나타내는 이모지
 * @param title 기능 제목
 * @param description 기능 설명
 */
@Composable
private fun FeatureCard(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이모지 아이콘
            Text(
                text = emoji,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

