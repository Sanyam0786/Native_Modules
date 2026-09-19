package com.widgets

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class CounterMinusTileService : TileService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(CounterPlusTileService.PREFS_NAME, Context.MODE_PRIVATE)
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
        if (key == CounterPlusTileService.KEY_COUNT) {
            updateTileState()
        }
    }

    override fun onClick() {
        super.onClick()
        val currentCount = prefs.getInt(CounterPlusTileService.KEY_COUNT, 0)
        val newCount = maxOf(0, currentCount - 1)

        prefs.edit().putInt(CounterPlusTileService.KEY_COUNT, newCount).apply()

        updateTileState()
        CounterWidgetProvider.updateAllWidgets(this)
        CounterPlusTileService.requestAllTilesUpdate(this)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val count = prefs.getInt(CounterPlusTileService.KEY_COUNT, 0)

        tile.state = if (count > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = "Counter −1"
            tile.subtitle = "Count: $count"
        } else {
            tile.label = "Count: $count (−1)"
        }
        tile.updateTile()
    }
}
