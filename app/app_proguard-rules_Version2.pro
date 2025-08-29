# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keep class com.example.foundryvttcompanion.** { *; }
-dontwarn kotlinx.serialization.**
-dontwarn org.intellij.lang.annotations.**

# Keep JS bridge methods
-keepclassmembers class com.example.foundryvttcompanion.ui.screens.FoundryJsBridge {
    <methods>;
}