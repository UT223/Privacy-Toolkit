# Keep Room entities and DAOs
-keep class com.privacytoolkit.data.database.** { *; }

# Keep ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# Keep ViewBinding
-keep class com.privacytoolkit.databinding.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
