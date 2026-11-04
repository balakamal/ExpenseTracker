# R8 Shrinking & Proguard Rules for Spendwell Expense Tracker

# Preserve line numbers and source files for readable stack traces in crashes
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database Entities, DAOs, Converters, and RoomDatabase Implementations
-keep class com.innovatex.spendwell.database.entity.** { *; }
-keepclassmembers class com.innovatex.spendwell.database.entity.** { *; }
-keep class com.innovatex.spendwell.database.dao.** { *; }
-keep class com.innovatex.spendwell.database.converter.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.innovatex.spendwell.database.SpendwellDatabase { *; }
-keep class com.innovatex.spendwell.database.SpendwellDatabase_Impl { *; }
-dontwarn androidx.room.paging.**

# SQLCipher Native and Java JNI Bindings (net.zetetic and net.sqlcipher)
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** {
    <fields>;
    <methods>;
}
-keepclassmembers class net.zetetic.database.sqlcipher.** {
    <fields>;
    <methods>;
}
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.database.**
-dontwarn net.zetetic.**

# Domain Models and Money Value Object
-keep class com.innovatex.spendwell.domain.model.** { *; }
-keep class com.innovatex.spendwell.common.Money { *; }
-keep class com.innovatex.spendwell.analytics.** { *; }
-keep class com.innovatex.spendwell.transactionengine.model.** { *; }

# Security and Keystore
-keep class com.innovatex.spendwell.security.** { *; }

# Hilt & Dagger Dependency Injection
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class com.innovatex.spendwell.di.** { *; }

# ViewModels and UI state
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class com.innovatex.spendwell.features.dashboard.DashboardUiState { *; }

# AndroidX Glance Widgets & Receivers
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.innovatex.spendwell.integrations.widget.** { *; }

# Android Receivers and Services
-keep class com.innovatex.spendwell.integrations.sms.SmsBroadcastReceiver { *; }
-keep class com.innovatex.spendwell.integrations.notification.SpendwellNotificationListener { *; }

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# LiteRT-LM native model runtime (if present)
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# Protocol Buffers used by native runtime metadata
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class com.google.protobuf.** { *; }

# WorkManager & Hilt Worker
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.hilt.work.HiltWorkerFactory

# JNI and Native Method Preservation
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembernames class * {
    native <methods>;
}

# Suppress optional native/runtime warnings
-dontwarn org.tensorflow.**
-dontwarn com.google.crypto.tink.**
-dontwarn sun.misc.Unsafe
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
-dontwarn javax.annotation.**
-dontwarn autovalue.shaded.com.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**
