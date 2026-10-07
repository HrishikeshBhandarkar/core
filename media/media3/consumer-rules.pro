# Keep Media3 core, session, exoplayer, and UI
-keep class androidx.media3.common.** { *; }
-keep interface androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep interface androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep interface androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep interface androidx.media3.ui.** { *; }

# Keep private player field in ForwardingPlayer for DelegatingForwardingPlayer reflection
-keepclassmembers class androidx.media3.common.ForwardingPlayer {
    private androidx.media3.common.Player player;
    <fields>;
}

# Keep our media3 classes, callbacks, services, and interfaces
-keep class com.maxrave.media3.** { *; }
-keep interface com.maxrave.media3.** { *; }

# Keep MediaSession and MediaBrowser services and receivers
-keep public class * extends androidx.media3.session.MediaSessionService
-keep public class * extends androidx.media3.session.MediaLibraryService
-keep public class androidx.media3.session.MediaButtonReceiver { *; }

# Keep AndroidX Media and MediaCompat for IPC and Auto
-keep class android.support.v4.media.** { *; }
-keep interface android.support.v4.media.** { *; }
-keep class androidx.media.** { *; }
-keep interface androidx.media.** { *; }
