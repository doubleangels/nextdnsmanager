# Keep all classes and methods in your package
-keep class com.doubleangels.nextdnsmanagement.** { *; }
-keepattributes *Annotation*
-dontwarn com.doubleangels.nextdnsmanagement.**

# General ProGuard rules
-dontwarn javax.annotation.**

# AndroidX libraries
-keep class androidx.** { *; }
-dontwarn androidx.**

# AndroidX App Startup (InitializationProvider runs before Application.onCreate)
-keep class androidx.startup.** { *; }
-keep class * extends androidx.startup.Initializer {
    <init>();
}

# Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Webkit
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# Appcompat and Preferences
-keep class androidx.appcompat.** { *; }
-keep class androidx.preference.** { *; }
-dontwarn androidx.appcompat.**
-dontwarn androidx.preference.**

# Retrofit (Gson Converter)
-keep class com.squareup.retrofit2.** { *; }
-dontwarn com.squareup.retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# CircleImageView
-keep class de.hdodenhof.circleimageview.** { *; }
-dontwarn de.hdodenhof.circleimageview.**

# Sentry Android
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
-keepattributes *Annotation*

# LeakCanary (debug only, no need to obfuscate for debug builds)
-dontwarn com.squareup.leakcanary.**
-keep class com.squareup.leakcanary.** { *; }

# General Gson rules
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}
