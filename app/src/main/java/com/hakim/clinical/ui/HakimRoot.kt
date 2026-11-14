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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hakim.clinical.ui.components.AppBackground
import com.hakim.clinical.ui.screens.HistoryScreen
import com.hakim.clinical.ui.screens.LockScreen
import com.hakim.clinical.ui.screens.PatientDetailScreen
import com.hakim.clinical.ui.screens.PatientEditScreen
import com.hakim.clinical.ui.screens.PatientsScreen
import com.hakim.clinical.ui.screens.QuickScreen
import com.hakim.clinical.ui.screens.ResultScreen
import com.hakim.clinical.ui.screens.SettingsScreen
import com.hakim.clinical.ui.screens.SetupScreen
import com.hakim.clinical.ui.screens.VisitScreen
import com.hakim.clinical.ui.theme.Hk
import kotlinx.coroutines.delay

/** جسر للبصمة/رمز الجهاز (بينفّذه MainActivity). */
interface AuthBridge {
    fun available(): Boolean
    fun authenticate(onSuccess: () -> Unit)
}

@Composable
fun HakimRoot(vm: MainViewModel, auth: AuthBridge) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Hk.c

    val view = LocalView.current
    SideEffect { view.keepScreenOn = state.isGenerating }

    BackHandler(enabled = !state.locked && state.screen != Screen.Patients) { vm.back() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    val pickModel: () -> Unit = { picker.launch(arrayOf("*/*")) }

    AppBackground {
        if (state.locked) {
            LockScreen(onUnlock = { auth.authenticate { vm.unlock() } })
        } else {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 30 }) togetherWith fadeOut(tween(140))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Setup -> SetupScreen(state, onPickModel = pickModel, onCancelImport = vm::cancelImport, onSkip = vm::skipSetup)
                Screen.Patients -> PatientsScreen(
                    state = state,
                    onQuery = vm::setPatientQuery,
                    onOpen = vm::openPatient,
                    onNew = vm::newPatient,
                )
                Screen.PatientEdit -> {
                    val d = state.patientDraft
                    if (d != null) {
                        PatientEditScreen(
                            draft = d,
                            isNew = state.patientDraftIsNew,
                            onChange = vm::updatePatientDraft,
                            onSave = vm::savePatient,
                            onBack = { vm.back() },
                        )
                    }
                }
                Screen.PatientDetail -> PatientDetailScreen(
                    state = state,
                    onBack = { vm.back() },
                    onEdit = vm::editPatient,
                    onDelete = vm::deletePatient,
                    onNewVisit = vm::newVisit,
                    onOpenVisit = vm::openVisit,
                    onTab = vm::setDetailTab,
                    onExportPdf = vm::exportPdf,
                )
                Screen.Visit -> VisitScreen(
                    state = state,
                    onBack = { vm.back() },
                    onChange = vm::updateVisitDraft,
                    onAnalyze = vm::analyzeVisit,
                    onStop = vm::stop,
                    onSave = vm::saveVisit,
                    onDelete = vm::deleteVisit,
                    onOpenSettings = { vm.navigate(Screen.Settings) },
                )
                Screen.Quick -> QuickScreen(
                    state = state,
                    onCaseChange = vm::updateCase,
                    onVitalsChange = vm::updateVitals,
                    onExample = vm::fillExample,
                    onClear = vm::clearCase,
                    onAnalyze = vm::analyzeQuick,
                    onRetryLoad = vm::retryLoad,
                    onOpenSettings = { vm.navigate(Screen.Settings) },
                )
                Screen.Result -> ResultScreen(
                    state = state,
                    onBack = { vm.back() },
                    onStop = vm::stop,
                    onNewCase = vm::newQuickCase,
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
                    onToggleLock = { enable ->
                        if (!enable) {
                            vm.updateSettings { it.copy(lockEnabled = false) }
                        } else if (!auth.available()) {
                            vm.toast("لازم تفعّل قفل شاشة (بصمة أو رمز) بالجهاز أول.")
                        } else {
                            auth.authenticate { vm.updateSettings { it.copy(lockEnabled = true) } }
                        }
                    },
                    onExportBackup = vm::exportBackup,
                    onImportBackup = vm::importBackup,
                )
            }
        }

        val tabs = state.screen == Screen.Patients || state.screen == Screen.Quick ||
            state.screen == Screen.History || state.screen == Screen.Settings
        Column(Modifier.align(Alignment.BottomCenter)) {
            AnimatedVisibility(
                visible = state.screen == Screen.Patients && state.patients.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.End),
            ) {
                NewPatientFab(vm::newPatient)
            }
            AnimatedVisibility(
                visible = tabs,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                BottomNav(state.screen) { vm.navigate(it) }
            }
        }

        val toast = state.toast
        if (toast != null) {
            LaunchedEffect(toast) {
                delay(2600)
                vm.consumeToast()
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp, start = 24.dp, end = 24.dp)
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (c.isDark) c.surfaceAlt else c.text)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(toast, style = MaterialTheme.typography.bodyMedium.copy(color = if (c.isDark) c.text else c.bg))
            }
        }
        }
    }
}

@Composable
private fun NewPatientFab(onClick: () -> Unit) {
    val c = Hk.c
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .padding(start = 22.dp, end = 22.dp, bottom = 4.dp)
            .shadow(12.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .clip(shape)
            .background(c.brand)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(22.dp))
        Text("  مريض جديد", style = MaterialTheme.typography.labelLarge.copy(color = c.onPrimary, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    val c = Hk.c
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NavItem("المرضى", Icons.Rounded.People, current == Screen.Patients, Modifier.weight(1f)) { onSelect(Screen.Patients) }
        NavItem("تحليل سريع", Icons.Rounded.AutoAwesome, current == Screen.Quick, Modifier.weight(1f)) { onSelect(Screen.Quick) }
        NavItem("السجل", Icons.Rounded.History, current == Screen.History, Modifier.weight(1f)) { onSelect(Screen.History) }
        NavItem("الإعدادات", Icons.Rounded.Tune, current == Screen.Settings, Modifier.weight(1f)) { onSelect(Screen.Settings) }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Hk.c
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) c.primarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) c.primary else c.textFaint, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) c.primary else c.textFaint,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}
