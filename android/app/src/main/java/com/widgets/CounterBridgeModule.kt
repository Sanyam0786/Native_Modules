package com.widgets

import android.content.Context
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class CounterBridgeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "CounterBridge"
    }

    @ReactMethod
    fun setCount(count: Int, promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(
                CounterWidgetProvider.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            prefs.edit().putInt(CounterWidgetProvider.KEY_COUNT, count).apply()
            CounterWidgetProvider.updateAllWidgets(reactContext)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SET_COUNT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getCount(promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(
                CounterWidgetProvider.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val count = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
            promise.resolve(count)
        } catch (e: Exception) {
            promise.reject("GET_COUNT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun pinWidget(promise: Promise) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val appWidgetManager = reactContext.getSystemService(android.appwidget.AppWidgetManager::class.java)
                val myProvider = android.content.ComponentName(reactContext, CounterWidgetProvider::class.java)
                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                    val pinned = appWidgetManager.requestPinAppWidget(myProvider, null, null)
                    promise.resolve(pinned)
                    return
                }
            }
            promise.resolve(false)
        } catch (e: Exception) {
            promise.reject("PIN_WIDGET_ERROR", e.message, e)
        }
    }
}
