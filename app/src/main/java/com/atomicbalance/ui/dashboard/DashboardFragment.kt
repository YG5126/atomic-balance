package com.atomicbalance.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.atomicbalance.R  // <-- Добавьте эту строку явно
import com.atomicbalance.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.random.Random

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var isSimulationRunning = false
    private val updateInterval = 2000L // 2 секунды
    private var time = 0f // Для графика
    private val fluxEntries = mutableListOf<Entry>()

    private lateinit var sharedPrefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = requireContext().getSharedPreferences("reactor_params", Context.MODE_PRIVATE)

        setupChart()
        setupButtons()
        updateParameters() // Начальный апдейт
    }

    private fun setupChart() {
        binding.neutronChart.apply {
            description.isEnabled = false
            xAxis.isEnabled = true
            axisLeft.isEnabled = true
            axisRight.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            invalidate()
        }
    }

    private fun setupButtons() {
        binding.btnStartPause.setOnClickListener {
            isSimulationRunning = !isSimulationRunning

            if (isSimulationRunning) {
                binding.btnStartPause.text = "СТОП"
                binding.btnStartPause.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFFDC2626.toInt())
                binding.tvStatusBadge.text = "АКТИВЕН"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.badge_active)
                startSimulation()
            } else {
                binding.btnStartPause.text = "ПУСК"
                binding.btnStartPause.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFF16A34A.toInt())
                binding.tvStatusBadge.text = "ОСТАНОВЛЕН"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.badge_stopped)
                stopSimulation()
            }
        }
    }

    private fun startSimulation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isSimulationRunning) {
                    updateParameters()
                    handler.postDelayed(this, updateInterval)
                }
            }
        }, updateInterval)
    }

    private fun stopSimulation() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateParameters() {
        // Чтение из SharedPreferences (от Студента 2)
        val rods = sharedPrefs.getFloat("rods", 50f) // 0-100%
        val pumps = sharedPrefs.getFloat("pumps", 50f)
        val steamPower = sharedPrefs.getFloat("steam_power", 50f)

        // Простая симуляция PWR (на основе поисковых данных)
        val kEff = 1.2f - (rods / 100f * 0.3f) + Random.nextFloat() * 0.02f - 0.01f // Шум для реализма
        val power = (kEff * (steamPower / 100f) * 100f).coerceIn(0f, 100f) // %
        val temperature = 280f + (power / 100f * 40f) + ((100f - pumps) / 100f * 20f) // 280-320°C
        val pressure = 140f + ((temperature - 280f) / 40f * 20f) // 140-160 бар

        // ИСПРАВЛЕНО: явное приведение к Float
        val neutronFlux = (power * 1e12).toFloat() // Примерная плотность нейтронов (flux ~ power)

        // Обновление показателей
        binding.tvKEff.text = "k-eff: %.3f".format(kEff)
        binding.tvKEff.setTextColor(when {
            kEff < 1f -> 0xFFFF0000.toInt() // Красный
            kEff == 1f -> 0xFF00FF00.toInt() // Зеленый
            else -> 0xFF0000FF.toInt() // Синий
        })

        binding.tvPower.text = "Мощность: %.1f%%".format(power)
        binding.tvTemperature.text = "Температура: %.1f°C".format(temperature)
        binding.tvPressure.text = "Давление: %.1f бар".format(pressure)

        // Обновление графика нейтронов
        fluxEntries.add(Entry(time, neutronFlux))
        if (fluxEntries.size > 20) fluxEntries.removeAt(0) // Ограничение
        val dataSet = LineDataSet(fluxEntries, "Плотность нейтронов")
        dataSet.color = 0xFF00E5FF.toInt()
        dataSet.setDrawCircles(false) // Убираем точки для производительности
        dataSet.lineWidth = 2f
        binding.neutronChart.data = LineData(dataSet)
        binding.neutronChart.invalidate()

        time += 2f // Каждые 2 секунды

        // Анимация нейтронов в NeutronView (обновляется автоматически)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSimulation()
        _binding = null
    }
}