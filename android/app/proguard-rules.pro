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
