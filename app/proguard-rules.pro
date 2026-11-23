# ----------------------------------------------------------------------
# GLOBAL ATTRIBUTES
# ----------------------------------------------------------------------
# Prevent R8 from stripping generic signatures (List<T>) and annotations
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# ----------------------------------------------------------------------
# DATA MODELS (Precision Rules)
# ----------------------------------------------------------------------
-keep @kotlinx.serialization.Serializable class com.example.loom.data.models.** { *; }
-keep @androidx.room.Entity class com.example.loom.data.local.entity.** { *; }
# Gson reflects on field names for backups: keep every entity/helper (e.g. PresetExercise) and the backup DTOs
-keep class com.example.loom.data.local.entity.** { *; }
-keep class com.example.loom.data.models.** { *; }
-keep class com.example.loom.ui.home.LoomBackup { *; }
-keep class com.example.loom.ui.home.SpaceBackupDto { *; }

# ----------------------------------------------------------------------
# GSON
# ----------------------------------------------------------------------
# Suppress missing Unsafe class warning for Gson
-dontwarn sun.misc.Unsafe

# Keep all Gson TypeToken subclasses so JSON list parsing doesn't crash
-keep class * extends com.google.gson.reflect.TypeToken

# ----------------------------------------------------------------------
# KOTLINX SERIALIZATION
# ----------------------------------------------------------------------
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# ----------------------------------------------------------------------
# UTILS & MANAGERS
# ----------------------------------------------------------------------
-keep class com.example.loom.util.TemplateManager$** { *; }