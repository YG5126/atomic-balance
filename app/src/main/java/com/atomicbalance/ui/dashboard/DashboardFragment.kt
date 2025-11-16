package com.atomicbalance.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.atomicbalance.R
import com.atomicbalance.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var isSimulationRunning = false
    private val updateInterval = 2000L // ms
    private var simTime = 0f // seconds, for chart x-axis
    private val fluxEntries = mutableListOf<Entry>()

    private lateinit var sharedPrefs: android.content.SharedPreferences

    // --- State variables (units: percent or physical proxies) ---
    private var powerPct = 1f                      // реакторная мощность в %
    private var tempCore = 280f                    // °C (температура активной зоны)
    private var pressureSecondary = 140f           // бар (вторичный контур)
    private var neutronFlux = 1e12f                // произв. ед.
    private var timeStep = 2f                      // сек (соответствует updateInterval / 1000)

    // --- Model parameters (подберите под ваши нужды) ---
    private val maxRodReactivity = 0.08f           // максимальная положительная реактивность когда стержни полностью выведены
    private val tauPower = 6.0f                    // с — характерное время отклика мощности
    private val thermalCapacity = 1.2e6f           // J/°C — теплоёмкость активной зоны (масштаб)
    private val heatPerPct = 3.0e6f                // J/s при 100% -> умножаем на (powerPct/100)
    private val primaryFlowCoeff = 0.01f           // как мощность насоса переводится в объём протока (условная)
    private val secondaryFlowCoeff = 0.012f
    private val turbineExtractionEff = 0.9f        // эффективность отбора мощности на турбину (если не сброшено)
    private val baseSecondaryPressure = 140f       // базовое давление (бар)
    private val pressureCoeff = 25f                // как поток влияет на давление (бар)
    private val tempFeedbackCoeff = 5e-5f          // насколько температура даёт отрицательную обратную связь в реактивности

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("DashboardFragment", "onCreateView called")
        try {
            _binding = FragmentDashboardBinding.inflate(inflater, container, false)
            Log.d("DashboardFragment", "Binding inflated successfully")
            return binding.root
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error inflating view", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DashboardFragment", "onViewCreated called")
        try {
            sharedPrefs = requireContext().getSharedPreferences("reactor_params", Context.MODE_PRIVATE)
            Log.d("DashboardFragment", "SharedPreferences initialized")

            // Инициализация стартовых значений (если ещё нет)
            if (!sharedPrefs.contains("rods")) {
                sharedPrefs.edit()
                    .putFloat("rods", 50f)
                    .putFloat("primary_pumps", 50f)
                    .putFloat("secondary_pumps", 50f)
                    .putFloat("steam_dump", 0f)
                    .apply()
                Log.d("DashboardFragment", "Initial values saved")
            }

        setupChart()
        Log.d("DashboardFragment", "Chart setup completed")
        setupButtons()
        Log.d("DashboardFragment", "Buttons setup completed")
        setupControls()
        Log.d("DashboardFragment", "Controls setup completed")
        // начальные вычисления
        updateParameters()
        Log.d("DashboardFragment", "Initial parameters updated")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error in onViewCreated", e)
            e.printStackTrace()
        }
    }

    private fun setupChart() {
        try {
            binding.neutronChart.apply {
                description.isEnabled = false
                xAxis.isEnabled = true
                axisLeft.isEnabled = true
                axisRight.isEnabled = false
                legend.isEnabled = true
                setTouchEnabled(true)
                
                // Настройка осей
                xAxis.textColor = 0xFF00E5FF.toInt()
                axisLeft.textColor = 0xFF00E5FF.toInt()
                legend.textColor = 0xFF00E5FF.toInt()
                
                // Настройка сетки
                xAxis.gridColor = 0x4000E5FF.toInt()
                axisLeft.gridColor = 0x4000E5FF.toInt()
                
                invalidate()
            }
            Log.d("DashboardFragment", "Chart configured successfully")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error setting up chart", e)
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        try {
            binding.btnStartPause.setOnClickListener {
                isSimulationRunning = !isSimulationRunning
                if (isSimulationRunning) {
                    binding.btnStartPause.text = "СТОП"
                    binding.tvStatusBadge.text = "АКТИВЕН"
                    binding.tvStatusBadge.setBackgroundColor(0xFF16A34A.toInt())
                    startSimulation()
                } else {
                    binding.btnStartPause.text = "ПУСК"
                    binding.tvStatusBadge.text = "ОСТАНОВЛЕН"
                    binding.tvStatusBadge.setBackgroundColor(0xFF6B7280.toInt())
                    stopSimulation()
                }
            }
            Log.d("DashboardFragment", "Button click listener set")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error setting up buttons", e)
            e.printStackTrace()
        }
    }

    private fun setupControls() {
        try {
            // Управление стержнями
            binding.sliderRodsDashboard.addOnChangeListener { _, value, _ ->
                val iv = value.toInt()
                binding.tvRodsDashboard.text = "$iv%"
                sharedPrefs.edit().putFloat("rods", value).apply()
            }

            // Насосы 1-го контура
            binding.sliderPrimaryPumpsDashboard.addOnChangeListener { _, value, _ ->
                val iv = value.toInt()
                binding.tvPrimaryPumpsDashboard.text = "$iv%"
                sharedPrefs.edit().putFloat("primary_pumps", value).apply()
            }

            // Насосы 2-го контура
            binding.sliderSecondaryPumpsDashboard.addOnChangeListener { _, value, _ ->
                val iv = value.toInt()
                binding.tvSecondaryPumpsDashboard.text = "$iv%"
                sharedPrefs.edit().putFloat("secondary_pumps", value).apply()
            }

            // Сброс пара
            binding.sliderSteamDumpDashboard.addOnChangeListener { _, value, _ ->
                val iv = value.toInt()
                binding.tvSteamDumpDashboard.text = "$iv%"
                sharedPrefs.edit().putFloat("steam_dump", value).apply()
            }

            // Загрузка начальных значений
            loadControlValues()
            Log.d("DashboardFragment", "Control sliders configured")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error setting up controls", e)
            e.printStackTrace()
        }
    }

    private fun loadControlValues() {
        try {
            val rods = sharedPrefs.getFloat("rods", 50f)
            val primary = sharedPrefs.getFloat("primary_pumps", 50f)
            val secondary = sharedPrefs.getFloat("secondary_pumps", 50f)
            val dump = sharedPrefs.getFloat("steam_dump", 0f)

            binding.sliderRodsDashboard.value = rods
            binding.tvRodsDashboard.text = "${rods.toInt()}%"

            binding.sliderPrimaryPumpsDashboard.value = primary
            binding.tvPrimaryPumpsDashboard.text = "${primary.toInt()}%"

            binding.sliderSecondaryPumpsDashboard.value = secondary
            binding.tvSecondaryPumpsDashboard.text = "${secondary.toInt()}%"

            binding.sliderSteamDumpDashboard.value = dump
            binding.tvSteamDumpDashboard.text = "${dump.toInt()}%"
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error loading control values", e)
            e.printStackTrace()
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

    /**
     * Основной шаг симуляции:
     * 1) читаем текущие установки (стержни, насосы, сброс пара)
     * 2) считаем реактивность и динамику мощности (упрощённая кинетика)
     * 3) считаем тепловой баланс и температуру
     * 4) считаем давление вторичного контура и нейтронный поток
     */
    private fun updateParameters() {
        // --- считываем управляющие ---
        val rods = sharedPrefs.getFloat("rods", 0f)            // 0..100 (процент выдвижения)
        val primaryPumps = sharedPrefs.getFloat("primary_pumps", 50f) // 0..100
        val secondaryPumps = sharedPrefs.getFloat("secondary_pumps", 50f) // 0..100
        val steamDump = sharedPrefs.getFloat("steam_dump", 100f)  // 0..100 (доля сброса пара)

        // --- 1. Реактивность (rho) ---
        // st Род: при 0% (полностью вставлены) дают -maxRodReactivity; при 100% — +maxRodReactivity
        val rodFrac = rods / 100f
        val rodRho = (rodFrac * 2f - 1f) * maxRodReactivity // когда rods=0 -> -max, rods=100 -> +max

        // отрицательная обратная связь по температуре: чем выше temp -> тем меньше эффективная реактивность
        val tempFeedback = tempFeedbackCoeff * (tempCore - 280f) // нулевая при 280°C

        // итоговая реактивность
        val rho = rodRho - tempFeedback

        // --- 2. Динамика мощности (упрощённо, first-order) ---
        // dP/dt = (rho / tauPower) * P
        // используем явную Эйлера интеграцию
        val dPdt = (rho / tauPower) * powerPct
        powerPct += dPdt * timeStep

        // небольшая безопасность — ограничить 0..100
        powerPct = powerPct.coerceIn(0f, 100f)

        // --- 3. Тепловой баланс ---
        // Выделяемая тепловая мощность (в J/s) пропорциональна powerPct
        val heatProduced = (powerPct / 100f) * heatPerPct * 100f // масштабируем: powerPct в % -> J/s
        // Поток в первичном контуре пропорционален мощности насоса
        val primaryFlow = primaryFlowCoeff * (primaryPumps / 100f) // услов. м³/s
        // Поток во вторичном контуре пропорционален мощности вторичных насосов
        val secondaryFlow = secondaryFlowCoeff * (secondaryPumps / 100f)

        // Отвод тепла через теплообмен зависит от primaryFlow и secondaryFlow и от доли пара, уводимого в турбину
        // Чем больше steamDump — тем меньше отбор на турбину; также часть тепла уходит при сбросе, но с пониженной эффективностью
        val steamToTurbineFrac = (1f - steamDump / 100f) * turbineExtractionEff
        val heatRemoved = (primaryFlow * 1e6f + secondaryFlow * 1e6f) * steamToTurbineFrac // J/s (условно)

        // Изменение температуры: dT = (heatProduced - heatRemoved) / thermalCapacity * dt
        val dTdt = (heatProduced - heatRemoved) / thermalCapacity
        tempCore += dTdt * timeStep

        // Ограничим температуру в разумных пределах
        tempCore = tempCore.coerceIn(50f, 1000f)

        // --- 4. Давление второго контура (упрощённая зависимость от потока и сброса) ---
        // Чем больше вторичный поток и меньше сброс -> ниже давление (больше отвода уходит на турбину), при большом сбросе давление растёт.
        val effectiveSecondaryFlow = secondaryFlow * steamToTurbineFrac
        pressureSecondary = baseSecondaryPressure - pressureCoeff * effectiveSecondaryFlow + (steamDump / 100f) * 20f
        pressureSecondary = pressureSecondary.coerceIn(1f, 200f)

        // --- 5. Нейтронный поток (масштабируем от мощности) ---
        neutronFlux = (powerPct * 1e10f) * (1f + (Random.nextFloat() - 0.5f) * 0.02f) // шум ±1%

        // --- 6. Обновление UI ---
        try {
            if (_binding == null) {
                Log.w("DashboardFragment", "Binding is null, skipping UI update")
                return
            }
            
            binding.tvKEff.text = "rho: %.4f".format(rho)
            // color: red if negative reactivity (subcritical), green near zero, blue positive
            binding.tvKEff.setTextColor(
                when {
                    rho < 0f -> 0xFFFF0000.toInt()
                    rho < 0.001f -> 0xFF00FF00.toInt()
                    else -> 0xFF0000FF.toInt()
                }
            )

            binding.tvPower.text = "Мощность: %.1f%%".format(powerPct)
            binding.tvTemperature.text = "%.1f".format(tempCore)
            binding.tvPressure.text = "%.1f".format(pressureSecondary)
            binding.tvNeutronFlux.text = "%.3e".format(neutronFlux)
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error updating UI", e)
            e.printStackTrace()
        }

        // --- 7. Обновление графика нейтронного потока ---
        fluxEntries.add(Entry(simTime, neutronFlux))
        if (fluxEntries.size > 40) fluxEntries.removeAt(0)
        val dataSet = LineDataSet(fluxEntries, "Нейтронный поток")
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f
        // цвет можно настроить в xml, здесь используем один цвет
        dataSet.color = 0xFF00E5FF.toInt()
        binding.neutronChart.data = LineData(dataSet)
        binding.neutronChart.invalidate()

        simTime += timeStep
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSimulation()
        _binding = null
    }
}
