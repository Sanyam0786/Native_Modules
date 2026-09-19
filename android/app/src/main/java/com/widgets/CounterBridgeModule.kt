package com.widgets

import android.content.Context
import android.content.SharedPreferences
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule

@ReactModule(name = CounterBridgeModule.NAME)
class CounterBridgeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs: SharedPreferences by lazy {
        reactContext.getSharedPreferences(
            CounterWidgetProvider.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    override fun initialize() {
        super.initialize()
        instance = this
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun invalidate() {
        if (instance == this) {
            instance = null
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.invalidate()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == CounterWidgetProvider.KEY_COUNT) {
            val count = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
            sendCountEvent(count)
        }
    }

    fun sendCountEvent(count: Int) {
        try {
            if (reactContext.hasActiveReactInstance()) {
                val params = Arguments.createMap().apply {
                    putInt("count", count)
                }
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    ?.emit("onCountChanged", params)
            }
        } catch (e: Exception) {
            // Ignore if React context is not ready
        }
    }

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun setCount(count: Double, promise: Promise) {
        try {
            val countInt = count.toInt()
            prefs.edit().putInt(CounterWidgetProvider.KEY_COUNT, countInt).apply()
            CounterWidgetProvider.updateAllWidgets(reactContext)
            CounterPlusTileService.requestAllTilesUpdate(reactContext)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SET_COUNT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getCount(promise: Promise) {
        try {
            val count = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
            promise.resolve(count.toDouble())
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

    @ReactMethod
    fun addListener(eventName: String) {
        // Keep: Required for RN built-in Event Emitter Calls
    }

    @ReactMethod
    fun removeListeners(count: Double) {
        // Keep: Required for RN built-in Event Emitter Calls
    }

    companion object {
        const val NAME = "CounterBridge"
        private var instance: CounterBridgeModule? = null

        fun notifyCountChanged(count: Int) {
            instance?.sendCountEvent(count)
        }
    }
}
