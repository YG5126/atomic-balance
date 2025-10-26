package com.atomicbalance.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class NeutronView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintFast = Paint().apply {
        color = Color.YELLOW // Быстрые нейтроны
        style = Paint.Style.FILL
    }

    private val paintThermal = Paint().apply {
        color = Color.CYAN // Тепловые (голубые)
        style = Paint.Style.FILL
    }

    private val neutrons = mutableListOf<Neutron>()

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val updateInterval = 33L // ~30 FPS

    init {
        startAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        neutrons.forEach { neutron ->
            val paint = if (neutron.isFast) paintFast else paintThermal
            canvas.drawCircle(neutron.x, neutron.y, 5f, paint)
        }
    }

    private fun startAnimation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateNeutrons()
                invalidate() // Перерисовка
                handler.postDelayed(this, updateInterval)
            }
        }, updateInterval)
    }

    private fun updateNeutrons() {
        if (neutrons.size < 50) { // Добавляем новые
            neutrons.add(Neutron(Random.nextBoolean()))
        }

        neutrons.forEach { neutron ->
            neutron.x += neutron.vx
            neutron.y += neutron.vy

            // Отражение от границ
            if (neutron.x <= 0 || neutron.x >= width) neutron.vx = -neutron.vx
            if (neutron.y <= 0 || neutron.y >= height) neutron.vy = -neutron.vy

            // Шанс "замедления" (быстрый -> тепловой)
            if (neutron.isFast && Random.nextFloat() < 0.05f) neutron.isFast = false
        }

        // Удаляем "вышедшие" (симуляция поглощения)
        neutrons.removeIf { Random.nextFloat() < 0.01f }
    }

    data class Neutron(
        var isFast: Boolean,
        var x: Float = Random.nextFloat() * 400f, // Пример ширины
        var y: Float = Random.nextFloat() * 200f,
        var vx: Float = Random.nextFloat() * 10f - 5f,
        var vy: Float = Random.nextFloat() * 10f - 5f
    )
}