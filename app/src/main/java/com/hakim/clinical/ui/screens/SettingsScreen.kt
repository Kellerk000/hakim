package com.hakim.clinical.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.AppSettings
import com.hakim.clinical.ui.ModelStatus
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.SecondaryButton
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.Segmented
import com.hakim.clinical.ui.components.SwitchRow
import com.hakim.clinical.ui.components.TopBar
import com.hakim.clinical.ui.components.gb
import com.hakim.clinical.ui.theme.Hk
import com.hakim.clinical.ui.theme.MonoSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: UiState,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onPickModel: () -> Unit,
    onCancelImport: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onExportBackup: (Uri, String) -> Unit,
    onImportBackup: (Uri, String) -> Unit,
) {
    val c = Hk.c
    val s = state.settings

    var askExportPw by remember { mutableStateOf(false) }
    var exportPw by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pw = exportPw
        if (uri != null && pw != null) onExportBackup(uri, pw)
        exportPw = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        TopBar("الإعدادات", onBack = null)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // المظهر
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("المظهر", Icons.Rounded.Palette, c.secondary)
                Gap(12.dp)
                Segmented(
                    options = listOf(0, 1, 2),
                    selected = s.themeMode,
                    label = { listOf("حسب الجهاز", "نهاري", "ليلي")[it] },
                ) { m -> onUpdate { it.copy(themeMode = m) } }
            }

            // الحماية والنسخ الاحتياطي
            AppCard(Modifier.fillMaxWidth(), accent = c.primary) {
                SectionTitle("الحماية والبيانات", Icons.Rounded.Lock, c.primary)
                Gap(10.dp)
                SwitchRow(
                    "قفل التطبيق",
                    "بالبصمة أو رمز الجهاز، وقت الفتح وبعد دقيقة بالخلفية.",
                    s.lockEnabled,
                    onToggleLock,
                )
                Gap(14.dp)
                Text(
                    "بيانات المرضى مشفّرة على الجهاز. إذا ضاع الموبايل أو انمسح التطبيق بتضيع، فاعمل نسخة احتياطية كل فترة واحفظها بمكان آمن.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Gap(12.dp)
                if (state.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = c.primary, strokeWidth = 2.dp, modifier = Modifier.padding(4.dp))
                        Text("  لحظة…", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("نسخة احتياطية", onClick = { askExportPw = true }, icon = Icons.Rounded.Backup, color = c.primary, modifier = Modifier.weight(1f))
                        SecondaryButton("استرجاع", onClick = { importLauncher.launch(arrayOf("*/*")) }, icon = Icons.Rounded.Restore, modifier = Modifier.weight(1f))
                    }
                }
            }

            // النموذج
            AppCard(Modifier.fillMaxWidth(), accent = c.info) {
                SectionTitle("نموذج الذكاء", Icons.Rounded.Memory, c.info)
                Gap(10.dp)
                Text(s.modelName ?: "ما في نموذج", style = MaterialTheme.typography.titleMedium)
                val status = when (state.modelStatus) {
                    ModelStatus.Ready -> "جاهز"
                    ModelStatus.Loading -> "عم يتحمّل…"
                    ModelStatus.Failed -> "فشل التحميل"
                    ModelStatus.None -> "غير محمّل"
                }
                val size = if (s.modelSize > 0) " · ${gb(s.modelSize)} غيغا" else ""
                Text("$status$size", style = MaterialTheme.typography.bodySmall)
                if (state.modelDesc.isNotBlank()) Text(state.modelDesc, style = MonoSmall.copy(color = c.textFaint))
                if (state.deviceRamBytes > 0 && s.modelSize > 0 && s.modelSize > state.deviceRamBytes * 0.55) {
                    Gap(8.dp)
                    Text(
                        "تنبيه: النموذج كبير بالنسبة لذاكرة جهازك (${gb(state.deviceRamBytes)} غيغا). إذا صار بطيء أو سكّر، جرّب نسخة أصغر.",
                        style = MaterialTheme.typography.bodySmall.copy(color = c.warning),
                    )
                }
                Gap(14.dp)
                ImportBlock(state.importState, onPickModel, onCancelImport)
            }

            // الأداء
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("الأداء والجودة", Icons.Rounded.Speed, c.secondary)
                SliderRow("عدد الأنوية", "${s.threads}", "٤ لـ ٦ أنسب للأجهزة الحديثة.", s.threads.toFloat(), 1f..8f, 6) { v ->
                    onUpdate { it.copy(threads = v.roundToInt()) }
                }
                Text("حجم السياق", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Text("أكبر = بيستوعب حالات وسوابق أطول، بس بياخد ذاكرة أكتر.", style = MaterialTheme.typography.bodySmall)
                Gap(8.dp)
                Segmented(options = listOf(2048, 4096, 8192), selected = s.nCtx, label = { "$it" }) { v -> onUpdate { it.copy(nCtx = v) } }
                SliderRow("أقصى طول للجواب", "${s.maxTokens}", "أطول = تفاصيل أكتر ووقت أطول.", s.maxTokens.toFloat(), 300f..1600f, 12) { v ->
                    onUpdate { it.copy(maxTokens = (v / 100f).roundToInt() * 100) }
                }
                SliderRow(
                    "درجة الإبداع",
                    String.format(Locale.US, "%.1f", s.temperature),
                    "منخفضة = أجوبة ثابتة ومنضبطة، وهي الأنسب للطب.",
                    s.temperature, 0f..1f, 9,
                ) { v -> onUpdate { it.copy(temperature = (v * 10f).roundToInt() / 10f) } }
            }

            // المكتبة
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("المكتبة المرجعية", Icons.Rounded.CollectionsBookmark, c.pink)
                Gap(8.dp)
                SwitchRow(
                    "استعملها بالتحليل",
                    "184 تشخيص بتنعرض دائماً للمقارنة. إذا فعّلتها بتنبعت للنموذج كتلميح، وممكن تشدّه لتشخيص غلط.",
                    s.useReferences,
                ) { v -> onUpdate { it.copy(useReferences = v) } }
            }

            // حول
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("حول DrAli", Icons.Rounded.Info, c.info)
                Gap(8.dp)
                Text(
                    "DrAli أداة مساعدة للطبيب، مو بديل عن التقييم السريري. النموذج ممكن يغلط وهو واثق، فراجع كل اقتراح بحكمك وبالإرشادات المحلية. التطبيق ما عنده إذن إنترنت، وكل شي محفوظ على الجهاز.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.systemInfo.isNotBlank()) {
                    Gap(10.dp)
                    Text(state.systemInfo, style = MonoSmall.copy(color = c.textFaint))
                }
            }
            Gap(150.dp)
        }
    }

    if (askExportPw) {
        PasswordDialog(
            title = "كلمة سر للنسخة الاحتياطية",
            message = "رح تحتاجها لتسترجع النسخة. إذا نسيتها ما في طريقة لفتح الملف.",
            confirm = true,
            onDismiss = { askExportPw = false },
        ) { pw ->
            askExportPw = false
            exportPw = pw
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            exportLauncher.launch("DrAli-backup-$date.drali")
        }
    }
    val pendingImport = importUri
    if (pendingImport != null) {
        PasswordDialog(
            title = "استرجاع نسخة احتياطية",
            message = "اكتب كلمة سر النسخة. المرضى الموجودين بيضلّوا، واللي بالنسخة بينضافوا أو بيتحدّثوا.",
            confirm = false,
            onDismiss = { importUri = null },
        ) { pw ->
            importUri = null
            onImportBackup(pendingImport, pw)
        }
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    confirm: Boolean,
    onDismiss: () -> Unit,
    onOk: (String) -> Unit,
) {
    val c = Hk.c
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val valid = pw.length >= 6 && (!confirm || pw == pw2)
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.border,
        cursorColor = c.primary,
        focusedTextColor = c.text,
        unfocusedTextColor = c.text,
        focusedLabelColor = c.primary,
        unfocusedLabelColor = c.textDim,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodySmall)
                Gap(12.dp)
                OutlinedTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    label = { Text("كلمة السر (٦ أحرف على الأقل)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp),
                    colors = colors,
                )
                if (confirm) {
                    Gap(8.dp)
                    OutlinedTextField(
                        value = pw2,
                        onValueChange = { pw2 = it },
                        label = { Text("أعد كتابتها") },
                        singleLine = true,
                        isError = pw2.isNotEmpty() && pw2 != pw,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        colors = colors,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOk(pw) }, enabled = valid) { Text("متابعة", color = if (valid) c.primary else c.textFaint) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = c.textDim) } },
        containerColor = c.surface,
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: String,
    hint: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    val c = Hk.c
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(color = c.secondary))
        }
        Text(hint, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = current.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.secondary,
                activeTrackColor = c.secondary,
                inactiveTrackColor = c.surfaceAlt,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}
