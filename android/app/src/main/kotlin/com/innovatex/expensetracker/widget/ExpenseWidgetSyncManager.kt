package com.innovatex.expensetracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseWidgetSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifyDataChanged(appContext: Context = context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                // Update Small Widget
                SmallExpenseGlanceWidget().updateAll(appContext)
                // Update Medium Widget
                MediumExpenseGlanceWidget().updateAll(appContext)
                // Update Large Widget
                LargeExpenseGlanceWidget().updateAll(appContext)
            }
        }
    }
}
