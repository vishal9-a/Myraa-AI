package com.myraa.assistant.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.AssistantBrand
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaOrb
import com.myraa.assistant.ui.components.NovaOrbStyle
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.NovaOrbState
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val orbStyle: NovaOrbStyle,
    val orbState: NovaOrbState
)

@Composable
fun OnboardingScreen(
    appState: NovaAppState,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPageData(
            title = "Voice-First Companion",
            subtitle = "Talk seamlessly in Hindi, Hinglish, or English with real-time Gemini Live audio.",
            orbStyle = NovaOrbStyle.NOVA,
            orbState = NovaOrbState.IDLE
        ),
        OnboardingPageData(
            title = "Offline Hands-Free",
            subtitle = "Just say 'Hey Myraa' or 'Myraa' to control your phone even with the screen off and no internet.",
            orbStyle = NovaOrbStyle.ENERGY,
            orbState = NovaOrbState.LISTENING
        ),
        OnboardingPageData(
            title = "10 Dynamic Personalities",
            subtitle = "From Friendly and Teacher to Developer and Bollywood Nautanki, choose your companion's vibe.",
            orbStyle = NovaOrbStyle.PLASMA,
            orbState = NovaOrbState.THINKING
        ),
        OnboardingPageData(
            title = "3D Website Forge",
            subtitle = "Say 'Myraa, ek cafe ki website banao' and watch a complete animated 3D website build live.",
            orbStyle = NovaOrbStyle.ARCHER,
            orbState = NovaOrbState.SPEAKING
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip button top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            NovaButton(
                text = "Skip",
                onClick = {
                    appState.applyHasOnboarded(context, true)
                    onComplete()
                },
                type = NovaButtonType.TEXT
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIdx ->
            val page = pages[pageIdx]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (page.orbStyle == NovaOrbStyle.NOVA) {
                        MyraaOrb3D(state = page.orbState, size = 220.dp)
                    } else {
                        NovaOrb(state = page.orbState, size = 210.dp, style = page.orbStyle)
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Bottom Controls with Page Indicator and Next/Get Started
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in pages.indices) {
                    val isCurrent = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .size(width = if (isCurrent) 24.dp else 8.dp, height = 8.dp)
                            .background(if (isCurrent) colors.accent else colors.surfaceBorder, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val isLastPage = pagerState.currentPage == pages.size - 1
            NovaButton(
                text = if (isLastPage) "Get Started with Myraa" else "Continue",
                onClick = {
                    if (isLastPage) {
                        appState.applyHasOnboarded(context, true)
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                type = NovaButtonType.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
