package com.pharmacy.app.data.supabase

import org.json.JSONObject

/**
 * بيانات جلسة المستخدم بعد تسجيل الدخول أو إنشاء الحساب
 */
data class SupabaseUserSession(
    val userId: String,
    val email: String,
    val name: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("userId", userId)
            put("email", email)
            put("name", name)
            put("accessToken", accessToken)
            put("refreshToken", refreshToken)
            put("expiresAt", expiresAt)
        }.toString()
    }

    val isExpired: Boolean
        get() = expiresAt > 0 && System.currentTimeMillis() >= (expiresAt * 1000L)

    companion object {
        fun fromJson(jsonStr: String): SupabaseUserSession? {
            return try {
                val obj = JSONObject(jsonStr)
                SupabaseUserSession(
                    userId = obj.optString("userId", ""),
                    email = obj.optString("email", ""),
                    name = obj.optString("name", ""),
                    accessToken = obj.optString("accessToken", ""),
                    refreshToken = obj.optString("refreshToken", ""),
                    expiresAt = obj.optLong("expiresAt", 0L)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * نتيجة عمليات المصادقة
 */
sealed class AuthResult {
    data class Success(val session: SupabaseUserSession) : AuthResult()
    data class RequiresEmailVerification(val email: String, val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
