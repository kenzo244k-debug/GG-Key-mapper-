package com.ggkeymapper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class HudEditorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var mappings: MutableList<KeyMapping> = mutableListOf()
    var selectedIndex: Int = -1
    var onSelectionChanged: (() -> Unit)? = null
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(12, 24, 16))
        paint.color = Color.rgb(40, 75, 51)
        paint.strokeWidth = 2f
        val stepX = width / 8f
        val stepY = height / 5f
        var x = 0f
        while (x <= width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += stepX }
        var y = 0f
        while (y <= height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += stepY }
        mappings.forEachIndexed { index, mapping ->
            val cx = mapping.x * width
            val cy = mapping.y * height
            paint.color = if (index == selectedIndex) Color.YELLOW else Color.rgb(57, 232, 121)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, 25f, paint)
            paint.color = Color.BLACK
            paint.textSize = 19f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("K${mapping.keyCode}", cx, cy + 7f, paint)
        }
        paint.color = Color.LTGRAY
        paint.textSize = 13f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Toque em um ponto para posicionar o botão selecionado", 12f, height - 12f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedIndex = mappings.indices.minByOrNull { index ->
                    val m = mappings[index]
                    val dx = event.x - m.x * width
                    val dy = event.y - m.y * height
                    dx * dx + dy * dy
                }?.takeIf { index ->
                    val m = mappings[index]
                    abs(event.x - m.x * width) < 45 && abs(event.y - m.y * height) < 45
                } ?: selectedIndex
                dragging = true
                updatePosition(event.x, event.y)
                onSelectionChanged?.invoke()
                return true
            }
            MotionEvent.ACTION_MOVE -> { if (dragging) updatePosition(event.x, event.y); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { dragging = false; return true }
        }
        return true
    }

    private fun updatePosition(x: Float, y: Float) {
        if (selectedIndex in mappings.indices) {
            mappings[selectedIndex] = mappings[selectedIndex].copy(x = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f), y = (y / height.coerceAtLeast(1)).coerceIn(0f, 1f))
            invalidate()
        }
    }
}
