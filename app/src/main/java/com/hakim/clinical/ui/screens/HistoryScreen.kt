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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.ArabicText
import com.hakim.clinical.data.HistoryItem
import com.hakim.clinical.data.Mode
import com.hakim.clinical.data.Urgency
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.AppField
import com.hakim.clinical.ui.components.EmptyState
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.TopBar
import com.hakim.clinical.ui.components.friendlyDate
import com.hakim.clinical.ui.theme.Hk

@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onOpen: (HistoryItem) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    val c = Hk.c
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
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
        TopBar("سجل التحليلات السريعة", onBack = null, subtitle = "${items.size} تحليل محفوظ ومشفّر على الجهاز") {
            if (items.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "حذف الكل", tint = c.textDim)
                }
            }
        }
        if (items.isNotEmpty()) {
            AppField(query, { query = it }, "بحث", placeholder = "بالتشخيص أو الشكوى", singleLine = true, leadingIcon = Icons.Rounded.Search)
        }
        if (items.isEmpty()) {
            EmptyState(Icons.Rounded.History, "ما في تحليلات لسا", "التحليلات السريعة بتنحفظ هون. زيارات المرضى بتنحفظ بملفاتهن.")
        } else if (filtered.isEmpty()) {
            EmptyState(Icons.Rounded.Search, "ما في نتائج", "جرّب كلمة تانية.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 12.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { item -> HistoryRow(item, onOpen, onDelete) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("حذف كل السجل؟") },
            text = { Text("رح تنحذف كل التحليلات السريعة المحفوظة. ملفات المرضى ما بتتأثر.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("حذف", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } },
            containerColor = c.surface,
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onOpen: (HistoryItem) -> Unit, onDelete: (Long) -> Unit) {
    val c = Hk.c
    val urgencyColor = when (item.urgency) {
        Urgency.EMERGENCY -> c.danger
        Urgency.URGENT -> c.warning
        Urgency.ROUTINE -> c.success
        Urgency.UNKNOWN -> c.info
    }
    AppCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onOpen(item) }) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 7.dp)
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
                    if (item.mode == Mode.MULTIPLE) Pill("احتمالات", c.warning, Modifier.padding(start = 8.dp))
                }
                Text(item.caseText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Text(friendlyDate(item.time), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            }
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = c.textFaint)
            }
        }
    }
}
