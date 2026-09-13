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
import com.pharmacy.app.data.supabase.SupabaseAuthManager
import com.pharmacy.app.ui.MainPharmacyScreen
import com.pharmacy.app.ui.MainViewModel
import com.pharmacy.app.ui.PharmacySplashScreen
import com.pharmacy.app.ui.auth.PharmacyAuthScreen

/**
 * نقطة الدخول الرئيسية لتطبيق إدارة الصيدلية
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = SupabaseAuthManager.getInstance(applicationContext)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val isLoggedIn by authManager.isLoggedIn.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // دعم واجهة المستخدم من اليمين إلى اليسار (RTL) لراحة الصيادلة والمستخدمين
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isLoggedIn) {
                                // الواجهة الرئيسية للصيدلية تعمل بكامل وظائفها السابقة دون أي مساس
                                MainPharmacyScreen(viewModel = viewModel)
                            } else {
                                // شاشة تسجيل الدخول / إنشاء الحساب تظهر فقط عند أول تشغيل قبل تسجيل الدخول
                                PharmacyAuthScreen(
                                    authManager = authManager,
                                    onAuthSuccess = {
                                        // عند نجاح الدخول يتم الانتقال تلقائياً للواجهة الرئيسية وتخزين الجلسة محلياً
                                        // وسحب بيانات الصيدلية تلقائياً من السحابة إذا كان جهازاً جديداً
                                        viewModel.pullCloudDataNow()
                                    }
                                )
                            }

                            // شاشة الترحيب تظهر وتتلاشى بنعومة عند فتح التطبيق
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
