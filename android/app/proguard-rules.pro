# R8 Shrinking & Proguard Rules for Apex Expense Tracker

# Room Database Entities and Converters
-keep class com.innovatex.expensetracker.data.local.entity.** { *; }
-keep class com.innovatex.expensetracker.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt & Dagger
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }

# AndroidX Glance Widgets & Receivers
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.innovatex.expensetracker.widget.** { *; }

# Android BroadcastReceivers and NotificationListenerService
-keep class com.innovatex.expensetracker.ingestion.sms.SmsBroadcastReceiver { *; }
-keep class com.innovatex.expensetracker.ingestion.notification.EmailNotificationListenerService { *; }

# LiteRT-LM native model runtime
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# Protocol Buffers used by native runtime metadata
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class com.google.protobuf.** { *; }

# Google Common Flogger (runtime logging)
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
-keepclassmembers class com.google.common.flogger.** { *; }

# WorkManager & Hilt Worker
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.hilt.work.HiltWorkerFactory

# JNI and Native Method Preservation (required by LiteRT-LM)
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembernames class * {
    native <methods>;
}
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

# Suppress optional native/runtime warnings
-dontwarn org.tensorflow.**
-dontwarn com.google.crypto.tink.**
-dontwarn sun.misc.Unsafe

# Shaded AutoValue, JavaPoet, and Java compiler model classes used by Google runtime dependencies
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
-dontwarn javax.annotation.**
-dontwarn autovalue.shaded.com.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**
