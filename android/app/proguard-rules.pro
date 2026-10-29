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

# ONNX Runtime Android (Local AI)
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# MediaPipe Core & Tasks GenAI (On-Device LLM Inference)
-keep class com.google.mediapipe.** { *; }
-keep class * implements com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.mediapipe.tasks.genai.**
-keepclassmembers class com.google.mediapipe.**$$ExternalSyntheticLambda* { *; }
-keepclassmembers,allowoptimization class com.google.mediapipe.** { *; }

# Protocol Buffers (used by MediaPipe & GenAI serialization)
-keep class com.google.mediapipe.proto.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class com.google.protobuf.** { *; }

# Google Common Flogger (internal logging in MediaPipe)
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
-keepclassmembers class com.google.common.flogger.** { *; }

# WorkManager & Hilt Worker
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.hilt.work.HiltWorkerFactory

# JNI and Native Method Preservation (Critical for MediaPipe GenAI and ONNX Runtime)
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembernames class * {
    native <methods>;
}
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

# Suppress optional library warnings that MediaPipe or ONNX transitively touches
-dontwarn org.tensorflow.**
-dontwarn com.google.crypto.tink.**
-dontwarn sun.misc.Unsafe

# Shaded AutoValue, JavaPoet, and Java compiler model classes (used by Google/MediaPipe dependencies)
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
-dontwarn javax.annotation.**
-dontwarn autovalue.shaded.com.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**
