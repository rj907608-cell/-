package com.pharmacy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.MyApplicationTheme
import com.pharmacy.app.ui.MainPharmacyScreen
import com.pharmacy.app.ui.MainViewModel
import com.pharmacy.app.ui.PharmacySplashScreen

/**
 * نقطة الدخول الرئيسية لتطبيق إدارة الصيدلية
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // دعم واجهة المستخدم من اليمين إلى اليسار (RTL) لراحة الصيادلة والمستخدمين
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // تحميل واجهة الصيدلية بالكامل في الخلفية لضمان أعلى سرعة وسلاسة
                            MainPharmacyScreen(viewModel = viewModel)

                            // شاشة الترحيب تظهر وتتلاشى بنعومة دون أي تأخير
                            AnimatedVisibility(
                                visible = showSplash,
                                enter = fadeIn(),
                                exit = fadeOut(animationSpec = tween(durationMillis = 350))
                            ) {
                                PharmacySplashScreen(
                                    onSplashFinished = { showSplash = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
