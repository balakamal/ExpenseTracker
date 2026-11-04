# R8 Shrinking & Proguard Rules for Spendwell Expense Tracker

# Preserve line numbers and source files for readable stack traces in crashes
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database Entities, DAOs, Converters, and RoomDatabase Implementations
-keep class com.innovatex.spendwell.database.entity.** { *; }
-keepclassmembers class com.innovatex.spendwell.database.entity.** { *; }
-keep class com.innovatex.spendwell.database.dao.** { *; }
-keepclassmembers class com.innovatex.spendwell.database.dao.** { *; }
-keep class com.innovatex.spendwell.database.converter.** { *; }
-keepclassmembers class com.innovatex.spendwell.database.converter.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.innovatex.spendwell.database.SpendwellDatabase { *; }
-keep class com.innovatex.spendwell.database.SpendwellDatabase_Impl { *; }
-dontwarn androidx.room.paging.**

# SQLCipher Native and Java JNI Bindings (net.zetetic and net.sqlcipher)
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
-keep,includedescriptorclasses class net.zetetic.** { *; }
-keep,includedescriptorclasses interface net.zetetic.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.database.**
-dontwarn net.zetetic.**

# Domain Models, Repositories and Money Value Object
-keep class com.innovatex.spendwell.domain.model.** { *; }
-keep class com.innovatex.spendwell.domain.repository.** { *; }
-keep class com.innovatex.spendwell.data.** { *; }
-keepclassmembers class com.innovatex.spendwell.data.** { *; }
-keep class com.innovatex.spendwell.common.Money { *; }
-keep class com.innovatex.spendwell.analytics.** { *; }
-keep class com.innovatex.spendwell.transactionengine.model.** { *; }

# Security and Keystore
-keep class com.innovatex.spendwell.security.** { *; }
-keepclassmembers class com.innovatex.spendwell.security.** { *; }

# Hilt & Dagger Dependency Injection
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep class com.innovatex.spendwell.di.** { *; }
-keepclassmembers class com.innovatex.spendwell.di.** { *; }
-keep class com.innovatex.spendwell.**_HiltModules* { *; }
-keepclassmembers class com.innovatex.spendwell.**_HiltModules* { *; }

# ViewModels, UI State and Navigation
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepclassmembers @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.innovatex.spendwell.features.**ViewModel* { *; }
-keepclassmembers class com.innovatex.spendwell.features.**ViewModel* { *; }
-keep class com.innovatex.spendwell.features.**UiState* { *; }
-keepclassmembers class com.innovatex.spendwell.features.**UiState* { *; }
-keep class com.innovatex.spendwell.features.**State* { *; }
-keepclassmembers class com.innovatex.spendwell.features.**State* { *; }
-keep class com.innovatex.spendwell.navigation.** { *; }
-keepclassmembers class com.innovatex.spendwell.navigation.** { *; }

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
