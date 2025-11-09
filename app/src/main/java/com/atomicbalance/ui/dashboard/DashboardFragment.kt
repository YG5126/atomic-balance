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

        // 1. Инициализация стартовых значений, если SharedPreferences пусты
        if (!sharedPrefs.contains("rods")) {
            sharedPrefs.edit()
                .putFloat("rods", 50f)          // среднее выдвижение стержней
                .putFloat("pumps", 50f)         // средняя мощность насосов
                .putFloat("steam_power", 100f)   // средний отбор пара
                .apply()
        }

        setupChart()
        setupButtons()
        updateParameters() // Начальный апдейт после установки стартовых значений
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
        val rods = sharedPrefs.getFloat("rods", 50f)
        val pumps = sharedPrefs.getFloat("pumps", 50f)
        val steamPower = sharedPrefs.getFloat("steam_power", 50f)

        // 1. k-eff на основе выдвижения стержней
        val rodEffect = 0.3f * (rods / 100f)
        val kEff = 1.05f - rodEffect + Random.nextFloat() * 0.01f - 0.005f

        // 2. Мощность реактора
        val pumpEffect = 0.5f * (pumps / 100f) // влияние циркуляции на температуру
        val power = (kEff * (steamPower / 100f) * 100f).coerceIn(0f, 100f)

        // 3. Температура активной зоны
        val tempCore = 280f + (power / 100f * 100f) * (1f - pumpEffect)

        // 4. Давление вторичного контура
        val steamLoad = steamPower / 100f
        val pressure = 140f + (power / 100f * 20f) * (1f - 0.3f * steamLoad)

        // 5. Нейтронный поток
        val neutronFlux = power * 1e12f * (1f + Random.nextFloat() * 0.02f - 0.01f)

        // 6. Обновление UI
        binding.tvKEff.text = "k-eff: %.3f".format(kEff)
        binding.tvKEff.setTextColor(
            when {
                kEff < 1f -> 0xFFFF0000.toInt()
                kEff == 1f -> 0xFF00FF00.toInt()
                else -> 0xFF0000FF.toInt()
            }
        )
        binding.tvPower.text = "Мощность: %.1f%%".format(power)
        binding.tvTemperature.text = "Температура: %.1f°C".format(tempCore)
        binding.tvPressure.text = "Давление: %.1f бар".format(pressure)

        // 7. Обновление графика нейтронного потока
        fluxEntries.add(Entry(time, neutronFlux))
        if (fluxEntries.size > 20) fluxEntries.removeAt(0)
        val dataSet = LineDataSet(fluxEntries, "Нейтронный поток")
        dataSet.color = 0xFF00E5FF.toInt()
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f
        binding.neutronChart.data = LineData(dataSet)
        binding.neutronChart.invalidate()

        time += 2f
    }



    override fun onDestroyView() {
        super.onDestroyView()
        stopSimulation()
        _binding = null
    }
}