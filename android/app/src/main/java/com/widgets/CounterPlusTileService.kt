package com.widgets

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class CounterPlusTileService : TileService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        prefs.registerOnSharedPreferenceChangeListener(this)
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == KEY_COUNT) {
            updateTileState()
        }
    }

    override fun onClick() {
        super.onClick()
        val currentCount = prefs.getInt(KEY_COUNT, 0)
        val newCount = currentCount + 1

        prefs.edit().putInt(KEY_COUNT, newCount).apply()

        updateTileState()
        CounterWidgetProvider.updateAllWidgets(this)
        requestAllTilesUpdate(this)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val count = prefs.getInt(KEY_COUNT, 0)

        tile.state = Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = "Counter +1"
            tile.subtitle = "Count: $count"
        } else {
            tile.label = "Count: $count (+1)"
        }
        tile.updateTile()
    }

    companion object {
        const val PREFS_NAME = "counter_widget_prefs"
        const val KEY_COUNT = "count"

        fun requestAllTilesUpdate(context: Context) {
            try {
                val appContext = context.applicationContext ?: context
                requestListeningState(
                    appContext,
                    ComponentName(appContext, CounterPlusTileService::class.java)
                )
                requestListeningState(
                    appContext,
                    ComponentName(appContext, CounterMinusTileService::class.java)
                )
            } catch (e: Exception) {
                // Catch any exception on devices where requestListeningState is unsupported
            }
        }
    }
}
