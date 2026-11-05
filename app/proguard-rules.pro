# GameNote ProGuard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
-keep class cl.topingames.gamenote.data.local.entities.** { *; }
