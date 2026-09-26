# ProGuard / R8 rules for Discipline Alarm

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep TensorFlow Lite and Support
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Domain & Data Models
-keep class com.discipline.alarm.domain.model.** { *; }
-keep class com.discipline.alarm.data.local.entity.** { *; }

# Keep Dagger Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-dontwarn com.google.errorprone.annotations.**
