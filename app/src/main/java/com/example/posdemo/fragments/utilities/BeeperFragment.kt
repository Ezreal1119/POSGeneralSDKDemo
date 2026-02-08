package com.example.posdemo.fragments.utilities

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.posdemo.UtilitiesActivity
import com.example.posdemo.databinding.FragmentBeeperBinding

class BeeperFragment : Fragment() {
    companion object {
        private val durationArray = arrayOf(100, 200, 500, 1000, 2000)
    }

    private var _binding: FragmentBeeperBinding? = null
    private val binding
        get() = _binding!!

    private val mBeeperManager
        get() = (requireActivity() as UtilitiesActivity).mBeeperManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBeeperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStartBeeper.setOnClickListener { onStartBeeperButtonClicked() }
        binding.spDuration.adapter = ArrayAdapter(requireContext(), simple_spinner_item, durationArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onStartBeeperButtonClicked() {
        val count = binding.sliderBeeperCount.value.toInt()
        val duration = binding.spDuration.selectedItem as Int
        Thread {
            runCatching {
                mBeeperManager.startBeep(count, duration)
            }.onFailure {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    it.printStackTrace()
                }
            }
        }.apply { start() }
        binding.btnStartBeeper.isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            binding.btnStartBeeper.isEnabled = true
        }, ((duration + 80) * (count - 1)).toLong())
    }
}