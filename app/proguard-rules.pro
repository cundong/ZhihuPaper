# Project specific ProGuard rules. These are appended to the AGP default
# config selected in app/build.gradle (getDefaultProguardFile('proguard-android.txt')).

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-dontwarn uk.co.senab.photoview.**
-keep class uk.co.senab.photoview.** { *;}

-dontwarn com.cundong.izhihu.entity.**
-keep class com.cundong.izhihu.entity.** { *;}

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson. Keeping
# only these reflective model fields lets R8 shrink Gson itself normally.
-keep class com.cundong.izhihu.entity.** { *;}

##---------------End: proguard configuration for Gson  ----------

# NOTE: the original proguard-project.txt had `-libraryjars libs/<jar>` lines for
# gson/jsoup and the image loader. Removed here: under Gradle the AGP
# ProGuard plugin injects the full application+library classpath automatically,
# so -libraryjars would only trigger spurious "can't find referenced class" errors.

# Jsoup loads entities-full.properties relative to its own package at runtime.
# Keep package/class names (not members) so R8 can still shrink and optimize
# code while that relative resource lookup remains valid.
-keepnames class org.jsoup.**

# Glide (replaced universal-image-loader). Glide ships its own consumer rules in
# the AAR; only the generated AppGlideModule entry point needs pinning here.
-keep public class com.cundong.izhihu.ZhihuGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# OkHttp (replaced Apache HttpClient). It ships consumer ProGuard rules in the
# AAR, so no -keep is needed here. These two -dontwarn cover optional compile-only
# references that are absent on Android:
#   - Animal Sniffer / Kotlin metadata annotations left in okio + okhttp
#   - Conscrypt/BouncyCastle/OpenJSSE TLS providers that OkHttp probes reflectively
#     and gracefully skips when missing.
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
