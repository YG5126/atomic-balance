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
        binding.sliderRods.addOnChangeListener { _, value, _ ->
            binding.tvRods.text = getString(com.atomicbalance.R.string.rods, value.toInt())
            saveValue("rods", value)
            showTooltip("Стержни", getString(com.atomicbalance.R.string.fact_rods))
        }

        binding.sliderPumps.addOnChangeListener { _, value, _ ->
            binding.tvPumps.text = getString(com.atomicbalance.R.string.pumps, value.toInt())
            saveValue("pumps", value)
            showTooltip("Насосы", getString(com.atomicbalance.R.string.fact_pumps))
        }

        binding.sliderSteam.addOnChangeListener { _, value, _ ->
            binding.tvSteam.text = getString(com.atomicbalance.R.string.steam_power, value.toInt())
            saveValue("steam_power", value)
            showTooltip("Парогенераторы", getString(com.atomicbalance.R.string.fact_steam))
        }
    }

    private fun loadValues() {
        val rods = sharedPrefs.getFloat("rods", 50f)
        val pumps = sharedPrefs.getFloat("pumps", 50f)
        val steam = sharedPrefs.getFloat("steam_power", 50f)

        binding.sliderRods.value = rods
        binding.sliderPumps.value = pumps
        binding.sliderSteam.value = steam

        binding.tvRods.text = getString(com.atomicbalance.R.string.rods, rods.toInt())
        binding.tvPumps.text = getString(com.atomicbalance.R.string.pumps, pumps.toInt())
        binding.tvSteam.text = getString(com.atomicbalance.R.string.steam_power, steam.toInt())
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