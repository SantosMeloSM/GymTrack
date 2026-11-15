# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.gymtrack.app.**$$serializer { *; }
-keepclassmembers class com.gymtrack.app.** { *** Companion; }
-keepclasseswithmembers class com.gymtrack.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep readable stack traces (the app is shrunk, not obfuscated)
-dontobfuscate
