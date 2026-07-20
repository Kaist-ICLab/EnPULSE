# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

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

# Strip debug and verbose logs in release build
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}