package com.pharmacy.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmacy.app.data.supabase.AuthResult
import com.pharmacy.app.data.supabase.PendingAccountData
import com.pharmacy.app.data.supabase.SupabaseAuthManager
import com.pharmacy.app.data.sync.NetworkConnectivityMonitor
import kotlinx.coroutines.launch

private const val SUPPORT_PHONE_NUMBER = "0931650248"
private val WhatsAppGreen = Color(0xFF25D366)
private val AlertRed = Color(0xFFD32F2F)

/**
 * شاشة المصادقة وإدارة الحسابات
 * مصممة لتكون جذابة، عصرية وسهلة للصيدلي:
 * 1. إنشاء حساب جديد مع تأكيد كلمة المرور
 * 2. تسجيل الدخول
 * 3. واجهة الانتظار عند نجاح إرسال البيانات مع زري اتصال وواتساب للدعم الفني
 * 4. إشعار أحمر بارز في حال عدم الاتصال بالإنترنت
 */
@Composable
fun PharmacyAuthScreen(
    authManager: SupabaseAuthManager,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val connectivityMonitor = remember { NetworkConnectivityMonitor(context) }
    val isOnline by connectivityMonitor.isOnline.collectAsState(initial = connectivityMonitor.isCurrentlyConnected())

    var pendingAccount by remember { mutableStateOf<PendingAccountData?>(authManager.getPendingAccount()) }

    var isSignUpMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    var logoTapCount by remember { mutableStateOf(0) }
    var showAdminConfigDialog by remember { mutableStateOf(false) }
    var adminUrl by remember { mutableStateOf(com.pharmacy.app.data.supabase.SupabaseConfig.SUPABASE_URL) }
    var adminKey by remember { mutableStateOf(com.pharmacy.app.data.supabase.SupabaseConfig.SUPABASE_KEY) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // إشعار بارز باللون الأحمر إذا كان الجهاز غير متصل بالإنترنت
            AnimatedVisibility(
                visible = !isOnline,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("offline_red_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "جهازك غير متصل بالإنترنت",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "يرجى الاتصال بشبكة الإنترنت لإتمام العمليات والمزامنة.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // إذا كان المستخدم قد أنشأ حسابه وهو بانتظار التفعيل
            if (pendingAccount != null) {
                WaitingActivationView(
                    pendingAccount = pendingAccount!!,
                    isOnline = isOnline,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    successNotice = successNotice,
                    onCheckStatus = {
                        if (!isOnline) {
                            errorMessage = "جهازك غير متصل بالإنترنت. يرجى الاتصال بالإنترنت أولاً."
                            return@WaitingActivationView
                        }
                        isLoading = true
                        errorMessage = null
                        successNotice = null
                        coroutineScope.launch {
                            val res = authManager.signIn(pendingAccount!!.email, pendingAccount!!.password)
                            isLoading = false
                            when (res) {
                                is AuthResult.Success -> {
                                    authManager.clearPendingAccount()
                                    onAuthSuccess()
                                }
                                is AuthResult.Error -> {
                                    errorMessage = res.message
                                }
                                is AuthResult.RequiresEmailVerification -> {
                                    errorMessage = "الحساب ما زال قيد التفعيل والمراجعة من قبل الإدارة."
                                }
                            }
                        }
                    },
                    onChangeAccount = {
                        authManager.clearPendingAccount()
                        pendingAccount = null
                        errorMessage = null
                        successNotice = null
                    }
                )
            } else {
                // شاشة تسجيل الدخول / إنشاء الحساب
                // الشعار والعنوان الترحيبي الجذاب
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .clickable {
                            logoTapCount++
                            if (logoTapCount >= 5) {
                                logoTapCount = 0
                                adminUrl = com.pharmacy.app.data.supabase.SupabaseConfig.SUPABASE_URL
                                adminKey = com.pharmacy.app.data.supabase.SupabaseConfig.SUPABASE_KEY
                                showAdminConfigDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = "شعار الصيدلية",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "نظام إدارة الصيدلية الذكي",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "المنظومة الاحترافية لإدارة المبيعات والمخزون الصيدلاني",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // التبديل بين إنشاء حساب وتسجيل الدخول
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = {
                                isSignUpMode = true
                                errorMessage = null
                                successNotice = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = if (isSignUpMode) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "إنشاء حساب جديد",
                                fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                isSignUpMode = false
                                errorMessage = null
                                successNotice = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = if (!isSignUpMode) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "تسجيل الدخول",
                                fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // بطاقة نموذج الإدخال
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) "بيانات تسجيل الصيدلية" else "تسجيل الدخول إلى حسابك",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // حقل الاسم في وضع إنشاء الحساب
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    errorMessage = null
                                },
                                label = { Text("اسم الصيدلية أو المسؤول") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_field")
                            )
                        }

                        // حقل البريد الإلكتروني
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("البريد الإلكتروني") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_field")
                        )

                        // حقل كلمة المرور
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("كلمة المرور") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPasswordVisible) "إخفاء" else "إظهار"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (!isSignUpMode) focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_field")
                        )

                        // حقل تأكيد كلمة المرور في وضع إنشاء الحساب
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    if (errorMessage != null && password == it) {
                                        errorMessage = null
                                    }
                                },
                                label = { Text("تأكيد كلمة المرور") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isConfirmPasswordVisible) "إخفاء" else "إظهار"
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_confirm_password_field")
                            )
                        }

                        // زر الإجراء الأساسي
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (!isOnline) {
                                    errorMessage = "جهازك غير متصل بالإنترنت. يرجى الاتصال بالإنترنت للمتابعة."
                                    return@Button
                                }
                                if (isSignUpMode && name.isBlank()) {
                                    errorMessage = "يرجى كتابة اسم الصيدلية أو المسؤول"
                                    return@Button
                                }
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = "يرجى إدخال بريد إلكتروني صحيح"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "كلمة المرور يجب ألا تقل عن 6 أحرف"
                                    return@Button
                                }
                                if (isSignUpMode) {
                                    if (confirmPassword.isBlank()) {
                                        errorMessage = "يرجى تأكيد كلمة المرور"
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "كلمة المرور وتأكيد كلمة المرور غير متطابقتين"
                                        return@Button
                                    }
                                }

                                isLoading = true
                                errorMessage = null
                                successNotice = null

                                coroutineScope.launch {
                                    if (isSignUpMode) {
                                        val res = authManager.signUp(email, password, name)
                                        isLoading = false
                                        when (res) {
                                            is AuthResult.Success -> {
                                                authManager.clearPendingAccount()
                                                onAuthSuccess()
                                            }
                                            is AuthResult.RequiresEmailVerification -> {
                                                authManager.savePendingAccount(name, email, password)
                                                pendingAccount = authManager.getPendingAccount()
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = res.message
                                            }
                                        }
                                    } else {
                                        // تسجيل الدخول
                                        val res = authManager.signIn(email, password)
                                        isLoading = false
                                        when (res) {
                                            is AuthResult.Success -> {
                                                authManager.clearPendingAccount()
                                                onAuthSuccess()
                                            }
                                            is AuthResult.Error -> {
                                                // إذا كان الخطأ يدل على عدم التفعيل، نعرض شاشة الانتظار
                                                if (res.message.contains("بانتظار التفعيل") || res.message.contains("لم يتم تأكيد")) {
                                                    authManager.savePendingAccount(name.ifBlank { "الصيدلية" }, email, password)
                                                    pendingAccount = authManager.getPendingAccount()
                                                } else {
                                                    errorMessage = res.message
                                                }
                                            }
                                            is AuthResult.RequiresEmailVerification -> {
                                                authManager.savePendingAccount(name.ifBlank { "الصيدلية" }, email, password)
                                                pendingAccount = authManager.getPendingAccount()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUpMode) "إنشاء الحساب ومتابعة" else "تسجيل الدخول",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // رسائل الخطأ والتنبيهات
                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (successNotice != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = successNotice ?: "",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showAdminConfigDialog) {
                AlertDialog(
                    onDismissRequest = { showAdminConfigDialog = false },
                    title = {
                        Text(
                            text = "إعدادات الربط السحابي (خاص بالمسؤول)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "أدخل بيانات مشروع Supabase لربط قاعدة البيانات وتسجيل المستخدمين مباشرة في لوحة التحكم:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = adminUrl,
                                onValueChange = { adminUrl = it },
                                label = { Text("Project URL") },
                                placeholder = { Text("https://xxx.supabase.co") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = adminKey,
                                onValueChange = { adminKey = it },
                                label = { Text("anon public key") },
                                placeholder = { Text("eyJhbGciOi...") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (adminUrl.isNotBlank() && adminKey.isNotBlank()) {
                                    com.pharmacy.app.data.supabase.SupabaseConfig.saveCustomConfig(
                                        context,
                                        adminUrl.trim(),
                                        adminKey.trim()
                                    )
                                    Toast.makeText(context, "تم حفظ وتفعيل الاتصال السحابي بنجاح", Toast.LENGTH_SHORT).show()
                                    showAdminConfigDialog = false
                                } else {
                                    Toast.makeText(context, "يرجى تعبئة الرابط والمفتاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("حفظ وتفعيل")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAdminConfigDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * واجهة الانتظار حتى يتم تفعيل الحساب
 * تحتوي على:
 * - نص الانتظار حتى يتم التفعيل من الإدارة
 * - زري الدعم الفني: هاتف (0931650248) وواتساب (0931650248)
 * - زر فحص حالة التفعيل
 */
@Composable
private fun WaitingActivationView(
    pendingAccount: PendingAccountData,
    isOnline: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    successNotice: String?,
    onCheckStatus: () -> Unit,
    onChangeAccount: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("waiting_activation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // أيقونة الساعة والانتظار الأنيقة
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                text = "نرجو الانتظار حتى يتم تفعيل حسابك",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "تم استلام بيانات تسجيلك بنجاح. حسابك الآن قيد المراجعة والاعتماد من قِبل إدارة النظام.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            // بطاقة تفاصيل الحساب المسجل
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pendingAccount.name.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "الاسم / الصيدلية: ",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pendingAccount.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "البريد الإلكتروني: ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pendingAccount.email,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // قسم الدعم الفني
            Text(
                text = "وإن واجهتك أي مشكلة تواصل مع الدعم:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // زري الدعم الفني: اتصال هاتفي + محادثة واتساب
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. زر الاتصال الهاتفي (يفتح شاشة الاتصال ويضع الرقم 0931650248)
                Button(
                    onClick = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$SUPPORT_PHONE_NUMBER")
                            }
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح لوحة الاتصال", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("call_support_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "اتصال بالدعم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 2. زر محادثة واتساب (يدخل إلى واتساب ويفتح محادثة مع الرقم 0931650248)
                Button(
                    onClick = {
                        try {
                            // الرقم الدولي لسوريا 963
                            val waNumber = if (SUPPORT_PHONE_NUMBER.startsWith("0")) {
                                "963" + SUPPORT_PHONE_NUMBER.substring(1)
                            } else {
                                SUPPORT_PHONE_NUMBER
                            }
                            val defaultMsg = "مرحباً، أرجو تفعيل حسابي في تطبيق الصيدلية.\nالبريد: ${pendingAccount.email}\nالاسم: ${pendingAccount.name}"
                            val waUrl = "https://wa.me/$waNumber?text=${Uri.encode(defaultMsg)}"
                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("whatsapp_support_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "واتساب",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "محادثة واتساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // زر فحص حالة التفعيل بعد قيام الإدارة بتأكيد الحساب
            OutlinedButton(
                onClick = onCheckStatus,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_activation_status_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فحص حالة التفعيل الآن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // رسائل الخطأ والتنبيهات
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (successNotice != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successNotice,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // خيار العودة لتسجيل الدخول بحساب آخر
            TextButton(
                onClick = onChangeAccount,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "تسجيل الدخول بحساب آخر أو تعديل البيانات",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
