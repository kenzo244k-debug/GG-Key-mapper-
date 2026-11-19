package com.ggkeymapper

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var menu: View? = null
    private val green = Color.rgb(57, 232, 121)
    private val bg = Color.rgb(9, 19, 12)

    override fun onCreate() { super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; startAsForeground(); showBubble() }

    private fun startAsForeground() {
        val channelId = "gg_mapper_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "GG Key mapper", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, channelId) else Notification.Builder(this)
        notification.setContentTitle("GG Key mapper").setContentText("Bolinha flutuante ativada").setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true)
        startForeground(41, notification.build())
    }

    private fun params(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        return WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 180 }
    }

    private fun label(text: String, size: Float): TextView = TextView(this).apply { this.text = text; textSize = size; setTextColor(green); setPadding(18, 13, 18, 13); setBackgroundColor(bg) }

    private fun showBubble() {
        val b = label("GG", 18f).apply { gravity = Gravity.CENTER; setBackgroundColor(Color.rgb(16, 45, 27)) }
        val lp = params().apply { width = dp(58); height = dp(58); x = 20; y = 160 }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        b.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = lp.x; startY = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> { val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt(); if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true; lp.x = startX + dx; lp.y = startY + dy; try { wm.updateViewLayout(b, lp) } catch (_: Exception) {}; true }
                MotionEvent.ACTION_UP -> { if (!moved) toggleMenu(); true }
                else -> true
            }
        }
        bubble = b; wm.addView(b, lp)
    }

    private fun toggleMenu() {
        if (menu != null) { closeMenu(); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)); setBackgroundColor(bg) }
        box.addView(label("GG • CONTROLES", 15f))
        listOf("Abrir editor HUD", "Salvar mapeamento", "Fechar menu").forEach { item ->
            val row = label(item, 13f)
            row.setOnClickListener {
                when (item) {
                    "Fechar menu" -> closeMenu()
                    "Abrir editor HUD" -> { val launch = packageManager.getLaunchIntentForPackage(packageName); launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP); if (launch != null) startActivity(launch); closeMenu() }
                    "Salvar mapeamento" -> Toast.makeText(this, "Os mapeamentos são salvos no editor HUD.", Toast.LENGTH_SHORT).show()
                }
            }
            box.addView(row)
        }
        menu = box
        val lp = params().apply { x = dp(88); y = dp(160); flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE }
        wm.addView(box, lp)
    }

    private fun closeMenu() { menu?.let { try { wm.removeView(it) } catch (_: Exception) {} }; menu = null }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onDestroy() { closeMenu(); bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubble = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
