# NMDLock 1.0 ProGuard Rules
# Keep the application class and main activity
-keep class com.nmdlock.app.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.nmdlock.app.data.remote.api.** { *; }

# Keep Gson serialization models
-keep class com.nmdlock.app.data.remote.dto.** { *; }
-keepclassmembers class com.nmdlock.app.data.remote.dto.** { *; }

# Keep DataStore
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Compose
-keep class androidx.compose.** { *; }

# General rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
