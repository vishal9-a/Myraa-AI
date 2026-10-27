# Proguard rules for Myraa
 
-keep class org.json.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }

# Keep model classes and enums
-keep class com.myraa.assistant.data.** { *; }
-keep class com.myraa.assistant.voice.** { *; }
-keep class com.myraa.assistant.forge.** { *; }

# Keep OkHttp internals used for WebSockets
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Three.js bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
