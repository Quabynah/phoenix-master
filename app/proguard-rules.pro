# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontobfuscate #STOPSHIP remove this
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn io.sentry.**
-dontwarn org.slf4j.**
-dontwarn com.google.common.util.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.fasterxml.jackson.databind.ext.**
-dontwarn com.pubnub.api.**
-dontwarn javax.annotation.**
-keep class io.pergasus.data.**  { *; }
-keep class io.pergasus.api.**  { *; }
-keep class android.support.v7.widget.LinearLayoutManager {
    public protected *;
}
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep class in.uncod.android.bypass.** { *; }
-keepattributes *Annotation*,Signature,Exceptions

-keepclasseswithmembers class * {
    @retrofit.http.* <methods>;
}

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepattributes EnclosingMethod
-keepattributes InnerClasses
