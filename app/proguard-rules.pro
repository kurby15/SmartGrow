# Project-specific ProGuard rules for Smart Grow Release on APKPure

# Optimize and keep line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Firebase ProGuard Rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# OkHttp ProGuard Rules
-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A Class is referenced but not present on classpath
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Glide ProGuard Rules
-keep public class * extends com.github.bumptech.glide.module.AppGlideModule {
    public <init>();
}
-keep public class * extends com.github.bumptech.glide.module.LibraryGlideModule {
    public <init>();
}
-keep class com.github.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.github.bumptech.glide.integration.okhttp3.OkHttpGlideModule { *; }
-dontwarn com.github.bumptech.glide.**

# Kotlin Coroutines ProGuard Rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Mapbox ProGuard Rules
-keep class com.mapbox.** { *; }
-dontwarn com.mapbox.**

# osmdroid ProGuard Rules
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# JavaMail (com.sun.mail) ProGuard Rules
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn java.awt.**
-dontwarn javax.activation.**

# Keep model/data classes from being obfuscated (essential for Firestore / Realtime DB deserialization)
-keep class com.example.smartgrow.history.** { *; }
-keep class com.example.smartgrow.plants.** { *; }
-keep class com.example.smartgrow.auth.** { *; }
-keep class com.example.smartgrow.core.** { *; }
