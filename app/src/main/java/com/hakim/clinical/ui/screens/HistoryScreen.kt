package com.hakim.clinical.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.ArabicText
import com.hakim.clinical.data.HistoryItem
import com.hakim.clinical.data.Mode
import com.hakim.clinical.data.Urgency
import com.hakim.clinical.ui.components.EmptyState
import com.hakim.clinical.ui.components.GlassCard
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.theme.Hk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onOpen: (HistoryItem) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale("ar")) }
    val filtered = remember(items, query) {
        if (query.isBlank()) items
        else {
            val q = ArabicText.normalize(query.trim())
            items.filter { ArabicText.normalize(it.title + " " + it.caseText).contains(q) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("سجل الحالات", style = MaterialTheme.typography.headlineSmall)
                Text("${items.size} حالة محفوظة على الجهاز", style = MaterialTheme.typography.bodySmall)
            }
            if (items.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "حذف الكل", tint = Hk.TextDim)
                }
            }
        }
        if (items.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Hk.TextDim) },
                placeholder = { Text("ابحث بالتشخيص أو الشكوى", color = Hk.TextFaint) },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Hk.Cyan,
                    unfocusedBorderColor = Hk.Stroke,
                    cursorColor = Hk.Cyan,
                    focusedTextColor = Hk.Text,
                    unfocusedTextColor = Hk.Text,
                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                ),
            )
        }

        if (items.isEmpty()) {
            EmptyState(
                Icons.Rounded.History,
                "ما في حالات لسا",
                "كل حالة بتحللها بتنحفظ هون تلقائياً، على جهازك بس.",
            )
        } else if (filtered.isEmpty()) {
            EmptyState(Icons.Rounded.Search, "ما في نتائج", "جرّب كلمة تانية.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { item ->
                    HistoryRow(item, fmt.format(Date(item.time)), onOpen, onDelete)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("حذف كل السجل؟") },
            text = { Text("رح تنحذف كل الحالات المحفوظة، وما فيك ترجعها.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("حذف", color = Hk.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("إلغاء") }
            },
            containerColor = Hk.Bg2,
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, date: String, onOpen: (HistoryItem) -> Unit, onDelete: (Long) -> Unit) {
    val urgencyColor = when (item.urgency) {
        Urgency.EMERGENCY -> Hk.Danger
        Urgency.URGENT -> Hk.Warning
        Urgency.ROUTINE -> Hk.Success
        Urgency.UNKNOWN -> Hk.Sky
    }
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onOpen(item) }) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(urgencyColor)
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.mode == Mode.MULTIPLE) {
                        Pill("احتمالات", Hk.MultiGrad, Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    item.caseText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(date, style = MaterialTheme.typography.labelMedium, color = Hk.TextFaint, modifier = Modifier.padding(top = 6.dp))
            }
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Hk.TextFaint)
            }
        }
    }
}
