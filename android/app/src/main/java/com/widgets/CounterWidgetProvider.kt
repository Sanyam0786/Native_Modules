package com.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews

class CounterWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        when (intent.action) {
            ACTION_COUNTER_INCREMENT -> {
                val currentCount = prefs.getInt(KEY_COUNT, 0)
                editor.putInt(KEY_COUNT, currentCount + 1)
                editor.apply()
                updateAllWidgets(context)
            }
            ACTION_COUNTER_DECREMENT -> {
                val currentCount = prefs.getInt(KEY_COUNT, 0)
                editor.putInt(KEY_COUNT, maxOf(0, currentCount - 1))
                editor.apply()
                updateAllWidgets(context)
            }
            ACTION_COUNTER_RESET -> {
                editor.putInt(KEY_COUNT, 0)
                editor.apply()
                updateAllWidgets(context)
            }
            else -> {
                super.onReceive(context, intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        const val ACTION_COUNTER_INCREMENT = "com.widgets.ACTION_COUNTER_INCREMENT"
        const val ACTION_COUNTER_DECREMENT = "com.widgets.ACTION_COUNTER_DECREMENT"
        const val ACTION_COUNTER_RESET = "com.widgets.ACTION_COUNTER_RESET"

        const val PREFS_NAME = "counter_widget_prefs"
        const val KEY_COUNT = "count"

        private fun createConfiguredRemoteViews(
            context: Context,
            layoutId: Int,
            count: Int
        ): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)
            views.setTextViewText(R.id.widget_count_text, count.toString())

            fun makeBroadcast(action: String, reqCode: Int): PendingIntent {
                val intent = Intent(context, CounterWidgetProvider::class.java).apply {
                    this.action = action
                    this.setPackage(context.packageName)
                }
                return PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            views.setOnClickPendingIntent(
                R.id.widget_btn_decrement,
                makeBroadcast(ACTION_COUNTER_DECREMENT, 1001)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_increment,
                makeBroadcast(ACTION_COUNTER_INCREMENT, 1002)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_reset,
                makeBroadcast(ACTION_COUNTER_RESET, 1003)
            )

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_counter_title, openPendingIntent)

            return views
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_COUNT, 0)
            val remoteViews = createConfiguredRemoteViews(context, R.layout.counter_widget_layout, count)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        fun updateAllWidgets(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_COUNT, 0)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CounterWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            CounterPlusTileService.requestAllTilesUpdate(context)
            CounterBridgeModule.notifyCountChanged(count)
            CounterShortcutHelper.updateDynamicShortcuts(context)
        }
    }
}


