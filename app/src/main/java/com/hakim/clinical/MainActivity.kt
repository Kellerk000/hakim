package com.hakim.clinical

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hakim.clinical.ui.AuthBridge
import com.hakim.clinical.ui.HakimRoot
import com.hakim.clinical.ui.MainViewModel
import com.hakim.clinical.ui.theme.DrAliTheme
import com.hakim.clinical.ui.theme.resolveDark

class MainActivity : FragmentActivity(), AuthBridge {

    private val vm: MainViewModel by viewModels()

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val dark = resolveDark(state.settings.themeMode, isSystemInDarkTheme())
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            DrAliTheme(themeMode = state.settings.themeMode) {
                HakimRoot(vm, this@MainActivity)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.onBackground()
    }

    override fun onStart() {
        super.onStart()
        vm.onForeground()
    }

    override fun available(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    override fun authenticate(onSuccess: () -> Unit) {
        if (!available()) {
            // ما في قفل شاشة بالجهاز: منفتح مباشرة حتى ما ينقفل الطبيب برّا ملفاته
            onSuccess()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("DrAli")
            .setSubtitle("افتح ملفات المرضى")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}
