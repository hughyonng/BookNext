-keep class com.booknext.app.data.remote.dto.** { *; }
-keep class com.booknext.app.data.local.db.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Apache POI
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.xmlbeans.

# Readium
-keep class org.readium.** { *; }
-dontwarn org.readium.**

# DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Gson
-keepattributes EnclosingMethod
-keep class sun.misc.Unsafe { *; }

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Throws**
