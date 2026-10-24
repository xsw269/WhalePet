package com.example.whalepet

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.math.abs

class PetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var petView: FrameLayout
    private lateinit var webView: WebView
    private lateinit var params: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressed = false

    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var dragging = false

    private val themes = arrayOf("classic", "pink", "violet")
    private val themeNames = arrayOf("经典蓝", "樱粉", "紫梦")
    private var themeIndex = 0

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createPetView()
    }

    private fun startForegroundNotification() {
        val channelId = "pet"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "桌宠运行中", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, channelId)
        else @Suppress("DEPRECATION") Notification.Builder(this)

        val notification = builder
            .setContentTitle("鲸鱼娘桌宠运行中")
            .setContentText("点击打开 DeepSeek · 长按换形象")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createPetView() {
        val size = (170 * resources.displayMetrics.density).toInt()

        petView = object : FrameLayout(this) {
            override fun onInterceptTouchEvent(ev: MotionEvent) = true
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            loadUrl("file:///android_asset/whale.html#${themes[themeIndex]}")
        }

        petView.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 320
        }

        petView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    dragging = false
                    longPressed = false
                    longPressRunnable = Runnable {
                        longPressed = true
                        switchTheme()
                    }.also { handler.postDelayed(it, 650) }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (longPressed) {
                        true
                    } else {
                        val dx = e.rawX - touchX
                        val dy = e.rawY - touchY
                        if (abs(dx) > 12 || abs(dy) > 12) {
                            dragging = true
                            longPressRunnable?.let { handler.removeCallbacks(it) }
                            params.x = initialX + dx.toInt()
                            params.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(petView, params)
                        }
                        true
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    longPressRunnable = null
                    if (!dragging && !longPressed) openDeepSeek()
                    true
                }

                else -> false
            }
        }

        windowManager.addView(petView, params)
    }

    private fun switchTheme() {
        themeIndex = (themeIndex + 1) % themes.size
        webView.loadUrl("file:///android_asset/whale.html#${themes[themeIndex]}")
        Toast.makeText(this, "形象：${themeNames[themeIndex]}", Toast.LENGTH_SHORT).show()
    }

    private fun openDeepSeek() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://chat.deepseek.com")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            startActivity(intent)
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
        if (::petView.isInitialized) {
            windowManager.removeView(petView)
            webView.destroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
