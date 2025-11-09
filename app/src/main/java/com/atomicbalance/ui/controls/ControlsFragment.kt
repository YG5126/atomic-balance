package com.atomicbalance.ui.controls

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.atomicbalance.databinding.FragmentControlsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ControlsFragment : Fragment() {

    private var _binding: FragmentControlsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPrefs = requireContext().getSharedPreferences("reactor_params", Context.MODE_PRIVATE)

        setupSliders()
        loadValues()
    }

    private fun setupSliders() {
        // Управление стержнями (0..100 % выдвижения; 0 = полностью вставлены (макс отриц реактив), 100 = полностью выдвинуты)
        binding.sliderRods.addOnChangeListener { _, value, _ ->
            val iv = value.toInt()
            binding.tvRods.text = getString(com.atomicbalance.R.string.rods, iv)
            saveValue("rods", value)
            // подсказка
            showTooltip("Стержни", "Выдвижение стержней: $iv% — влияет непосредственно на реактивность.")
        }

        // Насосы 1-го контура (primary)
        binding.sliderPrimaryPumps.addOnChangeListener { _, value, _ ->
            val iv = value.toInt()
            binding.tvPrimaryPumps.text = "Насосы 1-го контура: $iv%"
            saveValue("primary_pumps", value)
            showTooltip("Насосы 1-го контура", "Увеличение мощности насоса повышает циркуляцию и отвод тепла из активной зоны.")
        }

        // Насосы 2-го контура (secondary)
        binding.sliderSecondaryPumps.addOnChangeListener { _, value, _ ->
            val iv = value.toInt()
            binding.tvSecondaryPumps.text = "Насосы 2-го контура: $iv%"
            saveValue("secondary_pumps", value)
            showTooltip("Насосы 2-го контура", "Вторичный контур управляет отбором пара и теплообменом с турбиной/сбросом.")
        }

        // Steam dump — доля пара, уводимого в байпас/сброс (0..100)
        binding.sliderSteamDump.addOnChangeListener { _, value, _ ->
            val iv = value.toInt()
            binding.tvSteamDump.text = "Сброс пара: $iv%"
            saveValue("steam_dump", value)
            showTooltip("Сброс пара", "Чем выше сброс, тем меньше пара идёт на турбину — меньше отбора мощности, давление меняется.")
        }
    }

    private fun loadValues() {
        val rods = sharedPrefs.getFloat("rods", 50f)
        val primary = sharedPrefs.getFloat("primary_pumps", 50f)
        val secondary = sharedPrefs.getFloat("secondary_pumps", 50f)
        val dump = sharedPrefs.getFloat("steam_dump", 0f)

        binding.sliderRods.value = rods
        binding.sliderPrimaryPumps.value = primary
        binding.sliderSecondaryPumps.value = secondary
        binding.sliderSteamDump.value = dump

        binding.tvRods.text = getString(com.atomicbalance.R.string.rods, rods.toInt())
        binding.tvPrimaryPumps.text = "Насосы 1-го контура: ${primary.toInt()}%"
        binding.tvSecondaryPumps.text = "Насосы 2-го контура: ${secondary.toInt()}%"
        binding.tvSteamDump.text = "Сброс пара: ${dump.toInt()}%"
    }

    private fun saveValue(key: String, value: Float) {
        sharedPrefs.edit().putFloat(key, value).apply()
    }

    private fun showTooltip(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
