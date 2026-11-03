# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Room DB rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.migration.Migration
-dontwarn androidx.room.paging.**

# Koin rules
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Kotlinx Serialization rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Ktor & Supabase rules
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Strip debug and verbose logs in release build
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}


# ObjectBox meta classes (Entity_) are looked up by name in SensorStore/WatchSensorStore
# via Class.forName("${entity}_") and getField("timestamp"), so keep their names and fields.
-keep class kaist.iclab.**_ { public static <fields>; }

# Couchbase Lite native code (libLiteCoreJNI) looks up its Java classes, fields and methods
# by name in JNI_OnLoad, so they must not be renamed or removed.
-keep class com.couchbase.lite.** { *; }
-dontwarn com.couchbase.lite.**

# Sensor ids/names (and their Couchbase collection names) come from the class simpleName in
# BaseSensor, so sensor classes must keep their names or campaign matching and uploads break.
-keepnames class * extends kaist.iclab.tracker.sensor.core.BaseSensor
