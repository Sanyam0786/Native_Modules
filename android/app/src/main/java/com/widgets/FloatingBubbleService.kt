package com.widgets

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatingBubbleService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var collapsedView: FrameLayout
    private lateinit var expandedView: LinearLayout
    private lateinit var collapsedCountText: TextView
    private lateinit var expandedCountText: TextView

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(CounterWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundServiceNotification()
        prefs.registerOnSharedPreferenceChangeListener(this)
        createFloatingBubbleView()
        updateViewsWithCount()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            createFloatingBubbleView()
        }
        updateViewsWithCount()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_bubble_channel"
        val channelName = "Counter Floating Bubble"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Active floating bubble service"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Counter Floating Bubble")
            .setContentText("Floating bubble active on screen")
            .setSmallIcon(R.drawable.ic_tile_plus)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun createFloatingBubbleView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_bubble_layout, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        floatingView?.let { root ->
            collapsedView = root.findViewById(R.id.bubble_collapsed_layout)
            expandedView = root.findViewById(R.id.bubble_expanded_layout)
            collapsedCountText = root.findViewById(R.id.bubble_collapsed_count_text)
            expandedCountText = root.findViewById(R.id.bubble_count_text)

            val btnPlus: Button = root.findViewById(R.id.bubble_btn_plus)
            val btnMinus: Button = root.findViewById(R.id.bubble_btn_minus)
            val btnReset: Button = root.findViewById(R.id.bubble_btn_reset)
            val btnCollapse: Button = root.findViewById(R.id.bubble_btn_collapse)
            val btnClose: ImageView = root.findViewById(R.id.bubble_btn_close)

            // Button actions
            btnPlus.setOnClickListener {
                val current = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
                prefs.edit().putInt(CounterWidgetProvider.KEY_COUNT, current + 1).apply()
                CounterWidgetProvider.updateAllWidgets(this)
            }

            btnMinus.setOnClickListener {
                val current = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
                prefs.edit().putInt(CounterWidgetProvider.KEY_COUNT, maxOf(0, current - 1)).apply()
                CounterWidgetProvider.updateAllWidgets(this)
            }

            btnReset.setOnClickListener {
                prefs.edit().putInt(CounterWidgetProvider.KEY_COUNT, 0).apply()
                CounterWidgetProvider.updateAllWidgets(this)
            }

            btnCollapse.setOnClickListener {
                expandedView.visibility = View.GONE
                collapsedView.visibility = View.VISIBLE
            }

            btnClose.setOnClickListener {
                stopSelf()
            }

            // Draggable collapsed bubble with click-to-expand
            collapsedView.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isClick = false

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    val ev = event ?: return false
                    when (ev.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams?.x ?: 0
                            initialY = layoutParams?.y ?: 0
                            initialTouchX = ev.rawX
                            initialTouchY = ev.rawY
                            isClick = true
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val diffX = (ev.rawX - initialTouchX).toInt()
                            val diffY = (ev.rawY - initialTouchY).toInt()
                            if (Math.abs(diffX) > 10 || Math.abs(diffY) > 10) {
                                isClick = false
                            }
                            layoutParams?.x = initialX + diffX
                            layoutParams?.y = initialY + diffY
                            windowManager?.updateViewLayout(floatingView, layoutParams)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                collapsedView.visibility = View.GONE
                                expandedView.visibility = View.VISIBLE
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager?.addView(floatingView, layoutParams)
        }
    }

    private fun updateViewsWithCount() {
        val count = prefs.getInt(CounterWidgetProvider.KEY_COUNT, 0)
        collapsedCountText.text = count.toString()
        expandedCountText.text = count.toString()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == CounterWidgetProvider.KEY_COUNT) {
            updateViewsWithCount()
        }
    }

    override fun onDestroy() {
        isRunning = false
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        var isRunning = false

        fun startService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }
}
