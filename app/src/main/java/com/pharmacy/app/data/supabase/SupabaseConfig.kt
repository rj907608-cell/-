package com.pharmacy.app.data.supabase

import com.example.BuildConfig

/**
 * ملف إعدادات وتهيئة الاتصال بـ Supabase
 * يتم قراءة القيم بأمان من متغيرات البيئة عبر BuildConfig
 * دون أي كتابة مباشرة لمفاتيح الاتصال داخل الكود البرمجي
 */
object SupabaseConfig {
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    val SUPABASE_KEY: String = BuildConfig.SUPABASE_KEY.trim()

    val isConfigured: Boolean
        get() = SUPABASE_URL.isNotBlank() &&
                SUPABASE_KEY.isNotBlank() &&
                !SUPABASE_URL.contains("your-project")
}
