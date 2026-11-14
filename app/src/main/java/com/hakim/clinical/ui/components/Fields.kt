package com.hakim.clinical.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.Vitals
import com.hakim.clinical.ui.theme.Hk

/** خانة كتابة موحّدة الشكل. */
@Composable
fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 1,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    accent: Color? = null,
    leadingIcon: ImageVector? = null,
) {
    val c = Hk.c
    val acc = accent ?: c.primary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, color = c.textFaint, style = MaterialTheme.typography.bodyMedium) }
        } else {
            null
        },
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = c.textDim) }
        } else {
            null
        },
        minLines = if (singleLine) 1 else minLines,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = acc,
            unfocusedBorderColor = c.border,
            focusedLabelColor = acc,
            unfocusedLabelColor = c.textDim,
            cursorColor = acc,
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
        ),
    )
}

/** خانات العلامات الحيوية. */
@Composable
fun VitalsEditor(v: Vitals, onChange: (Vitals) -> Unit, showExtra: Boolean = true) {
    val c = Hk.c
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppField(v.bp, { onChange(v.copy(bp = it.take(9))) }, "الضغط", Modifier.weight(1f), "120/80", singleLine = true, accent = c.pink)
            AppField(v.hr, { onChange(v.copy(hr = it.take(5))) }, "النبض /د", Modifier.weight(1f), "80", singleLine = true, keyboardType = KeyboardType.Number, accent = c.pink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppField(v.temp, { onChange(v.copy(temp = it.take(5))) }, "الحرارة °م", Modifier.weight(1f), "37", singleLine = true, keyboardType = KeyboardType.Decimal, accent = c.pink)
            AppField(v.spo2, { onChange(v.copy(spo2 = it.take(5))) }, "الأكسجة ٪", Modifier.weight(1f), "98", singleLine = true, keyboardType = KeyboardType.Number, accent = c.pink)
            AppField(v.rr, { onChange(v.copy(rr = it.take(5))) }, "التنفس /د", Modifier.weight(1f), "16", singleLine = true, keyboardType = KeyboardType.Number, accent = c.pink)
        }
        if (showExtra) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(v.glucose, { onChange(v.copy(glucose = it.take(6))) }, "السكر ملغ/دل", Modifier.weight(1f), "110", singleLine = true, keyboardType = KeyboardType.Decimal, accent = c.pink)
                AppField(v.weight, { onChange(v.copy(weight = it.take(6))) }, "الوزن كغ", Modifier.weight(1f), "70", singleLine = true, keyboardType = KeyboardType.Decimal, accent = c.pink)
            }
        }
    }
}

@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    val c = Hk.c
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceAlt)
            .border(1.dp, c.border, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val isSel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSel) c.surface else Color.Transparent)
                    .border(1.dp, if (isSel) c.primary.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable { onSelect(o) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isSel) c.primary else c.textDim,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Hk.c
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.textDim,
                uncheckedTrackColor = c.surfaceAlt,
                uncheckedBorderColor = c.border,
            ),
        )
    }
}
