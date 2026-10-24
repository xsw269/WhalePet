package com.example.whalepet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 纯代码创建界面
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(0xFF061224.toInt())
        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)
        title.text = "鲸鱼娘桌宠"
        title.textSize = 24f
        title.setTextColor(0xFF7DD3FC.toInt())
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val desc = TextView(this)
        desc.text = "开启后鲸鱼娘浮在桌面\n点击打开 DeepSeek，长按换形象"
        desc.textSize = 14f
        desc.setTextColor(0xFF9CA3AF.toInt())
        desc.gravity = Gravity.CENTER
        desc.setPadding(0, 30, 0, 60)
        layout.addView(desc)

        val btnStart = Button(this)
        btnStart.text = "开启桌宠"
        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                val intent = Intent(this, PetService::class.java)
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                moveTaskToBack(true)
            }
        }
        layout.addView(btnStart)

        val btnStop = Button(this)
        btnStop.text = "关闭桌宠"
        btnStop.setOnClickListener {
            stopService(Intent(this, PetService::class.java))
            Toast.makeText(this, "桌宠已关闭", Toast.LENGTH_SHORT).show()
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 30, 0, 0)
        layout.addView(btnStop, params)

        setContentView(layout)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
