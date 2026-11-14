package com.hakim.clinical.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hakim.clinical.ui.components.AuroraBackground
import com.hakim.clinical.ui.screens.HistoryScreen
import com.hakim.clinical.ui.screens.HomeScreen
import com.hakim.clinical.ui.screens.ResultScreen
import com.hakim.clinical.ui.screens.SettingsScreen
import com.hakim.clinical.ui.screens.SetupScreen
import com.hakim.clinical.ui.theme.Hk

@Composable
fun HakimRoot(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    // خلّي الشاشة شاعلة أثناء التحليل
    val view = LocalView.current
    SideEffect { view.keepScreenOn = state.isGenerating }

    BackHandler(enabled = state.screen == Screen.Result || state.screen == Screen.History || state.screen == Screen.Settings) {
        vm.back()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    val pickModel: () -> Unit = { picker.launch(arrayOf("*/*")) }

    AuroraBackground {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 24 }) togetherWith fadeOut(tween(160))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Setup -> SetupScreen(state, onPickModel = pickModel, onCancelImport = vm::cancelImport)
                Screen.Home -> HomeScreen(
                    state = state,
                    onCaseChange = vm::updateCase,
                    onVitalsChange = vm::updateVitals,
                    onExample = vm::fillExample,
                    onClear = vm::clearCase,
                    onAnalyze = vm::analyze,
                    onRetryLoad = vm::retryLoad,
                    onPickModel = pickModel,
                )
                Screen.Result -> ResultScreen(
                    state = state,
                    onBack = { vm.back() },
                    onStop = vm::stop,
                    onNewCase = {
                        vm.clearCase()
                        vm.navigate(Screen.Home)
                    },
                    onEditAgain = vm::editAgain,
                )
                Screen.History -> HistoryScreen(
                    items = state.history,
                    onOpen = vm::openHistory,
                    onDelete = vm::deleteHistory,
                    onClearAll = vm::clearHistory,
                )
                Screen.Settings -> SettingsScreen(
                    state = state,
                    onUpdate = vm::updateSettings,
                    onPickModel = pickModel,
                    onCancelImport = vm::cancelImport,
                )
            }
        }

        val showNav = state.screen == Screen.Home || state.screen == Screen.History || state.screen == Screen.Settings
        AnimatedVisibility(
            visible = showNav,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomNav(state.screen) { vm.navigate(it) }
        }
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Hk.Violet)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Hk.Bg2.copy(alpha = 0.96f), Hk.Bg1.copy(alpha = 0.96f))))
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NavItem("حالة جديدة", Icons.Rounded.MedicalServices, current == Screen.Home, Modifier.weight(1f)) { onSelect(Screen.Home) }
        NavItem("السجل", Icons.Rounded.History, current == Screen.History, Modifier.weight(1f)) { onSelect(Screen.History) }
        NavItem("الإعدادات", Icons.Rounded.Tune, current == Screen.Settings, Modifier.weight(1f)) { onSelect(Screen.Settings) }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(Hk.Brand) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Hk.Bg0 else Hk.TextDim, modifier = Modifier.size(20.dp))
        Text(
            "  $label",
            color = if (selected) Hk.Bg0 else Hk.TextDim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}
