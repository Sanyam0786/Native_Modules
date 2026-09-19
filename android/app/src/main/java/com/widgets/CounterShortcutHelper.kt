package com.widgets

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build

object CounterShortcutHelper {

    const val ACTION_SHORTCUT_INCREMENT = "com.widgets.ACTION_SHORTCUT_INCREMENT"
    const val ACTION_SHORTCUT_DECREMENT = "com.widgets.ACTION_SHORTCUT_DECREMENT"
    const val ACTION_SHORTCUT_RESET = "com.widgets.ACTION_SHORTCUT_RESET"

    private const val ID_INCREMENT = "shortcut_increment"
    private const val ID_DECREMENT = "shortcut_decrement"
    private const val ID_RESET = "shortcut_reset"

    fun updateDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val prefs = context.getSharedPreferences(CounterWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)

            fun createIntent(action: String): Intent {
                return Intent(context, ShortcutActionActivity::class.java).apply {
                    this.action = action
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
            }

            val incrementShortcut = ShortcutInfo.Builder(context, ID_INCREMENT)
                .setShortLabel("+1 Increment")
                .setLongLabel("Quick Increment (+1) • Current: $currentCount")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_plus))
                .setIntent(createIntent(ACTION_SHORTCUT_INCREMENT))
                .setRank(1)
                .build()

            val decrementShortcut = ShortcutInfo.Builder(context, ID_DECREMENT)
                .setShortLabel("−1 Decrement")
                .setLongLabel("Quick Decrement (−1) • Current: $currentCount")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_minus))
                .setIntent(createIntent(ACTION_SHORTCUT_DECREMENT))
                .setRank(2)
                .build()

            val resetShortcut = ShortcutInfo.Builder(context, ID_RESET)
                .setShortLabel("Reset Counter")
                .setLongLabel("Reset Counter to 0")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_reset))
                .setIntent(createIntent(ACTION_SHORTCUT_RESET))
                .setRank(3)
                .build()

            shortcutManager.dynamicShortcuts = listOf(incrementShortcut, decrementShortcut, resetShortcut)
        } catch (e: Exception) {
            // Ignore exception if dynamic shortcuts are disabled or restricted
        }
    }

    fun handleShortcutIntent(context: Context, intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        val prefs = context.getSharedPreferences(CounterWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        return when (action) {
            ACTION_SHORTCUT_INCREMENT -> {
                val currentCount = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
                editor.putInt(CounterWidgetProvider.KEY_COUNT, currentCount + 1).apply()
                CounterWidgetProvider.updateAllWidgets(context)
                true
            }
            ACTION_SHORTCUT_DECREMENT -> {
                val currentCount = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
                editor.putInt(CounterWidgetProvider.KEY_COUNT, maxOf(0, currentCount - 1)).apply()
                CounterWidgetProvider.updateAllWidgets(context)
                true
            }
            ACTION_SHORTCUT_RESET -> {
                editor.putInt(CounterWidgetProvider.KEY_COUNT, 0).apply()
                CounterWidgetProvider.updateAllWidgets(context)
                true
            }
            else -> false
        }
    }
}
