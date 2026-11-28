-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.cartvr.app.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.cartvr.app.data.remote.dto.** { *** Companion; }
-keepclassmembers class com.cartvr.app.data.updater.** { *; }

# Ktor / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**

# CameraX & ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
