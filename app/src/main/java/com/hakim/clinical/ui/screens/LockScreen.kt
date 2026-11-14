package com.hakim.clinical.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.ui.components.BrandText
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.PulseOrb

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PulseOrb(Icons.Rounded.Lock, size = 130.dp)
        Gap(12.dp)
        BrandText("DrAli", fontSize = 38.sp, textAlign = TextAlign.Center)
        Text(
            "ملفات المرضى محمية. افتح بالبصمة أو رمز الجهاز.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Gap(28.dp)
        PrimaryButton("افتح", onClick = onUnlock, icon = Icons.Rounded.Fingerprint, modifier = Modifier.fillMaxWidth())
    }
}
