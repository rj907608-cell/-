package com.pharmacy.app.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * ملف إعدادات وتهيئة الاتصال بـ Supabase
 * يتم قراءة القيم بأمان إما من إعدادات المستخدم المحفوظة محلياً (SharedPreferences)
 * أو من متغيرات البيئة عبر BuildConfig دون أي كتابة مباشرة لمفاتيح الاتصال داخل الكود
 */
object SupabaseConfig {

    private const val PREFS_NAME = "supabase_runtime_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_KEY = "custom_supabase_key"

    @Volatile
    private var cachedUrl: String? = null
    @Volatile
    private var cachedKey: String? = null
    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            val appCtx = context.applicationContext
            prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            cachedUrl = prefs?.getString(KEY_CUSTOM_URL, null)
            cachedKey = prefs?.getString(KEY_CUSTOM_KEY, null)
        }
    }

    val SUPABASE_URL: String
        get() {
            val custom = cachedUrl ?: prefs?.getString(KEY_CUSTOM_URL, null)
            if (!custom.isNullOrBlank() && !custom.contains("your-project")) {
                return custom.trim().trimEnd('/')
            }
            return try {
                BuildConfig.SUPABASE_URL.trim().trimEnd('/')
            } catch (e: Throwable) {
                ""
            }
        }

    val SUPABASE_KEY: String
        get() {
            val custom = cachedKey ?: prefs?.getString(KEY_CUSTOM_KEY, null)
            if (!custom.isNullOrBlank() && !custom.contains("your-publishable-key")) {
                return custom.trim()
            }
            return try {
                BuildConfig.SUPABASE_KEY.trim()
            } catch (e: Throwable) {
                ""
            }
        }

    val isConfigured: Boolean
        get() {
            val url = SUPABASE_URL
            val key = SUPABASE_KEY
            return url.isNotBlank() &&
                    key.isNotBlank() &&
                    !url.contains("your-project") &&
                    !key.contains("your-publishable-key")
        }

    fun saveCustomConfig(context: Context, url: String, key: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = key.trim()
        cachedUrl = cleanUrl
        cachedKey = cleanKey
        val p = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        p.edit()
            .putString(KEY_CUSTOM_URL, cleanUrl)
            .putString(KEY_CUSTOM_KEY, cleanKey)
            .apply()
    }
}

