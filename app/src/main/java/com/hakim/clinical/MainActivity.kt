package com.hakim.clinical

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hakim.clinical.ui.HakimRoot
import com.hakim.clinical.ui.MainViewModel
import com.hakim.clinical.ui.theme.HakimTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HakimTheme {
                HakimRoot(vm)
            }
        }
    }
}
