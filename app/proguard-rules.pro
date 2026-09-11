# Add project specific ProGuard rules here.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.pharmacy.app.data.** { *; }

# ZXing barcode scanner
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Compose Runtime
-keep class androidx.compose.runtime.** { *; }

